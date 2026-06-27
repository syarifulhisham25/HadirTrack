package com.example.hadirtrack;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class LecturerDashboardActivity extends AppCompatActivity {

    Button manageCoursesButton, logoutButton, profileButton;
    FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lecturer_dashboard);

        auth = FirebaseAuth.getInstance();

        manageCoursesButton = findViewById(R.id.manageCoursesButton);
        logoutButton = findViewById(R.id.logoutButton);
        profileButton = findViewById(R.id.profileButton);

        manageCoursesButton.setOnClickListener(v -> {
            Intent intent = new Intent(LecturerDashboardActivity.this, CourseListActivity.class);
            startActivity(intent);
        });

        profileButton.setOnClickListener(v -> {
            Intent intent = new Intent(LecturerDashboardActivity.this, ProfileActivity.class);
            startActivity(intent);
        });

        logoutButton.setOnClickListener(v -> {
            auth.signOut();
            Intent intent = new Intent(LecturerDashboardActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });
    }
}