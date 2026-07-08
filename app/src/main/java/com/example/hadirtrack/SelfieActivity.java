package com.example.hadirtrack;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

public class SelfieActivity extends AppCompatActivity {

    TextView sessionInfoText;
    ImageView selfieImageView;
    Button takeSelfieButton, submitAttendanceButton, backButton;

    FirebaseAuth auth;
    FirebaseFirestore db;
    FirebaseStorage storage;

    String sessionId, courseId;
    double studentLatitude, studentLongitude;
    float distanceFromClass;

    Bitmap selfieBitmap = null;

    ActivityResultLauncher<String> cameraPermissionLauncher;
    ActivityResultLauncher<Intent> cameraLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selfie);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        sessionInfoText = findViewById(R.id.sessionInfoText);
        selfieImageView = findViewById(R.id.selfieImageView);
        takeSelfieButton = findViewById(R.id.takeSelfieButton);
        submitAttendanceButton = findViewById(R.id.submitAttendanceButton);
        backButton = findViewById(R.id.backButton);

        sessionId = getIntent().getStringExtra("sessionId");
        courseId = getIntent().getStringExtra("courseId");
        studentLatitude = getIntent().getDoubleExtra("studentLatitude", 0.0);
        studentLongitude = getIntent().getDoubleExtra("studentLongitude", 0.0);
        distanceFromClass = getIntent().getFloatExtra("distanceFromClass", 0f);

        sessionInfoText.setText(
                "Location verified.\nDistance from class: "
                        + Math.round(distanceFromClass)
                        + " meters\n\nTake a selfie to complete attendance."
        );

        cameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        openCamera();
                    } else {
                        Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Bundle extras = result.getData().getExtras();

                        if (extras != null) {
                            selfieBitmap = (Bitmap) extras.get("data");
                            selfieImageView.setImageBitmap(selfieBitmap);
                            submitAttendanceButton.setEnabled(true);
                        }
                    }
                }
        );

        takeSelfieButton.setOnClickListener(v -> checkCameraPermission());

        submitAttendanceButton.setOnClickListener(v -> {
            if (selfieBitmap == null) {
                Toast.makeText(this, "Please take selfie first", Toast.LENGTH_SHORT).show();
                return;
            }

            checkDuplicateAttendance();
        });

        backButton.setOnClickListener(v -> finish());
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED) {

            openCamera();

        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void openCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraLauncher.launch(cameraIntent);
    }

    private void checkDuplicateAttendance() {
        String studentUserId = auth.getCurrentUser().getUid();
        String attendanceDocId = sessionId + "_" + studentUserId;

        submitAttendanceButton.setEnabled(false);
        submitAttendanceButton.setText("Checking...");

        db.collection("attendance")
                .document(attendanceDocId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        submitAttendanceButton.setEnabled(true);
                        submitAttendanceButton.setText("Submit Attendance");
                        Toast.makeText(this, "You have already submitted attendance for this session", Toast.LENGTH_LONG).show();
                    } else {
                        uploadSelfie(attendanceDocId, studentUserId);
                    }
                })
                .addOnFailureListener(e -> {
                    submitAttendanceButton.setEnabled(true);
                    submitAttendanceButton.setText("Submit Attendance");
                    Toast.makeText(this, "Failed to check attendance: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void uploadSelfie(String attendanceDocId, String studentUserId) {
        submitAttendanceButton.setText("Uploading selfie...");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        selfieBitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
        byte[] imageData = baos.toByteArray();

        String fileName = "attendance_selfies/" + sessionId + "/" + studentUserId + ".jpg";

        StorageReference selfieRef = storage.getReference().child(fileName);

        selfieRef.putBytes(imageData)
                .addOnSuccessListener(taskSnapshot -> {
                    selfieRef.getDownloadUrl()
                            .addOnSuccessListener(uri -> {
                                String selfieUrl = uri.toString();
                                loadStudentProfileAndSave(attendanceDocId, studentUserId, selfieUrl);
                            })
                            .addOnFailureListener(e -> {
                                submitAttendanceButton.setEnabled(true);
                                submitAttendanceButton.setText("Submit Attendance");
                                Toast.makeText(this, "Failed to get selfie URL: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    submitAttendanceButton.setEnabled(true);
                    submitAttendanceButton.setText("Submit Attendance");
                    Toast.makeText(this, "Failed to upload selfie: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void loadStudentProfileAndSave(String attendanceDocId, String studentUserId, String selfieUrl) {
        submitAttendanceButton.setText("Saving attendance...");

        db.collection("users")
                .document(studentUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        submitAttendanceButton.setEnabled(true);
                        submitAttendanceButton.setText("Submit Attendance");
                        Toast.makeText(this, "Student profile not found", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String studentName = documentSnapshot.getString("name");
                    String studentId = documentSnapshot.getString("studentId");

                    saveAttendance(attendanceDocId, studentUserId, studentName, studentId, selfieUrl);
                })
                .addOnFailureListener(e -> {
                    submitAttendanceButton.setEnabled(true);
                    submitAttendanceButton.setText("Submit Attendance");
                    Toast.makeText(this, "Failed to load student profile: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void saveAttendance(String attendanceDocId, String studentUserId, String studentName, String studentId, String selfieUrl) {
        Map<String, Object> attendance = new HashMap<>();

        attendance.put("sessionId", sessionId);
        attendance.put("courseId", courseId);
        attendance.put("studentUserId", studentUserId);
        attendance.put("studentName", studentName);
        attendance.put("studentId", studentId);
        attendance.put("selfieUrl", selfieUrl);
        attendance.put("selfieCaptured", true);
        attendance.put("latitude", studentLatitude);
        attendance.put("longitude", studentLongitude);
        attendance.put("distanceFromClass", distanceFromClass);
        attendance.put("status", "present");
        attendance.put("timestamp", FieldValue.serverTimestamp());
        attendance.put("createdAt", FieldValue.serverTimestamp());

        db.collection("attendance")
                .document(attendanceDocId)
                .set(attendance)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Attendance submitted successfully", Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    submitAttendanceButton.setEnabled(true);
                    submitAttendanceButton.setText("Submit Attendance");
                    Toast.makeText(this, "Failed to submit attendance: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}