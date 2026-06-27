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
    String courseId = "";
    boolean isEditMode = false;

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

        String mode = getIntent().getStringExtra("mode");
        courseId = getIntent().getStringExtra("courseId");

        if ("edit".equals(mode) && courseId != null && !courseId.isEmpty()) {
            isEditMode = true;
            saveCourseButton.setText("Update Course");
            loadCourseForEdit();
        } else {
            loadLecturerName();
        }

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

    private void loadCourseForEdit() {
        db.collection("courses").document(courseId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        Toast.makeText(this, "Course not found", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    courseCodeInput.setText(documentSnapshot.getString("courseCode"));
                    courseNameInput.setText(documentSnapshot.getString("courseName"));
                    groupNameInput.setText(documentSnapshot.getString("groupName"));
                    semesterInput.setText(documentSnapshot.getString("semester"));

                    lecturerName = documentSnapshot.getString("lecturerName");
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load course: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    finish();
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

        if (isEditMode) {
            updateCourse(courseCode, courseName, groupName, semester);
        } else {
            saveCourse(courseCode, courseName, groupName, semester);
        }
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

    private void updateCourse(String courseCode, String courseName, String groupName, String semester) {
        saveCourseButton.setEnabled(false);
        saveCourseButton.setText("Updating...");

        Map<String, Object> course = new HashMap<>();
        course.put("courseCode", courseCode);
        course.put("courseName", courseName);
        course.put("groupName", groupName);
        course.put("semester", semester);
        course.put("updatedAt", FieldValue.serverTimestamp());

        db.collection("courses")
                .document(courseId)
                .update(course)
                .addOnSuccessListener(unused -> {
                    updateRelatedSessions(courseCode, courseName);
                })
                .addOnFailureListener(e -> {
                    saveCourseButton.setEnabled(true);
                    saveCourseButton.setText("Update Course");
                    Toast.makeText(this, "Failed to update course: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void updateRelatedSessions(String courseCode, String courseName) {
        db.collection("sessions")
                .whereEqualTo("courseId", courseId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    if (queryDocumentSnapshots.isEmpty()) {
                        Toast.makeText(this, "Course updated successfully", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    for (int i = 0; i < queryDocumentSnapshots.size(); i++) {
                        queryDocumentSnapshots.getDocuments().get(i).getReference()
                                .update("courseCode", courseCode, "courseName", courseName);
                    }

                    Toast.makeText(this, "Course updated successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Course updated, but failed to update sessions", Toast.LENGTH_LONG).show();
                    finish();
                });
    }
}