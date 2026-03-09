package com.example.breeze_seas;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/*** The AttendingTicketsFragment displays the events that the entrant has accepted and will attend.
 ** <p>Intended Content:* - Accepted (Attending).
 *
 * <p>Current state:* - Renders temporary repository data while the backend contract is still pending.
 ** <p>Outstanding/Future Work:* - Replace repository seed data with Firestore loading after the
 * event/ticket schema is finalized.
 */
public class AttendingTicketsFragment extends Fragment {

    private final TicketsRepository repository = TicketsRepository.getInstance();
    private final TicketsRepository.Listener ticketsListener = this::renderTickets;

    private AttendingTicketsAdapter adapter;
    private RecyclerView recyclerView;
    private View emptyState;

    public AttendingTicketsFragment() {
        super(R.layout.fragment_attending_tickets);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.attending_tickets_recycler);
        emptyState = view.findViewById(R.id.attending_tickets_empty_state);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new AttendingTicketsAdapter(this::openTicket);
        recyclerView.setAdapter(adapter);
        repository.addListener(ticketsListener);
        renderTickets();
    }

    @Override
    public void onDestroyView() {
        repository.removeListener(ticketsListener);
        recyclerView = null;
        emptyState = null;
        adapter = null;
        super.onDestroyView();
    }

    private void renderTickets() {
        if (adapter == null || recyclerView == null || emptyState == null) {
            return;
        }

        java.util.List<AttendingTicketUIModel> tickets = repository.getAttendingTickets();
        boolean hasTickets = !tickets.isEmpty();
        adapter.submitList(tickets);
        recyclerView.setVisibility(hasTickets ? View.VISIBLE : View.GONE);
        emptyState.setVisibility(hasTickets ? View.GONE : View.VISIBLE);
    }

    private void openTicket(@NonNull AttendingTicketUIModel ticket) {
        if (!isAdded()) {
            return;
        }

        Intent intent = new Intent(requireContext(), ViewQrCodeActivity.class);
        intent.putExtra("eventId", ticket.getEventId());
        intent.putExtra("eventName", ticket.getTitle());
        startActivity(intent);
    }
}
