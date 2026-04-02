package com.example.breeze_seas;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.google.firebase.Timestamp;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

/**
 * OrganizerEventPreviewFragment displays one organizer-owned event and provides organizer actions
 * such as editing event metadata, managing entrants, and opening the announcement flow.
 */
public class OrganizerEventPreviewFragment extends Fragment {
    private SessionViewModel viewModel;
    private OrganizeViewModel organizeViewModel;
    private Event currentEvent;
    private boolean deleteInProgress = false;

    private ImageView posterImageView;
    private TextInputEditText nameInput;
    private TextInputEditText regFromInput;
    private TextInputEditText regToInput;
    private TextInputEditText eventStartInput;
    private TextInputEditText eventEndInput;
    private TextInputEditText capacityInput;
    private TextInputEditText waitingListCapacityInput;
    private TextInputEditText detailsInput;
    private SwitchMaterial geoSwitch;

    private Timestamp regStartDate;
    private Timestamp regEndDate;
    private Timestamp eventStartDate;
    private Timestamp eventEndDate;
    private Image poster;
    private Image newPoster;
    private String posterBase64 = "";
    private EventCommentsSectionController commentsSectionController;

    private interface DateTimeSelectionListener {
        void onSelected(@NonNull Timestamp timestamp);
    }

    private final ActivityResultLauncher<String> pickImage =
            registerForActivityResult(new ActivityResultContracts.GetContent(), new androidx.activity.result.ActivityResultCallback<Uri>() {
                /**
                 * Stores the selected poster image and updates the preview if a result was picked.
                 *
                 * @param uri Uri returned by the system picker, or {@code null} when cancelled.
                 */
                @Override
                public void onActivityResult(Uri uri) {
                    if (uri == null) {
                        return;
                    }
                    handleSelectedPoster(uri);
                }
            });

    /**
     * Creates the organizer event preview fragment using the shared organizer detail layout.
     */
    public OrganizerEventPreviewFragment() {
        super(R.layout.fragment_organizer_event_preview);
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Setup viewModels
        viewModel = new ViewModelProvider(requireActivity()).get(SessionViewModel.class);
        organizeViewModel = new ViewModelProvider(requireActivity()).get(OrganizeViewModel.class);

        // Grab current event
        currentEvent = organizeViewModel.getEventHandler().getEventShown().getValue();
        assert currentEvent != null;

        // Start listeners for participants
        currentEvent.startListenAllLists(new StatusList.ListUpdateListener() {
            @Override
            public void onUpdate() {
                if (deleteInProgress) {
                    return;
                }
                populateFields(currentEvent);
            }

            @Override
            public void onError(Exception e) {
                if (deleteInProgress) {
                    return;
                }
                Log.e("Realtime DB", "Error in listener", e);

                // If list classes listener breaks, other organizer functionalities break.
                // Thus, the best course of action is to leave the page.
                // Unassign eventShown
                organizeViewModel.getEventHandler().setEventShown(null);

                // Return to explore fragment
                requireActivity().getSupportFragmentManager().popBackStack();
            }
        });
    }

    @Override
    public void onStop() {
        super.onStop();
        // TODO: Stopping all lists will break other organizer functionalities so
        // TODO: This is commented out for now
        // TODO: If left unchecked, may lead to memory leaks.
        //currentEvent.stopListenAllLists();
    }

