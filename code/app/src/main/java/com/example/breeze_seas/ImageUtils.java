package com.example.breeze_seas;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Utility methods for image compression, Base64 conversion, and bitmap decoding.
 * This class centralizes all image-specific logic so UI classes do not need to
 * manage compression rules or decoding details directly.
 */
public final class ImageUtils {

    /**
     * Safe default max size for the Base64 string stored in Firestore.
     * Kept below Firestore document limit to leave room for other fields.
     */
    private static final int MAX_BASE64_BYTES = 700 * 1024;

    /**
     * Safe default max image dimension before compression.
     */
    private static final int MAX_DIMENSION = 1280;

    private ImageUtils() {
        // Utility class
    }

    /**
     * Converts a Uri image into a compressed Base64 string using default limits.
     *
     * @param context Context used to open the Uri stream.
     * @param uri Selected image Uri.
     * @return Compressed Base64 string.
     * @throws IOException When the image cannot be read or compressed.
     */
    public static String uriToCompressedBase64(Context context, Uri uri) throws IOException {
        Bitmap bitmap = decodeSampledBitmapFromUri(context, uri, MAX_DIMENSION, MAX_DIMENSION);
        return bitmapToCompressedBase64(bitmap, MAX_BASE64_BYTES);
    }

    /**
     * Converts a Uri image into a compressed Base64 string using custom limits.
     *
     * @param context Context used to open the Uri stream.
     * @param uri Selected image Uri.
     * @param maxDimension Maximum dimension for width/height.
     * @param maxBase64Bytes Maximum allowed Base64 size in bytes.
     * @return Compressed Base64 string.
     * @throws IOException When the image cannot be read or compressed.
     */
    public static String uriToCompressedBase64(Context context,
                                               Uri uri,
                                               int maxDimension,
                                               int maxBase64Bytes) throws IOException {
        Bitmap bitmap = decodeSampledBitmapFromUri(context, uri, maxDimension, maxDimension);
        return bitmapToCompressedBase64(bitmap, maxBase64Bytes);
    }

    /**
     * Compresses a bitmap until its Base64 representation fits under the size limit.
     *
     * @param bitmap Source bitmap.
     * @param maxBase64Bytes Maximum allowed Base64 size in bytes.
     * @return Compressed Base64 string.
     * @throws IOException When compression fails.
     */
    public static String bitmapToCompressedBase64(Bitmap bitmap, int maxBase64Bytes) throws IOException {
        if (bitmap == null) {
            throw new IOException("Bitmap is null.");
        }

        Bitmap currentBitmap = bitmap;
        int quality = 90;

        while (true) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            boolean compressed = currentBitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos);
            if (!compressed) {
                throw new IOException("Failed to compress bitmap.");
            }

            byte[] jpegBytes = baos.toByteArray();
            String base64 = Base64.encodeToString(jpegBytes, Base64.NO_WRAP);
            int base64Size = base64.getBytes(StandardCharsets.UTF_8).length;

            if (base64Size <= maxBase64Bytes) {
                return base64;
            }

            // First reduce quality
            if (quality > 40) {
                quality -= 10;
                continue;
            }

            // Then reduce dimensions
            int newWidth = Math.max(300, Math.round(currentBitmap.getWidth() * 0.85f));
            int newHeight = Math.max(300, Math.round(currentBitmap.getHeight() * 0.85f));

            if (newWidth == currentBitmap.getWidth() && newHeight == currentBitmap.getHeight()) {
                throw new IOException("Image is still too large after compression.");
            }

            currentBitmap = Bitmap.createScaledBitmap(currentBitmap, newWidth, newHeight, true);
            quality = 85;
        }
    }

    /**
     * Decodes a Base64 image string into a bitmap.
     *
     * @param base64 Base64 image string.
     * @return Decoded bitmap, or null if empty/invalid.
     */
    public static Bitmap base64ToBitmap(String base64) {
        if (base64 == null || base64.trim().isEmpty()) {
            return null;
        }

        try {
            byte[] bytes = Base64.decode(base64.trim(), Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Decodes a Base64 image string into raw bytes.
     *
     * @param base64 Base64 image string.
     * @return Decoded bytes, or null if empty/invalid.
     */
    public static byte[] base64ToBytes(String base64) {
        if (base64 == null || base64.trim().isEmpty()) {
            return null;
        }

        try {
            return Base64.decode(base64.trim(), Base64.DEFAULT);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Decodes and down-samples an image from a content Uri.
     *
     * @param context Context used to open the Uri stream.
     * @param uri Source image Uri.
     * @param reqWidth Requested max width.
     * @param reqHeight Requested max height.
     * @return Decoded bitmap.
     * @throws IOException When the image cannot be opened or decoded.
     */
    public static Bitmap decodeSampledBitmapFromUri(Context context,
                                                    Uri uri,
                                                    int reqWidth,
                                                    int reqHeight) throws IOException {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;

        InputStream boundsStream = context.getContentResolver().openInputStream(uri);
        if (boundsStream == null) {
            throw new IOException("Cannot open image stream.");
        }

        try {
            BitmapFactory.decodeStream(boundsStream, null, bounds);
        } finally {
            boundsStream.close();
        }

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = calculateInSampleSize(bounds, reqWidth, reqHeight);
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;

        InputStream decodeStream = context.getContentResolver().openInputStream(uri);
        if (decodeStream == null) {
            throw new IOException("Cannot open image stream.");
        }

        Bitmap decoded;
        try {
            decoded = BitmapFactory.decodeStream(decodeStream, null, options);
        } finally {
            decodeStream.close();
        }

        if (decoded == null) {
            throw new IOException("Failed to decode selected image.");
        }

        return scaleDown(decoded, reqWidth, reqHeight);
    }

    /**
     * Calculates a sample size for efficient bitmap decoding.
     *
     * @param options Bitmap bounds options.
     * @param reqWidth Requested max width.
     * @param reqHeight Requested max height.
     * @return Sample size to use.
     */
    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int height = options.outHeight;
        int width = options.outWidth;
        int inSampleSize = 1;

        while ((height / inSampleSize) > reqHeight * 2 || (width / inSampleSize) > reqWidth * 2) {
            inSampleSize *= 2;
        }

        return Math.max(1, inSampleSize);
    }

    /**
     * Scales a bitmap down to fit within the specified bounds.
     *
     * @param src Source bitmap.
     * @param maxWidth Maximum width.
     * @param maxHeight Maximum height.
     * @return Scaled bitmap.
     */
    public static Bitmap scaleDown(Bitmap src, int maxWidth, int maxHeight) {
        if (src == null) {
            return null;
        }

        float ratio = Math.min((float) maxWidth / src.getWidth(), (float) maxHeight / src.getHeight());

        if (ratio >= 1.0f) {
            return src;
        }

        int newWidth = Math.round(src.getWidth() * ratio);
        int newHeight = Math.round(src.getHeight() * ratio);
        return Bitmap.createScaledBitmap(src, newWidth, newHeight, true);
    }

    /**
     * Returns the default max Base64 size used by this utility.
     *
     * @return Default Base64 byte limit.
     */
    public static int getDefaultMaxBase64Bytes() {
        return MAX_BASE64_BYTES;
    }

    /**
     * Returns the default max dimension used by this utility.
     *
     * @return Default max dimension.
     */
    public static int getDefaultMaxDimension() {
        return MAX_DIMENSION;
    }
}