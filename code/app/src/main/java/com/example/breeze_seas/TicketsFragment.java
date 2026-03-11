package com.example.breeze_seas;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
/**
 * TicketsFragment is the top-level Tickets destination displayed in bottom navigation.
 *
 * <p>Current state:
 * - Hosts the tab shell for {@link TicketDB}-backed ticket fragments.
 */
public class TicketsFragment extends Fragment {
    private static final String STATE_SELECTED_TAB = "state_selected_tab";

    private ViewPager2 viewPager;
    private TextView[] tabButtons;
    private ViewPager2.OnPageChangeCallback pageChangeCallback;
    private int selectedTabIndex;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        View view = inflater.inflate(R.layout.fragment_tickets, container, false);

        viewPager = view.findViewById(R.id.tickets_view_pager);
        tabButtons = new TextView[] {
                view.findViewById(R.id.tickets_tab_active),
                view.findViewById(R.id.tickets_tab_attending),
                view.findViewById(R.id.tickets_tab_past)
        };

        viewPager.setAdapter(new TicketsPagerAdapter(this));
        viewPager.setOffscreenPageLimit(3);

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

        selectedTabIndex = savedInstanceState != null
                ? savedInstanceState.getInt(STATE_SELECTED_TAB, 0)
                : 0;
        selectTab(selectedTabIndex, false);

        return view;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_SELECTED_TAB, selectedTabIndex);
    }

    @Override
    public void onDestroyView() {
        if (viewPager != null && pageChangeCallback != null) {
            viewPager.unregisterOnPageChangeCallback(pageChangeCallback);
            pageChangeCallback = null;
        }

        if (viewPager != null) {
            viewPager.setAdapter(null);
            viewPager = null;
        }

        tabButtons = null;

        super.onDestroyView();
    }

    private void selectTab(int index, boolean smoothScroll) {
        if (viewPager == null) {
            return;
        }

        selectedTabIndex = index;
        updateTabSelection(index);

        if (viewPager.getCurrentItem() != index) {
            viewPager.setCurrentItem(index, smoothScroll);
        }
    }

    private void updateTabSelection(int selectedIndex) {
        selectedTabIndex = selectedIndex;

        if (tabButtons == null) {
            return;
        }

        for (int i = 0; i < tabButtons.length; i++) {
            boolean isSelected = i == selectedIndex;
            tabButtons[i].setActivated(isSelected);
            tabButtons[i].setSelected(isSelected);
        }
    }

    private static class TicketsPagerAdapter extends FragmentStateAdapter {

        public TicketsPagerAdapter(@NonNull Fragment fragment) {
            super(fragment);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            if (position == 0) return new ActiveTicketsFragment();
            if (position == 1) return new AttendingTicketsFragment();
            return new PastTicketsFragment();
        }

        @Override
        public int getItemCount() {
            return 3;
        }
    }
}
