package com.example.breeze_seas;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
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

    private final ExploreFragment exploreFragment = new ExploreFragment();
    private final TicketsFragment ticketsFragment = new TicketsFragment();
    private final OrganizeFragment organizeFragment = new OrganizeFragment();
    private final NotificationFragment notificationFragment = new NotificationFragment();
    private final ProfileFragment profileFragment = new ProfileFragment();

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

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

        // Default tab
        if (savedInstanceState == null) {
            setCurrentFragment(exploreFragment);
            bottomNav.setSelectedItemId(R.id.nav_explore);
        }


        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_explore) setCurrentFragment(exploreFragment);
            else if (id == R.id.nav_tickets) setCurrentFragment(ticketsFragment);
            else if (id == R.id.nav_organize) setCurrentFragment(organizeFragment);
            else if (id == R.id.nav_notification) setCurrentFragment(notificationFragment);
            else if (id == R.id.nav_profile) setCurrentFragment(profileFragment);
            return true;
        });
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

