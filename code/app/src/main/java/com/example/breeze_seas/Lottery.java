package com.example.breeze_seas;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Handles entrant selection process of an event by running a lottery
 * to pick random entrants.
 */
public class Lottery {
    private final Event event;
    private final WaitingList waitingList;
    private final int capacity;

    /**
     * Lottery constructor
     * @param event {@link Event} object
     */
    public Lottery(Event event) {
        this.event = event;
        this.capacity = event.getEventCapacity();
        this.waitingList = event.getWaitingList();
    }

    /**
     * Executes the lottery selection process.
     * @param finalListener The {@link StatusList.ListUpdateListener} to notify when complete.
     */
    public void onRunLottery(StatusList.ListUpdateListener finalListener) {
        FirebaseFirestore db = DBConnector.getDb();
        String eventId = event.getEventId();

        db.collection("events").document(eventId)
                .collection("participants")
                .whereIn("status", java.util.Arrays.asList("pending", "accepted"))
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int currentOccupancy = queryDocumentSnapshots.size();

                    if (currentOccupancy >= capacity) {
                        if (finalListener != null) {
                            finalListener.onError(new Exception("Event is already at full capacity!"));
                        }
                        return;
                    }

                    int remainingSpots = capacity - currentOccupancy;
                    List<User> pool = waitingList.getUserList();

                    if (pool == null || pool.isEmpty()) {
                        if (finalListener != null) finalListener.onUpdate();
                        return;
                    }

                    Collections.shuffle(pool);
                    int totalToSelect = Math.min(remainingSpots, pool.size());


                    final List<User> winners = new ArrayList<>(pool.subList(0, totalToSelect));


                    final AtomicInteger writesCompleted = new AtomicInteger(0);
                    final AtomicBoolean hasFailed = new AtomicBoolean(false);
                    final int BATCH_LIMIT = 450;

                    db.collection("events").document(eventId)
                            .update("drawARound", FieldValue.increment(1));

                    for (int i = 0; i < totalToSelect; i += BATCH_LIMIT) {
                        WriteBatch batch = db.batch();
                        int end = Math.min(i + BATCH_LIMIT, totalToSelect);
                        final int currentBatchSize = end - i;

                        for (int j = i; j < end; j++) {
                            User winner = winners.get(j);
                            DocumentReference participantRef = db.collection("events")
                                    .document(eventId)
                                    .collection("participants")
                                    .document(winner.getDeviceId());

                            Map<String, Object> update = new HashMap<>();
                            update.put("deviceId", winner.getDeviceId());
                            update.put("status", "pending");
                            batch.set(participantRef, update, SetOptions.merge());
                        }

                        batch.commit().addOnCompleteListener(task -> {
                            if (hasFailed.get()) return;

                            if (!task.isSuccessful()) {
                                hasFailed.set(true);
                                if (finalListener != null) finalListener.onError(task.getException());
                                return;
                            }


                            if (writesCompleted.addAndGet(currentBatchSize) >= totalToSelect) {
                                if (finalListener != null) finalListener.onUpdate();
                            }
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    if (finalListener != null) finalListener.onError(e);
                });
    }
}