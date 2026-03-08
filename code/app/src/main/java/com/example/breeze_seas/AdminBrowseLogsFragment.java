package com.example.breeze_seas;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.MaterialToolbar;

public class AdminBrowseLogsFragment extends Fragment {
    public AdminBrowseLogsFragment() { super(R.layout.fragment_admin_browse_logs); }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MaterialToolbar toolbar = view.findViewById(R.id.abl_topAppBar);
        toolbar.setNavigationOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new AdminDashboardFragment())
                    .commit();
        });
    }
}
