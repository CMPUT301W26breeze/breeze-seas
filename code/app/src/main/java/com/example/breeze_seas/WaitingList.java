package com.example.breeze_seas;


import android.view.View;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class WaitingList {
    private String event;
    private ArrayList<User> entrantList;
    private int size;
    private FirebaseFirestore db;
    public WaitingList(String event){
        this.event=event;
        this.size=0;
        this.db=DBConnector.getDb();
        this.entrantList=new ArrayList<User>();
    }

    //should have been implemented from the user side
    //for testing purposes
    public void addEntrant(User entrant){
        DocumentReference entrantRef=db.collection("Events").document(event).
                    collection("WaitingList").document(entrant.getDeviceId());
        Map<String,Object> data=new HashMap<>();
        data.put("Name",entrant.getUserName());
        data.put("Email",entrant.getEmail());
        data.put("DeviceId",entrant.getDeviceId());
        data.put("Status","Waiting");
        data.put("Timestamp",FieldValue.serverTimestamp());
        entrantRef.set(data);

    }

    public void getWaitingList(android.widget.BaseAdapter adapter){
        CollectionReference list=db.collection("Events").document(event)
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
                });
    }

    public void removeEntrant(android.widget.BaseAdapter adapter, User entrant,String event){
        String id=entrant.getDeviceId();
        DocumentReference deleteEntrant=db.collection("Events")
                .document(event).collection("WaitingList").document(id);
        deleteEntrant.delete();
        entrantList.remove(entrant);
        if(adapter!=null) adapter.notifyDataSetChanged();
    }

}
