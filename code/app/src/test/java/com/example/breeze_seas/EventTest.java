package com.example.breeze_seas;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Unit tests for small organizer and ownership helpers on {@link Event}.
 */
public class EventTest {

    private Event createTestEvent(User org) {
        return new Event(org.getDeviceId(), 50);
    }

    private User createTestUser(String id) {
        return new User(id, "", "", "", "", false);
    }

    @Test
    public void add() {
        User userA = createTestUser("A");
        Event testEvent = createTestEvent(userA);
        assertTrue(testEvent.userIsOrganizer(userA));
    }
}
