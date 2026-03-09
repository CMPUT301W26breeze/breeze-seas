package com.example.breeze_seas;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.contrib.RecyclerViewActions.actionOnItem;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import androidx.fragment.app.FragmentFactory;
import androidx.fragment.app.testing.FragmentScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class ActiveTicketsFragmentTest {

    private FragmentScenario<ActiveTicketsFragment> scenario;

    @Before
    public void setUp() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(
                TicketsRepositoryTestUtils::resetDemoData
        );
        scenario = FragmentScenario.launchInContainer(
                ActiveTicketsFragment.class,
                null,
                R.style.Theme_Breezeseas,
                (FragmentFactory) null
        );
    }

    @After
    public void tearDown() {
        if (scenario != null) {
            scenario.close();
        }
    }

    @Test
    public void clickingBackupTicket_showsWaitlistDialog() {
        clickTicketWithTitle("Tech Conference 2026");

        onView(withText("You're on the Waitlist")).check(matches(isDisplayed()));
        onView(withText("Okay")).check(matches(isDisplayed()));
    }

    @Test
    public void clickingActionRequiredTicket_showsInvitationDialog() {
        clickTicketWithTitle("Community Dance Night");

        onView(withText("You won!")).check(matches(isDisplayed()));
        onView(withText("Accept Invitation")).check(matches(isDisplayed()));
        onView(withText("Decline")).check(matches(isDisplayed()));
    }

    @Test
    public void acceptInvitationButton_showsConfirmationSnackbar() {
        clickTicketWithTitle("Community Dance Night");

        onView(withText("Accept Invitation")).perform(click());

        onView(withText("Invitation accepted. Ticket moved to Attending."))
                .check(matches(isDisplayed()));
    }

    @Test
    public void declineInvitationFlow_showsConfirmationSnackbar() {
        clickTicketWithTitle("Community Dance Night");

        onView(withText("Decline")).perform(click());
        onView(withText("Decline invitation?")).check(matches(isDisplayed()));
        onView(withText("Yes, decline")).perform(click());

        onView(withText("Invitation declined")).check(matches(isDisplayed()));
    }

    private void clickTicketWithTitle(String ticketTitle) {
        onView(withId(R.id.active_tickets_recycler)).perform(
                actionOnItem(
                        hasDescendant(withText(ticketTitle)),
                        click()
                )
        );
    }
}
