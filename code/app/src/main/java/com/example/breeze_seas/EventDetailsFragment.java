package com.example.breeze_seas;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textview.MaterialTextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class EventDetailsFragment extends Fragment {
    private ShapeableImageView returnButton;
    private MaterialTextView eventTitle;
    private ShapeableImageView eventPoster;
    private MaterialButton viewQRCodeButton;
    private MaterialTextView eventCapacity;
    private MaterialTextView eventWaitingListCount;
    private MaterialTextView eventStartDate;
    private MaterialTextView eventEndDate;
    private MaterialTextView eventDescription;
    private MaterialButton joinWaitingListButton;
    private MaterialButton leaveWaitingListButton;
    private MaterialTextView eventInviteText;
    private MaterialButton acceptInviteButton;
    private MaterialButton declineInviteButton;
    private MaterialTextView eventInviteAcceptedText;
    private MaterialTextView eventInviteDeclinedText;

    private SessionViewModel viewModel;
    private Event eventShown;
    private WaitingList waitingList;
    private PendingList pendingList;
    private AcceptedList acceptedList;
    private DeclinedList declinedList;
    private User user;

    public EventDetailsFragment () {
        super(R.layout.fragment_event_details);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Setup viewModel
        viewModel = new ViewModelProvider(requireActivity()).get(SessionViewModel.class);

        // Grab event and user from SessionViewModel
        user = viewModel.getUser().getValue();
        eventShown = viewModel.getEventShown().getValue();
        assert eventShown != null;
        waitingList = eventShown.getWaitingList();
        pendingList = eventShown.getPendingList();
        acceptedList = eventShown.getAcceptedList();
        declinedList = eventShown.getDeclinedList();

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        // Bind all views
        eventTitle = view.findViewById(R.id.event_details_event_title);
        eventPoster = view.findViewById(R.id.event_details_event_photo);
        eventCapacity = view.findViewById(R.id.event_details_event_capacity);
        eventWaitingListCount = view.findViewById(R.id.event_details_event_waiting_list_count);
        eventStartDate = view.findViewById(R.id.event_details_event_start_date);
        eventEndDate = view.findViewById(R.id.event_details_event_end_date);
        eventDescription = view.findViewById(R.id.event_details_event_description);
        eventInviteText = view.findViewById(R.id.event_details_invite_text);
        eventInviteAcceptedText = view.findViewById(R.id.event_details_invite_accepted_text);
        eventInviteDeclinedText = view.findViewById(R.id.event_details_invite_declined_text);

        // Buttons
        returnButton = view.findViewById(R.id.event_details_return_button);
        returnButton.setOnClickListener(v -> {
            getParentFragmentManager()
                    .popBackStack();
        });

        viewQRCodeButton = view.findViewById(R.id.event_details_view_QRCode_button);
        viewQRCodeButton.setOnClickListener(v -> {
            // TODO: implement logic to show QRCode
        });

        joinWaitingListButton = view.findViewById(R.id.event_details_join_waitlist_button);
        joinWaitingListButton.setOnClickListener(v -> {
            // Add user to waitlist logic
            // First check waiting list capacity
            waitingList.refresh(new StatusList.ListUpdateListener() {
                @Override
                public void onUpdate() {
                    int waitingListCapacity = waitingList.getCapacity();
                    int waitingListSize = waitingList.getSize();
                    if ((waitingListCapacity != -1) && (waitingListSize>= waitingListCapacity)) {
                        return; // TODO: implement unable to join msg
                    }
                    waitingList.addUser(user, new StatusList.ListUpdateListener() {
                        @Override
                        public void onUpdate() {
                            showWaiting();
                            updateView();
                        }

                        @Override
                        public void onError(Exception e) {
                            Log.e("waitingList DB Call", "Unable to add user to DB", e);
                        }
                    });
                }
                @Override
                public void onError(Exception e) {
                    Log.e("waitingList DB Call", "Unable to refresh users", e);
                }
            });
        });

        leaveWaitingListButton = view.findViewById(R.id.event_details_leave_waitlist_button);
        leaveWaitingListButton.setOnClickListener(v -> {
            // Remove user from waitlist logic
            waitingList.refresh(new StatusList.ListUpdateListener() {
                @Override
                public void onUpdate() {
                    waitingList.removeUserFromDB(user, new StatusList.ListUpdateListener() {
                        @Override
                        public void onUpdate() {
                            waitingList.popUser(user);
                            showJoin();
                            updateView();
                        }
                        @Override
                        public void onError(Exception e) {
                            Log.e("waitingList DB Call", "Unable to delete user from DB", e);
                        }
                    });
                }
                @Override
                public void onError(Exception e) {
                    Log.e("waitingList DB Call", "Unable to refresh users", e);
                }
            });
        });

        acceptInviteButton = view.findViewById(R.id.event_details_accept_invite_button);
        acceptInviteButton.setOnClickListener(v -> {
            // Add user to accepted list
            acceptedList.refresh(new StatusList.ListUpdateListener() {
                @Override
                public void onUpdate() {
                    // First check waiting list capacity
                    int acceptedListCapacity = acceptedList.getCapacity();
                    int acceptedListSize = waitingList.getSize();
                    if (acceptedListSize >= acceptedListCapacity) {
                        return; // TODO: implement unable to join msg
                    }
                    acceptedList.addUser(user, new StatusList.ListUpdateListener() {
                        @Override
                        public void onUpdate() {
                            // remove from waiting list (in memory)
                            waitingList.popUser(user);
                            showAccepted();
                            updateView();
                        }
                        @Override
                        public void onError(Exception e) {
                            Log.e("acceptedList DB Call", "Unable to add user to DB", e);
                        }
                    });
                }
                @Override
                public void onError(Exception e) {
                    Log.e("acceptedList DB Call", "Unable to refresh users", e);
                }
            });
        });

        declineInviteButton = view.findViewById(R.id.event_details_decline_invite_button);
        declineInviteButton.setOnClickListener(v -> {
            // Add user to declined list
            declinedList.refresh(new StatusList.ListUpdateListener() {
                @Override
                public void onUpdate() {
                    declinedList.addUser(user, new StatusList.ListUpdateListener() {
                        @Override
                        public void onUpdate() {
                            // remove from waiting list (in memory)
                            waitingList.popUser(user);
                            showDeclined();
                            updateView();
                        }
                        @Override
                        public void onError(Exception e) {
                            Log.e("pendingList DB Call", "Unable to add user to DB", e);
                        }
                    });
                }

                @Override
                public void onError(Exception e) {
                    Log.e("pendingList DB Call", "Unable to refresh users", e);
                }
            });

        });

        // Update event details
        updateView();

        // Display options based on user's current status
        if (waitingList.userIsInList(user)) {
            showWaiting();
        } else if (pendingList.userIsInList(user)) {
            showPending();
        } else if (acceptedList.userIsInList(user)) {
            showAccepted();
        } else if (declinedList.userIsInList(user)) {
            showDeclined();
        } else {
            showJoin();
        }
    }

    /**
     * Takes eventShown and updates all views to reflect event information
     */
    private void updateView() {
        eventTitle.setText(eventShown.getName());
        // eventPoster.setImageDrawable();  // TODO: need Poster and PosterDB
        eventPoster.setImageResource(R.drawable.ic_image_placeholder);
        eventCapacity.setText(fmt("Capacity:", String.valueOf(eventShown.getEventCapacity())));
        eventWaitingListCount.setText(fmt("Currently in Waiting List:", String.valueOf(waitingList.getSize())));
        eventStartDate.setText(fmt("Starts:", formatTimestamp(eventShown.getRegistrationStartDate())));
        eventEndDate.setText(fmt("Ends:", formatTimestamp(eventShown.getRegistrationEndDate())));
        eventDescription.setText(eventShown.getDescription());
    }

    /**
     * Helper method to create a String
     * @param header The first line string.
     * @param value The second line string.
     * @return A combination string of header and value with a newline in between.
     */
    private String fmt(String header, String value) {
        return header + "\n" + value;
    }

    /**
     * Formats a Firestore timestamp to show on the user view of event page.
     *
     * @param timestamp Firestore timestamp object to format into date.
     * @return Date string
     */
    private String formatTimestamp(com.google.firebase.Timestamp timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy", Locale.US);
        return sdf.format(new Date(timestamp.toDate().getTime()));
    }

    /**
     * This method shows the UI elements for a user not in the event yet.
     */
    private void showJoin() {
        // Hide
        leaveWaitingListButton.setVisibility(View.GONE);
        eventInviteText.setVisibility(View.GONE);
        acceptInviteButton.setVisibility(View.GONE);
        declineInviteButton.setVisibility(View.GONE);
        eventInviteAcceptedText.setVisibility(View.GONE);
        eventInviteDeclinedText.setVisibility(View.GONE);

        // Show
        joinWaitingListButton.setVisibility(View.VISIBLE);
    }

    /**
     * This method shows UI elements for a user currently in the waitinglist.
     */
    private void showWaiting() {
        // Hide
        joinWaitingListButton.setVisibility(View.GONE);
        eventInviteText.setVisibility(View.GONE);
        acceptInviteButton.setVisibility(View.GONE);
        declineInviteButton.setVisibility(View.GONE);
        eventInviteAcceptedText.setVisibility(View.GONE);
        eventInviteDeclinedText.setVisibility(View.GONE);

        // Show
        leaveWaitingListButton.setVisibility(View.VISIBLE);
    }

    /**
     * This method shows the UI elements for a user currently pending on invite.
     */
    private void showPending() {
        // HIDE
        joinWaitingListButton.setVisibility(View.GONE);
        leaveWaitingListButton.setVisibility(View.GONE);
        eventInviteAcceptedText.setVisibility(View.GONE);
        eventInviteDeclinedText.setVisibility(View.GONE);

        // Show
        eventInviteText.setVisibility(View.VISIBLE);
        acceptInviteButton.setVisibility(View.VISIBLE);
        declineInviteButton.setVisibility(View.VISIBLE);
    }

    /**
     * This method shows the UI elements for a user who accepted the invite.
     */
    private void showAccepted() {
        // Hide
        joinWaitingListButton.setVisibility(View.GONE);
        leaveWaitingListButton.setVisibility(View.GONE);
        eventInviteText.setVisibility(View.GONE);
        acceptInviteButton.setVisibility(View.GONE);
        declineInviteButton.setVisibility(View.GONE);
        eventInviteDeclinedText.setVisibility(View.GONE);

        // Show
        eventInviteAcceptedText.setVisibility(View.VISIBLE);

    }

    /**
     * This method shows the UI elements for a user who declined the invite.
     */
    private void showDeclined() {
        // Hide
        joinWaitingListButton.setVisibility(View.GONE);
        leaveWaitingListButton.setVisibility(View.GONE);
        eventInviteText.setVisibility(View.GONE);
        acceptInviteButton.setVisibility(View.GONE);
        declineInviteButton.setVisibility(View.GONE);
        eventInviteAcceptedText.setVisibility(View.GONE);

        // Show
        eventInviteDeclinedText.setVisibility(View.VISIBLE);
    }
}
