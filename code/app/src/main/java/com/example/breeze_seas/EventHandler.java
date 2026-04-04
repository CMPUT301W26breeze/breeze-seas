package com.example.breeze_seas;

import static androidx.core.content.ContentProviderCompat.requireContext;

import android.app.Activity;
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

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Class to help manage a list of events with realtime updates and search filters.
 */
public class EventHandler {
    private final WeakReference<Activity> activity;
    private final Context context;
    private final String userId;
    private final boolean hideCoOrganizerEvents;
    private final boolean searchFilterEnabled;
    private ListenerRegistration eventHandlerListener = null;
    private final HashMap<String, Event> imageListeners = new HashMap<String, Event>();
    private final Observer<Image> imageObserver = new Observer<Image>() {
        @Override
        public void onChanged(Image image) {
            post();
        }
    };
    private final Query query;
    private final HashMap<String, Event> queryHashMapOfEvents = new HashMap<String, Event>();
    private String keywordString = "";
    @Nullable
    private Long availabilityStartMillis = null;
    @Nullable
    private Long availabilityEndMillis = null;
    @Nullable
    private Integer minCapacityFilter = null;
    @Nullable
    private Integer maxCapacityFilter = null;
    private final MutableLiveData<ArrayList<Event>> filteredListOfEventsData = new MutableLiveData<>(new ArrayList<Event>());
    private final MutableLiveData<Event> eventShownData = new MutableLiveData<>();

    /**
     * Creates EventHandler based on passed query.
     * @param activity Activity object. Used for exiting out of the app when firebase listener fails.
     * @param context Context object used for displaying toast messages.
     * @param q Query to generate events from.
     * @param userId DeviceId of current user. Used for co organizer checks.
     * @param hideCoOrganizerEvents Boolean dictating whether coOrganized Events should be displayed or not.
     * @param enableSearchFilter Boolean dictating whether filter computations should be enabled or not.
     */
    public EventHandler(Activity activity,
                        Context context,
                        Query q,
                        String userId,
                        boolean hideCoOrganizerEvents,
                        boolean enableSearchFilter) {
        this.activity = new WeakReference<>(activity);
        this.context = context;
        this.query = q;
        this.userId = userId;
        this.hideCoOrganizerEvents = hideCoOrganizerEvents;
        this.searchFilterEnabled = enableSearchFilter;
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

        // Check if filtering is enabled
        if (!searchFilterEnabled) {
            // Simply copy over
            filteredEvents.addAll(queryHashMapOfEvents.values());
            return;
        }

        // Apply filter
        for (Event e : queryHashMapOfEvents.values()) {
            if (matchesFilter(e)) {
                filteredEvents.add(e);
            }
        }
    }

    /**
     * Sorts the filteredListOfEvents in a specified order.
     * If search filter is enabled, sorted order is by registration deadline.
     * If search filter is disabled, sorted order is by creation date, latest at the top.
     * Notifying observers is done by {@link EventHandler#post()}
     */
    private void sort() {
        if (searchFilterEnabled) {  // Used by explore fragment
            // Sort by registration deadline.
            filteredListOfEventsData.getValue().sort(new Comparator<Event>() {
                @Override
                public int compare(Event event, Event t1) {
                    // event compared to t1 -> most upcoming deadlines at the top
                    return event.getRegistrationEndTimestamp().compareTo(t1.getRegistrationEndTimestamp());
                }
            });
        } else {  // Used by organize fragment
            // Sort by creation date.
            filteredListOfEventsData.getValue().sort(new Comparator<Event>() {
                @Override
                public int compare(Event event, Event t1) {
                    // t1 compared to event -> latest at the top
                    return t1.getCreatedTimestamp().compareTo(event.getCreatedTimestamp());
                }
            });
        }
    }

