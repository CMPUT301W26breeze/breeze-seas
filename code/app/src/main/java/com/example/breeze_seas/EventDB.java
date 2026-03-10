package com.example.breeze_seas;

import java.util.ArrayList;
import java.util.List;

public class EventDB {
    private static EventDB instance;
    private final List<Event> events;

    private EventDB() {
        events = new ArrayList<>();

        // seed data
        events.add(new Event(
                "seed-1",
                "CMPUT 301 Meetup",
                "Discuss milestone planning.",
                null,
                1772688000000L,
                1773292800000L,
                null,
                false
        ));

        events.add(new Event(
                "seed-2",
                "Hack Night",
                "Late-night coding event.",
                null,
                1772947200000L,
                1773120000000L,
                50,
                true
        ));
    }

    public static EventDB getInstance() {
        if (instance == null) {
            instance = new EventDB();
        }
        return instance;
    }

    public List<Event> getAllEvents() {
        return new ArrayList<>(events);
    }

    public void addEvent(Event event) {
        events.add(0, event);
    }

    public Event getEventById(String id) {
        for (Event event : events) {
            if (event.getId().equals(id)) {
                return event;
            }
        }
        return null;
    }
}