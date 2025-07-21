const {onDocumentCreated} = require("firebase-functions/v2/firestore");
const {initializeApp} = require("firebase-admin/app");
const {getFirestore} = require("firebase-admin/firestore");
const {getMessaging} = require("firebase-admin/messaging");

// Initialize Firebase Admin
initializeApp();

exports.notifyNewTask = onDocumentCreated("users/{userId}/assigned_tasks/{taskId}", async (event) => {
  console.log("--- Function Start ---");
  const {userId, taskId} = event.params;
  const task = event.data.data();

  console.log(`Function triggered for userId: ${userId}, taskId: ${taskId}`);

  const db = getFirestore();

  try {
    // Fetch user's FCM token
    const userDoc = await db.collection("users").doc(userId).get();
    if (!userDoc.exists) {
        console.log(`User document not found for userId: ${userId}`);
        return;
    }

    const userData = userDoc.data();
    const fcmToken = userData.fcmToken;
    console.log(`Found user data. Checking for FCM Token.`);

    if (!fcmToken) {
      console.log(`No FCM token for user: ${userId}`);
      return;
    }
    console.log(`FCM Token found: ${fcmToken}`);

    // Notification payload
    const payload = {
      notification: {
        title: "New Task Assigned",
        body: `Client: ${task.clientName || "Unknown"}`
      },
      data: {
        clientName: String(task.clientName || ""),
        clientPhone: String(task.clientPhone || ""),
        clientLat: String(task.clientLat || ""),
        clientLng: String(task.clientLng || ""),
        assignedBy: String(task.assignedBy || ""),
        status: String(task.status || ""),
        taskId: taskId
      }
    };
    console.log("Payload created:", JSON.stringify(payload, null, 2));

    // Send notification
    console.log("Attempting to send notification...");
    const response = await getMessaging().sendToDevice(fcmToken, payload);
    console.log("✅ Successfully sent notification:", response);

  } catch (error) {
    console.error("!!! CRITICAL ERROR sending notification:", error);
  }
  console.log("--- Function End ---");
});