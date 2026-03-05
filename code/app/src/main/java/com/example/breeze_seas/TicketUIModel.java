package com.example.breeze_seas;

/**
 * TicketUIModel represents a single event "ticket" entry displayed in the Tickets feature UI.
 *
 * <p>Role:
 * - Lightweight view model used by RecyclerView adapters to render ticket cards.
 * - This is NOT the final Firestore model; it exists to support UI wiring and prototyping.
 *
 * <p>Outstanding:
 * - Replace/extend with Firestore-backed ticket model once integration is implemented.
 */
public class TicketUIModel {

    public enum Status {
        PENDING,        // WAITING
        BACKUP,         // Not selected (backup pool)
        ACTION_REQUIRED // INVITED
    }

    private final String eventId;
    private final String title;
    private final String dateLabel;
    private final Status status;

    public TicketUIModel(String eventId, String title, String dateLabel, Status status) {
        this.eventId = eventId;
        this.title = title;
        this.dateLabel = dateLabel;
        this.status = status;
    }

    /** @return Firestore event id (or placeholder during prototyping). */
    public String getEventId() { return eventId; }

    /** @return Event title shown on the ticket card. */
    public String getTitle() { return title; }

    /** @return Human-readable date label (e.g., "Mon, Mar 4 • 6:00 PM"). */
    public String getDateLabel() { return dateLabel; }

    /** @return Ticket status used to drive chip text and click behavior. */
    public Status getStatus() { return status; }
}