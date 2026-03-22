package com.example.breeze_seas;

import android.app.AlertDialog;
import android.graphics.Bitmap;
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
    private String posterBase64 = "";

    private final ActivityResultLauncher<String> pickImage =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri == null) {
                    return;
                }
                handleSelectedPoster(uri);
            });

    public OrganizerEventPreviewFragment() {
        super(R.layout.fragment_organizer_event_preview);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(SessionViewModel.class);

        bindViews(view);

        view.findViewById(R.id.organizer_event_preview_back).setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());

        View.OnClickListener posterClickListener = v -> pickImage.launch("image/*");
        view.findViewById(R.id.organizer_event_preview_poster_card).setOnClickListener(posterClickListener);
        view.findViewById(R.id.organizer_event_preview_update_poster_button).setOnClickListener(posterClickListener);

        View.OnClickListener dateClickListener = v -> openDateRangePicker();
        view.findViewById(R.id.organizer_event_preview_reg_period_button).setOnClickListener(dateClickListener);
        regFromInput.setOnClickListener(dateClickListener);
        regToInput.setOnClickListener(dateClickListener);

        view.findViewById(R.id.organizer_event_preview_save_button).setOnClickListener(v -> saveChanges());
        view.findViewById(R.id.organizer_event_preview_delete_button).setOnClickListener(v -> confirmDelete());
        view.findViewById(R.id.organizer_event_preview_manage_button).setOnClickListener(v -> openManageEntrantsFragment());
        view.findViewById(R.id.organizer_event_preview_announcement_button).setOnClickListener(v -> openAnnouncementFragment());
        view.findViewById(R.id.organizer_event_preview_map_button).setOnClickListener(v -> openMapFragment());

        resolveAndLoadEvent();
    }

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

    private void loadEvent(@NonNull String eventId) {
        EventDB.getEventById(eventId, new EventDB.LoadSingleEventCallback() {
            @Override
            public void onSuccess(Event event) {
                if (!isAdded()) {
                    return;
                }
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

    private void populateFields(@NonNull Event event) {
        View root = getView();
        if (root == null) {
            return;
        }

        regStartDate = event.getRegistrationStartTimestamp();
        regEndDate = event.getRegistrationEndTimestamp();
        posterBase64 = event.getImage() == null ? "" : event.getImage();

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

        bindPoster(posterBase64);
    }

    private void handleSelectedPoster(@NonNull Uri uri) {
        try {
            posterBase64 = ImageUtils.uriToCompressedBase64(requireContext(), uri);
            bindPoster(posterBase64);
        } catch (IOException e) {
            Toast.makeText(requireContext(), "Failed to process image", Toast.LENGTH_SHORT).show();
        }
    }

    private void openDateRangePicker() {
        MaterialDatePicker.Builder<androidx.core.util.Pair<Long, Long>> builder =
                MaterialDatePicker.Builder.dateRangePicker()
                        .setTheme(R.style.ThemeOverlay_Breezeseas_DateRangePicker)
                        .setTitleText("Select registration period");

        if (regStartDate != null && regEndDate != null) {
            builder.setSelection(new androidx.core.util.Pair<>(
                    regStartDate.toDate().getTime(),
                    regEndDate.toDate().getTime()
            ));
        }

        MaterialDatePicker<androidx.core.util.Pair<Long, Long>> picker = builder.build();
        picker.addOnPositiveButtonClickListener(selection -> {
            if (selection == null) {
                return;
            }

            regStartDate = new Timestamp(new Date(selection.first));
            regEndDate = new Timestamp(new Date(selection.second));

            regFromInput.setText(formatDate(regStartDate));
            regToInput.setText(formatDate(regEndDate));
        });

        picker.show(getParentFragmentManager(), "organizer_reg_range");
    }

    private void saveChanges() {
        if (currentEvent == null) {
            Toast.makeText(requireContext(), "Event not loaded yet", Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(requireContext(), "Editing events is coming soon.", Toast.LENGTH_SHORT).show();
    }

    private void confirmDelete() {
        if (currentEvent == null) {
            return;
        }

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.organizer_event_preview_delete_title)
                .setMessage(R.string.organizer_event_preview_delete_message)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.organizer_event_preview_delete_confirm,
                        (dialog, which) -> deleteCurrentEvent())
                .show();
    }

    private void deleteCurrentEvent() {
        if (currentEvent == null) {
            return;
        }
        Toast.makeText(requireContext(), "Deleting events is coming soon.", Toast.LENGTH_SHORT).show();
    }

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

    private void bindPoster(@Nullable String base64String) {
        posterImageView.setImageResource(R.drawable.ic_image_placeholder);
        if (base64String == null || base64String.trim().isEmpty()) {
            return;
        }

        Bitmap bitmap = ImageUtils.base64ToBitmap(base64String);
        if (bitmap != null) {
            posterImageView.setImageBitmap(bitmap);
        } else {
            posterImageView.setImageResource(R.drawable.ic_image_placeholder);
        }
    }

    private String formatDate(@Nullable Timestamp timestamp) {
        if (timestamp == null) {
            return "";
        }
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy", Locale.US);
        return sdf.format(new Date(timestamp.toDate().getTime()));
    }
}