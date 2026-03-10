package com.example.breeze_seas;

import static androidx.fragment.app.FragmentManagerKt.commit;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

/**
 * WelcomeScreenFragment is a {@link Fragment} subclass.
 * This fragment is displayed upon a user first opening up the app.
 * Contains a button to lead to the SignUpFragment.
 */
public class WelcomeScreenFragment extends Fragment {

    public WelcomeScreenFragment() {
        super(R.layout.fragment_welcome_screen);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Bind Started Button
        Button startButton = view.findViewById(R.id.welcomeScreenStartButton);

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SignUpFragment signUpFragment = new SignUpFragment();
                signUpFragment.setArguments(getArguments());  // forward arguments to Sign Up Page

                // Switch to sign up page
                getActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, signUpFragment)
                        .commit();

            }
        });
    }
}
