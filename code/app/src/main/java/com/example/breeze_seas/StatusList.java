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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * A super class for participant lists. Creates an ArrayList
 * of users registering for events and manages their status in Firestore.
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

    public void startListening(ListUpdateListener listener){
        stopListening();
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
            if (totalChanges == 0) {
                if (listener != null) listener.onUpdate();
                return;
            }


            final int[] fetched = {0};
            for (DocumentChange change : snapshot.getDocumentChanges()) {
                String deviceId = change.getDocument().getId();
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

    public void determineLocation(Context context, User user, ListUpdateListener listener) {
        this.tempLocation = null;
        if (event.isGeolocationEnforced()) {
            com.google.android.gms.location.FusedLocationProviderClient client =
                    com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(context);


            if (androidx.core.app.ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION)
                    == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                client.getCurrentLocation(com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, null).addOnSuccessListener(location -> {
                    if (location != null) {
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
     * Adds a user to the event's participant sub-collection in Firestore.
     * Uses merge to preserve existing fields (like location) while updating status.
     * @param user The {@link User} object to be added.
     * @param listener Callback to handle success or failure of the DB operation.
     */

    public void addUser(User user,ListUpdateListener listener) {
        if (user == null || user.getDeviceId() == null) return;
        FirebaseFirestore db = DBConnector.getDb();
        DocumentReference participantRef = db.collection("events")
                .document(event.getEventId())
                .collection("participants")
                .document(user.getDeviceId());




        String status = getStatusName();
        Map<String, Object> update = new HashMap<>();
        update.put("deviceId",user.getDeviceId());
        update.put("status", status);
        update.put("timeJoined", FieldValue.serverTimestamp());
        if (tempLocation != null) {
            update.put("location", tempLocation);
        }




        participantRef.set(update, SetOptions.merge()) //update field if doc exists
                .addOnSuccessListener(aVoid -> {
                    this.tempLocation = null;
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
     * Fetches detailed User objects for each participant ID found.
     * @param participantDocs The QuerySnapshot from the participants sub-collection.
     * @param listener Callback to notify the UI when all users are fetched.
     */

    private void fetchFullUserDetails(QuerySnapshot participantDocs, ListUpdateListener listener) {
        int total = participantDocs.size();
        final int[] fetched = {0};
        for (DocumentSnapshot participantDoc : participantDocs) {
            String deviceId = participantDoc.getId();

            DBConnector.getDb().collection("users")
                    .document(deviceId)
                    .get()
                    .addOnSuccessListener(userDoc -> {
                        if (userDoc.exists()) {
                            User user = userDoc.toObject(User.class);
                            if (user != null) {
                                user.setDeviceId(userDoc.getId());
                                userList.add(user);
                            }
                        }
                        counter(fetched, total, listener);
                    })
                    .addOnFailureListener(e -> counter(fetched, total, listener));
        }
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


    public Event getEvent() { return event; }
    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }
    public void popUser(String userId) {
        if (userId == null) return;
        userList.removeIf(u -> u.getDeviceId().equals(userId));
    }

    public ArrayList<User> getUserList(){
        return userList;
    }

    public boolean userIsInList(User user) {
        // loop check
        for (User tmp : userList) {
            if (tmp.getDeviceId().equals(user.getDeviceId())) {
                return true;
            }
        }
        return false;
    }

    public int getSize(){
        return userList.size();
    }

}


