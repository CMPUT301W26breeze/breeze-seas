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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class OrganizerEventPreviewFragment extends Fragment {

    private static final String ARG_EVENT_ID = "eventId";

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

        String eventId = getArguments() == null ? null : getArguments().getString(ARG_EVENT_ID);
        if (eventId == null || eventId.trim().isEmpty()) {
            Toast.makeText(requireContext(), "Unable to open event preview", Toast.LENGTH_SHORT).show();
            requireActivity().getSupportFragmentManager().popBackStack();
            return;
        }

        view.findViewById(R.id.organizer_event_preview_back).setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack()
        );

        loadEvent(view, eventId);
    }

    private void loadEvent(@NonNull View view, @NonNull String eventId) {
        TextView title = view.findViewById(R.id.organizer_event_preview_title);
        title.setText(R.string.organizer_event_preview_loading);

        EventDB.getInstance().getEventById(eventId, new EventDB.LoadSingleEventCallback() {
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

        title.setText(event.getName());
        subtitle.setText(R.string.organizer_event_preview_subtitle_text);
        regWindow.setText(getString(
                R.string.organizer_event_preview_registration_window,
                formatDate(event.getRegFromMillis()),
                formatDate(event.getRegToMillis())
        ));
        waitingCap.setText(event.getWaitingListCap() == null
                ? getString(R.string.organizer_event_preview_waiting_unlimited)
                : getString(R.string.organizer_event_preview_waiting_limited, event.getWaitingListCap()));
        geoRequirement.setText(event.isGeoRequired()
                ? getString(R.string.organizer_event_preview_geo_required)
                : getString(R.string.organizer_event_preview_geo_optional));
        details.setText(event.getDetails().trim().isEmpty()
                ? getString(R.string.organizer_event_preview_no_description)
                : event.getDetails());

        bindPoster(poster, event.getPosterUriString());

        view.findViewById(R.id.organizer_event_preview_manage_button).setOnClickListener(v -> {
            openManageEntrantsFragment(event);
        });

        view.findViewById(R.id.organizer_event_preview_announcement_button).setOnClickListener(v -> {
            openAnnouncementFragment(event);
        });
    }

    private void openManageEntrantsFragment(@NonNull Event event) {
        try {
            Class<?> fragmentClass = Class.forName("com.example.breeze_seas.ManageEntrantsFragment");
            Object instance = fragmentClass.getDeclaredConstructor().newInstance();
            if (!(instance instanceof Fragment)) {
                throw new IllegalStateException("ManageEntrantsFragment is not a Fragment");
            }

            Fragment fragment = (Fragment) instance;
            Bundle args = new Bundle();
            args.putString("eventId", event.getId());
            args.putString("eventName", event.getName());
            fragment.setArguments(args);
            ((MainActivity) requireActivity()).openSecondaryFragment(fragment);
        } catch (Exception e) {
            Toast.makeText(
                    requireContext(),
                    R.string.organizer_event_preview_manage_unavailable,
                    Toast.LENGTH_SHORT
            ).show();
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
            Bundle args = new Bundle();
            args.putString("eventId", event.getId());
            args.putString("eventName", event.getName());
            fragment.setArguments(args);
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
