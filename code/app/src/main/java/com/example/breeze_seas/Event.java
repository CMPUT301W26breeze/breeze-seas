package com.example.breeze_seas;

import android.graphics.Bitmap;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

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
    private MutableLiveData<Image> image = new MutableLiveData<Image>();
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
    private ListenerRegistration eventListener = null;
    private ListenerRegistration imageListener = null;

    /**
     * Creates an event using the full field set expected when hydrating from {@link EventDB}.
     * @param eventId Unique event identifier.
     * @param isPrivate Whether event is private.
     * @param organizerId Organizer identifier that owns the event.
     * @param coOrganizerId Other organizers that manages the event.
     * @param name Display name of the event.
     * @param description Organizer-provided description.
     * @param image Image object.
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
                 Image image,
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
        this.image.setValue(image);
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
     * Creates an event from Firebase using a map object.
     * @param map Map object to retrieve and initialize values from.
     */
    public Event(Map<String, Object> map) {
        loadMap(map);
        this.waitingList = null;
        this.pendingList = null;
        this.acceptedList = null;
        this.declinedList = null;
    }

    /**
     * Creates a new event for organizer-side event creation before it is persisted.
     * @param isPrivate Whether event is private.
     * @param organizerId Organizer identifier that owns the new event.
     * @param name Display name of the event.
     * @param description Organizer-provided description.
     * @param image Image object.
     * @param registrationStartTimestamp Registration opening timestamp.
     * @param registrationEndTimestamp Registration closing timestamp.
     * @param eventStartTimestamp Event start timestamp.
     * @param eventEndTimestamp Event end timestamp.
     * @param geolocationEnforced Whether registration requires geolocation.
     * @param eventCapacity Maximum accepted entrants.
     * @param waitingListCapacity Maximum waiting-list entrants.
     */
    public Event(boolean isPrivate,
                 String organizerId,
                 String name,
                 String description,
                 Image image,
                 Timestamp registrationStartTimestamp,
                 Timestamp registrationEndTimestamp,
                 Timestamp eventStartTimestamp,
                 Timestamp eventEndTimestamp,
                 boolean geolocationEnforced,
                 int eventCapacity,
                 int waitingListCapacity) {
        this.eventId = EventDB.genNewEventId();
        this.isPrivate = isPrivate;
        this.organizerId = organizerId;
        this.coOrganizerId = new ArrayList<String>();// Empty List
        this.name = name;
        this.description = description;
        this.image.setValue(image);
        this.createdTimestamp = Timestamp.now();  // Use timestamp at creation
        this.modifiedTimestamp = Timestamp.now();  // Use timestamp at creation
        this.registrationStartTimestamp = registrationStartTimestamp;
        this.registrationEndTimestamp = registrationEndTimestamp;
        this.eventStartTimestamp = eventStartTimestamp;
        this.eventEndTimestamp = eventEndTimestamp;
        this.geolocationEnforced = geolocationEnforced;
        this.eventCapacity = eventCapacity;
        this.waitingListCapacity = waitingListCapacity;
        this.drawARound = 0;
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
        this.image.setValue(null);
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
        map.put("coOrganizerId", getCoOrganizerId());
        map.put("name", getName());
        map.put("description", getDescription());
        map.put("imageDocId", (getImage() != null) ? getImage().getImageId() : null);
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
     * Loads the map object from Firestore and updates the event details accordingly.
     * DOES NOT LOAD COMPLEX OBJECTS LIKE participants (the lists classes) and image
     * @param map Map object to populate attribute from.
     */
    public void loadMap(Map<String, Object>  map) {
        // Event Details
        this.eventId = map.get("eventId").toString();
        this.isPrivate = Boolean.TRUE.equals(map.get("isPrivate"));
        this.organizerId = map.get("organizerId").toString();
        this.coOrganizerId = (ArrayList<String>) map.get("coOrganizerId");
        this.name = map.get("name").toString();
        this.description = (map.get("description") == null) ? "" : map.get("description").toString();

        // Timestamps
        this.createdTimestamp = (Timestamp) map.get("createdTimestamp");
        this.modifiedTimestamp = (Timestamp) map.get("modifiedTimestamp");
        this.registrationStartTimestamp = (Timestamp) map.get("registrationStartTimestamp");
        this.registrationEndTimestamp = (Timestamp) map.get("registrationEndTimestamp");
        this.eventStartTimestamp = (Timestamp) map.get("eventStartTimestamp");
        this.eventEndTimestamp = (Timestamp) map.get("eventEndTimestamp");

        // Geolocation
        this.geolocationEnforced = Boolean.TRUE.equals(map.get("geolocationEnforced"));

        // Misc
        this.eventCapacity = ((Number) map.get("eventCapacity")).intValue();
        this.waitingListCapacity = ((Number) map.getOrDefault("waitingListCapacity", -1)).intValue();
        this.drawARound = ((Number) map.getOrDefault("drawARound", 0)).intValue();
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
     * Returns a list of all user IDs that are any kind of organizers.
     * @reutrn Arraylist of IDs
     */
    public ArrayList<String> getAllOrganizerId() {
        ArrayList<String> allOrganizers = new ArrayList<String>(this.coOrganizerId);
        allOrganizers.add(this.organizerId);
        return allOrganizers;
    }

    /**
     * Method to check if a user is the ORIGINAL organizer of the event
     * @param user User object
     * @return true is user is the ORIGINAL organizer of event, otherwise false
     */
    public boolean userIsOrganizer(User user) {
        return Objects.equals(user.getDeviceId(), this.organizerId);
    }

    /**
     * Method to check if a user is a co-organizer of the event
     * @param user User object
     * @return true is user is a co-organizer of event, otherwise false
     */
    public boolean userIsCoOrganizer(User user) {
        return this.coOrganizerId.contains(user.getDeviceId());
    }

    /**
     * Method to check if a user is any kind of organizer of the event
     * @param user User object
     * @return true is user is any kind of organizer of event, otherwise false
     */
    public boolean userIsAnOrganizer(User user) {
        return getAllOrganizerId().contains(user.getDeviceId());
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
     * Returns the image object associated with the event.
     * @return Image object.
     */
    public Image getImage() {
        return image.getValue();
    }

    /**
     * Updates the image object associated with the event.
     * @param image Image object to hold a reference to.
     */
    public void setImage(Image image) {
        this.image.setValue(image);
    }

    /**
     * Returns the image object wrapped in a MutableLiveData.
     * Suitable for use when fragments need to observe when images get updated.
     * @return MutableLiveData that encloses an image object.
     */
    public MutableLiveData<Image> getImageData() {
        return image;
    }

    /**
     * Synonym of {@link Event#getImageBitmap()}
     * @return Bitmap of event poster. Null if image object does not exist.
     */
    public Bitmap getPosterBitmap() {
        return getImageBitmap();
    }

    /**
     * Returns bitmap of event poster to caller.
     * @return Bitmap of event poster. Null if image object does not exist.
     */
    public Bitmap getImageBitmap() {
        return (this.image.getValue() == null) ? null : this.image.getValue().display();
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
        if (waitingList == null) {
            waitingList = new WaitingList(this, waitingListCapacity);
        }
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
        if (pendingList == null) {
            pendingList = new PendingList(this, eventCapacity);
        }
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
        if (acceptedList == null) {
            acceptedList = new AcceptedList(this, eventCapacity);
        }
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
        if (declinedList == null) {
            declinedList = new DeclinedList(this, -1);
        }
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
     * Helper method to remove all users from the list classes.
     */
    private void emptyLists() {
        this.waitingList.getUserList().clear();
        this.pendingList.getUserList().clear();
        this.acceptedList.getUserList().clear();
        this.declinedList.getUserList().clear();
    }

    /**
     * Helper method to ensure list classes are initialized.
     */
    private void setupLists() {
        if (waitingList == null) {
            waitingList = new WaitingList(this, waitingListCapacity);
        }
        if (pendingList == null) {
            pendingList = new PendingList(this, eventCapacity);
        }
        if (acceptedList == null) {
            acceptedList = new AcceptedList(this, eventCapacity);
        }
        if (declinedList == null) {
            declinedList = new DeclinedList(this, -1);
        }
    }

    /**
     * Helper method to place the User in the correct list.
     * @param user User object to add to list.
     * @param status String variable to determine which list the user belongs to.
     */
    private void addUserToList(User user, String status) {
        switch(status) {
            case "waiting":
                waitingList.getUserList().add(user);
                break;
            case "pending":
                pendingList.getUserList().add(user);
                break;
            case "accepted":
                acceptedList.getUserList().add(user);
                break;
            case "declined":
                declinedList.getUserList().add(user);
                break;
            default:
                Log.e("Event Class", "Unable to place User in the correct list");
        }
    }

    /**
     * Helper method to remove user from list classes based on deviceId.
     * @param user User object containing the deviceId to match against and remove.
     *             Can be a simple user object with only deviceId present.
     */
    private void removeUserFromList(User user) {
        if (waitingList.userIsInList(user)) {
            waitingList.popUser(user.getDeviceId());
            return;
        }
        if (pendingList.userIsInList(user)) {
            pendingList.popUser(user.getDeviceId());
            return;
        }
        if (acceptedList.userIsInList(user)) {
            acceptedList.popUser(user.getDeviceId());
            return;
        }
        if (declinedList.userIsInList(user)) {
            declinedList.popUser(user.getDeviceId());
        }
    }

    public interface EventUpdatedCallback {
        void onUpdated();
        void onDeleted();
        void onFailure(Exception e);
    }

    /**
     * Method to get realtime updates of event.
     */
    public void startEventListen(EventUpdatedCallback callback) {
        // Check whether the listener is already present
        if (eventListener != null) {
            return;
        }

        // Listener on Event
        final DocumentReference docRef = EventDB.getEventRef().document(this.eventId);
        eventListener = docRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot eventDoc,
                                @Nullable FirebaseFirestoreException error) {
                // Check for errors
                if (error != null) {
                    Log.w("Event Class", "Listen failed.", error);
                    callback.onFailure(error);
                    return;  // This will automatically close listener
                }

                // Determine whether change was local (database changed caused by this device) or
                // change was server (database changed caused by another device, not this device)
                String source = eventDoc != null && eventDoc.getMetadata().hasPendingWrites()
                        ? "Local" : "Server";

                if (eventDoc != null && eventDoc.exists()) {
                    // Event document is updated
                    Map<String, Object> map = eventDoc.getData();
                    Log.d("Event Class", source + " data: " + map);
                    // Load new information
                    loadMap(map);
                    callback.onUpdated();

                } else {
                    // Event document gets deleted
                    Log.d("Event Class", source + " data: null");
                    callback.onDeleted();
                }
            }
        });
    }

    /**
     * Stop fetching realtime updates of event.
     */
    public void stopEventListen() {
        // Check if listener is active
        if (eventListener != null) {
            eventListener.remove();
        }
    }

    public void startListenAllLists(StatusList.ListUpdateListener listener) {
        if (getWaitingList() != null) getWaitingList().startListening(listener);
        if (getPendingList() != null) getPendingList().startListening(listener);
        if (getAcceptedList() != null) getAcceptedList().startListening(listener);
        if (getDeclinedList() != null) getDeclinedList().startListening(listener);
    }

    public void stopListenAllLists() {
        if (waitingList != null) waitingList.stopListening();
        if (pendingList != null) pendingList.stopListening();
        if (acceptedList != null) acceptedList.stopListening();
        if (declinedList != null) declinedList.stopListening();
    }


    /**
     * Activates an image listener for the given event.
     * @param imageDocId Image Document Id to listen at.
     */
    public void startImageListen(String imageDocId) {
        // Check whether the listener is already present
        if (imageListener != null) {
            return;
        }

        // Lister on image
        imageListener = DBConnector.getDb().collection("images")
                .document(imageDocId)
                .addSnapshotListener(new EventListener<DocumentSnapshot>() {
                    @Override
                    public void onEvent(@Nullable DocumentSnapshot imgDoc,
                                        @Nullable FirebaseFirestoreException error) {
                        // Check for errors
                        if (error != null) {
                            Log.w("Event Class", "Listen failed.", error);
                            setImage(null);
                            return;  // This will automatically close listener
                        }

                        // Determine whether change was local (database changed caused by this device) or
                        // change was server (database changed caused by another device, not this device)
                        String source = imgDoc != null && imgDoc.getMetadata().hasPendingWrites()
                                ? "Local" : "Server";

                        if (imgDoc != null && imgDoc.exists()) {
                            // Image document is updated
                            Map<String, Object> map = imgDoc.getData();
                            Log.d("Event Class", source + " data: " + map);
                            // Load new information
                            setImage(new Image(imgDoc.getId(), map.get("data").toString()));

                        } else {
                            // Image document gets deleted
                            Log.d("Event Class", source + " data: null");
                            setImage(null);
                        }
                    }
                });
    }

    /**
     * Stop fetching realtime updates of image pertaining to this event.
     */
    public void stopImageListen() {
        // Check if listener is active
        if (imageListener != null) {
            imageListener.remove();
        }
    }


    public interface ListsLoadedCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    /**
     * Helper method to query and generate and populate lists with user objects.
     * @param callback Callback method after firebase transaction.
     */
    public void fetchLists(ListsLoadedCallback callback) {
        // WARNING: Asynchronous call
        EventDB.getEventRef()
                .document(eventId)
                .collection("participants")
                .get()
                .addOnSuccessListener(participantsDocs -> {
                    emptyLists();

                    // Fetch each user
                    CollectionReference usersCollection = DBConnector.getDb().collection("users");
                    int total = participantsDocs.size();
                    final int[] fetched = {0};
                    for (DocumentSnapshot participantDoc: participantsDocs) {
                        String deviceId = participantDoc.getId();
                        String status = participantDoc.get("status", String.class).toString();

                        usersCollection.document(deviceId)
                                .get()
                                .addOnSuccessListener(userDoc -> {
                                    if (userDoc.exists()) {
                                        addUserToList(userDoc.toObject(User.class), status);

                                        // Check Finish Condition
                                        fetched[0]++;
                                        if (fetched[0] >= total) {
                                            callback.onSuccess();
                                        }
                                    }

                                })
                                .addOnFailureListener(e -> {
                                    Log.e("Event Class", "Unable to fetch user document.");
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("Event Class", "Error: ", e);
                    callback.onFailure(e);
                });
    }

    /**
     * Resyncs the list objects with information from the db
     */
    public void refreshListsFromDB() {
        // Ensure list classes are initialized
        setupLists();

        fetchLists(new ListsLoadedCallback() {
            @Override
            public void onSuccess() {

            }

            @Override
            public void onFailure(Exception e) {

            }
        });
    }

    /**
     * Generates a string representation of the event object.
     * Useful when filtering for specific keywords.
     * @return A string representation of the event object.
     */
    public String toString() {
        String tmp;
        // Get name and description of event
        tmp = getName() == null ? "" : getName().toLowerCase(Locale.US);
        tmp = tmp + ((getDescription() == null) ? "" : getDescription().toLowerCase(Locale.US));
        return tmp;
    }
}
