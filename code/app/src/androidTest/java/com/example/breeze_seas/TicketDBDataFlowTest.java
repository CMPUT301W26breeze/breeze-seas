package com.example.breeze_seas;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Data-flow tests for {@link TicketDB} that verify how cached Firestore data becomes ticket tabs.
 */
@RunWith(AndroidJUnit4.class)
public class TicketDBDataFlowTest {

    private TicketDB ticketDb;

    @Before
    public void setUp() throws Exception {
        ticketDb = TicketDB.getInstance();
        resetTicketDbState();
    }

    @After
    public void tearDown() throws Exception {
        resetTicketDbState();
    }

    /**
     * Verifies that an accepted participant row with matching event metadata is classified
     * into the Attending tab and mapped into the expected attending ticket fields.
     *
     * @throws Exception When reflection-based cache setup fails.
     */
    @Test
    public void recomputeTicketsFromRealtimeCache_acceptedParticipantCreatesAttendingTicket() throws Exception {
        Timestamp eventStart = new Timestamp(new Date(1893459600000L)); // Tue, Jan 1 2030 5:00 PM UTC
        Timestamp timeJoined = new Timestamp(new Date(1893373200000L));

        DocumentSnapshot eventDocument = mock(DocumentSnapshot.class);
        when(eventDocument.exists()).thenReturn(true);
        when(eventDocument.getId()).thenReturn("event-123");
        when(eventDocument.getString("name")).thenReturn("City Concert");
        when(eventDocument.getString("location")).thenReturn("Main Hall");
        when(eventDocument.getBoolean("isPrivate")).thenReturn(false);
        when(eventDocument.getTimestamp("eventStartTimestamp")).thenReturn(eventStart);

        putIntoPrivateMap("participantEntries", "event-123",
                newParticipantEntry("event-123", "accepted", timeJoined));
        putIntoPrivateMap("eventSnapshots", "event-123", eventDocument);

        invokePrivateMethod("recomputeTicketsFromRealtimeCache");

        List<TicketUIModel> activeTickets = ticketDb.getActiveTickets();
        List<AttendingTicketUIModel> attendingTickets = ticketDb.getAttendingTickets();
        List<PastEventUIModel> pastTickets = ticketDb.getPastTickets();

        assertTrue(activeTickets.isEmpty());
        assertTrue(pastTickets.isEmpty());
        assertEquals(1, attendingTickets.size());

        AttendingTicketUIModel attendingTicket = attendingTickets.get(0);
        assertEquals("event-123", attendingTicket.getEventId());
        assertEquals("City Concert", attendingTicket.getTitle());
        assertEquals("Main Hall", attendingTicket.getLocationLabel());
        assertEquals("Confirmed entry", attendingTicket.getTicketTypeLabel());
        assertEquals("Show this ticket during event check-in.", attendingTicket.getEntryNote());
        assertEquals("Open QR pass", attendingTicket.getActionLabel());
        assertEquals(
                EventMetadataUtils.formatDateTime(eventStart),
                attendingTicket.getDateLabel()
        );
    }

    /**
     * Verifies that unsupported participant statuses are ignored instead of being classified
     * into the Active, Attending, or Past tab lists.
     *
     * @throws Exception When reflection-based cache setup fails.
     */
    @Test
    public void recomputeTicketsFromRealtimeCache_unknownStatusAddsNoTickets() throws Exception {
        Timestamp eventStart = new Timestamp(new Date(1730502000000L));
        Timestamp timeJoined = new Timestamp(new Date(1730415600000L));

        DocumentSnapshot eventDocument = mock(DocumentSnapshot.class);
        when(eventDocument.exists()).thenReturn(true);
        when(eventDocument.getId()).thenReturn("event-unknown");
        when(eventDocument.getString("name")).thenReturn("Unknown Status Event");
        when(eventDocument.getString("location")).thenReturn("Studio 2");
        when(eventDocument.getBoolean("isPrivate")).thenReturn(false);
        when(eventDocument.getTimestamp("eventStartDate")).thenReturn(eventStart);

        putIntoPrivateMap("participantEntries", "event-unknown",
                newParticipantEntry("event-unknown", "mystery_status", timeJoined));
        putIntoPrivateMap("eventSnapshots", "event-unknown", eventDocument);

        invokePrivateMethod("recomputeTicketsFromRealtimeCache");

        assertTrue(ticketDb.getActiveTickets().isEmpty());
        assertTrue(ticketDb.getAttendingTickets().isEmpty());
        assertTrue(ticketDb.getPastTickets().isEmpty());
    }

