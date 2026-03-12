package com.example.breeze_seas;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class TicketTabMapperTest {

    @Test
    public void mapToTab_pendingReturnsActive() {
        assertEquals(TicketTabMapper.TicketTab.ACTIVE,
                TicketTabMapper.mapToTab(TicketTabMapper.TicketState.PENDING));
    }

    @Test
    public void mapToTab_backupReturnsActive() {
        assertEquals(TicketTabMapper.TicketTab.ACTIVE,
                TicketTabMapper.mapToTab(TicketTabMapper.TicketState.BACKUP));
    }

    @Test
    public void mapToTab_actionRequiredReturnsActive() {
        assertEquals(TicketTabMapper.TicketTab.ACTIVE,
                TicketTabMapper.mapToTab(TicketTabMapper.TicketState.ACTION_REQUIRED));
    }

    @Test
    public void mapToTab_acceptedReturnsAttending() {
        assertEquals(TicketTabMapper.TicketTab.ATTENDING,
                TicketTabMapper.mapToTab(TicketTabMapper.TicketState.ACCEPTED));
    }

    @Test
    public void mapToTab_declinedReturnsPast() {
        assertEquals(TicketTabMapper.TicketTab.PAST,
                TicketTabMapper.mapToTab(TicketTabMapper.TicketState.DECLINED));
    }

    @Test
    public void mapToTab_cancelledReturnsPast() {
        assertEquals(TicketTabMapper.TicketTab.PAST,
                TicketTabMapper.mapToTab(TicketTabMapper.TicketState.CANCELLED));
    }

    @Test
    public void mapToTab_notSelectedReturnsPast() {
        assertEquals(TicketTabMapper.TicketTab.PAST,
                TicketTabMapper.mapToTab(TicketTabMapper.TicketState.NOT_SELECTED));
    }

    @Test
    public void filterStatesForTab_activeReturnsOnlyActiveStates() {
        List<TicketTabMapper.TicketState> states = Arrays.asList(
                TicketTabMapper.TicketState.PENDING,
                TicketTabMapper.TicketState.ACCEPTED,
                TicketTabMapper.TicketState.BACKUP,
                TicketTabMapper.TicketState.DECLINED,
                TicketTabMapper.TicketState.ACTION_REQUIRED,
                TicketTabMapper.TicketState.CANCELLED,
                TicketTabMapper.TicketState.NOT_SELECTED
        );

        assertEquals(
                Arrays.asList(
                        TicketTabMapper.TicketState.PENDING,
                        TicketTabMapper.TicketState.BACKUP,
                        TicketTabMapper.TicketState.ACTION_REQUIRED
                ),
                TicketTabMapper.filterStatesForTab(states, TicketTabMapper.TicketTab.ACTIVE)
        );
    }

    @Test
    public void filterStatesForTab_attendingReturnsOnlyAcceptedStates() {
        List<TicketTabMapper.TicketState> states = Arrays.asList(
                TicketTabMapper.TicketState.PENDING,
                TicketTabMapper.TicketState.ACCEPTED,
                TicketTabMapper.TicketState.BACKUP,
                TicketTabMapper.TicketState.DECLINED
        );

        assertEquals(
                Arrays.asList(TicketTabMapper.TicketState.ACCEPTED),
                TicketTabMapper.filterStatesForTab(states, TicketTabMapper.TicketTab.ATTENDING)
        );
    }

    @Test
    public void filterStatesForTab_pastReturnsOnlyPastStates() {
        List<TicketTabMapper.TicketState> states = Arrays.asList(
                TicketTabMapper.TicketState.CANCELLED,
                TicketTabMapper.TicketState.PENDING,
                TicketTabMapper.TicketState.DECLINED,
                TicketTabMapper.TicketState.ACCEPTED,
                TicketTabMapper.TicketState.NOT_SELECTED
        );

        assertEquals(
                Arrays.asList(
                        TicketTabMapper.TicketState.CANCELLED,
                        TicketTabMapper.TicketState.DECLINED,
                        TicketTabMapper.TicketState.NOT_SELECTED
                ),
                TicketTabMapper.filterStatesForTab(states, TicketTabMapper.TicketTab.PAST)
        );
    }
}
