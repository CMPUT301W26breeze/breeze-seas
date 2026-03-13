package com.example.breeze_seas;


import android.util.Log;

import com.google.firebase.firestore.AggregateSource;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class WaitingList extends StatusList {


    public WaitingList(Event event, int capacity) {
        super(event, capacity);
    }

    @Override
    protected String getStatusName() {
        return "waiting";
    }
}