    /**
     * Helper method to determine if an event matches the filter
     * @param e Event object.
     * @return true if event matches filter, otherwise false.
     */
    private boolean matchesFilter(Event e) {
        return e.toString().contains(keywordString)
                && matchesAvailabilityFilter(e)
                && matchesCapacityFilter(e);
    }

    /**
     * Checks whether the event schedule overlaps the selected availability window.
     * Events without a schedule are excluded only when the availability filter is active.
     * @param e Event object.
     * @return true if the event falls within the chosen availability window.
     */
    private boolean matchesAvailabilityFilter(Event e) {
        if (availabilityStartMillis == null && availabilityEndMillis == null) {
            return true;
        }

        if (e.getEventStartTimestamp() == null && e.getEventEndTimestamp() == null) {
            return false;
        }

        long eventStartMillis = (e.getEventStartTimestamp() != null)
                ? e.getEventStartTimestamp().toDate().getTime()
                : Objects.requireNonNull(e.getEventEndTimestamp()).toDate().getTime();
        long eventEndMillis = (e.getEventEndTimestamp() != null)
                ? e.getEventEndTimestamp().toDate().getTime()
                : eventStartMillis;
        long filterStartMillis = (availabilityStartMillis == null) ? Long.MIN_VALUE : availabilityStartMillis;
        long filterEndMillis = (availabilityEndMillis == null) ? Long.MAX_VALUE : availabilityEndMillis;

        return eventEndMillis >= filterStartMillis && eventStartMillis <= filterEndMillis;
    }

    /**
     * Checks whether the event capacity is inside the selected capacity range.
     * @param e Event object.
     * @return true if the event capacity matches the selected range.
     */
    private boolean matchesCapacityFilter(Event e) {
        int eventCapacity = e.getEventCapacity();

        if (minCapacityFilter != null && eventCapacity < minCapacityFilter) {
            return false;
        }

        if (maxCapacityFilter != null && eventCapacity > maxCapacityFilter) {
            return false;
        }

        return true;
    }

    /**
     * Takes the passed string and updates the filteredListOfEvents.
     * @param keywordString Keyword search string.
     */
    public void setKeywordString(String keywordString) {
        this.keywordString = keywordString.trim().toLowerCase(Locale.US);
        filter();
        sort();
        post();
    }

    /**
     * Applies the advanced Explore filters and refreshes the filtered event list.
     * @param availabilityStartMillis Start of the selected availability window, or null.
     * @param availabilityEndMillis End of the selected availability window, or null.
     * @param minCapacityFilter Minimum capacity filter, or null.
     * @param maxCapacityFilter Maximum capacity filter, or null.
     */
    public void setAdvancedFilters(@Nullable Long availabilityStartMillis,
                                   @Nullable Long availabilityEndMillis,
                                   @Nullable Integer minCapacityFilter,
                                   @Nullable Integer maxCapacityFilter) {
        this.availabilityStartMillis = availabilityStartMillis;
        this.availabilityEndMillis = availabilityEndMillis;
        this.minCapacityFilter = minCapacityFilter;
        this.maxCapacityFilter = maxCapacityFilter;
        filter();
        sort();
        post();
    }

    /**
     * Clears all advanced Explore filters and refreshes the list.
     */
    public void clearAdvancedFilters() {
        setAdvancedFilters(null, null, null, null);
    }

    /**
     * Returns the currently selected availability start.
     * @return Start of the selected availability window, or null.
     */
    @Nullable
    public Long getAvailabilityStartMillis() {
        return availabilityStartMillis;
    }

    /**
     * Returns the currently selected availability end.
     * @return End of the selected availability window, or null.
     */
    @Nullable
    public Long getAvailabilityEndMillis() {
        return availabilityEndMillis;
    }

    /**
     * Returns the selected minimum event capacity filter.
     * @return Minimum capacity filter, or null.
     */
    @Nullable
    public Integer getMinCapacityFilter() {
        return minCapacityFilter;
    }

