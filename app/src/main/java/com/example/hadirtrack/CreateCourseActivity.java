package com.example.hadirtrack;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class CreateCourseActivity extends AppCompatActivity {

    EditText courseCodeInput, courseNameInput, groupNameInput, semesterInput;
    Button saveCourseButton, backButton;

    FirebaseAuth auth;
    FirebaseFirestore db;

    String lecturerName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_course);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        courseCodeInput = findViewById(R.id.courseCodeInput);
        courseNameInput = findViewById(R.id.courseNameInput);
        groupNameInput = findViewById(R.id.groupNameInput);
        semesterInput = findViewById(R.id.semesterInput);
        saveCourseButton = findViewById(R.id.saveCourseButton);
        backButton = findViewById(R.id.backButton);

        loadLecturerName();

        saveCourseButton.setOnClickListener(v -> validateAndSaveCourse());

        backButton.setOnClickListener(v -> finish());
    }

    private void loadLecturerName() {
        String lecturerId = auth.getCurrentUser().getUid();

        db.collection("users").document(lecturerId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        lecturerName = documentSnapshot.getString("name");
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load lecturer name", Toast.LENGTH_SHORT).show();
                });
    }

    private void validateAndSaveCourse() {
        String courseCode = courseCodeInput.getText().toString().trim();
        String courseName = courseNameInput.getText().toString().trim();
        String groupName = groupNameInput.getText().toString().trim();
        String semester = semesterInput.getText().toString().trim();

        if (courseCode.isEmpty()) {
            courseCodeInput.setError("Course code is required");
            return;
        }

        if (courseName.isEmpty()) {
            courseNameInput.setError("Course name is required");
            return;
        }

        if (groupName.isEmpty()) {
            groupNameInput.setError("Group name is required");
            return;
        }

        if (semester.isEmpty()) {
            semesterInput.setError("Semester is required");
            return;
        }

        saveCourse(courseCode, courseName, groupName, semester);
    }

    private void saveCourse(String courseCode, String courseName, String groupName, String semester) {
        String lecturerId = auth.getCurrentUser().getUid();

        saveCourseButton.setEnabled(false);
        saveCourseButton.setText("Saving...");

        Map<String, Object> course = new HashMap<>();
        course.put("courseCode", courseCode);
        course.put("courseName", courseName);
        course.put("lecturerId", lecturerId);
        course.put("lecturerName", lecturerName);
        course.put("groupName", groupName);
        course.put("semester", semester);
        course.put("createdAt", FieldValue.serverTimestamp());

        db.collection("courses")
                .add(course)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Course created successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    saveCourseButton.setEnabled(true);
                    saveCourseButton.setText("Save Course");
                    Toast.makeText(this, "Failed to save course: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}