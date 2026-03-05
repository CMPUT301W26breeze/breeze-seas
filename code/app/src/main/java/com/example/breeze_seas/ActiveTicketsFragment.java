package com.example.breeze_seas;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.util.Arrays;
import java.util.List;

/**
 * ActiveTicketsFragment displays the entrant's active ticket states.
 *
 * <p>Current state:
 * - Renders demo data in a RecyclerView to validate card UI.
 *
 * <p>Outstanding:
 * - Replace demo data with Firestore queries and implement dialog flows.
 */
public class ActiveTicketsFragment extends Fragment {

    public ActiveTicketsFragment() {
        super(R.layout.fragment_active_tickets);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView recyclerView = view.findViewById(R.id.active_tickets_recycler);

        ActiveTicketsAdapter adapter = new ActiveTicketsAdapter(ticket -> {

            if (ticket.getStatus() == TicketUIModel.Status.BACKUP) {
                showBackupPoolDialog();
                return;
            }

            if (ticket.getStatus() == TicketUIModel.Status.ACTION_REQUIRED) {
                showInvitationActionDialog(view, ticket);
                return;
            }

            Snackbar.make(view, "Clicked: " + ticket.getTitle(), Snackbar.LENGTH_SHORT).show();
        });

        recyclerView.setAdapter(adapter);

        List<TicketUIModel> demo = Arrays.asList(
                new TicketUIModel("e1", "Beginner Swimming Lessons", "Closes in 2 days", TicketUIModel.Status.PENDING),
                new TicketUIModel("e2", "Piano Lessons", "Lottery drawn • Backup pool", TicketUIModel.Status.BACKUP),
                new TicketUIModel("e3", "Interpretive Dance Basics", "Invite expires soon", TicketUIModel.Status.ACTION_REQUIRED)
        );

        adapter.submitList(demo);
    }

    /**
     * Shows a centered informational dialog explaining the backup pool state.
     *
     * <p>This replaces the previous bottom sheet approach to avoid excessive empty space.
     *
     * Source:
     * Google Material Design, "Dialogs", accessed 2026-03-04:
     * https://m3.material.io/components/dialogs/overview
     */
    private void showBackupPoolDialog() {
        if (!isAdded()) return;

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("You're on the Waitlist")
                .setMessage("The first draw is complete. You are currently in the backup pool. If any tickets are declined, we will run another draw and notify you.")
                .setPositiveButton("Okay", (dialog, which) -> dialog.dismiss())
                .show();
    }

    /**
     * Shows a centered dialog for the "Action Required" state where the entrant can accept or decline.
     *
     * <p>Current behavior:
     * - Accept: shows confirmation snackbar.
     * - Decline: asks for confirmation, then shows snackbar.
     *
     * <p>Outstanding:
     * - Wire accept/decline to Firestore and move the ticket between tabs using real data lists.
     *
     * Source:
     * Google Material Design, "Dialogs", accessed 2026-03-04:
     * https://m3.material.io/components/dialogs/overview
     */
    private void showInvitationActionDialog(@NonNull View rootView, @NonNull TicketUIModel ticket) {
        if (!isAdded()) return;

        String title = "You won!";
        String message =
                "You've been selected for:\n\n" +
                        ticket.getTitle() + "\n" +
                        ticket.getDateLabel() + "\n\n" +
                        "Accept to confirm your spot, or decline to release it to the backup pool.";

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Accept Invitation", (dialog, which) -> {
                    dialog.dismiss();
                    Snackbar.make(rootView, "You're going!", Snackbar.LENGTH_SHORT).show();
                })
                .setNegativeButton("Decline", (dialog, which) -> {
                    dialog.dismiss();
                    showDeclineConfirmDialog(rootView, ticket);
                })
                .show();
    }

    /**
     * Confirmation dialog to avoid accidental declines.
     */
    private void showDeclineConfirmDialog(@NonNull View rootView, @NonNull TicketUIModel ticket) {
        if (!isAdded()) return;

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Decline invitation?")
                .setMessage("If you decline, your spot will be offered to someone in the backup pool.")
                .setPositiveButton("Yes, decline", (d, w) -> {
                    d.dismiss();
                    Snackbar.make(rootView, "Invitation declined", Snackbar.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", (d, w) -> d.dismiss())
                .show();
    }
}