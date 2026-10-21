package com.example.a1stactivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

// The CUSTOMERS screen.
//   ADD CUSTOMER  -> sends the form to POST /customers  (INSERT into Supabase)
//   CUSTOMER LIST -> gets the rows from GET /customers  (SELECT from Supabase)
public class CustomersActivity extends AppCompatActivity {

    // The /customers route on your Render server (used for both POST and GET)
    private static final String CUSTOMERS_URL = "https://android-studio-activity-2.onrender.com/customers";

    // Fields, because both onCreate() and loadCustomers() use them
    private TextView tvCustomerCount;
    private TableLayout tableCustomers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the design from res/layout/activity_customers.xml
        setContentView(R.layout.activity_customers);

        // Get the views from the XML
        ImageButton btnBack = findViewById(R.id.btnBack);
        EditText etCustomerId = findViewById(R.id.etCustomerId);
        EditText etLastName = findViewById(R.id.etCustLastName);
        EditText etFirstName = findViewById(R.id.etCustFirstName);
        EditText etMobileNo = findViewById(R.id.etMobileNo);
        EditText etAddress = findViewById(R.id.etAddress);
        Button btnAddCustomer = findViewById(R.id.btnAddCustomer);
        tvCustomerCount = findViewById(R.id.tvCustomerCount);
        tableCustomers = findViewById(R.id.tableCustomers);

        // Back arrow: return to the dashboard
        btnBack.setOnClickListener(v -> finish());

        // Show the Customer List as soon as the screen opens
        loadCustomers();

