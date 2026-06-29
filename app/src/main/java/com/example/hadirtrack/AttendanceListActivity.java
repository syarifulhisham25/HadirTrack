package com.example.hadirtrack;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

public class AttendanceListActivity extends AppCompatActivity {

    TextView titleText, subtitleText, totalPresentText;
    ListView attendanceListView;
    Button backButton;

    FirebaseFirestore db;

    String sessionId, courseId, courseCode, courseName;

    ArrayList<String> attendanceDisplayList = new ArrayList<>();
    ArrayList<String> attendanceIdList = new ArrayList<>();

    ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance_list);

        db = FirebaseFirestore.getInstance();

        titleText = findViewById(R.id.titleText);
        subtitleText = findViewById(R.id.subtitleText);
        attendanceListView = findViewById(R.id.attendanceListView);
        backButton = findViewById(R.id.backButton);
        totalPresentText = findViewById(R.id.totalPresentText);

        sessionId = getIntent().getStringExtra("sessionId");
        courseId = getIntent().getStringExtra("courseId");
        courseCode = getIntent().getStringExtra("courseCode");
        courseName = getIntent().getStringExtra("courseName");

        titleText.setText("Attendance List");
        subtitleText.setText(courseCode + " - " + courseName);

        adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                attendanceDisplayList
        );

        attendanceListView.setAdapter(adapter);

        attendanceListView.setOnItemClickListener((parent, view, position, id) -> {
            if (position >= attendanceIdList.size()) {
                return;
            }

            Intent intent = new Intent(AttendanceListActivity.this, AttendanceDetailActivity.class);
            intent.putExtra("attendanceId", attendanceIdList.get(position));
            startActivity(intent);
        });

        backButton.setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAttendance();
    }

    private void loadAttendance() {
        db.collection("attendance")
                .whereEqualTo("sessionId", sessionId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    attendanceDisplayList.clear();
                    attendanceIdList.clear();

                    if (queryDocumentSnapshots.isEmpty()) {
                        totalPresentText.setText("Total Present: 0");
                        attendanceDisplayList.add("No attendance submitted yet");
                        adapter.notifyDataSetChanged();
                        return;
                    }

                    int totalPresent = 0;

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String attendanceId = document.getId();
                        String studentName = document.getString("studentName");
                        String studentId = document.getString("studentId");
                        String status = document.getString("status");

                        Double distance = document.getDouble("distanceFromClass");

                        attendanceIdList.add(attendanceId);

                        String distanceText = distance == null
                                ? "-"
                                : Math.round(distance) + "m";

                        String displayText = studentName
                                + "\nStudent ID: " + studentId
                                + "\nStatus: " + status
                                + "\nDistance: " + distanceText
                                + "\n\nTap to view";

                        attendanceDisplayList.add(displayText);

                        if ("present".equals(status)) {
                            totalPresent++;
                        }
                    }

                    totalPresentText.setText("Total Present: " + totalPresent);

                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load attendance: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}