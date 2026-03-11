package com.example.breeze_seas;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class AdminEventDetailsFragment extends Fragment {

    private String eventId;

    public AdminEventDetailsFragment() { super(R.layout.fragment_admin_event_details); }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Extract the ID that the Admin list passed to us
        if (getArguments() != null) {
            eventId = getArguments().getString("eventId");
        }
    }
}
