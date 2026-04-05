package com.example.breeze_seas;

import android.graphics.Bitmap;

/**
 * Represents an image stored by the application.
 * <p>
 * Each image has a unique image ID, a compressed Base64 string representation,
 * and a decoded {@link Bitmap} for display purposes.
 * </p>
 */
public class Image {
    private String imageId;
    private String compressedBase64;
    private Bitmap imageData;

    /**
     * Creates an image object from existing database data.
     *
     * @param imageId the unique ID of the image document
     * @param compressedBase64 the compressed Base64 image string
     */
    public Image(String imageId, String compressedBase64) {
        this.imageId = imageId;
        this.compressedBase64 = compressedBase64;
        this.imageData = ImageUtils.base64ToBitmap(this.compressedBase64);
    }

    /**
     * Creates a new image object and automatically generates a new image ID.
     *
     * @param compressedBase64 the compressed Base64 image string
     */
    public Image(String compressedBase64) {
        this.imageId = ImageDB.genNewId();
        this.compressedBase64 = compressedBase64;
        this.imageData = ImageUtils.base64ToBitmap(this.compressedBase64);
    }

    /**
     * Returns the image ID.
     *
     * @return the image ID
     */
    public String getImageId() {
        return imageId;
    }

    /**
     * Sets the image ID.
     *
     * @param imageId the new image ID
     */
    public void setImageId(String imageId) {
        this.imageId = imageId;
    }

    /**
     * Returns the compressed Base64 representation of the image.
     *
     * @return the compressed Base64 image string
     */
    public String getCompressedBase64() {
        return compressedBase64;
    }

    /**
     * Sets the compressed Base64 representation of the image and refreshes
     * the decoded bitmap data.
     *
     * @param compressedBase64 the new compressed Base64 image string
     */
    public void setCompressedBase64(String compressedBase64) {
        this.compressedBase64 = compressedBase64;
        this.imageData = ImageUtils.base64ToBitmap(this.compressedBase64);
    }

    /**
     * Returns the bitmap representation of the image for display.
     *
     * @return the decoded bitmap, or {@code null} if unavailable
     */
    public Bitmap display() {
        return getImageData();
    }

    /**
     * Returns the decoded bitmap image data.
     *
     * @return the bitmap image data, or {@code null} if unavailable
     */
    public Bitmap getImageData() {
        return imageData;
    }

    /**
     * Sets the decoded bitmap image data.
     *
     * @param imageData the bitmap to store
     */
    public void setImageData(Bitmap imageData) {
        this.imageData = imageData;
    }
}