    /**
     * Verifies that an ended event is moved into the Past tab even when the participant status
     * would normally map to Attending.
     *
     * @throws Exception When reflection-based cache setup fails.
     */
    @Test
    public void recomputeTicketsFromRealtimeCache_endedAcceptedEventCreatesPastTicket() throws Exception {
        Timestamp eventStart = new Timestamp(new Date(1714867200000L)); // Sun, May 5 2024 12:00 AM UTC
        Timestamp eventEnd = new Timestamp(new Date(1714953600000L));   // Mon, May 6 2024 12:00 AM UTC
        Timestamp timeJoined = new Timestamp(new Date(1714780800000L));

        DocumentSnapshot eventDocument = mock(DocumentSnapshot.class);
        when(eventDocument.exists()).thenReturn(true);
        when(eventDocument.getId()).thenReturn("event-past");
        when(eventDocument.getString("name")).thenReturn("Past Concert");
        when(eventDocument.getString("location")).thenReturn("Old Arena");
        when(eventDocument.getBoolean("isPrivate")).thenReturn(false);
        when(eventDocument.getTimestamp("eventStartTimestamp")).thenReturn(eventStart);
        when(eventDocument.getTimestamp("eventEndTimestamp")).thenReturn(eventEnd);

        putIntoPrivateMap("participantEntries", "event-past",
                newParticipantEntry("event-past", "accepted", timeJoined));
        putIntoPrivateMap("eventSnapshots", "event-past", eventDocument);

        invokePrivateMethod("recomputeTicketsFromRealtimeCache");

        assertTrue(ticketDb.getActiveTickets().isEmpty());
        assertTrue(ticketDb.getAttendingTickets().isEmpty());
        assertEquals(1, ticketDb.getPastTickets().size());

        PastEventUIModel pastTicket = ticketDb.getPastTickets().get(0);
        assertEquals("Past Concert", pastTicket.getTitle());
        assertEquals("Old Arena", pastTicket.getLocationLabel());
        assertEquals("Past", pastTicket.getStatusLabel());
        assertEquals("This event has already happened.", pastTicket.getDetailLabel());
        assertEquals(R.drawable.ic_clock, pastTicket.getIconResId());
        assertEquals(EventMetadataUtils.formatDateTime(eventStart), pastTicket.getDateLabel());
    }

    private void resetTicketDbState() throws Exception {
        clearPrivateCollection("listeners");
        clearPrivateCollection("activeTickets");
        clearPrivateCollection("attendingTickets");
        clearPrivateCollection("pastTickets");
        clearPrivateCollection("participantEntries");
        clearPrivateCollection("eventSnapshots");
        clearPrivateCollection("eventListenerRegistrations");
        setPrivateField("currentDeviceId", null);
        setPrivateField("participantEntriesListener", null);
    }

    private void clearPrivateCollection(String fieldName) throws Exception {
        Field field = TicketDB.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        Object value = field.get(ticketDb);
        if (value instanceof List) {
            ((List<?>) value).clear();
        } else if (value instanceof Map) {
            ((Map<?, ?>) value).clear();
        }
    }

    private void setPrivateField(String fieldName, Object value) throws Exception {
        Field field = TicketDB.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(ticketDb, value);
    }

    @SuppressWarnings("unchecked")
    private void putIntoPrivateMap(String fieldName, Object key, Object value) throws Exception {
        Field field = TicketDB.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        Map<Object, Object> map = (Map<Object, Object>) field.get(ticketDb);
        map.put(key, value);
    }

    private Object newParticipantEntry(String eventId, String status, Timestamp timeJoined) throws Exception {
        Class<?> participantEntryClass = Class.forName("com.example.breeze_seas.TicketDB$ParticipantEntry");
        Constructor<?> constructor = participantEntryClass.getDeclaredConstructor(
                String.class,
                String.class,
                Timestamp.class
        );
        constructor.setAccessible(true);
        return constructor.newInstance(eventId, status, timeJoined);
    }

    private void invokePrivateMethod(String methodName) throws Exception {
        Method method = TicketDB.class.getDeclaredMethod(methodName);
        method.setAccessible(true);
        method.invoke(ticketDb);
    }
}
