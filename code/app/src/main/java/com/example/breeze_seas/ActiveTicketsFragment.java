package com.example.breeze_seas;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

/**
 * ActiveTicketsFragment displays the entrant's active ticket states.
 *
 * <p>Current state:
 * - Loads waiting, backup, and invited tickets through {@link TicketDB}.
 *
 * <p>Outstanding:
 * - Replace temporary success/failure messaging with the final UX once the wider notification flow is done.
 */
public class ActiveTicketsFragment extends Fragment {

    private final TicketDB ticketDb = TicketDB.getInstance();
    private final TicketDB.Listener ticketsListener = new TicketDB.Listener() {
        /**
         * Refreshes the active-ticket list when {@link TicketDB} publishes new data.
         */
        @Override
        public void onTicketsChanged() {
            renderTickets();
        }
    };

    private ActiveTicketsAdapter adapter;

    /**
     * Inflates the Active tab, binds its list, and starts listening for ticket updates.
     *
     * @param inflater Layout inflater used to build the fragment view.
     * @param container Optional parent that will host the inflated hierarchy.
     * @param savedInstanceState Saved state bundle, if the fragment is being recreated.
     * @return The inflated Active Tickets view.
     */
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        View view = inflater.inflate(R.layout.fragment_active_tickets, container, false);

        RecyclerView recyclerView = view.findViewById(R.id.active_tickets_recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new ActiveTicketsAdapter(new ActiveTicketsAdapter.OnTicketClickListener() {
            /**
             * Routes each active ticket click to the correct dialog or status message.
             *
             * @param ticket The active ticket card the entrant tapped.
             */
            @Override
            public void onTicketClick(@NonNull TicketUIModel ticket) {
                handleTicketClick(view, ticket);
            }
        });

        recyclerView.setAdapter(adapter);
        ticketDb.addListener(ticketsListener);
        refreshTickets();
        renderTickets();

        return view;
    }

    /**
     * Removes ticket listeners tied to the destroyed view hierarchy.
     */
    @Override
    public void onDestroyView() {
        ticketDb.removeListener(ticketsListener);
        adapter = null;
        super.onDestroyView();
    }

    /**
     * Rebinds the current active-ticket list into the adapter.
     */
    private void renderTickets() {
        if (adapter == null) {
            return;
        }

        adapter.submitList(ticketDb.getActiveTickets());
    }

    /**
     * Reloads the ticket lists using the current session user's device id when available.
     */
    private void refreshTickets() {
        SessionViewModel viewModel = new ViewModelProvider(requireActivity()).get(SessionViewModel.class);
        User currentUser = viewModel.getUser().getValue();
        String preferredDeviceId = currentUser == null ? null : currentUser.getDeviceId();
        ticketDb.refreshTickets(requireContext(), preferredDeviceId);
    }

    /**
     * Handles taps on active ticket cards based on their current status.
     *
     * @param rootView Root fragment view used for transient snackbar messages.
     * @param ticket The ticket card the entrant tapped.
     */
    private void handleTicketClick(@NonNull View rootView, @NonNull TicketUIModel ticket) {
        if (ticket.getStatus() == TicketUIModel.Status.PENDING) {
            showLeaveWaitlistDialog(rootView, ticket);
            return;
        }

        if (ticket.getStatus() == TicketUIModel.Status.BACKUP) {
            showBackupPoolDialog(rootView, ticket);
            return;
        }

        if (ticket.getStatus() == TicketUIModel.Status.ACTION_REQUIRED) {
            showInvitationActionDialog(rootView, ticket);
            return;
        }

        Snackbar.make(
                rootView,
                "Your entry is still active. We will notify you when the draw status changes.",
                Snackbar.LENGTH_SHORT
        ).show();
    }

    /**
     * Shows a centered informational dialog explaining the backup pool state.
     *
     * <p>This replaces the previous bottom sheet approach to avoid excessive empty space.
     * The dialog also lets the entrant leave the waitlist from the backup pool.
     *
     * Source:
     * Google Material Design, "Dialogs", accessed 2026-03-04:
     * https://m3.material.io/components/dialogs/overview
     *
     * @param rootView Root fragment view used for the follow-up snackbar message.
     * @param ticket The backup-pool ticket the entrant tapped.
     */
    private void showBackupPoolDialog(@NonNull View rootView, @NonNull TicketUIModel ticket) {
        if (!isAdded()) return;
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_ticket_action, null, false);
        bindEventDialogContent(
                dialogView,
                "You're on the Waitlist",
                "The first draw is complete. You are currently in the backup pool. If any spots open up, we will run another draw and notify you.",
                ticket,
                "Okay",
                "Leave Waitlist"
        );

        AlertDialog dialog = createStyledDialog(dialogView);
        Button primaryButton = dialogView.findViewById(R.id.dialog_primary_button);
        Button secondaryButton = dialogView.findViewById(R.id.dialog_secondary_button);

