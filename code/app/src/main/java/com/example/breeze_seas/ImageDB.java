package com.example.breeze_seas;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class ImageDB {
    private static FirebaseFirestore db;
    private static CollectionReference imageRef;
    private static boolean setup = false;

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
        void onSuccess(String base64);
        void onFailure(Exception e);
    }

    /**
     * Saves a Base64 image string into images/{imageDocId}.
     *
     * @param imageDocId Document id to use in the images collection
     * @param base64 Base64 image string
     * @param callback Callback after Firestore write
     */
    public static void saveImage(String imageDocId, String base64, ImageMutationCallback callback) {
        setup();

        Map<String, Object> imageMap = new HashMap<>();
        imageMap.put("data", base64 == null ? "" : base64);
        imageMap.put("updatedTimestamp", Timestamp.now());

        imageRef.document(imageDocId)
                .set(imageMap, SetOptions.merge())
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Loads a Base64 image string from images/{imageDocId}.
     *
     * @param imageDocId Document id in the images collection
     * @param callback Callback returning Base64 image data
     */
    public static void loadImage(String imageDocId, LoadImageCallback callback) {
        setup();

        if (imageDocId == null || imageDocId.trim().isEmpty()) {
            callback.onSuccess("");
            return;
        }

        imageRef.document(imageDocId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        callback.onSuccess("");
                        return;
                    }

                    String data = documentSnapshot.getString("data");
                    callback.onSuccess(data == null ? "" : data);
                })
                .addOnFailureListener(callback::onFailure);
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