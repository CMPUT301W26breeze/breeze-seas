package com.example.breeze_seas;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.MaterialToolbar;

public class AdminDashboardFragment extends Fragment {
    // Using the modern constructor to automatically inflate the layout
    public AdminDashboardFragment() { super(R.layout.fragment_admin_dashboard); }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Setup the Toolbar back button
        MaterialToolbar toolbar = view.findViewById(R.id.ad_topAppBar);
        toolbar.setNavigationOnClickListener(v -> {
            // This will pop the dashboard off and return to your main app flow (e.g., the Profile tab)
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new ProfileFragment())
                    .commit();
        });

        // 2. Find the "View All Events" button
        Button btnViewEvents = view.findViewById(R.id.ad_btn_view_events);

        // 3. Set the click listener to route to the Events list
        btnViewEvents.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new AdminBrowseEventsFragment())
                    // If you want the physical hardware back button on the phone to return to
                    // this dashboard from the events list, uncomment the line below:
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
