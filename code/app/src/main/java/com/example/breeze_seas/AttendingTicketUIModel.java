package com.example.breeze_seas;

/**
 * UI-facing model for confirmed attending tickets.
 *
 * <p>This remains separate from any eventual Firestore document shape so the repository can map
 * backend data into ticket-specific presentation fields later.
 */
public class AttendingTicketUIModel {

    private final String eventId;
    private final String title;
    private final String dateLabel;
    private final String locationLabel;
    private final String ticketTypeLabel;
    private final String entryNote;
    private final String actionLabel;

    public AttendingTicketUIModel(
            String eventId,
            String title,
            String dateLabel,
            String locationLabel,
            String ticketTypeLabel,
            String entryNote,
            String actionLabel
    ) {
        this.eventId = eventId;
        this.title = title;
        this.dateLabel = dateLabel;
        this.locationLabel = locationLabel;
        this.ticketTypeLabel = ticketTypeLabel;
        this.entryNote = entryNote;
        this.actionLabel = actionLabel;
    }

    public String getEventId() {
        return eventId;
    }

    public String getTitle() {
        return title;
    }

    public String getDateLabel() {
        return dateLabel;
    }

    public String getLocationLabel() {
        return locationLabel;
    }

    public String getTicketTypeLabel() {
        return ticketTypeLabel;
    }

    public String getEntryNote() {
        return entryNote;
    }

    public String getActionLabel() {
        return actionLabel;
    }
}
