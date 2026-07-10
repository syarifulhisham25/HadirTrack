package com.example.hadirtrack;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    TextView roleText;
    EditText nameInput, emailInput, phoneInput, studentIdInput, staffIdInput;
    Button updateProfileButton, backButton;

    FirebaseAuth auth;
    FirebaseFirestore db;

    String userId = "";
    String role = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        role = getIntent().getStringExtra("role");

        Log.d("PROFILE_DEBUG", "Role from Intent: " + role);

        if ("student".equalsIgnoreCase(role)) {
            setContentView(R.layout.activity_profile_student);
        } else if ("lecturer".equalsIgnoreCase(role)) {
            setContentView(R.layout.activity_profile);
        } else {
            Toast.makeText(this, "Role not received", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        roleText = findViewById(R.id.roleText);
        nameInput = findViewById(R.id.nameInput);
        emailInput = findViewById(R.id.emailInput);
        phoneInput = findViewById(R.id.phoneInput);
        studentIdInput = findViewById(R.id.studentIdInput);
        staffIdInput = findViewById(R.id.staffIdInput);
        updateProfileButton = findViewById(R.id.updateProfileButton);
        backButton = findViewById(R.id.backButton);

        userId = auth.getCurrentUser().getUid();

        loadProfile();

        updateProfileButton.setOnClickListener(v -> validateAndUpdateProfile());

        backButton.setOnClickListener(v -> finish());
    }

    private void loadProfile() {
        db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        Toast.makeText(this, "Profile not found", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    String name = documentSnapshot.getString("name");
                    String email = documentSnapshot.getString("email");
                    String phone = documentSnapshot.getString("phone");
                    String studentId = documentSnapshot.getString("studentId");
                    String staffId = documentSnapshot.getString("staffId");
                    role = documentSnapshot.getString("role");

                    nameInput.setText(name);
                    emailInput.setText(email);
                    phoneInput.setText(phone);

                    if ("student".equals(role)) {
                        roleText.setText("Role: Student");
                        studentIdInput.setVisibility(View.VISIBLE);
                        staffIdInput.setVisibility(View.GONE);
                        studentIdInput.setText(studentId);
                    } else if ("lecturer".equals(role)) {
                        roleText.setText("Role: Lecturer");
                        studentIdInput.setVisibility(View.GONE);
                        staffIdInput.setVisibility(View.VISIBLE);
                        staffIdInput.setText(staffId);
                    } else {
                        roleText.setText("Role: Unknown");
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load profile: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void validateAndUpdateProfile() {
        String name = nameInput.getText().toString().trim();
        String phone = phoneInput.getText().toString().trim();
        String studentId = studentIdInput.getText().toString().trim();
        String staffId = staffIdInput.getText().toString().trim();

        if (name.isEmpty()) {
            nameInput.setError("Name is required");
            return;
        }

        if (phone.isEmpty()) {
            phoneInput.setError("Phone is required");
            return;
        }

        if ("student".equals(role) && studentId.isEmpty()) {
            studentIdInput.setError("Student ID is required");
            return;
        }

        if ("lecturer".equals(role) && staffId.isEmpty()) {
            staffIdInput.setError("Staff ID is required");
            return;
        }

        updateProfile(name, phone, studentId, staffId);
    }

    private void updateProfile(String name, String phone, String studentId, String staffId) {
        updateProfileButton.setEnabled(false);
        updateProfileButton.setText("Updating...");

        Map<String, Object> profile = new HashMap<>();
        profile.put("name", name);
        profile.put("phone", phone);
        profile.put("updatedAt", FieldValue.serverTimestamp());

        if ("student".equals(role)) {
            profile.put("studentId", studentId);
        } else if ("lecturer".equals(role)) {
            profile.put("staffId", staffId);
        }

        db.collection("users")
                .document(userId)
                .update(profile)
                .addOnSuccessListener(unused -> {
                    updateRelatedData(name, studentId);
                })
                .addOnFailureListener(e -> {
                    updateProfileButton.setEnabled(true);
                    updateProfileButton.setText("Update Profile");
                    Toast.makeText(this, "Failed to update profile: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void updateRelatedData(String name, String studentId) {
        if ("lecturer".equals(role)) {
            updateLecturerNameInCourses(name);
        } else if ("student".equals(role)) {
            updateStudentNameInAttendance(name, studentId);
        } else {
            finishAfterUpdate();
        }
    }

    private void updateLecturerNameInCourses(String lecturerName) {
        db.collection("courses")
                .whereEqualTo("lecturerId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        finishAfterUpdate();
                        return;
                    }

                    for (int i = 0; i < queryDocumentSnapshots.size(); i++) {
                        queryDocumentSnapshots.getDocuments().get(i).getReference()
                                .update("lecturerName", lecturerName);
                    }

                    finishAfterUpdate();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Profile updated, but failed to update courses", Toast.LENGTH_LONG).show();
                    finishAfterUpdate();
                });
    }

    private void updateStudentNameInAttendance(String studentName, String studentId) {
        db.collection("attendance")
                .whereEqualTo("studentUserId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        finishAfterUpdate();
                        return;
                    }

                    for (int i = 0; i < queryDocumentSnapshots.size(); i++) {
                        queryDocumentSnapshots.getDocuments().get(i).getReference()
                                .update("studentName", studentName, "studentId", studentId);
                    }

                    finishAfterUpdate();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Profile updated, but failed to update attendance records", Toast.LENGTH_LONG).show();
                    finishAfterUpdate();
                });
    }

    private void finishAfterUpdate() {
        Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
        finish();
    }
}