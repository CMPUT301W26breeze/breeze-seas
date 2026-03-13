package com.example.breeze_seas;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Represents the "Browse Events" screen for an administrator.
 * This fragment is responsible for displaying a scrollable list of all events currently
 * in the system. It handles UI setup, including a toolbar for navigation back to the
 * dashboard, and a RecyclerView to display the event data. It also manages
 * navigation to the event details screen when a specific event is selected.
 */
public class AdminBrowseEventsFragment extends Fragment {

    private AdminBrowseEventsAdapter adapter;
    private final List<Event> eventList = new ArrayList<>();

    private RecyclerView recyclerView;
    private SessionViewModel sessionViewModel;

    /**
     * Constructor for the fragment.
     * Initializes the fragment.
     */
    public AdminBrowseEventsFragment() { super(R.layout.fragment_admin_browse_events); }

    /**
     * Called immediately after the view has been created.
     * This method initializes the UI components:
     * 1. Sets up top app bar and back navigation.
     * 2. Initializes RecyclerView and its LayoutManager.
     * 3. Configures {@link AdminBrowseEventsAdapter} with a click listener that
     * passes the selected event's ID to {@link AdminEventDetailsFragment}.
     * 4. Fetches data to populate the list.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sessionViewModel = new ViewModelProvider(requireActivity()).get(SessionViewModel.class);

        recyclerView = view.findViewById(R.id.abe_rv_events_list);

        MaterialToolbar toolbar = view.findViewById(R.id.abe_topAppBar);
        toolbar.setNavigationOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new AdminDashboardFragment())
                    .commit();
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new AdminBrowseEventsAdapter(eventList, event -> {
            sessionViewModel.setEventShown(event);

            // TODO: Complete AdminEventDetailsFragment
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new AdminEventDetailsFragment())
                    .addToBackStack(null)
                    .commit();
        });

        recyclerView.setAdapter(adapter);

        fetchEvents();
    }

    /**
     * Fetches all events from the database with {@link EventDB}.
     * Upon successful data load, it clears the current list, adds the newly fetched
     * events, and notifies the adapter to refresh the RecyclerView. If it fails,
     * it logs the error and displays a message.
     */
    private void fetchEvents() {
        EventDB.getAllEvents(new EventDB.LoadEventsCallback() {
            @Override
            public void onSuccess(ArrayList<Event> events) {
                eventList.clear();

                if (events != null && !events.isEmpty()) {
                    eventList.addAll(events);
                }

                adapter.notifyDataSetChanged();
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(getContext(), "Failed to load events", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
