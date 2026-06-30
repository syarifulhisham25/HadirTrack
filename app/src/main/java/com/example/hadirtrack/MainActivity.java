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

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

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
                        Log.d("FCM", "Notification permission granted");
                    } else {
                        Log.d("FCM", "Notification permission denied");
                    }
                }
        );

        requestNotificationPermissionIfNeeded();

        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> {
                    Log.d("FCM", "Current FCM Token: " + token);
                })
                .addOnFailureListener(e -> {
                    Log.d("FCM", "Failed to get FCM token: " + e.getMessage());
                });

        registerButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        loginButton.setOnClickListener(v -> {
            loginUser();
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
                                        Intent intent = new Intent(MainActivity.this, LecturerDashboardActivity.class);
                                        startActivity(intent);
                                        finish();
                                    } else if ("student".equals(role)) {
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