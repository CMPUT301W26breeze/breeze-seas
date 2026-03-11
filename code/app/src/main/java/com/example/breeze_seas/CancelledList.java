package com.example.breeze_seas;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class CancelledList {
    private String eventId;
    private ArrayList<User> cancelledList;
    private final FirebaseFirestore db;
    public CancelledList(String eventId){
        this.eventId=eventId;
        this.db=DBConnector.getDb();
        this.cancelledList=new ArrayList<User>();
    }

    public ArrayList<User> getCancelledList() {
        return cancelledList;
    }

    public void fetchFinalList(android.widget.BaseAdapter adapter, Runnable onFinish){
        CollectionReference list=db.collection("events").document(eventId)
                .collection("WaitingList");
        list.whereEqualTo("status", "Cancelled").get()
                .addOnCompleteListener(op -> {
                    if (op.isSuccessful() && op.getResult() != null) {
                        cancelledList.clear();
                        for (DocumentSnapshot doc : op.getResult()) {
                            User invitedEntrant = doc.toObject(User.class);
                            if (invitedEntrant != null) cancelledList.add(invitedEntrant);
                        }
                        if (adapter != null) adapter.notifyDataSetChanged();
                    }
                    if (onFinish != null) onFinish.run();
                });

    }
}
