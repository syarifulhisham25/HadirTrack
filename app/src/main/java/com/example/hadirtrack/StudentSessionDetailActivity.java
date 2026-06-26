package com.example.hadirtrack;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class StudentSessionDetailActivity extends AppCompatActivity {

    TextView courseTitleText, sessionInfoText;
    Button checkInButton, backButton;

    FirebaseFirestore db;

    String sessionId, courseId;

    double classLatitude = 0.0;
    double classLongitude = 0.0;
    long radiusMeter = 50;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_session_detail);

        db = FirebaseFirestore.getInstance();

        courseTitleText = findViewById(R.id.courseTitleText);
        sessionInfoText = findViewById(R.id.sessionInfoText);
        checkInButton = findViewById(R.id.checkInButton);
        backButton = findViewById(R.id.backButton);

        sessionId = getIntent().getStringExtra("sessionId");
        courseId = getIntent().getStringExtra("courseId");

        loadSessionDetails();

        backButton.setOnClickListener(v -> finish());

        checkInButton.setOnClickListener(v -> {
            Toast.makeText(this, "Next: GPS validation + selfie camera", Toast.LENGTH_SHORT).show();
        });
    }

    private void loadSessionDetails() {
        db.collection("sessions").document(sessionId).get()
                .addOnSuccessListener(this::displaySession)
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load session: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void displaySession(DocumentSnapshot document) {
        if (!document.exists()) {
            Toast.makeText(this, "Session not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String courseCode = document.getString("courseCode");
        String courseName = document.getString("courseName");
        String sessionTitle = document.getString("sessionTitle");
        String locationName = document.getString("locationName");
        String roomName = document.getString("roomName");
        String startTime = document.getString("startTime");
        String endTime = document.getString("endTime");

        Double latitude = document.getDouble("latitude");
        Double longitude = document.getDouble("longitude");
        Long radius = document.getLong("radiusMeter");

        if (latitude != null) {
            classLatitude = latitude;
        }

        if (longitude != null) {
            classLongitude = longitude;
        }

        if (radius != null) {
            radiusMeter = radius;
        }

        courseTitleText.setText(courseCode + " - " + courseName);

        String info = "Session: " + sessionTitle
                + "\nLocation: " + locationName
                + "\nRoom: " + roomName
                + "\nStart: " + startTime
                + "\nEnd: " + endTime
                + "\nAllowed Radius: " + radiusMeter + " meters";

        sessionInfoText.setText(info);
    }
}