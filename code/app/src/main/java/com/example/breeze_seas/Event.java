package com.example.breeze_seas;

import com.google.firebase.firestore.DocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

public class Event {
    private final String id;
    private final String name;
    private final String details;
    private final String posterUriString;
    private final long regFromMillis;
    private final long regToMillis;
    private final Integer waitingListCap; // null = unlimited
    private final boolean geoRequired;

    public Event(String id,
                 String name,
                 String details,
                 String posterUriString,
                 long regFromMillis,
                 long regToMillis,
                 Integer waitingListCap,
                 boolean geoRequired) {
        this.id = id;
        this.name = name;
        this.details = details;
        this.posterUriString = posterUriString;
        this.regFromMillis = regFromMillis;
        this.regToMillis = regToMillis;
        this.waitingListCap = waitingListCap;
        this.geoRequired = geoRequired;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDetails() { return details; }
    public String getPosterUriString() { return posterUriString; }
    public long getRegFromMillis() { return regFromMillis; }
    public long getRegToMillis() { return regToMillis; }
    public Integer getWaitingListCap() { return waitingListCap; }
    public boolean isGeoRequired() { return geoRequired; }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("details", details);
        map.put("posterUriString", posterUriString);
        map.put("regFromMillis", regFromMillis);
        map.put("regToMillis", regToMillis);
        map.put("waitingListCap", waitingListCap);
        map.put("geoRequired", geoRequired);
        return map;
    }

    public static Event fromDocument(DocumentSnapshot doc) {
        if (doc == null || !doc.exists()) return null;

        String name = doc.getString("name");
        String details = doc.getString("details");
        String posterUriString = doc.getString("posterUriString");

        Long regFrom = doc.getLong("regFromMillis");
        Long regTo = doc.getLong("regToMillis");
        Long capLong = doc.getLong("waitingListCap");
        Boolean geo = doc.getBoolean("geoRequired");

        return new Event(
                doc.getId(),
                name == null ? "" : name,
                details == null ? "" : details,
                posterUriString,
                regFrom == null ? 0L : regFrom,
                regTo == null ? 0L : regTo,
                capLong == null ? null : capLong.intValue(),
                geo != null && geo
        );
    }
}