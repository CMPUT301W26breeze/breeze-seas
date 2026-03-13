package com.example.breeze_seas;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.List;

/**
 * A fragment responsible for displaying a scrollable list of notifications.
 * Uses a RecyclerView to present the data retrieved via NotificationService
 */
public class AdminBrowseLogsFragment extends Fragment {

    private AdminBrowseLogsAdapter adapter;
    private final List<Notification> logList = new ArrayList<>();

    private final NotificationService notificationService = new NotificationService();

    public AdminBrowseLogsFragment() { super(R.layout.fragment_admin_browse_logs); }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MaterialToolbar toolbar = view.findViewById(R.id.abl_topAppBar);
        toolbar.setNavigationOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new AdminDashboardFragment())
                    .commit();
        });

        RecyclerView recyclerView = view.findViewById(R.id.abl_rv_logs);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new AdminBrowseLogsAdapter(logList);
        recyclerView.setAdapter(adapter);

        fetchLogs();
    }

    /**
     * Asynchronously retrieves all notifications/logs using NotificationService.
     * Clears and repopulates the local list, then notifies the adapter to refresh the UI upon success.
     * Logs the exception and displays an error message via a Toast if the retrieval fails.
     */
    private void fetchLogs() {
        notificationService.getAllNotifications(new NotificationService.OnNotificationLoadedListener() {
            @Override
            public void onNotificationLoaded(List<Notification> notifications) {
                logList.clear();

                if (notifications != null && !notifications.isEmpty()) {
                    logList.addAll(notifications);
                }

                adapter.notifyDataSetChanged();
            }

            @Override
            public void onError(Exception e) {
                Log.e("AdminLogs", "Failed to fetch logs", e);
                Toast.makeText(getContext(), "Failed to load system logs", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
