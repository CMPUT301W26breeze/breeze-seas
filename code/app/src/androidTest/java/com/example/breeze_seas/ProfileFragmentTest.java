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

import android.view.View;
import android.widget.EditText;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class ProfileFragmentTest {

    private View decorView;

        @Rule
        public ActivityScenarioRule<MainActivity> activityRule =
                new ActivityScenarioRule<>(MainActivity.class);

        /**
         * Test if the Profile Fragment UI components are visible to the user.
         */
        @Test
        public void testProfileComponentsVisibility() {
            // Navigate to Profile (Assuming you use BottomNav)
            onView(withId(R.id.nav_profile)).perform(click());

            // Check if main layouts are displayed
            onView(withId(R.id.profile_image)).check(matches(isDisplayed()));
            onView(withId(R.id.first_name_filled_text_field)).check(matches(isDisplayed()));
            onView(withId(R.id.save_button)).check(matches(isDisplayed()));
        }

        /**
         * Test the "Edit" toggle logic. When the edit button is clicked,
         * the EditText should become enabled.
         */
        @Test
        public void testEditFieldToggle() {
            onView(withId(R.id.first_name_filled_text_field)).perform(click());

            // Click the edit icon for First Name
            onView(withId(R.id.edit_first_name_button)).perform(click());

            // Verify the internal EditText is now enabled
            onView(allOf(isDescendantOfA(withId(R.id.first_name_filled_text_field)),
                    isAssignableFrom(EditText.class)))
                    .check(matches(isEnabled()));
        }

    @Before
    public void setUp() {
        activityRule.getScenario().onActivity(activity -> {
            decorView = activity.getWindow().getDecorView();
        });
    }
    /**
     * Test the Secret Tap functionality. Clicking the profile image 5 times
     * should trigger the admin verification logic.
     */
    @Test
    public void testSecretAdminAccess() {
        onView(withId(R.id.nav_profile)).perform(click());

        for (int i = 0; i < 5; i++) {
            onView(withId(R.id.profile_image)).perform(click());
        }

        // Now 'decorView' is a View object, which 'not()' and 'is()' understand!
        onView(withText("Successfully verified!"))
                .inRoot(withDecorView(not(is(decorView))))
                .check(matches(isDisplayed()));
    }

        /**
         * Test email validation logic.
         */
        @Test
        public void testInvalidEmailToast() {
            onView(withId(R.id.nav_profile)).perform(click());

            // Enable field and type invalid email
            onView(withId(R.id.edit_email_button)).perform(click());
            onView(withId(R.id.email_filled_text_field)).perform(replaceText("invalid-email"));

            // Click Save -> Click "Yes" on Dialog
            onView(withId(R.id.save_button)).perform(click());
            onView(withText("Yes")).perform(click());

            // Check for the Toast
            activityRule.getScenario().onActivity(activity -> {
                onView(withText("Incorrect Email!"))
                        .inRoot(withDecorView(not(activity.getWindow().getDecorView())))
                        .check(matches(isDisplayed()));
            });
        }

}
