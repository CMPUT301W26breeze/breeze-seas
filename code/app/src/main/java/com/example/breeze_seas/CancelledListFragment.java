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


public class CancelledListFragment extends Fragment {
    private CancelledList cancelledList;
    private OrganizerListAdapter adapter;
    private ListView listView;
    private ProgressBar waitingProgress;
    private String event="test_event_1";
    public void refreshData() {
        if (cancelledList != null && adapter != null) {
            if (waitingProgress != null) waitingProgress.setVisibility(View.VISIBLE);
            cancelledList.fetchFinalList(adapter, () -> {
                if (waitingProgress != null) waitingProgress.setVisibility(View.GONE);
            });
        }
    }
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_cancelled_list, container, false);
        listView=view.findViewById(R.id.cancel_frag_list_view);
        waitingProgress = view.findViewById(R.id.cancelled_list_spinner);
        cancelledList=new CancelledList(event);
        adapter=new OrganizerListAdapter(getContext(), R.layout.item_organizer_list,cancelledList.getCancelledList());
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