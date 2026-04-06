package com.example.breeze_seas;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.view.View;
import android.widget.ImageButton;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.fragment.app.testing.FragmentScenario;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class AdminBrowseEventsFragmentTest {

    /**
     * Builds a basic Event with the given name and event ID.
     */
    private Event makeEvent(String name, String eventId) {
        Event event = new Event("organizer-id", 10);
        event.setEventId(eventId);
        event.setName(name);
        event.setDescription("Description for " + name);
        return event;
    }

    /**
     * Launches AdminDashboardFragment as the root, injects mock events into AdminViewModel,
     * then clicks View All Events to navigate to AdminBrowseEventsFragment.
     * The container ID and FragmentManager are captured in holders so subsequent steps
     * can query whichever fragment is currently in the container after navigation.
     */
    private FragmentScenario<AdminDashboardFragment> navigateToEventsScreen(
            List<Event> events,
            int[] containerIdHolder,
            FragmentManager[] fmHolder
    ) {
        FragmentScenario<AdminDashboardFragment> scenario = FragmentScenario.launchInContainer(
                AdminDashboardFragment.class,
                null,
                R.style.Theme_Breezeseas,
                Lifecycle.State.RESUMED
        );

        scenario.onFragment(fragment -> {
            containerIdHolder[0] = ((View) fragment.requireView().getParent()).getId();
            fmHolder[0] = fragment.requireActivity().getSupportFragmentManager();

            AdminViewModel viewModel = new ViewModelProvider(fragment.requireActivity())
                    .get(AdminViewModel.class);
            viewModel.getEvents().setValue(new ArrayList<>(events));

            fragment.requireView().findViewById(R.id.ad_btn_view_events).performClick();
        });

        return scenario;
    }

    /**
     * Verifies that event data persists across navigation. Three mock events are injected,
     * the user navigates to AdminBrowseEventsFragment, back to AdminDashboardFragment via
     * the toolbar, then forward again to AdminBrowseEventsFragment. The same three events
     * should still be present.
     */
    @Test
    public void testEventsDataPersistsAfterNavigatingAwayAndBack() {
        List<Event> events = Arrays.asList(
                makeEvent("Summer Festival", "event-1"),
                makeEvent("Winter Gala", "event-2"),
                makeEvent("Spring Fair", "event-3")
        );

        final int[] containerIdHolder = new int[1];
        final FragmentManager[] fmHolder = new FragmentManager[1];
        FragmentScenario<AdminDashboardFragment> scenario =
                navigateToEventsScreen(events, containerIdHolder, fmHolder);

        // Verify events loaded, then navigate back to dashboard via toolbar
        scenario.onFragment(fragment -> {
            Fragment current = fmHolder[0].findFragmentById(containerIdHolder[0]);
            assertTrue(current instanceof AdminBrowseEventsFragment);

            RecyclerView rv = current.requireView().findViewById(R.id.abe_rv_events_list);
            assertEquals(3, rv.getAdapter().getItemCount());

            android.view.ViewGroup toolbar = current.requireView().findViewById(R.id.abe_topAppBar);
            View navButton = null;
            for (int i = 0; i < toolbar.getChildCount(); i++) {
                if (toolbar.getChildAt(i) instanceof ImageButton) {
                    navButton = toolbar.getChildAt(i);
                    break;
                }
            }
            assertNotNull("Toolbar navigation icon should exist", navButton);
            navButton.performClick();
        });

        // Verify back on dashboard, then navigate forward again
        scenario.onFragment(fragment -> {
            Fragment current = fmHolder[0].findFragmentById(containerIdHolder[0]);
            assertTrue(current instanceof AdminDashboardFragment);
            current.requireView().findViewById(R.id.ad_btn_view_events).performClick();
        });

        // Verify same events still present after returning
        scenario.onFragment(fragment -> {
            Fragment current = fmHolder[0].findFragmentById(containerIdHolder[0]);
            assertTrue(current instanceof AdminBrowseEventsFragment);

            RecyclerView rv = current.requireView().findViewById(R.id.abe_rv_events_list);
            assertNotNull(rv);
            assertEquals("Events should persist after navigating away and back", 3,
                    rv.getAdapter().getItemCount());
        });
    }
}
