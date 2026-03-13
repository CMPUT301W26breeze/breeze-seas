package com.example.breeze_seas;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.withDecorView;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import android.content.Context;
import android.view.View;
import android.widget.EditText;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.firebase.FirebaseApp;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class ProfileFragmentTest {

    private View decorView;

    @Mock
    private UserDB mockUserDB;

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule =
            new ActivityScenarioRule<>(MainActivity.class);


    @Before
    public void setUp() {
        // Initialize Mockito
        MockitoAnnotations.openMocks(this);

        // Inject the mock into the fragment
        activityRule.getScenario().onActivity(activity -> {
            ProfileFragment fragment = (ProfileFragment) activity.getSupportFragmentManager()
                    .findFragmentById(R.id.fragment_container); // Use your actual ID
            if (fragment != null) {
                fragment.setUserDB(mockUserDB);
            }
        });
    }

    @Test
    public void testProfileComponentsVisibility() throws InterruptedException {

        // Wait for Firestore
        Thread.sleep(6000);

        onView(withId(R.id.nav_profile)).perform(click());
        onView(withId(R.id.profile_image)).check(matches(isDisplayed()));
        onView(withId(R.id.first_name_filled_text_field)).check(matches(isDisplayed()));
        onView(withId(R.id.save_button)).check(matches(isDisplayed()));
    }

    @Test
    public void testEditFieldToggle() throws InterruptedException {

        // Wait for Firestore
        Thread.sleep(6000);

        onView(withId(R.id.nav_profile)).perform(click()); // Added navigation
        onView(withId(R.id.edit_first_name_button)).perform(click());
        onView(allOf(isDescendantOfA(withId(R.id.first_name_filled_text_field)),
                isAssignableFrom(EditText.class)))
                .check(matches(isEnabled()));
    }

    @Test
    public void testSecretAdminAccess() throws InterruptedException{

        // Wait for Firestore
        Thread.sleep(6000);

        onView(withId(R.id.nav_profile)).perform(click());
        for (int i = 0; i < 5; i++) {
            onView(withId(R.id.profile_image)).perform(click());
        }
        onView(withText("Successfully verified!"))
                .inRoot(withDecorView(not(is(decorView))))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testInvalidEmailToast() throws InterruptedException {

        // Wait for Firestore
        Thread.sleep(6000);

        onView(withId(R.id.nav_profile)).perform(click());
        onView(withId(R.id.edit_email_button)).perform(click());
        onView(allOf(isDescendantOfA(withId(R.id.email_filled_text_field)),
                isAssignableFrom(EditText.class))).perform(replaceText("invalid-email"));

        onView(withId(R.id.save_button)).perform(click());
        onView(withText("Yes")).perform(click());

        onView(withText("Incorrect Email!"))
                .inRoot(withDecorView(not(is(decorView))))
                .check(matches(isDisplayed()));
    }
}