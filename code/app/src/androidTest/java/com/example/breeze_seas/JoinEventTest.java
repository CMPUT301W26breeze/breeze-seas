package com.example.breeze_seas;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertTrue;

import androidx.fragment.app.testing.FragmentScenario;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Test for the {@link Event} Joining functionality.
 */
@RunWith(AndroidJUnit4.class)
public class JoinEventTest {

    private FirebaseFirestore db;
    private final String TEST_EVENT_ID = "TEST_JOIN_EVENT_1";
    private final String TEST_USER_ID = "test_device_join_1";

    /**
     * Sets up the test environment before every test case.
     * @throws InterruptedException if synchronization latches time out.
     */
    @Before
    public void setup() throws InterruptedException {
        db = FirebaseFirestore.getInstance();

        CountDownLatch authLatch = new CountDownLatch(1);
        FirebaseAuth.getInstance().signInAnonymously()
                .addOnCompleteListener(t -> authLatch.countDown());
        authLatch.await(5, TimeUnit.SECONDS);

        cleanDb();

        Timestamp now = Timestamp.now();
        CountDownLatch eventCreateLatch = new CountDownLatch(1);
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("eventId", TEST_EVENT_ID);
        eventData.put("name", "Join Test Event");
        eventData.put("organizerId", "org_1");
        eventData.put("description", "Test description");
        eventData.put("eventCapacity", 100);
        eventData.put("waitingListCapacity", 200);
        eventData.put("drawARound", 0);
        eventData.put("isPrivate", false);
        eventData.put("coOrganizerId", new ArrayList<>());
        eventData.put("geolocationEnforced", false);
        eventData.put("imageDocId", null);
        eventData.put("registrationStartTimestamp", now);
        eventData.put("registrationEndTimestamp", now);
        eventData.put("eventStartTimestamp", now);
        eventData.put("eventEndTimestamp", now);
        eventData.put("createdTimestamp", now);
        eventData.put("modifiedTimestamp", now);
        db.collection("events").document(TEST_EVENT_ID)
                .set(eventData)
                .addOnCompleteListener(t -> eventCreateLatch.countDown());
        eventCreateLatch.await(5, TimeUnit.SECONDS);
    }

    /**
     * Performs final cleanup of the Firestore environment after test completion.
     * @throws InterruptedException if cleanup synchronization fails.
     */
    @After
    public void cleanup() throws InterruptedException {
        cleanDb();
    }

    /**
     * Helper method to delete the test event, participant records, and user profiles.
     * @throws InterruptedException if the deletion batch exceeds the timeout.
     */
    private void cleanDb() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(3);
        db.collection("events").document(TEST_EVENT_ID)
                .collection("participants").document(TEST_USER_ID)
                .delete().addOnCompleteListener(t -> latch.countDown());
        db.collection("users").document(TEST_USER_ID)
                .delete().addOnCompleteListener(t -> latch.countDown());
        db.collection("events").document(TEST_EVENT_ID)
                .delete().addOnCompleteListener(t -> latch.countDown());
        latch.await(10, TimeUnit.SECONDS);
    }

    /**
     * Verifies the complete UI-to-Database flow for joining an event.
     * Launches {@link EventDetailsFragment} in an initial state.
     * Mocks the Fragment backstack to prevent errors.
     * Populates ViewModels with a mock {@link User} and {@link Event}.
     * Transitions the fragment to RESUMED and uses Espresso to click "Join".
     * Confirms the Terms & Conditions dialog and accepts it.
     * Queries the real Firestore sub-collection to verify the user exists with "waiting" status.
     * @throws InterruptedException if the background Firestore write or latches time out.
     */
    @Test
    public void testJoinWaitingList() throws InterruptedException {
        FragmentScenario<EventDetailsFragment> scenario = FragmentScenario.launchInContainer(
                EventDetailsFragment.class,
                null,
                R.style.Theme_Breezeseas,
                Lifecycle.State.INITIALIZED
        );

        scenario.onFragment(fragment -> {
            fragment.getParentFragmentManager()
                    .beginTransaction()
                    .addToBackStack("Explore")
                    .commit();

            Timestamp now = Timestamp.now();
            Event testEvent = new Event(
                    TEST_EVENT_ID, false, "org_1", new ArrayList<>(),
                    "Join Test Event", "Test description", null,
                    now, now, now, now, now, now,
                    false, 100, 200, 0,
                    new WaitingList(null, 200),
                    new PendingList(null, 200),
                    new AcceptedList(null, 200),
                    new DeclinedList(null, 200)
            );
            testEvent.getWaitingList().event = testEvent;
            testEvent.getPendingList().event = testEvent;
            testEvent.getAcceptedList().event = testEvent;
            testEvent.getDeclinedList().event = testEvent;

            SessionViewModel sessionVM = new ViewModelProvider(fragment.requireActivity())
                    .get(SessionViewModel.class);
            User testUser = new User(TEST_USER_ID, "Test", "User", "testuser", "test@test.com", false);
            sessionVM.getUser().setValue(testUser);

            ExploreViewModel exploreVM = new ViewModelProvider(fragment.requireActivity())
                    .get(ExploreViewModel.class);
            EventHandler handler = new EventHandler(
                    fragment.requireActivity(),
                    fragment.requireContext(),
                    db.collection("events").whereEqualTo("eventId", TEST_EVENT_ID),
                    TEST_USER_ID,
                    false, false, false
            );
            handler.setEventShown(testEvent);
            exploreVM.setEventHandler(handler);
        });

        scenario.moveToState(Lifecycle.State.RESUMED);
        Thread.sleep(2000);

        onView(withId(R.id.event_details_join_waitlist_button))
                .check(matches(isDisplayed()))
                .perform(click());

        onView(withText("Accept"))
                .check(matches(isDisplayed()))
                .perform(click());

        Thread.sleep(3000);

        CountDownLatch checkParticipant = new CountDownLatch(1);
        final boolean[] found = {false};
        db.collection("events").document(TEST_EVENT_ID)
                .collection("participants").document(TEST_USER_ID)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists() && "waiting".equals(doc.getString("status"))) {
                        found[0] = true;
                    }
                    checkParticipant.countDown();
                })
                .addOnFailureListener(e -> checkParticipant.countDown());

        checkParticipant.await(5, TimeUnit.SECONDS);
        assertTrue("User was not found in participants collection", found[0]);
    }
}