        primaryButton.setOnClickListener(new View.OnClickListener() {
            /**
             * Dismisses the informational dialog once the entrant acknowledges it.
             *
             * @param v The pressed primary action view.
             */
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        secondaryButton.setOnClickListener(new View.OnClickListener() {
            /**
             * Opens a confirmation dialog before removing the entrant from the backup pool.
             *
             * @param v The pressed secondary action view.
             */
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                showLeaveWaitlistConfirmDialog(rootView, ticket);
            }
        });

        dialog.show();
    }

    /**
     * Shows the primary waitlist dialog for entrants who are still waiting on the initial draw.
     *
     * @param rootView Root fragment view used for the follow-up snackbar message.
     * @param ticket The waiting-list ticket the entrant tapped.
     */
    private void showLeaveWaitlistDialog(@NonNull View rootView, @NonNull TicketUIModel ticket) {
        if (!isAdded()) return;
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_ticket_action, null, false);
        bindEventDialogContent(
                dialogView,
                "Leave waitlist?",
                "If you leave now, your entry will be removed from the draw for this event.",
                ticket,
                "Leave Waitlist",
                "Stay"
        );

        AlertDialog dialog = createStyledDialog(dialogView);
        Button primaryButton = dialogView.findViewById(R.id.dialog_primary_button);
        Button secondaryButton = dialogView.findViewById(R.id.dialog_secondary_button);

