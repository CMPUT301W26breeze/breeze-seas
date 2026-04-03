package com.example.breeze_seas;

import static org.junit.Assert.assertEquals;

import android.widget.FrameLayout;

import androidx.appcompat.view.ContextThemeWrapper;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.firebase.Timestamp;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Instrumentation tests for organizer event-card binding on the first Organize page.
 */
@RunWith(AndroidJUnit4.class)
public class OrganizeEventAdapterBindingTest {

    /**
     * Verifies that a public organizer event with blank details binds the expected summary values
     * into the Organize event card, including the public/private label, registration window,
     * unlimited waiting-list copy, description fallback, and preview action text.
     */
    @Test
    public void onBindViewHolder_publicEventBindsOrganizerSummaryFields() {
        ContextThemeWrapper context = new ContextThemeWrapper(
                ApplicationProvider.getApplicationContext(),
                R.style.Theme_Breezeseas
        );
        Timestamp createdTimestamp = new Timestamp(new Date(1893286800000L));
        Timestamp modifiedTimestamp = new Timestamp(new Date(1893290400000L));
        Calendar registrationStartCalendar = Calendar.getInstance();
        registrationStartCalendar.clear();
        registrationStartCalendar.set(2030, Calendar.JANUARY, 1, 12, 0, 0);
        Calendar registrationEndCalendar = Calendar.getInstance();
        registrationEndCalendar.clear();
        registrationEndCalendar.set(2030, Calendar.JANUARY, 2, 12, 0, 0);
        Timestamp registrationStart = new Timestamp(registrationStartCalendar.getTime());
        Timestamp registrationEnd = new Timestamp(registrationEndCalendar.getTime());
        SimpleDateFormat organizerDateFormat = new SimpleDateFormat("MMM d, yyyy", Locale.US);
        Map<String, Object> eventMap = new HashMap<>();
        eventMap.put("eventId", "organize-card-event");
        eventMap.put("isPrivate", false);
        eventMap.put("organizerId", "organizer-device-id");
        eventMap.put("coOrganizerId", new ArrayList<>());
        eventMap.put("name", "Community Meetup");
        eventMap.put("description", "   ");
        eventMap.put("createdTimestamp", createdTimestamp);
        eventMap.put("modifiedTimestamp", modifiedTimestamp);
        eventMap.put("registrationStartTimestamp", registrationStart);
        eventMap.put("registrationEndTimestamp", registrationEnd);
        eventMap.put("eventStartTimestamp", null);
        eventMap.put("eventEndTimestamp", null);
        eventMap.put("geolocationEnforced", false);
        eventMap.put("eventCapacity", 80);
        eventMap.put("waitingListCapacity", -1);
        eventMap.put("drawARound", 0);
        Event event = new Event(eventMap);

        OrganizeFragment.EventAdapter adapter = new OrganizeFragment.EventAdapter(
                Collections.singletonList(event),
                selectedEvent -> { }
        );
        FrameLayout parent = new FrameLayout(context);
        OrganizeFragment.EventAdapter.VH holder = adapter.onCreateViewHolder(parent, 0);

        adapter.onBindViewHolder(holder, 0);

        assertEquals("Community Meetup", holder.tvName.getText().toString());
        assertEquals(
                context.getString(R.string.organize_event_type_public),
                holder.tvTypeChip.getText().toString()
        );
        assertEquals(
                "Reg: "
                        + organizerDateFormat.format(registrationStart.toDate())
                        + " \u2192 "
                        + organizerDateFormat.format(registrationEnd.toDate()),
                holder.tvDates.getText().toString()
        );
        assertEquals("Waiting list cap: Unlimited", holder.tvCap.getText().toString());
        assertEquals(
                context.getString(R.string.organize_event_no_description),
                holder.tvDetails.getText().toString()
        );
        assertEquals(
                context.getString(R.string.organize_event_open_preview),
                holder.tvAction.getText().toString()
        );
    }

