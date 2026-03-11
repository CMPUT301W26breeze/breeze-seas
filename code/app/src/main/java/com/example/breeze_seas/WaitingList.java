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
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class WaitingList {
    private String eventId;
    private int capacity;
    private ArrayList<User> entrantList;
    private final FirebaseFirestore db;

    public WaitingList(String eventId, int capacity){
        this.entrantList=new ArrayList<>();
        this.eventId=eventId;
        this.capacity=capacity;
        this.db=DBConnector.getDb();
    }

    public ArrayList<User> getWaitingList(){
        return entrantList;
    }




    public void fetchWaiting(android.widget.BaseAdapter adapter,Runnable onFinish){ //quick fetch
        entrantList.clear();
        CollectionReference eventRef=db.collection("events");
        Query listQuery = eventRef.document(eventId)
                .collection("participants")
                .orderBy("joinedAt", Query.Direction.ASCENDING);
        //change to participants
        ArrayList<String> idList=new ArrayList<>();
        listQuery.get().addOnCompleteListener(task->{
            if(task.isSuccessful() && task.getResult()!=null){
                entrantList.clear();
                for(DocumentSnapshot doc: task.getResult()){
                    if(Objects.equals(doc.getString("status"), "waiting")){
                        idList.add(doc.getId());
                    }
                }

                fetchWaitingUsers(idList,adapter,onFinish);

            }
            else{
                if(onFinish!=null){
                    onFinish.run();
                }
            }
        });
    }


    public void fetchWaitingUsers(ArrayList<String> ids,android.widget.BaseAdapter adapter,Runnable onFinish){
        if (ids.isEmpty()){
            onFinish.run();
            return;
        }

        AtomicInteger count = new AtomicInteger(0);

        User[] sortedUsers = new User[ids.size()];

        for (int i = 0; i < ids.size(); i++) {
            final int index = i;
            String id = ids.get(i);

            db.collection("users").document(id).get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult().exists()) {
                            User user = task.getResult().toObject(User.class);
                            if (user != null) {
                                sortedUsers[index] = user;
                            }
                        }

                        if (count.incrementAndGet() == ids.size()) {
                            entrantList.clear();
                            for (User u : sortedUsers) {
                                if (u != null) entrantList.add(u);
                            }
                            if (adapter != null) adapter.notifyDataSetChanged();
                            if (onFinish != null) onFinish.run();
                        }


                    });
        }

    }
}
