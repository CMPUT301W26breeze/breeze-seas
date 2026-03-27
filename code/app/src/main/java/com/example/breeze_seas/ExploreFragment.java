package com.example.breeze_seas;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Locale;

/*** ExploreFragment is a top-level destination accessible via Bottom Navigation.
 *
 * <p>Current state:* - A placeholder screen was created to validate the navigation wiring and theme style.
 ** <p>Outstanding/Future Work:* - Replace the placeholder UI with the Explore feature implementation.
 */
public class ExploreFragment extends Fragment implements RecyclerViewClickListener {

    private TextView noEventsTest;
    private View scanQRCodeBtn;
    private View filterButton;
    private EditText searchInput;
    private ArrayList<Event> eventList = new ArrayList<>();
    private final ArrayList<Event> allVisibleEvents = new ArrayList<>();
    private SessionViewModel viewModel;
    private User user;
    private ExploreEventViewAdapter adapter;

    public ExploreFragment() {
        super(R.layout.fragment_explore);
    }

    @Override
    public void recyclerViewListClicked(View v, int position) {
        // Code for event entry being clicked
        // Grab event
        Event selectedEvent = eventList.get(position);

        // Stow event in SessionViewModel
        viewModel.setEventShown(selectedEvent);

        // Switch to event details
        getActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, new EventDetailsFragment())
                .addToBackStack(null)
                .commit();
    }

    // Bind recycleview
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Setup viewModel
        viewModel = new ViewModelProvider(requireActivity()).get(SessionViewModel.class);
        user = viewModel.getUser().getValue();

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Bind views
        noEventsTest = view.findViewById(R.id.explore_no_events_found_text);
        scanQRCodeBtn = view.findViewById(R.id.explore_QRCode_floating_button);
        filterButton = view.findViewById(R.id.explore_filter_button);
        searchInput = view.findViewById(R.id.explore_search_input);
        scanQRCodeBtn.setOnClickListener(v -> {
            // TODO: Bind QR Code Action
        });
        filterButton.setOnClickListener(v ->
                ((MainActivity) requireActivity()).openSecondaryFragment(new FilterFragment())
        );

        RecyclerView eventsView = view.findViewById(R.id.explore_recycler_view_events);
        adapter = new ExploreEventViewAdapter(
                getContext(),
                this,
                eventList
        );
        eventsView.setAdapter(adapter);
        eventsView.setLayoutManager(new LinearLayoutManager(getContext()));

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                applySearchFilter(s == null ? "" : s.toString());
            }
        });

        // Get events from DB
        EventDB.getAllJoinableEvents(user, new EventDB.LoadEventsCallback() {
            @Override
            public void onSuccess(ArrayList<Event> events) {
                loadEvents(events);
            }

            @Override
            public void onFailure(Exception e) {
                Log.e("ExploreFragment", "database query failed", e);
                allVisibleEvents.clear();
                eventList = new ArrayList<>();
                if (adapter != null) {
                    adapter.submitList(eventList);
                }
                showNoEventsText(true);
            }
        });
    }

    /**
     * Load and display all events that are not organized by the user.
     * @param events ArrayList of events
     */
    private void loadEvents(ArrayList<Event> events) {
        allVisibleEvents.clear();
        allVisibleEvents.addAll(filterPublicEvents(events));
        String query = searchInput == null || searchInput.getText() == null
                ? ""
                : searchInput.getText().toString();
        applySearchFilter(query);
    }

    /**
     * Removes private events from the public Explore listing.
     *
     * @param events Events returned by {@link EventDB}, or {@code null}.
     * @return Publicly visible events only.
     */
    private ArrayList<Event> filterPublicEvents(@Nullable ArrayList<Event> events) {
        ArrayList<Event> publicEvents = new ArrayList<>();
        if (events == null) {
            return publicEvents;
        }

        for (Event event : events) {
            if (event != null && !event.isPrivate()) {
                publicEvents.add(event);
            }
        }
        return publicEvents;
    }

    /**
     * Applies a lightweight client-side keyword filter to the currently loaded Explore events.
     *
     * @param rawQuery Search text entered by the entrant.
     */
    private void applySearchFilter(@Nullable String rawQuery) {
        String query = rawQuery == null ? "" : rawQuery.trim().toLowerCase(Locale.US);
        ArrayList<Event> filteredEvents = new ArrayList<>();

        if (query.isEmpty()) {
            filteredEvents.addAll(allVisibleEvents);
        } else {
            for (Event event : allVisibleEvents) {
                if (event == null) {
                    continue;
                }

                String name = event.getName() == null ? "" : event.getName().toLowerCase(Locale.US);
                String description = event.getDescription() == null ? "" : event.getDescription().toLowerCase(Locale.US);

                if (name.contains(query) || description.contains(query)) {
                    filteredEvents.add(event);
                }
            }
        }

        eventList = filteredEvents;
        if (adapter != null) {
            adapter.submitList(eventList);
        }
        showNoEventsText(eventList.isEmpty());
    }

    /**
     * Removes private events from the public Explore listing.
     *
     * @param events Events returned by {@link EventDB}, or {@code null}.
     * @return Publicly visible events only.
     */
    private ArrayList<Event> filterPublicEvents(@Nullable ArrayList<Event> events) {
        ArrayList<Event> publicEvents = new ArrayList<>();
        if (events == null) {
            return publicEvents;
        }

        for (Event event : events) {
            if (event != null && !event.isPrivate()) {
                publicEvents.add(event);
            }
        }
        return publicEvents;
    }

    /**
     * Helper function to hide/show text when events are found or not
     * @param show Boolean value, true to display, false to hide
     */
    private void showNoEventsText(boolean show) {
        if (show) {
            noEventsTest.setVisibility(View.VISIBLE);
        } else {
            noEventsTest.setVisibility(View.GONE);
        }
    }
}
