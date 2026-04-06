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
 * A fragment that displays the list of users currently on the declined list for an event.
 */

public class DeclinedListFragment extends Fragment {
    private DeclinedList declinedList;
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


    public DeclinedListFragment() { }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sessionViewModel = new ViewModelProvider(requireActivity()).get(SessionViewModel.class);
        currentEvent = sessionViewModel.getEventShown().getValue();
        if (currentEvent != null) {
            declinedList = currentEvent.getDeclinedList();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (declinedList != null) {
            if (declinedList.getUserList().isEmpty()) {
                waitingProgress.setVisibility(View.VISIBLE);
            }
            declinedList.startListening(liveListener);
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
        }
    }


    @Override
    public void onStop() {
        super.onStop();
        if (declinedList != null) {
            declinedList.stopListening();
        }
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_declined_list, container, false);

        listView = view.findViewById(R.id.declined_frag_list_view);
        waitingProgress = view.findViewById(R.id.declined_list_spinner);

        if (declinedList != null) {
            adapter = new OrganizerListAdapter(getContext(), R.layout.item_organizer_list,
                    declinedList.getUserList(), "Declined", false);
            listView.setAdapter(adapter);
        }


        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        listView.setOnItemClickListener((parent, view1, position, id) -> {
            User selected = declinedList.getUserList().get(position);
            ListDialogFragment dialog = new ListDialogFragment(selected,declinedList);
            dialog.show(getChildFragmentManager(), "Entrant Actions");
        });
    }

}
