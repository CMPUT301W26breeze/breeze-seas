package com.example.breeze_seas;

import android.util.Log;


import com.google.android.gms.tasks.OnFailureListener;
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
                .add(map)
                .addOnSuccessListener(docRef -> {

                })
                .addOnFailureListener(callback::onFailure);

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
//        eventRef
//                .orderBy("regFromMillis")
//                .get()
//                .addOnSuccessListener(queryDocumentSnapshots -> {
//                    List<Event> events = new ArrayList<>();
//                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
//                        Event event = Event.fromDocument(doc);
//                        if (event != null) {
//                            events.add(event);
//                        }
//                    }
//                    callback.onSuccess(events);
//                })
//                .addOnFailureListener(callback::onFailure);
    }




    // Get event by id
    public interface LoadSingleEventCallback {
        void onSuccess(Event event);
        void onFailure(Exception e);
    }
    public static void getEventById(String eventId, LoadSingleEventCallback callback) {
        setup();

        // TODO: Return event object by ID
        // TODO: Reconstruct list classes from the participants sub-collection;
//        eventRef
//                .document(eventId)
//                .get()
//                .addOnSuccessListener(doc -> callback.onSuccess(Event.fromDocument(doc)))
//                .addOnFailureListener(callback::onFailure);
    }



//    public static Event fromDocument(DocumentSnapshot doc) {
//        if (doc == null || !doc.exists()) return null;
//
//        String name = doc.getString("name");
//        String details = doc.getString("details");
//        String posterUriString = doc.getString("posterUriString");
//
//        Long regFrom = doc.getLong("regFromMillis");
//        Long regTo = doc.getLong("regToMillis");
//        Long capLong = doc.getLong("waitingListCap");
//        Boolean geo = doc.getBoolean("geoRequired");
//
//        return new Event(
//                doc.getId(),
//                name == null ? "" : name,
//                details == null ? "" : details,
//                posterUriString,
//                regFrom == null ? 0L : regFrom,
//                regTo == null ? 0L : regTo,
//                capLong == null ? null : capLong.intValue(),
//                geo != null && geo
//        );

}
