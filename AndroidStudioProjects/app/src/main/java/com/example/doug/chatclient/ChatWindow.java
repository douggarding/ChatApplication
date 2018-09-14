package com.example.doug.chatclient;

import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.google.gson.Gson;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.WebSocket;

import java.util.ArrayList;

public class ChatWindow extends AppCompatActivity {

    // Member variables
    private String roomName;
    private String userName;
    private WebSocket webSocket;
    private ListView messageWindow;
    private ArrayList<String> messageList;
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_window);

        // Initialize everything for displaying the messages
        messageList = new ArrayList<String>();
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, messageList);
        messageWindow = (ListView) findViewById(R.id.MessageList);
        messageWindow.setAdapter(adapter);

        // Get the intent that started this activity, and get the room name
        Intent intent = getIntent();
        roomName = intent.getStringExtra(MainActivity.EXTRA_ROOM);
        userName = intent.getStringExtra(MainActivity.EXTRA_USER);

        // Set the title at the top of the app
        setTitle("Chat Room: " + roomName);


        // Create a websocket (Code from AndroidAsync) 10.0.2.2 is the address of the host machine running the Android emulator
        AsyncHttpClient.getDefaultInstance().websocket("ws://10.0.2.2:8080", "my-protocol", new AsyncHttpClient.WebSocketConnectCallback() {

            @Override
            public void onCompleted(Exception ex, WebSocket socket) {
                ChatWindow.this.webSocket = socket;

                if (ex != null) {
                    ex.printStackTrace();
                    return;
                }

                // Connect to the room
                Message joinRequest = new Message("join", roomName);
                webSocket.send(joinRequest.toJSON());

                // Set callback for when a String is received
                webSocket.setStringCallback(new WebSocket.StringCallback() {
                    public void onStringAvailable(String s) {

                        // Deserialize message
                        Gson gson = new Gson();
                        Message message = gson.fromJson(s, Message.class);

                        System.out.println("Message recieved: " + message.fromJSON());
                        messageList.add(message.fromJSON());
                        messageReceived();
                    }
                });

            }
        });

    }

    /**
     * What to do when a message is recieved from the server/WebSocket
     */
    public void messageReceived(){

        Handler h = new Handler(this.getMainLooper());
        h.post(new Runnable() {

            public void run(){
                adapter.notifyDataSetChanged();
            }

        });
    }


    /**
     * Sends a message to the server when the "Send" button is clicked.
     */
    public void sendMessage(View view){

        String textMessage = ((EditText) findViewById(R.id.MessageText)).getText().toString();

        Message message = new Message(userName, textMessage);

        // Clear the text field in the app
        ((EditText) findViewById(R.id.MessageText)).setText("");

        webSocket.send(message.toJSON());
    }


}
