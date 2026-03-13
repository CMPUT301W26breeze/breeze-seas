package com.example.breeze_seas;

import static androidx.core.content.ContentProviderCompat.requireContext;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Handles sending and fetching of notifications to the database.
 *
 * TODO: Add push notifications
 */
public class NotificationService {

    private final FirebaseFirestore db;
    private final CollectionReference notificationsRef;

    public NotificationService() {
        this.db = DBConnector.getDb();
        this.notificationsRef = db.collection("notifications");
    }

    /**
     * Writes the notification to the database.
     *
     * @param notification the notification that is sent to the database.
     * */
    public Task<Void> sendNotification(Notification notification) {
        String notificationId = notification.getNotificationId();
        Map<String, Object> notificationData = new HashMap<>();


        notificationData.put("type", notification.getType());
        notificationData.put("content", notification.getContent());
        notificationData.put("eventId", notification.getEventId());
        notificationData.put("eventName", notification.getEventName());
        notificationData.put("userId", notification.getUserId());
        notificationData.put("sentAt", FieldValue.serverTimestamp());
        notificationData.put("isSeen", notification.isSeen());

        Task<Void> writeTask = notificationsRef.document()
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

    /**
     * Gets the notifications that were meant for a specific user from the database.
     *
     * @param userId This user id is for filtering in the query.
     * @param listener this makes sure that data is actually received by the program.
     * */
    public void getNotifications(String userId, OnNotificationLoadedListener listener ) {
        notificationsRef
                .whereEqualTo("userId", userId)
                // .whereEqualTo("isSeen", false)
                .orderBy("sentAt", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Notification> notifications = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        // doc.getReference().update("isSeen", true);
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
