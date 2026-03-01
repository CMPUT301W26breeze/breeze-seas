package com.example.breeze_seas;

import androidx.fragment.app.Fragment;
/*** The AttendingTicketsFragment displays the events that the entrant has accepted and will attend.
 ** <p>Intended Content:* - Accepted (Attending).
 *
 * <p>Current state:* - A placeholder UI is used to validate the Tickets tab and pager wiring.
 ** <p>Outstanding/Future Work:* - Add the RecyclerView list of attending ticket cards.
 * - Connect to Firestore and run queries for accepted states.
 */
public class AttendingTicketsFragment extends Fragment {

    public AttendingTicketsFragment() {
        super(R.layout.fragment_attending_tickets);
    }

}