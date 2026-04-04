package com.example.breeze_seas;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * EventCommentsAdapter renders one event-level comments list for both entrant and organizer flows.
 */
public class EventCommentsAdapter extends RecyclerView.Adapter<EventCommentsAdapter.CommentViewHolder> {

    /**
     * Listener notified when the organizer taps the delete affordance for a comment.
     */
    public interface OnDeleteCommentClickListener {
        /**
         * Requests deletion of the selected comment.
         *
         * @param comment Comment row selected for deletion.
         */
        void onDeleteComment(@NonNull EventCommentUIModel comment);
    }

    private final List<EventCommentUIModel> comments = new ArrayList<>();
    private boolean canModerateEntrantComments;
    private boolean canAdminComments;
    private final OnDeleteCommentClickListener deleteCommentClickListener;

    /**
     * Creates the shared comments adapter.
     *
     * @param canModerateEntrantComments Whether delete controls should be shown for entrant comments.
     * @param deleteCommentClickListener Listener for organizer delete actions.
     */
    public EventCommentsAdapter(
            boolean canModerateEntrantComments,
            @NonNull OnDeleteCommentClickListener deleteCommentClickListener
    ) {
        this.canModerateEntrantComments = canModerateEntrantComments;
        this.deleteCommentClickListener = deleteCommentClickListener;
    }

    /**
     * Updates whether organizer-only delete controls should be shown for entrant comments.
     *
     * @param canModerateEntrantComments {@code true} when the current viewer can moderate comments.
     */
    public void setCanModerateEntrantComments(boolean canModerateEntrantComments) {
        this.canModerateEntrantComments = canModerateEntrantComments;
        notifyDataSetChanged();
    }

    /**
     * Updates whether admin delete controls should be shown on all comments.
     * When {@code true}, delete is visible on both entrant and organizer comments.
     * Intended for admins who can remove any comment regardless of author role.
     *
     * @param canAdminComments {@code true} when the current viewer can delete any comment.
     */
    public void setCanAdminComments(boolean canAdminComments) {
        this.canAdminComments = canAdminComments;
        notifyDataSetChanged();
    }

    /**
     * Replaces the adapter contents with a new comment snapshot.
     *
     * @param newComments Latest comment rows to display.
     */
    public void submitList(@NonNull List<EventCommentUIModel> newComments) {
        comments.clear();
        comments.addAll(newComments);
        notifyDataSetChanged();
    }

    /**
     * Inflates one comment row.
     *
     * @param parent RecyclerView that will host the new row.
     * @param viewType Adapter view type for the requested row.
     * @return ViewHolder bound to the inflated comment item.
     */
    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_event_comment, parent, false);
        return new CommentViewHolder(view);
    }

    /**
     * Binds one comment row into the visible list.
     *
     * @param holder ViewHolder receiving the comment data.
     * @param position Adapter position being bound.
     */
    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        EventCommentUIModel comment = comments.get(position);

        holder.authorNameView.setText(comment.getAuthorDisplayName());
        holder.bodyView.setText(comment.getBody());
        holder.timeView.setText(comment.getTimestampLabel());

        if (comment.isAuthorOrganizer()) {
            holder.roleChipView.setText(R.string.event_comments_role_organizer);
            holder.roleChipView.setBackgroundResource(R.drawable.bg_ticket_status_solid);
            holder.roleChipView.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.white));
        } else {
            holder.roleChipView.setText(R.string.event_comments_role_entrant);
            holder.roleChipView.setBackgroundResource(R.drawable.bg_ticket_status_outline);
            holder.roleChipView.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.text_primary));
        }

        boolean showDelete = canAdminComments || (canModerateEntrantComments && !comment.isAuthorOrganizer());
        holder.deleteView.setVisibility(showDelete ? View.VISIBLE : View.GONE);
        holder.deleteView.setOnClickListener(showDelete
                ? new View.OnClickListener() {
                    /**
                     * Requests deletion of the tapped entrant comment.
                     *
                     * @param v Delete affordance that was tapped.
                     */
                    @Override
                    public void onClick(View v) {
                        deleteCommentClickListener.onDeleteComment(comment);
                    }
                }
                : null);
    }

    /**
     * Returns the number of visible comments.
     *
     * @return Comment row count.
     */
    @Override
    public int getItemCount() {
        return comments.size();
    }

    /**
     * ViewHolder that caches the views for one comment row.
     */
    static class CommentViewHolder extends RecyclerView.ViewHolder {
        private final TextView authorNameView;
        private final TextView roleChipView;
        private final TextView deleteView;
        private final TextView bodyView;
        private final TextView timeView;

        /**
         * Creates a ViewHolder for one comment item view.
         *
         * @param itemView Inflated row view associated with this holder.
         */
        CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            authorNameView = itemView.findViewById(R.id.event_comment_author_name);
            roleChipView = itemView.findViewById(R.id.event_comment_role_chip);
            deleteView = itemView.findViewById(R.id.event_comment_delete);
            bodyView = itemView.findViewById(R.id.event_comment_body);
            timeView = itemView.findViewById(R.id.event_comment_time);
        }
    }
}
