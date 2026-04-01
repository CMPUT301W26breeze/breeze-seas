package com.example.breeze_seas;


import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
     * <p>
     * This method refreshes the {@code waitingList}, increments the {@code drawARound}
     * counter in Firestore, shuffles the pool of users to ensure randomness, and
     * moves a number of users (up to the {@code capacity}) into the {@code pendingList}.
     * </p>
     * @param finalListener The {@link StatusList.ListUpdateListener} to notify when the
     * entire lottery process and all database changes are complete.
     */

    public void onRunLottery(StatusList.ListUpdateListener finalListener) {
        FirebaseFirestore db = DBConnector.getDb();

        db.collection("events").document(event.getEventId())
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


                    int BATCH_LIMIT = 450;
                    final int[] writes = {0};
                    final boolean[] batchError = {false};


                    db.collection("events").document(event.getEventId())
                            .update("drawARound", FieldValue.increment(1));

                    for (int i = 0; i < totalToSelect; i += BATCH_LIMIT) {
                        WriteBatch batch = db.batch();
                        int end = Math.min(i + BATCH_LIMIT, totalToSelect);
                        int currentBatchSize = end - i;

                        for (int j = i; j < end; j++) {
                            User winner = pool.get(j);
                            DocumentReference participantRef = db.collection("events")
                                    .document(event.getEventId())
                                    .collection("participants")
                                    .document(winner.getDeviceId());

                            Map<String, Object> update = new HashMap<>();
                            update.put("deviceId", winner.getDeviceId());
                            update.put("status", "pending");
                            batch.set(participantRef, update, SetOptions.merge());
                        }

                        batch.commit().addOnCompleteListener(task -> {
                            if (!task.isSuccessful()) {
                                batchError[0] = true;
                                if (finalListener != null) finalListener.onError(task.getException());
                                return;
                            }

                            writes[0] += currentBatchSize;
                            if (writes[0] >= totalToSelect && !batchError[0]) {
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