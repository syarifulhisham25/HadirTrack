package com.example.hadirtrack;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.Manifest;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.media.AudioAttributes;
import android.net.Uri;

public class MainActivity extends AppCompatActivity {

    EditText emailInput, passwordInput;
    Button loginButton, registerButton;

    FirebaseAuth auth;
    FirebaseFirestore db;

    ActivityResultLauncher<String> notificationPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FirebaseAppCheck firebaseAppCheck = FirebaseAppCheck.getInstance();
        firebaseAppCheck.installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance());

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        createNotificationChannel();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left + 24, systemBars.top + 24, systemBars.right + 24, systemBars.bottom + 24);
            return insets;
        });

        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        loginButton = findViewById(R.id.loginButton);
        registerButton = findViewById(R.id.registerButton);

        notificationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                    } else {
                    }
                }
        );

        requestNotificationPermissionIfNeeded();

        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> {
                })
                .addOnFailureListener(e -> {
                });

        registerButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        loginButton.setOnClickListener(v -> {
            loginUser();
        });

        if (auth.getCurrentUser() != null) {
            checkUserRoleAndRedirect(auth.getCurrentUser().getUid());
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelId = "hadirtrack_urgent_channel";
            String channelName = "Urgent Notifications";
            String channelDescription = "Critical alerts for attendance";

            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    channelName,
                    NotificationManager.IMPORTANCE_HIGH
            );

            channel.setDescription(channelDescription);
            channel.enableLights(true);
            channel.enableVibration(true);
            channel.setLockscreenVisibility(android.app.Notification.VISIBILITY_PUBLIC);

            Uri soundUri = android.provider.Settings.System.DEFAULT_NOTIFICATION_URI;
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            channel.setSound(soundUri, audioAttributes);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private void checkUserRoleAndRedirect(String userId) {
        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String role = documentSnapshot.getString("role");
                        updateFCMToken(userId); // Ensure token is fresh

                        Intent intent;
                        if ("lecturer".equals(role)) {
                            intent = new Intent(MainActivity.this, LecturerDashboardActivity.class);
                        } else {
                            intent = new Intent(MainActivity.this, StudentDashboardActivity.class);
                        }
                        startActivity(intent);
                        finish();
                    }
                });
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED) {

                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    private void updateFCMToken(String userId) {
        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> {
                    db.collection("users").document(userId)
                            .update("fcmToken", token)
                            .addOnSuccessListener(aVoid -> {
                            })
                            .addOnFailureListener(e -> {
                            });
                });
    }

    private void loginUser() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        if (email.isEmpty()) {
            emailInput.setError("Email is required");
            return;
        }

        if (password.isEmpty()) {
            passwordInput.setError("Password is required");
            return;
        }

        loginButton.setEnabled(false);
        loginButton.setText("Logging in...");

        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    String userId = auth.getCurrentUser().getUid();

                    db.collection("users").document(userId).get()
                            .addOnSuccessListener(documentSnapshot -> {
                                loginButton.setEnabled(true);
                                loginButton.setText("Login");

                                if (documentSnapshot.exists()) {
                                    String role = documentSnapshot.getString("role");

                                    if ("lecturer".equals(role)) {
                                        updateFCMToken(userId);
                                        Intent intent = new Intent(MainActivity.this, LecturerDashboardActivity.class);
                                        startActivity(intent);
                                        finish();
                                    } else if ("student".equals(role)) {
                                        updateFCMToken(userId);
                                        Intent intent = new Intent(MainActivity.this, StudentDashboardActivity.class);
                                        startActivity(intent);
                                        finish();
                                    } else {
                                        Toast.makeText(MainActivity.this, "Invalid role", Toast.LENGTH_SHORT).show();
                                    }
                                } else {
                                    Toast.makeText(MainActivity.this, "User profile not found", Toast.LENGTH_SHORT).show();
                                }
                            })
                            .addOnFailureListener(e -> {
                                loginButton.setEnabled(true);
                                loginButton.setText("Login");
                                Toast.makeText(MainActivity.this, "Failed to get user: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    loginButton.setEnabled(true);
                    loginButton.setText("Login");
                    Toast.makeText(MainActivity.this, "Login failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}