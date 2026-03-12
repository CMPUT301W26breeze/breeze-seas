package com.example.breeze_seas;

import android.content.Context;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * TicketDB loads ticket-tab data and isolates Firestore access from the UI layer.
 *
 * <p>Current integration scope:
 * - uses the agreed device-based schema from {@code events/{eventId}/participants/{deviceId}}
 * - supports live Firestore loading for the agreed participant statuses
 * - keeps demo-mode seeding available for instrumented tests only
 *
 * <p>Outstanding:
 * - replace fallback display labels once the final event metadata contract is fully implemented
 */
public final class TicketDB {

    public interface Listener {
        void onTicketsChanged();
    }

    private static final String TAG = "TicketDB";
    private static final String EVENTS_COLLECTION = "events";
    private static final String PARTICIPANTS_COLLECTION = "participants";

    private static final TicketDB INSTANCE = new TicketDB();

    private final FirebaseFirestore db = DBConnector.getDb();
    private final List<Listener> listeners = new ArrayList<>();
    private final List<TicketUIModel> activeTickets = new ArrayList<>();
    private final List<AttendingTicketUIModel> attendingTickets = new ArrayList<>();
    private final List<PastEventUIModel> pastTickets = new ArrayList<>();

    private boolean useDemoData = false;
    @Nullable
    private String currentDeviceId;

    private TicketDB() {
    }

