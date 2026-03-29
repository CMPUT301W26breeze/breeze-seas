package com.example.breeze_seas;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class EventDetailsFragment extends Fragment {
    private ImageView returnButton;
    private TextView eventTitle;
    private FrameLayout eventPosterFrame;
    private ImageView eventPoster;
    private Button viewQRCodeButton;
    private TextView eventCapacity;
    private TextView eventWaitingListCount;
    private TextView eventStartDate;
    private TextView eventEndDate;
    private TextView eventDescription;
    private Button joinWaitingListButton;
    private Button leaveWaitingListButton;
    private TextView eventInviteText;
    private Button acceptInviteButton;
    private Button declineInviteButton;
    private TextView eventInviteAcceptedText;
    private TextView eventInviteDeclinedText;

    private SessionViewModel viewModel;
    private Event eventShown;
    private WaitingList waitingList;
    private PendingList pendingList;
    private AcceptedList acceptedList;
    private DeclinedList declinedList;
    private User user;
    private EventCommentsSectionController commentsSectionController;
    private final androidx.activity.result.ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.RequestPermission(), isGranted -> {
                joinWaitingListButton.setEnabled(true);
                if (isGranted) {

                    joinWaitingListButton.performClick();
                } else {
                    Toast.makeText(requireContext(), "Location permission is required for this event.", Toast.LENGTH_LONG).show();
                }
            });

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
        eventShown = viewModel.getExploreFragmentEventHandler().getEventShown().getValue();
        assert eventShown != null;

        // Grab references to list classes
        waitingList = eventShown.getWaitingList();
        pendingList = eventShown.getPendingList();
        acceptedList = eventShown.getAcceptedList();
        declinedList = eventShown.getDeclinedList();

        // Start listener on list classes
        eventShown.startParticipantsListen(new Event.ParticipantsUpdatedCallback() {
            @Override
            public void onUpdated() {
                // Update event details
                updateView();
                // Display options based on user's current status
                showOption(user);
            }

            @Override
            public void onFailure(Exception e) {
                // Unassign eventShown
                viewModel.getExploreFragmentEventHandler().setEventShown(null);

                // Return to explore fragment
                getParentFragmentManager()
                        .popBackStack();
            }
        });
    }

    @Override
    public void onStop() {
        super.onStop();
        // Close listener
        eventShown.stopParticipantsListen();
        Log.d("EventDetailsFragment", "Successfully closed event listener");
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Setup observer on eventShown
        viewModel.getExploreFragmentEventHandler()
                .getEventShown().observe(getViewLifecycleOwner(), e -> {
            // Check if event still exists
            if (e == null) {
                // Return to explore fragment
                getParentFragmentManager()
                        .popBackStack();
            }

            // Since reference to the object is the same, the details should be updated automatically.
            updateView();
            showOption(user);
        });

        // Setup observer for image
        eventShown.getImageData().observe(getViewLifecycleOwner(), image -> {
                    // Just need to update image
                    // Since the triggers for this observer will update the image object within event beforehand
                    // We should be able to just refresh handlePoster using the same object reference.
                    handlePoster(eventShown.getImage());
        });

        // Bind all views
        eventTitle = view.findViewById(R.id.event_details_event_title);
        eventPosterFrame = view.findViewById(R.id.event_details_event_photo_frame);
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

        // TODO: Fix logic as list classes are now real time, consistent with the database.

        ProgressBar progressBar = view.findViewById(R.id.event_details_loading_progress_bar);
        joinWaitingListButton = view.findViewById(R.id.event_details_join_waitlist_button);
        joinWaitingListButton.setOnClickListener(v -> {
            progressBar.setVisibility(View.VISIBLE);
            joinWaitingListButton.setEnabled(false);
            // Add user to waitlist logic
            // First check waiting list capacity
            waitingList.refresh(new StatusList.ListUpdateListener() {
                @Override
                public void onUpdate() {
                    int waitingListCapacity = waitingList.getCapacity();
                    int waitingListSize = waitingList.getSize();
                    if ((waitingListCapacity != -1) && (waitingListSize>= waitingListCapacity)) {
                        progressBar.setVisibility(View.GONE);
                        showJoin();
                        joinWaitingListButton.setEnabled(true);
                        Log.w("waitingList DB Call", "Waiting list capacity reached for event " + eventShown.getEventId());
                        Toast.makeText(requireContext(), "The waiting list is full for this event.", Toast.LENGTH_SHORT).show();
                        // TODO: implement unable to join msg
                        return;
                    }

                    // check for location permission if geolocation is enforced
                    if (!eventShown.isGeolocationEnforced()) {
                        waitingList.determineLocation(requireContext(), user, new StatusList.ListUpdateListener() {
                            @Override
                            public void onUpdate() {
                                progressBar.setVisibility(View.GONE);
                                showWaiting();
                                updateView();
                                refreshTickets();
                            }
                            @Override
                            public void onError(Exception e) {
                                progressBar.setVisibility(View.GONE);
                                showJoin();
                                joinWaitingListButton.setEnabled(true);
                                Log.e("waitingList DB Call", "Unable to add user", e);
                            }
                        });
                    } else if (androidx.core.app.ActivityCompat.checkSelfPermission(requireContext(),
                            android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED) {


                        waitingList.determineLocation(requireContext(), user, new StatusList.ListUpdateListener() {
                            @Override
                            public void onUpdate() {
                                progressBar.setVisibility(View.GONE);
                                showWaiting();
                                updateView();
                                refreshTickets();
                            }
                            @Override
                            public void onError(Exception e) {
                                progressBar.setVisibility(View.GONE);
                                showJoin();
                                joinWaitingListButton.setEnabled(true);
                                Log.e("waitingList DB Call", "Unable to add user", e);
                            }
                        });

                    } else {
                        progressBar.setVisibility(View.GONE);
                        showJoin();
                        joinWaitingListButton.setEnabled(true);
                        requestPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION);
                    }
                }
                @Override
                public void onError(Exception e) {
                    progressBar.setVisibility(View.GONE);
                    showJoin();
                    joinWaitingListButton.setEnabled(true);
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
                            refreshTickets();
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
                        Toast.makeText(requireContext(), "The event is already full.", Toast.LENGTH_SHORT).show();
                        return; // TODO: implement unable to join msg
                    }
                    acceptedList.addUser(user, new StatusList.ListUpdateListener() {
                        @Override
                        public void onUpdate() {
                            // remove from waiting list (in memory)
                            waitingList.popUser(user);
                            showAccepted();
                            updateView();
                            refreshTickets();
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
                            refreshTickets();
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
    }

    /**
     * Takes eventShown and updates all views to reflect event information
     */
    private void updateView() {
        eventTitle.setText(eventShown.getName());
        handlePoster(eventShown.getImage());
        eventCapacity.setText(fmt("Capacity:", String.valueOf(eventShown.getEventCapacity())));
        eventWaitingListCount.setText(fmt("Currently in Waiting List:", String.valueOf(waitingList.getSize())));
        eventStartDate.setText(fmt("Starts:", formatTimestamp(eventShown.getRegistrationStartTimestamp())));
        eventEndDate.setText(fmt("Ends:", formatTimestamp(eventShown.getRegistrationEndTimestamp())));
        eventDescription.setText(eventShown.getDescription());
    }

    /**
     * Helper method to display poster.
     * @param image Image object.
     */
    private void handlePoster(Image image) {
        if (image == null) {
            eventPoster.setImageResource(R.drawable.ic_image_placeholder);
            eventPoster.setVisibility(View.GONE);
            eventPosterFrame.setVisibility(View.GONE);
        } else {
            eventPoster.setImageBitmap(image.display());
            eventPoster.setVisibility(View.VISIBLE);
            eventPosterFrame.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Present options based on the status of the current user in the event.
     * @param user User to show options for.
     */
    private void showOption(User user) {
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

    /**
     * Reloads ticket data using the current session user's device id when available.
     */
    private void refreshTickets() {
        String preferredDeviceId = user == null ? null : user.getDeviceId();
        TicketDB.getInstance().refreshTickets(requireContext(), preferredDeviceId);
    }
}
