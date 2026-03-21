package com.example.breeze_seas;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * CreateEventFragment collects organizer input for a new event and submits it through
 * {@link EventDB}.
 */
public class CreateEventFragment extends Fragment {

    private static final int MAX_BASE64_BYTES = 700 * 1024;
    private static final int MAX_DIMENSION = 1280;

    private ImageView ivPoster;
    private LinearLayout posterPlaceholder;

    private TextInputEditText etRegFrom, etRegTo, etEventName, etEventDetails, etCapacity, etWaitingListCapacity;
    private SwitchMaterial swGeo;
    private SessionViewModel viewModel;

    private Long regFromMillis = null;
    private Long regToMillis = null;
    private String posterBase64 = "";

    private final ActivityResultLauncher<String> pickImage =
            registerForActivityResult(new ActivityResultContracts.GetContent(), new androidx.activity.result.ActivityResultCallback<Uri>() {
                /**
                 * Stores the selected poster image as compressed Base64 and updates the poster preview.
                 *
                 * @param uri Uri selected from the system picker, or {@code null} when cancelled.
                 */
                @Override
                public void onActivityResult(Uri uri) {
                    if (uri == null) {
                        return;
                    }

                    try {
                        posterBase64 = uriToCompressedBase64(uri);

                        Bitmap previewBitmap = base64ToBitmap(posterBase64);
                        if (previewBitmap != null) {
                            ivPoster.setImageBitmap(previewBitmap);
                            ivPoster.setVisibility(View.VISIBLE);
                            posterPlaceholder.setVisibility(View.GONE);
                        } else {
                            Toast.makeText(requireContext(), "Failed to preview image", Toast.LENGTH_SHORT).show();
                        }
                    } catch (IOException e) {
                        posterBase64 = "";
                        Toast.makeText(requireContext(), "Failed to process image", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    /**
     * Creates the organizer event-creation fragment using the shared create-event layout.
     */
    public CreateEventFragment() {
        super(R.layout.fragment_create_event);
    }

    /**
     * Binds organizer form fields and wires the create-event interactions.
     *
     * @param view Inflated create-event root view.
     * @param savedInstanceState Previously saved instance state bundle, or {@code null}.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(SessionViewModel.class);

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            /**
             * Returns to the previous organizer screen when the toolbar back button is pressed.
             *
             * @param v Toolbar navigation view that was tapped.
             */
            @Override
            public void onClick(View v) {
                requireActivity().getSupportFragmentManager().popBackStack();
            }
        });

        ivPoster = view.findViewById(R.id.ivPoster);
        posterPlaceholder = view.findViewById(R.id.posterPlaceholder);

        View cardPoster = view.findViewById(R.id.cardPoster);
        View btnAddImage = view.findViewById(R.id.btnAddImage);
        View btnRegPeriod = view.findViewById(R.id.btnRegPeriod);

        etRegFrom = view.findViewById(R.id.etRegFrom);
        etRegTo = view.findViewById(R.id.etRegTo);
        etEventName = view.findViewById(R.id.etEventName);
        etEventDetails = view.findViewById(R.id.etEventDetails);
        etCapacity = view.findViewById(R.id.etCapacity);
        etWaitingListCapacity = view.findViewById(R.id.etWaitingListCapacity);
        swGeo = view.findViewById(R.id.swGeo);

        View.OnClickListener pickPoster = new View.OnClickListener() {
            /**
             * Opens the poster image picker for the organizer.
             *
             * @param v Poster-related view that was tapped.
             */
            @Override
            public void onClick(View v) {
                pickImage.launch("image/*");
            }
        };
        cardPoster.setOnClickListener(pickPoster);
        btnAddImage.setOnClickListener(pickPoster);

        View.OnClickListener pickRange = new View.OnClickListener() {
            /**
             * Opens the registration-date range picker.
             *
             * @param v Date-related view that was tapped.
             */
            @Override
            public void onClick(View v) {
                openDateRangePicker();
            }
        };
        btnRegPeriod.setOnClickListener(pickRange);
        etRegFrom.setOnClickListener(pickRange);
        etRegTo.setOnClickListener(pickRange);

        view.findViewById(R.id.btnCreate).setOnClickListener(new View.OnClickListener() {
            /**
             * Validates the organizer input and starts event creation.
             *
             * @param v Create button that was tapped.
             */
            @Override
            public void onClick(View v) {
                onCreateClicked();
            }
        });
    }

    /**
     * Opens the registration-date picker and stores the selected date range.
     */
    private void openDateRangePicker() {
        MaterialDatePicker.Builder<androidx.core.util.Pair<Long, Long>> builder =
                MaterialDatePicker.Builder.dateRangePicker()
                        .setTheme(R.style.ThemeOverlay_Breezeseas_DateRangePicker)
                        .setTitleText("Select registration period");

        if (regFromMillis != null && regToMillis != null) {
            builder.setSelection(new androidx.core.util.Pair<>(regFromMillis, regToMillis));
        }

        MaterialDatePicker<androidx.core.util.Pair<Long, Long>> picker = builder.build();

        picker.addOnPositiveButtonClickListener(new com.google.android.material.datepicker.MaterialPickerOnPositiveButtonClickListener<androidx.core.util.Pair<Long, Long>>() {
            /**
             * Stores the selected registration range and updates the visible date fields.
             *
             * @param selection Selected registration start and end dates.
             */
            @Override
            public void onPositiveButtonClick(androidx.core.util.Pair<Long, Long> selection) {
                if (selection == null) return;
                regFromMillis = selection.first;
                regToMillis = selection.second;

                SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy", Locale.US);
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                etRegFrom.setText(sdf.format(new Date(regFromMillis)));
                etRegTo.setText(sdf.format(new Date(regToMillis)));
            }
        });

        picker.show(getParentFragmentManager(), "reg_range");
    }

    /**
     * Validates organizer input, builds a new {@link Event}, and submits it to {@link EventDB}.
     */
    private void onCreateClicked() {
        String name = etEventName.getText() == null ? "" : etEventName.getText().toString().trim();
        String details = etEventDetails.getText() == null ? "" : etEventDetails.getText().toString().trim();
        String capText = etCapacity.getText() == null ? "" : etCapacity.getText().toString().trim();
        String waitingListText = etWaitingListCapacity.getText() == null ? "" : etWaitingListCapacity.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            etEventName.setError("Required");
            return;
        }

        if (regFromMillis == null || regToMillis == null) {
            Toast.makeText(requireContext(), "Please set registration period", Toast.LENGTH_SHORT).show();
            return;
        }

        String organizerId = viewModel == null ? null : viewModel.getAndroidID().getValue();
        if (organizerId == null || organizerId.trim().isEmpty()) {
            Toast.makeText(requireContext(), "Unable to resolve organizer identity", Toast.LENGTH_SHORT).show();
            return;
        }

        Integer eventCap = null;
        if (TextUtils.isEmpty(capText)) {
            etCapacity.setError("Number required");
            return;
        }
        if (!TextUtils.isEmpty(capText)) {
            try {
                eventCap = Integer.parseInt(capText);
            } catch (NumberFormatException e) {
                etCapacity.setError("Enter a valid number");
                return;
            }
        }

        Integer eventWaitingListCap = null;
        if (!TextUtils.isEmpty(waitingListText)) {
            try {
                eventWaitingListCap = Integer.parseInt(waitingListText);
            } catch (NumberFormatException e) {
                etWaitingListCapacity.setError("Enter a valid number");
                return;
            }
        }

        int normalizedCapacity = eventCap == null ? -1 : eventCap;
        int normalizedEventWaitingListCapacity = eventWaitingListCap == null ? -1 : eventWaitingListCap;

        Event event = new Event(
                false,
                organizerId,
                name,
                details,
                posterBase64 == null ? "" : posterBase64,
                "",
                new Timestamp(new Date(regFromMillis)),
                new Timestamp(new Date(regToMillis)),
                null,
                null,
                swGeo.isChecked(),
                normalizedCapacity,
                normalizedEventWaitingListCapacity,
                0
        );

        EventDB.addEvent(event, new EventDB.AddEventCallback() {
            /**
             * Opens the QR screen for the newly created event.
             *
             * @param eventId Identifier assigned to the newly created event.
             */
            @Override
            public void onSuccess(String eventId) {
                Toast.makeText(requireContext(), "Event created", Toast.LENGTH_SHORT).show();

                if (viewModel != null) {
                    viewModel.setEventShown(event);
                }

                Bundle args = new Bundle();
                args.putString("eventId", eventId);

                ViewQrCodeFragment fragment = new ViewQrCodeFragment();
                fragment.setArguments(args);

                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, fragment)
                        .commit();
            }

            /**
             * Reports a user-visible error if event creation fails.
             *
             * @param e Failure returned by the create-event request.
             */
            @Override
            public void onFailure(Exception e) {
                Toast.makeText(requireContext(), "Failed to create event", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Converts an image Uri into a compressed Base64 poster string.
     *
     * @param uri Selected image Uri.
     * @return Compressed Base64 string.
     * @throws IOException When the image cannot be read or compressed.
     */
    private String uriToCompressedBase64(Uri uri) throws IOException {
        Bitmap bitmap = decodeSampledBitmapFromUri(uri, MAX_DIMENSION, MAX_DIMENSION);
        return bitmapToCompressedBase64(bitmap, MAX_BASE64_BYTES);
    }

    /**
     * Compresses a bitmap until the Base64 string is under the configured size limit.
     *
     * @param bitmap Source bitmap.
     * @param maxBase64Bytes Maximum allowed Base64 byte size.
     * @return Compressed Base64 string.
     * @throws IOException When compression fails.
     */
    private String bitmapToCompressedBase64(Bitmap bitmap, int maxBase64Bytes) throws IOException {
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

            if (quality > 40) {
                quality -= 10;
                continue;
            }

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
     * Decodes a Base64 image string into a bitmap for preview.
     *
     * @param base64 Base64 image string.
     * @return Decoded bitmap, or {@code null} if empty.
     */
    private Bitmap base64ToBitmap(String base64) {
        if (base64 == null || base64.isEmpty()) {
            return null;
        }

        byte[] bytes = Base64.decode(base64, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    /**
     * Decodes and down-samples an image from a content Uri.
     *
     * @param uri Source image Uri.
     * @param reqWidth Requested maximum width.
     * @param reqHeight Requested maximum height.
     * @return Decoded bitmap.
     * @throws IOException When the image cannot be opened or decoded.
     */
    private Bitmap decodeSampledBitmapFromUri(Uri uri, int reqWidth, int reqHeight) throws IOException {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;

        InputStream boundsStream = requireContext().getContentResolver().openInputStream(uri);
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

        InputStream decodeStream = requireContext().getContentResolver().openInputStream(uri);
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
     * Computes a bitmap sampling size for efficient image loading.
     *
     * @param options Bitmap bounds options.
     * @param reqWidth Requested maximum width.
     * @param reqHeight Requested maximum height.
     * @return Sample size to use when decoding.
     */
    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int height = options.outHeight;
        int width = options.outWidth;
        int inSampleSize = 1;

        while ((height / inSampleSize) > reqHeight * 2 || (width / inSampleSize) > reqWidth * 2) {
            inSampleSize *= 2;
        }

        return Math.max(1, inSampleSize);
    }

    /**
     * Scales down a bitmap so it fits within the provided size bounds.
     *
     * @param src Source bitmap.
     * @param maxWidth Maximum width.
     * @param maxHeight Maximum height.
     * @return Scaled bitmap.
     */
    private Bitmap scaleDown(Bitmap src, int maxWidth, int maxHeight) {
        float ratio = Math.min((float) maxWidth / src.getWidth(), (float) maxHeight / src.getHeight());

        if (ratio >= 1.0f) {
            return src;
        }

        int newWidth = Math.round(src.getWidth() * ratio);
        int newHeight = Math.round(src.getHeight() * ratio);
        return Bitmap.createScaledBitmap(src, newWidth, newHeight, true);
    }
}