package com.example.breeze_seas;

import static android.view.View.VISIBLE;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class NotificationFragment extends Fragment {

    private UserDB userDBInstance = new UserDB();
    private User currentUser;
    private NotificationService notificationService = new NotificationService();
    private RecyclerView notificationsRecycler;
    private LinearLayout optOutStateLayout;
    private List<Notification> notifications = new ArrayList<>();
    private NotificationEntryAdapter adapter =
            new NotificationEntryAdapter(notifications);

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notifications_inbox,
                container, false);

        notificationsRecycler = view.findViewById(R.id.notifications_recycler);
        notificationsRecycler.setAdapter(adapter);
        optOutStateLayout = view.findViewById(R.id.notifications_empty_state);


        return view;
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
                                    notifications = fetchedNotifications;
                                    if (notifications.isEmpty()) {

                                    }
                                    adapter.notifyDataSetChanged();
                                }
                                @Override
                                public void onError(Exception e) {
                                    Log.e("DB_ERROR", "Error fetching notifications", e);
                                }
                            });
                } else {
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