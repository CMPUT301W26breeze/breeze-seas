package com.example.breeze_seas;

import android.util.Log;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

/**
 * Event stores the organizer-facing and entrant-facing metadata for a single event document.
 */
public class Event {
    private String eventId;
    private String organizerId;
    private String name;
    private String description;
    private String image;
    private String qrValue;
    private Timestamp dateCreated;
    private Timestamp dateModified;
    private Timestamp registrationStartDate;
    private Timestamp registrationEndDate;
    private Timestamp eventStartDate;
    private Timestamp eventEndDate;
    private boolean geolocationEnforced;
    private int eventCapacity;
    private int waitingListCapacity;
    private int drawARound;
    private WaitingList waitingList;
    private PendingList pendingList;
    private AcceptedList acceptedList;
    private DeclinedList declinedList;

    /**
     * Creates an event using the full field set expected when hydrating from {@link EventDB}.
     *
     * @param eventId Unique event identifier.
     * @param organizerId Organizer identifier that owns the event.
     * @param name Display name of the event.
     * @param description Organizer-provided description.
     * @param image Poster or image URI string.
     * @param qrValue QR payload associated with the event.
     * @param dateCreated Timestamp when the event was created.
     * @param dateModified Timestamp when the event was last modified.
     * @param registrationStartDate Registration opening timestamp.
     * @param registrationEndDate Registration closing timestamp.
     * @param eventStartDate Event start timestamp.
     * @param eventEndDate Event end timestamp.
     * @param geolocationEnforced Whether registration requires geolocation.
     * @param eventCapacity Maximum accepted entrants.
     * @param waitingListCapacity Maximum waiting-list entrants.
     * @param drawARound Current draw round counter.
     * @param waitingList Waiting-list helper associated with the event.
     * @param pendingList Pending-list helper associated with the event.
     * @param acceptedList Accepted-list helper associated with the event.
     * @param declinedList Declined-list helper associated with the event.
     */
    public Event(String eventId,
                 String organizerId,
                 String name,
                 String description,
                 String image,
                 String qrValue,
                 Timestamp dateCreated,
                 Timestamp dateModified,
                 Timestamp registrationStartDate,
                 Timestamp registrationEndDate,
                 Timestamp eventStartDate,
                 Timestamp eventEndDate,
                 boolean geolocationEnforced,
                 int eventCapacity,
                 int waitingListCapacity,
                 int drawARound,
                 WaitingList waitingList,
                 PendingList pendingList,
                 AcceptedList acceptedList,
                 DeclinedList declinedList) {
        this.eventId = eventId;
        this.organizerId = organizerId;
        this.name = name;
        this.description = description;
        this.image = image;
        this.qrValue = qrValue;
        this.dateModified = dateModified;
        this.eventCapacity = eventCapacity;
        this.waitingListCapacity = waitingListCapacity;
        this.drawARound = drawARound;
        this.dateCreated = dateCreated;
        this.registrationStartDate = registrationStartDate;
        this.registrationEndDate = registrationEndDate;
        this.eventStartDate = eventStartDate;
        this.eventEndDate = eventEndDate;
        this.geolocationEnforced = geolocationEnforced;
        this.waitingList = waitingList;
        this.pendingList = pendingList;
        this.acceptedList = acceptedList;
        this.declinedList = declinedList;
    }

    /**
     * Creates a new event for organizer-side event creation before it is persisted.
     *
     * @param organizerId Organizer identifier that owns the new event.
     * @param name Display name of the event.
     * @param description Organizer-provided description.
     * @param image Poster or image URI string.
     * @param qrValue QR payload associated with the event.
     * @param registrationStartDate Registration opening timestamp.
     * @param registrationEndDate Registration closing timestamp.
     * @param eventStartDate Event start timestamp.
     * @param eventEndDate Event end timestamp.
     * @param geolocationEnforced Whether registration requires geolocation.
     * @param eventCapacity Maximum accepted entrants.
     * @param waitingListCapacity Maximum waiting-list entrants.
     * @param drawARound Current draw round counter.
     */
    public Event(String organizerId,
                 String name,
                 String description,
                 String image,
                 String qrValue,
                 Timestamp registrationStartDate,
                 Timestamp registrationEndDate,
                 Timestamp eventStartDate,
                 Timestamp eventEndDate,
                 boolean geolocationEnforced,
                 int eventCapacity,
                 int waitingListCapacity,
                 int drawARound) {
        this.organizerId = organizerId;
        this.eventId = EventDB.genNewEventId();
        this.name = name;
        this.description = description;
        this.image = image;
        this.qrValue = qrValue;
        this.dateModified = Timestamp.now();  // Use timestamp at creation
        this.eventCapacity = eventCapacity;
        this.waitingListCapacity = waitingListCapacity;
        this.drawARound = drawARound;
        this.dateCreated = Timestamp.now();  // Use timestamp at creation
        this.registrationStartDate = registrationStartDate;
        this.registrationEndDate = registrationEndDate;
        this.eventStartDate = eventStartDate;
        this.eventEndDate = eventEndDate;
        this.geolocationEnforced = geolocationEnforced;
        this.waitingList = new WaitingList(this, waitingListCapacity);
        this.pendingList = new PendingList(this, -1);
        this.acceptedList = new AcceptedList(this, eventCapacity);
        this.declinedList = new DeclinedList(this, -1);
    }

