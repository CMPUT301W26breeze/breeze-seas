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


/**
 * Represents the "waiting" status for an event.
 * Handles users who have registered but have not yet been selected or left.
 */

public class WaitingList extends StatusList {

    /**
     * Constructs a new WaitingList for a specific event.
     * @param event {@link Event} object this waiting list belongs to.
     * @param capacity The maximum number of entrants allowed on this waiting list.
     */

    public WaitingList(Event event, int capacity) {
        super(event, capacity);
    }

    /**
     * Defines the unique status identifier for this list type in Firestore.
     * @return A string literal "waiting" used to filter participant documents.
     */

    @Override
    protected String getStatusName() {
        return "waiting";
    }
}
