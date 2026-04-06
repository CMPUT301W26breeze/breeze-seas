package com.example.breeze_seas;

import android.os.Bundle;
import android.widget.TextView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

public class OrganizerListHostFragment extends Fragment {
    private static final String STATE_SELECTED_TAB = "state_selected_tab";

    private ViewPager2 viewPager;
    private TextView[] tabButtons;
    private ViewPager2.OnPageChangeCallback pageChangeCallback;
    private int selectedTabIndex;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle saved) {
        View view = inflater.inflate(R.layout.fragment_organizer_list_host, container, false);

        viewPager = view.findViewById(R.id.organizer_view_pager);
        OrganizerPagerAdapter adapter = new OrganizerPagerAdapter(this);
        viewPager.setAdapter(adapter);
        viewPager.setOffscreenPageLimit(3);

        view.findViewById(R.id.organizer_lists_back).setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack()
        );

        tabButtons = new TextView[] {
                view.findViewById(R.id.organizer_tab_waiting),
                view.findViewById(R.id.organizer_tab_pending),
                view.findViewById(R.id.organizer_tab_accepted),
                view.findViewById(R.id.organizer_tab_declined)
        };

        for (int i = 0; i < tabButtons.length; i++) {
            final int tabIndex = i;
            tabButtons[i].setOnClickListener(v -> selectTab(tabIndex, true));
        }

        pageChangeCallback = new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateTabSelection(position);
            }
        };
        viewPager.registerOnPageChangeCallback(pageChangeCallback);

        selectedTabIndex = saved != null ? saved.getInt(STATE_SELECTED_TAB, 0) : 0;
        updateTabSelection(selectedTabIndex);
        viewPager.setCurrentItem(selectedTabIndex, false);

        return view;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_SELECTED_TAB, selectedTabIndex);
    }

    @Override
    public void onDestroyView() {
        if (viewPager != null) {
            if (pageChangeCallback != null) {
                viewPager.unregisterOnPageChangeCallback(pageChangeCallback);
            }
            viewPager.setAdapter(null);
            viewPager = null;
        }
        pageChangeCallback = null;
        tabButtons = null;
        super.onDestroyView();
    }

    private void selectTab(int index, boolean smoothScroll) {
        if (viewPager != null) {
            selectedTabIndex = index;
            viewPager.setCurrentItem(index, smoothScroll);
        }
    }

    private void updateTabSelection(int selectedIndex) {
        selectedTabIndex = selectedIndex;
        if (tabButtons == null) return;

        for (int i = 0; i < tabButtons.length; i++) {
            boolean isSelected = (i == selectedIndex);
            tabButtons[i].setActivated(isSelected);
            tabButtons[i].setSelected(isSelected);
        }
    }
}