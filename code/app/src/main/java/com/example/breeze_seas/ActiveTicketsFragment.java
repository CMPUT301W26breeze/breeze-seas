package com.example.breeze_seas;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

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
            // Temporary click feedback; we implement real dialogs next.
            Snackbar.make(view, "Clicked: " + ticket.getTitle(), Snackbar.LENGTH_SHORT).show();
        });

        recyclerView.setAdapter(adapter);

        // Demo data: Pending + Backup + Action Required
        List<TicketUIModel> demo = Arrays.asList(
                new TicketUIModel("e1", "Beginner Swimming Lessons", "Closes in 2 days", TicketUIModel.Status.PENDING),
                new TicketUIModel("e2", "Piano Lessons", "Lottery drawn • Backup pool", TicketUIModel.Status.BACKUP),
                new TicketUIModel("e3", "Interpretive Dance Basics", "Invite expires soon", TicketUIModel.Status.ACTION_REQUIRED)
        );

        adapter.submitList(demo);
    }
}