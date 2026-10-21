package com.example.a1stactivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

// The MAIN DASHBOARD. The user is sent here after the OTP is verified.
// For now it has one menu button (Customers) and a Log Out button.
public class DashboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the design from res/layout/activity_dashboard.xml
        setContentView(R.layout.activity_dashboard);

        Button btnCustomers = findViewById(R.id.btnCustomers);
        Button btnLogOut = findViewById(R.id.btnLogOut);

        // Customers: open the Customers screen (Add Customer + Customer List)
        btnCustomers.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, CustomersActivity.class);
            startActivity(intent);
        });

        // Log Out: close the dashboard, which goes back to the Login screen
        btnLogOut.setOnClickListener(v -> finish());
    }
}