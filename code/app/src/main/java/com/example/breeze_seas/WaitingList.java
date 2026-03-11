package com.example.breeze_seas;


import android.util.Log;

import com.google.firebase.firestore.AggregateSource;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class WaitingList {
    private String eventId;
    private ArrayList<User> entrantList;
    private int capacity;
    private final FirebaseFirestore db;
    public WaitingList(String eventId, int capacity){
        this.eventId=eventId;
        this.capacity=capacity;
        this.db=DBConnector.getDb();
        this.entrantList=new ArrayList<User>();
    }


    public ArrayList<User> getWaitingList() {
        return entrantList;
    }

    public void addEntrant(String deviceId, String eventId) {
        DocumentReference eventRef = db.collection("events").document(eventId);
        DocumentReference userRef = db.collection("User").document(deviceId);
        CollectionReference waitingListColl = eventRef.collection("WaitingList");

        eventRef.get().addOnSuccessListener(eventDoc -> {
            if (!eventDoc.exists()) return;

            Long cap = eventDoc.getLong("waitingListCap");
            Boolean geoRequired = eventDoc.getBoolean("geoRequired");

            waitingListColl.count().get(AggregateSource.SERVER).addOnSuccessListener(snapshot -> {
                long currentCount = snapshot.getCount();

                if (cap == null || currentCount < cap) {
                    userRef.get().addOnSuccessListener(userDoc -> {
                        if (userDoc.exists()) {
                            User entrant = userDoc.toObject(User.class);
                            if (entrant != null) {
                                Map<String, Object> data = new HashMap<>();
                                data.put("deviceId", deviceId);
                                data.put("userName", entrant.getUserName());
                                data.put("firstName", entrant.getFirstName());
                                data.put("lastName", entrant.getLastName());
                                data.put("email", entrant.getEmail());
                                data.put("status", "Waiting");
                                data.put("timestamp", FieldValue.serverTimestamp());
                                data.put("location", Boolean.TRUE.equals(geoRequired) ? new GeoPoint(0.0, 0.0) : null);

                                waitingListColl.document(deviceId).set(data).addOnSuccessListener(aVoid -> {
                                    Log.d("WAITING_DEBUG", "Entrant added.");
                                });
                            }
                        }
                    });
                } else {
                    Log.d("WAITING_DEBUG", "Waiting list is full");
                }
            });
        });
    }
    public void fetchWaitingList(android.widget.BaseAdapter adapter, Runnable onFinish){
        CollectionReference list=db.collection("events").document(eventId)
                .collection("WaitingList");
        list.orderBy("timestamp", Query.Direction.ASCENDING).get()
                .addOnCompleteListener(op -> {
                    if (op.isSuccessful() && op.getResult()!=null){
                        entrantList.clear();
                        for (DocumentSnapshot doc : op.getResult()) {
                            User entrant=doc.toObject(User.class);
                            if (entrant!=null) entrantList.add(entrant);
                        }
                        if (adapter != null) adapter.notifyDataSetChanged();
                    }
                    if (onFinish != null) {
                        onFinish.run();
                    }
                });

    }

    public void removeEntrant(android.widget.BaseAdapter adapter, User entrant, Runnable onFinish){
        String id=entrant.getDeviceId();
        DocumentReference deleteEntrant=db.collection("Events")
                .document(eventId).collection("WaitingList").document(id);
        deleteEntrant.delete();
        entrantList.remove(entrant);
        if(adapter!=null) adapter.notifyDataSetChanged();
        if(onFinish!=null){
            onFinish.run();
        }
    }

}
