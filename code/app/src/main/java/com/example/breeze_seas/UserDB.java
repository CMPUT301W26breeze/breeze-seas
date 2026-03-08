package com.example.breeze_seas;

import android.util.Log;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class UserDB {

    private FirebaseFirestore db;
    private CollectionReference userRef;

    public UserDB() {
        // Access the shared Firestore instance from your connector
        this.db = DBConnector.getDb();
        // Point to your "users" collection
        this.userRef = db.collection("User");
    }

    /* Updates an existing user or creates a new one in the database*/
    public void saveUser(User user) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("Name",user.getUserName());
        userData.put("Email",user.getEmail());
        userData.put("Phone Number", user.getPhoneNumber());
        userData.put("DeviceId",user.getDeviceId());
        userData.put("IsAdmin", user.isAdmin());
        userData.put("Notification Enabled", user.notificationEnabled());
        userData.put("createdAt", FieldValue.serverTimestamp());
        userRef.document(user.getDeviceId()).set(userData)
                .addOnSuccessListener(aVoid ->
                        Log.d("DB_UPDATE", "Update successful"))
                .addOnFailureListener(e ->
                        Log.e("DB_UPDATE", "Update failed", e));
    }

    public void updateUser(String deviceId, Map<String, Object> updates) {
        // Always update the modified time, no matter what else changed
        updates.put("updatedAt", FieldValue.serverTimestamp());

        userRef.document(deviceId)
                .update(updates)
                .addOnSuccessListener(aVoid ->
                        Log.d("DB", "Update successful"))
                .addOnFailureListener(e ->
                        Log.e("DB", "Update failed", e));
    }

    public void deleteUser(String deviceId) {
        userRef.document(deviceId).delete()
                .addOnSuccessListener(aVoid ->
                        Log.d("DB_UPDATE", "Update successful"))
                .addOnFailureListener(e ->
                        Log.e("DB_UPDATE", "Update failed", e));

    }

    public interface OnUserLoadedListener {
        void onUserLoaded(User user);
        void onError(Exception e);
    }

    public void getUser(String deviceId, OnUserLoadedListener listener) {
        userRef.document(deviceId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // This converts the Firestore document directly into your User object
                        User user = documentSnapshot.toObject(User.class);
                        listener.onUserLoaded(user);
                    } else {
                        listener.onUserLoaded(null); // Document doesn't exist
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("DB_ERROR", "Error fetching user", e);
                    listener.onError(e);
                });
    }





}
