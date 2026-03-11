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
        if (hostFragment.getArguments() != null) {
            eventId = hostFragment.getArguments().getString("EVENT_ID");
        }

        if (position == 0) {
            return WaitingListFragment.newInstance(eventId, 0);
        } else if (position==1) {
            return PendingListFragment.newInstance(eventId, 0);
        } else if (position==2) {
            return AcceptedListFragment.newInstance(eventId,0);
        }
        else if (position==3){
            return CancelledListFragment.newInstance(eventId, 0);
        }
        else{
            return WaitingListFragment.newInstance(eventId, 0);
        }

    }

    @Override
    public int getItemCount() {
        return 4;
    }

}

