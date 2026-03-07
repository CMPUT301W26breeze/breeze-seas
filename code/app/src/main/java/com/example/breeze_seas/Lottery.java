package com.example.breeze_seas;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

public class Lottery {

    private FirebaseFirestore db; //the database
    private String event; //to uniquely identify the event
    private int size; //how many entrants the event can take
    private ArrayList<User> entrantList; //need to fill this with eligible users (/i.e still waiting)
    public Lottery(String event){
        this.event=event;
        this.size=0;
        this.entrantList=null;
        this.db=DBConnector.getDb();
    }
    public void runLottery(int capacity){
        CollectionReference list=db.collection("Events").document(event)
                .collection("WaitingList");
        list.get().addOnCompleteListener(op->{
            if (op.isSuccessful() && op.getResult()!=null){
                this.entrantList=new ArrayList<User>();
                int invited=0;
                for(DocumentSnapshot doc: op.getResult()){
                    if(doc.get("Status")=="Pending" || doc.get("Status")=="Cancelled") {
                        invited+=1;
                        continue;
                    }
                    User entrant=doc.toObject(User.class);
                    if (entrant!=null) this.entrantList.add(entrant); //eligible users only
                }
                int slots=size-invited;
                ArrayList<User> lotteryPush;
                if (slots>0 && !entrantList.isEmpty()){
                    for(int i=0;i<slots;i++){
                        java.util.Collections.shuffle(entrantList); //shuffle for randomness

                    }


                }

            }
        });
    }
}
