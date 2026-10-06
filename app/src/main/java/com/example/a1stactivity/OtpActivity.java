package com.example.a1stactivity; // keep YOUR project's package line if it's different

import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
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

// The OTP screen. The user is sent here after a successful login.
public class OtpActivity extends AppCompatActivity {

    // Your Render server link (without a route at the end)
    private static final String BASE_URL = "https://android-studio-activity-2.onrender.com";

    // These are "fields" (variables of the whole class) because
    // more than one method below needs to use them.
    private String username;
    private TextView tvDemoCode;
    private TextView tvTimer;
    private TextView tvResend;
    private EditText etOtp;
    private CountDownTimer countDownTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the design from res/layout/activity_otp.xml
        setContentView(R.layout.activity_otp);

        // Get the views from the XML
        ImageButton btnBack = findViewById(R.id.btnBack);
        Button btnVerify = findViewById(R.id.btnVerify);
        tvDemoCode = findViewById(R.id.tvDemoCode);
        tvTimer = findViewById(R.id.tvTimer);
        tvResend = findViewById(R.id.tvResend);
        etOtp = findViewById(R.id.etOtp);

        // Get the data that MainActivity attached with putExtra()
        username = getIntent().getStringExtra("username");
        String firstCode = getIntent().getStringExtra("otp");

        // Show the first code and start the 30-second countdown
        showNewCode(firstCode);

        // Back arrow: close this screen and return to the login screen
        btnBack.setOnClickListener(v -> finish());

        // ===== VERIFY BUTTON =====
        btnVerify.setOnClickListener(v -> {
            String typedCode = etOtp.getText().toString().trim();

            // Quick check in the app: the code must be exactly 4 digits
            if (typedCode.length() != 4) {
                Toast.makeText(this, R.string.error_otp_length, Toast.LENGTH_SHORT).show();
                return;
            }

            btnVerify.setEnabled(false);
            btnVerify.setText(R.string.verifying);

            // Ask the server if the code is correct (on a background thread)
            new Thread(() -> {
                boolean success = false;
                String message;

                try {
                    // Build the JSON: { "username": "...", "otp": "1234" }
                    JSONObject requestBody = new JSONObject();
                    requestBody.put("username", username);
                    requestBody.put("otp", typedCode);

                    JSONObject response = sendPostRequest(BASE_URL + "/verify-otp", requestBody);
                    success = response.getBoolean("success");
                    message = response.getString("message");

                } catch (Exception e) {
                    message = getString(R.string.error_connection);
                }

                boolean finalSuccess = success;
                String finalMessage = message;

                // Back to the main thread to update the screen
                runOnUiThread(() -> {
                    btnVerify.setEnabled(true);
                    btnVerify.setText(R.string.btn_verify);
                    Toast.makeText(OtpActivity.this, finalMessage, Toast.LENGTH_LONG).show();

                    if (finalSuccess) {
                        // Stop the countdown and show that the account is verified
                        countDownTimer.cancel();
                        tvTimer.setText(R.string.verified);
                        // NEXT STEP (optional): open a home/welcome screen here
                    }
                });
            }).start();
        });

        // ===== RESEND TEXT =====
        tvResend.setOnClickListener(v -> {
            tvResend.setEnabled(false);

            // Ask the server for a brand new code (on a background thread)
            new Thread(() -> {
                boolean success = false;
                String message;
                String newCode = "";

                try {
                    // Build the JSON: { "username": "..." }
                    JSONObject requestBody = new JSONObject();
                    requestBody.put("username", username);

                    JSONObject response = sendPostRequest(BASE_URL + "/resend-otp", requestBody);
                    success = response.getBoolean("success");
                    message = response.getString("message");
                    newCode = response.optString("otp", "");

                } catch (Exception e) {
                    message = getString(R.string.error_connection);
                }

                boolean finalSuccess = success;
                String finalMessage = message;
                String finalNewCode = newCode;

                runOnUiThread(() -> {
                    Toast.makeText(OtpActivity.this, finalMessage, Toast.LENGTH_SHORT).show();

                    if (finalSuccess) {
                        // Show the new code and restart the 30-second countdown
                        showNewCode(finalNewCode);
                    } else {
                        // Let the user try again
                        tvResend.setEnabled(true);
                        tvResend.setAlpha(1f);
                    }
                });
            }).start();
        });
    }

    // -----------------------------------------------------------------
    //  Shows a code on the screen and starts a 30-second countdown.
    //  Used when the screen opens AND every time Resend is tapped.
    // -----------------------------------------------------------------
    private void showNewCode(String code) {
        // Show the code in the grey box (pretend text message)
        tvDemoCode.setText(getString(R.string.demo_code, code));

        // Clear anything typed before
        etOtp.setText("");

        // Resend is disabled (and faded) until the code expires
        tvResend.setEnabled(false);
        tvResend.setAlpha(0.4f);

        // Stop the old countdown if there is one
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        // CountDownTimer(total time, tick every 1 second), both in milliseconds.
        // It works like a javax.swing.Timer that stops by itself after 30 seconds.
        countDownTimer = new CountDownTimer(30000, 1000) {

            // Runs every second
            @Override
            public void onTick(long millisUntilFinished) {
                // Change milliseconds to seconds (rounded up, so it starts at 30)
                long secondsLeft = (millisUntilFinished + 999) / 1000;
                tvTimer.setText(getString(R.string.timer_expires, secondsLeft));
            }

            // Runs once when the 30 seconds are over
            @Override
            public void onFinish() {
                tvTimer.setText(R.string.timer_expired);
                tvResend.setEnabled(true);
                tvResend.setAlpha(1f);
            }
        };
        countDownTimer.start();
    }

    // -----------------------------------------------------------------
    //  Sends JSON to the server with POST and returns the server's reply.
    //  Same steps as the login code in MainActivity, put in one method
    //  because Verify and Resend both need it.
    // -----------------------------------------------------------------
    private JSONObject sendPostRequest(String urlText, JSONObject requestBody) throws Exception {
        // Open the connection
        URL url = new URL(urlText);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);
        connection.setConnectTimeout(60000);
        connection.setReadTimeout(60000);

        // Send the JSON
        OutputStream outputStream = connection.getOutputStream();
        outputStream.write(requestBody.toString().getBytes(StandardCharsets.UTF_8));
        outputStream.close();

        // Successful replies come from getInputStream(), error replies from getErrorStream()
        int statusCode = connection.getResponseCode();
        InputStream inputStream;
        if (statusCode >= 200 && statusCode < 300) {
            inputStream = connection.getInputStream();
        } else {
            inputStream = connection.getErrorStream();
        }

        // Read the reply into one String
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        StringBuilder responseText = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            responseText.append(line);
        }
        reader.close();
        connection.disconnect();

        // Turn the String into a JSON object
        return new JSONObject(responseText.toString());
    }

    // Stop the countdown when the screen closes, so it doesn't keep running
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}