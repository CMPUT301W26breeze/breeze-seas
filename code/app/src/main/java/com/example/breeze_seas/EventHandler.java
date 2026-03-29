package com.example.breeze_seas;

import android.util.Log;

import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;

/**
 * Class to help manage a list of events with realtime updates and search filters.
 */
public class EventHandler {
    private ListenerRegistration eventHandlerListener = null;
    private final HashMap<String, MutableLiveData<Image>> imageListeners = new HashMap<String, MutableLiveData<Image>>();
    private final Observer<Image> imageObserver = new Observer<Image>() {
        @Override
        public void onChanged(Image image) {
            post();
        }
    };
    private final Query query;
    private final ArrayList<Event> queryListOfEvents = new ArrayList<Event>();
    private String keywordString = "";
    private final MutableLiveData<ArrayList<Event>> filteredListOfEventsData = new MutableLiveData<>(new ArrayList<Event>());
    private final MutableLiveData<Event> eventShownData = new MutableLiveData<>();

    /**
     * Creates EventHandler based on passed query.
     * @param q Query to generate events from.
     */
    public EventHandler(Query q) {
        this.query = q;
        startListen();
    }

    /**
     * Sets the eventShown. This is used to allow other fragments to observe this for event updates.
     * @param e The event object.
     */
    public void setEventShown(Event e) {
        this.eventShownData.setValue(e);
    }

    /**
     * Gets the eventShown. Other fragments may set up an observer in order to receive realtime updates.
     * @return MutableLiveData encapsulating the event.
     */
    public MutableLiveData<Event> getEventShown() {
        return eventShownData;
    }

    /**
     * Applies all filter options, and modifies filteredListOfEvents as a side effect.
     * Notifying observers is done by {@link EventHandler#post()}
     */
    private void filter() {
        // Refresh lists
        ArrayList<Event> filteredEvents = filteredListOfEventsData.getValue();
        filteredEvents.clear();

        // Apply filter
        for (Event e : queryListOfEvents) {
            if (matchesFilter(e)) {
                filteredEvents.add(e);
            }
        }
    }

    /**
     * Helper method to determine if an event matches the filter
     * @param e Event object.
     * @return true if event matches filter, otherwise false.
     */
    private boolean matchesFilter(Event e) {
        // Get name and description of event
        String name = e.getName() == null ? "" : e.getName().toLowerCase(Locale.US);
        String description = e.getDescription() == null ? "" : e.getDescription().toLowerCase(Locale.US);

        // Check if title or description contains keyword
        if (name.contains(keywordString) || description.contains(keywordString)) {
            return true;
        } else {
            return false;
        }
    }


    /**
     * Takes the passed string and updates the filteredListOfEvents.
     * @param keywordString Keyword search string.
     */
    public void setKeywordString(String keywordString) {
        this.keywordString = keywordString.trim().toLowerCase(Locale.US);
        filter();
        post();
    }


    /**
     * Returns the list of event to be shown in the adapter.
     * Use case is for callers to set up observers and update whenever the list changes.
     * @return MutableLiveData wrapping an ArrayList of events.
     */
    public MutableLiveData<ArrayList<Event>> getEvents() {
        return filteredListOfEventsData;
    }

    /**
     * Helper method to set the new array list.
     * (and as a side effect) triggers all observes listening on filteredListOfEvents.
     */
    private void post() {
        // Same reference to the same object, except the object now contains new values.
        // This essentially triggers all observers.
        filteredListOfEventsData.postValue(filteredListOfEventsData.getValue());
    }

    /**
     * Helper method to set the new eventShown.
     * Triggers all observers listening on eventShown.
     */
    private void postShown() {
        // Same reference to the same object, except the object now contains new values.
        // This essentially triggers all observers.
        eventShownData.postValue(eventShownData.getValue());
    }

    /**
     * Helper method to delete the eventShown.
     * Triggers all observers listening on eventShown.
     */
    private void postShownDelete() {
        eventShownData.postValue(null);
    }

    /**
     * Helper method to find and return event object based on eventId.
     * @param eventId String to identify the event object by.
     * @return Event object if present in list, otherwise null.
     */
    private Event findEventById(String eventId) {
        for (Event e: queryListOfEvents) {
            if (Objects.equals(e.getEventId(), eventId)) {
                return e;
            }
        }
        return null;
    }

