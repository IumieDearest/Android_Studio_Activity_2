package com.example.a1stactivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class UsersActivity extends AppCompatActivity {

    private static final String USERS_URL = "https://android-studio-activity-2.onrender.com/users";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_users);

        TextView tvUserCount = findViewById(R.id.tvUserCount);
        TableLayout tableUsers = findViewById(R.id.tableUsers);
        Button btnLogOut = findViewById(R.id.btnLogOut);

        btnLogOut.setOnClickListener(v -> finish());

        tvUserCount.setText(R.string.loading_users);

        new Thread(() -> {
            JSONArray users = null; // stays null if something goes wrong

            try {
                URL url = new URL(USERS_URL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(60000); // wait up to 60 seconds,
                connection.setReadTimeout(60000);    // in case Render is waking up

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder responseText = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    responseText.append(line);
                }
                reader.close();
                connection.disconnect();

                JSONObject response = new JSONObject(responseText.toString());
                users = response.getJSONArray("users");

            } catch (Exception e) {
                // No internet, server down, etc. "users" stays null.
            }

            JSONArray finalUsers = users;

            runOnUiThread(() -> {

                if (finalUsers == null) {
                    tvUserCount.setText(R.string.error_connection);
                    return;
                }

                tvUserCount.setText(getString(R.string.user_count, finalUsers.length()));

                float density = getResources().getDisplayMetrics().density;
                int padding = (int) (12 * density);

                for (int i = 0; i < finalUsers.length(); i++) {
                    JSONObject user = finalUsers.optJSONObject(i);

                    String number = String.valueOf(i + 1); // 1, 2, 3... instead of 0, 1, 2...
                    String fullName = user.optString("firstName") + " " + user.optString("lastName");
                    String email = user.optString("email");

                    View divider = new View(this);
                    divider.setBackgroundColor(ContextCompat.getColor(this, R.color.divider));
                    tableUsers.addView(divider, new TableLayout.LayoutParams(
                            TableLayout.LayoutParams.MATCH_PARENT, 1));

                    TableRow row = new TableRow(this);

                    String[] cells = {number, fullName, email};
                    for (String cellText : cells) {
                        TextView cell = new TextView(this);
                        cell.setText(cellText);
                        cell.setPadding(padding, padding, padding, padding);
                        cell.setTextSize(13); // in sp, same as the header
                        cell.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
                        row.addView(cell);
                    }

                    tableUsers.addView(row);
                }
            });
        }).start();
    }
}