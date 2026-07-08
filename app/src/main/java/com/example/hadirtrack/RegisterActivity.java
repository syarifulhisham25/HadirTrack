package com.example.hadirtrack;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    FirebaseAuth auth;
    FirebaseFirestore db;
    EditText nameInput, emailInput, passwordInput, phoneInput, studentIdInput, staffIdInput;
    RadioGroup roleGroup;
    RadioButton studentRadio, lecturerRadio;
    Button createAccountButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left + 24, systemBars.top + 24, systemBars.right + 24, systemBars.bottom + 24);
            return insets;
        });

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        nameInput = findViewById(R.id.nameInput);
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        phoneInput = findViewById(R.id.phoneInput);
        studentIdInput = findViewById(R.id.studentIdInput);
        staffIdInput = findViewById(R.id.staffIdInput);

        roleGroup = findViewById(R.id.roleGroup);
        studentRadio = findViewById(R.id.studentRadio);
        lecturerRadio = findViewById(R.id.lecturerRadio);

        createAccountButton = findViewById(R.id.createAccountButton);

        roleGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.studentRadio) {
                studentIdInput.setVisibility(View.VISIBLE);
                staffIdInput.setVisibility(View.GONE);
            } else if (checkedId == R.id.lecturerRadio) {
                studentIdInput.setVisibility(View.GONE);
                staffIdInput.setVisibility(View.VISIBLE);
            }
        });

        createAccountButton.setOnClickListener(v -> {
            validateRegisterForm();
        });
    }

    private void validateRegisterForm() {
        String name = nameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        String phone = phoneInput.getText().toString().trim();

        String role = studentRadio.isChecked() ? "student" : "lecturer";
        String studentId = studentIdInput.getText().toString().trim();
        String staffId = staffIdInput.getText().toString().trim();

        if (name.isEmpty()) {
            nameInput.setError("Name is required");
            return;
        }

        if (email.isEmpty()) {
            emailInput.setError("Email is required");
            return;
        }

        if (password.isEmpty()) {
            passwordInput.setError("Password is required");
            return;
        }

        if (password.length() < 6) {
            passwordInput.setError("Password must be at least 6 characters");
            return;
        }

        if (phone.isEmpty()) {
            phoneInput.setError("Phone is required");
            return;
        }

        if (role.equals("student") && studentId.isEmpty()) {
            studentIdInput.setError("Student ID is required");
            return;
        }

        if (role.equals("lecturer") && staffId.isEmpty()) {
            staffIdInput.setError("Staff ID is required");
            return;
        }

        registerUser(name, email, password, phone, role, studentId, staffId);
    }

    private void registerUser(String name, String email, String password, String phone,
                              String role, String studentId, String staffId) {

        createAccountButton.setEnabled(false);
        createAccountButton.setText("Creating Account...");

        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {

                    String userId = auth.getCurrentUser().getUid();

                    Map<String, Object> user = new HashMap<>();
                    user.put("name", name);
                    user.put("email", email);
                    user.put("role", role);
                    user.put("phone", phone);
                    user.put("createdAt", FieldValue.serverTimestamp());

                    if (role.equals("student")) {
                        user.put("studentId", studentId);
                        user.put("staffId", "");
                    } else {
                        user.put("studentId", "");
                        user.put("staffId", staffId);
                    }

                    db.collection("users")
                            .document(userId)
                            .set(user)
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(RegisterActivity.this, "Account created successfully", Toast.LENGTH_SHORT).show();
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                createAccountButton.setEnabled(true);
                                createAccountButton.setText("Create Account");
                                Toast.makeText(RegisterActivity.this, "Failed to save user: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });

                })
                .addOnFailureListener(e -> {
                    createAccountButton.setEnabled(true);
                    createAccountButton.setText("Create Account");
                    Toast.makeText(RegisterActivity.this, "Register failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}