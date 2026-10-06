package com.example.a1stactivity; // keep YOUR project's package line if it's different

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

// The SIGN UP screen.
public class SignUpActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the design from res/layout/activity_sign_up.xml
        setContentView(R.layout.activity_sign_up);

        // The back arrow at the top and the "Sign In" text at the bottom
        ImageButton btnBack = findViewById(R.id.btnBack);
        TextView tvGoToLogin = findViewById(R.id.tvGoToLogin);

        // Both of them close this screen.
        // finish() closes the current screen, which brings the
        // user back to the Login screen underneath it.
        btnBack.setOnClickListener(v -> finish());
        tvGoToLogin.setOnClickListener(v -> finish());

        // The Sign Up button has no action yet.
    }
}