package com.example.breeze_seas;

import static org.junit.Assert.assertEquals;

import com.google.firebase.Timestamp;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Calendar;
import java.util.TimeZone;

/**
 * Unit tests for organizer-facing event date-time helper logic used by create-event and preview
 * editing flows.
 */
public class EventMetadataUtilsTest {

    private TimeZone originalTimeZone;

    /**
     * Pins the JVM default time zone so date-picker conversion assertions are deterministic.
     */
    @Before
    public void setUp() {
        originalTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("America/Edmonton"));
    }

    /**
     * Restores the JVM default time zone after each test.
     */
    @After
    public void tearDown() {
        TimeZone.setDefault(originalTimeZone);
    }

    /**
     * Verifies that the organizer date-picker round trip preserves the originally chosen local
     * calendar day and time instead of shifting the event to a different day.
     */
    @Test
    public void datePickerRoundTrip_preservesOriginalLocalDateTime() {
        Calendar original = Calendar.getInstance();
        original.clear();
        original.set(2030, Calendar.JANUARY, 15, 18, 45, 0);

        Timestamp originalTimestamp = new Timestamp(original.getTime());
        long pickerSelection = EventMetadataUtils.toDatePickerSelection(
                originalTimestamp.toDate().getTime()
        );
        long combinedMillis = EventMetadataUtils.combineUtcDateWithLocalTime(
                pickerSelection,
                original.get(Calendar.HOUR_OF_DAY),
                original.get(Calendar.MINUTE)
        );

        Calendar restored = Calendar.getInstance();
        restored.setTimeInMillis(combinedMillis);

        assertEquals(original.get(Calendar.YEAR), restored.get(Calendar.YEAR));
        assertEquals(original.get(Calendar.MONTH), restored.get(Calendar.MONTH));
        assertEquals(original.get(Calendar.DAY_OF_MONTH), restored.get(Calendar.DAY_OF_MONTH));
        assertEquals(original.get(Calendar.HOUR_OF_DAY), restored.get(Calendar.HOUR_OF_DAY));
        assertEquals(original.get(Calendar.MINUTE), restored.get(Calendar.MINUTE));
    }
}
