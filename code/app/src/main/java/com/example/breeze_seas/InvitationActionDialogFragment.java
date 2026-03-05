package com.example.breeze_seas;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/**
 * InvitationActionDialogFragment shows the "You won!" flow when an entrant is selected.
 *
 * <p>Role:
 * - Presents a centered dialog with event details and Accept/Decline actions.
 *
 * <p>Outstanding:
 * - Wire Accept/Decline to Firestore updates and move tickets between tabs using real data.
 *
 * Source:
 * Google Material Design, "Dialogs", accessed 2026-03-04:
 * https://m3.material.io/components/dialogs/overview
 */
public class InvitationActionDialogFragment extends DialogFragment {

    public interface Listener {
        void onAccept(@NonNull TicketUIModel ticket);
        void onDecline(@NonNull TicketUIModel ticket);
    }

    private static final String ARG_ID = "arg_id";
    private static final String ARG_TITLE = "arg_title";
    private static final String ARG_DATE = "arg_date";

    private Listener listener;

    public static InvitationActionDialogFragment newInstance(@NonNull TicketUIModel ticket) {
        InvitationActionDialogFragment frag = new InvitationActionDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ID, ticket.getEventId());
        args.putString(ARG_TITLE, ticket.getTitle());
        args.putString(ARG_DATE, ticket.getDateLabel());
        frag.setArguments(args);
        return frag;
    }

    public void setListener(@Nullable Listener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View content = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_invitation_action_required, null, false);

        Bundle args = getArguments();
        String id = args != null ? args.getString(ARG_ID, "") : "";
        String title = args != null ? args.getString(ARG_TITLE, "") : "";
        String date = args != null ? args.getString(ARG_DATE, "") : "";

        TicketUIModel ticket = new TicketUIModel(id, title, date, TicketUIModel.Status.ACTION_REQUIRED);

        TextView eventTitle = content.findViewById(R.id.dialog_event_title);
        TextView eventDate = content.findViewById(R.id.dialog_event_date);
        MaterialButton accept = content.findViewById(R.id.btn_accept);
        MaterialButton decline = content.findViewById(R.id.btn_decline);

        eventTitle.setText(title);
        eventDate.setText(date);

        accept.setOnClickListener(v -> {
            dismiss();
            if (listener != null) listener.onAccept(ticket);
        });

        decline.setOnClickListener(v -> {
            dismiss();
            if (listener != null) listener.onDecline(ticket);
        });

        return new MaterialAlertDialogBuilder(requireContext())
                .setView(content)
                .create();
    }
}
