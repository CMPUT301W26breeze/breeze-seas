package com.example.breeze_seas;


import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import com.example.breeze_seas.AcceptedListFragment;
import com.example.breeze_seas.CancelledListFragment;
import com.example.breeze_seas.PendingListFragment;
import com.example.breeze_seas.WaitingListFragment;



public class OrganizerPagerAdapter extends FragmentStateAdapter {

    private final Fragment hostFragment;
    public OrganizerPagerAdapter(@NonNull Fragment fragment) {

        super(fragment);
        this.hostFragment=fragment;
    }
    @NonNull
    @Override
    public Fragment createFragment(int position) {
        String eventId = "";
        int capacity = 0;


        if (hostFragment.getArguments() != null) {
            eventId = hostFragment.getArguments().getString("EVENT_ID", "");
            capacity = hostFragment.getArguments().getInt("CAPACITY", 0);
        }


        switch (position) {
            case 0: return WaitingListFragment.newInstance(eventId, capacity);
            case 1: return PendingListFragment.newInstance(eventId, capacity);
            case 2: return AcceptedListFragment.newInstance(eventId, capacity);
            case 3: return CancelledListFragment.newInstance(eventId, capacity);
            default: return WaitingListFragment.newInstance(eventId, capacity);
        }
    }

    @Override
    public int getItemCount() {
        return 4;
    }

}

