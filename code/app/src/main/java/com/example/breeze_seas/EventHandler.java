package com.example.breeze_seas;

import static androidx.core.content.ContentProviderCompat.requireContext;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

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
    private Context context;
    private ListenerRegistration eventHandlerListener = null;
    private final HashMap<String, Event> imageListeners = new HashMap<String, Event>();
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
    public EventHandler(Context context, Query q) {
        this.context = context;
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
        // Check if title or description contains keyword
        if (e.toString().contains(keywordString)) {
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

        // Abort if listener is already active
        if (imageListeners.containsKey(e.getEventId())) {
            return;
        }

        // Start listener
        e.startImageListen(imageDocId);

        // Start observer
        e.getImageData().observeForever(imageObserver);

        // Add to hashmap
        imageListeners.put(e.getEventId(), e);
    }

    /**
     * Helper method to deactivate listeners from the hashmap for management purposes.
     * @param eventId Event object to remove listener from.
     */
    private void removeImageListener(String eventId) {
        // Check if listener is in hashmap
        if (imageListeners.containsKey(eventId)) {
            // Get live object
            Event e = imageListeners.get(eventId);
            if (e == null) {
                return;
            }

            // Stop observer
            e.getImageData().removeObserver(imageObserver);

            // Stop listener
            e.stopImageListen();
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
                    Toast.makeText(context, "EventHolder failed to listen.", Toast.LENGTH_SHORT).show();
                    // TODO: Stop the app?
                    return;  // This will automatically close listener
                }

                // Handle each new event
                Event eventTmp;
                Object tmpImageDocId;
                for (DocumentChange dc : eventDocs.getDocumentChanges()) {
                    switch (dc.getType()) {
                        case ADDED:
                            Log.d("EventHandler Class", "New event: " + dc.getDocument().getData());

                            // Create event
                            eventTmp = new Event(dc.getDocument().getData());
                            queryListOfEvents.add(eventTmp);

                            // Attach image listeners if applicable
                            tmpImageDocId = dc.getDocument().getData().get("imageDocId");
                            if (tmpImageDocId != null) {
                                attachImageListener(eventTmp,
                                                    tmpImageDocId.toString());
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

                                // Edge case: image uploaded or gone?
                                tmpImageDocId = dc.getDocument().getData().get("imageDocId");
                                // imageDocId exists
                                if (tmpImageDocId != null) {
                                    attachImageListener(modifiedEvent,
                                            tmpImageDocId.toString());
                                }
                                // No imageDocId
                                if (tmpImageDocId == null) {
                                    removeImageListener(modifiedEvent.getEventId());
                                }

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
                tmpImageDocId = null;

                Toast.makeText(context, "EventHolder failed to listen.", Toast.LENGTH_SHORT).show();
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
