package com.example.breeze_seas;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Minimal helper for classifying ticket states into Tickets tabs.
 *
 * <p>This keeps tab-mapping logic out of UI tests so it can be validated with plain JUnit.
 */
public final class TicketTabMapper {

    public enum TicketState {
        PENDING,
        BACKUP,
        ACTION_REQUIRED,
        ACCEPTED,
        DECLINED,
        CANCELLED,
        NOT_SELECTED
    }

    public enum TicketTab {
        ACTIVE,
        ATTENDING,
        PAST
    }

    private TicketTabMapper() {
    }

    @NonNull
    public static TicketTab mapToTab(@NonNull TicketState state) {
        switch (state) {
            case PENDING:
            case BACKUP:
            case ACTION_REQUIRED:
                return TicketTab.ACTIVE;
            case ACCEPTED:
                return TicketTab.ATTENDING;
            case DECLINED:
            case CANCELLED:
            case NOT_SELECTED:
            default:
                return TicketTab.PAST;
        }
    }

    @NonNull
    public static List<TicketState> filterStatesForTab(
            @NonNull List<TicketState> states,
            @NonNull TicketTab tab
    ) {
        List<TicketState> filteredStates = new ArrayList<>();
        for (TicketState state : states) {
            if (mapToTab(state) == tab) {
                filteredStates.add(state);
            }
        }
        return filteredStates;
    }
}
