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
            eventRef = db.collection("test_events");
            setup = true;
        }
    }

    // Takes event object and returns a hashmap
    private static Map<String, Object> getMap(Event event) {
        Map<String, Object> map = new HashMap<>();
        map.put("eventId", event.getEventId());
        map.put("organizerId", event.getOrganizerId());
        map.put("name", event.getName());
        map.put("description", event.getDescription());
        map.put("image", event.getImage());
        map.put("qrValue", event.getQrValue());
        map.put("dateCreated", event.getDateCreated());
        map.put("dateModified", event.getDateModified());
        map.put("registrationStartDate", event.getRegistrationStartDate());
        map.put("registrationEndDate", event.getRegistrationEndDate());
        map.put("eventStartDate", event.getEventStartDate());
        map.put("eventEndDate", event.getEventEndDate());
        map.put("geolocationEnforced", event.isGeolocationEnforced());
        map.put("eventCapacity", event.getEventCapacity());
        map.put("waitingListCapacity", event.getWaitingListCapacity());
        map.put("drawARound", event.getDrawARound());
        return map;
    }

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
     *
     * @param event
     * @param callback
     */
    public static void addEvent(Event event, AddEventCallback callback) {
        setup();

        // Safety
        if (event.getEventId() == null) {
            Log.e("DB_ERROR", "Cannot add Event to DB, eventID already exists.");
            throw new IllegalArgumentException("EventID already exists");
        }

        // Decompose event class
        Map<String, Object> map = getMap(event);

        // Add event details
        // TODO: After adding event collection, need to add participants
        eventRef
                .document(event.getEventId())
                .set(map)
                .addOnSuccessListener(unused -> {
                    // Add
                    callback.onSuccess(event.getEventId());
                })
                .addOnFailureListener(callback::onFailure);


    }
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
    public static void getAllEvents(LoadEventsCallback callback) {
        setup();

        // TODO: Fetch all events
        // TODO: For each event, grab event details, and reconstruct the the list classes from the participants sub-collection
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

    // TODO: to implement
    public void updateEvent(@NonNull Event event, @NonNull EventMutationCallback callback) {
        db.collection("events")
                .document(event.getId())
                .set(event.toMap())
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    // TODO: to implement

    public void deleteEvent(@NonNull String id, @NonNull EventMutationCallback callback) {
        db.collection("events")
                .document(id)
                .delete()
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }
    public interface LoadSingleEventCallback {
        void onSuccess(Event event);
        void onFailure(Exception e);
    }

    public static Event fromDocument(DocumentSnapshot doc) {
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

        return newEvent;
    }

}
