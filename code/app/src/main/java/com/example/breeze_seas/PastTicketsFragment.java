package com.example.breeze_seas;

import androidx.fragment.app.Fragment;
/*** PastTicketsFragment shows completed or inactive ticket history.
 ** <p>Intended Content:*- DECLINED, LOST, or (optional) CANCELLED.
 *
 * <p>Current state:* - A placeholder UI is used to validate the Tickets tab and pager wiring.
 ** <p>Outstanding/Future Work:* - Implement the RecyclerView list of previous ticket cards.
 * - Connect to Firestore queries for previous states.
 */
public class PastTicketsFragment extends Fragment {

    public PastTicketsFragment() {
        super(R.layout.fragment_past_tickets);
    }

}