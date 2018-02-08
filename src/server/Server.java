package server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Set;
import java.io.InputStream;
import java.io.OutputStream;

public class Server {

	// Member variables
	private ServerSocketChannel serverSocketChannel;
	private Selector selector;
	private HashMap<String, Room> rooms;
	

	// Constructs a new server
	public Server(int port) {
		try {
			// Perhaps not necessary anymore?
			//serverSocket = new ServerSocket(port);
			
			rooms = new HashMap<String, Room>();
			
			// Not sure if this is correct for ServerSocketChannel????
			serverSocketChannel = ServerSocketChannel.open();
			serverSocketChannel.bind(new InetSocketAddress(port));
			serverSocketChannel.configureBlocking(false);
			
			// Set up the selector
			selector = Selector.open();
			serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
			
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Couldn't listen on the specified port. Port is probably already be in use.");
		}
	}

	// Main method to run the server
	public static void main(String[] args) {
		Server server = new Server(8080);
		server.runServer();
	}

	// Run the server
	public void runServer() {

		// Loop that continually listens for an incoming socket for a client
		while (true) {
			
			try {
				// Blocks, waiting for one of its channels' events to occur?
				selector.select();
				
				// Get all the selector keys from the selector
				Set<SelectionKey> keys =  selector.selectedKeys();
				// Get an iterator for the set
				Iterator<SelectionKey> iter = keys.iterator();
				
				// If the selector has keys, it means an event has happened.
				// Right here we hope that OP_ACCEPT key has been meet
				while (iter.hasNext()) {
					SelectionKey key = iter.next();
					if(key.isAcceptable()) {
						iter.remove();
						// Returns a socket channel from the server socket channel
						SocketChannel clientSocketChannel = serverSocketChannel.accept();
						Thread processSocket = new Thread(new ProcessNewSocket(this, clientSocketChannel));
						processSocket.start();
					}
				}

			} catch (IOException e) {
				e.printStackTrace();
			}

		} // end while loop
	}


	/**
	 * Helper class that implements Runnable so that it can be run as its own
	 * thread. Handles processing a new client that has connected to the server.
	 * Reads their request and returns requested files.
	 */
	private class ProcessNewSocket implements Runnable {

		private SocketChannel clientSocket;
		private Server server;

		public ProcessNewSocket(Server s, SocketChannel sc) {
			clientSocket = sc;
			server = s;
		}

		@Override
		public void run() {

			try {
				InputStream clientInput = clientSocket.socket().getInputStream();
				OutputStream clientOutput = clientSocket.socket().getOutputStream();
				// Scanner to look through clientInput
				Scanner clientScanner = new Scanner(clientInput);

				// Read request to see what file to send and collect request header info
				HTTPRequest request = new HTTPRequest(clientScanner);

				// Begin a WebSocket if requested by client
				if (request.hasWebSocketKey()) {
					UserConnection user = new UserConnection(server, clientSocket);
					user.acceptConnection(clientOutput, request);
					user.run();
				}
				// Send the requested file to the client
				else {
					HTTPResponse.sendRequestedFile(request, clientOutput);
				}
				
				// Close out client socket and streams
				clientScanner.close();
				clientSocket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (BadRequestException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
	}
	
	
	public boolean roomExists(String roomName) {
		return rooms.containsKey(roomName);
	}
	
	
	public Room joinRoom(String roomName, UserConnection user) {
		// Room to be returned
		Room room;
		
		// If the room exists, join it
		if(rooms.containsKey(roomName)) {
			room = rooms.get(roomName);
			room.addUser(user);
		}
		// If room doesn't exist, create and join it
		else {
			room = new Room();
			rooms.put(roomName, room);
			room.addUser(user);
		}
		
		return room;
	}

	
}
