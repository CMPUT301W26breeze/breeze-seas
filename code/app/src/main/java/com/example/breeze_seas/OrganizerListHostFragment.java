package com.example.breeze_seas;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.example.breeze_seas.OrganizerPagerAdapter;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class OrganizerListHostFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle saved) {

        View view = inflater.inflate(R.layout.fragment_organizer_list_host, container, false);
        TabLayout tabLayout = view.findViewById(R.id.waiting_tabs_list);
        ViewPager2 viewPager = view.findViewById(R.id.organizer_view_pager);
        OrganizerPagerAdapter adapter = new OrganizerPagerAdapter(this);
        viewPager.setAdapter(adapter);
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0: tab.setText("Waiting"); break;
                case 1: tab.setText("Pending"); break;
                case 2: tab.setText("Accepted"); break;
                case 3: tab.setText("Cancelled"); break;
            }
        }).attach();
        return view;
    }
}
