package com.example.breeze_seas;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.datepicker.MaterialDatePicker;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Lets entrants refine the Explore list by availability dates and event capacity.
 */
public class FilterFragment extends Fragment {

    @Nullable
    private EventHandler exploreEventHandler;
    @Nullable
    private Long selectedAvailabilityStartMillis;
    @Nullable
    private Long selectedAvailabilityEndMillis;

    private TextView availabilitySummaryView;
    private TextView availabilityStartValueView;
    private TextView availabilityEndValueView;
    private EditText minCapacityInput;
    private EditText maxCapacityInput;

    /**
     * Creates the filter screen used by Explore.
     */
    public FilterFragment() {
        super(R.layout.fragment_filter);
    }

    /**
     * Binds the shared Explore event handler and the filter UI.
     * @param view Inflated root view for the screen.
     * @param savedInstanceState Previously saved state, or {@code null}.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ExploreViewModel exploreViewModel = new ViewModelProvider(requireActivity()).get(ExploreViewModel.class);
        if (!exploreViewModel.eventHandlerIsInitialized()) {
            Toast.makeText(requireContext(), R.string.filter_unavailable, Toast.LENGTH_SHORT).show();
            getParentFragmentManager().popBackStack();
            return;
        }

        exploreEventHandler = exploreViewModel.getEventHandler();
        selectedAvailabilityStartMillis = exploreEventHandler.getAvailabilityStartMillis();
        selectedAvailabilityEndMillis = exploreEventHandler.getAvailabilityEndMillis();

        bindViews(view);
        populateCurrentFilters();

        view.findViewById(R.id.filter_back_button).setOnClickListener(new View.OnClickListener() {
            /**
             * Returns to Explore without changing the current filter state.
             * @param v Back button view.
             */
            @Override
            public void onClick(View v) {
                getParentFragmentManager().popBackStack();
            }
        });

        view.findViewById(R.id.filter_availability_button).setOnClickListener(new View.OnClickListener() {
            /**
             * Opens the date-range picker for the availability filter.
             * @param v Availability picker button.
             */
            @Override
            public void onClick(View v) {
                openAvailabilityPicker();
            }
        });

        view.findViewById(R.id.filter_clear_dates_button).setOnClickListener(new View.OnClickListener() {
            /**
             * Clears the local availability selection while staying on this screen.
             * @param v Clear-dates action view.
             */
            @Override
            public void onClick(View v) {
                selectedAvailabilityStartMillis = null;
                selectedAvailabilityEndMillis = null;
                updateAvailabilityViews();
            }
        });

        view.findViewById(R.id.filter_clear_all_button).setOnClickListener(new View.OnClickListener() {
            /**
             * Resets every filter and immediately applies the cleared state to Explore.
             * @param v Clear-all button.
             */
            @Override
            public void onClick(View v) {
                clearAllFilters();
            }
        });

        view.findViewById(R.id.filter_apply_button).setOnClickListener(new View.OnClickListener() {
            /**
             * Validates the current inputs and applies them to Explore.
             * @param v Apply button.
             */
            @Override
            public void onClick(View v) {
                applyFilters();
            }
        });
    }

    /**
     * Binds the views used by the filter screen.
     * @param view Inflated root view for this fragment.
     */
    private void bindViews(@NonNull View view) {
        availabilitySummaryView = view.findViewById(R.id.filter_availability_summary);
        availabilityStartValueView = view.findViewById(R.id.filter_start_value);
        availabilityEndValueView = view.findViewById(R.id.filter_end_value);
        minCapacityInput = view.findViewById(R.id.filter_min_capacity_input);
        maxCapacityInput = view.findViewById(R.id.filter_max_capacity_input);
    }

    /**
     * Seeds the UI from the currently active Explore filters.
     */
    private void populateCurrentFilters() {
        if (exploreEventHandler == null) {
            return;
        }

        Integer minCapacity = exploreEventHandler.getMinCapacityFilter();
        Integer maxCapacity = exploreEventHandler.getMaxCapacityFilter();

        minCapacityInput.setText(minCapacity == null ? "" : String.valueOf(minCapacity));
        maxCapacityInput.setText(maxCapacity == null ? "" : String.valueOf(maxCapacity));
        updateAvailabilityViews();
    }

    /**
     * Opens the calendar used to choose the entrant's available dates.
     */
    private void openAvailabilityPicker() {
        MaterialDatePicker.Builder<Pair<Long, Long>> builder = MaterialDatePicker.Builder.dateRangePicker()
                .setTheme(R.style.ThemeOverlay_Breezeseas_DateRangePicker)
                .setTitleText(R.string.filter_date_picker_title);

        if (selectedAvailabilityStartMillis != null && selectedAvailabilityEndMillis != null) {
            builder.setSelection(new Pair<>(
                    toPickerSelection(selectedAvailabilityStartMillis),
                    toPickerSelection(selectedAvailabilityEndMillis)));
        }

        MaterialDatePicker<Pair<Long, Long>> picker = builder.build();
        picker.addOnPositiveButtonClickListener(selection -> {
            if (selection == null || selection.first == null || selection.second == null) {
                return;
            }

            selectedAvailabilityStartMillis = pickerSelectionToLocalStartOfDay(selection.first);
            selectedAvailabilityEndMillis = pickerSelectionToLocalEndOfDay(selection.second);
            updateAvailabilityViews();
        });
        picker.show(getParentFragmentManager(), "availability_range");
    }

    /**
     * Refreshes the visible availability summary labels.
     */
    private void updateAvailabilityViews() {
        boolean hasDates = selectedAvailabilityStartMillis != null && selectedAvailabilityEndMillis != null;

        availabilitySummaryView.setText(hasDates
                ? getString(R.string.filter_availability_summary_selected)
                : getString(R.string.filter_availability_summary_any));
        availabilityStartValueView.setText(formatDateLabel(selectedAvailabilityStartMillis));
        availabilityEndValueView.setText(formatDateLabel(selectedAvailabilityEndMillis));

        View clearDatesButton = requireView().findViewById(R.id.filter_clear_dates_button);
        clearDatesButton.setVisibility(hasDates ? View.VISIBLE : View.GONE);
    }

    /**
     * Resets every filter and returns to Explore with the cleared state applied.
     */
    private void clearAllFilters() {
        minCapacityInput.setError(null);
        maxCapacityInput.setError(null);
        minCapacityInput.setText("");
        maxCapacityInput.setText("");
        selectedAvailabilityStartMillis = null;
        selectedAvailabilityEndMillis = null;

        if (exploreEventHandler != null) {
            exploreEventHandler.clearAdvancedFilters();
        }
        getParentFragmentManager().popBackStack();
    }

    /**
     * Validates the visible inputs and applies them to the shared Explore event handler.
     */
    private void applyFilters() {
        minCapacityInput.setError(null);
        maxCapacityInput.setError(null);

        Integer minCapacity = parseCapacityInput(minCapacityInput);
        Integer maxCapacity = parseCapacityInput(maxCapacityInput);

        if (minCapacity == null && !TextUtils.isEmpty(minCapacityInput.getText())) {
            minCapacityInput.setError(getString(R.string.filter_capacity_invalid));
            return;
        }
        if (maxCapacity == null && !TextUtils.isEmpty(maxCapacityInput.getText())) {
            maxCapacityInput.setError(getString(R.string.filter_capacity_invalid));
            return;
        }
        if (minCapacity != null && minCapacity < 0) {
            minCapacityInput.setError(getString(R.string.filter_capacity_non_negative));
            return;
        }
        if (maxCapacity != null && maxCapacity < 0) {
            maxCapacityInput.setError(getString(R.string.filter_capacity_non_negative));
            return;
        }
        if (minCapacity != null && maxCapacity != null && minCapacity > maxCapacity) {
            minCapacityInput.setError(getString(R.string.filter_capacity_range_error));
            maxCapacityInput.setError(getString(R.string.filter_capacity_range_error));
            return;
        }

        if (exploreEventHandler != null) {
            exploreEventHandler.setAdvancedFilters(
                    selectedAvailabilityStartMillis,
                    selectedAvailabilityEndMillis,
                    minCapacity,
                    maxCapacity);
        }
        getParentFragmentManager().popBackStack();
    }

    /**
     * Parses an optional integer from the given capacity field.
     * @param input Input field to parse.
     * @return Parsed integer, or {@code null} if the field is empty or invalid.
     */
    @Nullable
    private Integer parseCapacityInput(@NonNull EditText input) {
        String value = input.getText() == null ? "" : input.getText().toString().trim();
        if (value.isEmpty()) {
            return null;
        }

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Formats a selected date for the mini summary cards.
     * @param millis Local epoch milliseconds, or {@code null}.
     * @return Human-readable date label.
     */
    @NonNull
    private String formatDateLabel(@Nullable Long millis) {
        if (millis == null) {
            return getString(R.string.filter_any_date);
        }

        SimpleDateFormat sdf = new SimpleDateFormat("EEE, MMM d", Locale.US);
        return sdf.format(millis);
    }

    /**
     * Converts a locally-stored date into the UTC midnight value expected by MaterialDatePicker.
     * @param localMillis Local epoch milliseconds.
     * @return UTC midnight selection value for the picker.
     */
    private long toPickerSelection(long localMillis) {
        Calendar localCalendar = Calendar.getInstance();
        localCalendar.setTimeInMillis(localMillis);

        Calendar utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.US);
        utcCalendar.clear();
        utcCalendar.set(
                localCalendar.get(Calendar.YEAR),
                localCalendar.get(Calendar.MONTH),
                localCalendar.get(Calendar.DAY_OF_MONTH),
                0,
                0,
                0
        );
        return utcCalendar.getTimeInMillis();
    }

    /**
     * Converts a MaterialDatePicker UTC selection into the local start of the chosen day.
     * @param pickerSelectionUtcMillis UTC midnight selection value.
     * @return Local start-of-day epoch milliseconds.
     */
    private long pickerSelectionToLocalStartOfDay(long pickerSelectionUtcMillis) {
        Calendar utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.US);
        utcCalendar.setTimeInMillis(pickerSelectionUtcMillis);

        Calendar localCalendar = Calendar.getInstance();
        localCalendar.clear();
        localCalendar.set(
                utcCalendar.get(Calendar.YEAR),
                utcCalendar.get(Calendar.MONTH),
                utcCalendar.get(Calendar.DAY_OF_MONTH),
                0,
                0,
                0
        );
        localCalendar.set(Calendar.MILLISECOND, 0);
        return localCalendar.getTimeInMillis();
    }

    /**
     * Converts a MaterialDatePicker UTC selection into the local end of the chosen day.
     * @param pickerSelectionUtcMillis UTC midnight selection value.
     * @return Local end-of-day epoch milliseconds.
     */
    private long pickerSelectionToLocalEndOfDay(long pickerSelectionUtcMillis) {
        Calendar utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.US);
        utcCalendar.setTimeInMillis(pickerSelectionUtcMillis);

        Calendar localCalendar = Calendar.getInstance();
        localCalendar.clear();
        localCalendar.set(
                utcCalendar.get(Calendar.YEAR),
                utcCalendar.get(Calendar.MONTH),
                utcCalendar.get(Calendar.DAY_OF_MONTH),
                23,
                59,
                59
        );
        localCalendar.set(Calendar.MILLISECOND, 999);
        return localCalendar.getTimeInMillis();
    }
}