        // ===== ADD CUSTOMER BUTTON =====
        btnAddCustomer.setOnClickListener(v -> {

            // 1. Read what the user typed
            String customerId = etCustomerId.getText().toString().trim();
            String lastName = etLastName.getText().toString().trim();
            String firstName = etFirstName.getText().toString().trim();
            String mobileNo = etMobileNo.getText().toString().trim();
            String address = etAddress.getText().toString().trim();

            // 2. Quick check in the app first (the server checks everything again)
            if (customerId.isEmpty() || lastName.isEmpty() || firstName.isEmpty()
                    || mobileNo.isEmpty() || address.isEmpty()) {
                Toast.makeText(this, R.string.error_fill_all, Toast.LENGTH_SHORT).show();
                return;
            }

            // 3. Disable the button while waiting
            btnAddCustomer.setEnabled(false);
            btnAddCustomer.setText(R.string.adding_customer);

            // 4. Send the customer to the server on a background thread
            new Thread(() -> {
                boolean success = false;
                String message;

                try {
                    // 4a. Open a POST connection to /customers
                    URL url = new URL(CUSTOMERS_URL);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("Content-Type", "application/json");
                    connection.setDoOutput(true);
                    connection.setConnectTimeout(60000);
                    connection.setReadTimeout(60000);

                    // 4b. Build the JSON with the same names as the Supabase columns.
                    //     Same body you sent in Postman!
                    JSONObject requestBody = new JSONObject();
                    requestBody.put("customerid", customerId);
                    requestBody.put("lastname", lastName);
                    requestBody.put("firstname", firstName);
                    requestBody.put("mobileno", mobileNo);
                    requestBody.put("address", address);

                    // 4c. Send the JSON
                    OutputStream outputStream = connection.getOutputStream();
                    outputStream.write(requestBody.toString().getBytes(StandardCharsets.UTF_8));
                    outputStream.close();

                    // 4d. Read the reply (success replies and error replies come from different streams)
                    JSONObject response = new JSONObject(readResponse(connection));
                    success = response.getBoolean("success");
                    message = response.getString("message");

                } catch (Exception e) {
                    message = getString(R.string.error_connection);
                }

                boolean finalSuccess = success;
                String finalMessage = message;

                // 5. Back to the main thread to update the screen
                runOnUiThread(() -> {
                    btnAddCustomer.setEnabled(true);
                    btnAddCustomer.setText(R.string.btn_add_customer);

                    // Show the server's message, e.g. "Customer added successfully."
                    // or "Customer ID 1 already exists."
                    Toast.makeText(CustomersActivity.this, finalMessage, Toast.LENGTH_LONG).show();

                    if (finalSuccess) {
                        // Clear the form for the next customer
                        etCustomerId.setText("");
                        etLastName.setText("");
                        etFirstName.setText("");
                        etMobileNo.setText("");
                        etAddress.setText("");

                        // Reload the list so the new customer shows up right away
                        loadCustomers();
                    }
                });
            }).start();
        });
    }

    // -----------------------------------------------------------------
    //  Gets all customers from GET /customers and shows them in the table.
    //  It's a separate method because it runs twice: when the screen opens,
    //  and again after every new customer is added.
    // -----------------------------------------------------------------
    private void loadCustomers() {
        tvCustomerCount.setText(R.string.loading_customers);

        new Thread(() -> {
            JSONArray customers = null; // stays null if something goes wrong

            try {
                // GET request, because we are only ASKING for data
                URL url = new URL(CUSTOMERS_URL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(60000);
                connection.setReadTimeout(60000);

                // Reply looks like: { "success": true, "count": 2, "customers": [ {...}, {...} ] }
                JSONObject response = new JSONObject(readResponse(connection));
                if (response.getBoolean("success")) {
                    customers = response.getJSONArray("customers");
                }

            } catch (Exception e) {
                // No internet, server down, etc. "customers" stays null.
            }

            JSONArray finalCustomers = customers;

            runOnUiThread(() -> {
                // 1. Remove the old customer rows, but keep the header row (child number 0).
                //    Without this, every reload would add the same customers again.
                int childCount = tableCustomers.getChildCount();
                if (childCount > 1) {
                    tableCustomers.removeViews(1, childCount - 1);
                }

                // 2. If loading failed, show the error and stop here
                if (finalCustomers == null) {
                    tvCustomerCount.setText(R.string.error_connection);
                    return;
                }

                // 3. Show how many customers there are, e.g. "Total customers: 3"
                tvCustomerCount.setText(getString(R.string.customer_count, finalCustomers.length()));

                // Change 10dp into pixels (the same padding as the header cells)
                int padding = (int) (10 * getResources().getDisplayMetrics().density);

                // 4. Make one table row for each customer
                for (int i = 0; i < finalCustomers.length(); i++) {
                    JSONObject customer = finalCustomers.optJSONObject(i);

                    String id = customer.optString("customerid");
                    String name = customer.optString("firstname") + " " + customer.optString("lastname");
                    String mobile = customer.optString("mobileno");
                    String address = customer.optString("address");

                    // 4a. Thin grey line between rows
                    View divider = new View(this);
                    divider.setBackgroundColor(ContextCompat.getColor(this, R.color.divider));
                    tableCustomers.addView(divider, new TableLayout.LayoutParams(
                            TableLayout.LayoutParams.MATCH_PARENT, 1));

                    // 4b. The row with one cell per column: ID, Name, Mobile, Address
                    TableRow row = new TableRow(this);
                    String[] cells = {id, name, mobile, address};
                    for (String cellText : cells) {
                        TextView cell = new TextView(this);
                        cell.setText(cellText);
                        cell.setPadding(padding, padding, padding, padding);
                        cell.setTextSize(12); // in sp, same as the header
                        cell.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
                        row.addView(cell);
                    }
                    tableCustomers.addView(row);
                }
            });
        }).start();
    }

    // -----------------------------------------------------------------
    //  Reads the server's reply into one String.
    //  Used by both the POST (add) and the GET (list).
    // -----------------------------------------------------------------
    private String readResponse(HttpURLConnection connection) throws Exception {
        // Successful replies come from getInputStream(), error replies from getErrorStream()
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

        return responseText.toString();
    }
}