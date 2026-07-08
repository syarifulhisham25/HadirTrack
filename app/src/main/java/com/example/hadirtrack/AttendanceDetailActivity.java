package com.example.hadirtrack;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class AttendanceDetailActivity extends AppCompatActivity {

    TextView titleText, infoText;
    ImageView selfieImageView;
    Button backButton;

    FirebaseFirestore db;

    String attendanceId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance_detail);

        db = FirebaseFirestore.getInstance();

        titleText = findViewById(R.id.titleText);
        infoText = findViewById(R.id.infoText);
        selfieImageView = findViewById(R.id.selfieImageView);
        backButton = findViewById(R.id.backButton);

        attendanceId = getIntent().getStringExtra("attendanceId");

        loadAttendanceDetail();

        backButton.setOnClickListener(v -> finish());
    }

    private void loadAttendanceDetail() {
        db.collection("attendance")
                .document(attendanceId)
                .get()
                .addOnSuccessListener(this::displayAttendance)
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load attendance: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void displayAttendance(DocumentSnapshot document) {
        if (!document.exists()) {
            Toast.makeText(this, "Attendance not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String studentName = document.getString("studentName");
        String studentId = document.getString("studentId");
        String status = document.getString("status");
        String selfieUrl = document.getString("selfieUrl");

        Double latitude = document.getDouble("latitude");
        Double longitude = document.getDouble("longitude");
        Double distance = document.getDouble("distanceFromClass");

        String distanceText = distance == null ? "-" : Math.round(distance) + "m";
        String latText = latitude == null ? "-" : String.valueOf(latitude);
        String lngText = longitude == null ? "-" : String.valueOf(longitude);

        titleText.setText(studentName);

        String info = "Student ID: " + studentId
                + "\nStatus: " + status
                + "\nDistance from class: " + distanceText
                + "\nLatitude: " + latText
                + "\nLongitude: " + lngText;

        infoText.setText(info);

        if (selfieUrl != null && !selfieUrl.isEmpty()) {
            Glide.with(this)
                    .load(selfieUrl)
                    .into(selfieImageView);
        } else {
            Toast.makeText(this, "No selfie URL found", Toast.LENGTH_SHORT).show();
        }
    }
}