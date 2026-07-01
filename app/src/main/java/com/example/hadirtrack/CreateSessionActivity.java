package com.example.hadirtrack;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CreateSessionActivity extends AppCompatActivity {

    TextView courseInfoText, startDateText, startTimeText, endDateText, endTimeText;
    TextView startDateTimeText, endDateTimeText;

    EditText sessionTitleInput, locationNameInput, roomNameInput,
            latitudeInput, longitudeInput, radiusInput, classRemarkInput;

    Button selectStartDateButton, selectStartTimeButton,
            selectEndDateButton, selectEndTimeButton,
            saveSessionButton, backButton,
            useCurrentLocationButton, pickLocationMapButton, captureImage1Button, captureImage2Button, captureImage3Button,
            removeImage1Button, removeImage2Button, removeImage3Button;

    ImageView classImage1View, classImage2View, classImage3View;

    ArrayList<String> existingClassImageUrls = new ArrayList<>();
    boolean removeImage1 = false;
    boolean removeImage2 = false;
    boolean removeImage3 = false;

    FirebaseStorage storage;

    Bitmap classImage1Bitmap = null;
    Bitmap classImage2Bitmap = null;
    Bitmap classImage3Bitmap = null;

    int selectedImageSlot = 0;

    ActivityResultLauncher<String> cameraPermissionLauncher;
    ActivityResultLauncher<Intent> classImageCameraLauncher;

    FusedLocationProviderClient fusedLocationClient;

    ActivityResultLauncher<String> locationPermissionLauncher;
    ActivityResultLauncher<Intent> mapPickerLauncher;

    FirebaseAuth auth;
    FirebaseFirestore db;

    String courseId, courseCode, courseName;
    String sessionId = "";
    boolean isEditMode = false;

    String startDate = "";
    String startTimeOnly = "";
    String endDate = "";
    String endTimeOnly = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_session);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        courseInfoText = findViewById(R.id.courseInfoText);

        sessionTitleInput = findViewById(R.id.sessionTitleInput);
        locationNameInput = findViewById(R.id.locationNameInput);
        roomNameInput = findViewById(R.id.roomNameInput);
        latitudeInput = findViewById(R.id.latitudeInput);
        longitudeInput = findViewById(R.id.longitudeInput);
        radiusInput = findViewById(R.id.radiusInput);

        startDateTimeText = findViewById(R.id.startDateTimeText);
        endDateTimeText = findViewById(R.id.endDateTimeText);

        startDateText = findViewById(R.id.startDateText);
        startTimeText = findViewById(R.id.startTimeText);
        endDateText = findViewById(R.id.endDateText);
        endTimeText = findViewById(R.id.endTimeText);

        selectStartDateButton = findViewById(R.id.selectStartDateButton);
        selectStartTimeButton = findViewById(R.id.selectStartTimeButton);
        selectEndDateButton = findViewById(R.id.selectEndDateButton);
        selectEndTimeButton = findViewById(R.id.selectEndTimeButton);

        saveSessionButton = findViewById(R.id.saveSessionButton);
        backButton = findViewById(R.id.backButton);

        useCurrentLocationButton = findViewById(R.id.useCurrentLocationButton);
        pickLocationMapButton = findViewById(R.id.pickLocationMapButton);

        classImage1View = findViewById(R.id.classImage1View);
        classImage2View = findViewById(R.id.classImage2View);
        classImage3View = findViewById(R.id.classImage3View);

        captureImage1Button = findViewById(R.id.captureImage1Button);
        captureImage2Button = findViewById(R.id.captureImage2Button);
        captureImage3Button = findViewById(R.id.captureImage3Button);

        removeImage1Button = findViewById(R.id.removeImage1Button);
        removeImage2Button = findViewById(R.id.removeImage2Button);
        removeImage3Button = findViewById(R.id.removeImage3Button);

        classRemarkInput = findViewById(R.id.classRemarkInput);

        courseId = getIntent().getStringExtra("courseId");
        courseCode = getIntent().getStringExtra("courseCode");
        courseName = getIntent().getStringExtra("courseName");

        String mode = getIntent().getStringExtra("mode");
        sessionId = getIntent().getStringExtra("sessionId");

        locationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        getCurrentLocationForSession();
                    } else {
                        Toast.makeText(this, "Location permission is required", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        mapPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        double latitude = result.getData().getDoubleExtra("latitude", 0.0);
                        double longitude = result.getData().getDoubleExtra("longitude", 0.0);

                        latitudeInput.setText(String.valueOf(latitude));
                        longitudeInput.setText(String.valueOf(longitude));

                        Toast.makeText(this, "Map location selected", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        useCurrentLocationButton.setOnClickListener(v -> checkLocationPermissionForSession());

        pickLocationMapButton.setOnClickListener(v -> openMapPicker());

        courseInfoText.setText(courseCode + " - " + courseName);

        if ("edit".equals(mode) && sessionId != null && !sessionId.isEmpty()) {
            isEditMode = true;
            saveSessionButton.setText("Update Session");
            loadSessionForEdit();
        }

        selectStartDateButton.setOnClickListener(v -> showDatePicker("start"));
        selectStartTimeButton.setOnClickListener(v -> showTimePicker("start"));
        selectEndDateButton.setOnClickListener(v -> showDatePicker("end"));
        selectEndTimeButton.setOnClickListener(v -> showTimePicker("end"));

        cameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        openClassImageCamera();
                    } else {
                        Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        classImageCameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Bundle extras = result.getData().getExtras();

                        if (extras != null) {
                            Bitmap bitmap = (Bitmap) extras.get("data");

                            if (selectedImageSlot == 1) {
                                classImage1Bitmap = bitmap;
                                classImage1View.setImageBitmap(bitmap);
                            } else if (selectedImageSlot == 2) {
                                classImage2Bitmap = bitmap;
                                classImage2View.setImageBitmap(bitmap);
                            } else if (selectedImageSlot == 3) {
                                classImage3Bitmap = bitmap;
                                classImage3View.setImageBitmap(bitmap);
                            }
                        }
                    }
                }
        );

        captureImage1Button.setOnClickListener(v -> {
            selectedImageSlot = 1;
            checkCameraPermissionForClassImage();
        });

        captureImage2Button.setOnClickListener(v -> {
            selectedImageSlot = 2;
            checkCameraPermissionForClassImage();
        });

        captureImage3Button.setOnClickListener(v -> {
            selectedImageSlot = 3;
            checkCameraPermissionForClassImage();
        });

        removeImage1Button.setOnClickListener(v -> {
            removeImage1 = true;
            classImage1Bitmap = null;
            classImage1View.setImageDrawable(null);
            classImage1View.setBackgroundColor(0xFFDDDDDD);
            Toast.makeText(this, "Image 1 removed", Toast.LENGTH_SHORT).show();
        });

        removeImage2Button.setOnClickListener(v -> {
            removeImage2 = true;
            classImage2Bitmap = null;
            classImage2View.setImageDrawable(null);
            classImage2View.setBackgroundColor(0xFFDDDDDD);
            Toast.makeText(this, "Image 2 removed", Toast.LENGTH_SHORT).show();
        });

        removeImage3Button.setOnClickListener(v -> {
            removeImage3 = true;
            classImage3Bitmap = null;
            classImage3View.setImageDrawable(null);
            classImage3View.setBackgroundColor(0xFFDDDDDD);
            Toast.makeText(this, "Image 3 removed", Toast.LENGTH_SHORT).show();
        });

        saveSessionButton.setOnClickListener(v -> validateAndSaveSession());

        backButton.setOnClickListener(v -> finish());
    }

    private void updateDateTimeDisplay() {
        if (!startDate.isEmpty() && !startTimeOnly.isEmpty()) {
            startDateTimeText.setText(startDate + " " + startTimeOnly);
        } else if (!startDate.isEmpty()) {
            startDateTimeText.setText(startDate + " --:--");
        } else if (!startTimeOnly.isEmpty()) {
            startDateTimeText.setText("---- -- -- " + startTimeOnly);
        } else {
            startDateTimeText.setText("Not selected");
        }

        if (!endDate.isEmpty() && !endTimeOnly.isEmpty()) {
            endDateTimeText.setText(endDate + " " + endTimeOnly);
        } else if (!endDate.isEmpty()) {
            endDateTimeText.setText(endDate + " --:--");
        } else if (!endTimeOnly.isEmpty()) {
            endDateTimeText.setText("---- -- -- " + endTimeOnly);
        } else {
            endDateTimeText.setText("Not selected");
        }
    }

    private void loadSessionForEdit() {
        db.collection("sessions").document(sessionId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        Toast.makeText(this, "Session not found", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    sessionTitleInput.setText(documentSnapshot.getString("sessionTitle"));
                    locationNameInput.setText(documentSnapshot.getString("locationName"));
                    roomNameInput.setText(documentSnapshot.getString("roomName"));

                    String classRemark = documentSnapshot.getString("classRemark");
                    if (classRemark != null) {
                        classRemarkInput.setText(classRemark);
                    }

                    Double latitude = documentSnapshot.getDouble("latitude");
                    Double longitude = documentSnapshot.getDouble("longitude");
                    Long radiusMeter = documentSnapshot.getLong("radiusMeter");

                    if (latitude != null) {
                        latitudeInput.setText(String.valueOf(latitude));
                    }

                    if (longitude != null) {
                        longitudeInput.setText(String.valueOf(longitude));
                    }

                    if (radiusMeter != null) {
                        radiusInput.setText(String.valueOf(radiusMeter));
                    }

                    String startTime = documentSnapshot.getString("startTime");
                    String endTime = documentSnapshot.getString("endTime");

                    if (startTime != null && startTime.length() >= 16) {
                        startDate = startTime.substring(0, 10);
                        startTimeOnly = startTime.substring(11, 16);

                        startDateText.setText("Start Date: " + startDate);
                        startTimeText.setText("Start Time: " + startTimeOnly);
                    }

                    if (endTime != null && endTime.length() >= 16) {
                        endDate = endTime.substring(0, 10);
                        endTimeOnly = endTime.substring(11, 16);

                        endDateText.setText("End Date: " + endDate);
                        endTimeText.setText("End Time: " + endTimeOnly);
                    }

                    List<String> classImageUrls = (List<String>) documentSnapshot.get("classImageUrls");

                    existingClassImageUrls.clear();

                    if (classImageUrls != null) {
                        existingClassImageUrls.addAll(classImageUrls);

                        if (classImageUrls.size() >= 1) {
                            Glide.with(this)
                                    .load(classImageUrls.get(0))
                                    .into(classImage1View);
                        }

                        if (classImageUrls.size() >= 2) {
                            Glide.with(this)
                                    .load(classImageUrls.get(1))
                                    .into(classImage2View);
                        }

                        if (classImageUrls.size() >= 3) {
                            Glide.with(this)
                                    .load(classImageUrls.get(2))
                                    .into(classImage3View);
                        }
                    }

                    updateDateTimeDisplay();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load session: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    finish();
                });
    }

    private void showDatePicker(String type) {
        Calendar calendar = Calendar.getInstance();

        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, selectedYear, selectedMonth, selectedDay) -> {

                    String date = String.format(
                            Locale.getDefault(),
                            "%04d-%02d-%02d",
                            selectedYear,
                            selectedMonth + 1,
                            selectedDay
                    );

                    if (type.equals("start")) {
                        startDate = date;
                        startDateText.setText("Start Date: " + date);
                        updateDateTimeDisplay();
                    } else {
                        endDate = date;
                        endDateText.setText("End Date: " + date);
                        updateDateTimeDisplay();
                    }
                },
                year,
                month,
                day
        );

        datePickerDialog.show();
    }

    private void showTimePicker(String type) {
        Calendar calendar = Calendar.getInstance();

        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                (view, selectedHour, selectedMinute) -> {

                    String time = String.format(
                            Locale.getDefault(),
                            "%02d:%02d",
                            selectedHour,
                            selectedMinute
                    );

                    if (type.equals("start")) {
                        startTimeOnly = time;
                        startTimeText.setText("Start Time: " + time);
                        updateDateTimeDisplay();
                    } else {
                        endTimeOnly = time;
                        endTimeText.setText("End Time: " + time);
                        updateDateTimeDisplay();
                    }
                },
                hour,
                minute,
                true
        );

        timePickerDialog.show();
    }

    private void validateAndSaveSession() {
        String sessionTitle = sessionTitleInput.getText().toString().trim();
        String locationName = locationNameInput.getText().toString().trim();
        String roomName = roomNameInput.getText().toString().trim();
        String latitudeText = latitudeInput.getText().toString().trim();
        String longitudeText = longitudeInput.getText().toString().trim();
        String radiusText = radiusInput.getText().toString().trim();

        if (sessionTitle.isEmpty()) {
            sessionTitleInput.setError("Session title is required");
            return;
        }

        if (locationName.isEmpty()) {
            locationNameInput.setError("Location name is required");
            return;
        }

        if (roomName.isEmpty()) {
            roomNameInput.setError("Room name is required");
            return;
        }

        if (latitudeText.isEmpty()) {
            latitudeInput.setError("Latitude is required");
            return;
        }

        if (longitudeText.isEmpty()) {
            longitudeInput.setError("Longitude is required");
            return;
        }

        if (radiusText.isEmpty()) {
            radiusInput.setError("Radius is required");
            return;
        }

        if (startDate.isEmpty()) {
            Toast.makeText(this, "Please select start date", Toast.LENGTH_SHORT).show();
            return;
        }

        if (startTimeOnly.isEmpty()) {
            Toast.makeText(this, "Please select start time", Toast.LENGTH_SHORT).show();
            return;
        }

        if (endDate.isEmpty()) {
            Toast.makeText(this, "Please select end date", Toast.LENGTH_SHORT).show();
            return;
        }

        if (endTimeOnly.isEmpty()) {
            Toast.makeText(this, "Please select end time", Toast.LENGTH_SHORT).show();
            return;
        }

        double latitude;
        double longitude;
        int radiusMeter;

        try {
            latitude = Double.parseDouble(latitudeText);
            longitude = Double.parseDouble(longitudeText);
            radiusMeter = Integer.parseInt(radiusText);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Latitude, longitude, and radius must be valid numbers", Toast.LENGTH_LONG).show();
            return;
        }

        String startTime = startDate + " " + startTimeOnly;
        String endTime = endDate + " " + endTimeOnly;

        String classRemark = classRemarkInput.getText().toString().trim();

        if (!isEditMode && classImage1Bitmap == null) {
            Toast.makeText(this, "Class image 1 is required", Toast.LENGTH_SHORT).show();
            return;
        }

        uploadClassImagesThenSave(
                sessionTitle,
                locationName,
                roomName,
                latitude,
                longitude,
                radiusMeter,
                startTime,
                endTime,
                classRemark
        );
    }

    private void saveSession(String sessionTitle, String locationName, String roomName,
                             double latitude, double longitude, int radiusMeter,
                             String startTime, String endTime,
                             String classRemark,
                             ArrayList<String> classImageUrls) {

        String lecturerId = auth.getCurrentUser().getUid();

        saveSessionButton.setEnabled(false);
        saveSessionButton.setText("Saving...");

        Map<String, Object> session = new HashMap<>();
        session.put("courseId", courseId);
        session.put("courseCode", courseCode);
        session.put("courseName", courseName);
        session.put("lecturerId", lecturerId);
        session.put("sessionTitle", sessionTitle);
        session.put("locationName", locationName);
        session.put("roomName", roomName);
        session.put("latitude", latitude);
        session.put("longitude", longitude);
        session.put("radiusMeter", radiusMeter);
        session.put("startTime", startTime);
        session.put("endTime", endTime);
        session.put("classPhotoUrl", "");
        session.put("status", "active");
        session.put("createdAt", FieldValue.serverTimestamp());
        session.put("classImageUrls", classImageUrls);
        session.put("classRemark", classRemark);

        db.collection("sessions")
                .add(session)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Session created successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    saveSessionButton.setEnabled(true);
                    saveSessionButton.setText("Save Session");
                    Toast.makeText(this, "Failed to save session: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void updateSession(String sessionTitle, String locationName, String roomName,
                               double latitude, double longitude, int radiusMeter,
                               String startTime, String endTime,
                               String classRemark,
                               ArrayList<String> classImageUrls){

        saveSessionButton.setEnabled(false);
        saveSessionButton.setText("Updating...");

        Map<String, Object> session = new HashMap<>();
        session.put("sessionTitle", sessionTitle);
        session.put("locationName", locationName);
        session.put("roomName", roomName);
        session.put("latitude", latitude);
        session.put("longitude", longitude);
        session.put("radiusMeter", radiusMeter);
        session.put("startTime", startTime);
        session.put("endTime", endTime);
        session.put("updatedAt", FieldValue.serverTimestamp());
        session.put("classRemark", classRemark);

        session.put("classImageUrls", classImageUrls);

        db.collection("sessions")
                .document(sessionId)
                .update(session)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Session updated successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    saveSessionButton.setEnabled(true);
                    saveSessionButton.setText("Update Session");
                    Toast.makeText(this, "Failed to update session: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void checkLocationPermissionForSession() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED) {

            getCurrentLocationForSession();

        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private void getCurrentLocationForSession() {
        useCurrentLocationButton.setEnabled(false);
        useCurrentLocationButton.setText("Getting location...");

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED) {
            useCurrentLocationButton.setEnabled(true);
            useCurrentLocationButton.setText("Use My Current Location");
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    useCurrentLocationButton.setEnabled(true);
                    useCurrentLocationButton.setText("Use My Current Location");

                    if (location == null) {
                        Toast.makeText(this, "Unable to get location. Please turn on GPS and try again.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    latitudeInput.setText(String.valueOf(location.getLatitude()));
                    longitudeInput.setText(String.valueOf(location.getLongitude()));

                    Toast.makeText(this, "Current location selected", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    useCurrentLocationButton.setEnabled(true);
                    useCurrentLocationButton.setText("Use My Current Location");
                    Toast.makeText(this, "Failed to get location: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void openMapPicker() {
        Intent intent = new Intent(CreateSessionActivity.this, MapPickerActivity.class);

        String latitudeText = latitudeInput.getText().toString().trim();
        String longitudeText = longitudeInput.getText().toString().trim();

        if (!latitudeText.isEmpty() && !longitudeText.isEmpty()) {
            try {
                intent.putExtra("currentLatitude", Double.parseDouble(latitudeText));
                intent.putExtra("currentLongitude", Double.parseDouble(longitudeText));
            } catch (NumberFormatException ignored) {
            }
        }

        mapPickerLauncher.launch(intent);
    }

    private void checkCameraPermissionForClassImage() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED) {

            openClassImageCamera();

        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void openClassImageCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        classImageCameraLauncher.launch(cameraIntent);
    }

    private void uploadClassImagesThenSave(
            String sessionTitle,
            String locationName,
            String roomName,
            double latitude,
            double longitude,
            int radiusMeter,
            String startTime,
            String endTime,
            String classRemark
    ) {
        saveSessionButton.setEnabled(false);
        saveSessionButton.setText("Uploading images...");

        ArrayList<Bitmap> imageBitmaps = new ArrayList<>();

        if (classImage1Bitmap != null) {
            imageBitmaps.add(classImage1Bitmap);
        }

        if (classImage2Bitmap != null) {
            imageBitmaps.add(classImage2Bitmap);
        }

        if (classImage3Bitmap != null) {
            imageBitmaps.add(classImage3Bitmap);
        }

        ArrayList<String> uploadedUrls = new ArrayList<>();

        if (imageBitmaps.isEmpty()) {
            if (isEditMode) {
                ArrayList<String> remainingUrls = getRemainingExistingImageUrls();

                if (remainingUrls.isEmpty()) {
                    Toast.makeText(this, "At least 1 class image is required", Toast.LENGTH_SHORT).show();
                    saveSessionButton.setEnabled(true);
                    saveSessionButton.setText("Update Session");
                    return;
                }

                updateSession(
                        sessionTitle,
                        locationName,
                        roomName,
                        latitude,
                        longitude,
                        radiusMeter,
                        startTime,
                        endTime,
                        classRemark,
                        remainingUrls
                );
            } else {
                Toast.makeText(this, "Please capture at least 1 class image", Toast.LENGTH_SHORT).show();
                saveSessionButton.setEnabled(true);
                saveSessionButton.setText("Save Session");
            }
            return;
        }

        uploadImageAtIndex(
                imageBitmaps,
                uploadedUrls,
                0,
                sessionTitle,
                locationName,
                roomName,
                latitude,
                longitude,
                radiusMeter,
                startTime,
                endTime,
                classRemark
        );
    }

    private void uploadImageAtIndex(
            ArrayList<Bitmap> imageBitmaps,
            ArrayList<String> uploadedUrls,
            int index,
            String sessionTitle,
            String locationName,
            String roomName,
            double latitude,
            double longitude,
            int radiusMeter,
            String startTime,
            String endTime,
            String classRemark
    ) {
        if (index >= imageBitmaps.size()) {
            if (isEditMode) {
                ArrayList<String> finalUrls = getRemainingExistingImageUrls();
                finalUrls.addAll(uploadedUrls);

                if (finalUrls.size() > 3) {
                    Toast.makeText(this, "Maximum 3 class images only. Remove old image first.", Toast.LENGTH_LONG).show();
                    saveSessionButton.setEnabled(true);
                    saveSessionButton.setText("Update Session");
                    return;
                }

                if (finalUrls.isEmpty()) {
                    Toast.makeText(this, "At least 1 class image is required", Toast.LENGTH_SHORT).show();
                    saveSessionButton.setEnabled(true);
                    saveSessionButton.setText("Update Session");
                    return;
                }

                updateSession(
                        sessionTitle,
                        locationName,
                        roomName,
                        latitude,
                        longitude,
                        radiusMeter,
                        startTime,
                        endTime,
                        classRemark,
                        finalUrls
                );
            } else {
                saveSession(sessionTitle, locationName, roomName, latitude, longitude, radiusMeter, startTime, endTime, classRemark, uploadedUrls);
            }
            return;
        }

        Bitmap bitmap = imageBitmaps.get(index);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
        byte[] imageData = baos.toByteArray();

        String fileName = "class_images/" + courseId + "/" + System.currentTimeMillis() + "_" + (index + 1) + ".jpg";

        StorageReference imageRef = storage.getReference().child(fileName);

        imageRef.putBytes(imageData)
                .addOnSuccessListener(taskSnapshot -> {
                    imageRef.getDownloadUrl()
                            .addOnSuccessListener(uri -> {
                                uploadedUrls.add(uri.toString());

                                uploadImageAtIndex(
                                        imageBitmaps,
                                        uploadedUrls,
                                        index + 1,
                                        sessionTitle,
                                        locationName,
                                        roomName,
                                        latitude,
                                        longitude,
                                        radiusMeter,
                                        startTime,
                                        endTime,
                                        classRemark
                                );
                            })
                            .addOnFailureListener(e -> {
                                saveSessionButton.setEnabled(true);
                                saveSessionButton.setText(isEditMode ? "Update Session" : "Save Session");
                                Toast.makeText(this, "Failed to get image URL: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    saveSessionButton.setEnabled(true);
                    saveSessionButton.setText(isEditMode ? "Update Session" : "Save Session");
                    Toast.makeText(this, "Failed to upload image: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private ArrayList<String> getRemainingExistingImageUrls() {
        ArrayList<String> finalUrls = new ArrayList<>();

        for (int i = 0; i < existingClassImageUrls.size(); i++) {
            if (i == 0 && removeImage1) continue;
            if (i == 1 && removeImage2) continue;
            if (i == 2 && removeImage3) continue;

            finalUrls.add(existingClassImageUrls.get(i));
        }

        return finalUrls;
    }
}