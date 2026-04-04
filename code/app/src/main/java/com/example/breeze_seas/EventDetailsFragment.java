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
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;

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
    private ExploreViewModel exploreViewModel;
    private Event eventShown;
    private WaitingList waitingList;
    private PendingList pendingList;
    private AcceptedList acceptedList;
    private DeclinedList declinedList;
    private User user;
    ProgressBar progressBar;
    private EventCommentsSectionController commentsSectionController;


    private final androidx.activity.result.ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.RequestPermission(), isGranted -> {
                joinWaitingListButton.setEnabled(true);
                if (isGranted) {
                    progressBar.setVisibility(View.VISIBLE);
                    joinWaitingListButton.setEnabled(false);

                    waitingList.determineLocation(requireContext(), user, new StatusList.ListUpdateListener() {
                        @Override
                        public void onUpdate() {
                            progressBar.setVisibility(View.GONE);
                            joinWaitingListButton.setEnabled(true);
                            refreshTickets();
                        }
                        @Override
                        public void onError(Exception e) {
                            progressBar.setVisibility(View.GONE);
                            joinWaitingListButton.setEnabled(true);
                            Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    Toast.makeText(requireContext(), "Location permission is required for this event.", Toast.LENGTH_LONG).show();
                }
            });

    public EventDetailsFragment () {
        super(R.layout.fragment_event_details);
    }

    StatusList.ListUpdateListener liveListener = new StatusList.ListUpdateListener() {
        @Override
        public void onUpdate() {
            updateView();
            showOption(user);
        }
        @Override
        public void onError(Exception e) {
            Log.e("Realtime DB", "Error in listener", e);

            // If list classes listener breaks, the logic for joining an event
            // would no longer be consistent with the database.
            // Thus, the best course of action is to leave the page.
            // Unassign eventShown
            exploreViewModel.getEventHandler().setEventShown(null);

            // Return to explore fragment
            getParentFragmentManager()
                    .popBackStack();
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Setup viewModel
        viewModel = new ViewModelProvider(requireActivity()).get(SessionViewModel.class);

        // Grab event and user from ViewModels
        user = viewModel.getUser().getValue();
      
        exploreViewModel = new ViewModelProvider(requireActivity()).get(ExploreViewModel.class);
        eventShown = exploreViewModel.getEventHandler().getEventShown().getValue();

        // Get the transaction sitting directly behind the current fragment
        FragmentManager fm = getParentFragmentManager();
        int backStackCount = fm.getBackStackEntryCount();
        FragmentManager.BackStackEntry previousEntry = fm.getBackStackEntryAt(backStackCount - 1);
        String previousFragmentTag = previousEntry.getName();

        // If the fragment is created by ScanFragment
        if ("Scan QR Code".equals(previousFragmentTag)) {
            eventShown = viewModel.getEventShown().getValue();
        }

        // Make sure event is valid.
        assert eventShown != null;

        // Grab references to list classes
        waitingList = eventShown.getWaitingList();
        pendingList = eventShown.getPendingList();
        acceptedList = eventShown.getAcceptedList();
        declinedList = eventShown.getDeclinedList();
    }

    @Override
    public void onStart() {
        super.onStart();
        // Start participants listeners
        eventShown.startListenAllLists(liveListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        eventShown.stopListenAllLists();
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Setup observer on eventShown
        exploreViewModel.getEventHandler()
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
            Bundle args = new Bundle();
            args.putString("eventId", eventShown.getEventId());
            args.putBoolean("isCreated", false);
            ViewQrCodeFragment fragment = new ViewQrCodeFragment();
            fragment.setArguments(args);

            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commit();
        });

        // TODO: Fix logic as list classes are now real time, consistent with the database.

        progressBar = view.findViewById(R.id.event_details_loading_progress_bar);
        joinWaitingListButton = view.findViewById(R.id.event_details_join_waitlist_button);
        joinWaitingListButton.setOnClickListener(v -> {
            int waitingListCapacity = waitingList.getCapacity();
            if (waitingListCapacity != -1 && waitingList.getSize() >= waitingListCapacity) {
                Toast.makeText(requireContext(), "The waiting list is full.", Toast.LENGTH_SHORT).show();
                return;
            }

            TermsAndCondition termsDialog = new TermsAndCondition(() -> {
                boolean locationEnforced = eventShown.isGeolocationEnforced();
                boolean hasPermission = androidx.core.app.ActivityCompat.checkSelfPermission(requireContext(),
                        android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED;

                if (locationEnforced && !hasPermission) {
                    requestPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION);
                } else {
                    progressBar.setVisibility(View.VISIBLE);
                    joinWaitingListButton.setEnabled(false);

                    waitingList.determineLocation(requireContext(), user, new StatusList.ListUpdateListener() {
                        @Override
                        public void onUpdate() {
                            if (isAdded()) {
                                progressBar.setVisibility(View.GONE);
                                joinWaitingListButton.setEnabled(true);
                                refreshTickets();
                            }
                        }
                        @Override
                        public void onError(Exception e) {
                            if (isAdded()) {
                                progressBar.setVisibility(View.GONE);
                                joinWaitingListButton.setEnabled(true);
                                Toast.makeText(getContext(), "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            }, eventShown);

            termsDialog.show(getParentFragmentManager(), "TermsDialog");
        });

        leaveWaitingListButton = view.findViewById(R.id.event_details_leave_waitlist_button);
        leaveWaitingListButton.setOnClickListener(v -> {
            progressBar.setVisibility(View.VISIBLE);
            // Remove user from waitlist logic
            waitingList.removeUserFromDB(user.getDeviceId(), new StatusList.ListUpdateListener() {
                @Override
                public void onUpdate() {
                    progressBar.setVisibility(View.GONE);
                    refreshTickets();
                }
                @Override public void onError(Exception e) { progressBar.setVisibility(View.GONE); }
            });
        });


        acceptInviteButton = view.findViewById(R.id.event_details_accept_invite_button);
        acceptInviteButton.setOnClickListener(v -> {
            // Add user to accepted list
            if (acceptedList.getSize() >= acceptedList.getCapacity()) {
                Toast.makeText(requireContext(), "The event is already full.", Toast.LENGTH_SHORT).show();
                return;
            }


            progressBar.setVisibility(View.VISIBLE);
            acceptedList.addUser(user, new StatusList.ListUpdateListener() {
                @Override
                public void onUpdate() {
                    progressBar.setVisibility(View.GONE);
                    refreshTickets();
                }
                @Override public void onError(Exception e) { progressBar.setVisibility(View.GONE); }
            });
        });


        declineInviteButton = view.findViewById(R.id.event_details_decline_invite_button);
        declineInviteButton.setOnClickListener(v -> {
            // Add user to declined list
            progressBar.setVisibility(View.VISIBLE);
            declinedList.addUser(user, new StatusList.ListUpdateListener() {
                @Override
                public void onUpdate() {
                    progressBar.setVisibility(View.GONE);
                    refreshTickets();
                }
                @Override public void onError(Exception e) { progressBar.setVisibility(View.GONE); }
            });


        });

        commentsSectionController = new EventCommentsSectionController(this, view);
        commentsSectionController.bind(eventShown, user, false, user != null && user.isAdmin());
        updateView();
        showOption(user);
    }

    /**
     * Takes eventShown and updates all views to reflect event information
     */
    private void updateView() {
        eventTitle.setText(eventShown.getName());
        handlePoster(eventShown.getImage());
        eventCapacity.setText(fmt("Capacity:", String.valueOf(eventShown.getEventCapacity())));
        eventWaitingListCount.setText(fmt("Currently in Waiting List:", String.valueOf(waitingList.getSize())));
        eventStartDate.setText(fmt("Starts:", EventMetadataUtils.formatDateTime(eventShown.getEventStartTimestamp())));
        eventEndDate.setText(fmt("Ends:", EventMetadataUtils.formatDateTime(eventShown.getEventEndTimestamp())));
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
     * Releases view-scoped resources when the event-details view is destroyed.
     */
    @Override
    public void onDestroyView() {
        if (commentsSectionController != null) {
            commentsSectionController.release();
            commentsSectionController = null;
        }
        super.onDestroyView();
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
