# HadirTrack — Smart Attendance Tracking System

HadirTrack is a native Android application built with **Android Studio (Java)** to simplify attendance management in higher education. The app provides separate experiences for **Lecturers** and **Students**, making it easy to manage courses, create attendance sessions, and check in securely using location verification.

## ✨ Features

### 📍 GPS & Geofencing

Uses **FusedLocationProviderClient** to verify that students are physically inside the classroom before allowing attendance check-in.

### 🔔 Real-Time Notifications

Powered by **Firebase Cloud Messaging (FCM)** and **Cloud Functions**, allowing lecturers to receive instant notifications whenever a student checks in.

### ⏰ Local Notifications

Uses **AlarmManager** and **NotificationManager** to remind students before their classes begin.

### 🔒 Security

Implements **Firebase App Check** with **Google Play Integrity** to ensure only authentic app instances can access backend services.

### ☁️ Cloud Database

Uses **Cloud Firestore** for storing users, courses, attendance sessions, and student records with full CRUD functionality.

### 👤 Authentication

Built with **Firebase Authentication** using Email/Password login and automatic role-based navigation for **Lecturers** and **Students**.

### 🎨 User Interface

Designed with **Material Design** components featuring:

* Modern purple/indigo theme (`#2D2363`)
* Swipe-to-refresh support
* Clean navigation flow
* Separate dashboards for lecturers and students

---

# 🚀 Getting Started

Before running the project, complete the following setup.

## 1. Firebase Configuration

1. Create a new Firebase project named **HadirTrack**.
2. Add an Android app with the package name:

```text
com.example.hadirtrack
```

3. Download `google-services.json`.
4. Place it inside the `app/` directory.
5. Enable the following Firebase services:

   * **Authentication** → Email/Password
   * **Cloud Firestore**
   * **Cloud Functions** (deploy the `index.js` file inside the `/functions` folder)

---

## 2. App Check & API Keys

1. Enable the **Google Play Integrity API** in Google Cloud Console.
2. Register your **SHA-256 fingerprint** under **Firebase App Check**.
3. Add your **Google Maps API Key** either:

   * in `local.properties`, or
   * directly inside `AndroidManifest.xml`.

This is required for location-based attendance verification.

---

## 3. Run the project!
