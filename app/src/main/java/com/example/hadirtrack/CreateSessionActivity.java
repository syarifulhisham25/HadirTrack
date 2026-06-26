package com.example.hadirtrack;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class CreateSessionActivity extends AppCompatActivity {

    TextView courseInfoText, startDateText, startTimeText, endDateText, endTimeText;

    EditText sessionTitleInput, locationNameInput, roomNameInput,
            latitudeInput, longitudeInput, radiusInput;

    Button selectStartDateButton, selectStartTimeButton,
            selectEndDateButton, selectEndTimeButton,
            saveSessionButton, backButton;

    FirebaseAuth auth;
    FirebaseFirestore db;

    String courseId, courseCode, courseName;

    String startDate = "";
    String startTimeOnly = "";
    String endDate = "";
    String endTimeOnly = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_session);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        courseInfoText = findViewById(R.id.courseInfoText);

        sessionTitleInput = findViewById(R.id.sessionTitleInput);
        locationNameInput = findViewById(R.id.locationNameInput);
        roomNameInput = findViewById(R.id.roomNameInput);
        latitudeInput = findViewById(R.id.latitudeInput);
        longitudeInput = findViewById(R.id.longitudeInput);
        radiusInput = findViewById(R.id.radiusInput);

        startDateText = findViewById(R.id.startDateText);
        startTimeText = findViewById(R.id.startTimeText);
        endDateText = findViewById(R.id.endDateText);
        endTimeText = findViewById(R.id.endTimeText);

        selectStartDateButton = findViewById(R.id.selectStartDateButton);
        selectStartTimeButton = findViewById(R.id.selectStartTimeButton);
        selectEndDateButton = findViewById(R.id.selectEndDateButton);
        selectEndTimeButton = findViewById(R.id.selectEndTimeButton);

        saveSessionButton = findViewById(R.id.saveSessionButton);
        backButton = findViewById(R.id.backButton);

        courseId = getIntent().getStringExtra("courseId");
        courseCode = getIntent().getStringExtra("courseCode");
        courseName = getIntent().getStringExtra("courseName");

        courseInfoText.setText(courseCode + " - " + courseName);

        selectStartDateButton.setOnClickListener(v -> showDatePicker("start"));
        selectStartTimeButton.setOnClickListener(v -> showTimePicker("start"));
        selectEndDateButton.setOnClickListener(v -> showDatePicker("end"));
        selectEndTimeButton.setOnClickListener(v -> showTimePicker("end"));

        saveSessionButton.setOnClickListener(v -> validateAndSaveSession());

        backButton.setOnClickListener(v -> finish());
    }

    private void showDatePicker(String type) {
        Calendar calendar = Calendar.getInstance();

        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, selectedYear, selectedMonth, selectedDay) -> {

                    String date = String.format(
                            Locale.getDefault(),
                            "%04d-%02d-%02d",
                            selectedYear,
                            selectedMonth + 1,
                            selectedDay
                    );

                    if (type.equals("start")) {
                        startDate = date;
                        startDateText.setText("Start Date: " + date);
                    } else {
                        endDate = date;
                        endDateText.setText("End Date: " + date);
                    }
                },
                year,
                month,
                day
        );

        datePickerDialog.show();
    }

    private void showTimePicker(String type) {
        Calendar calendar = Calendar.getInstance();

        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                (view, selectedHour, selectedMinute) -> {

                    String time = String.format(
                            Locale.getDefault(),
                            "%02d:%02d",
                            selectedHour,
                            selectedMinute
                    );

                    if (type.equals("start")) {
                        startTimeOnly = time;
                        startTimeText.setText("Start Time: " + time);
                    } else {
                        endTimeOnly = time;
                        endTimeText.setText("End Time: " + time);
                    }
                },
                hour,
                minute,
                true
        );

        timePickerDialog.show();
    }

    private void validateAndSaveSession() {
        String sessionTitle = sessionTitleInput.getText().toString().trim();
        String locationName = locationNameInput.getText().toString().trim();
        String roomName = roomNameInput.getText().toString().trim();
        String latitudeText = latitudeInput.getText().toString().trim();
        String longitudeText = longitudeInput.getText().toString().trim();
        String radiusText = radiusInput.getText().toString().trim();

        if (sessionTitle.isEmpty()) {
            sessionTitleInput.setError("Session title is required");
            return;
        }

        if (locationName.isEmpty()) {
            locationNameInput.setError("Location name is required");
            return;
        }

        if (roomName.isEmpty()) {
            roomNameInput.setError("Room name is required");
            return;
        }

        if (latitudeText.isEmpty()) {
            latitudeInput.setError("Latitude is required");
            return;
        }

        if (longitudeText.isEmpty()) {
            longitudeInput.setError("Longitude is required");
            return;
        }

        if (radiusText.isEmpty()) {
            radiusInput.setError("Radius is required");
            return;
        }

        if (startDate.isEmpty()) {
            Toast.makeText(this, "Please select start date", Toast.LENGTH_SHORT).show();
            return;
        }

        if (startTimeOnly.isEmpty()) {
            Toast.makeText(this, "Please select start time", Toast.LENGTH_SHORT).show();
            return;
        }

        if (endDate.isEmpty()) {
            Toast.makeText(this, "Please select end date", Toast.LENGTH_SHORT).show();
            return;
        }

        if (endTimeOnly.isEmpty()) {
            Toast.makeText(this, "Please select end time", Toast.LENGTH_SHORT).show();
            return;
        }

        double latitude;
        double longitude;
        int radiusMeter;

        try {
            latitude = Double.parseDouble(latitudeText);
            longitude = Double.parseDouble(longitudeText);
            radiusMeter = Integer.parseInt(radiusText);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Latitude, longitude, and radius must be valid numbers", Toast.LENGTH_LONG).show();
            return;
        }

        String startTime = startDate + " " + startTimeOnly;
        String endTime = endDate + " " + endTimeOnly;

        saveSession(sessionTitle, locationName, roomName, latitude, longitude, radiusMeter, startTime, endTime);
    }

    private void saveSession(String sessionTitle, String locationName, String roomName,
                             double latitude, double longitude, int radiusMeter,
                             String startTime, String endTime) {

        String lecturerId = auth.getCurrentUser().getUid();

        saveSessionButton.setEnabled(false);
        saveSessionButton.setText("Saving...");

        Map<String, Object> session = new HashMap<>();
        session.put("courseId", courseId);
        session.put("courseCode", courseCode);
        session.put("courseName", courseName);
        session.put("lecturerId", lecturerId);
        session.put("sessionTitle", sessionTitle);
        session.put("locationName", locationName);
        session.put("roomName", roomName);
        session.put("latitude", latitude);
        session.put("longitude", longitude);
        session.put("radiusMeter", radiusMeter);
        session.put("startTime", startTime);
        session.put("endTime", endTime);
        session.put("classPhotoUrl", "");
        session.put("status", "active");
        session.put("createdAt", FieldValue.serverTimestamp());

        db.collection("sessions")
                .add(session)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Session created successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    saveSessionButton.setEnabled(true);
                    saveSessionButton.setText("Save Session");
                    Toast.makeText(this, "Failed to save session: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}