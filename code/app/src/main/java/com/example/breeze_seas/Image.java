package com.example.breeze_seas;

import android.graphics.Bitmap;

import androidx.camera.core.internal.utils.ImageUtil;

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
        this.imageData = ImageUtils.base64ToBitmap(this.compressedBase64);
    }

    /**
     * For use when creating a new image object (as an organizer)
     */
    public Image(String compressedBase64) {
        this.imageId = ImageDB.genNewId();
        this.compressedBase64 = compressedBase64;
        this.imageData = ImageUtils.base64ToBitmap(this.compressedBase64);
    }

    public String getImageId() {
        return imageId;
    }

    public void setImageId(String imageId) {
        this.imageId = imageId;
    }

    public String getCompressedBase64() {
        return compressedBase64;
    }

    public void setCompressedBase64(String compressedBase64) {
        this.compressedBase64 = compressedBase64;
        this.imageData = ImageUtils.base64ToBitmap(this.compressedBase64);
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
