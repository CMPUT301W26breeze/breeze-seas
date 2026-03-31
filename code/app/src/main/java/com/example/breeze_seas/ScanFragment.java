package com.example.breeze_seas;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;


import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.util.concurrent.ExecutionException;

public class ScanFragment extends Fragment {


    private PreviewView previewView;
    private SessionViewModel sessionViewModel;
    private boolean isProcessingScan = false;

    // Camera set up
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    startCamera();
                } else {
                    Toast.makeText(requireContext(),
                            "Camera is needed to scan QR codes",
                            Toast.LENGTH_SHORT).show();
                }
            });


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_qr_scanner, container, false);

        previewView = view.findViewById(R.id.preview_view);

        return view;

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Remove this fragment and go back to the previous one
                requireActivity().getSupportFragmentManager().popBackStack();

                // Make the bottom navigation visible again
                ((MainActivity) getActivity()).showBottomNav(true);
            }
        });

        // Check if we already have permission
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            // Ask for permission
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(requireContext());

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());
                BarcodeScanner scanner = BarcodeScanning.getClient();
                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(requireContext()), imageProxy -> {
                    @SuppressWarnings("UnsafeOptInUsageError")
                    android.media.Image mediaImage = imageProxy.getImage();

                    // Ignore this frame if we are progressing a scan
                    if (isProcessingScan) {
                        imageProxy.close();
                        return;
                    }
                    if (mediaImage != null) {
                        InputImage image = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());

                        scanner.process(image)
                                .addOnSuccessListener(barcodes -> {
                                    for (Barcode barcode : barcodes) {
                                        String rawValue = barcode.getRawValue();
                                        if (rawValue != null) {
                                            isProcessingScan = true;

                                            // Get the event corresponding to the scanned code
                                            String eventId = rawValue.replace("event:", "");
                                            EventDB.getEventById(eventId, new EventDB.LoadSingleEventCallback() {
                                                @Override
                                                public void onSuccess(Event event) {

                                                    imageAnalysis.clearAnalyzer();

                                                    // Check if the barcode is invalid
                                                    if (event == null) {
                                                        Log.e("SCAN_ERROR", "Event does not exist.");
                                                        new MaterialAlertDialogBuilder(requireContext())
                                                                .setTitle("Failure")
                                                                .setMessage("Invalid QR Code: Event not found")
                                                                .setPositiveButton("OK", (dialog, which) -> {
                                                                    // Open Explore Fragment
                                                                    getActivity().getSupportFragmentManager()
                                                                            .beginTransaction()
                                                                            .replace(R.id.fragment_container, new ExploreFragment())
                                                                            .addToBackStack(null)
                                                                            .commit();
                                                                })
                                                                .show();

                                                        return;
                                                    }

                                                    // Check if the registration date is over
                                                    long currentTime = System.currentTimeMillis();
                                                    long eventEndTime = event.getRegistrationEndTimestamp()
                                                            .toDate().getTime();
                                                    if (currentTime > eventEndTime) {
                                                        Log.e("SCAN_ERROR", "Registration period has ended.");
                                                        new MaterialAlertDialogBuilder(requireContext())
                                                                .setTitle("Failure")
                                                                .setMessage("Registration period for this event is closed!")
                                                                .setPositiveButton("OK", (dialog, which) -> {
                                                                    // Open Explore Fragment
                                                                    getActivity().getSupportFragmentManager()
                                                                            .beginTransaction()
                                                                            .replace(R.id.fragment_container, new ExploreFragment())
                                                                            .addToBackStack(null)
                                                                            .commit();
                                                                })
                                                                .show();

                                                        return;
                                                    }

                                                    // Initialize the session view model
                                                    sessionViewModel = new ViewModelProvider(requireActivity())
                                                            .get(SessionViewModel.class);
                                                    sessionViewModel.setEventShown(event);

                                                    // Make the bottom navigation visible again
                                                    ((MainActivity) getActivity()).showBottomNav(true);

                                                    // Open Event Details Fragment
                                                    getActivity().getSupportFragmentManager()
                                                            .beginTransaction()
                                                            .replace(R.id.fragment_container, new EventDetailsFragment())
                                                            .addToBackStack("Scan QR Code")
                                                            .commit();
                                                }

                                                @Override
                                                public void onFailure(Exception e) {
                                                        Log.e("DB UPDATE",
                                                                "Couldn't fetch event from DB");
                                                }
                                            });

                                        }
                                    }
                                })
                                .addOnCompleteListener(task -> imageProxy.close());
                    } else {
                        imageProxy.close();
                    }
                });


                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(getViewLifecycleOwner(), cameraSelector, preview, imageAnalysis);

            } catch (ExecutionException | InterruptedException e) {
                Toast.makeText(requireContext(), "Failed to start camera", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }
}
