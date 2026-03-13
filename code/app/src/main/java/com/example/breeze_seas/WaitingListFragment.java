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
    private Event currentEvent;


    public WaitingListFragment() { }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sessionViewModel = new ViewModelProvider(requireActivity()).get(SessionViewModel.class);
        currentEvent = sessionViewModel.getEventShown().getValue();
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_waiting_list, container, false);
        listView = view.findViewById(R.id.waiting_frag_list_view);
        waitingProgress = view.findViewById(R.id.waiting_list_spinner);
        if (currentEvent != null) {
            waitingList = new WaitingList(currentEvent, currentEvent.getWaitingListCapacity());
            adapter = new OrganizerListAdapter(getContext(), R.layout.item_organizer_list, waitingList.getUserList());
            listView.setAdapter(adapter);
        }
        return view;
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        MaterialButton runLotteryBtn = view.findViewById(R.id.btn_run_lottery);

        view.findViewById(R.id.back_btn).setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });

        runLotteryBtn.setOnClickListener(v -> {
            if (currentEvent == null) return;
            waitingProgress.setVisibility(View.VISIBLE);
            runLotteryBtn.setEnabled(false);


            Lottery lottery=new Lottery(currentEvent);
            lottery.onRunLottery(new StatusList.ListUpdateListener() {
                @Override
                public void onUpdate() {
                    if (isAdded()) {
                        refreshWaitingList();
                        runLotteryBtn.setEnabled(true);


                    }
                }
                @Override
                public void onError(Exception e) {
                    if (isAdded()) {
                        waitingProgress.setVisibility(View.GONE);
                        runLotteryBtn.setEnabled(true);
                    }
                }
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
        waitingList.refresh(new StatusList.ListUpdateListener() {
            @Override
            public void onUpdate() {
                if (isAdded()) {
                    waitingProgress.setVisibility(View.GONE);
                    adapter.notifyDataSetChanged();
                }
            }


            @Override
            public void onError(Exception e) {
                if (isAdded()) {
                    waitingProgress.setVisibility(View.GONE);
                }
            }
        });
    }
}

