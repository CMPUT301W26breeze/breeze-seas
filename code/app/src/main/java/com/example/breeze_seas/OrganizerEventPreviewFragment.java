package com.example.breeze_seas;

import android.app.AlertDialog;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
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
import com.google.firebase.Timestamp;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.Locale;

/**
 * OrganizerEventPreviewFragment displays one organizer-owned event and provides organizer actions
 * such as editing event metadata, managing entrants, and opening the announcement flow.
 */
public class OrganizerEventPreviewFragment extends Fragment {

    private static final String ARG_EVENT_ID = "eventId";
    private SessionViewModel viewModel;
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

    /**
     * Binds organizer preview views, wires actions, and starts loading the selected event.
     *
     * @param view Inflated organizer-preview root view.
     * @param savedInstanceState Previously saved instance state, or {@code null}.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(SessionViewModel.class);

        bindViews(view);

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

        commentsSectionController = new EventCommentsSectionController(this, view);
        resolveAndLoadEvent();
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
     * Resolves the event identifier from fragment arguments or shared session state.
     */
    private void resolveAndLoadEvent() {
        String eventId = getArguments() == null ? null : getArguments().getString(ARG_EVENT_ID);
        if (eventId == null || eventId.trim().isEmpty()) {
            Event eventShown = viewModel == null ? null : viewModel.getEventShown().getValue();
            if (eventShown != null) {
                eventId = eventShown.getEventId();
            }
        }

        if (eventId == null || eventId.trim().isEmpty()) {
            Toast.makeText(requireContext(), "Unable to open event details", Toast.LENGTH_SHORT).show();
            requireActivity().getSupportFragmentManager().popBackStack();
            return;
        }

        loadEvent(eventId);
    }

    /**
     * Loads the selected event from {@link EventDB}.
     *
     * @param eventId Identifier of the event to display.
     */
    private void loadEvent(@NonNull String eventId) {
        EventDB.getEventById(eventId, new EventDB.LoadSingleEventCallback() {
            /**
             * Populates the organizer preview with the loaded event.
             *
             * @param event Event loaded from the database, or {@code null} if not found.
             */
            @Override
            public void onSuccess(Event event) {
                if (!isAdded()) return;
                if (event == null) {
                    Toast.makeText(requireContext(), R.string.organizer_event_preview_not_found, Toast.LENGTH_SHORT).show();
                    requireActivity().getSupportFragmentManager().popBackStack();
                    return;
                }

                currentEvent = event;
                populateFields(event);
                if (viewModel != null) {
                    viewModel.setEventShown(event);
                }
            }
            /**
             * Reports a user-visible error if the event cannot be loaded.
             *
             * @param e Failure returned by the event lookup.
             */
            @Override
            public void onFailure(Exception e) {
                if (!isAdded()) {
                    return;
                }

                Toast.makeText(requireContext(), "Failed to load event details", Toast.LENGTH_SHORT).show();
                requireActivity().getSupportFragmentManager().popBackStack();
            }
        });
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
        // TODO: Fix image handling
        poster = event.getImage();

        ((android.widget.TextView) root.findViewById(R.id.organizer_event_preview_title)).setText(event.getName());
        ((android.widget.TextView) root.findViewById(R.id.organizer_event_preview_subtitle))
                .setText(R.string.organizer_event_preview_subtitle_text);

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
            bindPoster(new Image(posterBase64));  // **Image object is never saved, nor uploaded to database

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

                // TODO: Fix API in the future
                regStartDate = new Timestamp(Instant.ofEpochSecond(selection.first));
                regEndDate = new Timestamp(Instant.ofEpochSecond(selection.second));


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
        Toast.makeText(requireContext(), "Editing events is coming soon.", Toast.LENGTH_SHORT).show();
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
        Toast.makeText(requireContext(), "Deleting events is coming soon.", Toast.LENGTH_SHORT).show();
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

    private void openMapFragment(){

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
