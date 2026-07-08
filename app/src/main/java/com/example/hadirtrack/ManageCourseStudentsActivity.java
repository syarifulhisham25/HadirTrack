package com.example.hadirtrack;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ManageCourseStudentsActivity extends AppCompatActivity {

    TextView courseTitleText, courseSubtitleText;
    EditText studentSearchInput;
    Button addStudentButton, backButton;
    ListView studentListView;

    FirebaseFirestore db;

    String courseId, courseCode, courseName;

    ArrayList<String> studentDisplayList = new ArrayList<>();
    ArrayList<String> enrollmentDocIdList = new ArrayList<>();

    StudentAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_course_students);

        db = FirebaseFirestore.getInstance();

        courseTitleText = findViewById(R.id.courseTitleText);
        courseSubtitleText = findViewById(R.id.courseSubtitleText);
        studentSearchInput = findViewById(R.id.studentSearchInput);
        addStudentButton = findViewById(R.id.addStudentButton);
        backButton = findViewById(R.id.backButton);
        studentListView = findViewById(R.id.studentListView);

        courseId = getIntent().getStringExtra("courseId");
        courseCode = getIntent().getStringExtra("courseCode");
        courseName = getIntent().getStringExtra("courseName");

        courseTitleText.setText("Manage Students");
        courseSubtitleText.setText(courseCode + " - " + courseName);

        adapter = new StudentAdapter(this);
        studentListView.setAdapter(adapter);

        addStudentButton.setOnClickListener(v -> addStudentToCourse());

        backButton.setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadEnrolledStudents();
    }

    private void addStudentToCourse() {
        String keyword = studentSearchInput.getText().toString().trim();

        if (keyword.isEmpty()) {
            studentSearchInput.setError("Enter student ID or email");
            return;
        }

        addStudentButton.setEnabled(false);
        addStudentButton.setText("Searching...");

        db.collection("users")
                .whereEqualTo("studentId", keyword)
                .whereEqualTo("role", "student")
                .get()
                .addOnSuccessListener(studentIdSnapshots -> {
                    if (!studentIdSnapshots.isEmpty()) {
                        QueryDocumentSnapshot studentDoc =
                                (QueryDocumentSnapshot) studentIdSnapshots.getDocuments().get(0);
                        enrollStudent(studentDoc);
                    } else {
                        searchStudentByEmail(keyword);
                    }
                })
                .addOnFailureListener(e -> {
                    addStudentButton.setEnabled(true);
                    addStudentButton.setText("Add Student");
                    Toast.makeText(this, "Failed to search student: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void searchStudentByEmail(String email) {
        db.collection("users")
                .whereEqualTo("email", email)
                .whereEqualTo("role", "student")
                .get()
                .addOnSuccessListener(emailSnapshots -> {
                    if (emailSnapshots.isEmpty()) {
                        addStudentButton.setEnabled(true);
                        addStudentButton.setText("Add Student");
                        Toast.makeText(this, "Student not found", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    QueryDocumentSnapshot studentDoc =
                            (QueryDocumentSnapshot) emailSnapshots.getDocuments().get(0);
                    enrollStudent(studentDoc);
                })
                .addOnFailureListener(e -> {
                    addStudentButton.setEnabled(true);
                    addStudentButton.setText("Add Student");
                    Toast.makeText(this, "Failed to search by email: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void enrollStudent(QueryDocumentSnapshot studentDoc) {
        String studentUserId = studentDoc.getId();
        String enrollmentDocId = courseId + "_" + studentUserId;

        db.collection("course_students")
                .document(enrollmentDocId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        addStudentButton.setEnabled(true);
                        addStudentButton.setText("Add Student");
                        Toast.makeText(this, "Student already enrolled", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String studentName = studentDoc.getString("name");
                    String studentId = studentDoc.getString("studentId");
                    String studentEmail = studentDoc.getString("email");

                    Map<String, Object> enrollment = new HashMap<>();
                    enrollment.put("courseId", courseId);
                    enrollment.put("courseCode", courseCode);
                    enrollment.put("courseName", courseName);
                    enrollment.put("studentUserId", studentUserId);
                    enrollment.put("studentName", studentName);
                    enrollment.put("studentId", studentId);
                    enrollment.put("studentEmail", studentEmail);
                    enrollment.put("enrolledAt", FieldValue.serverTimestamp());

                    db.collection("course_students")
                            .document(enrollmentDocId)
                            .set(enrollment)
                            .addOnSuccessListener(unused -> {
                                addStudentButton.setEnabled(true);
                                addStudentButton.setText("Add Student");
                                studentSearchInput.setText("");
                                Toast.makeText(this, "Student added successfully", Toast.LENGTH_SHORT).show();
                                loadEnrolledStudents();
                            })
                            .addOnFailureListener(e -> {
                                addStudentButton.setEnabled(true);
                                addStudentButton.setText("Add Student");
                                Toast.makeText(this, "Failed to add student: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    addStudentButton.setEnabled(true);
                    addStudentButton.setText("Add Student");
                    Toast.makeText(this, "Failed to check enrollment: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void loadEnrolledStudents() {
        db.collection("course_students")
                .whereEqualTo("courseId", courseId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    studentDisplayList.clear();
                    enrollmentDocIdList.clear();

                    if (queryDocumentSnapshots.isEmpty()) {
                        studentDisplayList.add("No students enrolled yet");
                        adapter.notifyDataSetChanged();
                        return;
                    }

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String enrollmentDocId = document.getId();
                        String studentName = document.getString("studentName");
                        String studentId = document.getString("studentId");
                        String studentEmail = document.getString("studentEmail");

                        enrollmentDocIdList.add(enrollmentDocId);

                        String displayText = studentName
                                + "\nStudent ID: " + studentId
                                + "\nEmail: " + studentEmail;

                        studentDisplayList.add(displayText);
                    }

                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load students: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void showStudentMenu(View anchorView, int position) {
        PopupMenu popupMenu = new PopupMenu(this, anchorView);

        popupMenu.getMenu().add("Remove");

        popupMenu.setOnMenuItemClickListener(item -> {
            if (item.getTitle().toString().equals("Remove")) {
                confirmRemoveStudent(position);
                return true;
            }

            return false;
        });

        popupMenu.show();
    }

    private void confirmRemoveStudent(int position) {
        String enrollmentDocId = enrollmentDocIdList.get(position);

        new AlertDialog.Builder(this)
                .setTitle("Remove Student")
                .setMessage("Remove this student from the course?")
                .setPositiveButton("Remove", (dialog, which) -> removeStudent(enrollmentDocId))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void removeStudent(String enrollmentDocId) {
        db.collection("course_students")
                .document(enrollmentDocId)
                .delete()
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Student removed", Toast.LENGTH_SHORT).show();
                    loadEnrolledStudents();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to remove student: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private class StudentAdapter extends BaseAdapter {

        Context context;

        StudentAdapter(Context context) {
            this.context = context;
        }

        @Override
        public int getCount() {
            return studentDisplayList.size();
        }

        @Override
        public Object getItem(int position) {
            return studentDisplayList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(R.layout.item_enrolled_student, parent, false);
            }

            TextView studentText = convertView.findViewById(R.id.studentText);
            TextView studentMenuButton = convertView.findViewById(R.id.studentMenuButton);

            studentText.setText(studentDisplayList.get(position));

            if (position >= enrollmentDocIdList.size()) {
                studentMenuButton.setVisibility(View.GONE);
            } else {
                studentMenuButton.setVisibility(View.VISIBLE);
            }

            studentMenuButton.setOnClickListener(v -> showStudentMenu(v, position));

            return convertView;
        }
    }
}