package com.example.breeze_seas;

import static org.junit.Assert.*;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@RunWith(RobolectricTestRunner.class)
public class ImageUtilsTest {

    private String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        return Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);
    }

    @Test
    public void base64ToBytes_validBase64_returnsBytes() {
        String text = "hello";
        String base64 = Base64.encodeToString(text.getBytes(), Base64.NO_WRAP);

        byte[] result = ImageUtils.base64ToBytes(base64);

        assertNotNull(result);
        assertEquals("hello", new String(result));
    }

    @Test
    public void base64ToBytes_null_returnsNull() {
        assertNull(ImageUtils.base64ToBytes(null));
    }

    @Test
    public void base64ToBytes_invalidBase64_returnsNull() {
        assertNull(ImageUtils.base64ToBytes("%%%not_base64%%%"));
    }

    @Test
    public void base64ToBitmap_validBase64_returnsBitmap() {
        Bitmap original = Bitmap.createBitmap(20, 20, Bitmap.Config.ARGB_8888);
        String base64 = bitmapToBase64(original);

        Bitmap result = ImageUtils.base64ToBitmap(base64);

        assertNotNull(result);
        assertEquals(20, result.getWidth());
        assertEquals(20, result.getHeight());
    }

    @Test
    public void base64ToBitmap_emptyString_returnsNull() {
        assertNull(ImageUtils.base64ToBitmap(""));
    }

    @Test
    public void scaleDown_null_returnsNull() {
        assertNull(ImageUtils.scaleDown(null, 100, 100));
    }

    @Test
    public void scaleDown_smallBitmap_returnsSameBitmap() {
        Bitmap bitmap = Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888);

        Bitmap result = ImageUtils.scaleDown(bitmap, 100, 100);

        assertSame(bitmap, result);
    }

    @Test
    public void scaleDown_largeBitmap_scalesCorrectly() {
        Bitmap bitmap = Bitmap.createBitmap(400, 200, Bitmap.Config.ARGB_8888);

        Bitmap result = ImageUtils.scaleDown(bitmap, 100, 100);

        assertNotNull(result);
        assertEquals(100, result.getWidth());
        assertEquals(50, result.getHeight());
    }

    @Test
    public void calculateInSampleSize_largeImage_returnsGreaterThanOne() {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.outWidth = 4000;
        options.outHeight = 3000;

        int sampleSize = ImageUtils.calculateInSampleSize(options, 1000, 1000);

        assertTrue(sampleSize > 1);
    }

    @Test
    public void calculateInSampleSize_smallImage_returnsOne() {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.outWidth = 500;
        options.outHeight = 500;

        int sampleSize = ImageUtils.calculateInSampleSize(options, 1000, 1000);

        assertEquals(1, sampleSize);
    }

    @Test
    public void bitmapToCompressedBase64_nullBitmap_throwsIOException() {
        try {
            ImageUtils.bitmapToCompressedBase64(null, 1024);
            fail("Expected IOException to be thrown");
        } catch (IOException e) {
            assertEquals("Bitmap is null.", e.getMessage());
        }
    }

    @Test
    public void bitmapToCompressedBase64_validBitmap_returnsBase64() throws IOException {
        Bitmap bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);

        String result = ImageUtils.bitmapToCompressedBase64(bitmap, 100000);

        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    public void getDefaultMaxBase64Bytes_returnsPositiveValue() {
        assertTrue(ImageUtils.getDefaultMaxBase64Bytes() > 0);
    }

    @Test
    public void getDefaultMaxDimension_returnsPositiveValue() {
        assertTrue(ImageUtils.getDefaultMaxDimension() > 0);
    }
}