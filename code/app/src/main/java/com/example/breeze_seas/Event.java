package com.example.breeze_seas;

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

    public String getId() {
        return id;
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

    public long getRegFromMillis() {
        return regFromMillis;
    }

    public long getRegToMillis() {
        return regToMillis;
    }

    public Integer getWaitingListCap() {
        return waitingListCap;
    }

    public boolean isGeoRequired() {
        return geoRequired;
    }
}