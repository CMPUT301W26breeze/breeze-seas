package com.example.breeze_seas;


import static com.example.breeze_seas.NotificationType.ANNOUNCEMENT_WAITLIST;
import static com.example.breeze_seas.NotificationType.LOSS;
import static com.example.breeze_seas.NotificationType.WIN;

import android.os.Bundle;


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
import android.widget.Toast;

import java.util.ArrayList;

/**
 * A fragment that displays the list of users currently on the waiting list for an event.
 * <p>
 * This fragment allows organizers to view entrants, run the lottery selection process,
 * and automatically sends out {@link NotificationType#WIN} and {@link NotificationType#LOSS}
 * notifications to participants based on the lottery results.
 * </p>
 */

public class WaitingListFragment extends Fragment {


    private WaitingList waitingList;
    ArrayList<User> waitingListUserList = new ArrayList<>();
    private OrganizerListAdapter adapter;
    private ListView listView;
    private ProgressBar waitingProgress;
    private SessionViewModel sessionViewModel;
    private Event currentEvent;
    private NotificationService notificationService = new NotificationService();

    private final StatusList.ListUpdateListener liveListener = new StatusList.ListUpdateListener() {
        @Override
        public void onUpdate() {
            if (isAdded()) {
                waitingProgress.setVisibility(View.GONE);
                waitingListUserList.clear();
                waitingListUserList.addAll(waitingList.getUserList());
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



    public WaitingListFragment() { }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sessionViewModel = new ViewModelProvider(requireActivity()).get(SessionViewModel.class);
        currentEvent = sessionViewModel.getEventShown().getValue();
        if (currentEvent != null) {
            waitingList = currentEvent.getWaitingList();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (waitingList != null) {
            waitingProgress.setVisibility(View.VISIBLE);
            waitingList.startListening(liveListener);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (waitingList != null) {
            waitingList.stopListening();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_waiting_list, container, false);
        listView = view.findViewById(R.id.waiting_frag_list_view);
        waitingProgress = view.findViewById(R.id.waiting_list_spinner);
        if (waitingList != null) {
            adapter = new OrganizerListAdapter(getContext(), R.layout.item_organizer_list, waitingList.getUserList(), "Waiting", false);
            listView.setAdapter(adapter);
        }
        return view;
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Button runLotteryBtn = view.findViewById(R.id.btn_run_lottery);

        view.findViewById(R.id.back_btn).setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });

        listView.setOnItemClickListener((parent, view1, position, id) -> {
            User selected = waitingList.getUserList().get(position);
            ListDialogFragment dialog = new ListDialogFragment(selected,waitingList);
            dialog.show(getChildFragmentManager(), "Entrant Actions");
        });

        runLotteryBtn.setOnClickListener(v -> {
            if (currentEvent == null) {
                return;
            }
            waitingProgress.setVisibility(View.VISIBLE);
            runLotteryBtn.setEnabled(false);


            Lottery lottery=new Lottery(currentEvent);
            lottery.onRunLottery(new StatusList.ListUpdateListener() {
                @Override
                public void onUpdate() {
                    if (isAdded()) {
                        // Notifications to winners and losers of the lottery
                        ArrayList<User> winnerList = currentEvent.getPendingList().getUserList();
                        ArrayList<User> loserList = new ArrayList<>(waitingListUserList);
                        loserList.removeAll(winnerList);

                        String userSent ="";
                        String content = "";
                        String eventId = currentEvent.getEventId();
                        String eventName = currentEvent.getName();

                        // Win announcement
                        if (!winnerList.isEmpty()) {
                            for (int i = 0; i < winnerList.size(); i++) {
                                userSent = winnerList.get(i).getDeviceId();
                                // Send the notification to the database
                                Notification notification = new Notification(WIN, content, eventId, eventName, userSent);
                                notificationService.sendNotification(notification);
                            }
                            Toast.makeText(getContext(), "Notification Sent!",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getContext(), "No users in this list!",
                                    Toast.LENGTH_SHORT).show();
                        }

                        // Loss announcement
                        if (!loserList.isEmpty()) {
                            for (int i = 0; i < loserList.size(); i++) {
                                userSent = loserList.get(i).getDeviceId();
                                // Send the notification to the database
                                Notification notification = new Notification(LOSS, content, eventId, eventName, userSent);
                                notificationService.sendNotification(notification);
                            }
                            Toast.makeText(getContext(), "Notification Sent!",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getContext(), "No users in this list!",
                                    Toast.LENGTH_SHORT).show();
                        }

                        runLotteryBtn.setEnabled(true);


                    }
                }
                @Override
                public void onError(Exception e) {
                    if (isAdded()) {
                        waitingProgress.setVisibility(View.GONE);
                        runLotteryBtn.setEnabled(true);
                        Toast.makeText(getContext(), "Lottery Failed", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        });
    }
}
