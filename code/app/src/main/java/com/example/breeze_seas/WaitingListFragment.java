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
    private String eventId="test_event_1"; //Bundle expected from EventDetail Page
    private int lotterySize=3; //bundle expected? No defined attribute yet
    private int capacity=10; //bundle expected from EventDetail page/ organizer page


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
        waitingProgress.setVisibility(View.VISIBLE);
        waitingList.fetchWaitingList(adapter,()->{waitingProgress.setVisibility(View.GONE);});
        MaterialButton runLottery=view.findViewById(R.id.btn_run_lottery);
        runLottery.setOnClickListener(v->{
            Lottery lottery=new Lottery(eventId);
            waitingProgress.setVisibility(View.VISIBLE);
            lottery.runLottery(lotterySize,() -> {
                // Refresh data once lottery is committed
                waitingList.fetchWaitingList(adapter, () -> {
                    waitingProgress.setVisibility(View.GONE);
                    runLottery.setEnabled(true);
                });
            });
        });
        listView.setOnItemClickListener((parent,view1,position,id)->{
            User selected=(User) parent.getItemAtPosition(position);
            new androidx.appcompat.app.AlertDialog.Builder(requireContext()).setTitle("Remove from waiting list")
                    .setMessage("Are you sure you want to remove this user?").setPositiveButton("No", (dialog, which) -> dialog.dismiss())
                    .setNegativeButton("Yes", (dialog, which) -> {
                        waitingProgress.setVisibility(View.VISIBLE);
                        waitingList.removeEntrant(adapter,selected,()->{
                            waitingProgress.setVisibility(View.GONE);
                        });
                    })
                    .show();
        });
    }
    @Override
    public void onResume() {
        super.onResume();
        waitingProgress.setVisibility(View.VISIBLE);
        waitingList.fetchWaitingList(adapter, () -> {
            waitingProgress.setVisibility(View.GONE);
        });
    }

}

