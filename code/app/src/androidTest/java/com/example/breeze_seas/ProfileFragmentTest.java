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
import androidx.test.espresso.action.ViewActions;
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

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule =
            new ActivityScenarioRule<>(MainActivity.class);


    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context);
        }
    }

    @Test
    public void testProfileComponentsVisibility() throws InterruptedException {

        Thread.sleep(5000);
        onView(withId(R.id.nav_profile)).perform(click());

        // Wait for Firestore
        Thread.sleep(9000);

        onView(withId(R.id.profile_image)).check(matches(isDisplayed()));
        onView(withId(R.id.first_name_filled_text_field)).check(matches(isDisplayed()));
        onView(withId(R.id.save_button))
                .perform(ViewActions.scrollTo())
                .check(matches(isDisplayed()));
    }

    @Test
    public void testEditFieldToggle() throws InterruptedException {

        Thread.sleep(5000);
        onView(withId(R.id.nav_profile)).perform(click());

        // Wait for Firestore
        Thread.sleep(6000);

        onView(withId(R.id.edit_first_name_button)).perform(click());
        onView(allOf(isDescendantOfA(withId(R.id.first_name_filled_text_field)),
                isAssignableFrom(EditText.class)))
                .check(matches(isEnabled()));
    }

    @Test
    public void testInvalidEmailToast() throws InterruptedException {

        Thread.sleep(5000);
        onView(withId(R.id.nav_profile)).perform(click());

        Thread.sleep(4000);

        onView(withId(R.id.edit_email_button)).perform(click());

        // Input bad data
        onView(allOf(isDescendantOfA(withId(R.id.email_filled_text_field)),
                isAssignableFrom(EditText.class)))
                .perform(replaceText("not-an-email"));

        onView(withId(R.id.save_button))
                .perform(ViewActions.scrollTo())
                        .perform(click());

        Thread.sleep(2000);

        // Handle the confirmation dialog if your app has one
        onView(withText("Yes")).perform(click());

        // Wait for Firebase/Validation logic to trigger the Toast
        Thread.sleep(2000);

        onView(withText("Incorrect Email!"))
                .inRoot(withDecorView(not(is(getActivityDecorView()))))
                .check(matches(isDisplayed()));
    }

    private View getActivityDecorView() {
        final View[] decorView = new View[1];
        activityRule.getScenario().onActivity(activity ->
                decorView[0] = activity.getWindow().getDecorView()
        );
        return decorView[0];
    }

}