package server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Base64.Decoder;

import javax.xml.bind.DatatypeConverter;

public class HTTPResponse {

	public static void sendRequestedFile(HTTPRequest clientRequest, OutputStream oByteStream)
			throws BadRequestException {

		try {
			// Open up a File and FileInputStream from the provided file name
			File file = new File(clientRequest.getRequestedFileName());
			FileInputStream fileInput = new FileInputStream(file);

			// Creates and sends the header information
			sendHeader(200, file, oByteStream, clientRequest);
			sendFile(fileInput, oByteStream);
			fileInput.close();
		}
		// If the file wasn't found, return a 404 Error
		catch (FileNotFoundException e) {
			File errorFile = new File("resources/404.html");
			try {
				FileInputStream fInput = new FileInputStream(errorFile);
				sendHeader(404, errorFile, oByteStream, clientRequest);
				sendFile(fInput, oByteStream);
			}
			// Couldn't find the 404 file, so catch THAT exception
			catch (FileNotFoundException e1) {
				e1.printStackTrace();
				throw new BadRequestException();
			} catch (IOException e1) {
				e1.printStackTrace();
				throw new BadRequestException();
			}

		} catch (IOException e) {
			e.printStackTrace();
			throw new BadRequestException();
		}

	}

	/**
	 * Sends the HTTP header information via an OutputStream
	 * 
	 * @param code
	 *            - The HTTP response status code (200 - request succeeded, 301 -
	 *            moved permanently, 404 - requested file was not found)
	 * @param file
	 *            - File that's to be returned to the client
	 * @param stream
	 *            - stream through which the response will be sent.
	 */
	static void sendHeader(int code, File file, OutputStream stream, HTTPRequest req) {
		// Output stream that writes Strings to the stream going to the client
		PrintWriter oStringStream = new PrintWriter(stream);

		// If the requested file is being sent
		if (code == 200) {
			oStringStream.print("HTTP/1.1 " + code + " OK" + "\r\n");
		}
		// If the requested file was not found
		else {
			oStringStream.print("HTTP/1.1 " + code + " File Not Found" + "\r\n");
		}
		oStringStream.print("Content-Length: " + file.length() + "\r\n");

		oStringStream.print("\r\n");
		oStringStream.flush();
	}

	/**
	 * Sends a file to the client via an OutputStream
	 * 
	 * @param file
	 *            - File to be returned to the client
	 * @param stream
	 *            - stream through which the file will be sent
	 * @throws IOException
	 */
	private static void sendFile(FileInputStream file, OutputStream stream) throws IOException {
		// Create a buffer of bytes to store file data
		byte[] buffer = new byte[1024];
		int bufferSize = 0;

		// transfer data from file to the client
		bufferSize = file.read(buffer);
		while (bufferSize > 0) {
			stream.write(buffer, 0, bufferSize);

			/*
			 * Code to artificially slowly load an image slowly to clients stream.flush();
			 * try { Thread.sleep(10); } catch (InterruptedException e) { // TODO
			 * Auto-generated catch block e.printStackTrace(); }
			 */

			bufferSize = file.read(buffer);
		}
	}
}