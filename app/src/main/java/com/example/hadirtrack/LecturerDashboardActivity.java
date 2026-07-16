package com.example.hadirtrack;

import android.app.AlertDialog;
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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

public class LecturerDashboardActivity extends AppCompatActivity {

    Button addCourseButton;
    ListView courseListView;
    SwipeRefreshLayout swipeRefreshLayout;
    View coursesTab, profileButton, logoutButton;

    FirebaseAuth auth;
    FirebaseFirestore db;

    ArrayList<String> courseIdList = new ArrayList<>();
    ArrayList<String> courseCodeList = new ArrayList<>();
    ArrayList<String> courseNameList = new ArrayList<>();
    ArrayList<String> groupList = new ArrayList<>();
    ArrayList<String> semesterList = new ArrayList<>();
    ArrayList<String> courseDisplayList = new ArrayList<>();

    CourseAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lecturer_dashboard);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        addCourseButton = findViewById(R.id.addCourseButton);
        courseListView = findViewById(R.id.courseListView);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        coursesTab = findViewById(R.id.coursesTab);
        profileButton = findViewById(R.id.profileButton);
        logoutButton = findViewById(R.id.logoutButton);

        adapter = new CourseAdapter();
        courseListView.setAdapter(adapter);

        addCourseButton.setOnClickListener(v -> {
            Intent intent = new Intent(LecturerDashboardActivity.this, CreateCourseActivity.class);
            startActivity(intent);
        });

        coursesTab.setOnClickListener(v -> {
            Toast.makeText(this, "You are already viewing courses", Toast.LENGTH_SHORT).show();
        });

        profileButton.setOnClickListener(v -> {
            Intent intent = new Intent(LecturerDashboardActivity.this, ProfileActivity.class);
            intent.putExtra("role", "lecturer");
            startActivity(intent);
        });

        logoutButton.setOnClickListener(v -> {
            auth.signOut();
            Intent intent = new Intent(LecturerDashboardActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });

        courseListView.setOnItemClickListener((parent, view, position, id) -> openCourse(position));

        swipeRefreshLayout.setOnRefreshListener(this::loadCourses);

        handleNotificationIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleNotificationIntent(intent);
    }

    private void handleNotificationIntent(Intent intent) {
        if (intent != null && intent.hasExtra("sessionId")) {
            String sessionId = intent.getStringExtra("sessionId");
            String courseId = intent.getStringExtra("courseId");
            String courseCode = intent.getStringExtra("courseCode");
            String courseName = intent.getStringExtra("courseName");

            if (sessionId != null && !sessionId.isEmpty()) {
                Intent attendIntent = new Intent(this, AttendanceListActivity.class);
                attendIntent.putExtra("sessionId", sessionId);
                attendIntent.putExtra("courseId", courseId);
                attendIntent.putExtra("courseCode", courseCode);
                attendIntent.putExtra("courseName", courseName);
                startActivity(attendIntent);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCourses();
    }

    private void loadCourses() {
        if (auth.getCurrentUser() == null) {
            if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
            return;
        }

        String lecturerId = auth.getCurrentUser().getUid();

        db.collection("courses")
                .whereEqualTo("lecturerId", lecturerId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                    courseIdList.clear();
                    courseCodeList.clear();
                    courseNameList.clear();
                    groupList.clear();
                    semesterList.clear();
                    courseDisplayList.clear();

                    if (queryDocumentSnapshots.isEmpty()) {
                        courseDisplayList.add("No courses yet.\nTap Add Course to create your first course.");
                        adapter.notifyDataSetChanged();
                        return;
                    }

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String courseId = document.getId();
                        String courseCode = document.getString("courseCode");
                        String courseName = document.getString("courseName");
                        String group = document.getString("groupName");
                        String semester = document.getString("semester");

                        courseIdList.add(courseId);
                        courseCodeList.add(courseCode);
                        courseNameList.add(courseName);
                        groupList.add(group);
                        semesterList.add(semester);

                        String displayText = courseCode + " - " + courseName
                                + "\nGroup: " + group
                                + "\nSemester: " + semester;

                        courseDisplayList.add(displayText);
                    }

                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                    Toast.makeText(this, "Failed to load courses: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void openCourse(int position) {
        if (position >= courseIdList.size()) {
            return;
        }

        Intent intent = new Intent(LecturerDashboardActivity.this, SessionListActivity.class);
        intent.putExtra("courseId", courseIdList.get(position));
        intent.putExtra("courseCode", courseCodeList.get(position));
        intent.putExtra("courseName", courseNameList.get(position));
        startActivity(intent);
    }

    private void showCourseMenu(View anchorView, int position) {
        if (position >= courseIdList.size()) {
            return;
        }

        PopupMenu popupMenu = new PopupMenu(this, anchorView);

        popupMenu.getMenu().add("Open");
        popupMenu.getMenu().add("Update");
        popupMenu.getMenu().add("Delete");

        popupMenu.setOnMenuItemClickListener(item -> {
            String selected = item.getTitle().toString();

            if (selected.equals("Open")) {
                openCourse(position);
                return true;
            }

            if (selected.equals("Update")) {
                Intent intent = new Intent(LecturerDashboardActivity.this, CreateCourseActivity.class);
                intent.putExtra("mode", "edit");
                intent.putExtra("courseId", courseIdList.get(position));
                intent.putExtra("courseCode", courseCodeList.get(position));
                intent.putExtra("courseName", courseNameList.get(position));
                intent.putExtra("groupName", groupList.get(position));
                intent.putExtra("semester", semesterList.get(position));
                startActivity(intent);
                return true;
            }

            if (selected.equals("Delete")) {
                confirmDeleteCourse(position);
                return true;
            }

            return false;
        });

        popupMenu.show();
    }

    private void confirmDeleteCourse(int position) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Course")
                .setMessage("Are you sure you want to delete this course?")
                .setPositiveButton("Delete", (dialog, which) -> deleteCourse(position))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteCourse(int position) {
        String courseId = courseIdList.get(position);

        db.collection("courses")
                .document(courseId)
                .delete()
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Course deleted", Toast.LENGTH_SHORT).show();
                    loadCourses();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to delete course: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private class CourseAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return courseDisplayList.size();
        }

        @Override
        public Object getItem(int position) {
            return courseDisplayList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(LecturerDashboardActivity.this)
                        .inflate(R.layout.item_course, parent, false);
            }

            TextView courseText = convertView.findViewById(R.id.courseText);
            TextView courseMenuButton = convertView.findViewById(R.id.courseMenuButton);

            courseText.setText(courseDisplayList.get(position));

            if (position >= courseIdList.size()) {
                courseMenuButton.setVisibility(View.GONE);
            } else {
                courseMenuButton.setVisibility(View.VISIBLE);
                courseMenuButton.setOnClickListener(v -> showCourseMenu(v, position));
            }

            return convertView;
        }
    }
}