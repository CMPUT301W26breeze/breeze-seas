package com.example.breeze_seas;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Event {
    private final String id;
    private final String organizerId;

    private final String name;
    private final String details;
    private final String posterUriString;

    private final Timestamp registrationOpen;
    private final Timestamp registrationClose;
    private final Timestamp createdAt;

    private final Integer eventCapacity;
    private final Integer waitingListCapacity;

    private final boolean geoRequired;

    private final List<String> waitingList;
    private final List<String> invitationList;
    private final List<String> finalList;
    private final List<String> cancelList;

    public Event(String id,
                 String organizerId,
                 String name,
                 String details,
                 String posterUriString,
                 Timestamp registrationOpen,
                 Timestamp registrationClose,
                 Timestamp createdAt,
                 Integer eventCapacity,
                 Integer waitingListCapacity,
                 boolean geoRequired,
                 List<String> waitingList,
                 List<String> invitationList,
                 List<String> finalList,
                 List<String> cancelList) {
        this.id = id;
        this.organizerId = organizerId;
        this.name = name;
        this.details = details;
        this.posterUriString = posterUriString;
        this.registrationOpen = registrationOpen;
        this.registrationClose = registrationClose;
        this.createdAt = createdAt;
        this.eventCapacity = eventCapacity;
        this.waitingListCapacity = waitingListCapacity;
        this.geoRequired = geoRequired;
        this.waitingList = waitingList == null ? new ArrayList<>() : waitingList;
        this.invitationList = invitationList == null ? new ArrayList<>() : invitationList;
        this.finalList = finalList == null ? new ArrayList<>() : finalList;
        this.cancelList = cancelList == null ? new ArrayList<>() : cancelList;
    }

    public String getId() {
        return id;
    }

    public String getOrganizerId() {
        return organizerId;
    }

    public String getName() {
        return name;
    }

    public String getDetails() {
        return details;
    }

    public String getPosterUriString() {
        return posterUriString;
    }

    public Timestamp getRegistrationOpen() {
        return registrationOpen;
    }

    public Timestamp getRegistrationClose() {
        return registrationClose;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public Integer getEventCapacity() {
        return eventCapacity;
    }

    public Integer getWaitingListCapacity() {
        return waitingListCapacity;
    }

    public boolean isGeoRequired() {
        return geoRequired;
    }

    public List<String> getWaitingList() {
        return waitingList;
    }

    public List<String> getInvitationList() {
        return invitationList;
    }

    public List<String> getFinalList() {
        return finalList;
    }

    public List<String> getCancelList() {
        return cancelList;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("organizerId", organizerId);
        map.put("name", name);
        map.put("details", details);
        map.put("posterUriString", posterUriString);

        map.put("registrationOpen", registrationOpen);
        map.put("registrationClose", registrationClose);
        map.put("createdAt", createdAt);

        map.put("eventCapacity", eventCapacity);
        map.put("waitingListCapacity", waitingListCapacity);

        map.put("geoRequired", geoRequired);

        map.put("waitingList", waitingList);
        map.put("invitationList", invitationList);
        map.put("finalList", finalList);
        map.put("cancelList", cancelList);

        return map;
    }

    @SuppressWarnings("unchecked")
    public static Event fromDocument(DocumentSnapshot doc) {
        if (doc == null || !doc.exists()) return null;

        String organizerId = doc.getString("organizerId");
        String name = doc.getString("name");
        String details = doc.getString("details");
        String posterUriString = doc.getString("posterUriString");

        Timestamp registrationOpen = doc.getTimestamp("registrationOpen");
        Timestamp registrationClose = doc.getTimestamp("registrationClose");
        Timestamp createdAt = doc.getTimestamp("createdAt");

        Long eventCapacityLong = doc.getLong("eventCapacity");
        Long waitingListCapacityLong = doc.getLong("waitingListCapacity");
        Boolean geoRequired = doc.getBoolean("geoRequired");

        List<String> waitingList = (List<String>) doc.get("waitingList");
        List<String> invitationList = (List<String>) doc.get("invitationList");
        List<String> finalList = (List<String>) doc.get("finalList");
        List<String> cancelList = (List<String>) doc.get("cancelList");

        return new Event(
                doc.getId(),
                organizerId == null ? "" : organizerId,
                name == null ? "" : name,
                details == null ? "" : details,
                posterUriString,
                registrationOpen,
                registrationClose,
                createdAt,
                eventCapacityLong == null ? null : eventCapacityLong.intValue(),
                waitingListCapacityLong == null ? null : waitingListCapacityLong.intValue(),
                geoRequired != null && geoRequired,
                waitingList == null ? new ArrayList<>() : waitingList,
                invitationList == null ? new ArrayList<>() : invitationList,
                finalList == null ? new ArrayList<>() : finalList,
                cancelList == null ? new ArrayList<>() : cancelList
        );
    }
}