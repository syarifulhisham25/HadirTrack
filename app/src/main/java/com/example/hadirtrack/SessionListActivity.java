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

public class SessionListActivity extends AppCompatActivity {

    TextView courseTitleText, courseSubtitleText;
    Button addSessionButton;
    ListView sessionListView;

    FirebaseFirestore db;

    String courseId, courseCode, courseName;

    ArrayList<String> sessionDisplayList = new ArrayList<>();
    ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session_list);

        db = FirebaseFirestore.getInstance();

        courseTitleText = findViewById(R.id.courseTitleText);
        courseSubtitleText = findViewById(R.id.courseSubtitleText);
        addSessionButton = findViewById(R.id.addSessionButton);
        sessionListView = findViewById(R.id.sessionListView);

        courseId = getIntent().getStringExtra("courseId");
        courseCode = getIntent().getStringExtra("courseCode");
        courseName = getIntent().getStringExtra("courseName");

        courseTitleText.setText(courseCode);
        courseSubtitleText.setText(courseName);

        adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                sessionDisplayList
        );

        sessionListView.setAdapter(adapter);

        addSessionButton.setOnClickListener(v -> {
            Intent intent = new Intent(SessionListActivity.this, CreateSessionActivity.class);
            intent.putExtra("courseId", courseId);
            intent.putExtra("courseCode", courseCode);
            intent.putExtra("courseName", courseName);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSessions();
    }

    private void loadSessions() {
        db.collection("sessions")
                .whereEqualTo("courseId", courseId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    sessionDisplayList.clear();

                    if (queryDocumentSnapshots.isEmpty()) {
                        sessionDisplayList.add("No sessions yet");
                        adapter.notifyDataSetChanged();
                        return;
                    }

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String sessionTitle = document.getString("sessionTitle");
                        String locationName = document.getString("locationName");
                        String roomName = document.getString("roomName");
                        String status = document.getString("status");

                        String displayText = sessionTitle
                                + "\nLocation: " + locationName
                                + "\nRoom: " + roomName
                                + "\nStatus: " + status;

                        sessionDisplayList.add(displayText);
                    }

                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load sessions: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}