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

public class ImageDB {
    private static FirebaseFirestore db;
    private static CollectionReference imageRef;
    private static boolean setup = false;
    private static ListenerRegistration imagesListener;

    private ImageDB() {
    }

    private static void setup() {
        if (!setup) {
            db = DBConnector.getDb();
            imageRef = db.collection("images");
            setup = true;
        }
    }

    public interface ImageMutationCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    public interface LoadImageCallback {
        void onSuccess(Image image);
        void onFailure(Exception e);
    }

    /**
     * Generate a new document ID from database.
     * @return the new document ID
     */
    public static String genNewId() {
        setup();
        return imageRef.document().getId();
    }

    /**
     * Saves an image into images/{imageDocId}.
     *
     * @param image Image object
     * @param callback Callback after Firestore write
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
     * Loads a Base64 image string from images/{imageDocId}.
     *
     * @param imageDocId Document id in the images collection.
     * @param callback Callback returning constructed image object.
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
     * Callbacks fired by the real-time images listener.
     */
    public interface ImagesChangedCallback {
        void onImageAdded(Image image);
        void onImageModified(Image image);
        void onImageRemoved(Image image);
        void onFailure(Exception e);
    }

    /**
     * Attaches a real-time listener to the "images" collection.
     * Fires the appropriate callback for every add, modify, or remove.
     *
     * @param callback Receives individual change events or errors.
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
     * Detaches the real-time images listener.
     * Call this when the screen is torn down to avoid memory leaks.
     */
    public static void stopImagesListen() {
        if (imagesListener != null) {
            imagesListener.remove();
            imagesListener = null;
        }
    }

    /**
     * Deletes images/{imageDocId}.
     *
     * @param imageDocId Document id in the images collection
     * @param callback Callback after Firestore delete
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