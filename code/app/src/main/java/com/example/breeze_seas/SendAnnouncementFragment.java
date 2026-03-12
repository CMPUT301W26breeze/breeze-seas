package com.example.breeze_seas;

import static com.example.breeze_seas.NotificationType.ANNOUNCEMENT_CANCELLED;
import static com.example.breeze_seas.NotificationType.ANNOUNCEMENT_SELECTED;
import static com.example.breeze_seas.NotificationType.ANNOUNCEMENT_WAITLIST;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputLayout;

public class SendAnnouncementFragment extends Fragment {

    private AppBarLayout appBarLayout;
    private TabLayout tabLayout;
    private TextInputLayout notificationTextBox;
    private MaterialButton sendButton;

    private NotificationService notificationService = new NotificationService();



    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_send_announcement,
                container, false);

        appBarLayout = view.findViewById(R.id.app_bar_layout);
        tabLayout = view.findViewById(R.id.user_tabs);
        notificationTextBox = view.findViewById(R.id.notification_text_box);
        sendButton = view.findViewById(R.id.send_button);

        return view;

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        SessionViewModel viewModel;
        viewModel = new ViewModelProvider(requireActivity()).get(SessionViewModel.class);
        String deviceId = viewModel.getAndroidID().getValue();

            if (deviceId != null) {
                Log.d("BreezeSeas", "Observed ID: " + deviceId);

            }


        sendButton.setOnClickListener(v -> {

            String content = notificationTextBox.getEditText().getText().toString();
            String userId = deviceId;
            Notification notification;
            NotificationType type = ANNOUNCEMENT_WAITLIST;

            // Check lists of users the notification will be sent to
            int selectedTabPosition = tabLayout.getSelectedTabPosition();
            if (selectedTabPosition == 0) {
                type = ANNOUNCEMENT_WAITLIST;
            } else if (selectedTabPosition == 1) {
                type = ANNOUNCEMENT_SELECTED;
            } else if (selectedTabPosition == 2) {
                type = ANNOUNCEMENT_CANCELLED;
            }

            String eventId = "hello bryant";
            String eventName = "hello world";

            notification = new Notification(type, content, eventId, eventName,userId);
            notificationService.sendNotification(notification);
            // TODO: Get eventId and eventName from organizer event details
            Toast.makeText(getContext(), "Announcement Sent!(TODO)",
                    Toast.LENGTH_SHORT).show();
        });
    }
}
