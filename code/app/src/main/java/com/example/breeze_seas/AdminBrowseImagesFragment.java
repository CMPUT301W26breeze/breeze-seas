package com.example.breeze_seas;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;

/**
 * Displays all uploaded images in a scrollable grid.
 * Uses {@link AdminViewModel} for the image list so data persists across navigation
 * and the Firestore listener is not restarted every time the fragment is recreated.
 */
public class AdminBrowseImagesFragment extends Fragment {

    private AdminBrowseImagesAdapter adapter;
    private AdminViewModel adminViewModel;

    public AdminBrowseImagesFragment() { super(R.layout.fragment_admin_browse_images); }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        adminViewModel = new ViewModelProvider(requireActivity()).get(AdminViewModel.class);

        MaterialToolbar toolbar = view.findViewById(R.id.abi_topAppBar);
        toolbar.setNavigationOnClickListener(v -> {
            int containerId = ((View) requireView().getParent()).getId();
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(containerId, new AdminDashboardFragment())
                    .commit();
        });

        RecyclerView recyclerView = view.findViewById(R.id.abi_rv_image_gallery);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));

        adapter = new AdminBrowseImagesAdapter(new ArrayList<>(), image -> {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Delete image")
                    .setMessage("This image will be permanently deleted.")
                    .setPositiveButton("Delete", (dialog, which) ->
                            adminViewModel.deleteImage(image, new ImageDB.ImageMutationCallback() {
                                @Override
                                public void onSuccess() { }

                                @Override
                                public void onFailure(Exception e) {
                                    if (getContext() != null) {
                                        Toast.makeText(getContext(), "Failed to delete image", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            }))
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        recyclerView.setAdapter(adapter);

        adminViewModel.getImages().observe(getViewLifecycleOwner(), images -> {
            int previousCount = adapter.getItemCount();
            adapter.setImages(images);
            if (images.size() < previousCount) {
                Toast.makeText(getContext(), "Image deleted", Toast.LENGTH_SHORT).show();
            }
        });

        // Start listening
        adminViewModel.startImagesListen();
    }
}
