package com.example.breeze_seas;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

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
import java.util.List;
import java.util.Locale;

/**
 * EventCommentsSectionController binds the reusable inline comments section used by event screens.
 */
public final class EventCommentsSectionController {

    private final Fragment hostFragment;
    private final RecyclerView recyclerView;
    private final View emptyStateView;
    private final AppCompatEditText commentInput;
    private final EventCommentsAdapter adapter;
    private final EventCommentsDB commentsDb;
    private final ArrayList<EventCommentUIModel> visibleComments;

    @Nullable
    private Event currentEvent;

    @Nullable
    private User currentUser;

    private boolean organizerViewer;

    @Nullable
    private Boolean organizerViewerOverride;

    @Nullable
    private String listeningEventId;

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
        this.commentsDb = new EventCommentsDB();
        this.visibleComments = new ArrayList<>();

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
             * Posts a new comment to the current event conversation.
             *
             * @param v Post button that was tapped.
             */
            @Override
            public void onClick(View v) {
                postComment();
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
        startListeningForCurrentEvent();
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
     * Stops the realtime listener owned by this controller.
     */
    public void release() {
        commentsDb.stopCommentsListen();
        listeningEventId = null;
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
             * Removes the selected comment from the current event comment thread.
             *
             * @param v Primary confirmation button that was tapped.
             */
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                deleteComment(comment);
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
     * Starts a realtime comments listener for the currently bound event.
     *
     */
    private void startListeningForCurrentEvent() {
        String eventId = currentEvent == null ? null : currentEvent.getEventId();
        if (TextUtils.isEmpty(eventId)) {
            commentsDb.stopCommentsListen();
            listeningEventId = null;
            visibleComments.clear();
            renderComments();
            return;
        }

        if (eventId.equals(listeningEventId)) {
            renderComments();
            return;
        }

        visibleComments.clear();
        renderComments();
        listeningEventId = eventId;
        commentsDb.startCommentsListen(eventId, new EventCommentsDB.CommentsUpdatedCallback() {
            /**
             * Converts the latest Firestore comments snapshot into visible UI models.
             *
             * @param comments Latest event comments returned by Firestore.
             */
            @Override
            public void onUpdated(@NonNull List<EventComment> comments) {
                if (hostFragment.getView() == null) {
                    return;
                }
                visibleComments.clear();
                for (EventComment comment : comments) {
                    visibleComments.add(new EventCommentUIModel(
                            comment.getCommentId(),
                            comment.getAuthorDisplayName(),
                            comment.getBody(),
                            buildTimestampLabel(comment),
                            comment.isAuthorOrganizer()
                    ));
                }
                renderComments();
            }

            /**
             * Reports that the realtime comments listener failed.
             *
             * @param e Firestore listener failure.
             */
            @Override
            public void onFailure(@NonNull Exception e) {
                if (!hostFragment.isAdded()) {
                    return;
                }
                Toast.makeText(
                        hostFragment.requireContext(),
                        R.string.event_comments_load_failure,
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }

    /**
     * Posts a new comment to the current event conversation.
     */
    private void postComment() {
        if (currentEvent == null) {
            return;
        }

        String body = commentInput.getText() == null ? "" : commentInput.getText().toString().trim();
        if (TextUtils.isEmpty(body)) {
            commentInput.setError(hostFragment.getString(R.string.event_comments_empty_error));
            return;
        }

        commentsDb.addComment(currentEvent, currentUser, body, organizerViewer, new EventCommentsDB.CommentMutationCallback() {
            /**
             * Clears the composer after the comment is saved successfully.
             */
            @Override
            public void onSuccess() {
                if (hostFragment.getView() == null) {
                    return;
                }
                commentInput.setText("");
                recyclerView.scrollToPosition(0);
            }

            /**
             * Reports that saving the new comment failed.
             *
             * @param e Firestore mutation failure.
             */
            @Override
            public void onFailure(@NonNull Exception e) {
                if (!hostFragment.isAdded()) {
                    return;
                }
                Toast.makeText(
                        hostFragment.requireContext(),
                        R.string.event_comments_post_failure,
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }

    /**
     * Deletes the selected comment from the current event conversation.
     *
     * @param comment Comment selected for deletion.
     */
    private void deleteComment(@NonNull EventCommentUIModel comment) {
        if (currentEvent == null || TextUtils.isEmpty(currentEvent.getEventId())) {
            return;
        }

        commentsDb.deleteComment(currentEvent.getEventId(), comment.getCommentId(), new EventCommentsDB.CommentMutationCallback() {
            /**
             * Waits for the realtime listener to remove the deleted comment row.
             */
            @Override
            public void onSuccess() {
                // No-op. The realtime listener updates the UI.
            }

            /**
             * Reports that deleting the selected comment failed.
             *
             * @param e Firestore mutation failure.
             */
            @Override
            public void onFailure(@NonNull Exception e) {
                if (!hostFragment.isAdded()) {
                    return;
                }
                Toast.makeText(
                        hostFragment.requireContext(),
                        R.string.event_comments_delete_failure,
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }

    /**
     * Re-renders the inline comments list and toggles the empty state.
     */
    private void renderComments() {
        adapter.submitList(new ArrayList<>(visibleComments));

        boolean hasComments = !visibleComments.isEmpty();
        recyclerView.setVisibility(hasComments ? View.VISIBLE : View.GONE);
        emptyStateView.setVisibility(hasComments ? View.GONE : View.VISIBLE);
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
     * Builds the visible timestamp label for one Firestore-backed comment.
     *
     * @param comment Comment whose timestamp should be formatted.
     * @return Short timestamp label for the comment row.
     */
    @NonNull
    private String buildTimestampLabel(@NonNull EventComment comment) {
        if (comment.getCreatedTimestamp() == null) {
            return hostFragment.getString(R.string.event_comments_time_now);
        }
        SimpleDateFormat formatter = new SimpleDateFormat("MMM d \u2022 h:mm a", Locale.US);
        return formatter.format(new Date(comment.getCreatedTimestamp().toDate().getTime()));
    }
}
