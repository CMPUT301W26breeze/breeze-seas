package com.example.breeze_seas;

import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NotificationService {

    private static NotificationService instance;
    private final FirebaseFirestore db;
    private final CollectionReference notificationsRef;

    public NotificationService() {
        this.db = DBConnector.getDb();
        this.notificationsRef = db.collection("notifications");
    }

    public Task<Void> sendNotification(Notification notification) {
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
        void onNotificationLoaded(List<Notification> notifications);
        void onError(Exception e);
    }

    public void getNotifications(String userId, OnNotificationLoadedListener listener ) {
        notificationsRef
                .whereEqualTo("userId", userId)
                .orderBy("sentAt", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Notification> notifications = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        notifications.add(doc.toObject(Notification.class));
                    }
                    listener.onNotificationLoaded(notifications);
                })
                .addOnFailureListener(e -> {
                    Log.e("DB_ERROR", "Error fetching notifications", e);
                    listener.onError(e);
                });
    }

}
