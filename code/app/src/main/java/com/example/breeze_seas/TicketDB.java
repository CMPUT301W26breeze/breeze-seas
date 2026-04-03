package com.example.breeze_seas;

import android.annotation.SuppressLint;
import android.content.Context;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * TicketDB loads ticket-tab data and isolates Firestore access from the UI layer.
 *
 * <p>Current integration scope:
 * - uses the agreed device-based schema from {@code events/{eventId}/participants/{deviceId}}
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
    private static final String EVENTS_COLLECTION = "events";
    private static final String PARTICIPANTS_COLLECTION = "participants";

    private static final TicketDB INSTANCE = new TicketDB();

    private final FirebaseFirestore db = DBConnector.getDb();
    private final List<Listener> listeners = new ArrayList<>();
    private final List<TicketUIModel> activeTickets = new ArrayList<>();
    private final List<AttendingTicketUIModel> attendingTickets = new ArrayList<>();
    private final List<PastEventUIModel> pastTickets = new ArrayList<>();
    private final Map<String, ParticipantEntry> participantEntries = new LinkedHashMap<>();
    private final Map<String, DocumentSnapshot> eventSnapshots = new HashMap<>();
    private final Map<String, ListenerRegistration> eventListenerRegistrations = new HashMap<>();

    @Nullable
    private String currentDeviceId;
    @Nullable
    private ListenerRegistration participantEntriesListener;

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
        boolean shouldStopListening;
        synchronized (this) {
            listeners.remove(listener);
            shouldStopListening = listeners.isEmpty();
        }

        if (shouldStopListening) {
            stopRealtimeListening();
        }
    }

    /**
     * Reloads all ticket lists for the provided device-scoped entrant.
     *
     * <p>If the preferred device id is unavailable, this falls back to the current Android device id.
     *
     * @param context Context used to resolve the Android device identifier fallback.
     * @param preferredDeviceId Preferred participant device id from the signed-in user session.
     */
    public void refreshTickets(@NonNull Context context, @Nullable String preferredDeviceId) {
        String deviceId = resolveCurrentDeviceId(context.getApplicationContext(), preferredDeviceId);
        if (deviceId == null) {
            Log.w(TAG, "No current device id could be resolved for Tickets.");
            stopRealtimeListening();
            replaceTickets(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
            return;
        }

        if (isListeningFor(deviceId)) {
            recomputeTicketsFromRealtimeCache();
            return;
        }

        synchronized (this) {
            currentDeviceId = deviceId;
        }

        startRealtimeListening(deviceId);
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
     * Accepts an active ticket that requires a user decision.
     *
     * <p>Public-event invitations move into the attending state. Private-event invitations
     * move into the waiting-list state because the entrant is only accepting the right to join
     * the waitlist for that event.
     *
     * @param ticket Invited ticket being accepted.
     */
    public void acceptInvitation(@NonNull TicketUIModel ticket) {
        updateParticipantStatus(ticket.getEventId(), ticket.isPrivateEvent() ? "waiting" : "accepted");
    }

    /**
     * Declines an active ticket that requires a user decision.
     *
     * <p>Public-event invitations move into the past-history state. Private-event invitations
     * are simply removed because the entrant is declining the chance to join that private
     * event's waitlist.
     *
     * @param ticket Invited ticket being declined.
     */
    public void declineInvitation(@NonNull TicketUIModel ticket) {
        if (ticket.isPrivateEvent()) {
            deleteParticipant(ticket.getEventId());
            return;
        }
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
    private String resolveCurrentDeviceId(@NonNull Context context, @Nullable String preferredDeviceId) {
        if (preferredDeviceId != null && !preferredDeviceId.trim().isEmpty()) {
            return preferredDeviceId.trim();
        }

        @SuppressLint("HardwareIds") String androidId = Settings.Secure.getString(
                context.getContentResolver(),
                Settings.Secure.ANDROID_ID
        );

        if (androidId == null || androidId.trim().isEmpty()) {
            return null;
        }

        return androidId;
    }

    /**
     * Returns whether this repository is already listening for the provided device id.
     *
     * @param deviceId Device-scoped entrant identifier the ticket feature should track.
     * @return {@code true} when the realtime listeners are already active for this device id.
     */
    private boolean isListeningFor(@NonNull String deviceId) {
        synchronized (this) {
            return participantEntriesListener != null && deviceId.equals(currentDeviceId);
        }
    }

    /**
     * Starts the realtime listener graph for one entrant's participant documents and related events.
     *
     * @param deviceId Device-scoped entrant identifier the ticket feature should track.
     */
    private void startRealtimeListening(@NonNull String deviceId) {
        stopRealtimeListening();
        replaceTickets(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());

        synchronized (this) {
            currentDeviceId = deviceId;
        }

        participantEntriesListener = db.collectionGroup(PARTICIPANTS_COLLECTION)
                .whereEqualTo("deviceId", deviceId)
                .addSnapshotListener(new EventListener<>() {
                    /**
                     * Rebuilds the participant cache whenever the current user's participant rows change.
                     *
                     * @param querySnapshot Current participant rows for the tracked entrant.
                     * @param error Firestore listener error, if one occurred.
                     */
                    @Override
                    public void onEvent(
                            @Nullable QuerySnapshot querySnapshot,
                            @Nullable FirebaseFirestoreException error
                    ) {
                        handleParticipantEntriesSnapshot(querySnapshot, error);
                    }
                });
    }

    /**
     * Stops all realtime ticket listeners and clears the listener registrations they own.
     */
    private void stopRealtimeListening() {
        ListenerRegistration participantListenerSnapshot;
        List<ListenerRegistration> eventListenerSnapshots;

        synchronized (this) {
            participantListenerSnapshot = participantEntriesListener;
            participantEntriesListener = null;

            eventListenerSnapshots = new ArrayList<>(eventListenerRegistrations.values());
            eventListenerRegistrations.clear();
            eventSnapshots.clear();
            participantEntries.clear();
            currentDeviceId = null;
        }

        if (participantListenerSnapshot != null) {
            participantListenerSnapshot.remove();
        }

        for (ListenerRegistration eventListener : eventListenerSnapshots) {
            eventListener.remove();
        }
    }

    /**
     * Updates the tracked participant cache after the current-user participant query changes.
     *
     * @param querySnapshot Current participant rows for the tracked entrant.
     * @param error Firestore listener error, if one occurred.
     */
    private void handleParticipantEntriesSnapshot(
            @Nullable QuerySnapshot querySnapshot,
            @Nullable FirebaseFirestoreException error
    ) {
        if (error != null) {
            Log.e(TAG, "Failed to listen to current user's participant rows.", error);
            return;
        }

        Map<String, ParticipantEntry> nextParticipantEntries = new LinkedHashMap<>();
        if (querySnapshot != null) {
            for (DocumentSnapshot participantDocument : querySnapshot.getDocuments()) {
                String eventId = resolveEventId(participantDocument);
                if (eventId == null || eventId.trim().isEmpty()) {
                    continue;
                }

                ParticipantEntry participantEntry = new ParticipantEntry(
                        eventId,
                        normalizeStatus(participantDocument.getString("status")),
                        participantDocument.getTimestamp("timeJoined")
                );
                nextParticipantEntries.put(eventId, participantEntry);
            }
        }

        synchronized (this) {
            participantEntries.clear();
            participantEntries.putAll(nextParticipantEntries);
        }

        syncEventDocumentListeners(nextParticipantEntries.keySet());
        recomputeTicketsFromRealtimeCache();
    }

    /**
     * Derives an event id from one participant document returned by the collection-group query.
     *
     * @param participantDocument Participant document associated with the current user.
     * @return Parent event id, or {@code null} when the path cannot be resolved.
     */
    @Nullable
    private String resolveEventId(@NonNull DocumentSnapshot participantDocument) {
        if (participantDocument.getReference().getParent().getParent() == null) {
            return null;
        }
        return participantDocument.getReference().getParent().getParent().getId();
    }

    /**
     * Adds and removes event-document listeners so they exactly match the currently tracked tickets.
     *
     * @param activeEventIds Event ids that still have a participant row for the tracked entrant.
     */
    private void syncEventDocumentListeners(@NonNull Set<String> activeEventIds) {
        List<String> eventIdsToRemove = new ArrayList<>();

        synchronized (this) {
            for (String existingEventId : eventListenerRegistrations.keySet()) {
                if (!activeEventIds.contains(existingEventId)) {
                    eventIdsToRemove.add(existingEventId);
                }
            }
        }

        for (String eventId : eventIdsToRemove) {
            stopEventDocumentListener(eventId);
        }

        for (String eventId : activeEventIds) {
            startEventDocumentListener(eventId);
        }
    }

    /**
     * Starts listening to one event document if it is not already being tracked.
     *
     * @param eventId Event id whose document should stay in sync with the ticket cache.
     */
    private void startEventDocumentListener(@NonNull String eventId) {
        synchronized (this) {
            if (eventListenerRegistrations.containsKey(eventId)) {
                return;
            }
        }

        ListenerRegistration eventListener = db.collection(EVENTS_COLLECTION)
                .document(eventId)
                .addSnapshotListener(new EventListener<>() {
                    /**
                     * Updates the cached event metadata for one tracked ticket event.
                     *
                     * @param eventDocument Current event document snapshot.
                     * @param error Firestore listener error, if one occurred.
                     */
                    @Override
                    public void onEvent(
                            @Nullable DocumentSnapshot eventDocument,
                            @Nullable FirebaseFirestoreException error
                    ) {
                        handleEventDocumentSnapshot(eventId, eventDocument, error);
                    }
                });

        synchronized (this) {
            eventListenerRegistrations.put(eventId, eventListener);
        }
    }

    /**
     * Stops listening to one event document and removes any cached snapshot tied to it.
     *
     * @param eventId Event id whose document listener should be removed.
     */
    private void stopEventDocumentListener(@NonNull String eventId) {
        ListenerRegistration eventListener;

        synchronized (this) {
            eventListener = eventListenerRegistrations.remove(eventId);
            eventSnapshots.remove(eventId);
        }

        if (eventListener != null) {
            eventListener.remove();
        }
    }

    /**
     * Updates the cached event snapshot for one tracked ticket event and rebuilds ticket tabs.
     *
     * @param eventId Event id associated with the incoming snapshot.
     * @param eventDocument Current event document snapshot.
     * @param error Firestore listener error, if one occurred.
     */
    private void handleEventDocumentSnapshot(
            @NonNull String eventId,
            @Nullable DocumentSnapshot eventDocument,
            @Nullable FirebaseFirestoreException error
    ) {
        if (error != null) {
            Log.e(TAG, "Failed to listen to event document " + eventId + ".", error);
            return;
        }

        synchronized (this) {
            if (eventDocument == null || !eventDocument.exists()) {
                eventSnapshots.remove(eventId);
            } else {
                eventSnapshots.put(eventId, eventDocument);
            }
        }

        recomputeTicketsFromRealtimeCache();
    }

    /**
     * Rebuilds the three ticket-tab lists from the current realtime participant and event caches.
     */
    private void recomputeTicketsFromRealtimeCache() {
        Map<String, ParticipantEntry> participantSnapshot;
        Map<String, DocumentSnapshot> eventSnapshot;

        synchronized (this) {
            participantSnapshot = new LinkedHashMap<>(participantEntries);
            eventSnapshot = new HashMap<>(eventSnapshots);
        }

        List<TicketUIModel> nextActive = new ArrayList<>();
        List<AttendingTicketUIModel> nextAttending = new ArrayList<>();
        List<PastEventUIModel> nextPast = new ArrayList<>();

        for (ParticipantEntry participantEntry : participantSnapshot.values()) {
            DocumentSnapshot eventDocument = eventSnapshot.get(participantEntry.eventId);
            LoadedTicket loadedTicket = mapTicket(
                    participantEntry.status,
                    eventDocument,
                    participantEntry.timeJoined
            );

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
     * Maps one participant row plus its event metadata into the correct ticket-tab payload.
     *
     * @param status Normalized participant status from Firestore.
     * @param eventDocument Event document used to build display fields.
     * @param timeJoined Participant join timestamp used as a display fallback.
     * @return Ticket payload for one of the three tabs, or {@code null} if the row should be ignored.
     */
    @Nullable
    private LoadedTicket mapTicket(
            @NonNull String status,
            @Nullable DocumentSnapshot eventDocument,
            @Nullable Timestamp timeJoined
    ) {
        if (eventDocument == null || !eventDocument.exists()) {
            return null;
        }

        String eventId = eventDocument.getId();
        String title = buildTitleLabel(eventDocument);
        String dateLabel = buildDateLabel(eventDocument, timeJoined);
        String locationLabel = buildLocationLabel(eventDocument);
        boolean privateEvent = Boolean.TRUE.equals(eventDocument.getBoolean("isPrivate"));
        boolean eventEnded = hasEventEnded(eventDocument);

        if (eventEnded && !isArchivedStatus(status)) {
            return LoadedTicket.forPast(new PastEventUIModel(
                    title,
                    dateLabel,
                    locationLabel,
                    "Past",
                    "This event has already happened.",
                    R.drawable.ic_clock
            ));
        }

        switch (status) {
            case "waiting":
                return LoadedTicket.forActive(new TicketUIModel(
                        eventId,
                        title,
                        dateLabel,
                        TicketUIModel.Status.PENDING,
                        privateEvent
                ));
            case "backup":
                return LoadedTicket.forActive(new TicketUIModel(
                        eventId,
                        title,
                        dateLabel,
                        TicketUIModel.Status.BACKUP,
                        privateEvent
                ));
            case "invited":
            case "pending":
                return LoadedTicket.forActive(new TicketUIModel(
                        eventId,
                        title,
                        dateLabel,
                        TicketUIModel.Status.ACTION_REQUIRED,
                        privateEvent
                ));
            case "accepted":
                return LoadedTicket.forAttending(new AttendingTicketUIModel(
                        eventId,
                        title,
                        dateLabel,
                        locationLabel,
                        "Confirmed entry",
                        "Show this ticket during event check-in.",
                        "Open QR pass"
                ));
            case "declined":
            case "cancelled":
            case "not_selected":
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
     * @param timeJoined Participant join timestamp used as the last fallback.
     * @return Human-readable date label for the ticket card.
     */
    @NonNull
    private String buildDateLabel(@NonNull DocumentSnapshot eventDocument, @Nullable Timestamp timeJoined) {
        Timestamp displayTimestamp = eventDocument.getTimestamp("eventStartTimestamp");
        if (displayTimestamp == null) {
            displayTimestamp = eventDocument.getTimestamp("eventEndTimestamp");
        }
        if (displayTimestamp == null) {
            displayTimestamp = eventDocument.getTimestamp("registrationEndTimestamp");
        }
        if (displayTimestamp == null) {
            displayTimestamp = eventDocument.getTimestamp("registrationStartTimestamp");
        }
        if (displayTimestamp == null) {
            displayTimestamp = timeJoined;
        }

        return displayTimestamp == null
                ? "Date unavailable"
                : EventMetadataUtils.formatDateTime(displayTimestamp);
    }

    /**
     * Returns whether a tracked event has already ended and should render in the Past tab.
     *
     * @param eventDocument Event metadata document tied to the current ticket.
     * @return {@code true} when the event end or start timestamp is already behind the current time.
     */
    private boolean hasEventEnded(@NonNull DocumentSnapshot eventDocument) {
        Timestamp eventEnd = eventDocument.getTimestamp("eventEndTimestamp");
        if (eventEnd == null) {
            eventEnd = eventDocument.getTimestamp("eventStartTimestamp");
        }
        return eventEnd != null && eventEnd.toDate().before(new Date());
    }

    /**
     * Returns whether a participant status already belongs to the archived Past tab.
     *
     * @param status Normalized participant status.
     * @return {@code true} when the status is already archived.
     */
    private boolean isArchivedStatus(@NonNull String status) {
        return "declined".equals(status)
                || "cancelled".equals(status)
                || "not_selected".equals(status);
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
        return "";
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
        switch (status) {
            case "declined":
                return "Declined";
            case "cancelled":
                return "Cancelled";
            case "not_selected":
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
        switch (status) {
            case "declined":
                return "You declined the invitation to register.";
            case "cancelled":
                return "This registration was cancelled.";
            case "not_selected":
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
                .addOnSuccessListener(new OnSuccessListener<>() {
                    /**
                     * Applies the successful status mutation to the local cache immediately.
                     *
                     * @param unused Unused success payload from the Firestore update task.
                     */
                    @Override
                    public void onSuccess(Void unused) {
                        applyLocalParticipantStatusUpdate(eventId, nextStatus);
                    }
                })
                .addOnFailureListener(new com.google.android.gms.tasks.OnFailureListener() {
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
                .addOnSuccessListener(new OnSuccessListener<>() {
                    /**
                     * Removes the successful deletion from the local cache immediately.
                     *
                     * @param unused Unused success payload from the Firestore delete task.
                     */
                    @Override
                    public void onSuccess(Void unused) {
                        applyLocalParticipantRemoval(eventId);
                    }
                })
                .addOnFailureListener(new com.google.android.gms.tasks.OnFailureListener() {
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
     * Applies a successful participant-status update to the in-memory realtime cache.
     *
     * @param eventId Event whose participant row changed status.
     * @param nextStatus Next normalized participant status.
     */
    private void applyLocalParticipantStatusUpdate(@NonNull String eventId, @NonNull String nextStatus) {
        synchronized (this) {
            ParticipantEntry currentEntry = participantEntries.get(eventId);
            if (currentEntry == null) {
                return;
            }

            participantEntries.put(
                    eventId,
                    new ParticipantEntry(
                            currentEntry.eventId,
                            normalizeStatus(nextStatus),
                            currentEntry.timeJoined
                    )
            );
        }

        recomputeTicketsFromRealtimeCache();
    }

    /**
     * Applies a successful participant-row deletion to the in-memory realtime cache.
     *
     * @param eventId Event whose participant row was removed for the current user.
     */
    private void applyLocalParticipantRemoval(@NonNull String eventId) {
        synchronized (this) {
            participantEntries.remove(eventId);
        }

        stopEventDocumentListener(eventId);
        recomputeTicketsFromRealtimeCache();
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

    /**
     * Cached participant-row state used while realtime listeners are active.
     */
    private static final class ParticipantEntry {
        private final String eventId;
        private final String status;
        @Nullable
        private final Timestamp timeJoined;

        /**
         * Stores the participant fields needed to build one ticket card.
         *
         * @param eventId Event id containing the participant document.
         * @param status Normalized participant status.
         * @param timeJoined Participant join timestamp used as a display fallback.
         */
        private ParticipantEntry(
                @NonNull String eventId,
                @NonNull String status,
                @Nullable Timestamp timeJoined
        ) {
            this.eventId = eventId;
            this.status = status;
            this.timeJoined = timeJoined;
        }
    }
}
