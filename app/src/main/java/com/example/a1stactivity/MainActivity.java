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

    // The address of the /login route on your Render server
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

            // 4. Internet requests must run on a background thread,
            //    otherwise the screen would freeze while waiting.
            new Thread(() -> {
                boolean success = false;
                String message;
                String serverUsername = "";
                String otp = "";

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

                    // 4d. Successful replies come from getInputStream(), error replies from getErrorStream()
                    int statusCode = connection.getResponseCode();
                    InputStream inputStream;
                    if (statusCode >= 200 && statusCode < 300) {
                        inputStream = connection.getInputStream();
                    } else {
                        inputStream = connection.getErrorStream();
                    }

                    // 4e. Read the server's reply into one String
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(inputStream, StandardCharsets.UTF_8));
                    StringBuilder responseText = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        responseText.append(line);
                    }
                    reader.close();
                    connection.disconnect();

                    // 4f. Read the values from the JSON reply.
                    //     optString() returns "" if the value isn't there (for example, on a failed login)
                    JSONObject response = new JSONObject(responseText.toString());
                    success = response.getBoolean("success");
                    message = response.getString("message");
                    serverUsername = response.optString("username", "");
                    otp = response.optString("otp", "");

                } catch (Exception e) {
                    // No internet, wrong link, server down, etc.
                    message = getString(R.string.error_connection);
                }

                // 5. Values used inside runOnUiThread must not change, so we copy them
                boolean finalSuccess = success;
                String finalMessage = message;
                String finalUsername = serverUsername;
                String finalOtp = otp;

                // 6. Only the main thread may change the screen, so we switch back to it
                runOnUiThread(() -> {
                    btnLogin.setEnabled(true);
                    btnLogin.setText(R.string.btn_sign_in);

                    Toast.makeText(MainActivity.this, finalMessage, Toast.LENGTH_SHORT).show();

                    if (finalSuccess) {
                        // Clear the password so it isn't still there when the user comes back
                        etPassword.setText("");

                        // 7. Open the OTP screen and pass along the username and the code.
                        //    putExtra() attaches data to the Intent, like passing
                        //    arguments to a constructor when opening a new JFrame.
                        Intent intent = new Intent(MainActivity.this, OtpActivity.class);
                        intent.putExtra("username", finalUsername);
                        intent.putExtra("otp", finalOtp);
                        startActivity(intent);
                    }
                });
            }).start();
        });
    }
}