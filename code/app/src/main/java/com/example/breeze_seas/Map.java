package com.example.breeze_seas;

import android.util.Log;

import com.google.firebase.Firebase;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.osmdroid.util.GeoPoint;

import java.util.ArrayList;
import java.util.HashMap;


/**
 * Manages the retrieval and mapping of participant locations for a specific event.
 * It converts Firestore GeoPoints into OSMDroid GeoPoints and associates them with
 * participant details; username and status; for map markers.
 */
public class Map {
    private HashMap<GeoPoint,String[]> location;
    private FirebaseFirestore db;
    private UserDB userDb;
    private Event event;


    /**
     * Interface to communicate the results of location fetching back to the UI (e.g., MapsFragment).
     */
    public interface FetchedLocationListener {
        void onLocationFetched(HashMap<GeoPoint,String[]> location);
        void onFailure(Error e);
    }


    /**
     * Constructor for the Map manager.
     * @param event The {@link Event} object whose participants' locations need to be fetched.
     */
    public Map(Event event) {
        this.event = event;
        this.db = DBConnector.getDb();
        this.location = new HashMap<>();
        this.userDb = new UserDB();
    }


    /**
     * Fetches participant documents from the event's sub-collection and resolves their
     * usernames to create a map of locations.
     * @param listener The listener to notify once the data set is complete.
     */
    public void fetchLocation(FetchedLocationListener listener){
        CollectionReference listRef = db.collection("events").document(event.getEventId()).collection("participants");

        listRef.get().addOnSuccessListener(task->{
            if (task.isEmpty()) {
                listener.onLocationFetched(this.location);
                return;
            }

            final int total = task.size(); // Total number of participant documents to process
            final int[] count = {0};
            for(QueryDocumentSnapshot doc:task) {
                // Retrieve location and status. Skip users who haven't provided location
                // or who have declined the event invitation.
                com.google.firebase.firestore.GeoPoint firestoreLocation=doc.getGeoPoint("location");
                String status = doc.getString("status");
                String docId = doc.getId();
                if (firestoreLocation == null || "declined".equals(status)) {
                    checkCount(total, count, listener);
                    continue;
                }

                // Convert Firestore's GeoPoint to OSMDroid's GeoPoint
                org.osmdroid.util.GeoPoint mapPoint = new org.osmdroid.util.GeoPoint(firestoreLocation.getLatitude(),firestoreLocation.getLongitude());

                //fetch the users collection to get the actual username for the marker label.
                userDb.getUser(docId, new UserDB.OnUserLoadedListener() {
                    @Override
                    public void onUserLoaded(User user) {
                        String username=user.getUserName();
                        location.put(mapPoint, new String[]{username, status});
                        checkCount(total, count, listener);
                    }
                    @Override
                    public void onError(Exception e) {
                        Log.e("Location Username","Couldn't fetch username at location");
                        checkCount(total, count, listener);
                    }
                });
            }

        }).addOnFailureListener(e->{
            listener.onFailure(new Error(e.getMessage()));
        });

    }


    /**
     * Synchronizes parallel asynchronous tasks.
     * @param total The total number of documents found in the initial fetch.
     * @param count The current completion tally.
     * @param listener The listener to trigger upon completion.
     */
    private void checkCount(int total, int[] count, FetchedLocationListener listener ){
        count[0]++;
        if (count[0] == total) {
            listener.onLocationFetched(this.location);
        }
    }

}