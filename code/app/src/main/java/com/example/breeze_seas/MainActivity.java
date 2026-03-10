package com.example.breeze_seas;

import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.lang.reflect.Method;

/*** MainActivity is the container for the app's main navigation.
 *
 * <p>Role in Architecture:* - Serves as a single-activity shell that switches top-level Fragments using a
 *
 * <p>Outstanding/Future Work:* - Replace placeholder fragments with actual feature panels.
 * - Consider using the Navigation Component for back-stack consistency once.
 * Flows grow increasingly complex.
 * - Confirm edge-to-edge inset behaviour across devices and confirm that the bottom
 *The navigation background is consistent with the theme.
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private final ExploreFragment exploreFragment = new ExploreFragment();
    private final TicketsFragment ticketsFragment = new TicketsFragment();
    private final OrganizeFragment organizeFragment = new OrganizeFragment();
    private final NotificationFragment notificationFragment = new NotificationFragment();
    private final ProfileFragment profileFragment = new ProfileFragment();
    private String androidID;

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


        // Get android ID
        this.androidID = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        FirebaseSession.ensureAuthenticated(new FirebaseSession.OnReadyListener() {
            @Override
            public void onReady() {
                UserDB userDBInstance = new UserDB();
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
            public void onError(Exception e) {
                Log.e(TAG, "Firebase auth bootstrap failed. Falling back to guest flow.", e);
                initializeUI(savedInstanceState, null);
            }
        });

    }

    /**
     * Determines which fragments should be loaded on start. This method is called by
     * the callback function after the firebase query.
     *
     * @param savedInstanceState Values pertaining to the current activity
     * @param user User class object, may be null if user does not exist for current device.
     */
    private void initializeUI(Bundle savedInstanceState, User user) {
        // Bind and setup bottom navigation bar
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_explore) setCurrentFragment(exploreFragment);
            else if (id == R.id.nav_tickets) setCurrentFragment(ticketsFragment);
            else if (id == R.id.nav_organize) setCurrentFragment(organizeFragment);
            else if (id == R.id.nav_notification) setCurrentFragment(notificationFragment);
            else if (id == R.id.nav_profile) {
                Bundle profileArgs = new Bundle();
                profileArgs.putString("deviceId", this.androidID);
                profileFragment.setArguments(profileArgs);
                setCurrentFragment(profileFragment);
            }
            return true;
        });

        //  user not found / login == false
        if (user == null) {
            // Hide bar
            showBottomNav(false);

            // Compose fragment and attach androidID info
            Bundle args = new Bundle();
            args.putString("androidID", this.androidID);
            WelcomeScreenFragment welcomeScreenFragment = new WelcomeScreenFragment();
            welcomeScreenFragment.setArguments(args);
            setCurrentFragment(welcomeScreenFragment);
        }
        // Default tab
        // TODO: Transfer user data to exploreFragment
        else {
            showBottomNav(true);
            if (savedInstanceState == null
                    || getSupportFragmentManager().findFragmentById(R.id.fragment_container) == null) {
                setCurrentFragment(exploreFragment);
                bottomNav.setSelectedItemId(R.id.nav_explore);
            }
        }

    }

    /**
     * Helper wrapper function for controlling visibility of the navigation bar.
     * Allows external control of the navigation bar. For example, being called from other fragments.
     * @param show Boolean value, true to display the bottomNavigation
     */
    public void showBottomNav(boolean show) {
        // TODO: Cleaner implementation? Check SignUpFragment.java
        // Bind
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        if (show) {
            bottomNav.setVisibility(View.VISIBLE);
        } else {
            bottomNav.setVisibility(View.GONE);
        }
    }


    /*** Replaces the existing fragment in the main fragment container.
     *
     * <p>Note: This use {@code replace()} without adding to the back stack since bottom* Navigation is designed to change top-level destinations rather than generate a
     * The back stack is deep.
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
}
