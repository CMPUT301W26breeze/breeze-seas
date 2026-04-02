package com.example.breeze_seas;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Shared helpers for formatting and converting event date-time metadata.
 */
public final class EventMetadataUtils {

    private EventMetadataUtils() {
    }

    /**
     * Formats a Firestore timestamp for organizer and entrant-facing date-time displays.
     *
     * @param timestamp Timestamp to format.
     * @return Display-ready date-time string, or {@code "Not set"} when absent.
     */
    @NonNull
    public static String formatDateTime(@Nullable Timestamp timestamp) {
        return timestamp == null ? "Not set" : formatDateTime(timestamp.toDate().getTime());
    }

    /**
     * Formats epoch milliseconds for organizer and entrant-facing date-time displays.
     *
     * @param millis Epoch milliseconds to format.
     * @return Display-ready date-time string, or {@code "Not set"} when absent.
     */
    @NonNull
    public static String formatDateTime(@Nullable Long millis) {
        if (millis == null) {
            return "Not set";
        }
        return new SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.US).format(new Date(millis));
    }

    /**
     * Converts a locally stored timestamp into the UTC midnight value expected by MaterialDatePicker.
     *
     * @param currentMillis Existing local timestamp, or {@code null}.
     * @return UTC midnight millis for the selected date.
     */
    public static long toDatePickerSelection(@Nullable Long currentMillis) {
        Calendar localCalendar = Calendar.getInstance();
        if (currentMillis != null) {
            localCalendar.setTimeInMillis(currentMillis);
        }

        Calendar utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utcCalendar.clear();
        utcCalendar.set(
                localCalendar.get(Calendar.YEAR),
                localCalendar.get(Calendar.MONTH),
                localCalendar.get(Calendar.DAY_OF_MONTH)
        );
        return utcCalendar.getTimeInMillis();
    }

    /**
     * Combines a MaterialDatePicker UTC date selection with a local time selection.
     *
     * @param utcDateMillis UTC midnight millis returned by MaterialDatePicker.
     * @param hourOfDay Selected local hour.
     * @param minute Selected local minute.
     * @return Local timestamp that preserves the chosen calendar date and time.
     */
    public static long combineUtcDateWithLocalTime(long utcDateMillis, int hourOfDay, int minute) {
        Calendar utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utcCalendar.setTimeInMillis(utcDateMillis);

        Calendar localCalendar = Calendar.getInstance();
        localCalendar.clear();
        localCalendar.set(
                utcCalendar.get(Calendar.YEAR),
                utcCalendar.get(Calendar.MONTH),
                utcCalendar.get(Calendar.DAY_OF_MONTH),
                hourOfDay,
                minute,
                0
        );
        localCalendar.set(Calendar.MILLISECOND, 0);
        return localCalendar.getTimeInMillis();
    }
}