    /**
     * Creates a minimal event instance for temporary or test organizer flows.
     *
     * @param organizerId Organizer identifier that owns the event.
     * @param eventCapacity Maximum accepted entrants.
     */
    public Event(String organizerId,
                 int eventCapacity) {
        this.organizerId = organizerId;
        this.eventId = EventDB.genNewEventId();
        this.name = null;
        this.description = null;
        this.image = null;
        this.qrValue = null;
        this.dateModified = Timestamp.now();  // Use timestamp at creation
        this.eventCapacity = eventCapacity;
        this.waitingListCapacity = -1;
        this.drawARound = 0;
        this.dateCreated = Timestamp.now();  // Use timestamp at creation
        this.registrationStartDate = null;
        this.registrationEndDate = null;
        this.eventStartDate = null;
        this.eventEndDate = null;
        this.geolocationEnforced = false;
        this.waitingList = new WaitingList(this, -1);
        this.pendingList = new PendingList(this, -1);
        this.acceptedList = new AcceptedList(this, eventCapacity);
        this.declinedList = new DeclinedList(this, -1);
    }

    /**
     * Serializes the current event into the Firestore map shape expected by {@link EventDB}.
     *
     * @return Firestore field map for this event.
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("eventId", getEventId());
        map.put("organizerId", getOrganizerId());
        map.put("name", getName());
        map.put("description", getDescription());
        map.put("image", getImage());
        map.put("qrValue", getQrValue());
        map.put("dateCreated", getDateCreated());
        map.put("dateModified", getDateModified());
        map.put("registrationStartDate", getRegistrationStartDate());
        map.put("registrationEndDate", getRegistrationEndDate());
        map.put("eventStartDate", getEventStartDate());
        map.put("eventEndDate", getEventEndDate());
        map.put("geolocationEnforced", isGeolocationEnforced());
        map.put("eventCapacity", getEventCapacity());
        map.put("waitingListCapacity", getWaitingListCapacity());
        map.put("drawARound", getDrawARound());
        return map;
    }

    /**
     * Returns the unique identifier for this event.
     *
     * @return Event identifier.
     */
    public String getEventId() {
        return eventId;
    }

    /**
     * Updates the unique identifier for this event.
     *
     * @param eventId Event identifier to store.
     */
    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    /**
     * Returns the organizer identifier that owns this event.
     *
     * @return Organizer identifier.
     */
    public String getOrganizerId() {
        return organizerId;
    }

    /**
     * Updates the organizer identifier associated with this event.
     *
     * @param organizerId Organizer identifier to store.
     */
    public void setOrganizerId(String organizerId) {
        this.organizerId = organizerId;
    }

    /**
     * Returns whether registration for this event enforces geolocation.
     *
     * @return {@code true} if geolocation checks are required.
     */
    public boolean isGeolocationEnforced() {
        return geolocationEnforced;
    }

    /**
     * Returns the display name of the event.
     *
     * @return Event name.
     */
    public String getName() {
        return name;
    }

    /**
     * Updates the display name of the event.
     *
     * @param name Event name to store.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the organizer-provided description for this event.
     *
     * @return Event description.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Updates the description for this event.
     *
     * @param description Event description to store.
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Returns the poster or image URI associated with the event.
     *
     * @return Poster or image URI string.
     */
    public String getImage() {
        return image;
    }

    /**
     * Updates the poster or image URI associated with the event.
     *
     * @param image Poster or image URI string to store.
     */
    public void setImage(String image) {
        this.image = image;
    }