    /**
     * Returns the selected maximum event capacity filter.
     * @return Maximum capacity filter, or null.
     */
    @Nullable
    public Integer getMaxCapacityFilter() {
        return maxCapacityFilter;
    }

    /**
     * Returns whether any advanced Explore filter is active.
     * @return true if at least one advanced filter is set.
     */
    public boolean hasAdvancedFilters() {
        return availabilityStartMillis != null
                || availabilityEndMillis != null
                || minCapacityFilter != null
                || maxCapacityFilter != null;
    }

    /**
     * Returns the number of advanced Explore filters that are currently active.
     * Date range counts as a single filter pill.
     * @return Number of active advanced filters.
     */
    public int getActiveFilterCount() {
        int count = 0;

        if (availabilityStartMillis != null || availabilityEndMillis != null) {
            count++;
        }
        if (minCapacityFilter != null) {
            count++;
        }
        if (maxCapacityFilter != null) {
            count++;
        }

        return count;
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
     * @return Event object if present in query hashmap, otherwise null.
     */
    public Event findEventById(String eventId) {
        return queryHashMapOfEvents.get(eventId);
    }

    /**
     * Helper method to add an event object to the hashmap.
     * @param map Map object to generate the event from and to add to the hashmap.
     */
    private void addEvent(Map<String, Object> map) {
        Event e = new Event(map);
        // Because firebase does not have an arrayNotContains operation.
        // We must manually filter out co-organizers.
        // Check if user is a coOrganizer for this event
        if (hideCoOrganizerEvents && userIsCoOrganizer(e)) {
            Log.d("EventHandler Class", "Event is being co-organized, discarding... " + map);
            return;  // Skip adding event to hashmap
        }

        // Add to query hashmap
        queryHashMapOfEvents.put(e.getEventId(), e);

        // Attach image listeners if applicable
        Object tmpImageDocId = map.get("imageDocId");
        if (tmpImageDocId != null) {
            attachImageListener(e,
                    tmpImageDocId.toString());
        }

        // Successfully added event to hashmap
        return;
    }

    /**
     * Helper method to modify an event in the hashmap.
     * @param eventId EventId of event object to modify.
     * @param map Map object to update the event object with new information.
     */
    private void modifyEvent(String eventId, Map<String, Object> map) {
        // Helper variables
        Event eventTmp;
        Object tmpImageDocId;

        // First Identify which event object is different
        eventTmp = findEventById(eventId);

        // Edge case: event object previously removed because user is a co-organizer.
        // However, now the user is no longer the organizer for the event.
        // In Firebase's eyes, the document is modified.
        // findEventById would not be able to find the event, returning null.
        if (eventTmp == null && hideCoOrganizerEvents) {
            addEvent(map);
            return;
        }

        // Load new values
        eventTmp.loadMap(map);

        // This check is after event object loads the new values.
        // Check if user is a coOrganizer for this event.
        if (hideCoOrganizerEvents && userIsCoOrganizer(eventTmp)) {
            Log.d("EventHandler Class", "Event is now being co-organized, removing... " + map);
            removeEventById(eventTmp.getEventId());
            return;  // Skip logic below.
        }

        // Edge case: image uploaded or gone?
        tmpImageDocId = map.get("imageDocId");
        // imageDocId exists
        if (tmpImageDocId != null) {
            attachImageListener(eventTmp,
                    tmpImageDocId.toString());
        }
        // No imageDocId
        if (tmpImageDocId == null) {
            removeImageListener(eventTmp.getEventId());
        }

        // Check if event is presently shown
        // This is to update the event in EventDetailsFragment
        // The check relies on object reference, which should work
        if (eventTmp == eventShownData.getValue()) {
            postShown();  // Trigger observers
        }
    }

    /**
     * Helper method to remove the event object from query hashmap.
     * Called by the realtime listeners when an event document is removed or
     * when an event document becomes co organized by the user.
     * @param eventId String to identify the event object to be removed.
     */
    private void removeEventById(String eventId) {
        Event eventTmp = findEventById(eventId);
        if (eventTmp == null) {
            return;  // Do nothing if event doesn't exist in hashmap.
        }

        // Remove list classes listeners (the participants)
        eventTmp.stopListenAllLists();

        // Detach image listeners if applicable
        removeImageListener(eventId);

        // Remove event
        queryHashMapOfEvents.remove(eventId);

        // Check if event is presently shown
        // Event ID matches
        Event shownEvent = eventShownData.getValue();
        if (shownEvent != null && eventId.equals(shownEvent.getEventId())) {
            postShownDelete();  // Trigger observers
        }
    }

    /**
     * Helper method to save references to the image listeners hashmap for management purposes.
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

        // Start firebase cloud listener
        e.startImageListen(imageDocId);

        // Start MutableDataLive observer
        e.getImageData().observeForever(imageObserver);

        // Add to hashmap
        imageListeners.put(e.getEventId(), e);
    }

    /**
     * Helper method to deactivate listeners from the image listeners hashmap for management purposes.
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

            // Stop MutableDataLive observer
            e.getImageData().removeObserver(imageObserver);

            // Stop firebase cloud listener
            e.stopImageListen();
        }
    }

    /**
     * Helper method to deactivate all image listeners from the image listeners hashmap.
     * Only useful when the query realtime listener fails.
     */
    private void removeAllImageListeners() {
        for (Event e: imageListeners.values()) {
            // Stop MutableDataLive observer
            e.getImageData().removeObserver(imageObserver);

            // Stop firebase cloud listener
            e.stopImageListen();
        }
    }

    /**
     * Helper method to deactivate all participants listeners from all held events.
     * Only useful when the query realtime listener fails.
     */
    private void removeAllParticipantsListeners() {
        for (Event e: queryHashMapOfEvents.values()) {
            // Stop firebase cloud listener for all list classes
            e.stopListenAllLists();
        }
    }


    /**
     * Helper method to check if an event is being co-organized by the current user.
     * @param e Event object.
     * @return True if user is an coOrganizer, otherwise False.
     */
    private boolean userIsCoOrganizer(Event e) {
        return e.getCoOrganizerId().contains(userId);
    }

    /**
     * Starts listeners to get realtime updates of the event list.
     */
    public void startListen() {
        // Check whether the listener is already present
        if (eventHandlerListener != null) {
            return;
        }

        // Clear Event hashmap before starting listener
        queryHashMapOfEvents.clear();

        eventHandlerListener = query.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot eventDocs,
                                @Nullable FirebaseFirestoreException error) {
                // Check for errors
                if (error != null) {
                    Log.w("EventHandler Class", "Listen failed.", error);
                    // Stop all list classes listeners (the participants)
                    removeAllParticipantsListeners();

                    // Stop all image listeners
                    removeAllImageListeners();

                    // Message and exit app
                    Toast.makeText(context, "EventHandler failed to fetch events.", Toast.LENGTH_SHORT).show();
                    if (activity.get() != null) {
                        activity.get().finish();
                    }
                    return;  // This will automatically close listener
                }

                for (DocumentChange dc : eventDocs.getDocumentChanges()) {
                    switch (dc.getType()) {
                        case ADDED:
                            Log.d("EventHandler Class", "New event: " + dc.getDocument().getData());
                            // Create event
                            addEvent(dc.getDocument().getData());
                            break;

                        case MODIFIED:
                            Log.d("EventHandler Class", "Modified event: " + dc.getDocument().getData());
                            // Modify event
                            modifyEvent(dc.getDocument().getId(), dc.getDocument().getData());
                            break;

                        case REMOVED:
                            Log.d("EventHandler Class", "Removed event: " + dc.getDocument().getData());
                            // Delete event
                            removeEventById(dc.getDocument().getId());
                            break;
                    }
                }
                // Recompute after processing changes
                filter();
                sort();
                post();
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
            eventHandlerListener = null;
        }
    }
}