    /**
     * Verifies that a private organizer event binds the private type label, finite waiting-list
     * capacity, and explicit description text into the Organize event card.
     */
    @Test
    public void onBindViewHolder_privateEventBindsPrivateSummaryFields() {
        ContextThemeWrapper context = new ContextThemeWrapper(
                ApplicationProvider.getApplicationContext(),
                R.style.Theme_Breezeseas
        );
        Timestamp createdTimestamp = new Timestamp(new Date(1893286800000L));
        Timestamp modifiedTimestamp = new Timestamp(new Date(1893290400000L));
        Calendar registrationStartCalendar = Calendar.getInstance();
        registrationStartCalendar.clear();
        registrationStartCalendar.set(2030, Calendar.FEBRUARY, 10, 12, 0, 0);
        Calendar registrationEndCalendar = Calendar.getInstance();
        registrationEndCalendar.clear();
        registrationEndCalendar.set(2030, Calendar.FEBRUARY, 12, 12, 0, 0);
        Timestamp registrationStart = new Timestamp(registrationStartCalendar.getTime());
        Timestamp registrationEnd = new Timestamp(registrationEndCalendar.getTime());
        SimpleDateFormat organizerDateFormat = new SimpleDateFormat("MMM d, yyyy", Locale.US);
        Map<String, Object> eventMap = new HashMap<>();
        eventMap.put("eventId", "organize-private-card-event");
        eventMap.put("isPrivate", true);
        eventMap.put("organizerId", "organizer-device-id");
        eventMap.put("coOrganizerId", new ArrayList<>());
        eventMap.put("name", "Invite-Only Showcase");
        eventMap.put("description", "Private launch event for invited guests.");
        eventMap.put("createdTimestamp", createdTimestamp);
        eventMap.put("modifiedTimestamp", modifiedTimestamp);
        eventMap.put("registrationStartTimestamp", registrationStart);
        eventMap.put("registrationEndTimestamp", registrationEnd);
        eventMap.put("eventStartTimestamp", null);
        eventMap.put("eventEndTimestamp", null);
        eventMap.put("geolocationEnforced", true);
        eventMap.put("eventCapacity", 40);
        eventMap.put("waitingListCapacity", 12);
        eventMap.put("drawARound", 0);
        Event event = new Event(eventMap);

        OrganizeFragment.EventAdapter adapter = new OrganizeFragment.EventAdapter(
                Collections.singletonList(event),
                selectedEvent -> { }
        );
        FrameLayout parent = new FrameLayout(context);
        OrganizeFragment.EventAdapter.VH holder = adapter.onCreateViewHolder(parent, 0);

        adapter.onBindViewHolder(holder, 0);

        assertEquals("Invite-Only Showcase", holder.tvName.getText().toString());
        assertEquals(
                context.getString(R.string.organize_event_type_private),
                holder.tvTypeChip.getText().toString()
        );
        assertEquals(
                "Reg: "
                        + organizerDateFormat.format(registrationStart.toDate())
                        + " \u2192 "
                        + organizerDateFormat.format(registrationEnd.toDate()),
                holder.tvDates.getText().toString()
        );
        assertEquals("Waiting list cap: 12", holder.tvCap.getText().toString());
        assertEquals("Private launch event for invited guests.", holder.tvDetails.getText().toString());
        assertEquals(
                context.getString(R.string.organize_event_open_preview),
                holder.tvAction.getText().toString()
        );
    }