    /**
     * Returns the timestamp when the event was created.
     *
     * @return Event creation timestamp.
     */
    public Timestamp getDateCreated() {
        return dateCreated;
    }

    /**
     * Updates the event creation timestamp.
     *
     * @param dateCreated Creation timestamp to store.
     */
    public void setDateCreated(Timestamp dateCreated) {
        this.dateCreated = dateCreated;
    }

    /**
     * Returns the timestamp when registration opens.
     *
     * @return Registration opening timestamp.
     */
    public Timestamp getRegistrationStartDate() {
        return registrationStartDate;
    }

    /**
     * Updates the registration opening timestamp.
     *
     * @param registrationStartDate Registration opening timestamp to store.
     */
    public void setRegistrationStartDate(Timestamp registrationStartDate) {
        this.registrationStartDate = registrationStartDate;
    }

    /**
     * Returns the timestamp when registration closes.
     *
     * @return Registration closing timestamp.
     */
    public Timestamp getRegistrationEndDate() {
        return registrationEndDate;
    }

    /**
     * Updates the registration closing timestamp.
     *
     * @param registrationEndDate Registration closing timestamp to store.
     */
    public void setRegistrationEndDate(Timestamp registrationEndDate) {
        this.registrationEndDate = registrationEndDate;
    }

    /**
     * Returns the event start timestamp.
     *
     * @return Event start timestamp.
     */
    public Timestamp getEventStartDate() {
        return eventStartDate;
    }

    /**
     * Updates the event start timestamp.
     *
     * @param eventStartDate Event start timestamp to store.
     */
    public void setEventStartDate(Timestamp eventStartDate) {
        this.eventStartDate = eventStartDate;
    }

    /**
     * Returns the event end timestamp.
     *
     * @return Event end timestamp.
     */
    public Timestamp getEventEndDate() {
        return eventEndDate;
    }

    /**
     * Updates the event end timestamp.
     *
     * @param eventEndDate Event end timestamp to store.
     */
    public void setEventEndDate(Timestamp eventEndDate) {
        this.eventEndDate = eventEndDate;
    }

    /**
     * Returns the current draw-round counter for this event.
     *
     * @return Draw-round counter.
     */
    public int getDrawARound() {
        return drawARound;
    }

    /**
     * Updates the draw-round counter for this event.
     *
     * @param drawARound Draw-round counter to store.
     */
    public void setDrawARound(int drawARound) {
        this.drawARound = drawARound;
    }

    /**
     * Returns the waiting-list helper associated with this event.
     *
     * @return Waiting-list helper.
     */
    public WaitingList getWaitingList() {
        return waitingList;
    }

    /**
     * Updates the waiting-list helper associated with this event.
     *
     * @param waitingList Waiting-list helper to store.
     */
    public void setWaitingList(WaitingList waitingList) {
        this.waitingList = waitingList;
    }

    /**
     * Returns the pending-list helper associated with this event.
     *
     * @return Pending-list helper.
     */
    public PendingList getPendingList() {
        return pendingList;
    }

    /**
     * Updates the pending-list helper associated with this event.
     *
     * @param pendingList Pending-list helper to store.
     */
    public void setPendingList(PendingList pendingList) {
        this.pendingList = pendingList;
    }

    /**
     * Returns the accepted-list helper associated with this event.
     *
     * @return Accepted-list helper.
     */
    public AcceptedList getAcceptedList() {
        return acceptedList;
    }

    /**
     * Updates the accepted-list helper associated with this event.
     *
     * @param acceptedList Accepted-list helper to store.
     */
    public void setAcceptedList(AcceptedList acceptedList) {
        this.acceptedList = acceptedList;
    }

    /**
     * Returns the declined-list helper associated with this event.
     *
     * @return Declined-list helper.
     */
    public DeclinedList getDeclinedList() {
        return declinedList;
    }

    /**
     * Updates the declined-list helper associated with this event.
     *
     * @param declinedList Declined-list helper to store.
     */
    public void setDeclinedList(DeclinedList declinedList) {
        this.declinedList = declinedList;
    }

    /**
     * Returns the timestamp when the event was last modified.
     *
     * @return Last-modified timestamp.
     */
    public Timestamp getDateModified() {
        return dateModified;
    }

