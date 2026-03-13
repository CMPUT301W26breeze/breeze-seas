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

/**
 * Represents the "Browse Events" screen for an administrator.
 * This fragment is responsible for displaying a scrollable list of all events currently
 * in the system. It handles UI setup, including a toolbar for navigation back to the
 * dashboard, and a RecyclerView to display the event data. It also manages
 * navigation to the event details screen when a specific event is selected.
 */
public class AdminBrowseEventsFragment extends Fragment {

    private AdminBrowseEventsAdapter adapter;
    private final List<Event> eventsList = new ArrayList<>();

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
     *
     * @param view View returned by onCreateView.
     * @param savedInstanceState If non-null, fragment is re-constructed
     * from a previous saved state.
     */
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