    /**
     * Verifies that organizer events missing registration dates still bind a safe "Not set"
     * fallback into the Organize event card instead of showing broken or empty date text.
     */
    @Test
    public void onBindViewHolder_missingRegistrationDatesShowsNotSetFallback() {
        ContextThemeWrapper context = new ContextThemeWrapper(
                ApplicationProvider.getApplicationContext(),
                R.style.Theme_Breezeseas
        );
        Timestamp createdTimestamp = new Timestamp(new Date(1893286800000L));
        Timestamp modifiedTimestamp = new Timestamp(new Date(1893290400000L));
        Map<String, Object> eventMap = new HashMap<>();
        eventMap.put("eventId", "organize-missing-dates-event");
        eventMap.put("isPrivate", false);
        eventMap.put("organizerId", "organizer-device-id");
        eventMap.put("coOrganizerId", new ArrayList<>());
        eventMap.put("name", "Date TBD Event");
        eventMap.put("description", "Schedule still being finalized.");
        eventMap.put("createdTimestamp", createdTimestamp);
        eventMap.put("modifiedTimestamp", modifiedTimestamp);
        eventMap.put("registrationStartTimestamp", null);
        eventMap.put("registrationEndTimestamp", null);
        eventMap.put("eventStartTimestamp", null);
        eventMap.put("eventEndTimestamp", null);
        eventMap.put("geolocationEnforced", false);
        eventMap.put("eventCapacity", 15);
        eventMap.put("waitingListCapacity", 4);
        eventMap.put("drawARound", 0);
        Event event = new Event(eventMap);

        OrganizeFragment.EventAdapter adapter = new OrganizeFragment.EventAdapter(
                Collections.singletonList(event),
                selectedEvent -> { }
        );
        FrameLayout parent = new FrameLayout(context);
        OrganizeFragment.EventAdapter.VH holder = adapter.onCreateViewHolder(parent, 0);

        adapter.onBindViewHolder(holder, 0);

        assertEquals("Date TBD Event", holder.tvName.getText().toString());
        assertEquals("Reg: Not set \u2192 Not set", holder.tvDates.getText().toString());
        assertEquals("Waiting list cap: 4", holder.tvCap.getText().toString());
        assertEquals("Schedule still being finalized.", holder.tvDetails.getText().toString());
    }

    /**
     * Verifies that tapping an organizer event card dispatches the exact bound event to the
     * click listener so the preview flow opens the correct event.
     */
    @Test
    public void onBindViewHolder_clickDispatchesBoundEvent() {
        ContextThemeWrapper context = new ContextThemeWrapper(
                ApplicationProvider.getApplicationContext(),
                R.style.Theme_Breezeseas
        );
        Timestamp createdTimestamp = new Timestamp(new Date(1893286800000L));
        Timestamp modifiedTimestamp = new Timestamp(new Date(1893290400000L));
        Event firstEvent = new Event(buildEventMap(
                "event-first",
                false,
                "First Event",
                "First description",
                createdTimestamp,
                modifiedTimestamp
        ));
        Event secondEvent = new Event(buildEventMap(
                "event-second",
                true,
                "Second Event",
                "Second description",
                createdTimestamp,
                modifiedTimestamp
        ));
        Event[] clickedEvent = new Event[1];

        OrganizeFragment.EventAdapter adapter = new OrganizeFragment.EventAdapter(
                java.util.Arrays.asList(firstEvent, secondEvent),
                selectedEvent -> clickedEvent[0] = selectedEvent
        );
        FrameLayout parent = new FrameLayout(context);
        OrganizeFragment.EventAdapter.VH holder = adapter.onCreateViewHolder(parent, 0);

        adapter.onBindViewHolder(holder, 1);
        holder.itemView.performClick();

        assertEquals(secondEvent, clickedEvent[0]);
    }

    /**
     * Builds a minimal organizer event map for adapter-binding tests.
     *
     * @param eventId Event identifier used by the adapter.
     * @param isPrivate Whether the event is private.
     * @param name Display name rendered on the event card.
     * @param description Description rendered on the event card.
     * @param createdTimestamp Event creation timestamp.
     * @param modifiedTimestamp Event modification timestamp.
     * @return Firestore-shaped event data for adapter tests.
     */
    private Map<String, Object> buildEventMap(
            String eventId,
            boolean isPrivate,
            String name,
            String description,
            Timestamp createdTimestamp,
            Timestamp modifiedTimestamp
    ) {
        Map<String, Object> eventMap = new HashMap<>();
        eventMap.put("eventId", eventId);
        eventMap.put("isPrivate", isPrivate);
        eventMap.put("organizerId", "organizer-device-id");
        eventMap.put("coOrganizerId", new ArrayList<>());
        eventMap.put("name", name);
        eventMap.put("description", description);
        eventMap.put("createdTimestamp", createdTimestamp);
        eventMap.put("modifiedTimestamp", modifiedTimestamp);
        eventMap.put("registrationStartTimestamp", null);
        eventMap.put("registrationEndTimestamp", null);
        eventMap.put("eventStartTimestamp", null);
        eventMap.put("eventEndTimestamp", null);
        eventMap.put("geolocationEnforced", false);
        eventMap.put("eventCapacity", 20);
        eventMap.put("waitingListCapacity", 2);
        eventMap.put("drawARound", 0);
        return eventMap;
    }
}
