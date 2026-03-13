package com.example.breeze_seas;

/**
 * Represents the "pending" status for an event.
 * Handles users who have been selected for the event.
 */


public class PendingList extends StatusList {

    /**
     * Constructs a new PendingList for a specific event.
     * @param event {@link Event} object this pending list belongs to.
     * @param capacity The maximum number of entrants allowed on this pending list.
     */

    public PendingList(Event event, int capacity) {
        super(event, capacity);
    }

    /**
     * Updates the capacity for the PendingList object
     * @param newCapacity The new capacity
     * @return boolean True if the new capacity is greater than zero, False otherwise
     */
    public boolean tryUpdateCapacity(int newCapacity) {
        if (newCapacity < 0) return false;
        this.capacity = newCapacity;
        return true;
    }

    /**
     * Defines the unique status identifier for this list type in Firestore.
     * @return A string literal "pending" used to filter participant documents.
     */
    @Override
    protected String getStatusName() {
        return "pending";
    }
}
