package com.example.a1stactivity; // keep YOUR project's package line if it's different

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

// The LOGIN screen.
// An "Activity" is one screen of the app, a bit like a JFrame in Swing.
public class MainActivity extends AppCompatActivity {

    // onCreate() runs automatically when this screen opens.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the design from res/layout/activity_main.xml
        setContentView(R.layout.activity_main);

        // The "Sign Up" text at the bottom of the screen
        TextView tvGoToSignUp = findViewById(R.id.tvGoToSignUp);

        // When it's clicked, open the Sign Up screen.
        // An Intent is a "request" to open another screen.
        tvGoToSignUp.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SignUpActivity.class);
            startActivity(intent);
        });

        // The Sign In button will be connected to the API in the next step.
    }
}