package com.example.breeze_seas;

import android.content.ContentValues;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.io.OutputStream;

public class ViewQrCodeFragment extends Fragment {

    private Bitmap qrBitmap;

    public ViewQrCodeFragment() {
        super(R.layout.fragment_view_qr_code);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String eventId = getArguments() == null ? null : getArguments().getString("eventId");

        TextView tvEventName = view.findViewById(R.id.tvEventName);
        ImageView ivQr = view.findViewById(R.id.ivQr);

        if (eventId != null) {
            EventDB.getInstance().getEventById(eventId, new EventDB.LoadSingleEventCallback() {
                @Override
                public void onSuccess(Event event) {
                    if (!isAdded()) return;

                    if (event != null) {
                        tvEventName.setText(event.getName());
                        qrBitmap = makeQr("event:" + event.getId());
                        ivQr.setImageBitmap(qrBitmap);
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

        view.findViewById(R.id.btnSaveQr).setOnClickListener(v -> {
            if (qrBitmap == null) {
                Toast.makeText(requireContext(), "QR code not ready yet", Toast.LENGTH_SHORT).show();
                return;
            }

            boolean saved = saveBitmapToGallery(qrBitmap, "event_qr_" + System.currentTimeMillis() + ".png");
            Toast.makeText(requireContext(),
                    saved ? "Saved to Gallery" : "Failed to save QR",
                    Toast.LENGTH_SHORT).show();
        });

        view.findViewById(R.id.btnManageEntrants).setOnClickListener(v ->
                ((MainActivity) requireActivity()).openSecondaryFragment(new ManageEntrantsFragment())
        );

        view.findViewById(R.id.btnClose).setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack()
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

    private boolean saveBitmapToGallery(Bitmap bitmap, String fileName) {
        try {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
            values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/BreezeSeas");

            Uri uri = requireContext().getContentResolver().insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    values
            );

            if (uri == null) return false;

            OutputStream outputStream = requireContext().getContentResolver().openOutputStream(uri);
            if (outputStream == null) return false;

            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
            outputStream.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}