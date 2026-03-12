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
import androidx.lifecycle.ViewModelProvider;

import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeEncoder;

public class ViewQrCodeFragment extends Fragment {

    private SessionViewModel viewModel;

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

        if (eventId != null) {
            EventDB.getEventById(eventId, new EventDB.LoadSingleEventCallback() {
                @Override
                public void onSuccess(Event event) {
                    if (!isAdded()) return;

                    if (event != null) {
                        tvEventName.setText(event.getName());
                        ivQr.setImageBitmap(makeQr("event:" + event.getEventId()));
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

    private Bitmap makeQr(String content) {
        try {
            BarcodeEncoder encoder = new BarcodeEncoder();
            return encoder.encodeBitmap(content, BarcodeFormat.QR_CODE, 800, 800);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
