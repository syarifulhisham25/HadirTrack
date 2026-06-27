package com.example.hadirtrack;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import android.content.Intent;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class SelfieActivity extends AppCompatActivity {

    TextView sessionInfoText;
    ImageView selfieImageView;
    Button takeSelfieButton, submitAttendanceButton, backButton;

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

            Toast.makeText(this, "Next: upload selfie and save attendance", Toast.LENGTH_SHORT).show();
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
}