package com.example.breeze_seas;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
/**
 * TicketsFragment is the top-level Tickets destination displayed in bottom navigation.
 *
 * <p>Current state:
 * - Hosts the tab shell for {@link TicketDB}-backed ticket fragments.
 */
public class TicketsFragment extends Fragment {
    // The tab switching implementation in this fragment was developed with Gemini,
    // "How to implement TabLayout with ViewPager2 in Android using Java", 2026-03-03.
    private ViewPager2 viewPager;
    private TabLayoutMediator tabLayoutMediator;

    public TicketsFragment() {
        super(R.layout.fragment_tickets);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TabLayout tabLayout = view.findViewById(R.id.tickets_tab_layout);
        viewPager = view.findViewById(R.id.tickets_view_pager);

        viewPager.setAdapter(new TicketsPagerAdapter(this));

        tabLayoutMediator = new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            if (position == 0) tab.setText("Active");
            else if (position == 1) tab.setText("Attending");
            else tab.setText("Past Events");
        });
        tabLayoutMediator.attach();
    }

    @Override
    public void onDestroyView() {
        if (tabLayoutMediator != null) {
            tabLayoutMediator.detach();
            tabLayoutMediator = null;
        }

        if (viewPager != null) {
            viewPager.setAdapter(null);
            viewPager = null;
        }

        super.onDestroyView();
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
