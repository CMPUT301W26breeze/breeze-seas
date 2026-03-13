package com.example.breeze_seas;

/**
 * Represents the "accepted" status for an event.
 * Handles users who have accepted invite for the event.
 */

public class AcceptedList extends StatusList{

    /**
     * Constructs a new AcceptedList for a specific event.
     * @param event {@link Event} object this accepted list belongs to.
     * @param capacity The maximum number of entrants allowed on this accepted list.
     */

    public AcceptedList(Event event, int capacity) {
        super(event, capacity);
    }

    /**
     * Defines the unique status identifier for this list type in Firestore.
     * @return A string literal "accepted" used to filter participant documents.
     */

    @Override
    protected String getStatusName() {
        return "accepted";
    }
}
