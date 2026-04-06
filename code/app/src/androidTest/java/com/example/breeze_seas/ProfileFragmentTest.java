package com.example.breeze_seas;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
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

/**
 * Instrumentation tests for the profile screen's basic visibility and editing affordances.
 */
@RunWith(AndroidJUnit4.class)
public class ProfileFragmentTest {

    /**
     * Rule that launches the {@link MainActivity} before every test case.
     * This ensures the fragment container is available for navigation.
     */
    @Rule
    public ActivityScenarioRule<MainActivity> activityRule =
            new ActivityScenarioRule<>(MainActivity.class);


    /**
     * Initializes the testing environment.
     * * <p>Ensures that {@link FirebaseApp} is initialized within the application context
     * if it hasn't been already, preventing "Firebase not initialized" exceptions
     * during real database operations.</p>
     */
    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context);
        }
    }

    /**
    * Verifies that all essential profile UI components are visible to the user
    * after data is successfully loaded from Firebase.
     */
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

    /**
    * Validates the toggling logic of profile input fields.
     */
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
}
