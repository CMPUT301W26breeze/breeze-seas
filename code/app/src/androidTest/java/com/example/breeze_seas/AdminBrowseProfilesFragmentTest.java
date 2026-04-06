package com.example.breeze_seas;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
public class AdminBrowseProfilesFragmentTest {

    /**
     * Builds a non-admin User with the given first name, last name, and device ID.
     */
    private User makeUser(String firstName, String lastName, String deviceId) {
        User user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setDeviceId(deviceId);
        user.setEmail(firstName.toLowerCase() + "@test.com");
        user.setAdmin(false);
        return user;
    }

    /**
     * Launches AdminBrowseProfilesFragment in an isolated container, pre-populates
     * AdminViewModel's user LiveData with the given list before the fragment resumes,
     * then sets it again immediately after resume.
     */
    private FragmentScenario<AdminBrowseProfilesFragment> launchWithUsers(List<User> users) {
        FragmentScenario<AdminBrowseProfilesFragment> scenario = FragmentScenario.launchInContainer(
                AdminBrowseProfilesFragment.class,
                null,
                R.style.Theme_Breezeseas,
                Lifecycle.State.INITIALIZED
        );

        scenario.onFragment(fragment -> {
            AdminViewModel viewModel = new ViewModelProvider(fragment.requireActivity())
                    .get(AdminViewModel.class);
            viewModel.getUsers().setValue(new ArrayList<>(users));
        });

        scenario.moveToState(Lifecycle.State.RESUMED);

        scenario.onFragment(fragment -> {
            AdminViewModel viewModel = new ViewModelProvider(fragment.requireActivity())
                    .get(AdminViewModel.class);
            viewModel.getUsers().setValue(new ArrayList<>(users));
        });

        return scenario;
    }

    /**
     * Verifies that three mock non-admin users are all shown in the RecyclerView.
     * The adapter filters out admin accounts so all three should appear.
     */
    @Test
    public void testProfilesListShowsMockUsers() {
        List<User> users = Arrays.asList(
                makeUser("Alice", "Smith", "device-1"),
                makeUser("Bob", "Jones", "device-2"),
                makeUser("Carol", "White", "device-3")
        );

        FragmentScenario<AdminBrowseProfilesFragment> scenario = launchWithUsers(users);

        scenario.onFragment(fragment -> {
            RecyclerView rv = fragment.requireView().findViewById(R.id.abp_rv_profiles_list);
            assertNotNull(rv);
            assertEquals(3, rv.getAdapter().getItemCount());
        });
    }

    /**
     * Verifies that admin users are excluded from the displayed list.
     * Two regular users and one admin are injected. Only the two regular users
     * should appear since the adapter always strips admin accounts.
     */
    @Test
    public void testAdminUsersAreExcluded() {
        User admin = makeUser("Super", "Admin", "device-admin");
        admin.setAdmin(true);

        List<User> users = Arrays.asList(
                makeUser("Alice", "Smith", "device-1"),
                admin,
                makeUser("Bob", "Jones", "device-2")
        );

        FragmentScenario<AdminBrowseProfilesFragment> scenario = launchWithUsers(users);

        scenario.onFragment(fragment -> {
            RecyclerView rv = fragment.requireView().findViewById(R.id.abp_rv_profiles_list);
            assertNotNull(rv);
            assertEquals(2, rv.getAdapter().getItemCount());
        });
    }

    /**
     * Verifies that typing a name into the search bar filters the list to only
     * profiles whose display name, username, or device ID contains the query.
     * Two users are injected and only the one matching the query should remain.
     */
    @Test
    public void testSearchFilterShowsMatchingProfile() {
        List<User> users = Arrays.asList(
                makeUser("Alice", "Smith", "device-1"),
                makeUser("Bob", "Jones", "device-2")
        );

        FragmentScenario<AdminBrowseProfilesFragment> scenario = launchWithUsers(users);

        onView(withId(R.id.abp_search_edit_text)).perform(replaceText("Alice"));

        scenario.onFragment(fragment -> {
            RecyclerView rv = fragment.requireView().findViewById(R.id.abp_rv_profiles_list);
            assertNotNull(rv);
            assertEquals(1, rv.getAdapter().getItemCount());
        });
    }

    /**
     * Verifies that typing a query that matches no profiles results in an empty list.
     */
    @Test
    public void testSearchFilterNoMatchShowsEmptyList() {
        List<User> users = Arrays.asList(
                makeUser("Alice", "Smith", "device-1"),
                makeUser("Bob", "Jones", "device-2")
        );

        FragmentScenario<AdminBrowseProfilesFragment> scenario = launchWithUsers(users);

        onView(withId(R.id.abp_search_edit_text)).perform(replaceText("zzznomatch"));

        scenario.onFragment(fragment -> {
            RecyclerView rv = fragment.requireView().findViewById(R.id.abp_rv_profiles_list);
            assertNotNull(rv);
            assertEquals(0, rv.getAdapter().getItemCount());
        });
    }

    /**
     * Verifies that an empty user list results in no items shown in the RecyclerView.
     */
    @Test
    public void testEmptyUserListShowsNoProfiles() {
        FragmentScenario<AdminBrowseProfilesFragment> scenario = launchWithUsers(new ArrayList<>());

        onView(withId(R.id.abp_rv_profiles_list)).check(matches(isDisplayed()));

        scenario.onFragment(fragment -> {
            RecyclerView rv = fragment.requireView().findViewById(R.id.abp_rv_profiles_list);
            assertNotNull(rv);
            assertEquals(0, rv.getAdapter().getItemCount());
        });
    }
}
