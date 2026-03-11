package com.example.breeze_seas;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import androidx.fragment.app.FragmentFactory;
import androidx.fragment.app.testing.FragmentScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class TicketsFragmentTest {

    private FragmentScenario<TicketsFragment> scenario;

    @Before
    public void setUp() {
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
    public void ticketsScreen_loadsWithTabsVisible() {
        onView(withText("Your Tickets")).check(matches(isDisplayed()));
        onView(withText("Active")).check(matches(isDisplayed()));
        onView(withText("Attending")).check(matches(isDisplayed()));
        onView(withText("Past")).check(matches(isDisplayed()));
    }

    @Test
    public void selectingAttendingTab_doesNotCrash() {
        onView(withText("Attending")).perform(click());
        onView(withText("Attending")).check(matches(isDisplayed()));
    }

    @Test
    public void selectingPastTab_doesNotCrash() {
        onView(withText("Past")).perform(click());
        onView(withText("Past")).check(matches(isDisplayed()));
    }
}
