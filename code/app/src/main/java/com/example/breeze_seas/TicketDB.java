package com.example.breeze_seas;

import android.content.Context;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * TicketDB loads ticket-tab data and isolates Firestore access from the UI layer.
 *
 * <p>Current integration scope:
 * - uses the agreed device-based schema from {@code test_events/{eventId}/participants/{deviceId}}
 * - supports live Firestore loading for the agreed participant statuses
 *
 * <p>Outstanding:
 * - replace fallback display labels once the final event metadata contract is fully implemented
 */
public final class TicketDB {

    /**
     * Listener notified when any of the ticket-tab lists are refreshed.
     */
    public interface Listener {
        /**
         * Called after {@link TicketDB} publishes a new snapshot of ticket data.
         */
        void onTicketsChanged();
    }

    private static final String TAG = "TicketDB";
    private static final String EVENTS_COLLECTION = "test_events";
    private static final String PARTICIPANTS_COLLECTION = "participants";

    private static final TicketDB INSTANCE = new TicketDB();

    private final FirebaseFirestore db = DBConnector.getDb();
    private final List<Listener> listeners = new ArrayList<>();
    private final List<TicketUIModel> activeTickets = new ArrayList<>();
    private final List<AttendingTicketUIModel> attendingTickets = new ArrayList<>();
    private final List<PastEventUIModel> pastTickets = new ArrayList<>();

    @Nullable
    private String currentDeviceId;

    /**
     * Prevents external instantiation of the shared ticket data source.
     */
    private TicketDB() {
    }

    /**
     * Returns the shared ticket data source used by the Tickets feature.
     *
     * @return Singleton {@link TicketDB} instance.
     */
    @NonNull
    public static TicketDB getInstance() {
        return INSTANCE;
    }

    /**
     * Registers a listener for ticket-list refresh events.
     *
     * @param listener Listener to add if it is not already registered.
     */
    public void addListener(@NonNull Listener listener) {
        synchronized (this) {
            if (!listeners.contains(listener)) {
                listeners.add(listener);
            }
        }
    }

    /**
     * Unregisters a listener that no longer needs ticket updates.
     *
     * @param listener Listener to remove.
     */
    public void removeListener(@NonNull Listener listener) {
        synchronized (this) {
            listeners.remove(listener);
        }
    }

