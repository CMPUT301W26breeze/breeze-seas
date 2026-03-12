package com.example.breeze_seas;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/*** ExploreFragment is a top-level destination accessible via Bottom Navigation.
 *
 * <p>Current state:* - A placeholder screen was created to validate the navigation wiring and theme style.
 ** <p>Outstanding/Future Work:* - Replace the placeholder UI with the Explore feature implementation.
 */
public class ExploreFragment extends Fragment implements RecyclerViewClickListener {

    private TextView noEventsTest;
    private View filterBtn;
    private View scanQRCodeBtn;
    private EventDB eventDBInstance;
    private List<Event> eventList;
    private SessionViewModel viewModel;

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

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Bind views
        noEventsTest = view.findViewById(R.id.explore_no_events_found_text);
        filterBtn = view.findViewById(R.id.explore_filter_button);
        scanQRCodeBtn = view.findViewById(R.id.explore_QRCode_floating_button);
        filterBtn.setOnClickListener(v ->
                ((MainActivity) requireActivity()).openSecondaryFragment(new FilterFragment())
        );
        scanQRCodeBtn.setOnClickListener(v -> {
            // TODO: Bind QR Code Action
        });

        // EventDB instance
        eventDBInstance = EventDB.getInstance();

        // Get events from DB
        eventDBInstance.getAllEvents(new EventDB.LoadEventsCallback() {
            @Override
            public void onSuccess(List<Event> events) {
                loadEvents(view, events);
            }

            @Override
            public void onFailure(Exception e) {
                eventList = null;
                showNoEventsText(true);
            }
        });
    }

    private void loadEvents(View view, List<Event> events) {
        eventList = events;
        // Guard against null
        if (eventList == null) {
            eventList = new ArrayList<Event>();
        }

        // Initiate adapter
        RecyclerView eventsView = view.findViewById(R.id.explore_recycler_view_events);
        ExploreEventViewAdapter adapter =  new ExploreEventViewAdapter(getContext(), this, eventList);
        eventsView.setAdapter(adapter);
        eventsView.setLayoutManager(new LinearLayoutManager(getContext()));

        // After initializing, check whether events are present or not
        if (adapter.getItemCount() == 0) {
            showNoEventsText(true);
        }
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
