package com.example.breeze_seas;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeEncoder;

public class ViewQrCodeFragment extends Fragment {

    public ViewQrCodeFragment() {
        super(R.layout.fragment_view_qr_code);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String eventId = getArguments() == null ? null : getArguments().getString("eventId");
        Event event = eventId == null ? null : EventDB.getInstance().getEventById(eventId);

        TextView tvEventName = view.findViewById(R.id.tvEventName);
        ImageView ivQr = view.findViewById(R.id.ivQr);

        if (event != null) {
            tvEventName.setText(event.getName());
            ivQr.setImageBitmap(makeQr("event:" + event.getId()));
        } else {
            tvEventName.setText("Unknown Event");
        }

        view.findViewById(R.id.btnManageEntrants).setOnClickListener(v ->
                Toast.makeText(requireContext(), "Manage Entrants (TODO)", Toast.LENGTH_SHORT).show()
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
}