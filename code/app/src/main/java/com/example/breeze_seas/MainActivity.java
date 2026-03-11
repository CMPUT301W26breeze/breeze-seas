package com.example.breeze_seas;

import android.app.AlertDialog;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;

/*** MainActivity is the container for the app's main navigation.
 *
 * <p>Role in Architecture:* - Serves as a single-activity shell that switches top-level Fragments using a
 *
 * <p>Outstanding/Future Work:* - Replace placeholder fragments with actual feature panels.
 * - Consider using the Navigation Component for back-stack consistency once.
 * Flows grow increasingly complex.
 * - Confirm edge-to-edge inset behavior across devices and confirm that the bottom
 *The navigation background is consistent with the theme.
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final String KEY_SELECTED_DESTINATION = "selected_destination";
    private static final int[] TOP_LEVEL_DESTINATIONS = {
            R.id.nav_explore,
            R.id.nav_tickets,
            R.id.nav_organize,
            R.id.nav_notification,
            R.id.nav_profile
    };

    private final ExploreFragment exploreFragment = new ExploreFragment();
    private final OrganizeFragment organizeFragment = new OrganizeFragment();
    private final NotificationFragment notificationFragment = new NotificationFragment();
    private final ProfileFragment profileFragment = new ProfileFragment();

    private SessionViewModel viewModel;
    private int selectedDestinationId = View.NO_ID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        if (savedInstanceState != null) {
            selectedDestinationId = savedInstanceState.getInt(KEY_SELECTED_DESTINATION, View.NO_ID);
        }

        bindBottomNavigation();

        viewModel = new ViewModelProvider(this).get(SessionViewModel.class);

        String androidID = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        viewModel.setAndroidID(androidID);

        showBottomNav(false);

        FirebaseSession.ensureAuthenticated(new FirebaseSession.OnReadyListener() {
            @Override
            public void onReady() {
                UserDB userDBInstance = new UserDB();
                viewModel.setUserDBInstance(userDBInstance);

                userDBInstance.getUser(androidID, new UserDB.OnUserLoadedListener() {
                    @Override
                    public void onUserLoaded(User user) {
                        initializeUI(savedInstanceState, user);
                    }

                    @Override
                    public void onError(Exception e) {
                        Log.e(TAG, "Failed to load user for startup. Falling back to guest flow.", e);
                        initializeUI(savedInstanceState, null);
                    }
                });
            }

            @Override
            public void onError(@NonNull Exception e) {
                Log.e(TAG, "Firebase auth bootstrap failed.", e);
                showFatalAuthError();
            }
        });
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_SELECTED_DESTINATION, selectedDestinationId);
    }

    /**
     * Determines which fragments should be loaded on start. This method is called by
     * the callback function after the firebase query.
     *
     * @param savedInstanceState Values pertaining to the current activity
     * @param user User class object, may be null if user does not exist for current device.
     */
    private void initializeUI(Bundle savedInstanceState, User user) {
        if (user == null) {
            showBottomNav(false);
            updateBottomNavSelection(View.NO_ID);

            WelcomeScreenFragment welcomeScreenFragment = new WelcomeScreenFragment();
            setCurrentFragment(welcomeScreenFragment);
            return;
        }

        showBottomNav(true);
        viewModel.setUser(user);

        if (savedInstanceState == null
                || getSupportFragmentManager().findFragmentById(R.id.fragment_container) == null) {
            openTopLevelDestination(R.id.nav_explore);
        } else {
            syncBottomNavSelectionWithCurrentFragment();
        }
    }

    /**
     * Helper wrapper function for controlling visibility of the navigation bar.
     * Allows external control of the navigation bar. For example, being called from other fragments.
     * @param show Boolean value, true to display the bottom navigation
     */
    public void showBottomNav(boolean show) {
        View bottomNav = findViewById(R.id.bottom_navigation);
        View fragmentContainer = findViewById(R.id.fragment_container);
        if (bottomNav == null || fragmentContainer == null) {
            return;
        }

        bottomNav.setVisibility(show ? View.VISIBLE : View.GONE);
        bottomNav.post(() -> applyBottomNavInset(show));
    }

    /*** Replaces the existing fragment in the main fragment container.
     *
     * <p>Note: This use {@code replace()} without adding to the back stack since bottom* Navigation is designed to change top-level destinations rather than generate a
     * back stack is deep.
     ** @param fragment The destination fragment to display.
     */
    public void setCurrentFragment(Fragment fragment) {
        getSupportFragmentManager().popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    public void openSecondaryFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(
                        android.R.anim.fade_in,
                        android.R.anim.fade_out,
                        android.R.anim.fade_in,
                        android.R.anim.fade_out
                )
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(fragment.getClass().getSimpleName())
                .commit();
    }

    private void bindBottomNavigation() {
        for (int destinationId : TOP_LEVEL_DESTINATIONS) {
            View navItem = findViewById(destinationId);
            if (navItem != null) {
                navItem.setOnClickListener(v -> openTopLevelDestination(destinationId));
            }
        }
    }

    private void openTopLevelDestination(int destinationId) {
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (selectedDestinationId == destinationId
                && isCurrentTopLevelFragment(currentFragment, destinationId)) {
            return;
        }

        updateBottomNavSelection(destinationId);

        Fragment destinationFragment = resolveTopLevelFragment(destinationId);
        if (destinationFragment != null) {
            setCurrentFragment(destinationFragment);
        }
    }

    private void syncBottomNavSelectionWithCurrentFragment() {
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);

        if (currentFragment instanceof ExploreFragment) {
            updateBottomNavSelection(R.id.nav_explore);
        } else if (currentFragment instanceof TicketsFragment) {
            updateBottomNavSelection(R.id.nav_tickets);
        } else if (currentFragment instanceof OrganizeFragment) {
            updateBottomNavSelection(R.id.nav_organize);
        } else if (currentFragment instanceof NotificationFragment) {
            updateBottomNavSelection(R.id.nav_notification);
        } else if (currentFragment instanceof ProfileFragment) {
            updateBottomNavSelection(R.id.nav_profile);
        } else {
            updateBottomNavSelection(selectedDestinationId);
        }
    }

    private void updateBottomNavSelection(int destinationId) {
        selectedDestinationId = destinationId;

        for (int navItemId : TOP_LEVEL_DESTINATIONS) {
            View navItem = findViewById(navItemId);
            if (navItem != null) {
                boolean isSelected = navItemId == destinationId;
                navItem.setActivated(isSelected);
                navItem.setSelected(isSelected);
            }
        }
    }

    private void applyBottomNavInset(boolean show) {
        View bottomNav = findViewById(R.id.bottom_navigation);
        View fragmentContainer = findViewById(R.id.fragment_container);
        if (bottomNav == null || fragmentContainer == null) {
            return;
        }

        int bottomPadding = 0;
        if (show) {
            ViewGroup.MarginLayoutParams params =
                    (ViewGroup.MarginLayoutParams) bottomNav.getLayoutParams();
            bottomPadding = bottomNav.getHeight()
                    + params.bottomMargin
                    + getResources().getDimensionPixelSize(R.dimen.bottom_nav_content_clearance);
        }

        fragmentContainer.setPadding(
                fragmentContainer.getPaddingLeft(),
                fragmentContainer.getPaddingTop(),
                fragmentContainer.getPaddingRight(),
                bottomPadding
        );
    }

    private boolean isCurrentTopLevelFragment(Fragment currentFragment, int destinationId) {
        if (currentFragment == null) {
            return false;
        }

        if (destinationId == R.id.nav_explore) {
            return currentFragment instanceof ExploreFragment;
        }

        if (destinationId == R.id.nav_tickets) {
            return currentFragment instanceof TicketsFragment;
        }

        if (destinationId == R.id.nav_organize) {
            return currentFragment instanceof OrganizeFragment;
        }

        if (destinationId == R.id.nav_notification) {
            return currentFragment instanceof NotificationFragment;
        }

        if (destinationId == R.id.nav_profile) {
            return currentFragment instanceof ProfileFragment;
        }

        return false;
    }

    private Fragment resolveTopLevelFragment(int destinationId) {
        if (destinationId == R.id.nav_explore) {
            return exploreFragment;
        }

        if (destinationId == R.id.nav_tickets) {
            return new TicketsFragment();
        }

        if (destinationId == R.id.nav_organize) {
            return organizeFragment;
        }

        if (destinationId == R.id.nav_notification) {
            return notificationFragment;
        }

        if (destinationId == R.id.nav_profile) {
            return profileFragment;
        }

        return null;
    }

    private void showFatalAuthError() {
        new AlertDialog.Builder(this)
                .setTitle("Authentication Error")
                .setMessage("Firebase authentication failed. The app will now close.")
                .setCancelable(false)
                .setPositiveButton("Close", (dialog, which) -> {
                    dialog.dismiss();
                    finish();
                })
                .show();
    }
}






