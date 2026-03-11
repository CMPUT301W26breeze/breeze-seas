package com.example.breeze_seas;

import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NotificationService {

    private final FirebaseFirestore db;
    private final CollectionReference notificationsRef;
    Notification notification;

    public NotificationService(Notification notification) {
        this.db = DBConnector.getDb();
        this.notificationsRef = db.collection("notifications");
        this.notification = notification;
    }

    public Task<Void> sendNotification() {
        String notificationId = notification.getNotificationId();
        Map<String, Object> notificationData = new HashMap<>();

        notificationData.put("notificationId", notificationId);
        notificationData.put("type", notification.getType());
        notificationData.put("content", notification.getContent());
        notificationData.put("eventId", notification.getEventId());
        notificationData.put("sentAt", notification.getSentAt());

        Task<Void> writeTask = notificationsRef.document(notificationId)
                .set(notificationData, SetOptions.merge());
        writeTask.addOnSuccessListener(aVoid ->
                        Log.d("DB_UPDATE", "User create/update successful"))
                .addOnFailureListener(e ->
                        Log.e("DB_UPDATE", "User create/update failed", e));

        return writeTask;


    }

    /**
     * Interface definition for a callback to be invoked when user data is loaded or fails.
     */
    public interface OnNotificationLoadedListener {
        void onNotificationLoaded(User user);
        void onError(Exception e);
    }

    public List<Notification> getNotifications(String userId, OnNotificationLoadedListener listener ) {
        ArrayList<Notification> myNotifications = new ArrayList<>();
        notificationsRef.whereEqualTo("userId", userId) // Only get MY notifications
                .orderBy("timestamp", Query.Direction.DESCENDING) // Newest first
                .limit(50) // Don't load 10,000 notes at once; start with 50
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        myNotifications.add(doc.toObject(Notification.class));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("DB_ERROR", "Error fetching notifications", e);
                    listener.onError(e);
                });

        return myNotifications;
    }

}
