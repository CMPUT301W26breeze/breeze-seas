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
import android.widget.EditText;

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
}
