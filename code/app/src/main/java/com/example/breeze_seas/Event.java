package com.example.breeze_seas;

import android.util.Log;

import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Event stores the organizer-facing and entrant-facing metadata for a single event document.
 */
public class Event {
    private String eventId;
    private boolean isPrivate;
    private String organizerId;
    private ArrayList<String> coOrganizerId;
    private String name;
    private String description;
    private String image;
    private String qrValue;
    private Timestamp createdTimestamp;
    private Timestamp modifiedTimestamp;
    private Timestamp registrationStartTimestamp;
    private Timestamp registrationEndTimestamp;
    private Timestamp eventStartTimestamp;
    private Timestamp eventEndTimestamp;
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
     * @param eventId Unique event identifier.
     * @param isPrivate Whether event is private.
     * @param organizerId Organizer identifier that owns the event.
     * @param coOrganizerId Other organizers that manages the event.
     * @param name Display name of the event.
     * @param description Organizer-provided description.
     * @param image Poster or image URI string.
     * @param qrValue QR payload associated with the event.
     * @param createdTimestamp Timestamp when the event was created.
     * @param modifiedTimestamp Timestamp when the event was last modified.
     * @param registrationStartTimestamp Registration opening timestamp.
     * @param registrationEndTimestamp Registration closing timestamp.
     * @param eventStartTimestamp Event start timestamp.
     * @param eventEndTimestamp Event end timestamp.
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
                 boolean isPrivate,
                 String organizerId,
                 ArrayList<String> coOrganizerId,
                 String name,
                 String description,
                 String image,
                 String qrValue,
                 Timestamp createdTimestamp,
                 Timestamp modifiedTimestamp,
                 Timestamp registrationStartTimestamp,
                 Timestamp registrationEndTimestamp,
                 Timestamp eventStartTimestamp,
                 Timestamp eventEndTimestamp,
                 boolean geolocationEnforced,
                 int eventCapacity,
                 int waitingListCapacity,
                 int drawARound,
                 WaitingList waitingList,
                 PendingList pendingList,
                 AcceptedList acceptedList,
                 DeclinedList declinedList) {
        this.eventId = eventId;
        this.isPrivate = isPrivate;
        this.organizerId = organizerId;
        this.coOrganizerId = coOrganizerId;
        this.name = name;
        this.description = description;
        this.image = image;
        this.qrValue = qrValue;
        this.createdTimestamp = createdTimestamp;
        this.modifiedTimestamp = modifiedTimestamp;
        this.registrationStartTimestamp = registrationStartTimestamp;
        this.registrationEndTimestamp = registrationEndTimestamp;
        this.eventStartTimestamp = eventStartTimestamp;
        this.eventEndTimestamp = eventEndTimestamp;
        this.geolocationEnforced = geolocationEnforced;
        this.eventCapacity = eventCapacity;
        this.waitingListCapacity = waitingListCapacity;
        this.drawARound = drawARound;
        this.waitingList = waitingList;
        this.pendingList = pendingList;
        this.acceptedList = acceptedList;
        this.declinedList = declinedList;
    }

    /**
     * Creates a new event for organizer-side event creation before it is persisted.
     * @param isPrivate Whether event is private.
     * @param organizerId Organizer identifier that owns the new event.
     * @param name Display name of the event.
     * @param description Organizer-provided description.
     * @param image Poster or image URI string.
     * @param qrValue QR payload associated with the event.
     * @param registrationStartTimestamp Registration opening timestamp.
     * @param registrationEndTimestamp Registration closing timestamp.
     * @param eventStartTimestamp Event start timestamp.
     * @param eventEndTimestamp Event end timestamp.
     * @param geolocationEnforced Whether registration requires geolocation.
     * @param eventCapacity Maximum accepted entrants.
     * @param waitingListCapacity Maximum waiting-list entrants.
     * @param drawARound Current draw round counter.
     */
    public Event(boolean isPrivate,
                 String organizerId,
                 String name,
                 String description,
                 String image,
                 String qrValue,
                 Timestamp registrationStartTimestamp,
                 Timestamp registrationEndTimestamp,
                 Timestamp eventStartTimestamp,
                 Timestamp eventEndTimestamp,
                 boolean geolocationEnforced,
                 int eventCapacity,
                 int waitingListCapacity,
                 int drawARound) {
        this.eventId = EventDB.genNewEventId();
        this.isPrivate = isPrivate;
        this.organizerId = organizerId;
        this.coOrganizerId = new ArrayList<String>();// Empty List
        this.name = name;
        this.description = description;
        this.image = image;
        this.qrValue = qrValue;
        this.createdTimestamp = Timestamp.now();  // Use timestamp at creation
        this.modifiedTimestamp = Timestamp.now();  // Use timestamp at creation
        this.registrationStartTimestamp = registrationStartTimestamp;
        this.registrationEndTimestamp = registrationEndTimestamp;
        this.eventStartTimestamp = eventStartTimestamp;
        this.eventEndTimestamp = eventEndTimestamp;
        this.geolocationEnforced = geolocationEnforced;
        this.eventCapacity = eventCapacity;
        this.waitingListCapacity = waitingListCapacity;
        this.drawARound = drawARound;
        this.waitingList = new WaitingList(this, waitingListCapacity);
        this.pendingList = new PendingList(this, -1);
        this.acceptedList = new AcceptedList(this, eventCapacity);
        this.declinedList = new DeclinedList(this, -1);
    }

    /**
     * Creates a minimal event instance for temporary or test organizer flows.
     * @param organizerId Organizer identifier that owns the event.
     * @param eventCapacity Maximum accepted entrants.
     */
    public Event(String organizerId,
                 int eventCapacity) {
        this.eventId = EventDB.genNewEventId();
        this.isPrivate = false;
        this.organizerId = organizerId;
        this.coOrganizerId = new ArrayList<String>();
        this.name = null;
        this.description = null;
        this.image = null;
        this.qrValue = null;
        this.geolocationEnforced = false;
        this.createdTimestamp = Timestamp.now();  // Use timestamp at creation
        this.modifiedTimestamp = Timestamp.now();  // Use timestamp at creation
        this.registrationStartTimestamp = null;
        this.registrationEndTimestamp = null;
        this.eventStartTimestamp = null;
        this.eventEndTimestamp = null;
        this.eventCapacity = eventCapacity;
        this.waitingListCapacity = -1;
        this.drawARound = 0;
        this.waitingList = new WaitingList(this, -1);
        this.pendingList = new PendingList(this, -1);
        this.acceptedList = new AcceptedList(this, eventCapacity);
        this.declinedList = new DeclinedList(this, -1);
    }

    /**
     * Serializes the current event into the Firestore map shape expected by {@link EventDB}.
     * @return Firestore field map for this event.
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("eventId", getEventId());
        map.put("isPrivate", isPrivate());
        map.put("organizerId", getOrganizerId());
        // TODO: Need co-organizers here?
        map.put("name", getName());
        map.put("description", getDescription());
        map.put("image", getImage());
        map.put("qrValue", getQrValue());
        map.put("createdTimestamp", getCreatedTimestamp());
        map.put("modifiedTimestamp", getModifiedTimestamp());
        map.put("registrationStartTimestamp", getRegistrationStartTimestamp());
        map.put("registrationEndTimestamp", getRegistrationEndTimestamp());
        map.put("eventStartTimestamp", getEventStartTimestamp());
        map.put("eventEndTimestamp", getEventEndTimestamp());
        map.put("geolocationEnforced", isGeolocationEnforced());
        map.put("eventCapacity", getEventCapacity());
        map.put("waitingListCapacity", getWaitingListCapacity());
        map.put("drawARound", getDrawARound());
        return map;
    }

    /**
     * Returns the unique identifier for this event.
     * @return Event identifier.
     */
    public String getEventId() {
        return eventId;
    }

    /**
     * Updates the unique identifier for this event.
     * @param eventId Event identifier to store.
     */

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    /**
     * Returns the private state for this event.
     * @return Private state.
     */
    public boolean isPrivate() {
        return isPrivate;
    }

    /**
     * Sets the private state for this event.
     * @param isPrivate Private state.
     */
    public void setPrivate(boolean isPrivate) {
        this.isPrivate = isPrivate;
    }

    /**
     * Returns the organizer identifier that owns this event.
     * @return Organizer identifier.
     */
    public String getOrganizerId() {
        return organizerId;
    }

    /**
     * Updates the organizer identifier associated with this event.
     * @param organizerId Organizer identifier to store.
     */
    public void setOrganizerId(String organizerId) {
        this.organizerId = organizerId;
    }

    /**
     * Returns a list of user IDs that are co-organizers.
     * @return Arraylist of IDs.
     */
    public ArrayList<String> getCoOrganizerId() {
        return coOrganizerId;
    }

    /**
     * Set the list of user IDs that are co-organizers.
     * @param coOrganizerId Arraylist of IDs.
     */
    public void setCoOrganizerId(ArrayList<String> coOrganizerId) {
        this.coOrganizerId = coOrganizerId;
    }

    /**
     * Returns the display name of the event.
     * @return Event name.
     */
    public String getName() {
        return name;
    }

    /**
     * Updates the display name of the event.
     * @param name Event name to store.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the organizer-provided description for this event.
     * @return Event description.
     */
    public String getDescription() {
        return description == null ? "" : description;
    }

    /**
     * Updates the description for this event.
     * @param description Event description to store.
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Returns the poster or image URI associated with the event.
     * @return Poster or image URI string.
     */
    public String getImage() {
        return image;
    }

    /**
     * Updates the poster or image URI associated with the event.
     * @param image Poster or image URI string to store.
     */
    public void setImage(String image) {
        this.image = image;
    }

    /**
     * Returns the QR payload associated with the event.
     * @return QR payload string.
     */
    public String getQrValue() {
        return qrValue;
    }

    /**
     * Updates the QR payload associated with the event.
     * @param qrValue QR payload string to store.
     */
    public void setQrValue(String qrValue) {
        this.qrValue = qrValue;
    }

    /**
     * Returns the timestamp when the event was created.
     * @return Event creation timestamp.
     */
    public Timestamp getCreatedTimestamp() {
        return createdTimestamp;
    }

    /**
     * Short for {@link Event#getCreatedTimestamp()}
     * @return Event creation timestamp.
     */
    public Timestamp created() {
        return createdTimestamp;
    }

    /**
     * Updates the event creation timestamp.
     * @param createdTimestamp Creation timestamp to store.
     */
    public void setCreatedTimestamp(Timestamp createdTimestamp) {
        this.createdTimestamp = createdTimestamp;
    }

    /**
     * Returns the timestamp when the event was last modified.
     * @return Last-modified timestamp.
     */
    public Timestamp getModifiedTimestamp() {
        return modifiedTimestamp;
    }

    /**
     * Short for {@link Event#getCreatedTimestamp()}
     * @return Last-modified timestamp.
     */
    public Timestamp modified() {
        return modifiedTimestamp;
    }

    /**
     * Updates the last-modified timestamp.
     * @param modifiedTimestamp Last-modified timestamp to store.
     */
    public void setModifiedTimestamp(Timestamp modifiedTimestamp) {
        this.modifiedTimestamp = modifiedTimestamp;
    }

    /**
     * Returns the timestamp when registration opens.
     * @return Registration opening timestamp.
     */
    public Timestamp getRegistrationStartTimestamp() {
        return registrationStartTimestamp;
    }

    /**
     * Short for {@link Event#getRegistrationStartTimestamp()}
     * @return Registration opening timestamp.
     */
    public Timestamp regStart() {
        return registrationStartTimestamp;
    }

    /**
     * (DEPRECATED) Returns the registration-start timestamp as epoch milliseconds.
     * @return Registration start timestamp in milliseconds, or {@code 0L} if unavailable.
     */
    public long getRegFromMillis() {
        return timestampToMillis(registrationStartTimestamp);
    }

    /**
     * Updates the registration opening timestamp.
     * @param registrationStartTimestamp Registration opening timestamp to store.
     */
    public void setRegistrationStartTimestamp(Timestamp registrationStartTimestamp) {
        this.registrationStartTimestamp = registrationStartTimestamp;
    }

    /**
     * Returns the timestamp when registration closes.
     * @return Registration closing timestamp.
     */
    public Timestamp getRegistrationEndTimestamp() {
        return registrationEndTimestamp;
    }

    /**
     * Short for {@link Event#getRegistrationEndTimestamp()}
     * @return Registration closing timestamp
     */
    public Timestamp regEnd() {
        return registrationEndTimestamp;
    }

    /**
     * (DEPRECATED) Returns the registration-end timestamp as epoch milliseconds.
     * @return Registration end timestamp in milliseconds, or {@code 0L} if unavailable.
     */
    public long getRegToMillis() {
        return timestampToMillis(registrationEndTimestamp);
    }

    /**
     * Updates the registration closing timestamp.
     * @param registrationEndTimestamp Registration closing timestamp to store.
     */
    public void setRegistrationEndTimestamp(Timestamp registrationEndTimestamp) {
        this.registrationEndTimestamp = registrationEndTimestamp;
    }

    /**
     * (DEPRECATED) Converts an optional Firestore timestamp into epoch milliseconds.
     * @param timestamp Firestore timestamp to convert.
     * @return Epoch milliseconds, or {@code 0L} if the timestamp is absent.
     */
    private long timestampToMillis(Timestamp timestamp) {
        return timestamp == null ? 0L : timestamp.toDate().getTime();
    }

    /**
     * Returns the event start timestamp.
     * @return Event start timestamp.
     */
    public Timestamp getEventStartTimestamp() {
        return eventStartTimestamp;
    }

    /**
     * Short for {@link Event#getEventStartTimestamp()}
     * @return Event start timestamp.
     */
    public Timestamp eventStart() {
        return eventStartTimestamp;
    }

    /**
     * Updates the event start timestamp.
     * @param eventStartTimestamp Event start timestamp to store.
     */
    public void setEventStartTimestamp(Timestamp eventStartTimestamp) {
        this.eventStartTimestamp = eventStartTimestamp;
    }

    /**
     * Returns the event end timestamp.
     * @return Event end timestamp.
     */
    public Timestamp getEventEndTimestamp() {
        return eventEndTimestamp;
    }

    /**
     * Short for {@link Event#getEventEndTimestamp()}
     * @return Event end timestamp.
     */
    public Timestamp eventEnd() {
        return eventEndTimestamp;
    }

    /**
     * Updates the event end timestamp.
     * @param eventEndTimestamp Event end timestamp to store.
     */
    public void setEventEndTimestamp(Timestamp eventEndTimestamp) {
        this.eventEndTimestamp = eventEndTimestamp;
    }

    /**
     * Returns whether registration for this event enforces geolocation.
     * @return {@code true} if geolocation checks are required.
     */
    public boolean isGeolocationEnforced() {
        return geolocationEnforced;
    }

    /**
     * Returns the event-capacity limit for accepted entrants.
     * @return Event-capacity limit.
     */
    public int getEventCapacity() {
        return eventCapacity;
    }

    /**
     * Updates the event-capacity limit for accepted entrants.
     * @param eventCapacity Event-capacity limit to store.
     */
    public void setEventCapacity(int eventCapacity) {
        this.eventCapacity = eventCapacity;
    }

    /**
     * Returns the maximum waiting-list capacity for the event.
     * @return Waiting-list capacity, or a negative value when effectively unlimited.
     */
    public int getWaitingListCapacity() {
        return waitingListCapacity;
    }

    /**
     * (DEPRECATED) Returns the waiting-list capacity using the older nullable helper style.
     * @return Waiting-list capacity, or {@code null} if the event is effectively unlimited.
     */
    public Integer getWaitingListCap() {
        return waitingListCapacity < 0 ? null : waitingListCapacity;
    }


    /**
     * Updates the maximum waiting-list capacity for the event.
     * @param waitingListCapacity Waiting-list capacity to store.
     */
    public void setWaitingListCapacity(int waitingListCapacity) {
        this.waitingListCapacity = waitingListCapacity;
    }

    /**
     * Returns the current draw-round counter for this event.
     * @return Draw-round counter.
     */
    public int getDrawARound() {
        return drawARound;
    }

    /**
     * Updates the draw-round counter for this event.
     * @param drawARound Draw-round counter to store.
     */
    public void setDrawARound(int drawARound) {
        this.drawARound = drawARound;
    }

    /**
     * Returns the waiting-list helper associated with this event.
     * @return Waiting-list helper.
     */
    public WaitingList getWaitingList() {
        return waitingList;
    }

    /**
     * Updates the waiting-list helper associated with this event.
     * @param waitingList Waiting-list helper to store.
     */
    public void setWaitingList(WaitingList waitingList) {
        this.waitingList = waitingList;
    }

    /**
     * Returns the pending-list helper associated with this event.
     * @return Pending-list helper.
     */
    public PendingList getPendingList() {
        return pendingList;
    }

    /**
     * Updates the pending-list helper associated with this event.
     * @param pendingList Pending-list helper to store.
     */
    public void setPendingList(PendingList pendingList) {
        this.pendingList = pendingList;
    }

    /**
     * Returns the accepted-list helper associated with this event.
     * @return Accepted-list helper.
     */
    public AcceptedList getAcceptedList() {
        return acceptedList;
    }

    /**
     * Updates the accepted-list helper associated with this event.
     * @param acceptedList Accepted-list helper to store.
     */
    public void setAcceptedList(AcceptedList acceptedList) {
        this.acceptedList = acceptedList;
    }

    /**
     * Returns the declined-list helper associated with this event.
     * @return Declined-list helper.
     */
    public DeclinedList getDeclinedList() {
        return declinedList;
    }

    /**
     * Updates the declined-list helper associated with this event.
     * @param declinedList Declined-list helper to store.
     */
    public void setDeclinedList(DeclinedList declinedList) {
        this.declinedList = declinedList;
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
