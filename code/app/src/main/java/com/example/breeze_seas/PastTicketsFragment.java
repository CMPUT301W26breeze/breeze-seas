package com.example.breeze_seas;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import java.util.Arrays;
import java.util.List;
/*** PastTicketsFragment shows completed or inactive ticket history.
 ** <p>Intended Content:*- DECLINED, LOST, or (optional) CANCELLED.
 *
 * <p>Current state:* - Renders temporary repository data while backend work is still in progress.
 ** <p>Outstanding/Future Work:* - Replace repository seed data with Firestore-backed loading once
 * the event/ticket schema exists.
 */
public class PastTicketsFragment extends Fragment {

    private final TicketsRepository repository = TicketsRepository.getInstance();
    private final TicketsRepository.Listener ticketsListener = this::renderTickets;

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
        repository.addListener(ticketsListener);
        renderTickets();
    }

    @Override
    public void onDestroyView() {
        repository.removeListener(ticketsListener);
        adapter = null;
        super.onDestroyView();
    }

    private void renderTickets() {
        if (adapter == null) {
            return;
        }

        adapter.submitList(repository.getPastTickets());
    }
}
        PastTicketsAdapter adapter = new PastTicketsAdapter(event ->
                Snackbar.make(view, "Past event history is demo UI for now.", Snackbar.LENGTH_SHORT).show()
        );
        recyclerView.setAdapter(adapter);
        adapter.submitList(buildDemoPastEvents());
    }

    private List<PastEventUIModel> buildDemoPastEvents() {
        return Arrays.asList(
                new PastEventUIModel(
                        "Beginner Swimming Lessons",
                        "Wed, Jan 15 • 5:30 PM",
                        "Kinsmen Sports Centre",
                        "Attended",
                        "Completed successfully",
                        R.drawable.ic_ticket
                ),
                new PastEventUIModel(
                        "Piano Lessons for Beginners",
                        "Mon, Feb 10 • 4:00 PM",
                        "West End Music Studio",
                        "Not selected",
                        "Lottery closed without selection",
                        R.drawable.ic_info
                ),
                new PastEventUIModel(
                        "Community Dance Night",
                        "Sat, Feb 22 • 8:00 PM",
                        "Old Strathcona Hall",
                        "Declined",
                        "Invitation released back to the pool",
                        R.drawable.ic_clock
                )
        );
    }
}
