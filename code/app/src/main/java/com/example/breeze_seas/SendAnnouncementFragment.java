package com.example.breeze_seas;

import static com.example.breeze_seas.NotificationType.ANNOUNCEMENT_CANCELLED;
import static com.example.breeze_seas.NotificationType.ANNOUNCEMENT_SELECTED;
import static com.example.breeze_seas.NotificationType.ANNOUNCEMENT_WAITLIST;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import java.util.ArrayList;

/**
 * A Fragment that provides the interface for organizers to send announcements.
 * It manages the tab selection for different audience types (Waitlist, Selected, Cancelled)
 */
public class SendAnnouncementFragment extends Fragment {

    private TextView[] audienceTabs;
    private TextView eventNameView;
    private AppCompatEditText notificationInput;
    private View sendButton;
    private final NotificationService notificationService = new NotificationService();
    private Event currentEvent;
    private int selectedTabIndex;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_send_announcement,
                container, false);

        eventNameView = view.findViewById(R.id.announcement_event_name);
        notificationInput = view.findViewById(R.id.notification_text_input);
        sendButton = view.findViewById(R.id.send_button);
        audienceTabs = new TextView[]{
                view.findViewById(R.id.announcement_tab_waiting),
                view.findViewById(R.id.announcement_tab_selected),
                view.findViewById(R.id.announcement_tab_cancelled)
        };

        return view;

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        SessionViewModel viewModel = new ViewModelProvider(requireActivity()).get(SessionViewModel.class);
        view.findViewById(R.id.announcement_back_button).setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack()
        );

        for (int i = 0; i < audienceTabs.length; i++) {
            final int tabIndex = i;
            audienceTabs[i].setOnClickListener(v -> selectAudienceTab(tabIndex));
        }
        selectAudienceTab(0);

        viewModel.getEventShown().observe(getViewLifecycleOwner(), eventShown -> {
            currentEvent = eventShown;
            eventNameView.setText(eventShown == null || eventShown.getName() == null || eventShown.getName().trim().isEmpty()
                    ? getString(R.string.send_announcement_event_placeholder)
                    : eventShown.getName());
        });

        // Send button
        sendButton.setOnClickListener(v -> {
            if (currentEvent == null) {
                Toast.makeText(getContext(), R.string.send_announcement_missing_event, Toast.LENGTH_SHORT).show();
                return;
            }

            ArrayList<User> userSentList = new ArrayList<>();
            NotificationType type = ANNOUNCEMENT_WAITLIST;
            String content = notificationInput.getText() == null ? "" : notificationInput.getText().toString().trim();
            String eventId = currentEvent.getEventId();
            String eventName = currentEvent.getName();

            if (selectedTabIndex == 0) {
                type = ANNOUNCEMENT_WAITLIST;
                userSentList = currentEvent.getWaitingList().getUserList();
            } else if (selectedTabIndex == 1) {
                type = ANNOUNCEMENT_SELECTED;
                userSentList = currentEvent.getAcceptedList().userList;
            } else if (selectedTabIndex == 2) {
                type = ANNOUNCEMENT_CANCELLED;
                userSentList = currentEvent.getDeclinedList().getUserList();
            }

            if (!userSentList.isEmpty()) {
                for (int i = 0; i < userSentList.size(); i++) {
                    String userSent = userSentList.get(i).getDeviceId();
                    Notification notification = new Notification(type, content, eventId, eventName, userSent);
                    notificationService.sendNotification(notification);
                }
                Toast.makeText(getContext(), "Announcement Sent!",
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "No users in this list!",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Updates the selected audience tab in the custom segmented control.
     *
     * @param index Audience tab index to activate.
     */
    private void selectAudienceTab(int index) {
        selectedTabIndex = index;
        if (audienceTabs == null) {
            return;
        }

        for (int i = 0; i < audienceTabs.length; i++) {
            boolean isSelected = i == index;
            audienceTabs[i].setActivated(isSelected);
            audienceTabs[i].setSelected(isSelected);
        }
    }
}
