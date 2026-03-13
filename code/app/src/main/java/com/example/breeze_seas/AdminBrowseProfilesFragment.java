package com.example.breeze_seas;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.MaterialToolbar;

/**
 * A fragment that will allow administrators to browse all profiles.
 * TODO: This will be implemented
 */
public class AdminBrowseProfilesFragment extends Fragment {
    public AdminBrowseProfilesFragment() { super(R.layout.fragment_admin_browse_profiles); }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MaterialToolbar toolbar = view.findViewById(R.id.abp_top_app_bar);
        toolbar.setNavigationOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new AdminDashboardFragment())
                    .commit();
        });
    }
}
