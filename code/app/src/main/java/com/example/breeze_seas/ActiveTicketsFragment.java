package com.example.breeze_seas;
import androidx.fragment.app.Fragment;
/*** ActiveTicketsFragment shows the entrant's active ticket states.
 ** <p>Intended Content:* - Waiting (pending)
 * - BACKUP (not selected/backup pool)
 * - INVITED (Action Required)
 *
 * <p>Current state:* - A placeholder UI is used to validate the Tickets tab and pager wiring.
 ** <p>Outstanding/Future Work:* - Implement RecyclerView's list of active ticket cards and click behaviors.
 * - Access Firestore queries for active states.*/

public class ActiveTicketsFragment extends Fragment {

    public ActiveTicketsFragment() {
        super(R.layout.fragment_active_tickets);
    }

}