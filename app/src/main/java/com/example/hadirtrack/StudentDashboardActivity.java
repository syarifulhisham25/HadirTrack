package com.example.hadirtrack;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

public class StudentDashboardActivity extends AppCompatActivity {

    ListView studentSessionListView;
    Button profileButton, logoutButton;

    FirebaseAuth auth;
    FirebaseFirestore db;

    ArrayList<String> sessionDisplayList = new ArrayList<>();
    ArrayList<String> sessionIdList = new ArrayList<>();
    ArrayList<String> courseIdList = new ArrayList<>();

    ArrayAdapter<String> adapter;

    ArrayList<String> enrolledCourseIdList = new ArrayList<>();

    int coursesLoadedCount = 0;
    int totalCoursesToLoad = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_dashboard);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        studentSessionListView = findViewById(R.id.studentSessionListView);
        profileButton = findViewById(R.id.profileButton);
        logoutButton = findViewById(R.id.logoutButton);

        adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                sessionDisplayList
        );

        studentSessionListView.setAdapter(adapter);

        studentSessionListView.setOnItemClickListener((parent, view, position, id) -> {
            if (position >= sessionIdList.size()) {
                return;
            }

            Intent intent = new Intent(StudentDashboardActivity.this, StudentSessionDetailActivity.class);
            intent.putExtra("sessionId", sessionIdList.get(position));
            intent.putExtra("courseId", courseIdList.get(position));
            startActivity(intent);
        });

        profileButton.setOnClickListener(v -> {
            Intent intent = new Intent(StudentDashboardActivity.this, ProfileActivity.class);
            startActivity(intent);
        });

        logoutButton.setOnClickListener(v -> {
            auth.signOut();
            Intent intent = new Intent(StudentDashboardActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadEnrolledCourses();
    }

    private void loadEnrolledCourses() {
        if (auth.getCurrentUser() == null) {
            return;
        }

        String studentUserId = auth.getCurrentUser().getUid();

        sessionDisplayList.clear();
        sessionIdList.clear();
        courseIdList.clear();
        enrolledCourseIdList.clear();
        adapter.notifyDataSetChanged();

        db.collection("course_students")
                .whereEqualTo("studentUserId", studentUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        sessionDisplayList.add("You are not enrolled in any course yet");
                        adapter.notifyDataSetChanged();
                        return;
                    }

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String courseId = document.getString("courseId");

                        if (courseId != null && !courseId.isEmpty()) {
                            enrolledCourseIdList.add(courseId);
                        }
                    }

                    if (enrolledCourseIdList.isEmpty()) {
                        sessionDisplayList.add("You are not enrolled in any course yet");
                        adapter.notifyDataSetChanged();
                        return;
                    }

                    loadSessionsForEnrolledCourses();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load enrolled courses: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void loadSessionsForEnrolledCourses() {
        sessionDisplayList.clear();
        sessionIdList.clear();
        courseIdList.clear();

        coursesLoadedCount = 0;
        totalCoursesToLoad = enrolledCourseIdList.size();

        for (String enrolledCourseId : enrolledCourseIdList) {
            db.collection("sessions")
                    .whereEqualTo("courseId", enrolledCourseId)
                    .whereEqualTo("status", "active")
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        coursesLoadedCount++;

                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            String sessionId = document.getId();
                            String courseId = document.getString("courseId");
                            String courseCode = document.getString("courseCode");
                            String courseName = document.getString("courseName");
                            String sessionTitle = document.getString("sessionTitle");
                            String locationName = document.getString("locationName");
                            String roomName = document.getString("roomName");
                            String startTime = document.getString("startTime");
                            String endTime = document.getString("endTime");

                            sessionIdList.add(sessionId);
                            courseIdList.add(courseId);

                            String displayText = courseCode + " - " + courseName
                                    + "\n" + sessionTitle
                                    + "\nLocation: " + locationName
                                    + "\nRoom: " + roomName
                                    + "\nTime: " + startTime + " - " + endTime;

                            sessionDisplayList.add(displayText);
                        }

                        checkIfAllCoursesLoaded();
                    })
                    .addOnFailureListener(e -> {
                        coursesLoadedCount++;
                        Toast.makeText(this, "Failed to load sessions: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        checkIfAllCoursesLoaded();
                    });
        }
    }

    private void checkIfAllCoursesLoaded() {
        if (coursesLoadedCount >= totalCoursesToLoad) {
            if (sessionDisplayList.isEmpty()) {
                sessionDisplayList.add("No active sessions for your enrolled courses");
            }

            adapter.notifyDataSetChanged();
        }
    }
}