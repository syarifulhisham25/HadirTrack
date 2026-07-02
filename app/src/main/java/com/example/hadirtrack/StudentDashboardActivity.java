package com.example.hadirtrack;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class StudentDashboardActivity extends AppCompatActivity {

    ListView studentSessionListView;
    View sessionsTab, profileButton, logoutButton;

    ActivityResultLauncher<String> notificationPermissionLauncher;

    FirebaseAuth auth;
    FirebaseFirestore db;

    ArrayList<String> sessionDisplayList = new ArrayList<>();
    ArrayList<String> sessionIdList = new ArrayList<>();
    ArrayList<String> courseIdList = new ArrayList<>();

    SessionAdapter adapter;

    ArrayList<String> enrolledCourseIdList = new ArrayList<>();

    int coursesLoadedCount = 0;
    int totalCoursesToLoad = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_dashboard);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        NotificationHelper.createNotificationChannel(this);

        notificationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        loadEnrolledCourses();
                    }
                }
        );

        requestNotificationPermissionIfNeeded();

        studentSessionListView = findViewById(R.id.studentSessionListView);
        sessionsTab = findViewById(R.id.sessionsTab);
        profileButton = findViewById(R.id.profileButton);
        logoutButton = findViewById(R.id.logoutButton);

        adapter = new SessionAdapter();
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

        sessionsTab.setOnClickListener(v -> {
            Toast.makeText(this, "You are already viewing sessions", Toast.LENGTH_SHORT).show();
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

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED) {

                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
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

                            scheduleNotificationForSession(sessionId, courseCode, sessionTitle, roomName, startTime);

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

    private void scheduleNotificationForSession(
            String sessionId,
            String courseCode,
            String sessionTitle,
            String roomName,
            String startTime
    ) {
        if (sessionId == null || startTime == null || startTime.isEmpty()) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }

        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            Date startDate = format.parse(startTime);

            if (startDate == null) {
                return;
            }

            long classStartMillis = startDate.getTime();

            long startsSoonMillis = classStartMillis - (10 * 60 * 1000);
            long attendanceReminderMillis = classStartMillis + (30 * 60 * 1000);

            int startsSoonId = Math.abs((sessionId + "_soon").hashCode());
            int attendanceReminderId = Math.abs((sessionId + "_attendance").hashCode());

            NotificationHelper.scheduleNotification(
                    this,
                    startsSoonMillis,
                    courseCode + " starts soon",
                    sessionTitle + " at " + roomName + " starts in 10 minutes.",
                    startsSoonId
            );

            NotificationHelper.scheduleNotification(
                    this,
                    attendanceReminderMillis,
                    "Attendance reminder",
                    "Have you submitted your attendance for " + courseCode + "?",
                    attendanceReminderId
            );

        } catch (Exception e) {
            Toast.makeText(this, "Failed to schedule notification", Toast.LENGTH_SHORT).show();
        }
    }

    private class SessionAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return sessionDisplayList.size();
        }

        @Override
        public Object getItem(int position) {
            return sessionDisplayList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(StudentDashboardActivity.this)
                        .inflate(R.layout.item_session, parent, false);
            }

            TextView sessionText = convertView.findViewById(R.id.sessionText);
            TextView sessionMenuButton = convertView.findViewById(R.id.sessionMenuButton);

            sessionText.setText(sessionDisplayList.get(position));

            // Student side does not need 3-dot menu
            sessionMenuButton.setVisibility(View.GONE);

            return convertView;
        }
    }
}