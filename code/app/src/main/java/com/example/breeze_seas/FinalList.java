package com.example.breeze_seas;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class FinalList {
    private String event;
    private ArrayList<User> acceptedList;
    private int capacity;
    FirebaseFirestore db;
    public FinalList(String event){
        this.event=event;
        this.capacity=capacity;
        this.db=DBConnector.getDb();
        this.capacity=-1;
        this.acceptedList=new ArrayList<User>();
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public ArrayList<User> getAcceptedList() {
        return acceptedList;
    }

    public void fetchFinalList(android.widget.BaseAdapter adapter, Runnable onFinish){
        CollectionReference list=db.collection("Events").document(event)
                .collection("WaitingList");
        list.whereEqualTo("status", "Accepted").get()
                .addOnCompleteListener(op -> {
                    if (op.isSuccessful() && op.getResult() != null) {
                        acceptedList.clear();
                        for (DocumentSnapshot doc : op.getResult()) {
                            User invitedEntrant = doc.toObject(User.class);
                            if (invitedEntrant != null) acceptedList.add(invitedEntrant);
                        }
                        if (adapter != null) adapter.notifyDataSetChanged();
                    }
                    if (onFinish != null) onFinish.run();
                });

    }
}
