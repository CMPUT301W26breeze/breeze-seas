package com.example.breeze_seas;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;


/**
 * A fragment for users to see their notifications.
 */
public class NotificationFragment extends Fragment {

    private UserDB userDBInstance = new UserDB();
    private User currentUser;
    private NotificationService notificationService = new NotificationService();
    private RecyclerView notificationsRecycler;
    private LinearLayout optOutStateLayout, emptyStateLayout;
    private List<Notification> notifications = new ArrayList<>();
    private NotificationEntryAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notifications_inbox,
                container, false);
        adapter = new NotificationEntryAdapter(notifications, this::onNotificationClicked);
        notificationsRecycler = view.findViewById(R.id.notifications_recycler);
        notificationsRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        notificationsRecycler.setAdapter(adapter);
        optOutStateLayout = view.findViewById(R.id.notifications_opt_out_state);
        emptyStateLayout = view.findViewById(R.id.notifications_empty_state);


        return view;
    }

    private void onNotificationClicked(Notification notification) {
        if (notification.getType() == NotificationType.CO_ORG_INVITE) {
            new android.app.AlertDialog.Builder(requireContext())
                    .setTitle("Event Invitation")
                    .setMessage("Accept invitation for " + notification.getEventName() + "?")
                    .setPositiveButton("Accept", (dialog, which) -> {


                        FirebaseFirestore db = DBConnector.getDb();
                        db.collection("events").document(notification.getEventId())
                                .update("coOrganizerId", com.google.firebase.firestore.FieldValue.arrayUnion(notification.getUserId()))
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(getContext(), "Accepted!", Toast.LENGTH_SHORT).show();
                                    notification.setSeen(true);
                                    adapter.notifyDataSetChanged();
                                });
                    })
                    .setNegativeButton("Decline", (dialog, which) -> {
                        dialog.dismiss();
                    })
                    .show();
        }
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize the view model, get the deviceId, and fetch user data
        SessionViewModel viewModel;
        viewModel = new ViewModelProvider(requireActivity()).get(SessionViewModel.class);
        viewModel.getAndroidID().observe(getViewLifecycleOwner(), deviceId -> {
            if (deviceId != null) {
                Log.d("BreezeSeas", "Observed ID: " + deviceId);
                fetchUserData(deviceId);
            }
        });




    }

    /**
     * Fetches user data and displays the notifications.
     *
     * @param deviceId Device ID of the logged-in user.
     */
    private void fetchUserData(String deviceId) {
        userDBInstance.getUser(deviceId, new UserDB.OnUserLoadedListener() {
            @Override
            public void onUserLoaded(User user) {

                currentUser = user;

                // Display notifications
                if (currentUser.notificationEnabled()) {
                    notificationService.getNotifications(currentUser.getDeviceId(),
                            new NotificationService.OnNotificationLoadedListener(){

                                public void onNotificationLoaded(List<Notification> fetchedNotifications) {
                                    notifications.addAll(fetchedNotifications);
                                    if (!notifications.isEmpty()) {
                                        notifications.clear();
                                        notifications.addAll(fetchedNotifications);
                                        notificationsRecycler.setVisibility(View.VISIBLE);
                                        emptyStateLayout.setVisibility(View.GONE);
                                        adapter.notifyDataSetChanged();

                                    }
                                    else {
                                        notificationsRecycler.setVisibility(GONE);
                                        emptyStateLayout.setVisibility(VISIBLE);
                                    }
                                }
                                @Override
                                public void onError(Exception e) {
                                    Log.e("DB_ERROR", "Error fetching notifications", e);
                                }
                            });
                } else {
                    notificationsRecycler.setVisibility(GONE);
                    optOutStateLayout.setVisibility(VISIBLE);
                }
            }

            @Override
            public void onError(Exception e) {
                Log.e("DB_ERROR", "Error fetching user by deviceId", e);
            }
        });

    }
}