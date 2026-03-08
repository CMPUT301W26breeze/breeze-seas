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

import com.google.android.material.tabs.TabLayout;

import java.util.List;


public class AcceptedListFragment extends Fragment {
    private FinalList acceptedList;
    private OrganizerListAdapter adapter;
    private ListView listView;
    private ProgressBar waitingProgress;
    private String event="test_event_1";
    public void refreshData() {
        if (acceptedList != null && adapter != null) {
            if (waitingProgress != null) waitingProgress.setVisibility(View.VISIBLE);
            acceptedList.fetchFinalList(adapter, () -> {
                if (waitingProgress != null) waitingProgress.setVisibility(View.GONE);
            });
        }
    }
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_accepted_list, container, false);
        listView=view.findViewById(R.id.accept_frag_list_view);
        waitingProgress = view.findViewById(R.id.accepted_list_spinner);
        acceptedList=new FinalList(event);
        adapter=new OrganizerListAdapter(getContext(), R.layout.item_organizer_list,acceptedList.getAcceptedList());
        listView.setAdapter(adapter);
        return view;
    }

    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState){
        super.onViewCreated(view, savedInstanceState);
    }
    @Override
    public void onResume() {
        super.onResume();
        refreshData();
    }
}