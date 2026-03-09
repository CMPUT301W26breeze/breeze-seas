package com.example.breeze_seas;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class WelcomeScreenFragment extends Fragment {

    public WelcomeScreenFragment() {
        super(R.layout.fragment_welcome_screen);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Bind Started Button
        Button startButton = view.findViewById(R.id.welcomeScreenStartButton);

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                System.exit(0);
            }
        });

    }

}
