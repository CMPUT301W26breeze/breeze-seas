package com.example.breeze_seas;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
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
public class TicketsFragmentTest {

    private FragmentScenario<TicketsFragment> scenario;

    @Before
    public void setUp() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(
                TicketsRepositoryTestUtils::resetDemoData
        );
        scenario = FragmentScenario.launchInContainer(
                TicketsFragment.class,
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
    public void ticketsScreen_loadsWithActiveTabVisible() {
        onView(withText("History")).check(matches(isDisplayed()));
        onView(withText("Active")).check(matches(isDisplayed()));
        onView(withText("Attending")).check(matches(isDisplayed()));
        onView(withText("Past Events")).check(matches(isDisplayed()));
        onView(withText("Piano Lessons for Beginners")).check(matches(isDisplayed()));
    }

    @Test
    public void selectingAttendingTab_showsAttendingTicket() {
        onView(withText("Attending")).perform(click());

        onView(withText("Summer Music Festival")).check(matches(isDisplayed()));
    }

    @Test
    public void selectingPastTab_showsPastTicketHistory() {
        onView(withText("Past Events")).perform(click());

        onView(withText("Beginner Swimming Lessons")).check(matches(isDisplayed()));
    }
}
