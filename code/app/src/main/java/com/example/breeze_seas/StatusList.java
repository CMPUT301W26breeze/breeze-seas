package com.example.breeze_seas;

import android.content.Context;

import com.google.firebase.Firebase;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * A super class for that manages a list of participants for an event.
 * It handles real-time database synchronization with Firestore, manages user status transitions,
 * handles geolocation requirements, and facilitates promotion of users to co-organizers.
 */

public abstract class StatusList {
    protected ArrayList<User> userList;
    protected int capacity;
    protected Event event;
    private ListenerRegistration listenerRegistration;
    protected com.google.firebase.firestore.GeoPoint tempLocation;

    /**
     * Interface to communicate result of asynchronous database tasks back to the UI.
     */

    public interface ListUpdateListener {
        void onUpdate();
        void onError(Exception e);
    }


    /**
     * Attaches a SnapshotListener to Firestore to monitor participants with a specific status.
     * This provides real-time updates and when a user's status changes in the DB, they are
     * automatically added to or removed from this list in the UI.
     *  @param listener The listener to notify when the local list has been synchronized.
     */
    public void startListening(ListUpdateListener listener){
        stopListening(); //clear existing listeners
        FirebaseFirestore db = DBConnector.getDb();

        Query listRef = db.collection("events").document(event.getEventId())
                .collection("participants").whereEqualTo("status",getStatusName());

        this.listenerRegistration = listRef.addSnapshotListener((snapshot,error)->{
            if (error != null) {
                if (listener != null) {
                    listener.onError(error);
                }
                return;
            }

            if (snapshot == null) {
                return;
            }

            int totalChanges = snapshot.getDocumentChanges().size();
            //no change was made
            if (totalChanges == 0) {
                if (listener != null) listener.onUpdate();
                return;
            }

            final int[] fetched = {0};  // Counter to track async user profile fetches
            for (DocumentChange change : snapshot.getDocumentChanges()) {
                String deviceId = change.getDocument().getId();
                // Check what kind of change was made
                switch (change.getType()) {
                    case ADDED:
                        fetchAndAddUser(deviceId, fetched, totalChanges, listener);
                        break;
                    case MODIFIED:
                        popUser(deviceId);
                        fetchAndAddUser(deviceId, fetched, totalChanges, listener);
                        break;
                    case REMOVED:
                        popUser(deviceId);
                        counter(fetched, totalChanges, listener);
                        break;
                }
            }
        });
    }


    /**
     * Detaches the Firestore listener and clears the local user list.
     */
    public void stopListening(){
        if (this.listenerRegistration != null) {
            this.listenerRegistration.remove();
            this.listenerRegistration = null;
            this.userList.clear();
        }
    }


    /**
     * Constructor for StatusList.
     * @param event The {@link Event} object associated with this list.
     * @param capacity The maximum number of users allowed in this specific list.
     */
    public StatusList(Event event, int capacity){
        this.event=event;
        this.capacity=capacity;
        this.userList=new ArrayList<>();
        this.tempLocation=null;
    }


    /**
     * Returns the status string used for database queries.
     * @return String representing the status (e.g., "waiting", "selected").
     */
    protected abstract String getStatusName();


