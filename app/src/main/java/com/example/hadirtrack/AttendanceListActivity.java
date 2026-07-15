package com.example.hadirtrack;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

public class AttendanceListActivity extends AppCompatActivity {

    TextView titleText, subtitleText, totalPresentText;
    ListView attendanceListView;
    Button backButton;
    SwipeRefreshLayout swipeRefreshLayout;

    FirebaseFirestore db;
    ListenerRegistration attendanceListener;

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
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

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

        swipeRefreshLayout.setOnRefreshListener(() -> {
            if (attendanceListener != null) {
                attendanceListener.remove();
            }
            startAttendanceListener();
        });

        backButton.setOnClickListener(v -> finish());
    }

    @Override
    protected void onStart() {
        super.onStart();
        startAttendanceListener();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (attendanceListener != null) {
            attendanceListener.remove();
        }
    }

    private void startAttendanceListener() {
        attendanceListener = db.collection("attendance")
                .whereEqualTo("sessionId", sessionId)
                .addSnapshotListener((snapshots, e) -> {
                    if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                    if (e != null) {
                        Toast.makeText(this, "Error listening to attendance: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (snapshots != null) {
                        attendanceDisplayList.clear();
                        attendanceIdList.clear();

                        if (snapshots.isEmpty()) {
                            totalPresentText.setText("Total Present: 0");
                            attendanceDisplayList.add("No attendance submitted yet");
                            adapter.notifyDataSetChanged();
                            return;
                        }

                        ArrayList<QueryDocumentSnapshot> sortedDocs = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : snapshots) {
                            sortedDocs.add(doc);
                        }

                        sortedDocs.sort((d1, d2) -> {
                            com.google.firebase.Timestamp t1 = d1.getTimestamp("timestamp");
                            com.google.firebase.Timestamp t2 = d2.getTimestamp("timestamp");
                            if (t1 == null || t2 == null) return 0;
                            return t2.compareTo(t1);
                        });

                        int totalPresent = 0;

                        for (QueryDocumentSnapshot document : sortedDocs) {
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
                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void loadAttendance() {
    }
}