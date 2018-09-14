package com.example.doug.chatclient;

import com.google.gson.Gson;

/**
 * Created by doug on 11/2/17.
 */

public class Message {

    // Member Variables
    private String user;
    private String message;

    /**
     * Constructs a message when provided a user and their message.
     *
     * @param u - Name of the user
     * @param m - User's message, or name of room if join request
     */
    public Message(String u, String m) {
            user = u;
            message = m;
        }

    /**
     * Constructs a message when provided a raw string message as it's received from the
     * client. The format is that the first word of the String is the users name, and the
     * rest of the string is the message itself.
     *
     * @param clientString
     */
    public Message(String clientString) {
        user = clientString.substring(0, clientString.indexOf(" "));
        message = clientString.substring(clientString.indexOf(" ") + 1);
    }

    /**
     * @return Returns the JSON version of this Message. (Serializes message)
     */
    public String toJSON() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }


    public String fromJSON() {
        return user + ": " + message;
    }



}