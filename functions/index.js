const {onDocumentCreated} = require("firebase-functions/v2/firestore");
const admin = require("firebase-admin");

admin.initializeApp();

/**
 * Triggers when a new document is created in 'notifications' collection.
 * Uses Firebase Functions v2 syntax.
 */
exports.sendCheckInNotification = onDocumentCreated(
    "notifications/{notificationId}",
    async (event) => {
      const snapshot = event.data;
      if (!snapshot) {
        console.log("No data associated with the event");
        return null;
      }

      const data = snapshot.data();
      const lecturerId = data.lecturerId;
      const title = data.title;
      const message = data.message;

      try {
        // 1. Get the lecturer's FCM token from the 'users' collection
        const userDoc = await admin.firestore()
            .collection("users")
            .doc(lecturerId)
            .get();

        if (!userDoc.exists) {
          console.log("No such lecturer found!");
          return null;
        }

        const fcmToken = userDoc.data().fcmToken;

        if (!fcmToken) {
          console.log("Lecturer has no FCM token stored.");
          return null;
        }

        // 2. Construct the FCM HTTP v1 Message
        const payload = {
          token: fcmToken,
          notification: {
            title: title,
            body: message,
          },
          data: {
            notificationId: event.params.notificationId,
            type: "check_in",
            sessionId: data.sessionId || "",
            courseId: data.courseId || "",
            courseCode: data.courseCode || "",
            courseName: data.courseName || "",
          },
          android: {
            priority: "high",
            notification: {
              channelId: "hadirtrack_urgent_channel",
              notificationPriority: "PRIORITY_MAX",
              sound: "default",
              visibility: "PUBLIC",
              clickAction: "OPEN_LECTURER_DASHBOARD",
            },
          },
        };

        // 3. Send the notification via FCM HTTP v1
        const response = await admin.messaging().send(payload);
        console.log("Successfully sent message:", response);
        return response;
      } catch (error) {
        console.error("Error sending notification:", error);
        return null;
      }
    });
