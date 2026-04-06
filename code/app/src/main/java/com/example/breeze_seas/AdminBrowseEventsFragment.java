package com.example.breeze_seas;

import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;

/**
 * Represents the "Browse Events" screen for an administrator.
 * Displays a searchable, sorted list of all events and handles navigation to event details.
 * Uses {@link AdminViewModel} so the event list and listener persist across navigation
 * without requiring a re-fetch each time this fragment is recreated.
 */
public class AdminBrowseEventsFragment extends Fragment {

    private AdminBrowseEventsAdapter adapter;
    private AdminViewModel adminViewModel;
    private SessionViewModel sessionViewModel;
    private TextView noEventsText;

    private final Handler searchHandler = new Handler();
    private Runnable searchRunnable;

    public AdminBrowseEventsFragment() { super(R.layout.fragment_admin_browse_events); }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        adminViewModel = new ViewModelProvider(requireActivity()).get(AdminViewModel.class);
        sessionViewModel = new ViewModelProvider(requireActivity()).get(SessionViewModel.class);

        noEventsText = view.findViewById(R.id.abe_no_events_text);

        MaterialToolbar toolbar = view.findViewById(R.id.abe_topAppBar);
        toolbar.setNavigationOnClickListener(v -> {
            int containerId = ((View) requireView().getParent()).getId();
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(containerId, new AdminDashboardFragment())
                    .commit();
        });

        RecyclerView recyclerView = view.findViewById(R.id.abe_rv_events_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new AdminBrowseEventsAdapter(new ArrayList<>(), event -> {
            sessionViewModel.setEventShown(event);
            int containerId = ((View) requireView().getParent()).getId();
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(containerId, new AdminEventDetailsFragment())
                    .addToBackStack(null)
                    .commit();
        }, event -> {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Delete event")
                    .setMessage("\"" + event.getName() + "\" and all its data will be permanently deleted.")
                    .setPositiveButton("Delete", (dialog, which) ->
                            adminViewModel.deleteEvent(event, new EventDB.EventMutationCallback() {
                                @Override
                                public void onSuccess() { }

                                @Override
                                public void onFailure(Exception e) {
                                    if (getContext() != null) {
                                        Toast.makeText(getContext(), "Failed to delete event", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            }))
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        recyclerView.setAdapter(adapter);

        // Observe the live list, sorted and filtered by the adapter
        adminViewModel.getEvents().observe(getViewLifecycleOwner(), events -> {
            int previousCount = adapter.getItemCount();
            adapter.setEvents(events);
            noEventsText.setVisibility(adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
            if (events.size() < previousCount) {
                Toast.makeText(getContext(), "Event deleted", Toast.LENGTH_SHORT).show();
            }
        });

        // Debounced search, same as ExploreFragment
        EditText searchInput = view.findViewById(R.id.abe_search_input);
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (searchRunnable != null) searchHandler.removeCallbacks(searchRunnable);
                searchRunnable = () -> {
                    adapter.filter(s == null ? "" : s.toString());
                    noEventsText.setVisibility(adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
                };
                searchHandler.postDelayed(searchRunnable, 300);
            }
        });

        adminViewModel.startEventsListen();
    }
}
