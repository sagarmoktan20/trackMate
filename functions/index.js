const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");

initializeApp();

exports.notifyNewTask = onDocumentCreated(
  "users/{userId}/assigned_tasks/{taskId}",
  async (event) => {
    const snap = event.data;
    if (!snap) return;
    const context = event.params;
    const userId = context.userId;
    const task = snap.data();

    console.log("--- Function Start ---");
    console.log(`Function triggered for userId: ${userId}, taskId: ${context.taskId}`);

    // Fetch the user's FCM token from Firestore
    const userDoc = await getFirestore()
      .collection("users")
      .doc(userId)
      .get();

    console.log("Found user data. Checking for FCM Token.");
    const fcmToken = userDoc.get("fcmToken");
    console.log("FCM Token found:", fcmToken);

    if (!fcmToken) {
      console.log(`No FCM token for user: ${userId}`);
      console.log("--- Function End ---");
      return null;
    }

    // Prepare the notification payload for sendEachForMulticast
    const message = {
      tokens: [fcmToken],
      notification: {
        title: "New Task Assigned",
        body: `Client: ${task.clientName || "Unknown"}`,
      },
      android: {
        // Set high priority for Android
        priority: "high",
        notification: {
          // Sound, vibration and priority settings
          sound: "default",
          priority: "high",
          channelId: "task_channel",
          // Make notification visible on lock screen
          visibility: "public",
          // Notification icon (optional)
          icon: "ic_dialog_info",
          // Set color (optional)
          color: "#f45342"
        }
      },
      apns: {
        // Set high priority for iOS
        headers: {
          "apns-priority": "10",
        },
        payload: {
          aps: {
            // Sound and badge settings
            sound: "default",
            badge: 1,
            // Critical alerts bypass Do Not Disturb
            "content-available": 1,
          },
        },
      },
      // Set high priority for web
      webpush: {
        headers: {
          Urgency: "high",
        },
        notification: {
          // Require interaction prevents auto-dismissal
          requireInteraction: true,
        },
      },
      data: {
        clientName: task.clientName || "",
        clientPhone: task.clientPhone || "",
        clientLat: String(task.clientLat || ""),
        clientLng: String(task.clientLng || ""),
        assignedBy: task.assignedBy || "",
        status: task.status || "",
        taskId: context.taskId,
        // Add a priority flag that can be read by the client app
        priority: "high",
      },
    };

    console.log("Payload created:", JSON.stringify(message, null, 2));
    console.log("Attempting to send notification...");

    // Send the notification using sendEachForMulticast
    try {
      const response = await getMessaging().sendEachForMulticast(message);
      console.log("Notification send response:", JSON.stringify(response, null, 2));
      console.log(
        "Notification sent to " +
          userId +
          " for task " +
          context.taskId
      );
    } catch (error) {
      console.error("!!! CRITICAL ERROR sending notification:", error);
    }

    console.log("--- Function End ---");
    return null;
  }
);
