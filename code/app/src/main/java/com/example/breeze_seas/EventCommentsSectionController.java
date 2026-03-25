package com.example.breeze_seas;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * EventCommentsSectionController binds the reusable inline comments section used by event screens.
 *
 * <p>This is UI-only for now. It keeps comment state in a local in-memory store until the real
 * backend contract is ready.
 */
public final class EventCommentsSectionController {

    private static final Map<String, ArrayList<EventCommentUIModel>> MOCK_COMMENTS_BY_EVENT = new HashMap<>();

    private final Fragment hostFragment;
    private final RecyclerView recyclerView;
    private final View emptyStateView;
    private final AppCompatEditText commentInput;
    private final EventCommentsAdapter adapter;

    @Nullable
    private Event currentEvent;

    @Nullable
    private User currentUser;

    private boolean organizerViewer;

    @Nullable
    private Boolean organizerViewerOverride;

    /**
     * Creates a reusable controller for the inline comments section.
     *
     * @param hostFragment Fragment that owns the comments section.
     * @param rootView Root event-screen view containing the included comments layout.
     */
    public EventCommentsSectionController(@NonNull Fragment hostFragment, @NonNull View rootView) {
        this.hostFragment = hostFragment;
        this.recyclerView = rootView.findViewById(R.id.event_comments_recycler);
        this.emptyStateView = rootView.findViewById(R.id.event_comments_empty_state);
        this.commentInput = rootView.findViewById(R.id.event_comments_input);

        recyclerView.setLayoutManager(new LinearLayoutManager(hostFragment.requireContext()));
        recyclerView.setNestedScrollingEnabled(false);

        this.adapter = new EventCommentsAdapter(false, new EventCommentsAdapter.OnDeleteCommentClickListener() {
            /**
             * Opens the delete confirmation dialog for the selected comment.
             *
             * @param comment Comment selected for deletion.
             */
            @Override
            public void onDeleteComment(@NonNull EventCommentUIModel comment) {
                showDeleteCommentDialog(comment);
            }
        });
        recyclerView.setAdapter(adapter);

        Button postButton = rootView.findViewById(R.id.event_comments_post_button);
        postButton.setOnClickListener(new View.OnClickListener() {
            /**
             * Adds a locally stored mock comment to the current event conversation.
             *
             * @param v Post button that was tapped.
             */
            @Override
            public void onClick(View v) {
                postLocalComment();
            }
        });
    }

    /**
     * Attaches the current event/user context and refreshes the visible comments UI.
     *
     * @param event Event whose comments should be displayed.
     * @param user Current signed-in app user, if available.
     */
    public void bind(@Nullable Event event, @Nullable User user) {
        currentEvent = event;
        currentUser = user;
        organizerViewer = organizerViewerOverride != null
                ? organizerViewerOverride
                : isOrganizerViewer();
        adapter.setCanModerateEntrantComments(organizerViewer);
        seedMockCommentsIfNeeded();
        renderComments();
    }

    /**
     * Attaches the current event/user context and explicitly controls organizer moderation mode.
     *
     * @param event Event whose comments should be displayed.
     * @param user Current signed-in app user, if available.
     * @param organizerViewerOverride Whether the current screen should behave as organizer view.
     */
    public void bind(@Nullable Event event, @Nullable User user, boolean organizerViewerOverride) {
        this.organizerViewerOverride = organizerViewerOverride;
        bind(event, user);
    }

    /**
     * Adds a locally stored comment for the current event and refreshes the section.
     */
    private void postLocalComment() {
        if (currentEvent == null) {
            return;
        }

        String body = commentInput.getText() == null ? "" : commentInput.getText().toString().trim();
        if (TextUtils.isEmpty(body)) {
            commentInput.setError(hostFragment.getString(R.string.event_comments_empty_error));
            return;
        }

        ArrayList<EventCommentUIModel> comments = getMutableCommentsForCurrentEvent();
        comments.add(0, new EventCommentUIModel(
                UUID.randomUUID().toString(),
                buildCurrentAuthorName(),
                body,
                buildCurrentTimestampLabel(),
                organizerViewer
        ));

        commentInput.setText("");
        renderComments();
        recyclerView.scrollToPosition(0);
    }

