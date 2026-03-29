package com.example.breeze_seas;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.Timestamp;

import java.util.HashMap;
import java.util.Map;

/**
 * EventComment stores one event-scoped comment document from
 * {@code events/{eventId}/comments/{commentId}}.
 */
public final class EventComment {

    private String commentId;
    private String authorDeviceId;
    private String authorDisplayName;
    private String body;
    private boolean authorOrganizer;
    private Timestamp createdTimestamp;

    /**
     * Creates an empty comment model for Firestore hydration.
     */
    public EventComment() {
        this.commentId = "";
        this.authorDeviceId = "";
        this.authorDisplayName = "";
        this.body = "";
        this.authorOrganizer = false;
        this.createdTimestamp = null;
    }

    /**
     * Creates a fully populated event comment model.
     *
     * @param commentId Unique identifier of the comment document.
     * @param authorDeviceId Device identifier of the comment author.
     * @param authorDisplayName Visible author name shown in the UI.
     * @param body Comment text content.
     * @param authorOrganizer Whether the author posted as an organizer.
     * @param createdTimestamp Timestamp when the comment was created.
     */
    public EventComment(
            @NonNull String commentId,
            @NonNull String authorDeviceId,
            @NonNull String authorDisplayName,
            @NonNull String body,
            boolean authorOrganizer,
            @Nullable Timestamp createdTimestamp
    ) {
        this.commentId = commentId;
        this.authorDeviceId = authorDeviceId;
        this.authorDisplayName = authorDisplayName;
        this.body = body;
        this.authorOrganizer = authorOrganizer;
        this.createdTimestamp = createdTimestamp;
    }

    /**
     * Serializes the comment into the Firestore field map used by the comments subcollection.
     *
     * @return Firestore field map for this comment.
     */
    @NonNull
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("commentId", getCommentId());
        map.put("authorDeviceId", getAuthorDeviceId());
        map.put("authorDisplayName", getAuthorDisplayName());
        map.put("body", getBody());
        map.put("authorOrganizer", isAuthorOrganizer());
        map.put("createdTimestamp", getCreatedTimestamp());
        return map;
    }

    /**
     * Returns the stable comment identifier.
     *
     * @return Comment document identifier.
     */
    @NonNull
    public String getCommentId() {
        return commentId == null ? "" : commentId;
    }

    /**
     * Stores the stable comment identifier.
     *
     * @param commentId Comment document identifier.
     */
    public void setCommentId(@Nullable String commentId) {
        this.commentId = commentId == null ? "" : commentId;
    }

    /**
     * Returns the device identifier of the comment author.
     *
     * @return Author device identifier.
     */
    @NonNull
    public String getAuthorDeviceId() {
        return authorDeviceId == null ? "" : authorDeviceId;
    }

    /**
     * Stores the device identifier of the comment author.
     *
     * @param authorDeviceId Author device identifier.
     */
    public void setAuthorDeviceId(@Nullable String authorDeviceId) {
        this.authorDeviceId = authorDeviceId == null ? "" : authorDeviceId;
    }

    /**
     * Returns the visible author label for the comment.
     *
     * @return Display name of the author.
     */
    @NonNull
    public String getAuthorDisplayName() {
        return authorDisplayName == null ? "" : authorDisplayName;
    }

    /**
     * Stores the visible author label for the comment.
     *
     * @param authorDisplayName Display name of the author.
     */
    public void setAuthorDisplayName(@Nullable String authorDisplayName) {
        this.authorDisplayName = authorDisplayName == null ? "" : authorDisplayName;
    }

    /**
     * Returns the text body of the comment.
     *
     * @return Comment body text.
     */
    @NonNull
    public String getBody() {
        return body == null ? "" : body;
    }

    /**
     * Stores the text body of the comment.
     *
     * @param body Comment body text.
     */
    public void setBody(@Nullable String body) {
        this.body = body == null ? "" : body;
    }

    /**
     * Returns whether the comment was authored by an organizer.
     *
     * @return {@code true} when the comment is from an organizer.
     */
    public boolean isAuthorOrganizer() {
        return authorOrganizer;
    }

    /**
     * Stores whether the comment was authored by an organizer.
     *
     * @param authorOrganizer Organizer-author flag.
     */
    public void setAuthorOrganizer(boolean authorOrganizer) {
        this.authorOrganizer = authorOrganizer;
    }

    /**
     * Returns the timestamp when the comment was created.
     *
     * @return Comment creation timestamp, or {@code null} when unavailable.
     */
    @Nullable
    public Timestamp getCreatedTimestamp() {
        return createdTimestamp;
    }

    /**
     * Stores the timestamp when the comment was created.
     *
     * @param createdTimestamp Comment creation timestamp.
     */
    public void setCreatedTimestamp(@Nullable Timestamp createdTimestamp) {
        this.createdTimestamp = createdTimestamp;
    }
}
