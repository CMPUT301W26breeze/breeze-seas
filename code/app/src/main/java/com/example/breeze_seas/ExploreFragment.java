package com.example.breeze_seas;

import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
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

/*** ExploreFragment is a top-level destination accessible via Bottom Navigation.
 *
 * <p>Current state:* - A placeholder screen was created to validate the navigation wiring and theme style.
 ** <p>Outstanding/Future Work:* - Replace the placeholder UI with the Explore feature implementation.
 */
public class ExploreFragment extends Fragment implements RecyclerViewClickListener {

    private TextView noEventsText;
    private View scanQRCodeBtn;
    private View filterButton;
    private EditText searchInput;
    private EventHandler exploreEventHandler;
    private Handler mhandler = new Handler();
    private Runnable keywordRunnable;
    private ArrayList<Event> displayedEventList = new ArrayList<>();
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
        Event selectedEvent = displayedEventList.get(position);

        // Stow event in EventHandler class
        exploreEventHandler.setEventShown(selectedEvent);

        // Switch to event details
        getActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, new EventDetailsFragment())
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Setup viewModel
        viewModel = new ViewModelProvider(requireActivity()).get(SessionViewModel.class);
        user = viewModel.getUser().getValue();

        // Setup EventHandler class if necessary
        if (!viewModel.exploreFragmentEventHandlerIsInitialized()) {
            viewModel.setExploreFragmentEventHandler(new EventHandler(getContext().getApplicationContext(),
                    EventDB.getAllJoinableEventsQuery(user)));
        }
        // Grab reference
        exploreEventHandler = viewModel.getExploreFragmentEventHandler();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Get realtime updated events from exploreEventHandler
        // The observer also runs on startup.
        exploreEventHandler.getEvents().observe(getViewLifecycleOwner(), this::loadEvents);

        // Bind views
        noEventsText = view.findViewById(R.id.explore_no_events_found_text);
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
                displayedEventList
        );
        eventsView.setAdapter(adapter);
        eventsView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Search field listener
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Remove previous calls (DEBOUNCE)
                if (keywordRunnable != null) {
                    mhandler.removeCallbacks(keywordRunnable);
                }

                // New runnable instance with latest string
                keywordRunnable = () -> {
                    exploreEventHandler.setKeywordString((s == null) ? "" : s.toString());
                };

                // New keyword search
                mhandler.postDelayed(keywordRunnable, 350);  // DEBOUNCE of 0.35 seconds.
            }
        });
    }

    /**
     * Load and display all events that are not organized by the user.
     * @param events ArrayList of events
     */
    private void loadEvents(ArrayList<Event> events) {
        if (adapter != null) {
            adapter.submitList(events);
        }
        showNoEventsText(events.isEmpty());
    }

    /**
     * Helper function to hide/show text when events are found or not
     * @param show Boolean value, true to display, false to hide
     */
    private void showNoEventsText(boolean show) {
        if (show) {
            noEventsText.setVisibility(View.VISIBLE);
        } else {
            noEventsText.setVisibility(View.GONE);
        }
    }
}
