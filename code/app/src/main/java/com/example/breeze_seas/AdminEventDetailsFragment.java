package com.example.breeze_seas;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.imageview.ShapeableImageView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Admin-only event details screen.
 *
 * Shows event info (poster, description, capacity, dates, organizer) and the full comment thread
 * with admin delete controls on every comment. Reached by tapping an event in
 * {@link AdminBrowseEventsFragment}; the event is passed via
 * {@link SessionViewModel#setEventShown(Event)}.
 */
public class AdminEventDetailsFragment extends Fragment {

    private EventCommentsSectionController commentsController;
    private SessionViewModel sessionViewModel;

    public AdminEventDetailsFragment() { super(R.layout.fragment_admin_event_details); }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sessionViewModel = new ViewModelProvider(requireActivity()).get(SessionViewModel.class);
        Event event = sessionViewModel.getEventShown().getValue();
        User currentUser = sessionViewModel.getUser().getValue();

        MaterialToolbar toolbar = view.findViewById(R.id.aed_toolbar);
        toolbar.setNavigationOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());

        if (event == null) {
            Toast.makeText(getContext(), "Event not found", Toast.LENGTH_SHORT).show();
            requireActivity().getSupportFragmentManager().popBackStack();
            return;
        }

        bindEventFields(view, event);
        bindOrganizers(view, event);

        // Wire comments with admin delete enabled on all comments
        commentsController = new EventCommentsSectionController(this, view);
        commentsController.bind(event, currentUser, false, true);
    }

    private void bindEventFields(@NonNull View view, @NonNull Event event) {
        ShapeableImageView poster = view.findViewById(R.id.aed_iv_poster);
        TextView tvName = view.findViewById(R.id.aed_tv_name);
        TextView tvDescription = view.findViewById(R.id.aed_tv_description);
        TextView tvCapacity = view.findViewById(R.id.aed_tv_capacity);
        TextView tvWaitingList = view.findViewById(R.id.aed_tv_waiting_list);
        TextView tvRegCloses = view.findViewById(R.id.aed_tv_reg_closes);

        tvName.setText(event.getName());
        tvDescription.setText(event.getDescription());

        // Capacity
        String capacityText = event.getEventCapacity() > 0
                ? "Capacity: " + event.getEventCapacity()
                : "Capacity: Unlimited";
        tvCapacity.setText(capacityText);

        // Waiting list
        String waitingText = event.getWaitingListCapacity() > 0
                ? "Waiting list: " + event.getWaitingListCapacity()
                : "Waiting list: Unlimited";
        tvWaitingList.setText(waitingText);

        // Registration closes date
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
        String regEnd = event.getRegistrationEndTimestamp() != null
                ? sdf.format(event.getRegistrationEndTimestamp().toDate())
                : "TBD";
        tvRegCloses.setText("Registration closes " + regEnd);

        // Poster
        poster.setImageTintList(null);
        Image image = event.getImage();
        if (image != null) {
            try {
                poster.setImageBitmap(image.display());
            } catch (Exception ignored) {
                poster.setImageResource(R.drawable.ic_profile);
            }
        }
    }

    private void bindOrganizers(@NonNull View view, @NonNull Event event) {
        TextView tvOrganizer = view.findViewById(R.id.aed_tv_organizer);
        TextView tvCoOrganizersLabel = view.findViewById(R.id.aed_tv_coorganizers_label);
        TextView tvCoOrganizers = view.findViewById(R.id.aed_tv_coorganizers);

        // Show device ID immediately as a fallback while the name loads
        String organizerId = event.getOrganizerId();
        tvOrganizer.setText(organizerId != null ? organizerId : "Unknown");

        if (organizerId != null && !organizerId.isEmpty()) {
            UserDB userDB = new UserDB();
            userDB.getUser(organizerId, new UserDB.OnUserLoadedListener() {
                @Override
                public void onUserLoaded(@Nullable User user) {
                    if (getView() == null) return;
                    tvOrganizer.setText(user != null ? buildDisplayName(user) : organizerId);
                }
                @Override
                public void onError(Exception e) { /* fallback device ID already shown */ }
            });
        }

        // Co-organizers
        List<String> coOrgIds = event.getCoOrganizerId();
        if (coOrgIds == null || coOrgIds.isEmpty()) return;

        tvCoOrganizersLabel.setVisibility(View.VISIBLE);
        tvCoOrganizers.setVisibility(View.VISIBLE);

        // Resolve each co-organizer device ID to a display name, then join them
        List<String> resolvedNames = new ArrayList<>();
        for (int i = 0; i < coOrgIds.size(); i++) resolvedNames.add(coOrgIds.get(i));

        AtomicInteger remaining = new AtomicInteger(coOrgIds.size());
        UserDB userDB = new UserDB();

        for (int i = 0; i < coOrgIds.size(); i++) {
            final int index = i;
            final String coOrgId = coOrgIds.get(i);
            userDB.getUser(coOrgId, new UserDB.OnUserLoadedListener() {
                @Override
                public void onUserLoaded(@Nullable User user) {
                    if (user != null) resolvedNames.set(index, buildDisplayName(user));
                    if (remaining.decrementAndGet() == 0 && getView() != null) {
                        tvCoOrganizers.setText(String.join(", ", resolvedNames));
                    }
                }
                @Override
                public void onError(Exception e) {
                    if (remaining.decrementAndGet() == 0 && getView() != null) {
                        tvCoOrganizers.setText(String.join(", ", resolvedNames));
                    }
                }
            });
        }
    }

    /** Builds a readable display name from a User, falling back to device ID. */
    private String buildDisplayName(@NonNull User user) {
        String first = user.getFirstName() != null ? user.getFirstName().trim() : "";
        String last = user.getLastName() != null ? user.getLastName().trim() : "";
        String full = (first + " " + last).trim();
        if (!full.isEmpty()) return full;
        if (user.getUserName() != null && !user.getUserName().isEmpty()) return user.getUserName();
        return user.getDeviceId() != null ? user.getDeviceId() : "Unknown";
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (commentsController != null) commentsController.release();
    }
}
