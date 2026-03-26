package com.example.breeze_seas;

import android.util.Log;

import com.google.firebase.Firebase;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.osmdroid.util.GeoPoint;

import java.util.ArrayList;
import java.util.HashMap;

public class Map {
    private HashMap<org.osmdroid.util.GeoPoint,String[]> location;
    private FirebaseFirestore db;
    private UserDB userDb;
    private Event event;


    public interface FetchedLocationListener {
        void onLocationFetched(HashMap<GeoPoint,String[]> location);
        void onFailure(Error e);
    }

    public Map(Event event) {
        this.event = event;
        this.db = DBConnector.getDb();
        this.location = new HashMap<>();
        this.userDb = new UserDB();
    }

    public void fetchLocation(FetchedLocationListener listener){
        CollectionReference listRef = db.collection("events").document(event.getEventId()).collection("participants");

        listRef.get().addOnSuccessListener(task->{
            if (task.isEmpty()) {
                listener.onLocationFetched(this.location);
                return;
            }

            final int total = task.size();
            final int[] count = {0};
            for(QueryDocumentSnapshot doc:task) {
                com.google.firebase.firestore.GeoPoint firestoreLocation=doc.getGeoPoint("location");
                String status = doc.getString("status");
                String docId = doc.getId();
                if (firestoreLocation == null || "declined".equals(status)) {
                    checkCount(total, count, listener);
                    continue;
                }
                org.osmdroid.util.GeoPoint mapPoint = new org.osmdroid.util.GeoPoint(firestoreLocation.getLatitude(),firestoreLocation.getLongitude());
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

    private void checkCount(int total, int[] count, FetchedLocationListener listener ){
        count[0]++;
        if (count[0] == total) {
            listener.onLocationFetched(this.location);
        }
    }

}
