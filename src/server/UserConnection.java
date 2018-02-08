package server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.channels.Channels;
import java.nio.channels.Pipe;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Iterator;
import java.util.Set;

import com.google.gson.Gson;

public class UserConnection {

	private SocketChannel socketChannel;
	private Server server;
	private Pipe pipe;
	private Selector selector;
	private Room room;
	private Gson gson; // For serializing/deserializing JSON

	// CONSTRUCTOR
	public UserConnection(Server s, SocketChannel sc) throws IOException {
		server = s;
		socketChannel = sc;
		pipe = Pipe.open();
		gson = new Gson();
	}

	/**
	 * ........
	 */
	public void run() throws IOException {
		// Transmit messages using the Web Socket
		DataInputStream incomingData = new DataInputStream(socketChannel.socket().getInputStream());

		// Join the requested room
		String joinRequest = readMessage(incomingData);
		Message joinRequestMessage = gson.fromJson(joinRequest, Message.class);
		
		// User field should be "join", Message field should be name of room
		if (!joinRequestMessage.getUser().equals("join")) {
			disconnect();
			return;
		} else {
			room = server.joinRoom(joinRequestMessage.getMessage(), this);
		}

		selector = Selector.open(); // Constructs a new selector

		socketChannel.configureBlocking(false); // Needs to be open to register with selector.
		socketChannel.register(selector, SelectionKey.OP_READ); // Register the channel to the selector
		pipe.source().configureBlocking(false);
		pipe.source().register(selector, SelectionKey.OP_READ);

		while (!socketChannel.socket().isClosed()) {
			selector.select(); // Blocks until any of its channels are "ready"
			Set<SelectionKey> keys = selector.selectedKeys();
			Iterator<SelectionKey> iter = keys.iterator();

			while (iter.hasNext()) {
				SelectionKey key = iter.next();

				// If we're reading from the SocketChannel
				if (key.isReadable() && key.channel() == this.socketChannel) {
					iter.remove(); // Remove key from the set for iterating
					socketChannel.keyFor(selector).cancel(); // Remove the SocketChannel from the selector?
					socketChannel.configureBlocking(true); // Turn on blocking so we can read from stream
					
					String message = readMessage(incomingData);
					Message msg = gson.fromJson(message, Message.class);
					room.addMessage(msg);

					// Re-register the channel to the selector
					socketChannel.configureBlocking(false); // Blocking must be off in order to register
					selector.selectNow(); // Makes selector happy
					socketChannel.register(selector, SelectionKey.OP_READ); // Notify channel when data to read	
				} 
				
				// If we're reading from the Pipe
				else if (key.isReadable()){
					try {
						iter.remove(); // Remove key from the set for iterating
						socketChannel.keyFor(selector).cancel(); // Remove the SocketChannel from the selector
						pipe.source().keyFor(selector).cancel(); // Remove the Pipe from the selector
						
						socketChannel.configureBlocking(true); // Turn on blocking so we can write to stream
						pipe.source().configureBlocking(true); // Turn on blocking so we can read from stream
						
						// Get message and send
						InputStream pipeInputStream = Channels.newInputStream(pipe.source());
						ObjectInputStream oos = new ObjectInputStream(pipeInputStream);
						String message = (String) oos.readObject();
						sendMessage(message);
						
						// Re-register the pipe to the selector
						pipe.source().configureBlocking(false);
						socketChannel.configureBlocking(false);
						selector.selectNow();
						
						socketChannel.register(selector, SelectionKey.OP_READ);
						pipe.source().register(selector, SelectionKey.OP_READ);
						
					} catch (ClassNotFoundException e) {
						System.out.println("Couldn't turn Object Stream Object to String");
						e.printStackTrace();
					}

					
				}

			}

		}
	}

	/**
	 * Extract the message from the WebSockets incoming data
	 */
	public String readMessage(DataInputStream incomingData) throws IOException {
		// Get the first two bytes (opcode and payload length)
		byte[] messageInfo = new byte[2];
		incomingData.readFully(messageInfo);

		// ensure the First four bytes and FIN are 1000
		byte fin = (byte) ((messageInfo[0] >>> 4) & 0x0F);
		if (fin != 8) {
			incomingData.close();
			System.out.println("Problem: the first four bytes of the message were not 1000");
		}

		// Get the opcode. 1 should be a message, close socket on anything else
		byte opcode = (byte) (messageInfo[0] & 0x0F);
		if (opcode == 1) {
		} else {
			disconnect();
			System.out.println("The opcode isn't 1");
			return "";
		}

		// Make sure message is masked
		int mask = (byte) ((messageInfo[1] >>> 7) & 0x01);
		if (mask != 1) {
			disconnect();
			System.out.println("Client sent unmasked message. 5.1 of spec says disconnect in response.");
			return "";
		}

		// Get the payload length
		int payloadLength = (byte) (messageInfo[1] & 0x7F);

		// If less than 126, then this is the length of the message
		if (payloadLength < 126) {
			// System.out.println("Payload is size: " + payloadLength);
		}
		// If length == 126, then the real message length is in the next two bytes
		else if (payloadLength == 126) {
			payloadLength = incomingData.readUnsignedShort();
			System.out.println("Real Payload Length: " + payloadLength);
		}
		// If length = 127, then the real message length is in the next four bytes
		else {
			disconnect();
			System.out.println("Payload is too big, not going to deal with it.");
		}

		// get the masking key
		byte[] maskingKey = new byte[4];
		incomingData.readFully(maskingKey);

		// Get the rest of the message
		byte[] messageAsBytes = new byte[payloadLength];
		incomingData.readFully(messageAsBytes);
		String message = "";
		for (int i = 0; i < payloadLength; i++) {
			message += ((char) (messageAsBytes[i] ^ maskingKey[i % 4]));
		}

		System.out.println("New message: " + message);
		return message;
	}

