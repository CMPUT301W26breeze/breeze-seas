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

    private final StatusList.ListUpdateListener liveListener = new StatusList.ListUpdateListener() {
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
    };

    public PendingListFragment() { }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sessionViewModel = new ViewModelProvider(requireActivity()).get(SessionViewModel.class);
        currentEvent = sessionViewModel.getEventShown().getValue();
        if (currentEvent != null) {
            pendingList = currentEvent.getPendingList();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (pendingList != null) {
            waitingProgress.setVisibility(View.VISIBLE);
            pendingList.startListening(liveListener);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (pendingList != null) {
            pendingList.stopListening();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_pending_list, container, false);


        listView = view.findViewById(R.id.pending_frag_list_view);
        waitingProgress = view.findViewById(R.id.pending_list_spinner);

        if (pendingList != null) {
            adapter = new OrganizerListAdapter(getContext(), R.layout.item_organizer_list,
                    pendingList.getUserList(), "Pending", true);
            listView.setAdapter(adapter);
        }

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState){
        super.onViewCreated(view, savedInstanceState);
        listView.setRecyclerListener(v-> {
            v.removeCallbacks(null);
            v.clearAnimation();
        });
        listView.setOnItemClickListener((parent, view1, position, id) -> {
            User selected = pendingList.getUserList().get(position);
            ListDialogFragment dialog = new ListDialogFragment(selected,pendingList);
            dialog.show(getChildFragmentManager(), "Entrant Actions");
        });

    }

}
