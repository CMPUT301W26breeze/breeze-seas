package com.example.breeze_seas;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

/**
 * PastTicketsFragment shows completed or inactive ticket history.
 *
 * <p>Intended content:
 * - Declined
 * - Lost / not selected
 * - Optional cancelled states
 *
 * <p>Current state:
 * - Loads declined, cancelled, and not-selected entries through {@link TicketDB}.
 *
 * <p>Outstanding:
 * - Expand past-history detail once the team finalizes whether completed accepted events should move here too.
 */
public class PastTicketsFragment extends Fragment {

    private final TicketDB ticketDb = TicketDB.getInstance();
    private final TicketDB.Listener ticketsListener = this::renderTickets;

    private PastTicketsAdapter adapter;

    public PastTicketsFragment() {
        super(R.layout.fragment_past_tickets);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView recyclerView = view.findViewById(R.id.past_tickets_recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new PastTicketsAdapter(event ->
                Snackbar.make(view, "Past event history is demo UI for now.", Snackbar.LENGTH_SHORT).show()
        );

        recyclerView.setAdapter(adapter);

        ticketDb.addListener(ticketsListener);
        ticketDb.refreshTickets(requireContext());
        renderTickets();
    }

    @Override
    public void onDestroyView() {
        ticketDb.removeListener(ticketsListener);
        adapter = null;
        super.onDestroyView();
    }

    private void renderTickets() {
        if (adapter == null) {
            return;
        }

        adapter.submitList(ticketDb.getPastTickets());
    }
}
