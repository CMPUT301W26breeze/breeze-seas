package com.example.breeze_seas;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

public class InvitationList {
    private String eventId;
    private ArrayList<User> invitedList;
    private FirebaseFirestore db;

    public InvitationList(String event){
        this.eventId=eventId;
        this.invitedList=new ArrayList<User>();
        this.db=DBConnector.getDb();
    }

    public ArrayList<User> getInvitedList() {
        return invitedList;
    }

    public void fetchInvitedList(android.widget.BaseAdapter adapter, Runnable onFinish) {
        CollectionReference list=db.collection("events").document(eventId)
                .collection("WaitingList");
        list.whereEqualTo("status", "Pending").get()
                .addOnCompleteListener(op -> {
                    if (op.isSuccessful() && op.getResult() != null) {
                        invitedList.clear();
                        for (DocumentSnapshot doc : op.getResult()) {
                            User invitedEntrant = doc.toObject(User.class);
                            if (invitedEntrant != null) invitedList.add(invitedEntrant);
                        }
                        if (adapter != null) adapter.notifyDataSetChanged();
                    }
                    if (onFinish != null) onFinish.run();
                });
    }

}
