package com.example.breeze_seas;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.google.firebase.Timestamp;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

/**
 * CreateEventFragment collects organizer input for a new event and submits it through
 * {@link EventDB}.
 */
public class CreateEventFragment extends Fragment {

    private static final String ARG_PRIVATE_EVENT = "arg_private_event";

    private ImageView ivPoster;
    private LinearLayout posterPlaceholder;
    private View privateEventRow;

    private TextInputEditText etRegFrom, etRegTo, etEventStart, etEventEnd, etEventName, etEventDetails, etCapacity, etWaitingListCapacity;
    private SwitchMaterial swGeo;
    private SwitchMaterial swPrivate;
    private SessionViewModel viewModel;

    private Long regFromMillis = null;
    private Long regToMillis = null;
    private Long eventStartMillis = null;
    private Long eventEndMillis = null;
    private String posterBase64 = "";

    private interface DateTimeSelectionListener {
        void onSelected(long millis);
    }

    private final ActivityResultLauncher<String> pickImage =
            registerForActivityResult(new ActivityResultContracts.GetContent(), new androidx.activity.result.ActivityResultCallback<Uri>() {
                /**
                 * Stores the selected poster image and updates the poster preview.
                 *
                 * @param uri Uri selected from the system picker, or {@code null} when cancelled.
                 */
                @Override
                public void onActivityResult(Uri uri) {
                    /// No image resource
                    if (uri == null) {
                        return;
                    }
                    handleSelectedPoster(uri);
                }
            });

    /**
     * Creates the organizer event-creation fragment using the shared create-event layout.
     */
    public CreateEventFragment() {
        super(R.layout.fragment_create_event);
    }

