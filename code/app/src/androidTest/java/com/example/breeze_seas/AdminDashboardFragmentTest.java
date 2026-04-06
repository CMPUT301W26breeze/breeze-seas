package com.example.breeze_seas;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;

import androidx.appcompat.app.AlertDialog;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.fragment.app.testing.FragmentScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class AdminDashboardFragmentTest {

    private static final int DELAY = 1000;

    private static void pause() {
        try { Thread.sleep(DELAY); } catch (InterruptedException ignored) {}
    }

    /**
     * Launches ProfileFragment in an isolated container with a mocked non-admin user.
     * The mock immediately delivers the fake user to any getUser() call so that
     * Firebase is never contacted and the UI is populated synchronously.
     */
    private FragmentScenario<ProfileFragment> launchProfileWithMockUser() {
        FragmentScenario<ProfileFragment> scenario = FragmentScenario.launchInContainer(
                ProfileFragment.class,
                null,
                R.style.Theme_Breezeseas,
                Lifecycle.State.INITIALIZED
        );

        scenario.onFragment(fragment -> {
            User fakeUser = new User();
            fakeUser.setDeviceId("test-device-id");
            fakeUser.setFirstName("John");
            fakeUser.setLastName("Doe");
            fakeUser.setEmail("john@example.com");
            fakeUser.setAdmin(false);

            UserDB mockUserDB = mock(UserDB.class);
            doAnswer(invocation -> {
                UserDB.OnUserLoadedListener listener = invocation.getArgument(1);
                listener.onUserLoaded(fakeUser);
                return null;
            }).when(mockUserDB).getUser(anyString(), any());

            fragment.setUserDB(mockUserDB);

            new ViewModelProvider(fragment.requireActivity())
                    .get(SessionViewModel.class)
                    .setAndroidID("test-device-id");
        });

        scenario.moveToState(Lifecycle.State.RESUMED);
        pause();
        return scenario;
    }

    /**
     * Taps the profile image five times on the UI thread to trigger the admin
     * verification flow in ProfileFragment.
     */
    private void tapProfileImageFiveTimes(FragmentScenario<ProfileFragment> scenario) {
        scenario.onFragment(fragment -> {
            for (int i = 0; i < 5; i++) {
                fragment.requireView().findViewById(R.id.profile_image).performClick();
            }
        });
        pause();
    }

    /**
     * Launches AdminDashboardFragment directly in an isolated container.
     * No Firebase or login flow is involved.
     */
    private FragmentScenario<AdminDashboardFragment> launchDashboard() {
        FragmentScenario<AdminDashboardFragment> scenario = FragmentScenario.launchInContainer(
                AdminDashboardFragment.class,
                null,
                R.style.Theme_Breezeseas,
                Lifecycle.State.RESUMED
        );
        pause();
        return scenario;
    }

    /**
     * Clicks a dashboard button, asserts the expected browse fragment is loaded,
     * then clicks the toolbar navigation icon and asserts AdminDashboardFragment
     * is back in the container.
     *
     * The container ID and FragmentManager are captured before the first click
     * because the click replaces AdminDashboardFragment, making requireView()
     * unavailable on it afterward.
     *
     * MaterialToolbar's navigation icon is a separate ImageButton child inside
     * the toolbar. Clicking the toolbar view itself does not trigger the navigation
     * listener, so the first ImageButton child is located and clicked directly.
     */
    private <F extends Fragment> void assertNavigateAndBack(
            FragmentScenario<AdminDashboardFragment> scenario,
            int buttonId,
            Class<F> expectedClass,
            int toolbarId
    ) {
        final int[] containerIdHolder = new int[1];
        final FragmentManager[] fmHolder = new FragmentManager[1];

        scenario.onFragment(fragment -> {
            containerIdHolder[0] = ((View) fragment.requireView().getParent()).getId();
            fmHolder[0] = fragment.requireActivity().getSupportFragmentManager();
            fragment.requireView().findViewById(buttonId).performClick();
        });

        pause();

        scenario.onFragment(fragment -> {
            Fragment current = fmHolder[0].findFragmentById(containerIdHolder[0]);
            assertTrue(
                    expectedClass.getSimpleName() + " should be loaded after button click",
                    expectedClass.isInstance(current)
            );

            assertNotNull(current);
            ViewGroup toolbar = current.requireView().findViewById(toolbarId);
            View navButton = null;
            for (int i = 0; i < toolbar.getChildCount(); i++) {
                if (toolbar.getChildAt(i) instanceof ImageButton) {
                    navButton = toolbar.getChildAt(i);
                    break;
                }
            }
            assertNotNull("Toolbar navigation icon button should exist", navButton);
            navButton.performClick();
        });

        pause();

        scenario.onFragment(fragment -> {
            Fragment current = fmHolder[0].findFragmentById(containerIdHolder[0]);
            assertTrue(
                    "AdminDashboardFragment should be shown after pressing back",
                    current instanceof AdminDashboardFragment
            );
        });

        pause();
    }

    /**
     * Verifies that tapping the profile image five times shows the AdminAuthDialogFragment.
     * The dialog is identified by the tag "AdminAuth" in the fragment manager.
     */
    @Test
    public void testFiveTapsOnProfileImageShowsAdminDialog() {
        FragmentScenario<ProfileFragment> scenario = launchProfileWithMockUser();
        tapProfileImageFiveTimes(scenario);

        scenario.onFragment(fragment -> {
            DialogFragment dialog = (DialogFragment) fragment.getParentFragmentManager()
                    .findFragmentByTag("AdminAuth");
            assertNotNull("Admin auth dialog should be showing", dialog);
            assertTrue("Admin auth dialog should be added", dialog.isAdded());
        });

        pause();
    }

    /**
     * Verifies that submitting an incorrect password in the admin auth dialog
     * does not navigate to AdminDashboardFragment. The container should still
     * hold ProfileFragment after the failed attempt.
     */
    @Test
    public void testWrongPasswordDoesNotNavigate() {
        FragmentScenario<ProfileFragment> scenario = launchProfileWithMockUser();
        tapProfileImageFiveTimes(scenario);

        scenario.onFragment(fragment -> {
            AdminAuthDialogFragment dialogFragment = (AdminAuthDialogFragment)
                    fragment.getParentFragmentManager().findFragmentByTag("AdminAuth");
            assertNotNull(dialogFragment);

            AlertDialog alertDialog = (AlertDialog) dialogFragment.getDialog();
            assertNotNull(alertDialog);

            EditText input = alertDialog.findViewById(R.id.admin_password_input);
            input.setText("wrongpassword");
            pause();
            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        });

        pause();

        scenario.onFragment(fragment -> {
            int containerId = ((View) fragment.requireView().getParent()).getId();
            Fragment current = fragment.requireActivity()
                    .getSupportFragmentManager().findFragmentById(containerId);
            assertNull(
                    "AdminDashboardFragment should not be loaded after wrong password",
                    current instanceof AdminDashboardFragment ? current : null
            );
        });

        pause();
    }

    /**
     * Verifies that submitting the correct password in the admin auth dialog
     * navigates to AdminDashboardFragment. The UserDB in the dialog is mocked
     * so that the updateUser() call does not reach Firebase.
     */
    @Test
    public void testCorrectPasswordNavigatesToAdminDashboard() {
        FragmentScenario<ProfileFragment> scenario = launchProfileWithMockUser();
        tapProfileImageFiveTimes(scenario);

        final int[] containerIdHolder = new int[1];
        final FragmentManager[] fmHolder = new FragmentManager[1];

        scenario.onFragment(fragment -> {
            containerIdHolder[0] = ((View) fragment.requireView().getParent()).getId();
            fmHolder[0] = fragment.requireActivity().getSupportFragmentManager();

            AdminAuthDialogFragment dialogFragment = (AdminAuthDialogFragment)
                    fragment.getParentFragmentManager().findFragmentByTag("AdminAuth");
            assertNotNull(dialogFragment);

            UserDB mockUserDB = mock(UserDB.class);
            doNothing().when(mockUserDB).updateUser(anyString(), any());
            dialogFragment.setUserDB(mockUserDB);

            AlertDialog alertDialog = (AlertDialog) dialogFragment.getDialog();
            assertNotNull(alertDialog);

            EditText input = alertDialog.findViewById(R.id.admin_password_input);
            input.setText("flyingfish");
            pause();
            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        });

        pause();

        scenario.onFragment(fragment -> {
            Fragment current = fmHolder[0].findFragmentById(containerIdHolder[0]);
            assertTrue(
                    "AdminDashboardFragment should be loaded after correct password",
                    current instanceof AdminDashboardFragment
            );
        });

        pause();
    }

    /**
     * Verifies that clicking the View All Events button navigates to
     * AdminBrowseEventsFragment and that pressing the toolbar back button
     * returns to AdminDashboardFragment.
     */
    @Test
    public void testNavigateToEventsAndBack() {
        FragmentScenario<AdminDashboardFragment> scenario = launchDashboard();
        assertNavigateAndBack(scenario,
                R.id.ad_btn_view_events,
                AdminBrowseEventsFragment.class,
                R.id.abe_topAppBar);
    }

    /**
     * Verifies that clicking the View User Profiles button navigates to
     * AdminBrowseProfilesFragment and that pressing the toolbar back button
     * returns to AdminDashboardFragment.
     */
    @Test
    public void testNavigateToProfilesAndBack() {
        FragmentScenario<AdminDashboardFragment> scenario = launchDashboard();
        assertNavigateAndBack(scenario,
                R.id.ad_btn_view_profiles,
                AdminBrowseProfilesFragment.class,
                R.id.abp_top_app_bar);
    }

    /**
     * Verifies that clicking the View All Images button navigates to
     * AdminBrowseImagesFragment and that pressing the toolbar back button
     * returns to AdminDashboardFragment.
     */
    @Test
    public void testNavigateToImagesAndBack() {
        FragmentScenario<AdminDashboardFragment> scenario = launchDashboard();
        assertNavigateAndBack(scenario,
                R.id.ad_btn_view_images,
                AdminBrowseImagesFragment.class,
                R.id.abi_topAppBar);
    }

    /**
     * Verifies that clicking the View Logs button navigates to
     * AdminBrowseLogsFragment and that pressing the toolbar back button
     * returns to AdminDashboardFragment.
     */
    @Test
    public void testNavigateToLogsAndBack() {
        FragmentScenario<AdminDashboardFragment> scenario = launchDashboard();
        assertNavigateAndBack(scenario,
                R.id.ad_btn_view_logs,
                AdminBrowseLogsFragment.class,
                R.id.abl_topAppBar);
    }
}
