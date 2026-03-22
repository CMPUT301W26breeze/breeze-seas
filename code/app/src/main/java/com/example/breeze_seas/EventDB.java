package com.example.breeze_seas;

import android.util.Log;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
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

        if (event.getEventId() == null || event.getEventId().trim().isEmpty()) {
            event.setEventId(genNewEventId());
        }

        String imageBase64 = event.getImage() == null ? "" : event.getImage().trim();
        Map<String, Object> eventMap = event.toMap();
        eventMap.remove("image");

        if (!imageBase64.isEmpty()) {
            String imageDocId = event.getEventId();
            eventMap.put("imageDocId", imageDocId);

            ImageDB.saveImage(imageDocId, imageBase64, new ImageDB.ImageMutationCallback() {
                @Override
                public void onSuccess() {
                    eventRef.document(event.getEventId())
                            .set(eventMap, SetOptions.merge())
                            .addOnSuccessListener(unused -> {
                                callback.onSuccess(event.getEventId());
                            })
                            .addOnFailureListener(callback::onFailure);
                }

                @Override
                public void onFailure(Exception e) {
                    callback.onFailure(e);
                }
            });
        } else {
            eventMap.put("imageDocId", "");
            eventRef.document(event.getEventId())
                    .set(eventMap, SetOptions.merge())
                    .addOnSuccessListener(unused -> {
                        callback.onSuccess(event.getEventId());
                    })
                    .addOnFailureListener(callback::onFailure);
        }
    }

    /**
     * Modifies an event collection from the database.
     *
     * @param event The event object to modifiy.
     * @param callback Callback method to run after firebase transaction.
     */
    public static void updateEvent(Event event, EventMutationCallback callback) {
        setup();
        event.setModifiedTimestamp(Timestamp.now());

        String imageBase64 = event.getImage() == null ? "" : event.getImage().trim();
        Map<String, Object> eventMap = event.toMap();
        eventMap.remove("image");

        if (!imageBase64.isEmpty()) {
            String imageDocId = event.getEventId();
            eventMap.put("imageDocId", imageDocId);

            ImageDB.saveImage(imageDocId, imageBase64, new ImageDB.ImageMutationCallback() {
                @Override
                public void onSuccess() {
                    eventRef.document(event.getEventId())
                            .set(eventMap, SetOptions.merge())
                            .addOnSuccessListener(unused -> callback.onSuccess())
                            .addOnFailureListener(callback::onFailure);
                }

                @Override
                public void onFailure(Exception e) {
                    callback.onFailure(e);
                }
            });
        } else {
            eventMap.put("imageDocId", "");

            ImageDB.deleteImage(event.getEventId(), new ImageDB.ImageMutationCallback() {
                @Override
                public void onSuccess() {
                    eventRef.document(event.getEventId())
                            .set(eventMap, SetOptions.merge())
                            .addOnSuccessListener(unused -> callback.onSuccess())
                            .addOnFailureListener(callback::onFailure);
                }

                @Override
                public void onFailure(Exception e) {
                    callback.onFailure(e);
                }
            });
        }
    }

    /**
     * Deletes an event collection from the database.
     * @param event The event object to delete
     * @param callback Callback method to run after firebase transaction.
     */
    public static void deleteEvent(Event event, EventMutationCallback callback) {
        setup();

        ImageDB.deleteImage(event.getEventId(), new ImageDB.ImageMutationCallback() {
            @Override
            public void onSuccess() {
                eventRef.document(event.getEventId())
                        .delete()
                        .addOnSuccessListener(unused -> callback.onSuccess())
                        .addOnFailureListener(callback::onFailure);
            }

            @Override
            public void onFailure(Exception e) {
                callback.onFailure(e);
            }
        });
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
    public static void getEventById(String eventId, LoadSingleEventCallback callback) {
        setup();
        eventRef.document(eventId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        loadEventWithImage(documentSnapshot, callback);
                    } else {
                        callback.onSuccess(null);
                    }
                })
                .addOnFailureListener(callback::onFailure);
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
                    hydrateMultiple(queryDocumentSnapshots, callback);
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
                    hydrateMultiple(queryDocumentSnapshots, callback);
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
                    hydrateMultiple(queryDocumentSnapshots, callback);
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
        for (DocumentSnapshot doc : documentSnapshots.getDocuments()) {
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
        String description = doc.getString("description") != null ? doc.getString("description") : "";
        String image = "";
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
        int eventCap = doc.getLong("eventCapacity") != null
                ? doc.getLong("eventCapacity").intValue()
                : -1;
        int waitCap = doc.getLong("waitingListCapacity") != null
                ? doc.getLong("waitingListCapacity").intValue()
                : -1;
        int drawRound = doc.getLong("drawARound") != null
                ? doc.getLong("drawARound").intValue()
                : 0;

        Event newEvent = new Event(
                eventId, isPrivate, organizerId, coOrganizerId, name, description, image, qrValue,
                created, modified, regStart, regEnd, eventStart, eventEnd,
                geo, eventCap, waitCap, drawRound,
                null, null, null, null
        );

        newEvent.setWaitingList(new WaitingList(newEvent, waitCap));
        newEvent.setPendingList(new PendingList(newEvent, eventCap));
        newEvent.setAcceptedList(new AcceptedList(newEvent, eventCap));
        newEvent.setDeclinedList(new DeclinedList(newEvent, -1));

        return newEvent;
    }

    /**
     * Loads the Base64 image string from ImageDB and then attaches it back to the event object.
     * Falls back to legacy inline image if imageDocId is missing.
     *
     * @param doc The event document snapshot
     * @param callback Callback returning a hydrated event object
     */
    private static void loadEventWithImage(DocumentSnapshot doc, LoadSingleEventCallback callback) {
        Event event = fromSingle(doc);
        if (event == null) {
            callback.onSuccess(null);
            return;
        }

        String imageDocId = doc.getString("imageDocId");
        String legacyInlineImage = doc.getString("image") != null ? doc.getString("image") : "";

        if (imageDocId == null || imageDocId.trim().isEmpty()) {
            event.setImage(legacyInlineImage);
            event.refreshListsFromDB();
            callback.onSuccess(event);
            return;
        }

        ImageDB.loadImage(imageDocId, new ImageDB.LoadImageCallback() {
            @Override
            public void onSuccess(String base64) {
                if (base64 == null || base64.trim().isEmpty()) {
                    event.setImage(legacyInlineImage);
                } else {
                    event.setImage(base64);
                }
                event.refreshListsFromDB();
                callback.onSuccess(event);
            }

            @Override
            public void onFailure(Exception e) {
                callback.onFailure(e);
            }
        });
    }

    /**
     * Hydrates all events from a query result and preserves the original query order.
     *
     * @param documentSnapshots Query results from Firestore
     * @param callback Callback returning hydrated events
     */
    private static void hydrateMultiple(QuerySnapshot documentSnapshots, LoadEventsCallback callback) {
        ArrayList<DocumentSnapshot> docs = new ArrayList<>(documentSnapshots.getDocuments());
        hydrateMultipleRecursive(docs, 0, new ArrayList<>(), callback);
    }

    /**
     * Recursive helper used by hydrateMultiple to load image data for each event.
     *
     * @param docs Event document snapshots
     * @param index Current index
     * @param events Hydrated events collected so far
     * @param callback Callback returning hydrated events
     */
    private static void hydrateMultipleRecursive(ArrayList<? extends DocumentSnapshot> docs,
                                                 int index,
                                                 ArrayList<Event> events,
                                                 LoadEventsCallback callback) {
        if (index >= docs.size()) {
            callback.onSuccess(events);
            return;
        }

        loadEventWithImage(docs.get(index), new LoadSingleEventCallback() {
            @Override
            public void onSuccess(Event event) {
                if (event != null) {
                    events.add(event);
                }
                hydrateMultipleRecursive(docs, index + 1, events, callback);
            }

            @Override
            public void onFailure(Exception e) {
                callback.onFailure(e);
            }
        });
    }
}