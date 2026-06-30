package com.example.hadirtrack;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;

public class SessionListActivity extends AppCompatActivity {

    TextView courseTitleText, courseSubtitleText;
    Button manageStudentsButton, addSessionButton;
    ListView sessionListView;

    FirebaseFirestore db;

    String courseId, courseCode, courseName;

    ArrayList<String> sessionDisplayList = new ArrayList<>();
    ArrayList<String> sessionIdList = new ArrayList<>();

    SessionAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session_list);

        db = FirebaseFirestore.getInstance();

        courseTitleText = findViewById(R.id.courseTitleText);
        courseSubtitleText = findViewById(R.id.courseSubtitleText);
        addSessionButton = findViewById(R.id.addSessionButton);
        sessionListView = findViewById(R.id.sessionListView);
        manageStudentsButton = findViewById(R.id.manageStudentsButton);

        courseId = getIntent().getStringExtra("courseId");
        courseCode = getIntent().getStringExtra("courseCode");
        courseName = getIntent().getStringExtra("courseName");

        courseTitleText.setText(courseCode);
        courseSubtitleText.setText(courseName);

        adapter = new SessionAdapter(this);
        sessionListView.setAdapter(adapter);

        addSessionButton.setOnClickListener(v -> {
            Intent intent = new Intent(SessionListActivity.this, CreateSessionActivity.class);
            intent.putExtra("courseId", courseId);
            intent.putExtra("courseCode", courseCode);
            intent.putExtra("courseName", courseName);
            startActivity(intent);
        });

        manageStudentsButton.setOnClickListener(v -> {
            Intent intent = new Intent(SessionListActivity.this, ManageCourseStudentsActivity.class);
            intent.putExtra("courseId", courseId);
            intent.putExtra("courseCode", courseCode);
            intent.putExtra("courseName", courseName);
            startActivity(intent);
        });

        sessionListView.setOnItemClickListener((parent, view, position, id) -> {
            if (position >= sessionIdList.size()) {
                return;
            }

            openSession(position);
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
                    sessionIdList.clear();

                    if (queryDocumentSnapshots.isEmpty()) {
                        sessionDisplayList.add("No sessions yet");
                        adapter.notifyDataSetChanged();
                        return;
                    }

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String sessionId = document.getId();
                        String sessionTitle = document.getString("sessionTitle");
                        String locationName = document.getString("locationName");
                        String roomName = document.getString("roomName");
                        String startTime = document.getString("startTime");
                        String endTime = document.getString("endTime");
                        String status = document.getString("status");

                        sessionIdList.add(sessionId);

                        String displayText = sessionTitle
                                + "\nLocation: " + locationName
                                + "\nRoom: " + roomName
                                + "\nTime: " + startTime + " - " + endTime
                                + "\nStatus: " + status;

                        sessionDisplayList.add(displayText);
                    }

                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load sessions: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void showSessionMenu(View anchorView, int position) {
        PopupMenu popupMenu = new PopupMenu(this, anchorView);

        popupMenu.getMenu().add("Open");
        popupMenu.getMenu().add("Update");
        popupMenu.getMenu().add("Delete");

        popupMenu.setOnMenuItemClickListener(item -> {
            String title = item.getTitle().toString();

            if (title.equals("Open")) {
                openSession(position);
                return true;
            }

            if (title.equals("Update")) {
                openEditSession(position);
                return true;
            }

            if (title.equals("Delete")) {
                confirmDeleteSession(position);
                return true;
            }

            return false;
        });

        popupMenu.show();
    }

    private void openSession(int position) {
        Intent intent = new Intent(SessionListActivity.this, AttendanceListActivity.class);
        intent.putExtra("sessionId", sessionIdList.get(position));
        intent.putExtra("courseId", courseId);
        intent.putExtra("courseCode", courseCode);
        intent.putExtra("courseName", courseName);
        startActivity(intent);
    }

    private void openEditSession(int position) {
        Intent intent = new Intent(SessionListActivity.this, CreateSessionActivity.class);
        intent.putExtra("mode", "edit");
        intent.putExtra("sessionId", sessionIdList.get(position));
        intent.putExtra("courseId", courseId);
        intent.putExtra("courseCode", courseCode);
        intent.putExtra("courseName", courseName);
        startActivity(intent);
    }

    private void confirmDeleteSession(int position) {
        String sessionId = sessionIdList.get(position);

        new AlertDialog.Builder(this)
                .setTitle("Delete Session")
                .setMessage("Are you sure you want to delete this session? Related attendance records will also be deleted.")
                .setPositiveButton("Delete", (dialog, which) -> deleteSession(sessionId))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteSession(String sessionId) {
        db.collection("attendance")
                .whereEqualTo("sessionId", sessionId)
                .get()
                .addOnSuccessListener(attendanceSnapshots -> {
                    WriteBatch batch = db.batch();

                    for (QueryDocumentSnapshot attendanceDoc : attendanceSnapshots) {
                        batch.delete(attendanceDoc.getReference());
                    }

                    batch.delete(db.collection("sessions").document(sessionId));

                    batch.commit()
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(this, "Session deleted successfully", Toast.LENGTH_SHORT).show();
                                loadSessions();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Failed to delete session: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to check attendance records: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private class SessionAdapter extends BaseAdapter {

        Context context;

        SessionAdapter(Context context) {
            this.context = context;
        }

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
                convertView = LayoutInflater.from(context).inflate(R.layout.item_session, parent, false);
            }

            TextView sessionText = convertView.findViewById(R.id.sessionText);
            TextView sessionMenuButton = convertView.findViewById(R.id.sessionMenuButton);

            sessionText.setText(sessionDisplayList.get(position));

            if (position >= sessionIdList.size()) {
                sessionMenuButton.setVisibility(View.GONE);
            } else {
                sessionMenuButton.setVisibility(View.VISIBLE);
            }

            sessionMenuButton.setOnClickListener(v -> {
                showSessionMenu(v, position);
            });

            return convertView;
        }
    }
}