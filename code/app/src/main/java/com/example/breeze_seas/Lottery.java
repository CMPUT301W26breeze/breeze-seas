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
import com.google.firebase.firestore.WriteBatch;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Lottery {
    private final Event event;
    private final WaitingList waitingList;
    private final PendingList pendingList;
    private final int capacity;


    public Lottery(Event event) {
        this.event = event;
        this.capacity = event.getEventCapacity();
        this.waitingList = new WaitingList(event, event.getWaitingListCapacity());
        this.pendingList = new PendingList(event, event.getEventCapacity());
    }
    public void onRunLottery(StatusList.ListUpdateListener finalListener) {


        waitingList.refresh(new StatusList.ListUpdateListener() {
            @Override
            public void onUpdate() {
                List<User> pool = waitingList.getUserList();
                if (pool.isEmpty()) {
                    if (finalListener != null){
                        finalListener.onUpdate();
                    }
                    return;
                }

                FirebaseFirestore db = DBConnector.getDb();
                db.collection("events").document(event.getEventId())
                        .update("drawARound", FieldValue.increment(1))
                        .addOnFailureListener(e -> {
                            Log.e("Lottery", "Failed to increment draw round", e);
                        });


                Collections.shuffle(pool);
                int slots= Math.min(capacity, pool.size());
                final int[] count = {0};


                for (int i = 0; i < slots; i++) {
                    User winner= pool.get(i);
                    pendingList.addUser(winner, new StatusList.ListUpdateListener() {
                        @Override
                        public void onUpdate() {
                            waitingList.removeUserFromDB(winner, null);
                            counter(count,slots, finalListener);
                        }
                        @Override
                        public void onError(Exception e) {
                            counter(count,slots,finalListener);
                        }
                    });
                }
            }


            @Override
            public void onError(Exception e) {
                if (finalListener != null) finalListener.onError(e);
            }
        });
    }


    private void counter(int[] count, int total, StatusList.ListUpdateListener listener) {
        count[0]++;
        if (count[0]==total && listener!=null) {
            listener.onUpdate();
        }
    }
}