package com.example.breeze_seas;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.List;

public class AdminBrowseEventsFragment extends Fragment {

    public AdminBrowseEventsFragment() { super(R.layout.fragment_admin_browse_events); }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MaterialToolbar toolbar = view.findViewById(R.id.abe_topAppBar);
        toolbar.setNavigationOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new AdminDashboardFragment())
                    .commit();
        });

        RecyclerView recyclerView = view.findViewById(R.id.abe_rv_events_list);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        List<AdminBrowseEventsAdapter.EventItem> dummyEvents = new ArrayList<>();
        dummyEvents.add(new AdminBrowseEventsAdapter.EventItem("Annual Summer Picnic", "Organizer: John Doe"));
        dummyEvents.add(new AdminBrowseEventsAdapter.EventItem("Tech Innovators Conference", "Organizer: Jane Smith"));
        dummyEvents.add(new AdminBrowseEventsAdapter.EventItem("Community Beach Cleanup", "Organizer: Ocean Protectors"));
        dummyEvents.add(new AdminBrowseEventsAdapter.EventItem("Local Art Walk", "Organizer: Sarah Jenkins"));
        dummyEvents.add(new AdminBrowseEventsAdapter.EventItem("Startup Pitch Night", "Organizer: Venture Capital Group"));
        dummyEvents.add(new AdminBrowseEventsAdapter.EventItem("Winter Charity Gala", "Organizer: The Foundation"));
        dummyEvents.add(new AdminBrowseEventsAdapter.EventItem("Spring Marathon", "Organizer: City Athletics"));
        dummyEvents.add(new AdminBrowseEventsAdapter.EventItem("Food Truck Festival", "Organizer: Culinary Arts Dept"));

        AdminBrowseEventsAdapter adapter = new AdminBrowseEventsAdapter(dummyEvents);
        recyclerView.setAdapter(adapter);
    }
}
