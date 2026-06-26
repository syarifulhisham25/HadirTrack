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

public class CourseListActivity extends AppCompatActivity {

    Button addCourseButton;
    ListView courseListView;

    FirebaseAuth auth;
    FirebaseFirestore db;

    ArrayList<String> courseDisplayList = new ArrayList<>();
    ArrayList<String> courseIdList = new ArrayList<>();
    ArrayList<String> courseCodeList = new ArrayList<>();
    ArrayList<String> courseNameList = new ArrayList<>();

    ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_course_list);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        addCourseButton = findViewById(R.id.addCourseButton);
        courseListView = findViewById(R.id.courseListView);

        adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                courseDisplayList
        );

        courseListView.setAdapter(adapter);

        addCourseButton.setOnClickListener(v -> {
            Intent intent = new Intent(CourseListActivity.this, CreateCourseActivity.class);
            startActivity(intent);
        });

        courseListView.setOnItemClickListener((parent, view, position, id) -> {
            Intent intent = new Intent(CourseListActivity.this, SessionListActivity.class);
            intent.putExtra("courseId", courseIdList.get(position));
            intent.putExtra("courseCode", courseCodeList.get(position));
            intent.putExtra("courseName", courseNameList.get(position));
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCourses();
    }

    private void loadCourses() {
        String lecturerId = auth.getCurrentUser().getUid();

        db.collection("courses")
                .whereEqualTo("lecturerId", lecturerId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    courseDisplayList.clear();
                    courseIdList.clear();
                    courseCodeList.clear();
                    courseNameList.clear();

                    if (queryDocumentSnapshots.isEmpty()) {
                        courseDisplayList.add("No courses yet");
                        adapter.notifyDataSetChanged();
                        return;
                    }

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String courseId = document.getId();
                        String courseCode = document.getString("courseCode");
                        String courseName = document.getString("courseName");
                        String groupName = document.getString("groupName");
                        String semester = document.getString("semester");

                        courseIdList.add(courseId);
                        courseCodeList.add(courseCode);
                        courseNameList.add(courseName);

                        String displayText = courseCode + " - " + courseName
                                + "\nGroup: " + groupName
                                + "\nSemester: " + semester;

                        courseDisplayList.add(displayText);
                    }

                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load courses: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}