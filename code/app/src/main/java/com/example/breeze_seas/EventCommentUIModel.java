package com.example.breeze_seas;

import androidx.annotation.NonNull;

/**
 * EventCommentUIModel stores the display-ready information for one event comment row.
 */
public final class EventCommentUIModel {

    private final String commentId;
    private final String authorDisplayName;
    private final String body;
    private final String timestampLabel;
    private final boolean authorIsOrganizer;

    /**
     * Creates one comment row model for the shared comments UI.
     *
     * @param commentId Stable identifier for the comment row.
     * @param authorDisplayName Visible author label shown in the UI.
     * @param body Comment text content.
     * @param timestampLabel Human-readable time label for the comment.
     * @param authorIsOrganizer Whether the comment was authored by the organizer.
     */
    public EventCommentUIModel(
            @NonNull String commentId,
            @NonNull String authorDisplayName,
            @NonNull String body,
            @NonNull String timestampLabel,
            boolean authorIsOrganizer
    ) {
        this.commentId = commentId;
        this.authorDisplayName = authorDisplayName;
        this.body = body;
        this.timestampLabel = timestampLabel;
        this.authorIsOrganizer = authorIsOrganizer;
    }

    /**
     * Returns the stable identifier for the comment row.
     *
     * @return Comment identifier.
     */
    @NonNull
    public String getCommentId() {
        return commentId;
    }

    /**
     * Returns the display name shown for the comment author.
     *
     * @return Author display name.
     */
    @NonNull
    public String getAuthorDisplayName() {
        return authorDisplayName;
    }

    /**
     * Returns the visible comment body text.
     *
     * @return Comment body.
     */
    @NonNull
    public String getBody() {
        return body;
    }

    /**
     * Returns the visible timestamp label for the comment.
     *
     * @return Formatted timestamp label.
     */
    @NonNull
    public String getTimestampLabel() {
        return timestampLabel;
    }

    /**
     * Returns whether this comment was authored by the organizer.
     *
     * @return {@code true} when the author is the organizer.
     */
    public boolean isAuthorOrganizer() {
        return authorIsOrganizer;
    }
}
