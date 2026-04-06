package com.example.breeze_seas;

import android.view.View;

/**
 * Callback used by RecyclerView rows that need to report a clicked view and adapter position.
 */
public interface RecyclerViewClickListener {
    // Source - https://stackoverflow.com/a/28304517
    public void recyclerViewListClicked(View v, int position);
}
