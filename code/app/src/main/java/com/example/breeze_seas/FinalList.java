package com.example.breeze_seas;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class FinalList {
    private String eventId;
    private int capacity;
    private ArrayList<User> acceptedList;
    private final FirebaseFirestore db;
    public FinalList(String eventId, int capacity){
        this.eventId=eventId;
        this.capacity=capacity;
        this.db=DBConnector.getDb();
        this.acceptedList=new ArrayList<>();
    }

    public ArrayList<User> getFinalList(){
        return acceptedList;
    }

    public void fetchAccepted(android.widget.BaseAdapter adapter,Runnable onFinish){ //quick fetch
        acceptedList.clear();
        CollectionReference usersRef=db.collection("users");
        CollectionReference eventRef=db.collection("events");
        //change to participants
        Query listQuery = eventRef.document(eventId)
                .collection("participants")
                .orderBy("invitedAt", Query.Direction.ASCENDING);
        ArrayList<String> idList=new ArrayList<>();
        listQuery.get().addOnCompleteListener(task->{
            if(task.isSuccessful() && task.getResult()!=null){
                acceptedList.clear();
                for(DocumentSnapshot doc: task.getResult()){
                    if(Objects.equals(doc.getString("status"), "accepted")){
                        idList.add(doc.getId());
                    }
                }
                fetchAcceptedUsers(idList,adapter,onFinish);
            }
            else{
                if(onFinish!=null){
                    onFinish.run();
                }
            }
        });
    }

    public void fetchAcceptedUsers(ArrayList<String> ids,android.widget.BaseAdapter adapter,Runnable onFinish){
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
                            acceptedList.clear();
                            for (User u : sortedUsers) {
                                if (u != null) acceptedList.add(u);
                            }
                            if (adapter != null) adapter.notifyDataSetChanged();
                            if (onFinish != null) onFinish.run();
                        }


                    });
        }

    }
}
