package com.example.breeze_seas;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

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
    private String eventId="test_event_001"; //Bundle expected from EventDetail Page
    private int capacity=10; //bundle expected from EventDetail page/ organizer page

    public static WaitingListFragment newInstance(String eventId, int capacity) {
        WaitingListFragment fragment = new WaitingListFragment();
        Bundle args = new Bundle();
        args.putString("EVENT_ID", eventId);
        args.putInt("CAPACITY", capacity);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_waiting_list, container, false);
        listView=view.findViewById(R.id.waiting_frag_list_view);
        waitingProgress = view.findViewById(R.id.waiting_list_spinner);
        waitingList=new WaitingList(eventId,capacity);
        adapter=new OrganizerListAdapter(getContext(), R.layout.item_organizer_list,waitingList.getWaitingList());
        listView.setAdapter(adapter);
        return view;
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        MaterialButton runLottery=view.findViewById(R.id.btn_run_lottery);
        runLottery.setOnClickListener(v->{
            runLottery.setEnabled(false);
            Lottery lottery=new Lottery(eventId,2);
            waitingProgress.setVisibility(View.VISIBLE);
            lottery.runLottery(2,() -> {
                // Refresh data once lottery is committed
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
        waitingProgress.setVisibility(View.VISIBLE);
        waitingList.fetchWaiting(adapter, () -> {
            if (isAdded()) {
                waitingProgress.setVisibility(View.GONE);
            }
        });
    }
}