        primaryButton.setOnClickListener(new View.OnClickListener() {
            /**
             * Opens the final confirmation step before deleting the waitlist entry.
             *
             * @param v The pressed primary action view.
             */
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                showLeaveWaitlistConfirmDialog(rootView, ticket);
            }
        });
        secondaryButton.setOnClickListener(new View.OnClickListener() {
            /**
             * Keeps the entrant on the waitlist and closes the dialog.
             *
             * @param v The pressed secondary action view.
             */
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    /**
     * Shows a confirmation dialog before removing the entrant from the waitlist.
     *
     * @param rootView Root fragment view used for the follow-up snackbar message.
     * @param ticket The active ticket whose waitlist entry may be removed.
     */
    private void showLeaveWaitlistConfirmDialog(@NonNull View rootView, @NonNull TicketUIModel ticket) {
        if (!isAdded()) return;
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_ticket_confirmation, null, false);
        bindConfirmationDialogContent(
                dialogView,
                "Confirm leave?",
                "This will remove your entry from the waitlist for this event.",
                "Yes, leave",
                "Cancel"
        );

        AlertDialog dialog = createStyledDialog(dialogView);
        Button primaryButton = dialogView.findViewById(R.id.dialog_primary_button);
        Button secondaryButton = dialogView.findViewById(R.id.dialog_secondary_button);

        primaryButton.setOnClickListener(new View.OnClickListener() {
            /**
             * Removes the entrant from the waitlist and refreshes the Active tab.
             *
             * @param v The pressed primary action view.
             */
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                ticketDb.leaveWaitlist(ticket);
                Snackbar.make(rootView, "You left the waitlist.", Snackbar.LENGTH_SHORT).show();
            }
        });
        secondaryButton.setOnClickListener(new View.OnClickListener() {
            /**
             * Cancels the leave-waitlist flow and dismisses the dialog.
             *
             * @param v The pressed secondary action view.
             */
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    /**
     * Shows a centered dialog for the "Action Required" state where the entrant can accept or decline.
     *
     * <p>Current behavior:
     * - Accept: updates the participant status to {@code accepted}.
     * - Decline: updates the participant status to {@code declined}.
     *
     * <p>Outstanding:
     * - Surface write failures with the final UX once the wider app error-handling pattern is settled.
     *
     * Source:
     * Google Material Design, "Dialogs", accessed 2026-03-04:
     * https://m3.material.io/components/dialogs/overview
     */
    private void showInvitationActionDialog(@NonNull View rootView, @NonNull TicketUIModel ticket) {
        if (!isAdded()) return;
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_ticket_action, null, false);
        String title = ticket.isPrivateEvent() ? "Private invite" : "Action required";
        String subtitle = ticket.isPrivateEvent()
                ? "You were invited to join the waitlist for this private event. Accept to join the waitlist, or decline to dismiss this invite."
                : "You were selected for this event. Accept to confirm your spot, or decline to release it to the backup pool.";
        String primaryLabel = ticket.isPrivateEvent() ? "Join Waitlist" : "Accept Invitation";
        bindEventDialogContent(
                dialogView,
                title,
                subtitle,
                ticket,
                primaryLabel,
                "Decline"
        );

        AlertDialog dialog = createStyledDialog(dialogView);
        Button primaryButton = dialogView.findViewById(R.id.dialog_primary_button);
        Button secondaryButton = dialogView.findViewById(R.id.dialog_secondary_button);

        primaryButton.setOnClickListener(new View.OnClickListener() {
            /**
             * Accepts the invitation and moves the ticket into the attending state.
             *
             * @param v The pressed primary action view.
             */
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                ticketDb.acceptInvitation(ticket);
                Snackbar.make(
                        rootView,
                        ticket.isPrivateEvent()
                                ? "Invite accepted. You joined the private waitlist."
                                : "Invitation accepted. Ticket moved to Attending.",
                        Snackbar.LENGTH_SHORT
                ).show();
            }
        });
        secondaryButton.setOnClickListener(new View.OnClickListener() {
            /**
             * Opens the confirmation dialog before declining the invitation.
             *
             * @param v The pressed secondary action view.
             */
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                showDeclineConfirmDialog(rootView, ticket);
            }
        });

        dialog.show();
    }

    /**
     * Shows a confirmation dialog to avoid accidental invitation declines.
     *
     * @param rootView Root fragment view used for the follow-up snackbar message.
     * @param ticket The invited ticket that may be declined.
     */
    private void showDeclineConfirmDialog(@NonNull View rootView, @NonNull TicketUIModel ticket) {
        if (!isAdded()) return;
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_ticket_confirmation, null, false);
        String title = ticket.isPrivateEvent() ? "Decline private invite?" : "Decline invitation?";
        String message = ticket.isPrivateEvent()
                ? "If you decline, this private-event waitlist invite will be removed."
                : "If you decline, your spot will be offered to someone in the backup pool.";
        bindConfirmationDialogContent(
                dialogView,
                title,
                message,
                "Yes, decline",
                "Cancel"
        );

        AlertDialog dialog = createStyledDialog(dialogView);
        Button primaryButton = dialogView.findViewById(R.id.dialog_primary_button);
        Button secondaryButton = dialogView.findViewById(R.id.dialog_secondary_button);

        primaryButton.setOnClickListener(new View.OnClickListener() {
            /**
             * Confirms the decline and updates the participant status.
             *
             * @param v The pressed primary action view.
             */
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                ticketDb.declineInvitation(ticket);
                Snackbar.make(
                        rootView,
                        ticket.isPrivateEvent() ? "Private invite declined" : "Invitation declined",
                        Snackbar.LENGTH_SHORT
                ).show();
            }
        });
        secondaryButton.setOnClickListener(new View.OnClickListener() {
            /**
             * Cancels the decline flow and dismisses the confirmation dialog.
             *
             * @param v The pressed secondary action view.
             */
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    /**
     * Creates a dialog window with the app's custom white shell and a transparent system frame.
     *
     * @param dialogView Fully configured custom dialog view.
     * @return A styled dialog ready to be shown.
     */
    @NonNull
    private AlertDialog createStyledDialog(@NonNull View dialogView) {
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        return dialog;
    }

    /**
     * Populates the shared event-action dialog layout for waiting, backup, and invitation states.
     *
     * @param dialogView Inflated dialog layout that contains the event shell and action buttons.
     * @param title Dialog headline text.
     * @param subtitle Supporting copy shown below the headline.
     * @param ticket Ticket whose event information should be shown in the dialog.
     * @param primaryLabel Label for the primary action button.
     * @param secondaryLabel Label for the secondary action button.
     */
    private void bindEventDialogContent(
            @NonNull View dialogView,
            @NonNull String title,
            @NonNull String subtitle,
            @NonNull TicketUIModel ticket,
            @NonNull String primaryLabel,
            @NonNull String secondaryLabel
    ) {
        TextView titleView = dialogView.findViewById(R.id.dialog_title);
        TextView subtitleView = dialogView.findViewById(R.id.dialog_subtitle);
        TextView eventTitleView = dialogView.findViewById(R.id.dialog_event_title);
        TextView eventDateView = dialogView.findViewById(R.id.dialog_event_date);
        Button primaryButton = dialogView.findViewById(R.id.dialog_primary_button);
        Button secondaryButton = dialogView.findViewById(R.id.dialog_secondary_button);

        titleView.setText(title);
        subtitleView.setText(subtitle);
        eventTitleView.setText(ticket.getTitle());
        eventDateView.setText(ticket.getDateLabel());
        primaryButton.setText(primaryLabel);
        secondaryButton.setText(secondaryLabel);
    }

    /**
     * Populates the compact confirmation dialog layout used before destructive ticket actions.
     *
     * @param dialogView Inflated confirmation dialog layout.
     * @param title Dialog headline text.
     * @param message Supporting confirmation message.
     * @param primaryLabel Label for the primary action button.
     * @param secondaryLabel Label for the secondary action button.
     */
    private void bindConfirmationDialogContent(
            @NonNull View dialogView,
            @NonNull String title,
            @NonNull String message,
            @NonNull String primaryLabel,
            @NonNull String secondaryLabel
    ) {
        TextView titleView = dialogView.findViewById(R.id.dialog_title);
        TextView messageView = dialogView.findViewById(R.id.dialog_message);
        Button primaryButton = dialogView.findViewById(R.id.dialog_primary_button);
        Button secondaryButton = dialogView.findViewById(R.id.dialog_secondary_button);

        titleView.setText(title);
        messageView.setText(message);
        primaryButton.setText(primaryLabel);
        secondaryButton.setText(secondaryLabel);
    }
}
