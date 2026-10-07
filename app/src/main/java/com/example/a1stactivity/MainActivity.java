package com.example.a1stactivity; // keep YOUR project's package line if it's different

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import android.content.SharedPreferences;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class MainActivity extends AppCompatActivity {
    private static final String LOGIN_URL = "https://android-studio-activity-2.onrender.com/login";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        EditText etUsername = findViewById(R.id.etUsername);
        EditText etPassword = findViewById(R.id.etPassword);
        Button btnLogin = findViewById(R.id.btnLogin);
        TextView tvGoToSignUp = findViewById(R.id.tvGoToSignUp);

        tvGoToSignUp.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SignUpActivity.class);
            startActivity(intent);
        });

        btnLogin.setOnClickListener(v -> {

            String username = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString();

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, R.string.error_empty_fields, Toast.LENGTH_SHORT).show();
                return;
            }

            btnLogin.setEnabled(false);
            btnLogin.setText(R.string.signing_in);

            new Thread(() -> {
                boolean success = false;
                String message;
                String serverUsername = "";
                String otp = "";

                try {
                    URL url = new URL(LOGIN_URL);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("POST");                              // we are SENDING data
                    connection.setRequestProperty("Content-Type", "application/json"); // the data is JSON
                    connection.setDoOutput(true);                                      // allow sending a body
                    connection.setConnectTimeout(60000); // wait up to 60 seconds, because a sleeping
                    connection.setReadTimeout(60000);    // Render server takes time to wake up

                    JSONObject requestBody = new JSONObject();
                    requestBody.put("username", username);
                    requestBody.put("password", password);

                    OutputStream outputStream = connection.getOutputStream();
                    outputStream.write(requestBody.toString().getBytes(StandardCharsets.UTF_8));
                    outputStream.close();

                    int statusCode = connection.getResponseCode();
                    InputStream inputStream;
                    if (statusCode >= 200 && statusCode < 300) {
                        inputStream = connection.getInputStream();
                    } else {
                        inputStream = connection.getErrorStream();
                    }

                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(inputStream, StandardCharsets.UTF_8));
                    StringBuilder responseText = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        responseText.append(line);
                    }
                    reader.close();
                    connection.disconnect();

                    JSONObject response = new JSONObject(responseText.toString());
                    success = response.getBoolean("success");
                    message = response.getString("message");
                    serverUsername = response.optString("username", "");
                    otp = response.optString("otp", "");

                } catch (Exception e) {
                    message = getString(R.string.error_connection);
                }

                boolean finalSuccess = success;
                String finalMessage = message;
                String finalUsername = serverUsername;
                String finalOtp = otp;

                runOnUiThread(() -> {
                    btnLogin.setEnabled(true);
                    btnLogin.setText(R.string.btn_sign_in);

                    Toast.makeText(MainActivity.this, finalMessage, Toast.LENGTH_SHORT).show();

                    if (finalSuccess) {
                        etPassword.setText("");

                        Intent intent = new Intent(MainActivity.this, OtpActivity.class);
                        intent.putExtra("username", finalUsername);
                        intent.putExtra("otp", finalOtp);
                        startActivity(intent);
                    }
                });
            }).start();
        });
    }
    @Override
    protected void onResume() {
        super.onResume();

        EditText etUsername = findViewById(R.id.etUsername);

        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String savedEmail = prefs.getString("email", "");

        if (etUsername.getText().toString().isEmpty()) {
            etUsername.setText(savedEmail);
        }
    }
}