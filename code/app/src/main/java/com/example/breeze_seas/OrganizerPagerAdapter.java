package com.example.breeze_seas;


import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import com.example.breeze_seas.AcceptedListFragment;
import com.example.breeze_seas.CancelledListFragment;
import com.example.breeze_seas.PendingListFragment;
import com.example.breeze_seas.WaitingListFragment;



public class OrganizerPagerAdapter extends FragmentStateAdapter {
    public OrganizerPagerAdapter(@NonNull Fragment fragment) {
        super(fragment);
    }
    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0: return new WaitingListFragment();
            case 1: return new PendingListFragment();
            case 2: return new AcceptedListFragment();
            case 3: return new CancelledListFragment();
            default: return new WaitingListFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 4;
    }

}

