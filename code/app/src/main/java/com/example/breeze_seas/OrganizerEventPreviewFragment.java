package com.example.breeze_seas;

import android.app.AlertDialog;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/**
 * OrganizerEventPreviewFragment displays one organizer-owned event and provides organizer actions
 * such as editing event metadata, managing entrants, and opening the announcement flow.
 */
public class OrganizerEventPreviewFragment extends Fragment {
    private SessionViewModel viewModel;
    private OrganizeViewModel organizeViewModel;
    private Event currentEvent;

    private ImageView posterImageView;
    private TextInputEditText nameInput;
    private TextInputEditText regFromInput;
    private TextInputEditText regToInput;
    private TextInputEditText capacityInput;
    private TextInputEditText waitingListCapacityInput;
    private TextInputEditText detailsInput;
    private SwitchMaterial geoSwitch;

    private Timestamp regStartDate;
    private Timestamp regEndDate;
    private Image poster;
    private Image newPoster;
    private String posterBase64 = "";
    private EventCommentsSectionController commentsSectionController;

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
                populateFields(currentEvent);
            }

            @Override
            public void onError(Exception e) {
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

                        // This happens when the EventHandler detects the event was deleted
                        // either by the organizer themselves or by other organizers or admin or database authorities
                        Toast.makeText(requireContext(), "Event deleted.", Toast.LENGTH_SHORT).show();

                        // Return to organize fragment
                        requireActivity().getSupportFragmentManager().popBackStack();
                    }

                    // Since reference to the object is the same, the details should be updated automatically.
                    // Populate views with values.
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
                openDateRangePicker();
            }
        };
        view.findViewById(R.id.organizer_event_preview_reg_period_button).setOnClickListener(dateClickListener);
        regFromInput.setOnClickListener(dateClickListener);
        regToInput.setOnClickListener(dateClickListener);

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
        regFromInput.setText(formatDate(regStartDate));
        regToInput.setText(formatDate(regEndDate));
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
     * Opens the registration-range picker and updates the displayed range when confirmed.
     */
    private void openDateRangePicker() {
        MaterialDatePicker.Builder<androidx.core.util.Pair<Long, Long>> builder =
                MaterialDatePicker.Builder.dateRangePicker()
                        .setTheme(R.style.ThemeOverlay_Breezeseas_DateRangePicker)
                        .setTitleText("Select registration period");

        if (regStartDate != null && regEndDate != null) {
            builder.setSelection(new androidx.core.util.Pair<>(regStartDate.toDate().getTime(), regEndDate.toDate().getTime()));
        }

        MaterialDatePicker<androidx.core.util.Pair<Long, Long>> picker = builder.build();
        picker.addOnPositiveButtonClickListener(new com.google.android.material.datepicker.MaterialPickerOnPositiveButtonClickListener<androidx.core.util.Pair<Long, Long>>() {
            /**
             * Stores the selected registration range and updates the visible date fields.
             *
             * @param selection Selected registration start and end dates.
             */
            @Override
            public void onPositiveButtonClick(androidx.core.util.Pair<Long, Long> selection) {
                if (selection == null) {
                    return;
                }

                //SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy", Locale.US);
                //sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                regStartDate = new Timestamp(new Date(selection.first));
                regEndDate = new Timestamp(new Date(selection.second));

                regFromInput.setText(formatDate(regStartDate));
                regToInput.setText(formatDate(regEndDate));
            }
        });

        picker.show(getParentFragmentManager(), "organizer_reg_range");
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
            Toast.makeText(requireContext(), "Please set registration period", Toast.LENGTH_SHORT).show();
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

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.organizer_event_preview_delete_title)
                .setMessage(R.string.organizer_event_preview_delete_message)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.organizer_event_preview_delete_confirm, new android.content.DialogInterface.OnClickListener() {
                    /**
                     * Deletes the current event after the organizer confirms the action.
                     *
                     * @param dialog Dialog that collected the delete confirmation.
                     * @param which Button identifier chosen by the user.
                     */
                    @Override
                    public void onClick(android.content.DialogInterface dialog, int which) {
                        deleteCurrentEvent();
                    }
                })
                .show();
    }

    /**
     * Starts the delete flow for the current organizer event.
     */
    private void deleteCurrentEvent() {
        if (currentEvent == null) {
            return;
        }
        // Delete Event
        EventDB.deleteEvent(currentEvent, new EventDB.EventMutationCallback() {
            @Override
            public void onSuccess() {
                Log.d("Event DB", "Event deletion success!");
                // If event is successfully deleted, the realtime listener from EventHandler should
                // trigger the observers in this fragment, leading the user back to the organizer
                // fragment.
            }

            @Override
            public void onFailure(Exception e) {
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

    /**
     * Formats a timestamp into the organizer preview date label.
     *
     * @param timestamp The timestamp to format.
     * @return Display-ready date string.
     */
    private String formatDate(Timestamp timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy", Locale.US);
        return sdf.format(new Date(timestamp.toDate().getTime()));
    }
}
