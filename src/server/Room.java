package server;

import java.util.ArrayList;

public class Room {
	
	// Member Variables
	ArrayList<UserConnection> users;
	ArrayList<Message> messages;
	
	
	/**
	 * Constructs a Room object. Has a list of all users connected to the room,
	 * and keeps track of all messages sent within this room.
	 */
	public Room() {
		users = new ArrayList<UserConnection>();
		messages = new ArrayList<Message>();
	}

	
	/**
	 * Broadcasts a message to all connected users. Also stores the message
	 * in the Rooms log of previous messages.
	 * 
	 * @param message - Message to be sent
	 */
	public synchronized void addMessage(Message message) {	
		messages.add(message);
	
		String jMessage = message.toJSON();		
		for(UserConnection user : users) {
			user.putInPipeAndSmokeIt(jMessage);
		}
	}

	
	/**
	 * Adds a new user to the room and sends them all previous messages.
	 * 
	 * @param user - UserConnection of user added to room
	 */
	public synchronized void addUser(UserConnection user) {
		users.add(user);
		// Send user all old messages
		for(Message message : messages) {
			user.sendMessage(message.toJSON());
		}
	}

	
	/**
	 * Removes 
	 * 
	 * @param userConnection
	 */
	public synchronized void removeUser(UserConnection userConnection) {
		users.remove(userConnection);
	}
	
}