	/**
	 * Sends a message via the WebSocket protocol. Doesn't handle exceptionally
	 * large messages. Expectation is that the message passed to this method is in
	 * JSON format.
	 */
	public void sendMessage(String message) {

		try {
			DataOutputStream outgoingData = new DataOutputStream(socketChannel.socket().getOutputStream());

			// FIN and opcode - 1000 0001
			outgoingData.write(0x81);
			// Length of message, mask of 0: <= 0111 1101
			if (message.length() < 126) {
				outgoingData.write(message.length());
			} else {
				// Length of message is contained in next 2 bytes: 0111 1110
				outgoingData.write(0x7E);
				// send length in next 2 bytes
				outgoingData.writeShort(message.length());
			}

			// Send actual message
			outgoingData.writeBytes(message);
			outgoingData.flush();

		}

		catch (IOException e) {
			disconnect();
			e.printStackTrace();
		}

	}

	/**
	 * Completes the handshake for opening a WebSocket
	 * 
	 * Sends the HTTP header to the client stating that the request to upgrade the
	 * data transmission protocol to a WebSocket has been accepted.
	 */
	public void acceptConnection(OutputStream stream, HTTPRequest req) {
		// Output stream that writes Strings to the stream going to the client
		PrintWriter oStringStream = new PrintWriter(stream);

		// Create the WebSocket Accept code (WebSocket Key + Magic String)
		String acceptCode = req.getWebSocketKey() + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
		String codeHashedAsString = hashAcceptCode(acceptCode);

		// Send header info
		oStringStream.print("HTTP/1.1 101 Switching Protocols" + "\r\n");
		oStringStream.print("Upgrade: websocket" + "\r\n");
		oStringStream.print("Connection: Upgrade" + "\r\n");
		oStringStream.print("Sec-WebSocket-Accept: " + codeHashedAsString + "\r\n");
		oStringStream.print("\r\n");
		oStringStream.flush();
	}

	/**
	 * Helper method that takes a clients Sec-WebSocket-Key concatenated with the
	 * magic string and returns the base64 encoding of its hash.
	 */
	private String hashAcceptCode(String acceptCode) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1"); // MessageDigest object needed for hashing
			byte[] codeAsBytes = acceptCode.getBytes(); // Turning the accept code into byte representation
			byte[] codeHashed = md.digest(codeAsBytes); // Hashing the byte code?
			String codeHashedAsString = Base64.getEncoder().encodeToString(codeHashed); // Turning hashed code to string
			return codeHashedAsString;

		} catch (NoSuchAlgorithmException e) {
			disconnect();
			e.printStackTrace();
		}

		// If we got here, there was a failure. Probably need to throw an Exception
		return acceptCode;
	}

	/**
	 * @param message
	 *            that's pre-formatted in JSON
	 */
	public synchronized void putInPipeAndSmokeIt(String message) {

		try {
			OutputStream pipeOutputStream = Channels.newOutputStream(pipe.sink());
			ObjectOutputStream oos;
			oos = new ObjectOutputStream(pipeOutputStream);
			oos.writeObject(message);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/**
	 * Close out the connection.
	 */
	private void disconnect() {
		if (room != null) {
			room.removeUser(this);
		}

		try {
			socketChannel.close();
		} catch (IOException e) {
			System.out.println("Problem closing the SocketChannel");
			e.printStackTrace();
		}
	}

}

/*
 * WebSocket frame format for reference:
 * 
 * 0 1 2 3 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 * +-+-+-+-+-------+-+-------------+-------------------------------+ |F|R|R|R|
 * opcode|M| Payload len | Extended payload length | |I|S|S|S| (4) |A| (7) |
 * (16/64) | |N|V|V|V| |S| | (if payload len==126/127) | | |1|2|3| |K| | |
 * +-+-+-+-+-------+-+-------------+ - - - - - - - - - - - - - - - + | Extended
 * payload length continued, if payload len == 127 | + - - - - - - - - - - - - -
 * - - +-------------------------------+ | |Masking-key, if MASK set to 1 |
 * +-------------------------------+-------------------------------+ |
 * Masking-key (continued) | Payload Data | +-------------------------------- -
 * - - - - - - - - - - - - - - + : Payload Data continued ... : + - - - - - - -
 * - - - - - - - - - - - - - - - - - - - - - - - - + | Payload Data continued
 * ... | +---------------------------------------------------------------+
 */
