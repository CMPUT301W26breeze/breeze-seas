package com.example.breeze_seas;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

/**
 * Displays the event details when the admin clicks on one of the events in the browsing list
 * TODO: To be implemented
 */
public class AdminEventDetailsFragment extends Fragment {
    // TODO: Complete this
    private String eventId;

    public AdminEventDetailsFragment() { super(R.layout.fragment_admin_event_details); }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            eventId = getArguments().getString("eventId");
        }
    }
}
