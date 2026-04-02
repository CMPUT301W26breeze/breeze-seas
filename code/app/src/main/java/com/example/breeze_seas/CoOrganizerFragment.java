package com.example.breeze_seas;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

public class CoOrganizerFragment extends Fragment {

    private ListView listView;
    private ProgressBar progressBar;
    private OrganizerListAdapter adapter;
    private SessionViewModel sessionViewModel;
    private ListenerRegistration eventListener;

    private final ArrayList<User> eligibleUsers = new ArrayList<>();
    private final ArrayList<User> allUsers = new ArrayList<>();
    // includes organizers, co-organizers and users in participants collection
    private final ArrayList<String> excludedIds = new ArrayList<>();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sessionViewModel = new ViewModelProvider(requireActivity()).get(SessionViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_co_organizer, container, false);
        listView = view.findViewById(R.id.user_selection_list);
        progressBar = view.findViewById(R.id.loading_users_progress);

        adapter = new OrganizerListAdapter(getContext(), R.layout.item_organizer_list, eligibleUsers, "User", false);
        listView.setAdapter(adapter);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        listView.setOnItemClickListener((parent, v, position, id) -> {
            promoteUser(eligibleUsers.get(position));
        });

        View backButton = view.findViewById(R.id.co_organizer_back_button);
        backButton.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        // Observe the ViewModel. If the event object is changed refresh UI
        Event currentEvent = sessionViewModel.getEventShown().getValue();
        if (currentEvent != null) {
            sessionViewModel.getEventShown().observe(getViewLifecycleOwner(), updatedEvent -> {
                if (updatedEvent != null) {
                    updateEligibleList(updatedEvent);
                }
            });

            StatusList.ListUpdateListener subCollectionListener = new StatusList.ListUpdateListener() {
                @Override
                public void onUpdate() {
                    // Triggered when someone joins/leaves the participants subcollection
                    updateEligibleList(currentEvent);
                }

                @Override
                public void onError(Exception e) {
                    Toast.makeText(getContext(), "Sync Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            };


            if (currentEvent.getWaitingList() != null){
                currentEvent.getWaitingList().startListening(subCollectionListener);
            }
        }

        startListeningUser();
    }

    private void updateEligibleList(Event event) {
        excludedIds.clear();

        // all user ids to be excluded
        if (event.getOrganizerId() != null) {
            excludedIds.add(event.getOrganizerId());
        }
        if (event.getCoOrganizerId() != null) {
            excludedIds.addAll(event.getCoOrganizerId());
        }

        addIdsFromList(event.getWaitingList());
        addIdsFromList(event.getAcceptedList());
        addIdsFromList(event.getPendingList());
        addIdsFromList(event.getDeclinedList());

        eligibleUsers.clear();
        for (User user : allUsers) {
            if (!excludedIds.contains(user.getDeviceId())) {
                eligibleUsers.add(user);
            }
        }

        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private void startListeningUser() {
        Event currentEvent = sessionViewModel.getEventShown().getValue();
        if (currentEvent == null) return;

        progressBar.setVisibility(View.VISIBLE);
        FirebaseFirestore db = DBConnector.getDb();

        // fetch all users once
        db.collection("users").get().addOnCompleteListener(task -> {
            if (!task.isSuccessful() || task.getResult() == null) {
                progressBar.setVisibility(View.GONE);
                return;
            }

            allUsers.clear();
            for (QueryDocumentSnapshot doc : task.getResult()) {
                User user = doc.toObject(User.class);
                if (user != null) {
                    user.setDeviceId(doc.getId());
                    allUsers.add(user);
                }
            }

            // listen to event doc to get rid of new co-organizers
            eventListener = db.collection("events").document(currentEvent.getEventId())
                    .addSnapshotListener((eventSnapshot, eventError) -> {
                        if (eventError != null || eventSnapshot == null || !eventSnapshot.exists()) return;

                        ArrayList<String> coOrgs = (ArrayList<String>) eventSnapshot.get("coOrganizerId");
                        String organizerId = eventSnapshot.getString("organizerId");

                        if (organizerId != null) {
                            currentEvent.setOrganizerId(organizerId);
                        }
                        if (coOrgs != null) {
                            currentEvent.setCoOrganizerId(coOrgs);
                        }
                        updateEligibleList(currentEvent);
                        progressBar.setVisibility(View.GONE);
                    });
        });
    }

    private void addIdsFromList(StatusList list) {
        if (list != null && list.getUserList() != null) {
            for (User u : list.getUserList()) {
                excludedIds.add(u.getDeviceId());
            }
        }
    }

    private void promoteUser(User user) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Promote User")
                .setMessage("Add " + user.getUserName() + " as a Co-Organizer?")
                .setPositiveButton("Confirm", (d, w) -> promoteInDb(user))
                .setNegativeButton("Cancel", null)
                .show();
    }


    private void promoteInDb(User targetUser) {
        Event event = sessionViewModel.getEventShown().getValue();
        if (event == null) return;


        // Create the invite notification
        Notification invite = new Notification(
                NotificationType.CO_ORG_INVITE,
                "Co-Organizer Invitation",
                event.getEventId(),
                event.getName(),
                targetUser.getDeviceId()
        );


        NotificationService service = new NotificationService();
        service.sendNotification(invite).addOnSuccessListener(aVoid -> {
            Toast.makeText(getContext(), "Invitation sent to " + targetUser.getUserName(), Toast.LENGTH_SHORT).show();
        }).addOnFailureListener(e -> {
            Toast.makeText(getContext(), "Failed to send invite", Toast.LENGTH_SHORT).show();
        });
    }




    @Override
    public void onStop() {
        super.onStop();
        if (eventListener != null) {
            eventListener.remove();
        }
        Event currentEvent = sessionViewModel.getEventShown().getValue();
        if (currentEvent != null) {
            if (currentEvent.getWaitingList() != null) {
                currentEvent.getWaitingList().stopListening();
            }
        }
    }
}
