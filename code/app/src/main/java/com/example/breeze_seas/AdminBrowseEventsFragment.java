package com.example.breeze_seas;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.List;

public class AdminBrowseEventsFragment extends Fragment {

    private AdminBrowseEventsAdapter adapter;
    private final List<Event> eventsList = new ArrayList<>();

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

        adapter = new AdminBrowseEventsAdapter(eventsList, event -> {
            AdminEventDetailsFragment detailsFragment = new AdminEventDetailsFragment();

            Bundle args = new Bundle();
            args.putString("eventId", event.getEventId());
            detailsFragment.setArguments(args);

            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, detailsFragment)
                    .addToBackStack(null)
                    .commit();
        });

        recyclerView.setAdapter(adapter);

        fetchEvents();
    }

    private void fetchEvents() {
        EventDB.getAllEvents(new EventDB.LoadEventsCallback() {
            @Override
            public void onSuccess(ArrayList<Event> events) {
                eventsList.clear();
                eventsList.addAll(events);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onFailure(Exception e) {
                Log.e("AdminEvents", "Error loading events", e);
                Toast.makeText(getContext(), "Failed to load events.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
