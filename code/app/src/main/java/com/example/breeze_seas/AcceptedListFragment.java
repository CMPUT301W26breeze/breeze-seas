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


public class AcceptedListFragment extends Fragment {
    private FinalList acceptedList;
    private OrganizerListAdapter adapter;
    private ListView listView;
    private ProgressBar waitingProgress;
    private SessionViewModel sessionViewModel;

    private String eventId;
    private int capacity;


    public AcceptedListFragment() { }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionViewModel = new ViewModelProvider(requireActivity()).get(SessionViewModel.class);

        Event currentEvent = sessionViewModel.getEventShown().getValue();
        if (currentEvent != null) {
            // Requires Event getter
            this.eventId = currentEvent.getId();
            this.capacity = currentEvent.getWaitingListCap();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_accepted_list, container, false);

        listView = view.findViewById(R.id.accept_frag_list_view);
        waitingProgress = view.findViewById(R.id.accepted_list_spinner);
        acceptedList = new FinalList(eventId, capacity);
        adapter = new OrganizerListAdapter(getContext(), R.layout.item_organizer_list, acceptedList.getFinalList());
        listView.setAdapter(adapter);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshAcceptedList();
    }

    private void refreshAcceptedList() {
        if (acceptedList == null) return;
        waitingProgress.setVisibility(View.VISIBLE);
        acceptedList.fetchAccepted(adapter, () -> {
            if (isAdded()) {
                waitingProgress.setVisibility(View.GONE);
            }
        });
    }
}