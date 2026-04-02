package com.example.breeze_seas;

import android.util.Log;

import androidx.annotation.Nullable;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;


/**
 * This class provides methods to create, update, delete, and retrieve users
 * using the device's unique ID as the primary key.
 */
public class UserDB {
    private final FirebaseFirestore db;
    private final CollectionReference userRef;
    // Holds the active Firestore snapshot listener so we can detach it later
    private ListenerRegistration usersListener;

    public interface UserMutationCallback {
        void onSuccess();
        void onFailure(Exception e);
    }


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
        userData.put("imageDocId", user.getImageDocId());
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
        updateUser(deviceId, updates, null);
    }

    /**
     * Updates specific fields of an existing user document and reports the result.
     *
     * @param deviceId The unique identifier of the user to update.
     * @param updates A Map containing field names as keys and the new values to be updated.
     * @param callback Optional callback receiving success or failure.
     */
    public void updateUser(String deviceId, Map<String, Object> updates, @Nullable UserMutationCallback callback) {
        updates.put("updatedAt", FieldValue.serverTimestamp());

        userRef.document(deviceId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d("DB", "Update successful");
                    if (callback != null) {
                        callback.onSuccess();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("DB", "Update failed", e);
                    if (callback != null) {
                        callback.onFailure(e);
                    }
                });
    }



    /**
     * Deletes all trace of the user from the database except comments.
     *
     * @param deviceId The unique identifier of the user to be deleted.
     */
    public void deleteUser(String deviceId) {
        getUser(deviceId, new OnUserLoadedListener() {
            @Override
            public void onUserLoaded(User user) {
                // Find all instances of this user in any "participants" collection
                db.collectionGroup("participants")
                        .whereEqualTo("deviceId", deviceId)
                        .get()
                        .addOnSuccessListener(participantSnapshots -> {

                            // Get all events
                            EventDB.getAllEvents(new EventDB.LoadEventsCallback() {
                                @Override
                                public void onSuccess(ArrayList<Event> events) {
                                    WriteBatch batch = db.batch();

                                    // Deletions for all participant instances found
                                    for (QueryDocumentSnapshot doc : participantSnapshots) {
                                        batch.delete(doc.getReference());
                                    }

                                    // Event deletions if the user is the organizer
                                    for (Event event : events) {
                                        DocumentReference eventRef = db.collection("events")
                                                .document(event.getEventId());

                                        if (Objects.equals(event.getOrganizerId(), deviceId)) {
                                            batch.delete(eventRef);
                                        }
                                    }

                                    if (user != null && user.getImageDocId() != null
                                            && !user.getImageDocId().trim().isEmpty()) {
                                        batch.delete(db.collection("images").document(user.getImageDocId()));
                                    }

                                    // The user document deletion
                                    DocumentReference userDocumentRef = userRef.document(deviceId);
                                    batch.delete(userDocumentRef);

                                    batch.commit()
                                            .addOnSuccessListener(aVoid -> {
                                                Log.d("DB_UPDATE", "User profile, owned events, and participant records deleted.");
                                            })
                                            .addOnFailureListener(e -> {
                                                Log.e("DB_UPDATE", "Deletion failed. No data was removed.", e);
                                            });
                                }

                                @Override
                                public void onFailure(Exception e) {
                                    Log.e("DB_UPDATE", "Failed to load events", e);
                                }
                            });
                        })
                        .addOnFailureListener(e -> {
                            Log.e("DB_UPDATE", "Failed to query participants", e);
                        });
            }

            @Override
            public void onError(Exception e) {
                Log.e("DB_UPDATE", "Failed to load user", e);
            }
        });
    }

    /**
     * Callbacks fired by the real-time users listener.
     * Each method corresponds to a type of change in the "users" collection.
     */
    public interface UsersChangedCallback {
        void onUserAdded(User user);      // A new user document appeared
        void onUserModified(User user);   // An existing user document was updated
        void onUserRemoved(User user);    // A user document was deleted
        void onFailure(Exception e);      // Something went wrong with the listener
    }

    /**
     * Starts listening to the "users" collection in real time.
     * Any add, modify, or remove on any user document will fire the appropriate callback.
     *
     * @param callback Receives individual change events or errors.
     */
    public void startUsersListen(UsersChangedCallback callback) {
        // Listener
        usersListener = userRef.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot snapshots,
                                @Nullable FirebaseFirestoreException error) {
                if (error != null) {
                    Log.w("UserDB", "Users listen failed.", error);
                    callback.onFailure(error);
                    return;
                }
                if (snapshots == null) return;

                // Process only the documents that changed, not the entire collection
                for (DocumentChange dc : snapshots.getDocumentChanges()) {
                    User user = parseUser(dc.getDocument());
                    switch (dc.getType()) {
                        case ADDED:
                            callback.onUserAdded(user);
                            break;
                        case MODIFIED:
                            callback.onUserModified(user);
                            break;
                        case REMOVED:
                            // Fires after the batch commit in deleteUser succeeds
                            callback.onUserRemoved(user);
                            break;
                    }
                }
            }
        });
    }

    /**
     * Stops listener for the "users" collection.
     */
    public void stopUsersListen() {
        if (usersListener != null) {
            usersListener.remove();
            usersListener = null;
        }
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
                        deliverUserWithImage(parseUser(documentSnapshot), listener);
                    } else {
                        userRef.whereEqualTo("deviceId", deviceId)
                                .limit(1)
                                .get()
                                .addOnSuccessListener(querySnapshot -> {
                                    if (querySnapshot != null && !querySnapshot.isEmpty()) {
                                        deliverUserWithImage(parseUser(querySnapshot.getDocuments().get(0)), listener);
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

    /**
     * Loads the user's profile image when an image reference exists before returning the user.
     *
     * @param user User loaded from the Firestore document.
     * @param listener Callback receiving the fully populated user.
     */
    private void deliverUserWithImage(@Nullable User user, OnUserLoadedListener listener) {
        if (user == null) {
            listener.onUserLoaded(null);
            return;
        }

        if (user.getImageDocId() == null || user.getImageDocId().trim().isEmpty()) {
            listener.onUserLoaded(user);
            return;
        }

        ImageDB.loadImage(user.getImageDocId(), new ImageDB.LoadImageCallback() {
            @Override
            public void onSuccess(Image image) {
                user.setProfileImage(image);
                listener.onUserLoaded(user);
            }

            @Override
            public void onFailure(Exception e) {
                Log.e("DB_ERROR", "Error loading user profile image", e);
                listener.onUserLoaded(user);
            }
        });
    }

    /**
     * Uses the user data from the database to create a
     * {@link User} instance.
     *
     * @param documentSnapshot User data fetched from the database
     */
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
        String imageDocId = documentSnapshot.getString("imageDocId");
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
        user.setImageDocId(imageDocId);
        user.setAdmin(Boolean.TRUE.equals(isAdmin));
        user.setNotificationEnabled(notificationEnabled == null || notificationEnabled);
        user.setCreatedAt(documentSnapshot.getTimestamp("createdAt"));
        user.setUpdatedAt(documentSnapshot.getTimestamp("updatedAt"));
        return user;
    }
}
