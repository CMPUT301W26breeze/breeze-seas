package com.example.breeze_seas;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.MaterialToolbar;

/**
 * A fragment for the main dashboard for admins.
 */
public class AdminDashboardFragment extends Fragment {
    public AdminDashboardFragment() { super(R.layout.fragment_admin_dashboard); }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MaterialToolbar toolbar = view.findViewById(R.id.ad_topAppBar);
        toolbar.setNavigationOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new AdminProfileFragment())
                    .commit();
        });

        Button btnViewEvents = view.findViewById(R.id.ad_btn_view_events);

        btnViewEvents.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new AdminBrowseEventsFragment())
                    .addToBackStack(null)
                    .commit();
        });

        Button btnViewProfiles = view.findViewById(R.id.ad_btn_view_profiles);

        btnViewProfiles.setOnClickListener(view1 -> {
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new AdminBrowseProfilesFragment())
                    .addToBackStack(null)
                    .commit();
        });

        Button btnViewImages = view.findViewById(R.id.ad_btn_view_images);

        btnViewImages.setOnClickListener(view1 -> {
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new AdminBrowseImagesFragment())
                    .addToBackStack(null)
                    .commit();
        });

        Button btnViewLogs = view.findViewById(R.id.ad_btn_view_logs);

        btnViewLogs.setOnClickListener(view1 -> {
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new AdminBrowseLogsFragment())
                    .addToBackStack(null)
                    .commit();
        });
    }
}
