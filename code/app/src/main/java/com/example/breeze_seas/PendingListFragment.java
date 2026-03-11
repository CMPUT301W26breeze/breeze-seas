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

public class PendingListFragment extends Fragment {
    private InvitationList inviteList;
    private OrganizerListAdapter adapter;
    private ListView listView;
    private ProgressBar waitingProgress;
    private String eventId = "test_event_001"; //Bundle expected from EventDetail Page
    private int capacity = 10; //bundle expected from EventDetail page/ organizer page

    public static PendingListFragment newInstance(String eventId, int capacity) {
        PendingListFragment fragment = new PendingListFragment();
        Bundle args = new Bundle();
        args.putString("EVENT_ID", eventId);
        args.putInt("CAPACITY", capacity);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_pending_list, container, false);
        listView = view.findViewById(R.id.pending_frag_list_view);
        waitingProgress = view.findViewById(R.id.pending_list_spinner);
        inviteList = new InvitationList(eventId, capacity);
        adapter = new OrganizerListAdapter(getContext(), R.layout.item_organizer_list, inviteList.getInvitedList());
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
        inviteList.fetchPending(adapter, () -> {
            waitingProgress.setVisibility(View.GONE);
        });
    }
}
