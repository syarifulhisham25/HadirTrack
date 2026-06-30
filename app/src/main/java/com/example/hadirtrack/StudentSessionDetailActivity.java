package com.example.hadirtrack;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;


public class StudentSessionDetailActivity extends AppCompatActivity {

    TextView courseTitleText, sessionInfoText, attendanceStatusText;
    Button openMapButton, checkInButton, backButton;

    FirebaseFirestore db;
    FirebaseAuth auth;
    FusedLocationProviderClient fusedLocationClient;

    String sessionId, courseId;

    double classLatitude = 0.0;
    double classLongitude = 0.0;
    long radiusMeter = 50;

    ActivityResultLauncher<String> locationPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_session_detail);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        courseTitleText = findViewById(R.id.courseTitleText);
        sessionInfoText = findViewById(R.id.sessionInfoText);
        checkInButton = findViewById(R.id.checkInButton);
        backButton = findViewById(R.id.backButton);
        openMapButton = findViewById(R.id.openMapButton);
        attendanceStatusText = findViewById(R.id.attendanceStatusText);

        sessionId = getIntent().getStringExtra("sessionId");
        courseId = getIntent().getStringExtra("courseId");


        locationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        getCurrentLocationAndValidate();
                    } else {
                        Toast.makeText(this, "Location permission is required for attendance", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        loadSessionDetails();

        backButton.setOnClickListener(v -> finish());

        openMapButton.setOnClickListener(v -> openClassLocationInMap());
        checkInButton.setOnClickListener(v -> checkLocationPermission());
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkEnrollmentAndAttendanceStatus();
    }

    private void checkEnrollmentAndAttendanceStatus() {
        if (auth.getCurrentUser() == null || sessionId == null || courseId == null) {
            checkInButton.setEnabled(false);
            checkInButton.setText("Not Allowed");
            attendanceStatusText.setText("Attendance Status: Unable to verify");
            attendanceStatusText.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            return;
        }

        String studentUserId = auth.getCurrentUser().getUid();
        String enrollmentDocId = courseId + "_" + studentUserId;

        db.collection("course_students")
                .document(enrollmentDocId)
                .get()
                .addOnSuccessListener(enrollmentSnapshot -> {
                    if (!enrollmentSnapshot.exists()) {
                        attendanceStatusText.setText("Attendance Status: Not enrolled in this course");
                        attendanceStatusText.setTextColor(getResources().getColor(android.R.color.holo_red_dark));

                        checkInButton.setEnabled(false);
                        checkInButton.setText("Not Enrolled");
                        return;
                    }

                    checkAttendanceStatusOnly();
                })
                .addOnFailureListener(e -> {
                    attendanceStatusText.setText("Attendance Status: Failed to verify enrollment");
                    attendanceStatusText.setTextColor(getResources().getColor(android.R.color.holo_red_dark));

                    checkInButton.setEnabled(false);
                    checkInButton.setText("Unable to Verify");
                    Toast.makeText(this, "Failed to verify enrollment", Toast.LENGTH_SHORT).show();
                });
    }

    private void checkAttendanceStatusOnly() {
        String studentUserId = auth.getCurrentUser().getUid();
        String attendanceDocId = sessionId + "_" + studentUserId;

        db.collection("attendance")
                .document(attendanceDocId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        attendanceStatusText.setText("Attendance Status: Submitted");
                        attendanceStatusText.setTextColor(getResources().getColor(android.R.color.holo_green_dark));

                        checkInButton.setEnabled(false);
                        checkInButton.setText("Attendance Submitted");
                    } else {
                        attendanceStatusText.setText("Attendance Status: Not submitted");
                        attendanceStatusText.setTextColor(getResources().getColor(android.R.color.holo_red_dark));

                        checkInButton.setEnabled(true);
                        checkInButton.setText("Check In");
                    }
                })
                .addOnFailureListener(e -> {
                    attendanceStatusText.setText("Attendance Status: Failed to check");
                    attendanceStatusText.setTextColor(getResources().getColor(android.R.color.holo_red_dark));

                    checkInButton.setEnabled(false);
                    checkInButton.setText("Unable to Check");
                    Toast.makeText(this, "Failed to check attendance status", Toast.LENGTH_SHORT).show();
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
                + "\nAllowed Radius: " + radiusMeter + " meters"
                + "\n\nClass GPS: " + classLatitude + ", " + classLongitude;

        sessionInfoText.setText(info);
    }

    private void openClassLocationInMap() {
        if (classLatitude == 0.0 && classLongitude == 0.0) {
            Toast.makeText(this, "Class location is not available", Toast.LENGTH_SHORT).show();
            return;
        }

        Uri navigationUri = Uri.parse("google.navigation:q=" + classLatitude + "," + classLongitude);

        Intent mapIntent = new Intent(Intent.ACTION_VIEW, navigationUri);
        mapIntent.setPackage("com.google.android.apps.maps");

        try {
            startActivity(mapIntent);
        } catch (Exception e) {
            Uri browserUri = Uri.parse("https://www.google.com/maps/search/?api=1&query="
                    + classLatitude + "," + classLongitude);

            Intent browserIntent = new Intent(Intent.ACTION_VIEW, browserUri);
            startActivity(browserIntent);
        }
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED) {

            getCurrentLocationAndValidate();

        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private void getCurrentLocationAndValidate() {
        checkInButton.setEnabled(false);
        checkInButton.setText("Checking location...");

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED) {
            checkInButton.setEnabled(true);
            checkInButton.setText("Check In");
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    checkInButton.setEnabled(true);
                    checkInButton.setText("Check In");

                    if (location == null) {
                        Toast.makeText(this, "Unable to get location. Please turn on GPS and try again.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    validateDistance(location);
                })
                .addOnFailureListener(e -> {
                    checkInButton.setEnabled(true);
                    checkInButton.setText("Check In");
                    Toast.makeText(this, "Failed to get location: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void validateDistance(Location studentLocation) {
        float[] results = new float[1];

        Location.distanceBetween(
                studentLocation.getLatitude(),
                studentLocation.getLongitude(),
                classLatitude,
                classLongitude,
                results
        );

        float distanceInMeter = results[0];

        if (distanceInMeter <= radiusMeter) {
            Intent intent = new Intent(StudentSessionDetailActivity.this, SelfieActivity.class);
            intent.putExtra("sessionId", sessionId);
            intent.putExtra("courseId", courseId);
            intent.putExtra("studentLatitude", studentLocation.getLatitude());
            intent.putExtra("studentLongitude", studentLocation.getLongitude());
            intent.putExtra("distanceFromClass", distanceInMeter);
            startActivity(intent);


        } else {
            Toast.makeText(
                    this,
                    "You are too far from class. Distance: " + Math.round(distanceInMeter) + "m",
                    Toast.LENGTH_LONG
            ).show();
        }
    }
}