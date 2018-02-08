package server;

import java.util.HashMap;
import java.util.Scanner;

public class HTTPRequest {

	// Member variables
	private HashMap<String, String> header;
	private String fileName;

	// Constructor
	public HTTPRequest(Scanner ClientScanner) throws BadRequestException {
		fileName = buildFileRequest(ClientScanner);
		header = buildHeader(ClientScanner);
	}

	
	/**
	 * Helper method for the constructor that returns the file requested in the GET request.
	 */
	private String buildFileRequest(Scanner clientScanner) throws BadRequestException {
		// Stores the first line of the header
		String firstLine;

		// Get the first line of the client input stream, which should be a GET request
		firstLine = clientScanner.nextLine();

		// Split the line into tokens for later
		String[] getRequest = firstLine.split("\\s+"); // divides string by all spaces
		// Make sure this is a valid GET request and ends with "HTTP/1.1"
		if (!getRequest[0].equals("GET") || !getRequest[2].equals("HTTP/1.1")) {
			throw new BadRequestException("Invalid client request.");
		}
		
		// construct path to file requested by the client
		if (getRequest[1].equals("/")) {
			getRequest[1] = "/index.html";
		}

		return "resources" + getRequest[1];
	}

	
	/**
	 * Helper method that stores all the header elements in a hash map.
	 */
	private HashMap<String, String> buildHeader(Scanner clientScanner) throws BadRequestException {
		HashMap<String, String> header = new HashMap<String, String>();
		
		// Stores each line of the header
		String currentInputLine;

		// Grab all the stuff from the rest of the header
		currentInputLine = clientScanner.nextLine();
		while (currentInputLine.length() > 0) {
			
			// Add all the pieces of the header to the map
			String[] headerPiece = currentInputLine.split(": ");
			//System.out.println(headerPiece[0] + ": " + headerPiece[1]);
			header.put(headerPiece[0], headerPiece[1]);
			
			// advance to the next item in the header
			currentInputLine = clientScanner.nextLine();
		}
		//System.out.println("\n");
		return header;
	}

	
	public String getRequestedFileName() {
		return fileName;
	}


	public boolean hasWebSocketKey() {
		return this.header.containsKey("Sec-WebSocket-Key");
	}


	public String getWebSocketKey() {
		return this.header.get("Sec-WebSocket-Key");
	}
	
}
