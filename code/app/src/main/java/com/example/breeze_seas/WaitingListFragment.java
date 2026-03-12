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

import com.google.android.material.button.MaterialButton;


public class WaitingListFragment extends Fragment {

    private WaitingList waitingList;
    private OrganizerListAdapter adapter;
    private ListView listView;
    private ProgressBar waitingProgress;
    private SessionViewModel sessionViewModel;

    private String eventId;
    private int capacity;

    public WaitingListFragment() { }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sessionViewModel= new ViewModelProvider(requireActivity()).get(SessionViewModel.class);
        Event currentEvent= sessionViewModel.getEventShown().getValue();
        if (currentEvent != null) {

            //getter required in the event class
            this.eventId = currentEvent.getId();
            this.capacity = currentEvent.getWaitingListCap();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_waiting_list, container, false);
        listView = view.findViewById(R.id.waiting_frag_list_view);
        waitingProgress = view.findViewById(R.id.waiting_list_spinner);
        waitingList = new WaitingList(eventId, capacity);
        adapter = new OrganizerListAdapter(getContext(), R.layout.item_organizer_list, waitingList.getWaitingList());
        listView.setAdapter(adapter);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        MaterialButton runLottery = view.findViewById(R.id.btn_run_lottery);

        runLottery.setOnClickListener(v -> {
            if (eventId == null) return;
            runLottery.setEnabled(false);
            Lottery lottery = new Lottery(eventId, capacity);
            waitingProgress.setVisibility(View.VISIBLE);
            lottery.runLottery(capacity, () -> {
                refreshWaitingList();
                runLottery.setEnabled(true);
            });
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshWaitingList();
    }

    private void refreshWaitingList() {
        if (waitingList == null) return;
        waitingProgress.setVisibility(View.VISIBLE);
        waitingList.fetchWaiting(adapter, () -> {
            if (isAdded()) {
                waitingProgress.setVisibility(View.GONE);
            }
        });
    }
}
