package com.example.breeze_seas;

import android.util.Log;


import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
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

    // Get event by id
    public interface LoadSingleEventCallback {
        void onSuccess(Event event);
        void onFailure(Exception e);
    }

    // Get all events
    public interface LoadEventsCallback {
        void onSuccess(ArrayList<Event> events);
        void onFailure(Exception e);
    }

    // Modify / Delete events
    public interface EventMutationCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    /**
     * Synonym method of addEvent
     * @param event Event object to add to the database
     * @param callback Callback method to run after firebase transaction
     */
    public static void createEvent(Event event, AddEventCallback callback) {
        addEvent(event, callback);
    }

    /**
     * Add an event collection to database
     * @param event Event object to add to database
     * @param callback Callback method to run after firebase transaction
     */
    public static void addEvent(Event event, AddEventCallback callback) {
        setup();
        // Add event details
        eventRef.document(event.getEventId())
                .set(event.toMap(), SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    callback.onSuccess(event.getEventId());
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Modifies an event collection from the database.
     * @param event The event object to modifiy.
     * @param callback Callback method to run after firebase transaction.
     */
    public static void updateEvent(Event event, EventMutationCallback callback) {
        setup();
        eventRef.document(event.getEventId())
                .set(event.toMap(), SetOptions.merge())
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Deletes an event collection from the database.
     * @param event The event object to delete
     * @param callback Callback method to run after firebase transaction.
     */
    public static void deleteEvent(Event event, EventMutationCallback callback) {
        setup();
        eventRef.document(event.getEventId())
                .delete()
                .addOnSuccessListener(unused -> callback.onSuccess())
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

    /**
     * Fetches an event based on documentID
     * @param eventId The event document to fetch for.
     * @param callback Callback method to run after firebase transaction.
     */
    public static void getEventById(String eventId,LoadSingleEventCallback callback) {
        setup();
        eventRef.document(eventId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        callback.onSuccess(fromSingle(documentSnapshot));
                    } else {
                        callback.onSuccess(null);
                    }
                } ).addOnFailureListener(callback::onFailure);
    }

    /**
     * Fetches all events from the database.
     * @param callback Callback method to run after firebase transaction.
     */
    public static void getAllEvents(LoadEventsCallback callback) {
        setup();
        eventRef.orderBy("createdTimestamp")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    callback.onSuccess(fromMultiple(queryDocumentSnapshots));
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Fetch all events that the current user is able to join. Registration is open and is not the organizer for event
     * @param user User to find joinable events fr
     * @param callback Callback method to run after firebase transaction.
     */
    public static void getAllJoinableEvents(User user, LoadEventsCallback callback) {
        setup();
        // Get userID
        String userId = user.getDeviceId();

        eventRef.whereLessThan("registrationStartTimestamp", Timestamp.now())
                .whereGreaterThan("registrationEndTimestamp", Timestamp.now())
                .whereNotEqualTo("organizerId", userId)
                .orderBy("registrationEndTimestamp")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    callback.onSuccess(fromMultiple(queryDocumentSnapshots));
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Fetch all events that the user is organizing
     * @param user User to check the id of organizers.
     * @param callback Callback method to run after firebase transaction.
     */
    public static void getAllEventsOrganizedByUser(User user, LoadEventsCallback callback) {
        setup();
        // Get userID
        String userId = user.getDeviceId();

        eventRef.whereEqualTo("organizerId", userId)
                .orderBy("registrationStartTimestamp")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    callback.onSuccess(fromMultiple(queryDocumentSnapshots));
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Helper method to generate arraylist of events.
     * @param documentSnapshots The querySnapshot to generate events from
     * @return ArrayList of events
     */
    private static ArrayList<Event> fromMultiple(QuerySnapshot documentSnapshots) {
        ArrayList<Event> events = new ArrayList<>();
        for (QueryDocumentSnapshot doc : documentSnapshots) {
            Event event = EventDB.fromSingle(doc);
            if (event != null) {
                events.add(event);
            }
        }
        return events;
    }

    /**
     * Helper method that takes a document snapshot and converts it into an event object
     * @param doc The document snapshot of event document
     * @return The event object
     */
    private static Event fromSingle(DocumentSnapshot doc) {
        if (doc == null || !doc.exists()) return null;

        //Values
        String eventId = doc.getId();
        boolean isPrivate = Boolean.TRUE.equals(doc.getBoolean("isPrivate"));
        String organizerId = doc.getString("organizerId");
        ArrayList<String> coOrganizerId = new ArrayList<String>();
        String name = doc.getString("name");
        String description = doc.getString("description")!=null ? doc.getString("description") : "";
        // Fetch image object down below
        String imageId = doc.getString("imageDocId") != null ? doc.getString("imageDocId") : "";

        String qrValue = doc.getString("qrValue") != null ? doc.getString("qrValue") : "";

        //timestamps
        Timestamp created = doc.getTimestamp("createdTimestamp");
        Timestamp modified = doc.getTimestamp("modifiedTimestamp");
        Timestamp regStart = doc.getTimestamp("registrationStartTimestamp");
        Timestamp regEnd = doc.getTimestamp("registrationEndTimestamp");
        Timestamp eventStart = doc.getTimestamp("eventStartTimestamp");
        Timestamp eventEnd = doc.getTimestamp("eventEndTimestamp");

        boolean geo = Boolean.TRUE.equals(doc.getBoolean("geolocationEnforced")); // false if null
        //int vals
        int eventCap = doc.getLong("eventCapacity").intValue();
        int waitCap = doc.getLong("waitingListCapacity") != null ? doc.getLong("waitingListCapacity").intValue() : -1;
        int drawRound = doc.getLong("drawARound") != null ? doc.getLong("drawARound").intValue() : 0;


        Event newEvent = new Event(
                eventId, isPrivate, organizerId, coOrganizerId, name, description, null, qrValue,
                created, modified, regStart, regEnd, eventStart, eventEnd,
                geo, eventCap, waitCap, drawRound,
                null, null, null, null
        );

        newEvent.setWaitingList(new WaitingList(newEvent, waitCap));
        newEvent.setPendingList(new PendingList(newEvent, eventCap));
        newEvent.setAcceptedList(new AcceptedList(newEvent, eventCap));
        newEvent.setDeclinedList(new DeclinedList(newEvent, -1));
        newEvent.refreshListsFromDB();

        // TODO: fetch image document, get imageId and base64 string
        Image newImage = new Image(imageId, "");
        newEvent.setImage(newImage);

        return newEvent;
    }
}