    /**
     * Binds organizer preview views, wires actions, and starts loading the selected event.
     *
     * @param view Inflated organizer-preview root view.
     * @param savedInstanceState Previously saved instance state, or {@code null}.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bindViews(view);
        // Setup observer on eventShown
        organizeViewModel.getEventHandler()
                .getEventShown().observe(getViewLifecycleOwner(), e -> {
                    // Check if event still exists
                    if (e == null || e.getEventId() == null || e.getEventId().trim().isEmpty()) {
                        if (deleteInProgress) {
                            if (isAdded()) {
                                requireActivity().getSupportFragmentManager().popBackStack();
                            }
                            return;
                        }

                        // This happens when the EventHandler detects the event was deleted
                        // either by the organizer themselves or by other organizers or admin or database authorities
                        if (isAdded()) {
                            Toast.makeText(requireContext(), "Event deleted.", Toast.LENGTH_SHORT).show();
                        }

                        // Return to organize fragment
                        if (isAdded()) {
                            requireActivity().getSupportFragmentManager().popBackStack();
                        }
                        return;
                    }

                    // Since reference to the object is the same, the details should be updated automatically.
                    // Populate views with values.
                    currentEvent = e;
                    populateFields(currentEvent);
                });

        // Setup observer for image
        currentEvent.getImageData().observe(getViewLifecycleOwner(), image -> {
            // Just need to update image
            // Since the triggers for this observer will update the image object within event beforehand
            // We should be able to just refresh handlePoster using the same object reference.
            bindPoster(currentEvent.getImage());
        });

        final View actionMenu = view.findViewById(R.id.organizer_event_preview_action_menu);

        view.findViewById(R.id.organizer_event_preview_back).setOnClickListener(new View.OnClickListener() {
            /**
             * Returns to the previous organizer screen.
             *
             * @param v Back button view that was tapped.
             */
            @Override
            public void onClick(View v) {
                requireActivity().getSupportFragmentManager().popBackStack();
            }
        });
        view.findViewById(R.id.organizer_event_preview_menu_button).setOnClickListener(new View.OnClickListener() {
            /**
             * Toggles the visibility of the compact organizer actions menu.
             *
             * @param v Menu button that was tapped.
             */
            @Override
            public void onClick(View v) {
                actionMenu.setVisibility(actionMenu.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
            }
        });

        View.OnClickListener posterClickListener = new View.OnClickListener() {
            /**
             * Opens the system image picker so the organizer can choose a poster image.
             *
             * @param v Poster-related view that was tapped.
             */
            @Override
            public void onClick(View v) {
                pickImage.launch("image/*");
            }
        };
        view.findViewById(R.id.organizer_event_preview_poster_card).setOnClickListener(posterClickListener);
        view.findViewById(R.id.organizer_event_preview_update_poster_button).setOnClickListener(posterClickListener);

        View.OnClickListener dateClickListener = new View.OnClickListener() {
            /**
             * Opens the registration-range picker for the organizer.
             *
             * @param v Date-related view that was tapped.
             */
            @Override
            public void onClick(View v) {
                openRegistrationStartPicker();
            }
        };
        view.findViewById(R.id.organizer_event_preview_reg_period_button).setOnClickListener(dateClickListener);
        regFromInput.setOnClickListener(dateClickListener);
        regToInput.setOnClickListener(v -> openRegistrationEndPicker());

        View.OnClickListener eventScheduleClickListener = new View.OnClickListener() {
            /**
             * Opens the event schedule picker for the organizer.
             *
             * @param v Schedule-related view that was tapped.
             */
            @Override
            public void onClick(View v) {
                openEventStartPicker();
            }
        };
        view.findViewById(R.id.organizer_event_preview_event_period_button).setOnClickListener(eventScheduleClickListener);
        eventStartInput.setOnClickListener(eventScheduleClickListener);
        eventEndInput.setOnClickListener(v -> openEventEndPicker());

        view.findViewById(R.id.organizer_event_preview_save_button).setOnClickListener(new View.OnClickListener() {
            /**
             * Starts the organizer save flow for the current event.
             *
             * @param v Save button that was tapped.
             */
            @Override
            public void onClick(View v) {
                saveChanges();
            }
        });
        view.findViewById(R.id.organizer_event_preview_delete_button).setOnClickListener(new View.OnClickListener() {
            /**
             * Opens a confirmation dialog before deleting the current event.
             *
             * @param v Delete button that was tapped.
             */
            @Override
            public void onClick(View v) {
                confirmDelete();
            }
        });
        view.findViewById(R.id.organizer_event_preview_manage_button).setOnClickListener(new View.OnClickListener() {
            /**
             * Opens the manage-entrants flow for the current event.
             *
             * @param v Manage button that was tapped.
             */
            @Override
            public void onClick(View v) {
                actionMenu.setVisibility(View.GONE);
                openManageEntrantsFragment();
            }
        });
        view.findViewById(R.id.organizer_event_preview_announcement_button).setOnClickListener(new View.OnClickListener() {
            /**
             * Opens the organizer announcement flow for the current event.
             *
             * @param v Announcement button that was tapped.
             */
            @Override
            public void onClick(View v) {
                actionMenu.setVisibility(View.GONE);
                openAnnouncementFragment();
            }
        });
        view.findViewById(R.id.organizer_event_preview_map_button).setOnClickListener(new View.OnClickListener(){
            /**
             * Opens the map to view where entrants are joining from
             *
             * @param v View Map button that was tapped
             */
            @Override
            public void onClick(View v){
                actionMenu.setVisibility(View.GONE);
                openMapFragment();
            }
        });

        view.findViewById(R.id.organizer_event_preview_coorg_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                actionMenu.setVisibility(View.GONE); // Close the menu
                openCoOrganizerFragment();           // Open the new screen
            }
        });
        view.findViewById(R.id.organizer_event_preview_invite_button).setOnClickListener(new View.OnClickListener() {
            /**
             * Opens the private-event invite picker for the current event.
             *
             * @param v Invite button that was tapped.
             */
            @Override
            public void onClick(View v) {
                actionMenu.setVisibility(View.GONE);
                openPrivateEventInviteFragment();
            }
        });

        commentsSectionController = new EventCommentsSectionController(this, view);
    }

    /**
     * Releases view-scoped resources when the organizer preview view is destroyed.
     */
    @Override
    public void onDestroyView() {
        if (commentsSectionController != null) {
            commentsSectionController.release();
            commentsSectionController = null;
        }
        super.onDestroyView();
    }

    /**
     * Binds view references used by the organizer preview screen.
     *
     * @param view Inflated organizer-preview root view.
     */
    private void bindViews(@NonNull View view) {
        posterImageView = view.findViewById(R.id.organizer_event_preview_poster);
        nameInput = view.findViewById(R.id.organizer_event_preview_name_input);
        regFromInput = view.findViewById(R.id.organizer_event_preview_reg_from_input);
        regToInput = view.findViewById(R.id.organizer_event_preview_reg_to_input);
        eventStartInput = view.findViewById(R.id.organizer_event_preview_event_start_input);
        eventEndInput = view.findViewById(R.id.organizer_event_preview_event_end_input);
        capacityInput = view.findViewById(R.id.organizer_event_preview_capacity_input);
        waitingListCapacityInput = view.findViewById(R.id.organizer_event_preview_waiting_list_capacity_input);
        detailsInput = view.findViewById(R.id.organizer_event_preview_details_input);
        geoSwitch = view.findViewById(R.id.organizer_event_preview_geo_switch);
    }

    /**
     * Copies event data into the organizer preview form fields.
     *
     * @param event Loaded event whose data should be displayed.
     */
    private void populateFields(@NonNull Event event) {
        View root = getView();
        if (root == null) {
            return;
        }

        regStartDate = event.getRegistrationStartTimestamp();
        regEndDate = event.getRegistrationEndTimestamp();
        eventStartDate = event.getEventStartTimestamp();
        eventEndDate = event.getEventEndTimestamp();
        poster = event.getImage();
        // Set newPoster to also the same object
        newPoster = poster;

        ((android.widget.TextView) root.findViewById(R.id.organizer_event_preview_title)).setText(event.getName());
        ((android.widget.TextView) root.findViewById(R.id.organizer_event_preview_subtitle))
                .setText(R.string.organizer_event_preview_subtitle_text);
        View privateInviteButton = root.findViewById(R.id.organizer_event_preview_invite_button);
        if (privateInviteButton != null) {
            privateInviteButton.setVisibility(event.isPrivate() ? View.VISIBLE : View.GONE);
        }

        nameInput.setText(event.getName());
        regFromInput.setText(EventMetadataUtils.formatDateTime(regStartDate));
        regToInput.setText(EventMetadataUtils.formatDateTime(regEndDate));
        eventStartInput.setText(EventMetadataUtils.formatDateTime(eventStartDate));
        eventEndInput.setText(EventMetadataUtils.formatDateTime(eventEndDate));
        capacityInput.setText(event.getEventCapacity() < 0 ? "" : String.valueOf(event.getEventCapacity()));
        waitingListCapacityInput.setText(event.getWaitingListCapacity() < 0 ? "" : String.valueOf(event.getWaitingListCapacity()));
        detailsInput.setText(event.getDescription());
        geoSwitch.setChecked(event.isGeolocationEnforced());

        bindPoster(poster);
        if (commentsSectionController != null) {
            commentsSectionController.bind(
                    event,
                    viewModel == null ? null : viewModel.getUser().getValue(),
                    true
            );
        }
    }

    private void handleSelectedPoster(@NonNull Uri uri) {
        try {
            posterBase64 = ImageUtils.uriToCompressedBase64(requireContext(), uri);
            // Success, so now create new image object and override newImage holder
            newPoster = new Image(posterBase64);  // **Image object is never saved, nor uploaded to database on creation
            newPoster.setImageId(poster.getImageId());  // Set same image ID, database uploads will update the same image document
            bindPoster(newPoster);

        } catch (IOException e) {
            Toast.makeText(requireContext(), "Failed to process image", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Opens the registration-start date-time picker.
     */
    private void openRegistrationStartPicker() {
        openDateTimePicker(
                "organizer_reg_start",
                getString(R.string.create_event_registration_start_picker_title),
                regStartDate,
                timestamp -> {
                    regStartDate = timestamp;
                    regFromInput.setText(EventMetadataUtils.formatDateTime(regStartDate));
                }
        );
    }

    /**
     * Opens the registration-end date-time picker.
     */
    private void openRegistrationEndPicker() {
        openDateTimePicker(
                "organizer_reg_end",
                getString(R.string.create_event_registration_end_picker_title),
                regEndDate,
                timestamp -> {
                    regEndDate = timestamp;
                    regToInput.setText(EventMetadataUtils.formatDateTime(regEndDate));
                }
        );
    }

    /**
     * Opens the event-start date-time picker.
     */
    private void openEventStartPicker() {
        openDateTimePicker(
                "organizer_event_start",
                getString(R.string.create_event_schedule_start_picker_title),
                eventStartDate,
                timestamp -> {
                    eventStartDate = timestamp;
                    eventStartInput.setText(EventMetadataUtils.formatDateTime(eventStartDate));
                }
        );
    }

    /**
     * Opens the event-end date-time picker.
     */
    private void openEventEndPicker() {
        openDateTimePicker(
                "organizer_event_end",
                getString(R.string.create_event_schedule_end_picker_title),
                eventEndDate,
                timestamp -> {
                    eventEndDate = timestamp;
                    eventEndInput.setText(EventMetadataUtils.formatDateTime(eventEndDate));
                }
        );
    }

    /**
     * Opens a Material date picker followed by a time picker for one editable date-time field.
     *
     * @param tagPrefix Fragment-manager tag prefix for the picker dialogs.
     * @param title Title shown on the picker surfaces.
     * @param currentValue Existing timestamp to seed the pickers with.
     * @param listener Callback that receives the combined timestamp.
     */
    private void openDateTimePicker(
            @NonNull String tagPrefix,
            @NonNull String title,
            @Nullable Timestamp currentValue,
            @NonNull DateTimeSelectionListener listener
    ) {
        Long currentMillis = currentValue == null ? null : currentValue.toDate().getTime();
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
     * Opens the time portion of the organizer date-time picker flow.
     *
     * @param tagPrefix Fragment-manager tag prefix for the picker dialogs.
     * @param title Title shown on the picker surface.
     * @param currentMillis Existing millis to seed the picker with.
     * @param selectedUtcDateMillis Date chosen from MaterialDatePicker.
     * @param listener Callback that receives the combined timestamp.
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
                new Timestamp(new Date(EventMetadataUtils.combineUtcDateWithLocalTime(
                        selectedUtcDateMillis,
                        timePicker.getHour(),
                        timePicker.getMinute()
                )))
        ));

        timePicker.show(getParentFragmentManager(), tagPrefix + "_time");
    }

    /**
     * Starts the save flow for organizer edits.
     */
    private void saveChanges() {
        if (currentEvent == null) {
            Toast.makeText(requireContext(), "Event not loaded yet", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check all values
        // Name
        String name = (nameInput.getText()) == null ? "" : nameInput.getText().toString().trim();
        if (TextUtils.isEmpty(name)) {
            nameInput.setError("Required");
            return;
        }

        // Registration Start and End Timestamps
        if (regStartDate == null || regEndDate == null) {
            Toast.makeText(requireContext(), R.string.create_event_set_registration_period, Toast.LENGTH_SHORT).show();
            return;
        }

        if (eventStartDate == null || eventEndDate == null) {
            Toast.makeText(requireContext(), R.string.create_event_set_schedule, Toast.LENGTH_SHORT).show();
            return;
        }

        if (regStartDate.toDate().getTime() >= regEndDate.toDate().getTime()) {
            Toast.makeText(requireContext(), R.string.create_event_registration_order_error, Toast.LENGTH_SHORT).show();
            return;
        }

        if (eventStartDate.toDate().getTime() > eventEndDate.toDate().getTime()) {
            Toast.makeText(requireContext(), R.string.create_event_schedule_order_error, Toast.LENGTH_SHORT).show();
            return;
        }

        if (regEndDate.toDate().getTime() > eventStartDate.toDate().getTime()) {
            Toast.makeText(requireContext(), R.string.create_event_schedule_conflict_error, Toast.LENGTH_SHORT).show();
            return;
        }

        // Event Capacity
        String capText = capacityInput.getText() == null ? "" : capacityInput.getText().toString().trim();
        Integer eventCap = null;
        if (TextUtils.isEmpty(capText)) {
            capacityInput.setError("Number required");
            return;
        }
        if (!TextUtils.isEmpty(capText)) {
            try {
                eventCap = Integer.parseInt(capText);
            } catch (NumberFormatException e) {
                capacityInput.setError("Enter a valid number");
                return;
            }
        }
        int normalizedCapacity = eventCap == null ? -1 : eventCap;

        // WaitingList Capacity
        String waitingListText = (waitingListCapacityInput.getText() == null) ? "" : waitingListCapacityInput.getText().toString().trim();
        Integer waitingListCap = null;
        if (TextUtils.isEmpty(waitingListText)) {
            waitingListCapacityInput.setError("Number required");
            return;
        }
        if (!TextUtils.isEmpty(waitingListText)) {
            try {
                waitingListCap = Integer.parseInt(waitingListText);
            } catch (NumberFormatException e) {
                waitingListCapacityInput.setError("Enter a valid number");
                return;
            }
        }
        int normalizedEventWaitingListCapacity = waitingListCap == null ? -1 : waitingListCap;


        // Geolocation
        boolean geoLocation = geoSwitch != null && geoSwitch.isChecked();

        // Details
        String details = detailsInput.getText() == null ? "" : detailsInput.getText().toString().trim();

        // Check if poster was modified
        // If poster was modified, newPoster will be a new image object
        // and the condition below will fail.
        if (poster != newPoster) {
            // Update image and upload to database (same document ID)
            ImageDB.saveImage(newPoster, new ImageDB.ImageMutationCallback() {
                @Override
                public void onSuccess() {
                    // Set the new default image object
                    poster = newPoster;
                    // TODO: potentially call update Event from here so that both image
                    //  and event upload do not run unless image uploads completes
                }

                @Override
                public void onFailure(Exception e) {
                    Log.e("Image DB", "Unable to upload image.", e);
                }
            });
        }

        // Save old copy of event
        Map<String, Object> oldCopy = currentEvent.toMap();

        // Update values
        currentEvent.setName(name);
        currentEvent.setRegistrationStartTimestamp(regStartDate);
        currentEvent.setRegistrationEndTimestamp(regEndDate);
        currentEvent.setEventStartTimestamp(eventStartDate);
        currentEvent.setEventEndTimestamp(eventEndDate);
        currentEvent.setEventCapacity(normalizedCapacity);
        currentEvent.setWaitingListCapacity(normalizedEventWaitingListCapacity);
        //currentEvent.isGeolocationEnforced(geoLocation);  // TODO: Remove geolocation modification??
        // Does not make sense to change geolocation, otherwise, it is difficult to ensure all previous users have geolocation on
        currentEvent.setDescription(details);

        // Modify or Update event
        EventDB.updateEvent(currentEvent, new EventDB.EventMutationCallback() {
            @Override
            public void onSuccess() {
                // If event is successfully modified, the realtime listener from EventHandler should
                // trigger the observers in this fragment.
                Toast.makeText(requireContext(), "Event successfully updated.", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Exception e) {
                Log.e("Event DB", "Unable to modify event", e);
                Toast.makeText(requireContext(), "Updating event failed. Please try again.", Toast.LENGTH_SHORT).show();

                // Restore object state
                currentEvent.loadMap(oldCopy);
            }
        });
    }

    /**
     * Opens a confirmation dialog before deleting the current event.
     */
    private void confirmDelete() {
        if (currentEvent == null) {
            return;
        }

        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_ticket_confirmation, null, false);

        TextView titleView = dialogView.findViewById(R.id.dialog_title);
        TextView messageView = dialogView.findViewById(R.id.dialog_message);
        Button primaryButton = dialogView.findViewById(R.id.dialog_primary_button);
        Button secondaryButton = dialogView.findViewById(R.id.dialog_secondary_button);

        titleView.setText(R.string.organizer_event_preview_delete_title);
        messageView.setText(R.string.organizer_event_preview_delete_message);
        primaryButton.setText(R.string.organizer_event_preview_delete_confirm);
        secondaryButton.setText(android.R.string.cancel);

        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(dialogView);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        primaryButton.setOnClickListener(new View.OnClickListener() {
            /**
             * Deletes the current event after the organizer confirms the action.
             *
             * @param v Primary confirmation button that was tapped.
             */
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                deleteCurrentEvent();
            }
        });
        secondaryButton.setOnClickListener(new View.OnClickListener() {
            /**
             * Cancels the delete flow and closes the confirmation dialog.
             *
             * @param v Secondary cancel button that was tapped.
             */
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    /**
     * Starts the delete flow for the current organizer event.
     */
    private void deleteCurrentEvent() {
        if (currentEvent == null) {
            return;
        }
        deleteInProgress = true;
        // Delete Event
        EventDB.deleteEvent(currentEvent, new EventDB.EventMutationCallback() {
            @Override
            public void onSuccess() {
                Log.d("Event DB", "Event deletion success!");
                currentEvent.stopListenAllLists();
                organizeViewModel.getEventHandler().setEventShown(null);
                if (viewModel != null) {
                    viewModel.setEventShown(null);
                }
            }

            @Override
            public void onFailure(Exception e) {
                deleteInProgress = false;
                Log.e("Event DB", "Unable to delete event", e);
                Toast.makeText(requireContext(), "Deleting event failed. Please try again.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Opens the teammate-owned manage-entrants fragment for the current event.
     */
    private void openManageEntrantsFragment() {
        if (currentEvent == null) {
            Toast.makeText(requireContext(), "Event not loaded yet", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Class<?> fragmentClass = Class.forName("com.example.breeze_seas.OrganizerListHostFragment");
            Object instance = fragmentClass.getDeclaredConstructor().newInstance();
            if (!(instance instanceof Fragment)) {
                throw new IllegalStateException("ManageEntrantsFragment is not a Fragment");
            }

            Fragment fragment = (Fragment) instance;
            if (viewModel != null) {
                viewModel.setEventShown(currentEvent);
            }
            ((MainActivity) requireActivity()).openSecondaryFragment(fragment);
        } catch (Exception e) {
            Toast.makeText(
                    requireContext(),
                    R.string.organizer_event_preview_manage_unavailable,
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    /**
     * Opens the teammate-owned announcement fragment for the current event.
     */
    private void openAnnouncementFragment() {
        if (currentEvent == null) {
            Toast.makeText(requireContext(), "Event not loaded yet", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Class<?> fragmentClass = Class.forName("com.example.breeze_seas.SendAnnouncementFragment");
            Object instance = fragmentClass.getDeclaredConstructor().newInstance();
            if (!(instance instanceof Fragment)) {
                throw new IllegalStateException("SendAnnouncementFragment is not a Fragment");
            }

            Fragment fragment = (Fragment) instance;
            if (viewModel != null) {
                viewModel.setEventShown(currentEvent);
            }
            ((MainActivity) requireActivity()).openSecondaryFragment(fragment);
        } catch (Exception e) {
            Toast.makeText(
                    requireContext(),
                    R.string.organizer_event_preview_announcement_unavailable,
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    /**
     * Opens the map view fragment
     */

    private void openMapFragment() {

        if (currentEvent == null) {
            Toast.makeText(requireContext(), "Event not loaded yet", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Class<?> fragmentClass = Class.forName("com.example.breeze_seas.MapsFragment");
            Object instance = fragmentClass.getDeclaredConstructor().newInstance();
            if (!(instance instanceof Fragment)) {
                throw new IllegalStateException("MapsFragment is not a Fragment");
            }

            Fragment fragment = (Fragment) instance;
            if (viewModel != null) {
                viewModel.setEventShown(currentEvent);
            }
            ((MainActivity) requireActivity()).openSecondaryFragment(fragment);
        } catch (Exception e) {
            Toast.makeText(
                    requireContext(),
                    R.string.organizer_event_preview_announcement_unavailable,
                    Toast.LENGTH_SHORT
            ).show();
        }

    }

    private void openCoOrganizerFragment() {
        if (currentEvent == null) {
            Toast.makeText(requireContext(), "Event not loaded yet", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Class<?> fragmentClass = Class.forName("com.example.breeze_seas.CoOrganizerFragment");
            Object instance = fragmentClass.getDeclaredConstructor().newInstance();
            if (!(instance instanceof Fragment)) {
                throw new IllegalStateException("CoOrganizerFragment is not a Fragment");
            }

            Fragment fragment = (Fragment) instance;
            if (viewModel != null) {
                viewModel.setEventShown(currentEvent);
            }
            ((MainActivity) requireActivity()).openSecondaryFragment(fragment);
        } catch (Exception e) {
            Toast.makeText(
                    requireContext(),
                    "Not available",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    /**
     * Opens the private-event invite picker for the current event.
     */
    private void openPrivateEventInviteFragment() {
        if (currentEvent == null) {
            Toast.makeText(requireContext(), "Event not loaded yet", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!currentEvent.isPrivate()) {
            Toast.makeText(requireContext(), R.string.private_event_invite_private_only, Toast.LENGTH_SHORT).show();
            return;
        }
        if (viewModel != null) {
            viewModel.setEventShown(currentEvent);
        }
        ((MainActivity) requireActivity()).openSecondaryFragment(new PrivateEventInviteFragment());
    }


    /**
     * Displays the current event poster or falls back to the placeholder artwork.
     *
     * @param image Image object to display, or {@code null} if unavailable.
     */
    private void bindPoster(@Nullable Image image) {
        posterImageView.setImageResource(R.drawable.ic_image_placeholder);
        if (image == null) {
            return;
        }

        try {
            posterImageView.setImageBitmap(image.display());
        } catch (Exception ignored) {
            posterImageView.setImageResource(R.drawable.ic_image_placeholder);
        }
    }
}
