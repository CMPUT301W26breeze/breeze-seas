package com.example.breeze_seas;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.google.android.material.tabs.TabLayout;

import java.util.List;


public class CancelledListFragment extends Fragment {
    private CancelledList cancelledList;
    private OrganizerListAdapter adapter;
    private ListView listView;
    private ProgressBar waitingProgress;
    private SessionViewModel sessionViewModel;

    private String eventId;
    private int capacity;


    public CancelledListFragment() { }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sessionViewModel = new ViewModelProvider(requireActivity()).get(SessionViewModel.class);

        Event currentEvent = sessionViewModel.getEventShown().getValue();
        if (currentEvent != null) {
            // requires Event getter
            this.eventId = currentEvent.getId();
            this.capacity = currentEvent.getWaitingListCap();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_cancelled_list, container, false);

        listView = view.findViewById(R.id.cancel_frag_list_view);
        waitingProgress = view.findViewById(R.id.cancelled_list_spinner);

        cancelledList = new CancelledList(eventId, capacity);
        adapter = new OrganizerListAdapter(getContext(), R.layout.item_organizer_list, cancelledList.getCancelledList());
        listView.setAdapter(adapter);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshCancelledList();
    }

    private void refreshCancelledList() {
        if (cancelledList == null) return;
        waitingProgress.setVisibility(View.VISIBLE);
        cancelledList.fetchCancelled(adapter, () -> {
            if (isAdded()) {
                waitingProgress.setVisibility(View.GONE);
            }
        });
    }
}