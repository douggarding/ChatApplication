package doug.myfirstapp;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

public class MainActivity extends AppCompatActivity {

    public static final String EXTRA_MESSAGE = "doub.myfirstapp.MESSAGE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    /**
     * Called when the user taps the send button
     */
    public void sendMessage(View view){
        // Intent constructor takes 1) a Context (Activity is a subclass of Context)
        // 2) The class of the app component to which the system should deliver the Intent
        Intent intent = new Intent(this, DisplayMessageActivity.class);
        EditText editText = (EditText) findViewById(R.id.editText);
        String message = editText.getText().toString();


        intent.putExtra(EXTRA_MESSAGE, message);
        startActivity(intent);
    }
}
