package com.example.doug.chatclient;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

public class MainActivity extends AppCompatActivity {

    // The key value for the putExtra() method. An intent can carry data types
    // as key-value pairs called extras. The key is a public constant
    public static final String EXTRA_ROOM = "room";
    public static final String EXTRA_USER = "username";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    /**
     * This is what happens when the "Login" button is pressed
     */
    public void login(View view){
        Intent intent = new Intent(this, ChatWindow.class);

        // Grab the text for the username
        EditText usernameEditText = (EditText) findViewById(R.id.editTextUser);
        String username = usernameEditText.getText().toString();
        // Grab the text for the chat room name
        EditText roomEditText = (EditText) findViewById(R.id.editTextRoom);
        String roomName = roomEditText.getText().toString();

        // Carries the value to the next Activity (the ChatWindow in this case)
        intent.putExtra(EXTRA_ROOM, roomName);
        intent.putExtra(EXTRA_USER, username);

        // Starts an instance of the ChatWindow class.
        startActivity(intent);
    }
}