    @NonNull
    public static TicketDB getInstance() {
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

    public void refreshTickets(@NonNull Context context) {
        boolean showDemoData;

        synchronized (this) {
            showDemoData = useDemoData;
        }

        if (showDemoData) {
            notifyListeners();
            return;
        }

        String deviceId = getCurrentDeviceId(context.getApplicationContext());
        if (deviceId == null) {
            Log.w(TAG, "No current device id could be resolved for Tickets.");
            replaceTickets(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
            return;
        }

        synchronized (this) {
            currentDeviceId = deviceId;
        }

        loadTicketsForDeviceId(deviceId);
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
        boolean showDemoData;

        synchronized (this) {
            showDemoData = useDemoData;
            if (useDemoData && removeActionRequiredTicket(ticket.getEventId())) {
                if (!containsAttendingTicket(ticket.getEventId())) {
                    attendingTickets.add(0, buildAttendingTicket(ticket));
                }
                changed = true;
            }
        }

        if (changed) {
            notifyListeners();
            return;
        }

        if (!showDemoData) {
            updateParticipantStatus(ticket.getEventId(), "accepted");
        }
    }

    public void declineInvitation(@NonNull TicketUIModel ticket) {
        boolean changed = false;
        boolean showDemoData;

        synchronized (this) {
            showDemoData = useDemoData;
            if (useDemoData) {
                changed = removeActionRequiredTicket(ticket.getEventId());
            }
        }

        if (changed) {
            notifyListeners();
            return;
        }

        if (!showDemoData) {
            updateParticipantStatus(ticket.getEventId(), "declined");
        }
    }

    @Nullable
    private String getCurrentDeviceId(@NonNull Context context) {
        String androidId = Settings.Secure.getString(
                context.getContentResolver(),
                Settings.Secure.ANDROID_ID
        );

        if (androidId == null || androidId.trim().isEmpty()) {
            return null;
        }

        return androidId;
    }

    private void loadTicketsForDeviceId(@NonNull String deviceId) {
        db.collectionGroup(PARTICIPANTS_COLLECTION)
                .whereEqualTo("deviceId", deviceId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot == null || querySnapshot.isEmpty()) {
                        replaceTickets(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
                        return;
                    }

                    List<Task<LoadedTicket>> ticketTasks = new ArrayList<>();
                    for (QueryDocumentSnapshot waitingListEntry : querySnapshot) {
                        ticketTasks.add(loadTicket(waitingListEntry));
                    }

                    Tasks.whenAllSuccess(ticketTasks)
                            .addOnSuccessListener(results -> {
                                List<TicketUIModel> nextActive = new ArrayList<>();
                                List<AttendingTicketUIModel> nextAttending = new ArrayList<>();
                                List<PastEventUIModel> nextPast = new ArrayList<>();

                                for (Object result : results) {
                                    LoadedTicket loadedTicket = (LoadedTicket) result;
                                    if (loadedTicket == null) {
                                        continue;
                                    }

                                    if (loadedTicket.activeTicket != null) {
                                        nextActive.add(loadedTicket.activeTicket);
                                    }
                                    if (loadedTicket.attendingTicket != null) {
                                        nextAttending.add(loadedTicket.attendingTicket);
                                    }
                                    if (loadedTicket.pastTicket != null) {
                                        nextPast.add(loadedTicket.pastTicket);
                                    }
                                }

                                replaceTickets(nextActive, nextAttending, nextPast);
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to resolve event documents for tickets.", e);
                                replaceTickets(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load waiting-list entries for current user.", e);
                    replaceTickets(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
                });
    }

    @NonNull
    private Task<LoadedTicket> loadTicket(@NonNull QueryDocumentSnapshot participantEntry) {
        String eventId = participantEntry.getString("eventId");
        if ((eventId == null || eventId.trim().isEmpty())
                && participantEntry.getReference().getParent() != null
                && participantEntry.getReference().getParent().getParent() != null) {
            eventId = participantEntry.getReference().getParent().getParent().getId();
        }

        if (eventId == null || eventId.trim().isEmpty()) {
            return Tasks.forResult(null);
        }

        String status = normalizeStatus(participantEntry.getString("status"));
        Timestamp joinedAt = participantEntry.getTimestamp("joinedAt");

        return db.collection(EVENTS_COLLECTION).document(eventId).get().continueWith(task -> {
            if (!task.isSuccessful()) {
                if (task.getException() != null) {
                    Log.e(TAG, "Failed to fetch event document for participant row " + participantEntry.getId(), task.getException());
                }
                return null;
            }

            return mapTicket(status, task.getResult(), joinedAt);
        });
    }

    @Nullable
    private LoadedTicket mapTicket(
            @NonNull String status,
            @Nullable DocumentSnapshot eventDocument,
            @Nullable Timestamp joinedAt
    ) {
        if (eventDocument == null || !eventDocument.exists()) {
            return null;
        }

        String eventId = eventDocument.getId();
        String title = fallback(eventDocument.getString("title"), "Untitled event");
        String dateLabel = buildDateLabel(eventDocument, joinedAt);
        String locationLabel = fallback(eventDocument.getString("location"), "Location details in event page");

        if ("waiting".equals(status)) {
            return LoadedTicket.forActive(new TicketUIModel(
                    eventId,
                    title,
                    dateLabel,
                    TicketUIModel.Status.PENDING
            ));
        }

        if ("backup".equals(status)) {
            return LoadedTicket.forActive(new TicketUIModel(
                    eventId,
                    title,
                    dateLabel,
                    TicketUIModel.Status.BACKUP
            ));
        }

        if ("invited".equals(status)) {
            return LoadedTicket.forActive(new TicketUIModel(
                    eventId,
                    title,
                    dateLabel,
                    TicketUIModel.Status.ACTION_REQUIRED
            ));
        }

        if ("accepted".equals(status)) {
            return LoadedTicket.forAttending(new AttendingTicketUIModel(
                    eventId,
                    title,
                    dateLabel,
                    locationLabel,
                    "Confirmed entry",
                    "Show this ticket during event check-in.",
                    "Open QR pass"
            ));
        }

        if ("declined".equals(status)
                || "cancelled".equals(status)
                || "not_selected".equals(status)) {
            return LoadedTicket.forPast(new PastEventUIModel(
                    title,
                    dateLabel,
                    locationLabel,
                    formatPastStatus(status),
                    formatPastDetail(status),
                    "declined".equals(status) ? R.drawable.ic_clock : R.drawable.ic_info
            ));
        }

        return null;
    }

    @NonNull
    private String buildDateLabel(@NonNull DocumentSnapshot eventDocument, @Nullable Timestamp joinedAt) {
        Timestamp displayTimestamp = eventDocument.getTimestamp("eventStart");
        if (displayTimestamp == null) {
            displayTimestamp = eventDocument.getTimestamp("eventEnd");
        }
        if (displayTimestamp == null) {
            displayTimestamp = eventDocument.getTimestamp("registrationCloseAt");
        }
        if (displayTimestamp == null) {
            displayTimestamp = eventDocument.getTimestamp("registrationOpenAt");
        }
        if (displayTimestamp == null) {
            displayTimestamp = joinedAt;
        }

        if (displayTimestamp == null) {
            return "Date unavailable";
        }

        Date displayDate = displayTimestamp.toDate();
        return new SimpleDateFormat("EEE, MMM d • h:mm a", Locale.US).format(displayDate);
    }

    @NonNull
    private String normalizeStatus(@Nullable String status) {
        if (status == null) {
            return "";
        }
        return status.trim().toLowerCase(Locale.US);
    }

    @NonNull
    private String fallback(@Nullable String value, @NonNull String fallbackValue) {
        if (value == null || value.trim().isEmpty()) {
            return fallbackValue;
        }
        return value.trim();
    }

    @NonNull
    private String formatPastStatus(@NonNull String status) {
        if ("declined".equals(status)) {
            return "Declined";
        }
        if ("cancelled".equals(status)) {
            return "Cancelled";
        }
        if ("not_selected".equals(status)) {
            return "Not selected";
        }
        return "Past";
    }

    @NonNull
    private String formatPastDetail(@NonNull String status) {
        if ("declined".equals(status)) {
            return "You declined the invitation to register.";
        }
        if ("cancelled".equals(status)) {
            return "This registration was cancelled.";
        }
        if ("not_selected".equals(status)) {
            return "The draw completed without selecting your entry.";
        }
        return "This ticket is no longer active.";
    }

    private void updateParticipantStatus(@NonNull String eventId, @NonNull String nextStatus) {
        String deviceId;

        synchronized (this) {
            deviceId = currentDeviceId;
        }

        if (deviceId == null || deviceId.trim().isEmpty()) {
            Log.w(TAG, "Cannot update participant status without a current device id.");
            return;
        }

        db.collection(EVENTS_COLLECTION)
                .document(eventId)
                .collection(PARTICIPANTS_COLLECTION)
                .document(deviceId)
                .update("status", nextStatus)
                .addOnSuccessListener(unused -> loadTicketsForDeviceId(deviceId))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to update participant status to " + nextStatus, e));
    }

    private void replaceTickets(
            @NonNull List<TicketUIModel> nextActive,
            @NonNull List<AttendingTicketUIModel> nextAttending,
            @NonNull List<PastEventUIModel> nextPast
    ) {
        synchronized (this) {
            activeTickets.clear();
            activeTickets.addAll(nextActive);

            attendingTickets.clear();
            attendingTickets.addAll(nextAttending);

            pastTickets.clear();
            pastTickets.addAll(nextPast);
        }

        notifyListeners();
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

    private static final class LoadedTicket {
        private final TicketUIModel activeTicket;
        private final AttendingTicketUIModel attendingTicket;
        private final PastEventUIModel pastTicket;

        private LoadedTicket(
                @Nullable TicketUIModel activeTicket,
                @Nullable AttendingTicketUIModel attendingTicket,
                @Nullable PastEventUIModel pastTicket
        ) {
            this.activeTicket = activeTicket;
            this.attendingTicket = attendingTicket;
            this.pastTicket = pastTicket;
        }

        @NonNull
        private static LoadedTicket forActive(@NonNull TicketUIModel ticket) {
            return new LoadedTicket(ticket, null, null);
        }

        @NonNull
        private static LoadedTicket forAttending(@NonNull AttendingTicketUIModel ticket) {
            return new LoadedTicket(null, ticket, null);
        }

        @NonNull
        private static LoadedTicket forPast(@NonNull PastEventUIModel ticket) {
            return new LoadedTicket(null, null, ticket);
        }
    }
}
