package com.example.a1stactivity; // keep YOUR project's package line if it's different

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

// The LOGIN screen.
public class MainActivity extends AppCompatActivity {

    // The address of the /login route on your Render server.
    // CHANGE THIS to your own Render link, and keep "/login" at the end.
    private static final String LOGIN_URL = "https://android-studio-activity-2.onrender.com/login";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the design from res/layout/activity_main.xml
        setContentView(R.layout.activity_main);

        // Get the views from the XML using their ids
        EditText etUsername = findViewById(R.id.etUsername);
        EditText etPassword = findViewById(R.id.etPassword);
        Button btnLogin = findViewById(R.id.btnLogin);
        TextView tvGoToSignUp = findViewById(R.id.tvGoToSignUp);

        // "Sign Up" text at the bottom: opens the Sign Up screen
        tvGoToSignUp.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SignUpActivity.class);
            startActivity(intent);
        });

        // "Sign In" button: sends the username and password to the Render server
        btnLogin.setOnClickListener(v -> {

            // 1. Read what the user typed
            String username = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString();

            // 2. Quick check in the app first, so we don't wait for the server for nothing
            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, R.string.error_empty_fields, Toast.LENGTH_SHORT).show();
                return;
            }

            // 3. Disable the button while waiting, so the user can't tap it many times
            btnLogin.setEnabled(false);
            btnLogin.setText(R.string.signing_in);

            // 4. Android does not allow internet requests on the main (screen) thread,
            //    because the app would freeze while waiting. So we use a new Thread,
            //    just like running a background task in Swing.
            new Thread(() -> {
                boolean success = false;
                String message;

                try {
                    // 4a. Open a connection to the server
                    URL url = new URL(LOGIN_URL);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("POST");                              // we are SENDING data
                    connection.setRequestProperty("Content-Type", "application/json"); // the data is JSON
                    connection.setDoOutput(true);                                      // allow sending a body
                    connection.setConnectTimeout(60000); // wait up to 60 seconds, because a sleeping
                    connection.setReadTimeout(60000);    // Render server takes time to wake up

                    // 4b. Build the JSON: { "username": "...", "password": "..." }
                    JSONObject requestBody = new JSONObject();
                    requestBody.put("username", username);
                    requestBody.put("password", password);

                    // 4c. Send the JSON to the server
                    OutputStream outputStream = connection.getOutputStream();
                    outputStream.write(requestBody.toString().getBytes(StandardCharsets.UTF_8));
                    outputStream.close();

                    // 4d. Get the status code (200 = OK, 400/401 = error from our server)
                    int statusCode = connection.getResponseCode();

                    // Successful replies come from getInputStream(), error replies from getErrorStream()
                    InputStream inputStream;
                    if (statusCode >= 200 && statusCode < 300) {
                        inputStream = connection.getInputStream();
                    } else {
                        inputStream = connection.getErrorStream();
                    }

                    // 4e. Read the server's reply line by line into one String
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(inputStream, StandardCharsets.UTF_8));
                    StringBuilder responseText = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        responseText.append(line);
                    }
                    reader.close();
                    connection.disconnect();

                    // 4f. Turn the reply into JSON and read "success" and "message"
                    //     Example reply: { "success": true, "message": "Login successful!..." }
                    JSONObject response = new JSONObject(responseText.toString());
                    success = response.getBoolean("success");
                    message = response.getString("message");

                } catch (Exception e) {
                    // No internet, wrong link, server down, etc.
                    message = getString(R.string.error_connection);
                }

                // 5. Values used inside runOnUiThread must not change, so we copy them
                boolean finalSuccess = success;
                String finalMessage = message;

                // 6. Only the main thread may change the screen, so we switch back to it
                runOnUiThread(() -> {
                    // Turn the button back on
                    btnLogin.setEnabled(true);
                    btnLogin.setText(R.string.btn_sign_in);

                    // Show the server's message (success or error)
                    Toast.makeText(MainActivity.this, finalMessage, Toast.LENGTH_LONG).show();

                    if (finalSuccess) {
                        // NEXT STEP: open the OTP screen here
                    }
                });
            }).start();
        });
    }
}