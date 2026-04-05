package com.example.breeze_seas;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import android.graphics.Bitmap;
import android.util.Base64;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.robolectric.RobolectricTestRunner;

import java.io.ByteArrayOutputStream;

@RunWith(RobolectricTestRunner.class)
public class ImageTest {

    private String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        return Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);
    }

    @Test
    public void constructor_withId_setsFieldsCorrectly() {
        Bitmap bitmap = Bitmap.createBitmap(20, 20, Bitmap.Config.ARGB_8888);
        String base64 = bitmapToBase64(bitmap);

        Image image = new Image("img123", base64);

        assertEquals("img123", image.getImageId());
        assertEquals(base64, image.getCompressedBase64());
        assertNotNull(image.getImageData());
    }

    @Test
    public void constructor_withoutId_generatesId() {
        Bitmap bitmap = Bitmap.createBitmap(20, 20, Bitmap.Config.ARGB_8888);
        String base64 = bitmapToBase64(bitmap);

        try (MockedStatic<ImageDB> mockedImageDB = mockStatic(ImageDB.class)) {
            mockedImageDB.when(ImageDB::genNewId).thenReturn("generated123");

            Image image = new Image(base64);

            assertEquals("generated123", image.getImageId());
            assertEquals(base64, image.getCompressedBase64());
            assertNotNull(image.getImageData());
        }
    }

    @Test
    public void setImageId_updatesId() {
        Image image = new Image("img1", null);

        image.setImageId("img2");

        assertEquals("img2", image.getImageId());
    }

    @Test
    public void setCompressedBase64_updatesBase64AndBitmap() {
        Bitmap bitmap = Bitmap.createBitmap(30, 30, Bitmap.Config.ARGB_8888);
        String base64 = bitmapToBase64(bitmap);

        Image image = new Image("img1", null);
        image.setCompressedBase64(base64);

        assertEquals(base64, image.getCompressedBase64());
        assertNotNull(image.getImageData());
    }

    @Test
    public void display_returnsSameAsGetImageData() {
        Bitmap bitmap = Bitmap.createBitmap(20, 20, Bitmap.Config.ARGB_8888);
        String base64 = bitmapToBase64(bitmap);

        Image image = new Image("img1", base64);

        assertSame(image.getImageData(), image.display());
    }

    @Test
    public void setImageData_updatesBitmap() {
        Image image = new Image("img1", null);
        Bitmap bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888);

        image.setImageData(bitmap);

        assertSame(bitmap, image.getImageData());
    }
}