    /**
     * Creates a preconfigured create-event fragment for the chosen visibility type.
     *
     * @param isPrivateEvent {@code true} for a private event, {@code false} for a public event.
     * @return Create-event fragment with the visibility choice bundled in its arguments.
     */
    @NonNull
    public static CreateEventFragment newInstance(boolean isPrivateEvent) {
        CreateEventFragment fragment = new CreateEventFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_PRIVATE_EVENT, isPrivateEvent);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Binds organizer form fields and wires the create-event interactions.
     *
     * @param view Inflated create-event root view.
     * @param savedInstanceState Previously saved instance state bundle, or {@code null}.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(SessionViewModel.class);

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            /**
             * Returns to the previous organizer screen when the toolbar back button is pressed.
             *
             * @param v Toolbar navigation view that was tapped.
             */
            @Override
            public void onClick(View v) {
                requireActivity().getSupportFragmentManager().popBackStack();
            }
        });

        ivPoster = view.findViewById(R.id.ivPoster);
        posterPlaceholder = view.findViewById(R.id.posterPlaceholder);
        privateEventRow = view.findViewById(R.id.privateEventRow);

        View cardPoster = view.findViewById(R.id.cardPoster);
        View btnAddImage = view.findViewById(R.id.btnAddImage);
        View btnRegPeriod = view.findViewById(R.id.btnRegPeriod);
        View btnEventPeriod = view.findViewById(R.id.btnEventPeriod);

        etRegFrom = view.findViewById(R.id.etRegFrom);
        etRegTo = view.findViewById(R.id.etRegTo);
        etEventStart = view.findViewById(R.id.etEventStart);
        etEventEnd = view.findViewById(R.id.etEventEnd);
        etEventName = view.findViewById(R.id.etEventName);
        etEventDetails = view.findViewById(R.id.etEventDetails);
        etCapacity = view.findViewById(R.id.etCapacity);
        etWaitingListCapacity = view.findViewById(R.id.etWaitingListCapacity);
        swGeo = view.findViewById(R.id.swGeo);
        swPrivate = view.findViewById(R.id.swPrivate);

        Bundle args = getArguments();
        if (args != null && args.containsKey(ARG_PRIVATE_EVENT) && swPrivate != null) {
            swPrivate.setChecked(args.getBoolean(ARG_PRIVATE_EVENT));
            if (privateEventRow != null) {
                privateEventRow.setVisibility(View.GONE);
            }
        }

        View.OnClickListener pickPoster = new View.OnClickListener() {
            /**
             * Opens the poster image picker for the organizer.
             *
             * @param v Poster-related view that was tapped.
             */
            @Override
            public void onClick(View v) {
                pickImage.launch("image/*");
            }
        };
        cardPoster.setOnClickListener(pickPoster);
        btnAddImage.setOnClickListener(pickPoster);

        btnRegPeriod.setOnClickListener(v -> openRegistrationStartPicker());
        etRegFrom.setOnClickListener(v -> openRegistrationStartPicker());
        etRegTo.setOnClickListener(v -> openRegistrationEndPicker());

        btnEventPeriod.setOnClickListener(v -> openEventStartPicker());
        etEventStart.setOnClickListener(v -> openEventStartPicker());
        etEventEnd.setOnClickListener(v -> openEventEndPicker());

        view.findViewById(R.id.btnCreate).setOnClickListener(new View.OnClickListener() {
            /**
             * Validates the organizer input and starts event creation.
             *
             * @param v Create button that was tapped.
             */
            @Override
            public void onClick(View v) {
                onCreateClicked();
            }
        });
    }

    /**
     * Handles a poster chosen from the system picker by compressing it into Base64 and
     * updating the on-screen preview.
     *
     * @param uri Selected poster image Uri.
     */
    private void handleSelectedPoster(Uri uri) {
        try {
            posterBase64 = ImageUtils.uriToCompressedBase64(requireContext(), uri);

            Bitmap previewBitmap = ImageUtils.base64ToBitmap(posterBase64);
            if (previewBitmap != null) {
                ivPoster.setImageBitmap(previewBitmap);
                ivPoster.setVisibility(View.VISIBLE);
                posterPlaceholder.setVisibility(View.GONE);
            } else {
                posterBase64 = "";
                Toast.makeText(requireContext(), "Failed to preview image", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            posterBase64 = "";
            Toast.makeText(requireContext(), "Failed to process image", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Opens the registration-start date-time picker.
     */
    private void openRegistrationStartPicker() {
        openDateTimePicker(
                "create_reg_start",
                getString(R.string.create_event_registration_start_picker_title),
                regFromMillis,
                millis -> {
                    regFromMillis = millis;
                    etRegFrom.setText(EventMetadataUtils.formatDateTime(millis));
                }
        );
    }

    /**
     * Opens the registration-end date-time picker.
     */
    private void openRegistrationEndPicker() {
        openDateTimePicker(
                "create_reg_end",
                getString(R.string.create_event_registration_end_picker_title),
                regToMillis,
                millis -> {
                    regToMillis = millis;
                    etRegTo.setText(EventMetadataUtils.formatDateTime(millis));
                }
        );
    }

    /**
     * Opens the event-start date-time picker.
     */
    private void openEventStartPicker() {
        openDateTimePicker(
                "create_event_start",
                getString(R.string.create_event_schedule_start_picker_title),
                eventStartMillis,
                millis -> {
                    eventStartMillis = millis;
                    etEventStart.setText(EventMetadataUtils.formatDateTime(millis));
                }
        );
    }

    /**
     * Opens the event-end date-time picker.
     */
    private void openEventEndPicker() {
        openDateTimePicker(
                "create_event_end",
                getString(R.string.create_event_schedule_end_picker_title),
                eventEndMillis,
                millis -> {
                    eventEndMillis = millis;
                    etEventEnd.setText(EventMetadataUtils.formatDateTime(millis));
                }
        );
    }

    /**
     * Opens a Material date picker followed by a time picker for one date-time field.
     *
     * @param tagPrefix Fragment-manager tag prefix for the picker dialogs.
     * @param title Title shown on the picker surfaces.
     * @param currentMillis Existing millis to seed the pickers with.
     * @param listener Callback that receives the combined local timestamp.
     */
    private void openDateTimePicker(
            @NonNull String tagPrefix,
            @NonNull String title,
            @Nullable Long currentMillis,
            @NonNull DateTimeSelectionListener listener
    ) {
        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTheme(R.style.ThemeOverlay_Breezeseas_DateRangePicker)
                .setTitleText(title)
                .setSelection(EventMetadataUtils.toDatePickerSelection(currentMillis))
                .build();

        datePicker.addOnPositiveButtonClickListener(selection -> {
            if (selection == null) {
                return;
            }
            openTimePicker(tagPrefix, title, currentMillis, selection, listener);
        });

        datePicker.show(getParentFragmentManager(), tagPrefix + "_date");
    }

    /**
     * Opens the time portion of the date-time picker flow.
     *
     * @param tagPrefix Fragment-manager tag prefix for the picker dialogs.
     * @param title Title shown on the picker surface.
     * @param currentMillis Existing millis to seed the picker with.
     * @param selectedUtcDateMillis Date chosen from MaterialDatePicker.
     * @param listener Callback that receives the combined local timestamp.
     */
    private void openTimePicker(
            @NonNull String tagPrefix,
            @NonNull String title,
            @Nullable Long currentMillis,
            long selectedUtcDateMillis,
            @NonNull DateTimeSelectionListener listener
    ) {
        Calendar calendar = Calendar.getInstance();
        if (currentMillis != null) {
            calendar.setTimeInMillis(currentMillis);
        }

        MaterialTimePicker timePicker = new MaterialTimePicker.Builder()
                .setTitleText(title)
                .setTimeFormat(DateFormat.is24HourFormat(requireContext())
                        ? TimeFormat.CLOCK_24H
                        : TimeFormat.CLOCK_12H)
                .setHour(calendar.get(Calendar.HOUR_OF_DAY))
                .setMinute(calendar.get(Calendar.MINUTE))
                .build();

        timePicker.addOnPositiveButtonClickListener(v -> listener.onSelected(
                EventMetadataUtils.combineUtcDateWithLocalTime(
                        selectedUtcDateMillis,
                        timePicker.getHour(),
                        timePicker.getMinute()
                )
        ));

        timePicker.show(getParentFragmentManager(), tagPrefix + "_time");
    }

    /**
     * Validates organizer input, builds a new {@link Event}, and submits it to {@link EventDB}.
     */
    private void onCreateClicked() {
        String name = etEventName.getText() == null ? "" : etEventName.getText().toString().trim();
        String details = etEventDetails.getText() == null ? "" : etEventDetails.getText().toString().trim();
        String capText = etCapacity.getText() == null ? "" : etCapacity.getText().toString().trim();
        String waitingListText = etWaitingListCapacity.getText() == null ? "" : etWaitingListCapacity.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            etEventName.setError("Required");
            return;
        }

        if (regFromMillis == null || regToMillis == null) {
            Toast.makeText(requireContext(), R.string.create_event_set_registration_period, Toast.LENGTH_SHORT).show();
            return;
        }

        if (eventStartMillis == null || eventEndMillis == null) {
            Toast.makeText(requireContext(), R.string.create_event_set_schedule, Toast.LENGTH_SHORT).show();
            return;
        }

        if (regFromMillis >= regToMillis) {
            Toast.makeText(requireContext(), R.string.create_event_registration_order_error, Toast.LENGTH_SHORT).show();
            return;
        }

        if (eventStartMillis > eventEndMillis) {
            Toast.makeText(requireContext(), R.string.create_event_schedule_order_error, Toast.LENGTH_SHORT).show();
            return;
        }

        if (regToMillis > eventStartMillis) {
            Toast.makeText(requireContext(), R.string.create_event_schedule_conflict_error, Toast.LENGTH_SHORT).show();
            return;
        }

        String organizerId = viewModel == null ? null : viewModel.getAndroidID().getValue();
        if (organizerId == null || organizerId.trim().isEmpty()) {
            Toast.makeText(requireContext(), "Unable to resolve organizer identity", Toast.LENGTH_SHORT).show();
            return;
        }

        Integer eventCap = null;
        if (TextUtils.isEmpty(capText)) {
            etCapacity.setError("Number required");
            return;
        }
        if (!TextUtils.isEmpty(capText)) {
            try {
                eventCap = Integer.parseInt(capText);
            } catch (NumberFormatException e) {
                etCapacity.setError("Enter a valid number");
                return;
            }
        }

        Integer eventWaitingListCap = null;
        if (!TextUtils.isEmpty(waitingListText)) {
            try {
                eventWaitingListCap = Integer.parseInt(waitingListText);
            } catch (NumberFormatException e) {
                etWaitingListCapacity.setError("Enter a valid number");
                return;
            }
        }

        int normalizedCapacity = eventCap == null ? -1 : eventCap;
        int normalizedEventWaitingListCapacity = eventWaitingListCap == null ? -1 : eventWaitingListCap;
        boolean isPrivateEvent = swPrivate != null && swPrivate.isChecked();
        // Create and upload image
        // Only create new image object if base64 has content, in otherwords, an image was selected.
        final Image[] newImage = new Image[1];
        if (posterBase64.isEmpty()) {
            newImage[0] = null;
        } else {
            newImage[0] = new Image(posterBase64);

            // Upload to database
            ImageDB.saveImage(newImage[0], new ImageDB.ImageMutationCallback() {
                @Override
                public void onSuccess() {
                }

                @Override
                public void onFailure(Exception e) {
                    Log.e("Image DB", "Unable to upload image.", e);
                    newImage[0] = null;
                }
            });
        }

        // Create event object
        Event event = new Event(
                isPrivateEvent,
                organizerId,
                name,
                details,
                newImage[0],
                new Timestamp(new Date(regFromMillis)),
                new Timestamp(new Date(regToMillis)),
                new Timestamp(new Date(eventStartMillis)),
                new Timestamp(new Date(eventEndMillis)),
                swGeo.isChecked(),
                normalizedCapacity,
                normalizedEventWaitingListCapacity
        );

        EventDB.addEvent(event, new EventDB.AddEventCallback() {
            /**
             * Opens the QR screen for the newly created event.
             *
             * @param eventId Identifier assigned to the newly created event.
             */
            @Override
            public void onSuccess(String eventId) {
                Toast.makeText(
                        requireContext(),
                        isPrivateEvent ? R.string.create_event_private_success : R.string.create_event_public_success,
                        Toast.LENGTH_SHORT
                ).show();

                if (viewModel != null) {
                    viewModel.setEventShown(event);
                }

                if (isPrivateEvent) {
                    requireActivity().getSupportFragmentManager().popBackStack();
                    return;
                }

                Bundle args = new Bundle();
                args.putString("eventId", eventId);
                args.putBoolean("isCreated", true);

                ViewQrCodeFragment fragment = new ViewQrCodeFragment();
                fragment.setArguments(args);

                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, fragment)
                        .commit();
            }

            /**
             * Reports a user-visible error if event creation fails.
             *
             * @param e Failure returned by the create-event request.
             */
            @Override
            public void onFailure(Exception e) {
                Toast.makeText(requireContext(), "Failed to create event", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