    /**
     * Helper method to remove the event object from list.
     * @param eventId String to identify the event object to be removed.
     */
    private void removeEventById(String eventId) {
        for (Event e: queryListOfEvents) {
            if (Objects.equals(e.getEventId(), eventId)) {
                queryListOfEvents.remove(e);
                break;
            }
        }
    }

    /**
     * Helper method to save references to the hashmap for management purposes.
     * @param e Event class to initialize image listener from.
     * @param imageDocId DocumentId to set the listener to listen at.
     */
    private void attachImageListener(Event e, @Nullable String imageDocId) {
        // Abort if imageDocId is not valid
        if ((imageDocId == null) || (imageDocId.isEmpty())) {
            return;
        }

        // Start listener
        e.startImageListen(imageDocId);

        // Save reference to MutableLiveData object
        MutableLiveData<Image> tmpImageLiveData = e.getImageData();
        tmpImageLiveData.observeForever(imageObserver);

        // Add to hashmap
        imageListeners.put(e.getEventId(), tmpImageLiveData);
    }

    /**
     * Helper method to deactivate listeners from the hashmap for management purposes.
     * @param eventId Event object to remove listener from.
     */
    private void removeImageListener(String eventId) {
        // Check if listener is in hashmap
        if (imageListeners.containsKey(eventId)) {
            // Get live object
            MutableLiveData<Image> tmpImageLiveData = imageListeners.get(eventId);
            if (tmpImageLiveData == null) {
                return;
            }
            tmpImageLiveData.removeObserver(imageObserver);
        }
    }

    /**
     * Starts listeners to get realtime updates of the event list.
     */
    public void startListen() {
        // Check whether the listener is already present
        if (eventHandlerListener != null) {
            return;
        }

        // Clear Event list before starting listener
        queryListOfEvents.clear();

        eventHandlerListener = query.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot eventDocs,
                                @Nullable FirebaseFirestoreException error) {
                // Check for errors
                if (error != null) {
                    Log.w("EventHandler Class", "Listen failed.", error);
                    // TODO: Stop the app?
                    return;  // This will automatically close listener
                }

                // Handle each new event
                Event eventTmp;
                for (DocumentChange dc : eventDocs.getDocumentChanges()) {
                    switch (dc.getType()) {
                        case ADDED:
                            Log.d("EventHandler Class", "New event: " + dc.getDocument().getData());

                            // Create event
                            eventTmp = new Event(dc.getDocument().getData());
                            queryListOfEvents.add(eventTmp);

                            // Attach image listeners if applicable
                            if (dc.getDocument().getData().get("imageDocId") != null) {
                                attachImageListener(eventTmp,
                                                    dc.getDocument().getData().get("imageDocId").toString());
                            }
                            break;

                        case MODIFIED:
                            Log.d("EventHandler Class", "Modified event: " + dc.getDocument().getData());

                            // Modify event
                            // First Identify which event object is different
                            Event modifiedEvent = findEventById(dc.getDocument().getId());
                            if (modifiedEvent != null) {
                                // Load new values
                                modifiedEvent.loadMap(dc.getDocument().getData());

                                // Check if event is presently shown
                                // This is to update the event in EventDetailsFragment
                                // The check relies on object reference, which should work
                                // TODO: may be better to implement eventId check to be 100% sure.
                                if (modifiedEvent == eventShownData.getValue()) {
                                    postShown();  // Trigger observers
                                }
                            }
                            break;

                        case REMOVED:
                            Log.d("EventHandler Class", "Removed event: " + dc.getDocument().getData());

                            // Detach image listeners if applicable
                            removeImageListener(dc.getDocument().getId());

                            // Remove event
                            removeEventById(dc.getDocument().getId());

                            // Check if event is presently shown
                            // Event ID matches
                            if (dc.getDocument().getId().equals(eventShownData.getValue().getEventId())) {
                                postShownDelete();  // Trigger observers
                            }
                            break;
                    }
                }
                // Recompute after processing changes
                filter();
                post();

                // Unassign reference
                eventTmp = null;
            }
        });
    }

    /**
     * Stop fetching realtime updates of event list.
     */
    public void stopListen() {
        // Check if listener is active
        if (eventHandlerListener != null) {
            eventHandlerListener.remove();
        }
    }
}
