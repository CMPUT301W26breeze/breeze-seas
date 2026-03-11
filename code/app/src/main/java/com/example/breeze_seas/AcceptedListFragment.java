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
    private String eventId="test_event_001"; //Bundle expected from EventDetail Page
    private int capacity=10; //bundle expected from EventDetail page/ organizer page

    public static AcceptedListFragment newInstance(String eventId, int capacity) {
        AcceptedListFragment fragment = new AcceptedListFragment();
        Bundle args = new Bundle();
        args.putString("EVENT_ID", eventId);
        args.putInt("CAPACITY", capacity);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_accepted_list, container, false);
        listView=view.findViewById(R.id.accept_frag_list_view);
        waitingProgress = view.findViewById(R.id.accepted_list_spinner);
        acceptedList=new FinalList(eventId,capacity);
        adapter=new OrganizerListAdapter(getContext(), R.layout.item_organizer_list,acceptedList.getFinalList());
        listView.setAdapter(adapter);
        return view;
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }
    @Override
    public void onResume() {
        super.onResume();
        waitingProgress.setVisibility(View.VISIBLE);
        acceptedList.fetchAccepted(adapter, () -> {
            waitingProgress.setVisibility(View.GONE);
        });
    }

}