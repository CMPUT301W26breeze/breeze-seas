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

public class PendingListFragment extends Fragment {
    private InvitationList inviteList;
    private OrganizerListAdapter adapter;
    private ListView listView;
    private ProgressBar waitingProgress;
    private SessionViewModel sessionViewModel;

    private String eventId;
    private int capacity;

    public PendingListFragment() { }

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
        View view = inflater.inflate(R.layout.fragment_pending_list, container, false);

        listView = view.findViewById(R.id.pending_frag_list_view);
        waitingProgress = view.findViewById(R.id.pending_list_spinner);


        inviteList = new InvitationList(eventId, capacity);
        adapter = new OrganizerListAdapter(getContext(), R.layout.item_organizer_list, inviteList.getInvitedList());
        listView.setAdapter(adapter);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshPendingList();
    }

    private void refreshPendingList() {
        if (inviteList == null) return;
        waitingProgress.setVisibility(View.VISIBLE);
        inviteList.fetchPending(adapter, () -> {
            if (isAdded()) {
                waitingProgress.setVisibility(View.GONE);
            }
        });
    }
}
