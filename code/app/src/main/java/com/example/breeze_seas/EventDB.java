package com.example.breeze_seas;

import android.util.Log;

import androidx.annotation.Nullable;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.Filter;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EventDB {
    private static FirebaseFirestore db;
    private static CollectionReference eventRef;
    private static boolean setup = false;
    // Holds the active collection-level listener so it can be detached later
    private static ListenerRegistration eventsListener;

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

    /**
     * Returns the event CollectionReference used in the database.
     * @return CollectionReference to the events collection
     */
    public static CollectionReference getEventRef() {
        setup();
        return eventRef;
    }

    // Add Event to DB
    public interface AddEventCallback {
        void onSuccess(String eventId);
        void onFailure(Exception e);
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

    public interface EventMutationCallback {
        void onSuccess();
        void onFailure(Exception e);
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
     * Deletes an event and all of its data from the database.
     *
     * <p>The original implementation only deleted the top level event document, leaving behind
     * orphaned documents in the "participants" subcollection.</p>
     *
     * <p>This version first fetches every document in the event's "participants" subcollection,
     * then uses a {@link WriteBatch} to delete all participant documents and the event document
     * together in a single write. Either everything is deleted or nothing is.</p>
     *
     * @param event    The event to delete.
     * @param callback Callback fired once all deletes are committed, or on failure.
     */
    public static void deleteEvent(Event event, EventMutationCallback callback) {
        setup();
        // Fetch the participants subcollection first so we can include them in the batch
        eventRef.document(event.getEventId())
                .collection("participants")
                .get()
                .addOnSuccessListener(participantSnapshots -> {
                    WriteBatch batch = db.batch();

                    // Queue each participant document for deletion
                    for (QueryDocumentSnapshot doc : participantSnapshots) {
                        batch.delete(doc.getReference());
                    }

                    // Queue the event document itself for deletion
                    batch.delete(eventRef.document(event.getEventId()));

                    // Commit everything at once
                    batch.commit()
                            .addOnSuccessListener(unused -> callback.onSuccess())
                            .addOnFailureListener(callback::onFailure);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Callbacks fired by the real-time events collection listener.
     *
     * <p>Unlike {@link Event#startEventListen}, which watches a single event document,
     * this listener watches the entire "events" collection. It is intended for admin screens
     * that need to display and react to changes across all events without a manual refresh.</p>
     */
    public interface EventsChangedCallback {
        void onEventAdded(Event event);
        void onEventModified(Event event);
        void onEventRemoved(Event event);
        void onFailure(Exception e);
    }

    /**
     * Attaches a real time listener to the entire "events" collection.
     * Any add, modify, or delete on any event document fires the appropriate callback.
     *
     * @param callback Receives individual change events or errors.
     */
    public static void startEventsListen(EventsChangedCallback callback) {
        setup();
        eventsListener = eventRef.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot snapshots,
                                @Nullable FirebaseFirestoreException error) {
                if (error != null) {
                    Log.w("EventDB", "Events listen failed.", error);
                    callback.onFailure(error);
                    return;
                }
                if (snapshots == null) return;

                // Only process documents that actually changed, not the full collection
                for (DocumentChange dc : snapshots.getDocumentChanges()) {
                    Event event = fromSingle(dc.getDocument());
                    if (event == null) continue;
                    switch (dc.getType()) {
                        case ADDED:
                            callback.onEventAdded(event);
                            break;
                        case MODIFIED:
                            callback.onEventModified(event);
                            break;
                        case REMOVED:
                            callback.onEventRemoved(event);
                            break;
                    }
                }
            }
        });
    }

    /**
     * Detaches the real-time collection listener.
     * Called when the screen is torn down to avoid memory leaks.
     */
    public static void stopEventsListen() {
        if (eventsListener != null) {
            eventsListener.remove();
            eventsListener = null;
        }
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

    // Get event by id
    public interface LoadSingleEventCallback {
        void onSuccess(Event event);
        void onFailure(Exception e);
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
     * Method to obtain query for all events.
     * @return Query for all events.
     */
    public static Query getAllEventsQuery() {
        setup();
        return eventRef.orderBy("createdTimestamp");
    }

    // Get all events
    public interface LoadEventsCallback {
        void onSuccess(ArrayList<Event> events);
        void onFailure(Exception e);
    }

    /**
     * Fetches all events from the database.
     * @param callback Callback method to run after firebase transaction.
     */
    public static void getAllEvents(LoadEventsCallback callback) {
        setup();
        getAllEventsQuery()
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    callback.onSuccess(fromMultiple(queryDocumentSnapshots));
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Method to obtain query for all joinable public events for given user.
     * @param user User to get query for.
     * @return Query for all joinable events for user.
     */
    public static Query getAllJoinableEventsQuery(User user) {
        setup();
        String userId = user.getDeviceId();
        return  eventRef.whereLessThan("registrationStartTimestamp", Timestamp.now())
                .whereGreaterThan("registrationEndTimestamp", Timestamp.now())
                .whereEqualTo("isPrivate", false)  // Public only events
                .whereNotEqualTo("organizerId", userId)
                .orderBy("registrationEndTimestamp");
    }

    /**
     * Fetch all events that the current user is able to join. Registration is open and is not the organizer for event
     * @param user User to find joinable events fr
     * @param callback Callback method to run after firebase transaction.
     */
    public static void getAllJoinableEvents(User user, LoadEventsCallback callback) {
        setup();
        getAllJoinableEventsQuery(user)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    callback.onSuccess(fromMultiple(queryDocumentSnapshots));
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Method to obtain all events that the user is any kind of organizer for.
     * @param user User that is an organizer.
     * @return Query for all events that is organized by the user.
     */
    public static Query getAllEventsOrganizedByUserQuery(User user) {
        setup();
        String userId = user.getDeviceId();
        return eventRef.where(Filter.or(
                        Filter.equalTo("organizerId", userId),
                        Filter.arrayContains("coOrganizerId", userId)))
                .orderBy("registrationStartTimestamp");
    }

    /**
     * Fetch all events that the user is organizing (any kind of organizer)
     * @param user User to check the id of organizers.
     * @param callback Callback method to run after firebase transaction.
     */
    public static void getAllEventsOrganizedByUser(User user, LoadEventsCallback callback) {
        setup();
        getAllEventsOrganizedByUserQuery(user)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    callback.onSuccess(fromMultiple(queryDocumentSnapshots));
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Method to obtain query for all events that the user is the original organizer for.
     * @param user User that is the original organizer.
     * @return Query for all events that is originally organized by the user.
     */
    public static Query getAllEventsOrganizedByOrganizerQuery(User user) {
        setup();
        String userId = user.getDeviceId();
        return eventRef.whereEqualTo("organizerId", userId)
                .orderBy("registrationStartTimestamp");
    }

    /**
     * Fetch all events that the user is organizing (the original organizer)
     * @param user User to check the id of organizers.
     * @param callback Callback method to run after firebase transaction.
     */
    public static void getAllEventsOrganizedByOrganizer(User user, LoadEventsCallback callback) {
        setup();
        getAllEventsOrganizedByOrganizerQuery(user)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    callback.onSuccess(fromMultiple(queryDocumentSnapshots));
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Method to obtain query for all events that the user is a co-organizer for.
     * @param user User that is the co-organizer.
     * @return Query for all events that is being co-organized by user.
     */
    public static Query getAllEventsOrganizedByCoOrganizerQuery(User user) {
        setup();
        String userId = user.getDeviceId();
        return eventRef.whereArrayContains("coOrganizerId", userId)
                .orderBy("registrationStartTimestamp");
    }

    /**
     * Fetch all events that the user is co-organizing.
     * @param user User to check the id of organizers.
     * @param callback Callback method to run after firebase transaction.
     */
    public static void getAllEventsOrganizedByCoOrganizer(User user, LoadEventsCallback callback) {
        setup();
        getAllEventsOrganizedByCoOrganizerQuery(user)
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

        // Convert into map object and pass as an argument
        Event newEvent = new Event(doc.getData());
        String imageDocId = (doc.getData().get("imageDocId") == null) ? null : doc.getData().get("imageDocId").toString();

        // Loading images
        // WARNING: this may cause a race condition.
        if (imageDocId != null) {
            ImageDB.loadImage(doc.getData().get("imageDocId").toString(), new ImageDB.LoadImageCallback() {
                @Override
                public void onSuccess(Image image) {
                    newEvent.setImage(image);
                }

                @Override
                public void onFailure(Exception e) {

                }
            });
        }

        // Setup lists
        newEvent.setWaitingList(new WaitingList(newEvent, newEvent.getWaitingListCapacity()));
        newEvent.setPendingList(new PendingList(newEvent, newEvent.getEventCapacity()));
        newEvent.setAcceptedList(new AcceptedList(newEvent, newEvent.getEventCapacity()));
        newEvent.setDeclinedList(new DeclinedList(newEvent, -1));
        newEvent.refreshListsFromDB();

        return newEvent;
    }
}