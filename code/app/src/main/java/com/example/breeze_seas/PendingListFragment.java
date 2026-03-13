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

/**
 * A fragment that displays the list of users currently on the pending list for an event.
 */

public class PendingListFragment extends Fragment {
    private PendingList pendingList;
    private OrganizerListAdapter adapter;
    private ListView listView;
    private ProgressBar waitingProgress;
    private SessionViewModel sessionViewModel;
    private Event currentEvent;


    public PendingListFragment() { }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sessionViewModel = new ViewModelProvider(requireActivity()).get(SessionViewModel.class);
        currentEvent = sessionViewModel.getEventShown().getValue();
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_pending_list, container, false);


        listView = view.findViewById(R.id.pending_frag_list_view);
        waitingProgress = view.findViewById(R.id.pending_list_spinner);


        if (currentEvent != null) {
            pendingList = new PendingList(currentEvent, currentEvent.getEventCapacity());
            adapter = new OrganizerListAdapter(getContext(), R.layout.item_organizer_list, pendingList.getUserList());
            listView.setAdapter(adapter);
        }


        return view;
    }


    @Override
    public void onResume() {
        super.onResume();
        refreshPendingList();
    }


    /**
     * Rebuilds the pending list by fetching the latest participant data from Firestore.
     * Toggles the visibility of the {@code waitingProgress} spinner during the update
     * and refreshes the adapter upon success.
     */

    private void refreshPendingList() {
        if (pendingList == null) return;
        waitingProgress.setVisibility(View.VISIBLE);


        pendingList.refresh(new StatusList.ListUpdateListener() {
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
