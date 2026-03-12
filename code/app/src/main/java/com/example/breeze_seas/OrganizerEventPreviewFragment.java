package com.example.breeze_seas;

import android.app.AlertDialog;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class OrganizerEventPreviewFragment extends Fragment {

    private static final String ARG_EVENT_ID = "eventId";

    private SessionViewModel viewModel;
    private Event currentEvent;

    private ImageView posterImageView;
    private TextInputEditText nameInput;
    private TextInputEditText regFromInput;
    private TextInputEditText regToInput;
    private TextInputEditText capacityInput;
    private TextInputEditText detailsInput;
    private SwitchMaterial geoSwitch;

    private Long regFromMillis;
    private Long regToMillis;
    private String posterUriString;

    private final ActivityResultLauncher<String> pickImage =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri == null) {
                    return;
                }

                posterUriString = uri.toString();
                if (posterImageView != null) {
                    posterImageView.setImageURI(uri);
                }
            });

    public OrganizerEventPreviewFragment() {
        super(R.layout.fragment_organizer_event_preview);
    }

    public static OrganizerEventPreviewFragment newInstance(@NonNull String eventId) {
        OrganizerEventPreviewFragment fragment = new OrganizerEventPreviewFragment();
        Bundle args = new Bundle();
        args.putString(ARG_EVENT_ID, eventId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(SessionViewModel.class);

        String eventId = getArguments() == null ? null : getArguments().getString(ARG_EVENT_ID);
        if (eventId == null || eventId.trim().isEmpty()) {
            Toast.makeText(requireContext(), "Unable to open event details", Toast.LENGTH_SHORT).show();
            requireActivity().getSupportFragmentManager().popBackStack();
            return;
        }

        bindViews(view);

        view.findViewById(R.id.organizer_event_preview_back).setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack()
        );

        View.OnClickListener posterClickListener = v -> pickImage.launch("image/*");
        view.findViewById(R.id.organizer_event_preview_poster_card).setOnClickListener(posterClickListener);
        view.findViewById(R.id.organizer_event_preview_update_poster_button).setOnClickListener(posterClickListener);

        View.OnClickListener dateClickListener = v -> openDateRangePicker();
        view.findViewById(R.id.organizer_event_preview_reg_period_button).setOnClickListener(dateClickListener);
        regFromInput.setOnClickListener(dateClickListener);
        regToInput.setOnClickListener(dateClickListener);

        view.findViewById(R.id.organizer_event_preview_save_button).setOnClickListener(v ->
                saveChanges()
        );
        view.findViewById(R.id.organizer_event_preview_delete_button).setOnClickListener(v ->
                confirmDelete()
        );
        view.findViewById(R.id.organizer_event_preview_manage_button).setOnClickListener(v ->
                openManageEntrantsFragment()
        );
        view.findViewById(R.id.organizer_event_preview_announcement_button).setOnClickListener(v ->
                openAnnouncementFragment()
        );

        loadEvent(eventId);
    }

    private void bindViews(@NonNull View view) {
        posterImageView = view.findViewById(R.id.organizer_event_preview_poster);
        nameInput = view.findViewById(R.id.organizer_event_preview_name_input);
        regFromInput = view.findViewById(R.id.organizer_event_preview_reg_from_input);
        regToInput = view.findViewById(R.id.organizer_event_preview_reg_to_input);
        capacityInput = view.findViewById(R.id.organizer_event_preview_capacity_input);
        detailsInput = view.findViewById(R.id.organizer_event_preview_details_input);
        geoSwitch = view.findViewById(R.id.organizer_event_preview_geo_switch);
    }

    private void loadEvent(@NonNull String eventId) {
        EventDB.getInstance().getEventById(eventId, new EventDB.LoadSingleEventCallback() {
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

        regFromMillis = event.getRegFromMillis();
        regToMillis = event.getRegToMillis();
        posterUriString = event.getPosterUriString();

        ((android.widget.TextView) root.findViewById(R.id.organizer_event_preview_title)).setText(event.getName());
        ((android.widget.TextView) root.findViewById(R.id.organizer_event_preview_subtitle))
                .setText(R.string.organizer_event_preview_subtitle_text);

        nameInput.setText(event.getName());
        regFromInput.setText(formatDate(event.getRegFromMillis()));
        regToInput.setText(formatDate(event.getRegToMillis()));
        capacityInput.setText(event.getWaitingListCap() == null ? "" : String.valueOf(event.getWaitingListCap()));
        detailsInput.setText(event.getDetails());
        geoSwitch.setChecked(event.isGeoRequired());

        bindPoster(posterUriString);
    }

    private void openDateRangePicker() {
        MaterialDatePicker.Builder<androidx.core.util.Pair<Long, Long>> builder =
                MaterialDatePicker.Builder.dateRangePicker()
                        .setTheme(R.style.ThemeOverlay_Breezeseas_DateRangePicker)
                        .setTitleText("Select registration period");

        if (regFromMillis != null && regToMillis != null) {
            builder.setSelection(new androidx.core.util.Pair<>(regFromMillis, regToMillis));
        }

        MaterialDatePicker<androidx.core.util.Pair<Long, Long>> picker = builder.build();
        picker.addOnPositiveButtonClickListener(selection -> {
            if (selection == null) {
                return;
            }

            regFromMillis = selection.first;
            regToMillis = selection.second;

            regFromInput.setText(formatDate(regFromMillis));
            regToInput.setText(formatDate(regToMillis));
        });

        picker.show(getParentFragmentManager(), "organizer_reg_range");
    }

    private void saveChanges() {
        if (currentEvent == null) {
            Toast.makeText(requireContext(), "Event not loaded yet", Toast.LENGTH_SHORT).show();
            return;
        }

        String name = getTrimmedText(nameInput);
        String details = getTrimmedText(detailsInput);
        String capText = getTrimmedText(capacityInput);

        if (TextUtils.isEmpty(name)) {
            nameInput.setError("Required");
            return;
        }

        if (regFromMillis == null || regToMillis == null) {
            Toast.makeText(requireContext(), "Please set registration period", Toast.LENGTH_SHORT).show();
            return;
        }

        Integer cap = null;
        if (!TextUtils.isEmpty(capText)) {
            try {
                cap = Integer.parseInt(capText);
            } catch (NumberFormatException e) {
                capacityInput.setError("Enter a valid number");
                return;
            }
        }

        Event updatedEvent = new Event(
                currentEvent.getId(),
                name,
                details,
                posterUriString,
                regFromMillis,
                regToMillis,
                cap,
                geoSwitch.isChecked()
        );

        EventDB.getInstance().updateEvent(updatedEvent, new EventDB.EventMutationCallback() {
            @Override
            public void onSuccess() {
                if (!isAdded()) {
                    return;
                }

                currentEvent = updatedEvent;
                if (viewModel != null) {
                    viewModel.setEventShown(updatedEvent);
                }
                ((android.widget.TextView) requireView().findViewById(R.id.organizer_event_preview_title))
                        .setText(updatedEvent.getName());
                Toast.makeText(requireContext(), "Event updated", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Exception e) {
                if (!isAdded()) {
                    return;
                }

                Toast.makeText(requireContext(), "Failed to update event", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void confirmDelete() {
        if (currentEvent == null) {
            return;
        }

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.organizer_event_preview_delete_title)
                .setMessage(R.string.organizer_event_preview_delete_message)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.organizer_event_preview_delete_confirm, (dialog, which) ->
                        deleteCurrentEvent()
                )
                .show();
    }

    private void deleteCurrentEvent() {
        if (currentEvent == null) {
            return;
        }

        EventDB.getInstance().deleteEvent(currentEvent.getId(), new EventDB.EventMutationCallback() {
            @Override
            public void onSuccess() {
                if (!isAdded()) {
                    return;
                }

                Toast.makeText(requireContext(), "Event deleted", Toast.LENGTH_SHORT).show();
                requireActivity().getSupportFragmentManager().popBackStack();
            }

            @Override
            public void onFailure(Exception e) {
                if (!isAdded()) {
                    return;
                }

                Toast.makeText(requireContext(), "Failed to delete event", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openManageEntrantsFragment() {
        if (currentEvent == null) {
            Toast.makeText(requireContext(), "Event not loaded yet", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Class<?> fragmentClass = Class.forName("com.example.breeze_seas.ManageEntrantsFragment");
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

    private void bindPoster(@Nullable String uriString) {
        posterImageView.setImageResource(R.drawable.ic_image_placeholder);
        if (uriString == null || uriString.trim().isEmpty()) {
            return;
        }

        try {
            posterImageView.setImageURI(Uri.parse(uriString));
        } catch (Exception ignored) {
            posterImageView.setImageResource(R.drawable.ic_image_placeholder);
        }
    }

    @NonNull
    private String getTrimmedText(@NonNull TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString().trim();
    }

    private String formatDate(long millis) {
        if (millis <= 0L) {
            return getString(R.string.organizer_event_preview_not_set);
        }
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(new Date(millis));
    }
}