    /**
     * Reloads all ticket lists for the current device-scoped entrant.
     *
     * @param context Context used to resolve the Android device identifier.
     */
    public void refreshTickets(@NonNull Context context) {
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

    /**
     * Returns a snapshot of the current Active tab ticket list.
     *
     * @return Copy of the active-ticket list.
     */
    @NonNull
    public List<TicketUIModel> getActiveTickets() {
        synchronized (this) {
            return new ArrayList<>(activeTickets);
        }
    }

    /**
     * Returns a snapshot of the current Attending tab ticket list.
     *
     * @return Copy of the attending-ticket list.
     */
    @NonNull
    public List<AttendingTicketUIModel> getAttendingTickets() {
        synchronized (this) {
            return new ArrayList<>(attendingTickets);
        }
    }

    /**
     * Returns a snapshot of the current Past tab ticket list.
     *
     * @return Copy of the past-ticket list.
     */
    @NonNull
    public List<PastEventUIModel> getPastTickets() {
        synchronized (this) {
            return new ArrayList<>(pastTickets);
        }
    }

    /**
     * Accepts an invited ticket and moves it into the attending state.
     *
     * @param ticket Invited ticket being accepted.
     */
    public void acceptInvitation(@NonNull TicketUIModel ticket) {
        updateParticipantStatus(ticket.getEventId(), "accepted");
    }

    /**
     * Declines an invited ticket and moves it into the past-history state.
     *
     * @param ticket Invited ticket being declined.
     */
    public void declineInvitation(@NonNull TicketUIModel ticket) {
        updateParticipantStatus(ticket.getEventId(), "declined");
    }

    /**
     * Removes the current entrant from an event's participant list.
     *
     * @param ticket Active ticket whose participant row should be deleted.
     */
    public void leaveWaitlist(@NonNull TicketUIModel ticket) {
        deleteParticipant(ticket.getEventId());
    }

    /**
     * Resolves the current device identifier used by the agreed participant schema.
     *
     * @param context Context used to access {@link Settings.Secure}.
     * @return Device identifier for the current install, or {@code null} if unavailable.
     */
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

    /**
     * Queries all participant rows associated with the provided device id.
     *
     * @param deviceId Device-scoped entrant identifier used by the participant schema.
     */
    private void loadTicketsForDeviceId(@NonNull String deviceId) {
        db.collectionGroup(PARTICIPANTS_COLLECTION)
                .whereEqualTo("deviceId", deviceId)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    /**
                     * Starts resolving event details for the matching participant rows.
                     *
                     * @param querySnapshot Matching participant rows for the current device id.
                     */
                    @Override
                    public void onSuccess(QuerySnapshot querySnapshot) {
                        handleParticipantsLoaded(querySnapshot);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    /**
                     * Clears the ticket lists if the participant query fails.
                     *
                     * @param e Query failure that prevented participant loading.
                     */
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        handleParticipantLoadFailure(e);
                    }
                });
    }

    /**
     * Builds ticket-loading tasks for each participant row returned by Firestore.
     *
     * @param querySnapshot Matching participant rows for the current device id.
     */
    private void handleParticipantsLoaded(@Nullable QuerySnapshot querySnapshot) {
        if (querySnapshot == null || querySnapshot.isEmpty()) {
            replaceTickets(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
            return;
        }

        List<Task<LoadedTicket>> ticketTasks = new ArrayList<>();
        for (QueryDocumentSnapshot participantEntry : querySnapshot) {
            ticketTasks.add(loadTicket(participantEntry));
        }

        Tasks.whenAllSuccess(ticketTasks)
                .addOnSuccessListener(new OnSuccessListener<List<Object>>() {
                    /**
                     * Splits the resolved ticket results into Active, Attending, and Past lists.
                     *
                     * @param results Resolved ticket payloads returned from the event lookups.
                     */
                    @Override
                    public void onSuccess(List<Object> results) {
                        handleLoadedTicketResults(results);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    /**
                     * Clears the ticket lists if the event lookups fail.
                     *
                     * @param e Failure that prevented event resolution for one or more tickets.
                     */
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        handleLoadedTicketFailure(e);
                    }
                });
    }

    /**
     * Clears the current lists when the initial participant query fails.
     *
     * @param e Failure that prevented participant loading.
     */
    private void handleParticipantLoadFailure(@NonNull Exception e) {
        Log.e(TAG, "Failed to load participant entries for current user.", e);
        replaceTickets(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
    }

    /**
     * Converts resolved ticket payloads into the three UI lists maintained by this data source.
     *
     * @param results Resolved ticket payloads returned from the event lookups.
     */
    private void handleLoadedTicketResults(@NonNull List<Object> results) {
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
    }

    /**
     * Clears the current lists if the event-resolution stage fails.
     *
     * @param e Failure that prevented event resolution for one or more tickets.
     */
    private void handleLoadedTicketFailure(@NonNull Exception e) {
        Log.e(TAG, "Failed to resolve event documents for tickets.", e);
        replaceTickets(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
    }

    /**
     * Loads the related event document and maps one participant row into a ticket UI model.
     *
     * @param participantEntry Participant row returned from the agreed schema.
     * @return Task that resolves to a mapped ticket payload or {@code null} if it cannot be built.
     */
    @NonNull
    private Task<LoadedTicket> loadTicket(@NonNull QueryDocumentSnapshot participantEntry) {
        String eventId = participantEntry.getString("eventId");
        DocumentReference eventReference = null;
        if (participantEntry.getReference().getParent() != null
                && participantEntry.getReference().getParent().getParent() != null) {
            eventReference = participantEntry.getReference().getParent().getParent();
        }

        if ((eventId == null || eventId.trim().isEmpty()) && eventReference != null) {
            eventId = eventReference.getId();
        }

        if (eventId == null || eventId.trim().isEmpty()) {
            return Tasks.forResult(null);
        }

        String status = normalizeStatus(participantEntry.getString("status"));
        Timestamp joinedAt = participantEntry.getTimestamp("joinedAt");

        DocumentReference resolvedEventReference = eventReference != null
                ? eventReference
                : db.collection(EVENTS_COLLECTION).document(eventId);

        return resolvedEventReference
                .get()
                .continueWith(new Continuation<DocumentSnapshot, LoadedTicket>() {
                    /**
                     * Maps the fetched event document into the corresponding ticket UI payload.
                     *
                     * @param task Event-document lookup task for the participant row.
                     * @return Resolved ticket payload, or {@code null} if mapping is not possible.
                     */
                    @Override
                    public LoadedTicket then(@NonNull Task<DocumentSnapshot> task) {
                        return continueLoadingTicket(task, participantEntry, status, joinedAt);
                    }
                });
    }

    /**
     * Continues ticket mapping after the related event document finishes loading.
     *
     * @param task Event-document lookup task for the participant row.
     * @param participantEntry Participant row that requested the event lookup.
     * @param status Normalized participant status.
     * @param joinedAt Participant join timestamp used as a display fallback.
     * @return Resolved ticket payload, or {@code null} if mapping is not possible.
     */
    @Nullable
    private LoadedTicket continueLoadingTicket(
            @NonNull Task<DocumentSnapshot> task,
            @NonNull QueryDocumentSnapshot participantEntry,
            @NonNull String status,
            @Nullable Timestamp joinedAt
    ) {
        if (!task.isSuccessful()) {
            if (task.getException() != null) {
                Log.e(TAG, "Failed to fetch event document for participant row " + participantEntry.getId(), task.getException());
            }
            return null;
        }

        return mapTicket(status, task.getResult(), joinedAt);
    }

    /**
     * Maps one participant row plus its event metadata into the correct ticket-tab payload.
     *
     * @param status Normalized participant status from Firestore.
     * @param eventDocument Event document used to build display fields.
     * @param joinedAt Participant join timestamp used as a display fallback.
     * @return Ticket payload for one of the three tabs, or {@code null} if the row should be ignored.
     */
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
        String title = buildTitleLabel(eventDocument);
        String dateLabel = buildDateLabel(eventDocument, joinedAt);
        String locationLabel = buildLocationLabel(eventDocument);

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

    /**
     * Builds the date label shown on ticket cards using the strongest available event timestamp.
     *
     * @param eventDocument Event document used to source display timestamps.
     * @param joinedAt Participant join timestamp used as the last fallback.
     * @return Human-readable date label for the ticket card.
     */
    @NonNull
    private String buildDateLabel(@NonNull DocumentSnapshot eventDocument, @Nullable Timestamp joinedAt) {
        Timestamp displayTimestamp = eventDocument.getTimestamp("eventStartDate");
        if (displayTimestamp == null) {
            displayTimestamp = eventDocument.getTimestamp("eventStart");
        }
        if (displayTimestamp == null) {
            displayTimestamp = eventDocument.getTimestamp("eventEndDate");
        }
        if (displayTimestamp == null) {
            displayTimestamp = eventDocument.getTimestamp("eventEnd");
        }
        if (displayTimestamp == null) {
            displayTimestamp = eventDocument.getTimestamp("registrationEndDate");
        }
        if (displayTimestamp == null) {
            displayTimestamp = eventDocument.getTimestamp("registrationCloseAt");
        }
        if (displayTimestamp == null) {
            displayTimestamp = eventDocument.getTimestamp("registrationStartDate");
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

    /**
     * Builds the display title for an event document using the new EventDB field names first.
     *
     * @param eventDocument Event document used to source the display title.
     * @return Best available event title for ticket cards.
     */
    @NonNull
    private String buildTitleLabel(@NonNull DocumentSnapshot eventDocument) {
        String currentTitle = fallback(eventDocument.getString("name"), "");
        if (!currentTitle.isEmpty()) {
            return currentTitle;
        }

        return fallback(eventDocument.getString("title"), "Untitled event");
    }

    /**
     * Builds the secondary location/detail label for an event document.
     *
     * @param eventDocument Event document used to source the display label.
     * @return Best available secondary detail label for ticket cards.
     */
    @NonNull
    private String buildLocationLabel(@NonNull DocumentSnapshot eventDocument) {
        String explicitLocation = fallback(eventDocument.getString("location"), "");
        if (!explicitLocation.isEmpty()) {
            return explicitLocation;
        }

        if (Boolean.TRUE.equals(eventDocument.getBoolean("geolocationEnforced"))) {
            return "Geolocation required";
        }

        return "Event details available in app";
    }

    /**
     * Normalizes Firestore participant status values for predictable comparisons.
     *
     * @param status Raw participant status from Firestore.
     * @return Lower-cased trimmed status string, or an empty string if input is {@code null}.
     */
    @NonNull
    private String normalizeStatus(@Nullable String status) {
        if (status == null) {
            return "";
        }
        return status.trim().toLowerCase(Locale.US);
    }

    /**
     * Returns a fallback display value when Firestore data is missing or blank.
     *
     * @param value Candidate value from Firestore.
     * @param fallbackValue Default display value to use if the candidate is empty.
     * @return Trimmed Firestore value, or the provided fallback.
     */
    @NonNull
    private String fallback(@Nullable String value, @NonNull String fallbackValue) {
        if (value == null || value.trim().isEmpty()) {
            return fallbackValue;
        }
        return value.trim();
    }

    /**
     * Converts archived participant statuses into short chip labels for the Past tab.
     *
     * @param status Normalized participant status.
     * @return Short status label shown in the history chip.
     */
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

    /**
     * Converts archived participant statuses into supporting copy for the Past tab.
     *
     * @param status Normalized participant status.
     * @return Supporting detail text for the archived ticket card.
     */
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

    /**
     * Updates the participant status for the current device and refreshes ticket lists afterward.
     *
     * @param eventId Event whose participant row should be updated.
     * @param nextStatus Next participant status to write to Firestore.
     */
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
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    /**
                     * Reloads the ticket lists after a successful participant-status update.
                     *
                     * @param unused Unused success payload from the Firestore update task.
                     */
                    @Override
                    public void onSuccess(Void unused) {
                        loadTicketsForDeviceId(deviceId);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    /**
                     * Logs a participant-status update failure without changing the cached lists.
                     *
                     * @param e Firestore failure returned by the update task.
                     */
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Failed to update participant status to " + nextStatus, e);
                    }
                });
    }

    /**
     * Deletes the participant row for the current device from the provided event.
     *
     * @param eventId Event whose participant row should be removed.
     */
    private void deleteParticipant(@NonNull String eventId) {
        String deviceId;

        synchronized (this) {
            deviceId = currentDeviceId;
        }

        if (deviceId == null || deviceId.trim().isEmpty()) {
            Log.w(TAG, "Cannot delete participant row without a current device id.");
            return;
        }

        db.collection(EVENTS_COLLECTION)
                .document(eventId)
                .collection(PARTICIPANTS_COLLECTION)
                .document(deviceId)
                .delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    /**
                     * Reloads the ticket lists after a successful waitlist-leave action.
                     *
                     * @param unused Unused success payload from the Firestore delete task.
                     */
                    @Override
                    public void onSuccess(Void unused) {
                        loadTicketsForDeviceId(deviceId);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    /**
                     * Logs a waitlist-leave failure without changing the cached lists.
                     *
                     * @param e Firestore failure returned by the delete task.
                     */
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Failed to remove participant row for event " + eventId, e);
                    }
                });
    }

    /**
     * Replaces the cached tab lists and broadcasts the refresh to listeners.
     *
     * @param nextActive New Active tab list.
     * @param nextAttending New Attending tab list.
     * @param nextPast New Past tab list.
     */
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

    /**
     * Notifies all registered listeners that the cached ticket lists have changed.
     */
    private void notifyListeners() {
        List<Listener> snapshot;

        synchronized (this) {
            snapshot = new ArrayList<>(listeners);
        }

        for (Listener listener : snapshot) {
            listener.onTicketsChanged();
        }
    }

    /**
     * Holder for a single mapped ticket payload targeted at exactly one ticket tab.
     */
    private static final class LoadedTicket {
        private final TicketUIModel activeTicket;
        private final AttendingTicketUIModel attendingTicket;
        private final PastEventUIModel pastTicket;

        /**
         * Creates a tab-specific ticket payload wrapper.
         *
         * @param activeTicket Active-tab payload, or {@code null} when not applicable.
         * @param attendingTicket Attending-tab payload, or {@code null} when not applicable.
         * @param pastTicket Past-tab payload, or {@code null} when not applicable.
         */
        private LoadedTicket(
                @Nullable TicketUIModel activeTicket,
                @Nullable AttendingTicketUIModel attendingTicket,
                @Nullable PastEventUIModel pastTicket
        ) {
            this.activeTicket = activeTicket;
            this.attendingTicket = attendingTicket;
            this.pastTicket = pastTicket;
        }

        /**
         * Creates a wrapper containing an Active-tab payload.
         *
         * @param ticket Active-tab ticket payload.
         * @return Wrapper containing only the active-ticket payload.
         */
        @NonNull
        private static LoadedTicket forActive(@NonNull TicketUIModel ticket) {
            return new LoadedTicket(ticket, null, null);
        }

        /**
         * Creates a wrapper containing an Attending-tab payload.
         *
         * @param ticket Attending-tab ticket payload.
         * @return Wrapper containing only the attending-ticket payload.
         */
        @NonNull
        private static LoadedTicket forAttending(@NonNull AttendingTicketUIModel ticket) {
            return new LoadedTicket(null, ticket, null);
        }

        /**
         * Creates a wrapper containing a Past-tab payload.
         *
         * @param ticket Past-tab ticket payload.
         * @return Wrapper containing only the past-ticket payload.
         */
        @NonNull
        private static LoadedTicket forPast(@NonNull PastEventUIModel ticket) {
            return new LoadedTicket(null, null, ticket);
        }
    }
}
