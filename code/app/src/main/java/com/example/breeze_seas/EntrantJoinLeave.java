package com.example.breeze_seas;

import android.util.Log;

import com.google.firebase.firestore.AggregateSource;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import java.util.HashMap;
import java.util.Map;

public class EntrantJoinLeave {

    private String eventId;
    private String deviceId;
    private final FirebaseFirestore db;

    //create this object with your deviceId and event you're on
    //to make Join Event and Leave Event buttons edit the database
    //the organizer side will actively fetch to reflect change
    public EntrantJoinLeave(String deviceId,String eventId){
        this.eventId=eventId;
        this.deviceId=deviceId;
        this.db=DBConnector.getDb();
    }

    public void addEntrant(String deviceId, String eventId) {
        DocumentReference eventRef = db.collection("events").document(eventId);
        CollectionReference waitingListColl = eventRef.collection("WaitingList");

        waitingListColl.count().get(AggregateSource.SERVER).addOnSuccessListener(snapshot -> {
            long currentCount = snapshot.getCount();
            eventRef.get().addOnSuccessListener(eventDoc -> {
                if (!eventDoc.exists()){
                    return;
                }

                Long cap = eventDoc.getLong("waitingListCap");

                if (cap == null || currentCount < cap) {

                    Map<String, Object> data = new HashMap<>();
                    data.put("deviceId", deviceId);
                    // not final implementation, default for halfway checkpoint
                    data.put("location", new GeoPoint(0.0, 0.0));
                    data.put("status", "Waiting");
                    data.put("timestamp", FieldValue.serverTimestamp());

                    waitingListColl.document(deviceId).set(data)
                            .addOnSuccessListener(aVoid -> Log.d("DB", "Successfully added to WaitingList"))
                            .addOnFailureListener(e -> Log.e("DB", "Failed to add: " + e.getMessage()));
                } else {
                    Log.d("DB", "Join failed: Event is at capacity (" + cap + ")");
                }
            });
        });
    }

    public void removeEntrant(String deviceId, String eventId) {
        DocumentReference entrantRef = db.collection("events").document(eventId)
                .collection("WaitingList").document(deviceId);

        entrantRef.delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d("DB_DELETE", "Successfully removed device: " + deviceId);
                })
                .addOnFailureListener(e -> {
                    Log.e("DB_DELETE", "Error deleting document: " + e.getMessage());
                });
    }
}
