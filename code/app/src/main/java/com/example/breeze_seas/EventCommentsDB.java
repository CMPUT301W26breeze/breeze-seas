package com.example.breeze_seas;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * EventCommentsDB provides event-scoped Firestore access for realtime comment viewing,
 * posting, and organizer deletion.
 */
public final class EventCommentsDB {

    /**
     * Callback used when the current event's comments change in realtime.
     */
    public interface CommentsUpdatedCallback {
        /**
         * Returns the latest ordered comments for the current event.
         *
         * @param comments Latest comments snapshot for the active event.
         */
        void onUpdated(@NonNull List<EventComment> comments);

        /**
         * Reports a realtime-listener failure.
         *
         * @param e Firestore failure encountered while listening.
         */
        void onFailure(@NonNull Exception e);
    }

    /**
     * Callback used for individual comment mutations.
     */
    public interface CommentMutationCallback {
        /**
         * Reports that the requested mutation completed successfully.
         */
        void onSuccess();

        /**
         * Reports that the requested mutation failed.
         *
         * @param e Firestore failure returned by the mutation.
         */
        void onFailure(@NonNull Exception e);
    }

    private final FirebaseFirestore db;

    @Nullable
    private ListenerRegistration commentsListener;

    /**
     * Creates a Firestore-backed comments data helper.
     */
    public EventCommentsDB() {
        this.db = DBConnector.getDb();
    }

    /**
     * Starts listening for realtime comments updates on one event.
     *
     * @param eventId Identifier of the event whose comments should be observed.
     * @param callback Listener that receives updated comment snapshots and errors.
     */
    public void startCommentsListen(
            @NonNull String eventId,
            @NonNull CommentsUpdatedCallback callback
    ) {
        stopCommentsListen();

        commentsListener = getCommentsRef(eventId)
                .orderBy("createdTimestamp", Query.Direction.DESCENDING)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    /**
                     * Maps the latest Firestore comments snapshot into app comment models.
                     *
                     * @param commentsDocs Snapshot of the current event comments collection.
                     * @param error Firestore listener error, or {@code null} on success.
                     */
                    @Override
                    public void onEvent(
                            @Nullable QuerySnapshot commentsDocs,
                            @Nullable FirebaseFirestoreException error
                    ) {
                        if (error != null) {
                            callback.onFailure(error);
                            return;
                        }

                        ArrayList<EventComment> comments = new ArrayList<>();
                        if (commentsDocs != null) {
                            for (DocumentSnapshot commentDoc : commentsDocs.getDocuments()) {
                                comments.add(fromDocument(commentDoc));
                            }
                        }
                        callback.onUpdated(comments);
                    }
                });
    }

    /**
     * Stops the active realtime comments listener, if one exists.
     */
    public void stopCommentsListen() {
        if (commentsListener != null) {
            commentsListener.remove();
            commentsListener = null;
        }
    }

    /**
     * Adds one new comment to the given event's comments subcollection.
     *
     * @param event Event receiving the new comment.
     * @param user Current app user who is posting the comment.
     * @param body Comment text content to save.
     * @param authorOrganizer Whether the author is posting as an organizer.
     * @param callback Listener notified when the mutation succeeds or fails.
     */
    public void addComment(
            @NonNull Event event,
            @Nullable User user,
            @NonNull String body,
            boolean authorOrganizer,
            @NonNull CommentMutationCallback callback
    ) {
        String eventId = event.getEventId();
        if (TextUtils.isEmpty(eventId)) {
            callback.onFailure(new IllegalArgumentException("Event ID is required to add a comment."));
            return;
        }

        DocumentReference commentRef = getCommentsRef(eventId).document();
        Timestamp now = Timestamp.now();
        EventComment comment = new EventComment(
                commentRef.getId(),
                buildAuthorDeviceId(user),
                buildAuthorDisplayName(user, authorOrganizer),
                body.trim(),
                authorOrganizer,
                now
        );

        commentRef.set(comment.toMap())
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Deletes one comment from the given event's comments subcollection.
     *
     * @param eventId Identifier of the event that owns the comment.
     * @param commentId Identifier of the comment document to remove.
     * @param callback Listener notified when the mutation succeeds or fails.
     */
    public void deleteComment(
            @NonNull String eventId,
            @NonNull String commentId,
            @NonNull CommentMutationCallback callback
    ) {
        if (TextUtils.isEmpty(eventId) || TextUtils.isEmpty(commentId)) {
            callback.onFailure(new IllegalArgumentException("Event ID and comment ID are required to delete a comment."));
            return;
        }

        getCommentsRef(eventId)
                .document(commentId)
                .delete()
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Returns the Firestore comments collection under one event document.
     *
     * @param eventId Identifier of the parent event.
     * @return Firestore collection reference for that event's comments.
     */
    @NonNull
    private CollectionReference getCommentsRef(@NonNull String eventId) {
        return db.collection("events")
                .document(eventId)
                .collection("comments");
    }

    /**
     * Converts one Firestore document into a resilient event comment model.
     *
     * @param commentDoc Firestore comment document snapshot.
     * @return Event comment model mapped from the snapshot fields.
     */
    @NonNull
    private EventComment fromDocument(@NonNull DocumentSnapshot commentDoc) {
        EventComment comment = commentDoc.toObject(EventComment.class);
        if (comment == null) {
            comment = new EventComment();
        }

        if (TextUtils.isEmpty(comment.getCommentId())) {
            comment.setCommentId(commentDoc.getId());
        }
        if (TextUtils.isEmpty(comment.getAuthorDisplayName())) {
            comment.setAuthorDisplayName("Guest");
        }
        return comment;
    }

    /**
     * Builds the stored author device identifier for a comment mutation.
     *
     * @param user Current app user, if available.
     * @return Device identifier to store with the comment.
     */
    @NonNull
    private String buildAuthorDeviceId(@Nullable User user) {
        if (user == null || user.getDeviceId() == null) {
            return "";
        }
        return user.getDeviceId();
    }

    /**
     * Builds the stored display name for a comment author.
     *
     * @param user Current app user, if available.
     * @param authorOrganizer Whether the user is posting as an organizer.
     * @return Display-ready author label to store with the comment.
     */
    @NonNull
    private String buildAuthorDisplayName(@Nullable User user, boolean authorOrganizer) {
        if (user == null) {
            return authorOrganizer ? "Organizer" : "Entrant";
        }

        String firstName = user.getFirstName() == null ? "" : user.getFirstName().trim();
        String lastName = user.getLastName() == null ? "" : user.getLastName().trim();
        String fullName = (firstName + " " + lastName).trim();
        if (!fullName.isEmpty()) {
            return fullName;
        }

        String userName = user.getUserName();
        if (!TextUtils.isEmpty(userName)) {
            return userName.trim();
        }

        return authorOrganizer ? "Organizer" : "Entrant";
    }
}
