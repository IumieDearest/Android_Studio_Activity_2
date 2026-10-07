package com.example.a1stactivity; // keep YOUR project's package line if it's different

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
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

// The SIGN UP screen.
// When the user signs up, the details are:
//   1. sent to the Render server (/register), and
//   2. saved on the phone with SharedPreferences.
public class SignUpActivity extends AppCompatActivity {

    // The address of the /register route on your Render server
    private static final String REGISTER_URL = "https://android-studio-activity-2.onrender.com/register";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the design from res/layout/activity_sign_up.xml
        setContentView(R.layout.activity_sign_up);

        // Get the views from the XML using their ids
        EditText etFirstName = findViewById(R.id.etFirstName);
        EditText etLastName = findViewById(R.id.etLastName);
        EditText etEmail = findViewById(R.id.etEmail);
        EditText etConfirmEmail = findViewById(R.id.etConfirmEmail);
        EditText etPassword = findViewById(R.id.etPassword);
        EditText etConfirmPassword = findViewById(R.id.etConfirmPassword);
        CheckBox cbTerms = findViewById(R.id.cbTerms);
        Button btnSignUp = findViewById(R.id.btnSignUp);
        ImageButton btnBack = findViewById(R.id.btnBack);
        TextView tvGoToLogin = findViewById(R.id.tvGoToLogin);

        // The back arrow and the "Sign In" text both close this screen,
        // which brings the user back to the Login screen underneath it.
        btnBack.setOnClickListener(v -> finish());
        tvGoToLogin.setOnClickListener(v -> finish());

        // ===== SIGN UP BUTTON =====
        btnSignUp.setOnClickListener(v -> {

            // 1. Read what the user typed
            String firstName = etFirstName.getText().toString().trim();
            String lastName = etLastName.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String confirmEmail = etConfirmEmail.getText().toString().trim();
            String password = etPassword.getText().toString();
            String confirmPassword = etConfirmPassword.getText().toString();

            // 2. Quick checks in the app first, so we don't wait for the server for nothing.
            //    (The server also checks the emails, password length, etc.)
            if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty()
                    || confirmEmail.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, R.string.error_fill_all, Toast.LENGTH_SHORT).show();
                return;
            }

            if (!cbTerms.isChecked()) {
                Toast.makeText(this, R.string.error_terms, Toast.LENGTH_SHORT).show();
                return;
            }

            // 3. Disable the button while waiting, so the user can't tap it many times
            btnSignUp.setEnabled(false);
            btnSignUp.setText(R.string.creating_account);

            // 4. Internet requests must run on a background thread,
            //    otherwise the screen would freeze while waiting.
            new Thread(() -> {
                boolean success = false;
                String message;

                try {
                    // 4a. Open a connection to the server
                    URL url = new URL(REGISTER_URL);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("POST");                              // we are SENDING data
                    connection.setRequestProperty("Content-Type", "application/json"); // the data is JSON
                    connection.setDoOutput(true);                                      // allow sending a body
                    connection.setConnectTimeout(60000); // wait up to 60 seconds, because a sleeping
                    connection.setReadTimeout(60000);    // Render server takes time to wake up

                    // 4b. Build the JSON with all the sign up fields
                    JSONObject requestBody = new JSONObject();
                    requestBody.put("firstName", firstName);
                    requestBody.put("lastName", lastName);
                    requestBody.put("email", email);
                    requestBody.put("confirmEmail", confirmEmail);
                    requestBody.put("password", password);
                    requestBody.put("confirmPassword", confirmPassword);

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

                    // 4f. Read "success" and "message" from the JSON reply
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
                    btnSignUp.setEnabled(true);
                    btnSignUp.setText(R.string.btn_sign_up);

                    // Show the server's message (success or error)
                    Toast.makeText(SignUpActivity.this, finalMessage, Toast.LENGTH_LONG).show();

                    if (finalSuccess) {
                        // 7. The server accepted the account, so ALSO save it on the phone.
                        //    SharedPreferences is a small key-value storage that stays
                        //    on the phone even after the app is closed.
                        //    "UserPrefs" is the name of our storage file.
                        //    MODE_PRIVATE means only this app can read it.
                        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);

                        // An Editor is used to make changes, like putting values in a HashMap
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putString("firstName", firstName);
                        editor.putString("lastName", lastName);
                        editor.putString("email", email);

                        // apply() saves the changes
                        editor.apply();

                        // 8. Go back to the Login screen
                        finish();
                    }
                });
            }).start();
        });
    }
}