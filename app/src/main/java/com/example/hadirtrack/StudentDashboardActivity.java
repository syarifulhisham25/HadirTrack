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
    Button logoutButton;

    FirebaseAuth auth;
    FirebaseFirestore db;

    ArrayList<String> sessionDisplayList = new ArrayList<>();
    ArrayList<String> sessionIdList = new ArrayList<>();
    ArrayList<String> courseIdList = new ArrayList<>();

    ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_dashboard);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        studentSessionListView = findViewById(R.id.studentSessionListView);
        logoutButton = findViewById(R.id.logoutButton);

        adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                sessionDisplayList
        );

        studentSessionListView.setAdapter(adapter);

        studentSessionListView.setOnItemClickListener((parent, view, position, id) -> {
            if (sessionIdList.isEmpty()) {
                return;
            }

            Intent intent = new Intent(StudentDashboardActivity.this, StudentSessionDetailActivity.class);
            intent.putExtra("sessionId", sessionIdList.get(position));
            intent.putExtra("courseId", courseIdList.get(position));
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
        loadActiveSessions();
    }

    private void loadActiveSessions() {
        db.collection("sessions")
                .whereEqualTo("status", "active")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    sessionDisplayList.clear();
                    sessionIdList.clear();
                    courseIdList.clear();

                    if (queryDocumentSnapshots.isEmpty()) {
                        sessionDisplayList.add("No active sessions available");
                        adapter.notifyDataSetChanged();
                        return;
                    }

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

                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load sessions: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}