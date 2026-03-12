package com.example.breeze_seas;

import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;


/**
 * This class provides methods to create, update, delete, and retrieve users
 * using the device's unique ID as the primary key.
 */
public class UserDB {
    private final FirebaseFirestore db;
    private final CollectionReference userRef;

    /**
     * Initializes a new UserDB instance.
     * Connects to the shared Firestore instance via {@link DBConnector}.
     */
    public UserDB() {
        this.db = DBConnector.getDb();
        this.userRef = db.collection("users");
    }

    /**
     * Creates a new user record in the database.
     * If the document already exists, it merges the new data with existing fields.
     * Sets the {@code createdAt} timestamp to the server time.
     *
     * @param user The {@link User} object containing the profile data to be stored.
     */
    public Task<Void> createUser(User user) {

        String deviceId = user.getDeviceId();
        Map<String, Object> userData = new HashMap<>();

        userData.put("deviceId", deviceId);
        userData.put("firstName", user.getFirstName());
        userData.put("lastName", user.getLastName());
        userData.put("userName", user.getUserName());
        userData.put("email", user.getEmail());
        userData.put("phoneNumber", user.getPhoneNumber());
        userData.put("isAdmin", user.isAdmin());
        userData.put("notificationEnabled", user.notificationEnabled());
        userData.put("createdAt", FieldValue.serverTimestamp());
        userData.put("updatedAt", FieldValue.serverTimestamp());

        Task<Void> writeTask = userRef.document(deviceId).set(userData, SetOptions.merge());
        writeTask.addOnSuccessListener(aVoid ->
                        Log.d("DB_UPDATE", "User create/update successful"))
                .addOnFailureListener(e ->
                        Log.e("DB_UPDATE", "User create/update failed", e));
        return writeTask;
    }

    /**
     * Updates specific fields of an existing user document.
     * Automatically appends an {@code updatedAt} server timestamp to the request.
     *
     * @param deviceId The unique identifier of the user to update.
     * @param updates  A Map containing field names as keys and the new values to be updated.
     */
    public void updateUser(String deviceId, Map<String, Object> updates) {
        updates.put("updatedAt", FieldValue.serverTimestamp());

        userRef.document(deviceId)
                .update(updates)
                .addOnSuccessListener(aVoid ->
                        Log.d("DB", "Update successful"))
                .addOnFailureListener(e ->
                        Log.e("DB", "Update failed", e));
    }

    /**
     * Deletes a user document from the "User" collection.
     *
     * @param deviceId The unique identifier of the user to be deleted.
     */
    public void deleteUser(String deviceId) {
        userRef.document(deviceId).delete()
                .addOnSuccessListener(aVoid ->
                        Log.d("DB_UPDATE", "User deleted successfully"))
                .addOnFailureListener(e ->
                        Log.e("DB_UPDATE", "Deletion failed", e));

    }

    /**
     * Interface definition for a callback to be invoked when user data is loaded or fails.
     */
    public interface OnUserLoadedListener {
        void onUserLoaded(User user);
        void onError(Exception e);
    }

    /**
     * Fetches a user document from Firestore.
     * Results are returned via the provided {@link OnUserLoadedListener}.
     *
     * @param deviceId The unique identifier of the user to fetch.
     * @param listener The callback listener to handle the retrieved data or errors.
     */
    public void getUser(String deviceId, OnUserLoadedListener listener) {
        userRef.document(deviceId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        listener.onUserLoaded(parseUser(documentSnapshot));
                    } else {
                        userRef.whereEqualTo("deviceId", deviceId)
                                .limit(1)
                                .get()
                                .addOnSuccessListener(querySnapshot -> {
                                    if (querySnapshot != null && !querySnapshot.isEmpty()) {
                                        listener.onUserLoaded(parseUser(querySnapshot.getDocuments().get(0)));
                                    } else {
                                        listener.onUserLoaded(null);
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("DB_ERROR", "Error fetching user by deviceId", e);
                                    listener.onError(e);
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("DB_ERROR", "Error fetching user", e);
                    listener.onError(e);
                });
    }

    private User parseUser(DocumentSnapshot documentSnapshot) {
        User user = new User();

        String deviceId = documentSnapshot.getString("deviceId");
        if (deviceId == null || deviceId.trim().isEmpty()) {
            deviceId = documentSnapshot.getId();
        }

        String firstName = documentSnapshot.getString("firstName");
        String lastName = documentSnapshot.getString("lastName");
        String userName = documentSnapshot.getString("userName");
        String fullName = documentSnapshot.getString("name");
        String email = documentSnapshot.getString("email");
        String phoneNumber = documentSnapshot.getString("phoneNumber");
        Boolean isAdmin = documentSnapshot.getBoolean("isAdmin");
        Boolean notificationEnabled = documentSnapshot.getBoolean("notificationEnabled");

        if ((userName == null || userName.trim().isEmpty()) && fullName != null) {
            userName = fullName;
        }

        user.setDeviceId(deviceId);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setUserName(userName);
        if (email != null && email.contains("@")) {
            user.setEmail(email);
        }
        user.setPhoneNumber(phoneNumber);
        user.setAdmin(Boolean.TRUE.equals(isAdmin));
        user.setNotificationEnabled(notificationEnabled == null || notificationEnabled);
        user.setCreatedAt(documentSnapshot.getTimestamp("createdAt"));
        user.setUpdatedAt(documentSnapshot.getTimestamp("updatedAt"));
        return user;
    }
}
