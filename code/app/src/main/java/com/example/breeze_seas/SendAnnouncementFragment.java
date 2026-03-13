package com.example.breeze_seas;

import static com.example.breeze_seas.NotificationType.ANNOUNCEMENT_CANCELLED;
import static com.example.breeze_seas.NotificationType.ANNOUNCEMENT_SELECTED;
import static com.example.breeze_seas.NotificationType.ANNOUNCEMENT_WAITLIST;

import android.os.Bundle;
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

import java.util.ArrayList;

/**
 * A Fragment that provides the interface for organizers to send announcements.
 * It manages the tab selection for different audience types (Waitlist, Selected, Cancelled)
 */
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

        sendButton.setOnClickListener(v -> {
            SessionViewModel viewModel = new ViewModelProvider(requireActivity()).get(SessionViewModel.class);

            // Get the current event and set the variables for the notification
            viewModel.getEventShown().observe(getViewLifecycleOwner(), eventShown -> {
                if (eventShown != null) {
                    ArrayList<User> userSentList = new ArrayList<>();
                    String userSent = "";
                    NotificationType type = ANNOUNCEMENT_WAITLIST;
                    String content = notificationTextBox.getEditText().getText().toString();
                    String eventId = eventShown.getEventId();
                    String eventName = eventShown.getName();
                    int selectedTabPosition = tabLayout.getSelectedTabPosition();

                    // If announcement is sent to people in the waiting list
                    if (selectedTabPosition == 0) {
                        type = ANNOUNCEMENT_WAITLIST;
                        userSentList = eventShown.getWaitingList().getUserList();

                    // If announcement is sent to people in the pending list
                    } else if (selectedTabPosition == 1) {
                        type = ANNOUNCEMENT_SELECTED;
                        userSentList = eventShown.getPendingList().getUserList();

                    // If announcement is sent to people in the cancelled list
                    } else if (selectedTabPosition == 2) {
                        type = ANNOUNCEMENT_CANCELLED;
                        userSentList = eventShown.getDeclinedList().getUserList();
                    }

                    if (!userSentList.isEmpty()) {
                        for (int i = 0; i < userSentList.size(); i++) {
                            userSent = userSentList.get(i).getDeviceId();
                            // Send the notification to the database
                            Notification notification = new Notification(type, content, eventId, eventName, userSent);
                            notificationService.sendNotification(notification);
                        }
                        Toast.makeText(getContext(), "Announcement Sent!",
                                Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "No users in this list!",
                                Toast.LENGTH_SHORT).show();
                    }


                }
            });
        });
    }
}
