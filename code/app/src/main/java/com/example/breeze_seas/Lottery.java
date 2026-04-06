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
    private final List<User> lastRoundWinners = new ArrayList<>();
    private final List<User> lastRoundLosers = new ArrayList<>();

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
     * Returns a copy of the winners from the last executed lottery.
     * @return List of {@link User} objects who were selected.
     */
    public List<User> getLastRoundWinners() {
        return new ArrayList<>(lastRoundWinners);
    }

    /**
     * Returns a copy of the losers from the last executed lottery.
     * @return List of {@link User} objects who were not selected.
     */
    public List<User> getLastRoundLosers() {
        return new ArrayList<>(lastRoundLosers);
    }

    /**
     * Executes the lottery selection process.
     * @param finalListener The {@link StatusList.ListUpdateListener} to notify when complete.
     */
    public void onRunLottery(StatusList.ListUpdateListener finalListener) {
        FirebaseFirestore db = DBConnector.getDb();
        String eventId = event.getEventId();

        //find how many spots are already taken
        db.collection("events").document(eventId)
                .collection("participants")
                .whereIn("status", java.util.Arrays.asList("pending", "accepted"))
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int currentOccupancy = queryDocumentSnapshots.size();

                    // Stop if the event is already full
                    if (currentOccupancy >= capacity) {
                        if (finalListener != null) {
                            finalListener.onError(new Exception("Event is already at full capacity!"));
                        }
                        return;
                    }

                    int remainingSpots = capacity - currentOccupancy;
                    List<User> pool = new ArrayList<>(waitingList.getUserList());

                    //exit if the waiting list is empty
                    if (pool == null || pool.isEmpty()) {
                        lastRoundWinners.clear();
                        lastRoundLosers.clear();
                        if (finalListener != null) finalListener.onUpdate();
                        return;
                    }

                    Collections.shuffle(pool);
                    int totalToSelect = Math.min(remainingSpots, pool.size());

                    // Divide the pool into winners and those who stay in the waiting list
                    final List<User> winners = new ArrayList<>(pool.subList(0, totalToSelect));
                    final List<User> losers = new ArrayList<>(pool.subList(totalToSelect, pool.size()));
                    lastRoundWinners.clear();
                    lastRoundWinners.addAll(winners);
                    lastRoundLosers.clear();
                    lastRoundLosers.addAll(losers);

                    final AtomicInteger writesCompleted = new AtomicInteger(0);
                    final AtomicBoolean hasFailed = new AtomicBoolean(false);
                    final int BATCH_LIMIT = 450;
                    final List<User> participantsToUpdate = new ArrayList<>(winners.size() + losers.size());
                    participantsToUpdate.addAll(winners);
                    participantsToUpdate.addAll(losers);
                    final java.util.Set<String> winnerIds = new java.util.HashSet<>();
                    for (User winner : winners) {
                        winnerIds.add(winner.getDeviceId());
                    }

                    db.collection("events").document(eventId)
                            .update("drawARound", FieldValue.increment(1));

                    if (participantsToUpdate.isEmpty()) {
                        if (finalListener != null) finalListener.onUpdate();
                        return;
                    }

                    for (int i = 0; i < participantsToUpdate.size(); i += BATCH_LIMIT) {
                        WriteBatch batch = db.batch();
                        int end = Math.min(i + BATCH_LIMIT, participantsToUpdate.size());
                        final int currentBatchSize = end - i;

                        for (int j = i; j < end; j++) {
                            User participant = participantsToUpdate.get(j);
                            DocumentReference participantRef = db.collection("events")
                                    .document(eventId)
                                    .collection("participants")
                                    .document(participant.getDeviceId());

                            Map<String, Object> update = new HashMap<>();
                            update.put("deviceId", participant.getDeviceId());
                            update.put("status", winnerIds.contains(participant.getDeviceId())
                                    ? "pending"
                                    : "waiting");
                            batch.set(participantRef, update, SetOptions.merge());
                        }

                        batch.commit().addOnCompleteListener(task -> {
                            if (hasFailed.get()) return;

                            if (!task.isSuccessful()) {
                                hasFailed.set(true);
                                if (finalListener != null) finalListener.onError(task.getException());
                                return;
                            }

                            if (writesCompleted.addAndGet(currentBatchSize) >= participantsToUpdate.size()) {
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
