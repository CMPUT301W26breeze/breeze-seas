package com.example.breeze_seas;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentContainerView;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textview.MaterialTextView;

public class EventDetailsFragment extends Fragment {

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

    public EventDetailsFragment () {
        super(R.layout.fragment_event_details);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Setup viewModel
        viewModel = new ViewModelProvider(requireActivity()).get(SessionViewModel.class);

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
        viewQRCodeButton = view.findViewById(R.id.event_details_view_QRCode_button);
        viewQRCodeButton.setOnClickListener(v -> {
            // TODO: implement logic to show QRCode
        });
        joinWaitingListButton = view.findViewById(R.id.event_details_join_waitlist_button);
        joinWaitingListButton.setOnClickListener(v -> {
            // TODO: implement join waiting list logic here
            //showPending();
        });
        leaveWaitingListButton = view.findViewById(R.id.event_details_leave_waitlist_button);
        leaveWaitingListButton.setOnClickListener(v -> {
            // TODO: implement leave waiting list logic here
            //showJoin();
        });
        acceptInviteButton = view.findViewById(R.id.event_details_accept_invite_button);
        acceptInviteButton.setOnClickListener(v -> {
            // TODO: implement logic for accepting invite here
            //showAccepted();
        });
        declineInviteButton = view.findViewById(R.id.event_details_decline_invite_button);
        declineInviteButton.setOnClickListener(v -> {
            // TODO: implement logic for declining invite here
            //showDeclined();
        });

        // Grab event form SessionViewModel
        eventShown = viewModel.getEventShown().getValue();

        // Feed info into updateView()
        updateView(eventShown);

        /*
        TODO: Need logic to present user options
         1. If user not in event, bring up join waiting list button
         2. If user already in event, bring up leave waiting list button
         3. If user got invited, bring up accept/decline buttons
         4. If user in final list, bring up message "Hooray!"
         5. If user in cancelled list, bring up message "Maybe next time."
         */

        // Assuming user is not in event
        // Show join waiting list button for now
        showJoin();

    }

    /**
     * Takes the event object and updates all views to reflect event information
     * @param event Event object to grab details from
     */
    private void updateView(Event event) {
        eventTitle.setText(event.getName());
        // eventPoster.setImageDrawable();  // TODO: need Poster and PosterDB
        eventPoster.setImageResource(R.drawable.ic_image_placeholder);

        // TODO: event class needs capacity attribute
        //eventCapacity.setText();
        eventCapacity.setText(formatter("Capacity:", "N/A"));

        // TODO: event class needs method to return count of people in waiting list
        //eventWaitingListCount.setText();
        eventWaitingListCount.setText(formatter("Currently in Waiting List:", "N/A"));

        // TODO: event class needs startDate
        //eventStartDate.setText();
        eventStartDate.setText(formatter("Starts:", "N/A"));

        // TODO: event class needs endDate
        //eventEndDate.setText();
        eventEndDate.setText(formatter("Ends:", "N/A"));

        eventDescription.setText(event.getDetails());
    }

    /**
     * Helper method to create a String
     * @param header The first line string.
     * @param value The second line string.
     * @return A combination string of header and value with a newline in between.
     */
    private String formatter(String header, String value) {
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
}
