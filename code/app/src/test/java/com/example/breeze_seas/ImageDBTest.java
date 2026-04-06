package com.example.breeze_seas;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Unit tests for defensive branches in {@link ImageDB}.
 */
public class ImageDBTest {

    @Before
    public void resetImageDBState() throws Exception {
        setStaticField("db", null);
        setStaticField("imageRef", null);
        setStaticField("setup", false);
        setStaticField("imagesListener", null);
    }

    private void setStaticField(String fieldName, Object value) throws Exception {
        Field field = ImageDB.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(null, value);
    }

    @Test
    public void loadImage_nullId_returnsNullImage() throws Exception {
        FirebaseFirestore mockDb = mock(FirebaseFirestore.class);
        CollectionReference mockCollection = mock(CollectionReference.class);

        when(mockDb.collection("images")).thenReturn(mockCollection);

        AtomicReference<Image> loadedImage = new AtomicReference<>();
        AtomicBoolean failed = new AtomicBoolean(false);

        try (MockedStatic<DBConnector> mockedDbConnector = mockStatic(DBConnector.class)) {
            mockedDbConnector.when(DBConnector::getDb).thenReturn(mockDb);

            ImageDB.loadImage(null, new ImageDB.LoadImageCallback() {
                @Override
                public void onSuccess(Image image) {
                    loadedImage.set(image);
                }

                @Override
                public void onFailure(Exception e) {
                    failed.set(true);
                }
            });

            assertNull(loadedImage.get());
            assertFalse(failed.get());
        }
    }

    @Test
    public void deleteImage_blankId_callsSuccessImmediately() throws Exception {
        FirebaseFirestore mockDb = mock(FirebaseFirestore.class);
        CollectionReference mockCollection = mock(CollectionReference.class);

        when(mockDb.collection("images")).thenReturn(mockCollection);

        AtomicBoolean success = new AtomicBoolean(false);
        AtomicBoolean failure = new AtomicBoolean(false);

        try (MockedStatic<DBConnector> mockedDbConnector = mockStatic(DBConnector.class)) {
            mockedDbConnector.when(DBConnector::getDb).thenReturn(mockDb);

            ImageDB.deleteImage("   ", new ImageDB.ImageMutationCallback() {
                @Override
                public void onSuccess() {
                    success.set(true);
                }

                @Override
                public void onFailure(Exception e) {
                    failure.set(true);
                }
            });

            assertTrue(success.get());
            assertFalse(failure.get());
        }
    }

    @Test
    public void stopImagesListen_withListener_removesListener() throws Exception {
        ListenerRegistration mockListener = mock(ListenerRegistration.class);
        setStaticField("imagesListener", mockListener);

        ImageDB.stopImagesListen();

        verify(mockListener).remove();

        Field field = ImageDB.class.getDeclaredField("imagesListener");
        field.setAccessible(true);
        assertNull(field.get(null));
    }
}
