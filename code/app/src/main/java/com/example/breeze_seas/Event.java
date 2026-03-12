package com.example.breeze_seas;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;

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

    // For use by EventDB
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

    //*
    // For practical instantiation use of event
    // Please pass the event object to EventDb.addEvent() afterwards.
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
        this.waitingList = new WaitingList(this.eventId, waitingListCapacity);
        this.pendingList = new PendingList(this.eventId -1);
        this.acceptedList = new AcceptedList(this.eventId, eventCapacity);
        this.declinedList = new DeclinedList(this.eventId, -1);
    }

    // Bare minimum
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
        this.waitingList = new WaitingList(this.eventId, -1);
        this.pendingList = new PendingList(this.eventId -1);
        this.acceptedList = new AcceptedList(this.eventId, eventCapacity);
        this.declinedList = new DeclinedList(this.eventId, -1);
    }

    public String getEventId() {
        return eventId;
    }
    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getOrganizerId() {
        return organizerId;
    }

    public void setOrganizerId(String organizerId) {
        this.organizerId = organizerId;
    }

    public boolean isGeolocationEnforced() {
        return geolocationEnforced;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public Timestamp getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(Timestamp dateCreated) {
        this.dateCreated = dateCreated;
    }

    public Timestamp getRegistrationStartDate() {
        return registrationStartDate;
    }

    public void setRegistrationStartDate(Timestamp registrationStartDate) {
        this.registrationStartDate = registrationStartDate;
    }

    public Timestamp getRegistrationEndDate() {
        return registrationEndDate;
    }

    public void setRegistrationEndDate(Timestamp registrationEndDate) {
        this.registrationEndDate = registrationEndDate;
    }

    public Timestamp getEventStartDate() {
        return eventStartDate;
    }

    public void setEventStartDate(Timestamp eventStartDate) {
        this.eventStartDate = eventStartDate;
    }

    public Timestamp getEventEndDate() {
        return eventEndDate;
    }

    public void setEventEndDate(Timestamp eventEndDate) {
        this.eventEndDate = eventEndDate;
    }

    public int getDrawARound() {
        return drawARound;
    }

    public void setDrawARound(int drawARound) {
        this.drawARound = drawARound;
    }

    public WaitingList getWaitingList() {
        return waitingList;
    }

    public void setWaitingList(WaitingList waitingList) {
        this.waitingList = waitingList;
    }

    public PendingList getPendingList() {
        return pendingList;
    }

    public void setPendingList(PendingList pendingList) {
        this.pendingList = pendingList;
    }

    public AcceptedList getAcceptedList() {
        return acceptedList;
    }

    public void setAcceptedList(AcceptedList acceptedList) {
        this.acceptedList = acceptedList;
    }

    public DeclinedList getDeclinedList() {
        return declinedList;
    }

    public void setDeclinedList(DeclinedList declinedList) {
        this.declinedList = declinedList;
    }

    public Timestamp getDateModified() {
        return dateModified;
    }

    public void setDateModified(Timestamp dateModified) {
        this.dateModified = dateModified;
    }

    public String getQrValue() {
        return qrValue;
    }

    public void setQrValue(String qrValue) {
        this.qrValue = qrValue;
    }

    public int getWaitingListCapacity() {
        return waitingListCapacity;
    }

    public void setWaitingListCapacity(int waitingListCapacity) {
        this.waitingListCapacity = waitingListCapacity;
    }

    public int getEventCapacity() {
        return eventCapacity;
    }

    public void setEventCapacity(int eventCapacity) {
        this.eventCapacity = eventCapacity;
    }

}