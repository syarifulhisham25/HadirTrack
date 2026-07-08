package com.example.hadirtrack;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;

public class CourseListActivity extends AppCompatActivity {

    Button addCourseButton;
    ListView courseListView;

    FirebaseAuth auth;
    FirebaseFirestore db;

    ArrayList<String> courseDisplayList = new ArrayList<>();
    ArrayList<String> courseIdList = new ArrayList<>();
    ArrayList<String> courseCodeList = new ArrayList<>();
    ArrayList<String> courseNameList = new ArrayList<>();

    CourseAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_course_list);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        addCourseButton = findViewById(R.id.addCourseButton);
        courseListView = findViewById(R.id.courseListView);

        adapter = new CourseAdapter(this);
        courseListView.setAdapter(adapter);

        addCourseButton.setOnClickListener(v -> {
            Intent intent = new Intent(CourseListActivity.this, CreateCourseActivity.class);
            startActivity(intent);
        });

        courseListView.setOnItemClickListener((parent, view, position, id) -> {
            if (position >= courseIdList.size()) {
                return;
            }

            Intent intent = new Intent(CourseListActivity.this, SessionListActivity.class);
            intent.putExtra("courseId", courseIdList.get(position));
            intent.putExtra("courseCode", courseCodeList.get(position));
            intent.putExtra("courseName", courseNameList.get(position));
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCourses();
    }

    private void loadCourses() {
        String lecturerId = auth.getCurrentUser().getUid();

        db.collection("courses")
                .whereEqualTo("lecturerId", lecturerId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    courseDisplayList.clear();
                    courseIdList.clear();
                    courseCodeList.clear();
                    courseNameList.clear();

                    if (queryDocumentSnapshots.isEmpty()) {
                        courseDisplayList.add("No courses yet");
                        adapter.notifyDataSetChanged();
                        return;
                    }

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String courseId = document.getId();
                        String courseCode = document.getString("courseCode");
                        String courseName = document.getString("courseName");
                        String groupName = document.getString("groupName");
                        String semester = document.getString("semester");

                        courseIdList.add(courseId);
                        courseCodeList.add(courseCode);
                        courseNameList.add(courseName);

                        String displayText = courseCode + " - " + courseName
                                + "\nGroup: " + groupName
                                + "\nSemester: " + semester;

                        courseDisplayList.add(displayText);
                    }

                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load courses: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void showCourseMenu(View anchorView, int position) {
        PopupMenu popupMenu = new PopupMenu(this, anchorView);

        popupMenu.getMenu().add("Open");
        popupMenu.getMenu().add("Update");
        popupMenu.getMenu().add("Delete");

        popupMenu.setOnMenuItemClickListener(item -> {
            String title = item.getTitle().toString();

            if (title.equals("Open")) {
                openCourse(position);
                return true;
            }

            if (title.equals("Update")) {
                openEditCourse(position);
                return true;
            }

            if (title.equals("Delete")) {
                confirmDeleteCourse(position);
                return true;
            }

            return false;
        });

        popupMenu.show();
    }

    private void openCourse(int position) {
        Intent intent = new Intent(CourseListActivity.this, SessionListActivity.class);
        intent.putExtra("courseId", courseIdList.get(position));
        intent.putExtra("courseCode", courseCodeList.get(position));
        intent.putExtra("courseName", courseNameList.get(position));
        startActivity(intent);
    }

    private void openEditCourse(int position) {
        Intent intent = new Intent(CourseListActivity.this, CreateCourseActivity.class);
        intent.putExtra("mode", "edit");
        intent.putExtra("courseId", courseIdList.get(position));
        startActivity(intent);
    }

    private void confirmDeleteCourse(int position) {
        String courseId = courseIdList.get(position);
        String courseCode = courseCodeList.get(position);

        new AlertDialog.Builder(this)
                .setTitle("Delete Course")
                .setMessage("Are you sure you want to delete " + courseCode + "? This will also delete related sessions and attendance records.")
                .setPositiveButton("Delete", (dialog, which) -> deleteCourse(courseId))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteCourse(String courseId) {
        db.collection("sessions")
                .whereEqualTo("courseId", courseId)
                .get()
                .addOnSuccessListener(sessionSnapshots -> {

                    db.collection("attendance")
                            .whereEqualTo("courseId", courseId)
                            .get()
                            .addOnSuccessListener(attendanceSnapshots -> {

                                WriteBatch batch = db.batch();

                                for (QueryDocumentSnapshot sessionDoc : sessionSnapshots) {
                                    batch.delete(sessionDoc.getReference());
                                }

                                for (QueryDocumentSnapshot attendanceDoc : attendanceSnapshots) {
                                    batch.delete(attendanceDoc.getReference());
                                }

                                batch.delete(db.collection("courses").document(courseId));

                                batch.commit()
                                        .addOnSuccessListener(unused -> {
                                            Toast.makeText(this, "Course deleted successfully", Toast.LENGTH_SHORT).show();
                                            loadCourses();
                                        })
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(this, "Failed to delete course: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                        });

                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Failed to check attendance records: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });

                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to check sessions: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private class CourseAdapter extends BaseAdapter {

        Context context;

        CourseAdapter(Context context) {
            this.context = context;
        }

        @Override
        public int getCount() {
            return courseDisplayList.size();
        }

        @Override
        public Object getItem(int position) {
            return courseDisplayList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(R.layout.item_course, parent, false);
            }

            TextView courseText = convertView.findViewById(R.id.courseText);
            TextView courseMenuButton = convertView.findViewById(R.id.courseMenuButton);

            courseText.setText(courseDisplayList.get(position));

            if (position >= courseIdList.size()) {
                courseMenuButton.setVisibility(View.GONE);
            } else {
                courseMenuButton.setVisibility(View.VISIBLE);
            }

            courseMenuButton.setOnClickListener(v -> {
                showCourseMenu(v, position);
            });

            return convertView;
        }
    }
}