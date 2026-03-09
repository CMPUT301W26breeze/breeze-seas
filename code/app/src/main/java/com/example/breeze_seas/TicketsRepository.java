package com.example.breeze_seas;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Temporary in-memory ticket source used to keep the Tickets UI decoupled from Firestore.
 *
 * <p>TODO:
 * - Replace the seeded lists with DBConnector-backed loading after the event/ticket schema exists.
 * - Keep this fragment-facing API stable so the Tickets UI does not need Firestore code.
 */
public final class TicketsRepository {

    public interface Listener {
        void onTicketsChanged();
    }

    private static final TicketsRepository INSTANCE = new TicketsRepository();

    private final List<Listener> listeners = new ArrayList<>();
    private final List<TicketUIModel> activeTickets = new ArrayList<>();
    private final List<AttendingTicketUIModel> attendingTickets = new ArrayList<>();
    private final List<PastEventUIModel> pastTickets = new ArrayList<>();

    private TicketsRepository() {
        seedDemoData();
    }

    @NonNull
    public static TicketsRepository getInstance() {
        return INSTANCE;
    }

    public void addListener(@NonNull Listener listener) {
        synchronized (this) {
            if (!listeners.contains(listener)) {
                listeners.add(listener);
            }
        }
    }

    public void removeListener(@NonNull Listener listener) {
        synchronized (this) {
            listeners.remove(listener);
        }
    }

    @NonNull
    public List<TicketUIModel> getActiveTickets() {
        synchronized (this) {
            return new ArrayList<>(activeTickets);
        }
    }

    @NonNull
    public List<AttendingTicketUIModel> getAttendingTickets() {
        synchronized (this) {
            return new ArrayList<>(attendingTickets);
        }
    }

    @NonNull
    public List<PastEventUIModel> getPastTickets() {
        synchronized (this) {
            return new ArrayList<>(pastTickets);
        }
    }

    public void acceptInvitation(@NonNull TicketUIModel ticket) {
        boolean changed = false;

        synchronized (this) {
            if (removeActionRequiredTicket(ticket.getEventId())) {
                if (!containsAttendingTicket(ticket.getEventId())) {
                    attendingTickets.add(0, buildAttendingTicket(ticket));
                }
                changed = true;
            }
        }

        if (changed) {
            notifyListeners();
        }
    }

    public void declineInvitation(@NonNull TicketUIModel ticket) {
        boolean changed = false;

        synchronized (this) {
            changed = removeActionRequiredTicket(ticket.getEventId());
        }

        if (changed) {
            notifyListeners();
        }
    }

    private void seedDemoData() {
        activeTickets.add(new TicketUIModel(
                "e1",
                "Piano Lessons for Beginners",
                "Registration closes tomorrow",
                TicketUIModel.Status.PENDING
        ));
        activeTickets.add(new TicketUIModel(
                "e2",
                "Tech Conference 2026",
                "Lottery drawn • Backup pool",
                TicketUIModel.Status.BACKUP
        ));
        activeTickets.add(new TicketUIModel(
                "e3",
                "Community Dance Night",
                "Invitation expires at 6:00 PM",
                TicketUIModel.Status.ACTION_REQUIRED
        ));

        attendingTickets.add(new AttendingTicketUIModel(
                "a1",
                "Summer Music Festival",
                "Sat, Jul 18 • 7:30 PM",
                "Riverfront Stage, Edmonton",
                "Admit One",
                "Gate opens at 6:45 PM. Keep this ticket ready for the usher.",
                "Open QR pass"
        ));
        attendingTickets.add(new AttendingTicketUIModel(
                "a2",
                "Art Gallery Opening",
                "Fri, Oct 2 • 6:30 PM",
                "North Hall Gallery",
                "Reserved Entry",
                "Present this pass at the entrance desk for your reserved time slot.",
                "Open QR pass"
        ));

        pastTickets.add(new PastEventUIModel(
                "Beginner Swimming Lessons",
                "Wed, Jan 15 • 5:30 PM",
                "Kinsmen Sports Centre",
                "Attended",
                "Completed successfully",
                R.drawable.ic_ticket
        ));
        pastTickets.add(new PastEventUIModel(
                "Piano Lessons for Beginners",
                "Mon, Feb 10 • 4:00 PM",
                "West End Music Studio",
                "Not selected",
                "Lottery closed without selection",
                R.drawable.ic_info
        ));
        pastTickets.add(new PastEventUIModel(
                "Community Dance Night",
                "Sat, Feb 22 • 8:00 PM",
                "Old Strathcona Hall",
                "Declined",
                "Invitation released back to the pool",
                R.drawable.ic_clock
        ));
    }

    private boolean removeActionRequiredTicket(@NonNull String eventId) {
        for (int i = 0; i < activeTickets.size(); i++) {
            TicketUIModel ticket = activeTickets.get(i);
            if (ticket.getEventId().equals(eventId)
                    && ticket.getStatus() == TicketUIModel.Status.ACTION_REQUIRED) {
                activeTickets.remove(i);
                return true;
            }
        }
        return false;
    }

    private boolean containsAttendingTicket(@NonNull String eventId) {
        for (AttendingTicketUIModel ticket : attendingTickets) {
            if (ticket.getEventId().equals(eventId)) {
                return true;
            }
        }
        return false;
    }

    @NonNull
    private AttendingTicketUIModel buildAttendingTicket(@NonNull TicketUIModel ticket) {
        if ("e3".equals(ticket.getEventId())) {
            return new AttendingTicketUIModel(
                    ticket.getEventId(),
                    ticket.getTitle(),
                    "Sat, Mar 21 • 7:00 PM",
                    "East Hall, Edmonton Arts Centre",
                    "Dance Floor Entry",
                    "Doors open at 6:15 PM. Show this code at the main check-in desk.",
                    "Open QR pass"
            );
        }

        return new AttendingTicketUIModel(
                ticket.getEventId(),
                ticket.getTitle(),
                ticket.getDateLabel(),
                "Venue details available in the event page",
                "General Admission",
                "Show this ticket during event check-in.",
                "Open QR pass"
        );
    }

    private void notifyListeners() {
        List<Listener> snapshot;

        synchronized (this) {
            snapshot = new ArrayList<>(listeners);
        }

        for (Listener listener : snapshot) {
            listener.onTicketsChanged();
        }
    }
}
