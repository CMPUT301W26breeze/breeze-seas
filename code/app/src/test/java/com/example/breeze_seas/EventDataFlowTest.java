package com.example.breeze_seas;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.google.firebase.Timestamp;

import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.Map;

/**
 * Unit tests for organizer-facing event data flow used by the Organize landing page and event
 * preview screens.
 */
public class EventDataFlowTest {

    /**
     * Forces Architecture Components work onto the test thread so {@link Event} can create and
     * hydrate {@link androidx.lifecycle.MutableLiveData} safely in the JVM test environment.
     */
    @Rule
    public final InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    /**
     * Verifies that organizer-facing event metadata survives the create-to-map-to-hydrated-event
     * round trip used by the Organize landing page and event preview.
     */
    @Test
    public void toMapAndLoadMap_preservesOrganizerFacingFields() {
        Timestamp createdTimestamp = new Timestamp(new Date(1893286800000L));
        Timestamp modifiedTimestamp = new Timestamp(new Date(1893290400000L));
        Timestamp registrationStart = new Timestamp(new Date(1893373200000L));
        Timestamp registrationEnd = new Timestamp(new Date(1893459600000L));
        Timestamp eventStart = new Timestamp(new Date(1893546000000L));
        Timestamp eventEnd = new Timestamp(new Date(1893632400000L));

        Event originalEvent = new Event(
                "event-data-flow",
                true,
                "organizer-device-id",
                new ArrayList<>(),
                "Private Showcase",
                "Invite-only launch event",
                null,
                createdTimestamp,
                modifiedTimestamp,
                registrationStart,
                registrationEnd,
                eventStart,
                eventEnd,
                true,
                120,
                30,
                2,
                null,
                null,
                null,
                null
        );

        Map<String, Object> serializedEvent = originalEvent.toMap();
        Event hydratedEvent = new Event(serializedEvent);

        assertTrue(hydratedEvent.isPrivate());
        assertEquals("event-data-flow", hydratedEvent.getEventId());
        assertEquals("organizer-device-id", hydratedEvent.getOrganizerId());
        assertEquals("Private Showcase", hydratedEvent.getName());
        assertEquals("Invite-only launch event", hydratedEvent.getDescription());
        assertEquals(createdTimestamp, hydratedEvent.getCreatedTimestamp());
        assertEquals(modifiedTimestamp, hydratedEvent.getModifiedTimestamp());
        assertEquals(registrationStart, hydratedEvent.getRegistrationStartTimestamp());
        assertEquals(registrationEnd, hydratedEvent.getRegistrationEndTimestamp());
        assertEquals(eventStart, hydratedEvent.getEventStartTimestamp());
        assertEquals(eventEnd, hydratedEvent.getEventEndTimestamp());
        assertTrue(hydratedEvent.isGeolocationEnforced());
        assertEquals(120, hydratedEvent.getEventCapacity());
        assertEquals(30, hydratedEvent.getWaitingListCapacity());
        assertEquals(2, hydratedEvent.getDrawARound());
    }

    /**
     * Verifies that Firestore event maps missing optional organizer fields still hydrate into a
     * safe event model for the Organize landing page and event preview.
     */
    @Test
    public void loadMap_missingOptionalOrganizerFieldsUsesSafeDefaults() {
        Timestamp createdTimestamp = new Timestamp(new Date(1893286800000L));
        Timestamp modifiedTimestamp = new Timestamp(new Date(1893290400000L));
        Timestamp registrationStart = new Timestamp(new Date(1893373200000L));
        Timestamp registrationEnd = new Timestamp(new Date(1893459600000L));
        ArrayList<String> coOrganizers = new ArrayList<>();
        Map<String, Object> eventMap = new java.util.HashMap<>();

        eventMap.put("eventId", "event-legacy");
        eventMap.put("isPrivate", false);
        eventMap.put("organizerId", "legacy-organizer");
        eventMap.put("coOrganizerId", coOrganizers);
        eventMap.put("name", "Legacy Public Event");
        eventMap.put("description", null);
        eventMap.put("createdTimestamp", createdTimestamp);
        eventMap.put("modifiedTimestamp", modifiedTimestamp);
        eventMap.put("registrationStartTimestamp", registrationStart);
        eventMap.put("registrationEndTimestamp", registrationEnd);
        eventMap.put("eventStartTimestamp", null);
        eventMap.put("eventEndTimestamp", null);
        eventMap.put("geolocationEnforced", false);
        eventMap.put("eventCapacity", 75);

        Event hydratedEvent = new Event(eventMap);

        assertEquals("event-legacy", hydratedEvent.getEventId());
        assertFalse(hydratedEvent.isPrivate());
        assertEquals("legacy-organizer", hydratedEvent.getOrganizerId());
        assertEquals(coOrganizers, hydratedEvent.getCoOrganizerId());
        assertEquals("Legacy Public Event", hydratedEvent.getName());
        assertEquals("", hydratedEvent.getDescription());
        assertEquals(createdTimestamp, hydratedEvent.getCreatedTimestamp());
        assertEquals(modifiedTimestamp, hydratedEvent.getModifiedTimestamp());
        assertEquals(registrationStart, hydratedEvent.getRegistrationStartTimestamp());
        assertEquals(registrationEnd, hydratedEvent.getRegistrationEndTimestamp());
        assertNull(hydratedEvent.getEventStartTimestamp());
        assertNull(hydratedEvent.getEventEndTimestamp());
        assertFalse(hydratedEvent.isGeolocationEnforced());
        assertEquals(75, hydratedEvent.getEventCapacity());
        assertEquals(-1, hydratedEvent.getWaitingListCapacity());
        assertEquals(0, hydratedEvent.getDrawARound());
    }

    /**
     * Verifies that co-organizer identifiers survive the Firestore map round trip so organizer
     * membership data remains available to the Organize landing page and event preview.
     */
    @Test
    public void toMapAndLoadMap_preservesCoOrganizerIds() {
        Timestamp createdTimestamp = new Timestamp(new Date(1893286800000L));
        Timestamp modifiedTimestamp = new Timestamp(new Date(1893290400000L));
        ArrayList<String> coOrganizers = new ArrayList<>();
        coOrganizers.add("coorg-1");
        coOrganizers.add("coorg-2");

        Event originalEvent = new Event(
                "event-coorganizers",
                false,
                "lead-organizer",
                coOrganizers,
                "Team Planning Session",
                "Organizer-only planning event",
                null,
                createdTimestamp,
                modifiedTimestamp,
                null,
                null,
                null,
                null,
                false,
                25,
                5,
                0,
                null,
                null,
                null,
                null
        );

        Map<String, Object> serializedEvent = originalEvent.toMap();
        Event hydratedEvent = new Event(serializedEvent);

        assertEquals("lead-organizer", hydratedEvent.getOrganizerId());
        assertEquals(coOrganizers, hydratedEvent.getCoOrganizerId());
        assertEquals(3, hydratedEvent.getAllOrganizerId().size());
        assertTrue(hydratedEvent.getAllOrganizerId().contains("lead-organizer"));
        assertTrue(hydratedEvent.getAllOrganizerId().contains("coorg-1"));
        assertTrue(hydratedEvent.getAllOrganizerId().contains("coorg-2"));
    }
}