    /**
     * Updates the last-modified timestamp.
     *
     * @param dateModified Last-modified timestamp to store.
     */
    public void setDateModified(Timestamp dateModified) {
        this.dateModified = dateModified;
    }

    /**
     * Returns the QR payload associated with the event.
     *
     * @return QR payload string.
     */
    public String getQrValue() {
        return qrValue;
    }

    /**
     * Updates the QR payload associated with the event.
     *
     * @param qrValue QR payload string to store.
     */
    public void setQrValue(String qrValue) {
        this.qrValue = qrValue;
    }

    /**
     * Returns the maximum waiting-list capacity for the event.
     *
     * @return Waiting-list capacity, or a negative value when effectively unlimited.
     */
    public int getWaitingListCapacity() {
        return waitingListCapacity;
    }

    /**
     * Updates the maximum waiting-list capacity for the event.
     *
     * @param waitingListCapacity Waiting-list capacity to store.
     */
    public void setWaitingListCapacity(int waitingListCapacity) {
        this.waitingListCapacity = waitingListCapacity;
    }

    /**
     * Returns the event-capacity limit for accepted entrants.
     *
     * @return Event-capacity limit.
     */
    public int getEventCapacity() {
        return eventCapacity;
    }

    /**
     * Updates the event-capacity limit for accepted entrants.
     *
     * @param eventCapacity Event-capacity limit to store.
     */
    public void setEventCapacity(int eventCapacity) {
        this.eventCapacity = eventCapacity;
    }

    /**
     * Returns the event identifier using the older helper name still referenced in some screens.
     *
     * @return Event document identifier.
     */
    public String getId() {
        return getEventId();
    }

    /**
     * Returns the event description using the older helper name still referenced in some screens.
     *
     * @return Event description or an empty string if none is set.
     */
    public String getDetails() {
        return description == null ? "" : description;
    }

    /**
     * Returns the poster/image URI using the older helper name still referenced in some screens.
     *
     * @return Poster URI string, or {@code null} if none exists.
     */
    public String getPosterUriString() {
        return image;
    }

    /**
     * Returns the registration-start timestamp as epoch milliseconds.
     *
     * @return Registration start timestamp in milliseconds, or {@code 0L} if unavailable.
     */
    public long getRegFromMillis() {
        return timestampToMillis(registrationStartDate);
    }

    /**
     * Returns the registration-end timestamp as epoch milliseconds.
     *
     * @return Registration end timestamp in milliseconds, or {@code 0L} if unavailable.
     */
    public long getRegToMillis() {
        return timestampToMillis(registrationEndDate);
    }

    /**
     * Returns the waiting-list capacity using the older nullable helper style.
     *
     * @return Waiting-list capacity, or {@code null} if the event is effectively unlimited.
     */
    public Integer getWaitingListCap() {
        return waitingListCapacity < 0 ? null : waitingListCapacity;
    }

    /**
     * Returns the geolocation requirement using the older helper name still referenced in some screens.
     *
     * @return {@code true} if entrants must satisfy geolocation checks.
     */
    public boolean isGeoRequired() {
        return isGeolocationEnforced();
    }

    /**
     * Converts an optional Firestore timestamp into epoch milliseconds.
     *
     * @param timestamp Firestore timestamp to convert.
     * @return Epoch milliseconds, or {@code 0L} if the timestamp is absent.
     */
    private long timestampToMillis(Timestamp timestamp) {
        return timestamp == null ? 0L : timestamp.toDate().getTime();
    }

    /**
     * Resyncs the list objects with information from the db
     */
    public void refreshListsFromDB() {
        waitingList.refresh(new StatusList.ListUpdateListener() {
            @Override
            public void onUpdate() {
            }

            @Override
            public void onError(Exception e) {
                Log.e("EventDB", "Error: ", e);
            }
        });
        pendingList.refresh(new StatusList.ListUpdateListener() {
            @Override
            public void onUpdate() {
            }

            @Override
            public void onError(Exception e) {
                Log.e("EventDB", "Error: ", e);
            }
        });
        acceptedList.refresh(new StatusList.ListUpdateListener() {
            @Override
            public void onUpdate() {
            }

            @Override
            public void onError(Exception e) {
                Log.e("EventDB", "Error: ", e);
            }
        });
        declinedList.refresh(new StatusList.ListUpdateListener() {
            @Override
            public void onUpdate() {
            }

            @Override
            public void onError(Exception e) {
                Log.e("EventDB", "Error: ", e);
            }
        });
    }
}
