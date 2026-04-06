package com.example.breeze_seas;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.fail;

import androidx.fragment.app.testing.FragmentScenario;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public class StatusListTest {

    private FirebaseFirestore db;
    private final String TEST_EVENT_ID = "TEST_EVENT_1";
    private final String[] ALL_DEVICE_IDS = {"device_1" ,"device_2" , "device_3"};

    @Before
    public void setup() throws InterruptedException {
        db = FirebaseFirestore.getInstance();
        CountDownLatch startLatch = new CountDownLatch(1);
        FirebaseAuth.getInstance().signInAnonymously()
                .addOnCompleteListener(t -> startLatch.countDown());
        startLatch.await(5, TimeUnit.SECONDS);

        cleanDb();
        CountDownLatch eventLatch = new CountDownLatch(1);
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("eventId", TEST_EVENT_ID);
        eventData.put("name", "Final Test Event");
        db.collection("events").document(TEST_EVENT_ID)
                .set(eventData)
                .addOnCompleteListener(t -> eventLatch.countDown());
        eventLatch.await(5, TimeUnit.SECONDS);


    }

    private void cleanDb() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(ALL_DEVICE_IDS.length * 2);
        for (String id : ALL_DEVICE_IDS) {
            db.collection("events").document(TEST_EVENT_ID)
                    .collection("participants").document(id)
                    .delete().addOnCompleteListener(t -> latch.countDown());
            db.collection("users").document(id)
                    .delete().addOnCompleteListener(t -> latch.countDown());
        }

        db.collection("events").document(TEST_EVENT_ID)
                .delete()
                .addOnCompleteListener(t -> latch.countDown());

        latch.await(10, TimeUnit.SECONDS);

        db.collection("notifications")
                .whereEqualTo("eventId", TEST_EVENT_ID)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot) {
                        doc.getReference().delete();
                    }
                });

        latch.await(10, TimeUnit.SECONDS);
        Thread.sleep(1000);
    }

    private void writeInUsers(String deviceId, String firstName,
                                             String lastName, String userName, CountDownLatch latch) {
        Map<String, Object> profile = new HashMap<>();
        profile.put("deviceId", deviceId);
        profile.put("firstName", firstName);
        profile.put("lastName", lastName);
        profile.put("userName", userName);
        profile.put("email", "");
        profile.put("phoneNumber", "");
        db.collection("users").document(deviceId)
                .set(profile)
                .addOnCompleteListener(t -> latch.countDown());

    }

    private FragmentScenario<OrganizerListHostFragment> launchHostFragment(int waitingListCapacity) {
        FragmentScenario<OrganizerListHostFragment> scenario = FragmentScenario.launchInContainer(
                OrganizerListHostFragment.class,
                null,
                R.style.Theme_Breezeseas,
                Lifecycle.State.INITIALIZED
        );

        scenario.onFragment(fragment -> {
            SessionViewModel viewModel = new ViewModelProvider(fragment.requireActivity())
                    .get(SessionViewModel.class);

            Event testEvent = new Event("organizer_id", 2);
            testEvent.setEventId(TEST_EVENT_ID);
            testEvent.setName("Lottery Test Event");

            testEvent.setWaitingListCapacity(waitingListCapacity);
            testEvent.getWaitingList().setCapacity(waitingListCapacity);
            viewModel.getEventShown().setValue(testEvent);
        });

        scenario.moveToState(Lifecycle.State.RESUMED);
        return scenario;
    }

    @Test
    public void appearsOnWaitingList() throws InterruptedException {
        CountDownLatch write = new CountDownLatch(1);
        writeInUsers(ALL_DEVICE_IDS[0], "FNAME_1", "LNAME_1", "UNAME_1", write);
        write.await(10, TimeUnit.SECONDS);

        FragmentScenario<OrganizerListHostFragment> scenario = launchHostFragment(2);

        CountDownLatch addLatch = new CountDownLatch(1);
        scenario.onFragment(fragment -> {
            WaitingList list = new ViewModelProvider(fragment.requireActivity())
                    .get(SessionViewModel.class)
                    .getEventShown().getValue().getWaitingList();

            list.addUser(
                    new User(ALL_DEVICE_IDS[0], "FNAME_1", "LNAME_1", "UNAME_1", "", false),
                    new StatusList.ListUpdateListener() {
                        @Override public void onUpdate() { addLatch.countDown(); }
                        @Override public void onError(Exception e) { addLatch.countDown(); }
                    });
        });

        addLatch.await(10, TimeUnit.SECONDS);
        Thread.sleep(3000);

        onData(is(instanceOf(User.class)))
                .inAdapterView(withId(R.id.waiting_frag_list_view))
                .atPosition(0)
                .check(matches(isDisplayed()));

        onData(is(instanceOf(User.class)))
                .inAdapterView(withId(R.id.waiting_frag_list_view))
                .atPosition(0)
                .onChildView(withId(R.id.entrant_name_text))
                .check(matches(withText("FNAME_1 LNAME_1")));
    }

    @Test
    public void testWaitingListCapacity() throws InterruptedException {
        CountDownLatch write = new CountDownLatch(3);
        writeInUsers(ALL_DEVICE_IDS[0], "FNAME_1", "LNAME_1", "UNAME_1", write);
        writeInUsers(ALL_DEVICE_IDS[1], "FNAME_2", "LNAME_2", "UNAME_2", write);
        writeInUsers(ALL_DEVICE_IDS[2], "FNAME_3", "LNAME_3", "UNAME_3", write);
        write.await(10, TimeUnit.SECONDS);

        FragmentScenario<OrganizerListHostFragment> scenario = launchHostFragment(2);
        CountDownLatch fillLatch = new CountDownLatch(2);
        scenario.onFragment(fragment -> {
            WaitingList list = new ViewModelProvider(fragment.requireActivity())
                    .get(SessionViewModel.class)
                    .getEventShown().getValue().getWaitingList();

            for (int i = 0; i < 2; i++) {
                list.addUser(
                        new User(ALL_DEVICE_IDS[i], "FNAME_" + (i+1), "LNAME_" + (i+1), "UNAME_" + (i+1), "", false),
                        new StatusList.ListUpdateListener() {
                            @Override public void onUpdate() { fillLatch.countDown(); }
                            @Override public void onError(Exception e) { fillLatch.countDown(); }
                        });
            }
        });

        fillLatch.await(10, TimeUnit.SECONDS);
        Thread.sleep(1000);

        CountDownLatch extraWriteLatch = new CountDownLatch(1);
        scenario.onFragment(fragment -> {
            WaitingList list = new ViewModelProvider(fragment.requireActivity())
                    .get(SessionViewModel.class)
                    .getEventShown().getValue().getWaitingList();

            list.addUser(
                    new User(ALL_DEVICE_IDS[2], "FNAME_3", "LNAME_3", "UNAME_3", "", false),
                    new StatusList.ListUpdateListener() {
                        @Override
                        public void onUpdate() {
                            extraWriteLatch.countDown();
                            fail("Capacity should have been enforced — overflow user was added");
                        }
                        @Override
                        public void onError(Exception e) {
                            extraWriteLatch.countDown();
                        }
                    });
        });

        extraWriteLatch.await(10, TimeUnit.SECONDS);
    }

    @Test
    public void testFullLotteryFlow() throws InterruptedException {

        CountDownLatch write = new CountDownLatch(3);
        writeInUsers(ALL_DEVICE_IDS[0], "FNAME_1", "LNAME_1", "UNAME_1", write);
        writeInUsers(ALL_DEVICE_IDS[1], "FNAME_2", "LNAME_2", "UNAME_2", write);
        writeInUsers(ALL_DEVICE_IDS[2], "FNAME_3", "LNAME_3", "UNAME_3", write);
        write.await(10, TimeUnit.SECONDS);


        FragmentScenario<OrganizerListHostFragment> scenario = launchHostFragment(3);
        Thread.sleep(2000);


        CountDownLatch addLatch = new CountDownLatch(3);
        scenario.onFragment(fragment -> {
            WaitingList list = new ViewModelProvider(fragment.requireActivity())
                    .get(SessionViewModel.class)
                    .getEventShown().getValue().getWaitingList();

            for (int i = 0; i < 3; i++) {
                list.addUser(
                        new User(ALL_DEVICE_IDS[i], "FNAME_" + (i+1), "LNAME_" + (i+1), "UNAME_" + (i+1), "", false),
                        new StatusList.ListUpdateListener() {
                            @Override public void onUpdate() { addLatch.countDown(); }
                            @Override public void onError(Exception e) { addLatch.countDown(); }
                        });
            }
        });

        addLatch.await(10, TimeUnit.SECONDS);
        Thread.sleep(2000);

        onView(withId(R.id.btn_run_lottery)).perform(click());
        Thread.sleep(5000);


        onView(withId(R.id.organizer_tab_pending)).perform(click());
        Thread.sleep(2000);

        Matcher<String> anyWinner = anyOf(Arrays.asList(
                is("FNAME_1 LNAME_1"),
                is("FNAME_2 LNAME_2"),
                is("FNAME_3 LNAME_3")
        ));

        onData(is(instanceOf(User.class)))
                .inAdapterView(withId(R.id.pending_frag_list_view))
                .atPosition(0)
                .onChildView(withId(R.id.entrant_name_text))
                .check(matches(withText(anyWinner)));
    }


    @After
    public void cleanup() throws InterruptedException {
        cleanDb();
    }
}