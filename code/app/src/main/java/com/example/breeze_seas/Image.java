package com.example.breeze_seas;

import android.graphics.Bitmap;

/**
 * Wrapper class to handle images
 */
public class Image {
    private String imageId;
    private String compressedBase64;
    private Bitmap imageData;

    /**
     * For use when constructing an image object from the database
     */
    public Image(String imageId, String compressedBase64) {
        this.imageId = imageId;
        this.compressedBase64 = compressedBase64;
        // TODO: Take compressedBase64 and construct the bitmap
        this.imageData = null;
    }

    /**
     * For use when creating a new image object (as an organizer)
     */
    public Image(String compressedBase64) {
        // TODO: Generate a new imageId (from ImageDB) (Refer to Event constructor for example)
        this.imageId = null;
        this.compressedBase64 = compressedBase64;
        // TODO: Take compressedBase64 and construct the bitmap
        this.imageData = null;
    }


    public String getImageId() {
        return (imageId == null) ? "" : imageId;  // temporary fix
    }

    public void setImageId(String imageId) {
        this.imageId = imageId;
    }

    public String getCompressedBase64() {
        return compressedBase64;
    }

    public void setCompressedBase64(String compressedBase64) {
        this.compressedBase64 = compressedBase64;
        // TODO: recompute bitmap
    }

    /**
     * Synonym of {@link Image#getImageData()}
     * @return Bitmap of image
     */
    public Bitmap display() {
        return getImageData();
    }

    public Bitmap getImageData() {
        return imageData;
    }

    public void setImageData(Bitmap imageData) {
        this.imageData = imageData;
    }
}
