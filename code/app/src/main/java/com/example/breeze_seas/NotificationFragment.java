package com.example.breeze_seas;

import static android.view.View.VISIBLE;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Arrays;
import java.util.List;

public class NotificationFragment extends Fragment {

    private UserDB userDBInstance = new UserDB();
    private User currentUser;
    private RecyclerView notificationsRecycler;

    private LinearLayout emptyStateLayout;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notifications_inbox,
                container, false);

        notificationsRecycler = view.findViewById(R.id.notifications_recycler);
        emptyStateLayout = view.findViewById(R.id.notifications_empty_state);


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

        if (currentUser.notificationEnabled()) {

        } else {
            emptyStateLayout.setVisibility(VISIBLE);
        }


    }

    private void fetchUserData(String deviceId) {
        userDBInstance.getUser(deviceId, new UserDB.OnUserLoadedListener() {
            @Override
            public void onUserLoaded(User user) {

                currentUser = user;

            }

            @Override
            public void onError(Exception e) {
                currentUser = new User();
            }
        });

    }
}