    /**
     * Shows the organizer-only confirmation dialog before deleting an entrant comment.
     *
     * @param comment Comment selected for deletion.
     */
    private void showDeleteCommentDialog(@NonNull EventCommentUIModel comment) {
        if (!hostFragment.isAdded()) {
            return;
        }

        View dialogView = LayoutInflater.from(hostFragment.requireContext())
                .inflate(R.layout.dialog_ticket_confirmation, null, false);

        TextView titleView = dialogView.findViewById(R.id.dialog_title);
        TextView messageView = dialogView.findViewById(R.id.dialog_message);
        Button primaryButton = dialogView.findViewById(R.id.dialog_primary_button);
        Button secondaryButton = dialogView.findViewById(R.id.dialog_secondary_button);

        titleView.setText(R.string.event_comments_delete_title);
        messageView.setText(R.string.event_comments_delete_message);
        primaryButton.setText(R.string.event_comments_delete_confirm);
        secondaryButton.setText(R.string.event_comments_delete_cancel);

        AlertDialog dialog = new AlertDialog.Builder(hostFragment.requireContext())
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        primaryButton.setOnClickListener(new View.OnClickListener() {
            /**
             * Removes the selected comment from the local mock store.
             *
             * @param v Primary confirmation button that was tapped.
             */
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                deleteLocalComment(comment);
            }
        });
        secondaryButton.setOnClickListener(new View.OnClickListener() {
            /**
             * Cancels comment deletion and closes the confirmation dialog.
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
     * Removes a local mock comment for the current event and refreshes the section.
     *
     * @param comment Comment selected for deletion.
     */
    private void deleteLocalComment(@NonNull EventCommentUIModel comment) {
        ArrayList<EventCommentUIModel> comments = getMutableCommentsForCurrentEvent();
        comments.removeIf(existing -> existing.getCommentId().equals(comment.getCommentId()));
        renderComments();
    }

    /**
     * Re-renders the inline comments list and toggles the empty state.
     */
    private void renderComments() {
        List<EventCommentUIModel> comments = getMutableCommentsForCurrentEvent();
        adapter.submitList(new ArrayList<>(comments));

        boolean hasComments = !comments.isEmpty();
        recyclerView.setVisibility(hasComments ? View.VISIBLE : View.GONE);
        emptyStateView.setVisibility(hasComments ? View.GONE : View.VISIBLE);
    }

    /**
     * Seeds local mock comments once per event so the UI is useful before backend hookup.
     */
    private void seedMockCommentsIfNeeded() {
        if (currentEvent == null) {
            return;
        }

        ArrayList<EventCommentUIModel> comments = getMutableCommentsForCurrentEvent();
        if (!comments.isEmpty()) {
            return;
        }

        comments.add(new EventCommentUIModel(
                UUID.randomUUID().toString(),
                hostFragment.getString(R.string.event_comments_sample_organizer_name),
                hostFragment.getString(R.string.event_comments_sample_organizer_body),
                hostFragment.getString(R.string.event_comments_sample_time_recent),
                true
        ));
        comments.add(new EventCommentUIModel(
                UUID.randomUUID().toString(),
                hostFragment.getString(R.string.event_comments_sample_entrant_name),
                hostFragment.getString(R.string.event_comments_sample_entrant_body),
                hostFragment.getString(R.string.event_comments_sample_time_earlier),
                false
        ));
    }

    /**
     * Returns the mutable local mock comment list for the current event key.
     *
     * @return Mutable event-scoped mock comment list.
     */
    @NonNull
    private ArrayList<EventCommentUIModel> getMutableCommentsForCurrentEvent() {
        String eventKey = currentEvent == null || currentEvent.getEventId() == null
                ? "default"
                : currentEvent.getEventId();

        ArrayList<EventCommentUIModel> comments = MOCK_COMMENTS_BY_EVENT.get(eventKey);
        if (comments == null) {
            comments = new ArrayList<>();
            MOCK_COMMENTS_BY_EVENT.put(eventKey, comments);
        }
        return comments;
    }

    /**
     * Determines whether the current viewer is the organizer for the open event.
     *
     * @return {@code true} when the current user is the organizer.
     */
    private boolean isOrganizerViewer() {
        if (currentEvent == null || currentUser == null) {
            return false;
        }

        String organizerId = currentEvent.getOrganizerId();
        String currentDeviceId = currentUser.getDeviceId();
        return organizerId != null && organizerId.equals(currentDeviceId);
    }

    /**
     * Builds the current author's display name for a newly posted local comment.
     *
     * @return Current author label for the composer.
     */
    @NonNull
    private String buildCurrentAuthorName() {
        if (currentUser == null) {
            return organizerViewer
                    ? hostFragment.getString(R.string.event_comments_role_organizer)
                    : hostFragment.getString(R.string.event_comments_role_entrant);
        }

        String firstName = currentUser.getFirstName() == null ? "" : currentUser.getFirstName().trim();
        String lastName = currentUser.getLastName() == null ? "" : currentUser.getLastName().trim();
        String fullName = (firstName + " " + lastName).trim();
        if (!fullName.isEmpty()) {
            return fullName;
        }

        String userName = currentUser.getUserName();
        if (userName != null && !userName.trim().isEmpty()) {
            return userName.trim();
        }

        return organizerViewer
                ? hostFragment.getString(R.string.event_comments_sample_organizer_name)
                : hostFragment.getString(R.string.event_comments_sample_entrant_name);
    }

    /**
     * Builds a short timestamp label for newly added local comments.
     *
     * @return Formatted current timestamp label.
     */
    @NonNull
    private String buildCurrentTimestampLabel() {
        SimpleDateFormat formatter = new SimpleDateFormat("MMM d \u2022 h:mm a", Locale.US);
        return formatter.format(new Date());
    }
}
