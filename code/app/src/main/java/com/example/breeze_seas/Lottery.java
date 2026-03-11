package com.example.breeze_seas;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;

public class Lottery {

    private final FirebaseFirestore db; //the database
    private String event; //to uniquely identify the event
    private ArrayList<User> entrantList; //need to fill this with eligible users (i.e still waiting)
    public Lottery(String event){
        this.event=event;
        this.entrantList=null;
        this.db=DBConnector.getDb();
    }

    public void runLottery(int size,Runnable onFinish){

        if (size <= 0) {
            return; //lottery should select at least one person
        }

        //navigate to WaitingList collection of the event
        CollectionReference list=db.collection("Events").document(event)
                .collection("WaitingList");

        //send fetch data request
        list.get().addOnCompleteListener(op->{
            //on successful fetch
            if (op.isSuccessful() && op.getResult()!=null){
                this.entrantList=new ArrayList<User>();
                int invited=0;
                for(DocumentSnapshot doc: op.getResult()){
                    String status = doc.getString("status");

                    if("Waiting".equals(status)) {
                        User entrant = doc.toObject(User.class);
                        if (entrant != null) this.entrantList.add(entrant);
                    }
                    else if ("Pending".equals(status) || "Accepted".equals(status)) {
                        // Only these two statuses occupy a spot in the event
                        invited += 1;
                    }
                }

                if (entrantList.isEmpty()) return; //no eligible user

                int slots=size-invited;
                int toFill=Math.min(slots,entrantList.size()); //number of users we can invite
                WriteBatch batch = db.batch();
                int batchCount=0; //500 at a time limit
                java.util.Collections.shuffle(entrantList); //shuffle for randomness

                //fill with shuffled list's first n number of users
                for(int i=0;i<toFill;i++){
                    User winner= entrantList.get(i);
                    //pushing in batches
                    batch.update(list.document(winner.getDeviceId()),"status", "Pending");
                    batchCount++;
                    if (batchCount >= 450) {
                        batch.commit();
                        batch=db.batch(); // Reset for the next 450
                        batchCount=0;
                    }
                }
                if (batchCount > 0) {
                    batch.commit().addOnCompleteListener(task -> {
                        if (onFinish != null) onFinish.run();
                    });
                } else {
                    if (onFinish != null) onFinish.run();
                }

            }
            else {if (onFinish != null) onFinish.run();}
        });
    }
}