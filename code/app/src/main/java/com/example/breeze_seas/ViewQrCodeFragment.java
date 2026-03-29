package com.example.breeze_seas;

import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

public class ViewQrCodeFragment extends Fragment {

    private SessionViewModel viewModel;
    @Nullable
    private Bitmap currentQrBitmap;
    @Nullable
    private String currentEventName;

    private final androidx.activity.result.ActivityResultLauncher<String> savePermissionLauncher =
            registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.RequestPermission(), isGranted -> {
                if (!isAdded()) {
                    return;
                }

                if (isGranted) {
                    saveQrToDevice();
                } else {
                    Toast.makeText(requireContext(), R.string.view_qr_code_save_permission_denied, Toast.LENGTH_SHORT).show();
                }
            });

    public ViewQrCodeFragment() {
        super(R.layout.fragment_view_qr_code);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(SessionViewModel.class);

        String eventId = getArguments() == null ? null : getArguments().getString("eventId");

        TextView tvEventName = view.findViewById(R.id.tvEventName);
        ImageView ivQr = view.findViewById(R.id.ivQr);
        Button saveQrButton = view.findViewById(R.id.btnSaveQr);
        saveQrButton.setOnClickListener(v -> maybeSaveQr());

        if (eventId != null) {
            EventDB.getEventById(eventId, new EventDB.LoadSingleEventCallback() {
                @Override
                public void onSuccess(Event event) {
                    if (!isAdded()) return;

                    if (event != null) {
                        tvEventName.setText(event.getName());
                        currentEventName = event.getName();
                        currentQrBitmap = makeQr("event:" + event.getEventId());
                        ivQr.setImageBitmap(currentQrBitmap);
                        view.findViewById(R.id.btnManageEntrants).setOnClickListener(v ->
                                openManageEntrantsFragment(event)
                        );
                    } else {
                        tvEventName.setText("Unknown Event");
                    }
                }

                @Override
                public void onFailure(Exception e) {
                    if (isAdded()) {
                        tvEventName.setText("Error loading event");
                    }
                }
            });
        } else {
            tvEventName.setText("Unknown Event");
        }

        view.findViewById(R.id.btnClose).setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack()
        );
    }

    private void maybeSaveQr() {
        if (currentQrBitmap == null) {
            Toast.makeText(requireContext(), R.string.view_qr_code_save_unavailable, Toast.LENGTH_SHORT).show();
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveQrToDevice();
            return;
        }

        if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            saveQrToDevice();
        } else {
            savePermissionLauncher.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
    }

    private void saveQrToDevice() {
        Bitmap qrBitmap = currentQrBitmap;
        if (qrBitmap == null) {
            return;
        }

        try {
            String fileName = buildQrFileName();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveQrWithMediaStore(qrBitmap, fileName);
            } else {
                saveQrLegacy(qrBitmap, fileName);
            }

            Toast.makeText(requireContext(), R.string.view_qr_code_save_success, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(requireContext(), R.string.view_qr_code_save_failure, Toast.LENGTH_SHORT).show();
        }
    }

    private void openManageEntrantsFragment(@NonNull Event event) {
        try {
            Class<?> fragmentClass = Class.forName("com.example.breeze_seas.ManageEntrantsFragment");
            Object instance = fragmentClass.getDeclaredConstructor().newInstance();
            if (!(instance instanceof Fragment)) {
                throw new IllegalStateException("ManageEntrantsFragment is not a Fragment");
            }

            Fragment fragment = (Fragment) instance;
            if (viewModel != null) {
                viewModel.setEventShown(event);
            }
            ((MainActivity) requireActivity()).openSecondaryFragment(fragment);
        } catch (Exception e) {
            Toast.makeText(
                    requireContext(),
                    R.string.organizer_event_preview_manage_unavailable,
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    @NonNull
    private String buildQrFileName() {
        String eventName = currentEventName == null ? "event" : currentEventName.trim();
        if (eventName.isEmpty()) {
            eventName = "event";
        }
        eventName = eventName.replaceAll("[^a-zA-Z0-9_-]+", "_");
        return "breeze_seas_qr_" + eventName + "_" + System.currentTimeMillis() + ".png";
    }

    private void saveQrWithMediaStore(@NonNull Bitmap qrBitmap, @NonNull String fileName) throws Exception {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
        values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/BreezeSeas");
        values.put(MediaStore.Images.Media.IS_PENDING, 1);

        Uri imageUri = requireContext()
                .getContentResolver()
                .insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        if (imageUri == null) {
            throw new IllegalStateException("Unable to create MediaStore entry.");
        }

        try (OutputStream outputStream = requireContext().getContentResolver().openOutputStream(imageUri)) {
            if (outputStream == null || !qrBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)) {
                throw new IllegalStateException("Unable to write QR image.");
            }
        }

        values.clear();
        values.put(MediaStore.Images.Media.IS_PENDING, 0);
        requireContext().getContentResolver().update(imageUri, values, null, null);
    }

    private void saveQrLegacy(@NonNull Bitmap qrBitmap, @NonNull String fileName) throws Exception {
        File picturesDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File appDirectory = new File(picturesDirectory, "BreezeSeas");
        if (!appDirectory.exists() && !appDirectory.mkdirs()) {
            throw new IllegalStateException("Unable to create image directory.");
        }

        File outputFile = new File(appDirectory, fileName);
        try (FileOutputStream outputStream = new FileOutputStream(outputFile)) {
            if (!qrBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)) {
                throw new IllegalStateException("Unable to write QR image.");
            }
        }

        MediaScannerConnection.scanFile(
                requireContext(),
                new String[]{outputFile.getAbsolutePath()},
                new String[]{"image/png"},
                null
        );
    }

    private Bitmap makeQr(String content) {
        try {
            BarcodeEncoder encoder = new BarcodeEncoder();
            return encoder.encodeBitmap(content, BarcodeFormat.QR_CODE, 800, 800);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
