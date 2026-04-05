package com.example.breeze_seas;

import androidx.annotation.Nullable;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

/**
 * Provides Firestore database operations for image documents.
 * <p>
 * This class is responsible for managing image records stored in the
 * {@code images} collection. It supports generating new image document IDs,
 * saving image data, loading image data, deleting images, and listening for
 * real-time updates from Firestore.
 * </p>
 * <p>
 * All methods are static because this class acts as a utility-style database
 * access layer for image-related operations.
 * </p>
 */
public class ImageDB {
    private static FirebaseFirestore db;
    private static CollectionReference imageRef;
    private static boolean setup = false;
    private static ListenerRegistration imagesListener;

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private ImageDB() {
    }

    /**
     * Initializes Firestore references for the images collection if they have
     * not already been set up.
     */
    private static void setup() {
        if (!setup) {
            db = DBConnector.getDb();
            imageRef = db.collection("images");
            setup = true;
        }
    }

    /**
     * Callback interface for image write and delete operations.
     */
    public interface ImageMutationCallback {
        /**
         * Called when the mutation operation completes successfully.
         */
        void onSuccess();

        /**
         * Called when the mutation operation fails.
         *
         * @param e the exception describing the failure
         */
        void onFailure(Exception e);
    }

    /**
     * Callback interface for loading a single image.
     */
    public interface LoadImageCallback {
        /**
         * Called when the image is loaded successfully.
         *
         * @param image the loaded image, or {@code null} if no image exists
         *              for the provided document ID
         */
        void onSuccess(Image image);

        /**
         * Called when the image load operation fails.
         *
         * @param e the exception describing the failure
         */
        void onFailure(Exception e);
    }

    /**
     * Generates a new document ID from the Firestore images collection.
     *
     * @return the newly generated document ID
     */
    public static String genNewId() {
        setup();
        return imageRef.document().getId();
    }

    /**
     * Saves an image into {@code images/{imageDocId}}.
     * <p>
     * The image data is stored under the {@code data} field, and the current
     * timestamp is stored under {@code updatedTimestamp}. Existing fields are
     * preserved because the write uses merge semantics.
     * </p>
     *
     * @param image the image object to save
     * @param callback the callback to invoke after the Firestore write succeeds
     *                 or fails
     */
    public static void saveImage(Image image, ImageMutationCallback callback) {
        setup();

        Map<String, Object> imageMap = new HashMap<>();
        imageMap.put("data", image.getCompressedBase64() == null ? "" : image.getCompressedBase64());
        imageMap.put("updatedTimestamp", Timestamp.now());

        imageRef.document(image.getImageId())
                .set(imageMap, SetOptions.merge())
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Loads a Base64 image string from {@code images/{imageDocId}} and
     * constructs an {@link Image} object from the retrieved data.
     *
     * @param imageDocId the document ID in the images collection
     * @param callback the callback that receives the loaded image or an error
     */
    public static void loadImage(String imageDocId, LoadImageCallback callback) {
        setup();

        if (imageDocId == null || imageDocId.trim().isEmpty()) {
            callback.onSuccess(null);
            return;
        }

        imageRef.document(imageDocId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        callback.onSuccess(null);
                        return;
                    }

                    String data = documentSnapshot.getString("data");
                    callback.onSuccess(new Image(imageDocId, data));
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Callback interface for real-time image collection updates.
     */
    public interface ImagesChangedCallback {
        /**
         * Called when a new image document is added.
         *
         * @param image the added image
         */
        void onImageAdded(Image image);

        /**
         * Called when an existing image document is modified.
         *
         * @param image the modified image
         */
        void onImageModified(Image image);

        /**
         * Called when an image document is removed.
         *
         * @param image the removed image
         */
        void onImageRemoved(Image image);

        /**
         * Called when the Firestore listener encounters an error.
         *
         * @param e the exception describing the failure
         */
        void onFailure(Exception e);
    }

    /**
     * Attaches a real-time listener to the {@code images} collection.
     * <p>
     * For each document change received from Firestore, the corresponding
     * callback method is invoked based on whether the image was added,
     * modified, or removed.
     * </p>
     *
     * @param callback receives individual change events or listener errors
     */
    public static void startImagesListen(ImagesChangedCallback callback) {
        setup();
        imagesListener = imageRef.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot snapshots,
                                @Nullable FirebaseFirestoreException error) {
                if (error != null) {
                    callback.onFailure(error);
                    return;
                }
                if (snapshots == null) return;

                for (DocumentChange dc : snapshots.getDocumentChanges()) {
                    String data = dc.getDocument().getString("data");
                    Image image = new Image(dc.getDocument().getId(), data);
                    switch (dc.getType()) {
                        case ADDED:    callback.onImageAdded(image);    break;
                        case MODIFIED: callback.onImageModified(image); break;
                        case REMOVED:  callback.onImageRemoved(image);  break;
                    }
                }
            }
        });
    }

    /**
     * Detaches the real-time images listener if one is currently active.
     * <p>
     * This should be called when the associated screen or component is being
     * torn down in order to avoid memory leaks and unnecessary Firestore
     * updates.
     * </p>
     */
    public static void stopImagesListen() {
        if (imagesListener != null) {
            imagesListener.remove();
            imagesListener = null;
        }
    }

    /**
     * Deletes {@code images/{imageDocId}} from the Firestore images collection.
     *
     * @param imageDocId the document ID in the images collection
     * @param callback the callback to invoke after the delete succeeds or fails
     */
    public static void deleteImage(String imageDocId, ImageMutationCallback callback) {
        setup();

        if (imageDocId == null || imageDocId.trim().isEmpty()) {
            callback.onSuccess();
            return;
        }

        imageRef.document(imageDocId)
                .delete()
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }
}