    /**
     * Determines the user's current GPS location if the event requires it.
     * If geolocation is not enforced, it proceeds directly to adding the user.
     * @param context The application or activity context.
     * @param user The user object requesting to join.
     * @param listener Callback for completion.
     */
    public void determineLocation(Context context, User user, ListUpdateListener listener) {
        this.tempLocation = null;
        if (event.getCoOrganizerId() != null && event.getCoOrganizerId().contains(user.getDeviceId())) {
            if (listener != null) {
                listener.onError(new Exception("Co-Organizers cannot join the event as participants."));
            }
            return;
        }
        if (event.isGeolocationEnforced()) {
            com.google.android.gms.location.FusedLocationProviderClient client =
                    com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(context);

            // Check for system-level location permissions
            if (androidx.core.app.ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION)
                    == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                client.getCurrentLocation(com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, null).addOnSuccessListener(location -> {
                    if (location != null) {
                        // Convert Android Location to Firestore GeoPoint
                        this.tempLocation = new com.google.firebase.firestore.GeoPoint(
                                location.getLatitude(), location.getLongitude());
                    }
                    addUser(user, listener);
                }).addOnFailureListener(e->{
                    if(listener!=null) listener.onError(e);
                });
            } else {
                if (listener != null) {
                    listener.onError(new Exception("Permission Denied"));
                }
            }
        }
        else{
            this.tempLocation = null;
            addUser(user, listener);
        }

    }


    /**
     * Adds or updates a user in the event's participant sub-collection.
     * @param user The {@link User} object to be added.
     * @param listener Callback to handle success or failure of the DB operation.
     */
    public void addUser(User user,ListUpdateListener listener) {
        if (user == null || user.getDeviceId() == null) {
            return;
        }

        if (this.capacity > 0 && getSize() >= this.capacity) {
            if (listener != null) {
                listener.onError(new Exception("This list is currently full (" + capacity + ")."));
            }
            return;
        }

        FirebaseFirestore db = DBConnector.getDb();
        DocumentReference participantRef = db.collection("events")
                .document(event.getEventId())
                .collection("participants")
                .document(user.getDeviceId());

        String status = getStatusName();
        Map<String, Object> update = new HashMap<>();
        update.put("deviceId",user.getDeviceId());
        update.put("status", status);

        // Only set the join timestamp and location if the user is entering the "waiting" list
        if (getStatusName().equals("waiting")) {
            update.put("timeJoined", FieldValue.serverTimestamp());
            if (tempLocation != null) {
                update.put("location", tempLocation);
            }
        }

        participantRef.set(update, SetOptions.merge()) //update field if doc exists
                .addOnSuccessListener(aVoid -> {
                    this.tempLocation = null; // Clear location after successful save
                    if (listener != null) {
                        listener.onUpdate();
                    }
                })
                .addOnFailureListener(e -> {
                    if (listener != null) {
                        listener.onError(e);
                    }
                });
    }


    /**
     * Fetches a full User profile from the 'users' collection using a deviceId.
     * This is used because the 'participants' sub-collection only stores IDs/Status.
     */
    private void fetchAndAddUser(String deviceId, int[] fetched, int total, ListUpdateListener listener) {
        DBConnector.getDb().collection("users")
                .document(deviceId)
                .get()
                .addOnSuccessListener(userDoc -> {
                    if (userDoc.exists()) {
                        User user = userDoc.toObject(User.class);
                        if (user != null) {
                            user.setDeviceId(userDoc.getId());
                            if (!userIsInList(user)) {
                                userList.add(user);
                            }
                        }
                    }
                    counter(fetched, total, listener);
                })
                .addOnFailureListener(e -> {
                    counter(fetched, total, listener);
                });
    }


    /**
     * Removes a user from the event's participant sub-collection in Firestore.
     * @param deviceId The deviceId of the User that is to be removed.
     * @param listener Callback to handle success or failure of the DB operation.
     */
    public void removeUserFromDB(String deviceId, ListUpdateListener listener) {
        if (deviceId == null) return;

        FirebaseFirestore db = DBConnector.getDb();
        DocumentReference participantRef = db.collection("events")
                .document(event.getEventId())
                .collection("participants")
                .document(deviceId);

        participantRef.delete()
                .addOnSuccessListener(aVoid -> {
                    if (listener != null) listener.onUpdate();
                })
                .addOnFailureListener(e -> {
                    if (listener != null) listener.onError(e);
                });
    }


    /**
     * Counter for synchronizing parallel Firestore calls.
     * @param count Array containing the current completion count.
     * @param total Total number of operations expected.
     * @param listener The listener to trigger upon reaching the total.
     */
    private void counter(int[] count, int total, ListUpdateListener listener) {
        count[0]++;
        if (count[0] == total && listener != null) {
            listener.onUpdate();
        }
    }


    /**
     * @return The {@link Event} object associated with this list.
     * */
    public Event getEvent() { return event; }


    /**
     * @return The maximum capacity of this status list.
     * */
    public int getCapacity() { return capacity; }


    /**
     * @param capacity The new capacity to set for this status list.
     * */
    public void setCapacity(int capacity) { this.capacity = capacity; }


    /**
     * Removes a user from the local memory list by ID.
     * @param userId The ID of the user to remove.
     */
    public void popUser(String userId) {
        if (userId == null) return;
        userList.removeIf(u -> u.getDeviceId().equals(userId));
    }


    /**
     * @return The current local ArrayList of Users in this list.
     * */
    public ArrayList<User> getUserList(){
        return userList;
    }


    /**
     * Checks if a user is already present in the local list.
     * @param user The user to check.
     * @return True if found, false otherwise.
     */
    public boolean userIsInList(User user) {
        // loop check
        for (User tmp : userList) {
            if (tmp.getDeviceId().equals(user.getDeviceId())) {
                return true;
            }
        }
        return false;
    }


    /**
     * @return The number of users currently in the local list.
     * */
    public int getSize(){
        return userList.size();
    }

}


