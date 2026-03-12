package com.example.breeze_seas;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class OrganizerEventPreviewFragment extends Fragment {

    private Event currentEvent;
    private SessionViewModel viewModel;

    public OrganizerEventPreviewFragment() {
        super(R.layout.fragment_organizer_event_preview);
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(SessionViewModel.class);
        currentEvent = viewModel.getEventShown().getValue();
        String eventId=currentEvent.getEventId();
        view.findViewById(R.id.organizer_event_preview_back).setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack()
        );

        loadEvent(view, eventId);
    }

    private void loadEvent(@NonNull View view, @NonNull String eventId) {
        TextView title = view.findViewById(R.id.organizer_event_preview_title);
        title.setText(R.string.organizer_event_preview_loading);

        EventDB.getEventById(eventId, new EventDB.LoadSingleEventCallback() {
            @Override
            public void onSuccess(Event event) {
                if (!isAdded()) {
                    return;
                }

                if (event == null) {
                    title.setText(R.string.organizer_event_preview_not_found);
                    return;
                }

                bindEvent(view, event);
            }

            @Override
            public void onFailure(Exception e) {
                if (!isAdded()) {
                    return;
                }

                title.setText(R.string.organizer_event_preview_error);
                Toast.makeText(requireContext(), "Failed to load event preview", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void bindEvent(@NonNull View view, @NonNull Event event) {
        TextView title = view.findViewById(R.id.organizer_event_preview_title);
        TextView subtitle = view.findViewById(R.id.organizer_event_preview_subtitle);
        TextView regWindow = view.findViewById(R.id.organizer_event_preview_reg_window);
        TextView waitingCap = view.findViewById(R.id.organizer_event_preview_waiting_cap);
        TextView geoRequirement = view.findViewById(R.id.organizer_event_preview_geo);
        TextView details = view.findViewById(R.id.organizer_event_preview_details);
        ImageView poster = view.findViewById(R.id.organizer_event_preview_poster);

//        title.setText(event.getName());
//        subtitle.setText(R.string.organizer_event_preview_subtitle_text);
//        regWindow.setText(getString(
//                R.string.organizer_event_preview_registration_window,
//                formatDate(event.getRegFromMillis()),
//                formatDate(event.getRegToMillis())
//        ));
//        waitingCap.setText(event.getWaitingListCap() == null
//                ? getString(R.string.organizer_event_preview_waiting_unlimited)
//                : getString(R.string.organizer_event_preview_waiting_limited, event.getWaitingListCap()));
//        geoRequirement.setText(event.isGeoRequired()
//                ? getString(R.string.organizer_event_preview_geo_required)
//                : getString(R.string.organizer_event_preview_geo_optional));
//        details.setText(event.getDetails().trim().isEmpty()
//                ? getString(R.string.organizer_event_preview_no_description)
//                : event.getDetails());
//
//        bindPoster(poster, event.getPosterUriString());

        view.findViewById(R.id.organizer_event_preview_manage_button).setOnClickListener(v -> {
            openManageEntrantsFragment(event);
        });

        view.findViewById(R.id.organizer_event_preview_announcement_button).setOnClickListener(v -> {
            openAnnouncementFragment(event);
        });
    }

    private void openManageEntrantsFragment(@NonNull Event event) {

        if (viewModel != null) {
            viewModel.setEventShown(event);
        }

        OrganizerListHostFragment fragment = new OrganizerListHostFragment();
        if (requireActivity() instanceof MainActivity) {
            ((MainActivity) requireActivity()).openSecondaryFragment(fragment);
        } else {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit();
        }
    }

    private void openAnnouncementFragment(@NonNull Event event) {
        try {
            Class<?> fragmentClass = Class.forName("com.example.breeze_seas.SendAnnouncementFragment");
            Object instance = fragmentClass.getDeclaredConstructor().newInstance();
            if (!(instance instanceof Fragment)) {
                throw new IllegalStateException("SendAnnouncementFragment is not a Fragment");
            }

            Fragment fragment = (Fragment) instance;
            if (viewModel != null) {
                viewModel.setEventShown(event);
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

    private void bindPoster(@NonNull ImageView poster, @Nullable String posterUriString) {
        poster.setImageResource(R.drawable.ic_image_placeholder);
        if (posterUriString == null || posterUriString.trim().isEmpty()) {
            return;
        }

        try {
            poster.setImageURI(Uri.parse(posterUriString));
        } catch (Exception ignored) {
            poster.setImageResource(R.drawable.ic_image_placeholder);
        }
    }

    private String formatDate(long millis) {
        if (millis <= 0L) {
            return getString(R.string.organizer_event_preview_not_set);
        }
        return new SimpleDateFormat("MMM d, yyyy", Locale.US).format(new Date(millis));
    }
}
