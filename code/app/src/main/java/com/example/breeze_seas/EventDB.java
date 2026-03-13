package com.example.breeze_seas;

import android.util.Log;


import com.google.android.gms.tasks.OnFailureListener;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EventDB {
    private static FirebaseFirestore db;
    private static CollectionReference eventRef;
    private static boolean setup = false;

    private EventDB() {
    }

    private static void setup() {
        if (!setup) {
            db = DBConnector.getDb();
            eventRef = db.collection("events");
            setup = true;
        }
    }

    /**
     * Generate a new document ID from database.
     * @return the new document ID
     */
    public static String genNewEventId() {
        setup();
        return eventRef.document().getId();
    }

    // Add Event to DB
    public interface AddEventCallback {
        void onSuccess(String eventId);
        void onFailure(Exception e);
    }

    /**
     * Synonym method of addEvent
     *
     * @param event Event object to add to the database
     * @param callback Callback method to run after firebase transaction
     */
    public static void createEvent(Event event, AddEventCallback callback) {
        addEvent(event, callback);
    }

    /**
     * Add an event collection to database
     *
     * @param event Event object to add to database
     * @param callback Callback method to run after firebase transaction
     */
    public static void addEvent(Event event, AddEventCallback callback) {
        setup();

        // Safety
        if (event.getEventId() == null) {
            Log.e("DB_ERROR", "Cannot add Event to DB, eventID already exists.");
            throw new IllegalArgumentException("EventID already exists");
        }

        // Decompose event class
        Map<String, Object> map = event.toMap();

        // Add event details
        eventRef
                .document(event.getEventId())
                .set(map, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    // Add
                    callback.onSuccess(event.getEventId());
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * helper method to add participants to database
     * @param event event class that contains the participants
     */
    private static void addParticipants(Event event) {
        // Mange all list classes
        WaitingList waitingList = event.getWaitingList();
        PendingList pendingList = event.getPendingList();
        AcceptedList acceptedList = event.getAcceptedList();
        DeclinedList declinedList = event.getDeclinedList();

    }

    // Get all events
    public interface LoadEventsCallback {
        public void onSuccess(ArrayList<Event> events);
        public void onFailure(Exception e);
    }

    /**
     * Fetches all events from the database.
     * @param callback Callback method to run after firebase transaction.
     */
    public static void getAllEvents(LoadEventsCallback callback) {
        setup();

        eventRef.orderBy("registrationStartDate").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    ArrayList<Event> events = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Event event = EventDB.fromDocument(doc);
                        if (event != null) {
                            events.add(event);
                        }
                    }
                    callback.onSuccess(events);
                })
                .addOnFailureListener(callback::onFailure);
    }

    // Get event by id
    public interface LoadSingleEventCallback {
        void onSuccess(Event event);
        void onFailure(Exception e);
    }

    /**
     *
     * @param eventId
     * @param callback Callback method to run after firebase transaction.
     */
    public static void getEventById(String eventId,LoadSingleEventCallback callback){
        setup();
        eventRef.document(eventId).get().
                addOnSuccessListener(documentSnapshot ->{
                    if(documentSnapshot.exists()){
                        callback.onSuccess(EventDB.fromDocument(documentSnapshot));
                    }
                    else{
                        callback.onSuccess(null);
                    }
                } ).addOnFailureListener(callback::onFailure);

    }

    /**
     * Modifies an event collection from the database.
     *
     * @param event The event object to modifiy.
     * @param callback Callback method to run after firebase transaction.
     */
    public static void updateEvent(@NonNull Event event, @NonNull EventMutationCallback callback) {
        eventRef
                .document(event.getEventId())
                .set(event.toMap(), SetOptions.merge())
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Deletes an event collection from the database.
     *
     * @param event The event object to delete
     * @param callback Callback method to run after firebase transaction.
     */
    public static void deleteEvent(@NonNull Event event, @NonNull EventMutationCallback callback) {
        eventRef
                .document(event.getEventId())
                .delete()
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Helper method that takes a document snapshot and converts it into an event object
     *
     * @param doc The document snapshot of event document
     * @return The event object
     */
    private static Event fromDocument(DocumentSnapshot doc) {
        if (doc == null || !doc.exists()) return null;

        //Strings
        String eventId = doc.getId();
        String organizerId = doc.getString("organizerId");
        String name = doc.getString("name");
        String description = doc.getString("description")!=null ? doc.getString("description") : "";
        String image = doc.getString("image") != null ? doc.getString("image") : "";
        String qrValue = doc.getString("qrValue") != null ? doc.getString("qrValue") : "";

        boolean geo = Boolean.TRUE.equals(doc.getBoolean("geolocationEnforced")); // false if null

        //timestamps
        Timestamp dateCreated = doc.getTimestamp("dateCreated");
        Timestamp dateModified = doc.getTimestamp("dateModified");
        Timestamp regStart = doc.getTimestamp("registrationStartDate");
        Timestamp regEnd = doc.getTimestamp("registrationEndDate");
        Timestamp eventStart = doc.getTimestamp("eventStartDate");
        Timestamp eventEnd = doc.getTimestamp("eventEndDate");


        //int vals
        int eventCap = doc.getLong("eventCapacity").intValue();
        int waitCap = doc.getLong("waitingListCapacity") != null ? doc.getLong("waitingListCapacity").intValue() : -1;
        int drawRound = doc.getLong("drawARound") != null ? doc.getLong("drawARound").intValue() : 0;


        Event newEvent = new Event(
                eventId, organizerId, name, description, image, qrValue,
                dateCreated, dateModified, regStart, regEnd, eventStart, eventEnd,
                geo, eventCap, waitCap, drawRound,
                null, null, null, null
        );

        newEvent.setWaitingList(new WaitingList(newEvent, waitCap));
        newEvent.setPendingList(new PendingList(newEvent, eventCap));
        newEvent.setAcceptedList(new AcceptedList(newEvent, eventCap));
        newEvent.setDeclinedList(new DeclinedList(newEvent, -1));
        newEvent.refreshListsFromDB();

        return newEvent;
    }

}
