package com.example.breeze_seas;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Lottery {
    private final FirebaseFirestore db;
    private String eventId;
    private int capacity;
    

    public Lottery(String eventId, int capacity){
        this.eventId=eventId;
        this.capacity=capacity;
        this.db=DBConnector.getDb();
    }


    public void runLottery(int capacity, Runnable onFinish) {
        if (capacity <= 0) {
            if (onFinish != null) onFinish.run();
            return;
        }

        // Reference event document to update drawRound
        DocumentReference eventRef = db.collection("events").document(eventId);
        // Reference to the sub-collection of entrants
        CollectionReference listRef = eventRef.collection("participants");

        listRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                ArrayList<String> eligibleIds = new ArrayList<>();
                int alreadyFilledSlots = 0;
                for (DocumentSnapshot doc : task.getResult()) {
                    String status = doc.getString("status");
                    if ("waiting".equals(status)) {
                        eligibleIds.add(doc.getId());
                    } else if ("pending".equals(status) || "accepted".equals(status)) {
                        // These people already have a spot reserved
                        alreadyFilledSlots++;
                    }
                }

                int slots=capacity - alreadyFilledSlots;
                int toFill=Math.min(slots, eligibleIds.size());

                if (toFill <= 0) {
                    if (onFinish != null) onFinish.run();
                    return;
                }

                // Randomize the eligible users
                java.util.Collections.shuffle(eligibleIds);

                WriteBatch batch = db.batch();
                int count = 0;

                for (int i = 0; i < toFill; i++) {
                    String winnerId = eligibleIds.get(i);
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("status", "pending");
                    updates.put("invitedAt", FieldValue.serverTimestamp());
                    batch.update(listRef.document(winnerId), updates);
                    count++;

                    if (count == 450) {
                        batch.commit();
                        batch = db.batch();
                        count = 0;
                    }
                }

                batch.update(eventRef, "drawRound", FieldValue.increment(1));
                batch.commit().addOnCompleteListener(task1 -> {
                    if (onFinish != null) onFinish.run();
                });
            } else {
                if (onFinish != null) onFinish.run();
            }
        });
    }
}
