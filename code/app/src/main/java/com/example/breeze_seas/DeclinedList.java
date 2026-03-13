package com.example.breeze_seas;

/**
 * Represents the "declined" status for an event.
 * Handles users who have declined invitation for the event.
 */



public class DeclinedList extends StatusList{

    /**
     * Constructs a new DeclinedList for a specific event.
     * @param event {@link Event} object this declined list belongs to.
     * @param capacity Capacity is set to -1 for a declined list object
     */

    public DeclinedList(Event event, int capacity) {
        super(event, -1);
    }


    /**
     * Defines the unique status identifier for this list type in Firestore.
     * @return A string literal "declined" used to filter participant documents.
     */

    @Override
    protected String getStatusName() {
        return "declined";
    }
}
