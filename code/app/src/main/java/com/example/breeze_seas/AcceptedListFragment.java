package com.example.breeze_seas;


import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;


import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;

/**
 * A fragment that displays the list of users currently on the accepted list for an event.
 */

public class AcceptedListFragment extends Fragment {
    private AcceptedList acceptedList;
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


    public AcceptedListFragment() {
    }

    private final ActivityResultLauncher<Intent> csvLauncher = registerForActivityResult
            (new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    acceptedList.exportCsv(requireContext(), uri);
                }
    });

    private void onExportClick(){
        Intent intent= new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/csv");
        csvLauncher.launch(intent);
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sessionViewModel = new ViewModelProvider(requireActivity()).get(SessionViewModel.class);
        currentEvent = sessionViewModel.getEventShown().getValue();
        if (currentEvent != null) {
            acceptedList = currentEvent.getAcceptedList();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (acceptedList != null) {
            waitingProgress.setVisibility(View.VISIBLE);
            acceptedList.startListening(liveListener);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (acceptedList != null) {
            acceptedList.stopListening();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_accepted_list, container, false);


        listView = view.findViewById(R.id.accept_frag_list_view);
        waitingProgress = view.findViewById(R.id.accepted_list_spinner);
        Button exportButton=view.findViewById(R.id.btn_export_csv);
        exportButton.setOnClickListener(v->{
            onExportClick();
        });

        if (acceptedList != null) {
            adapter = new OrganizerListAdapter(getContext(), R.layout.item_organizer_list,
                    acceptedList.getUserList(), "Accepted", false);
            listView.setAdapter(adapter);
        }

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        listView.setOnItemClickListener((parent, view1, position, id) -> {
            User selected = acceptedList.getUserList().get(position);
            ListDialogFragment dialog = new ListDialogFragment(selected,acceptedList);
            dialog.show(getChildFragmentManager(), "Entrant Actions");
        });
    }

}
