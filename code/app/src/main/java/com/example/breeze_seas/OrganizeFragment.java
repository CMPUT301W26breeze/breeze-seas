package com.example.breeze_seas;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;

public class OrganizeFragment extends Fragment {

    private final List<EventRow> events = new ArrayList<>();
    private EventRowAdapter adapter;

    public OrganizeFragment() {
        super(R.layout.fragment_organize);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView rv = view.findViewById(R.id.rvMyEvents);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new EventRowAdapter(events);
        rv.setAdapter(adapter);

        events.clear();
        events.add(new EventRow("CMPUT 301 Meetup", "Mar 5, 2026", "Mar 12, 2026", null));
        events.add(new EventRow("Hack Night", "Mar 8, 2026", "Mar 10, 2026", 50));
        adapter.notifyDataSetChanged();

        FloatingActionButton fab = view.findViewById(R.id.fabCreateEvent);
        fab.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), CreateEventActivity.class))
        );

        TextInputLayout til = view.findViewById(R.id.tilSearch);
        til.setEndIconOnClickListener(v ->
                Toast.makeText(requireContext(), "Notifications (TODO)", Toast.LENGTH_SHORT).show()
        );

        view.findViewById(R.id.btnScanQr).setOnClickListener(v -> {
            BottomNavigationView bottomNav = findBottomNav(requireActivity().getWindow().getDecorView());
            if (bottomNav == null) {
                Toast.makeText(requireContext(), "Bottom nav not found", Toast.LENGTH_SHORT).show();
                return;
            }

            int scanItemId = findMenuItemIdByTitle(bottomNav.getMenu(), "Scan"); // change keyword if needed
            if (scanItemId == 0) {
                Toast.makeText(requireContext(), "Scan tab not found (check menu title)", Toast.LENGTH_SHORT).show();
                return;
            }

            bottomNav.setSelectedItemId(scanItemId);
        });

        view.findViewById(R.id.btnFilter).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), FilterActivity.class))
        );
    }

    static class EventRow {
        String name, from, to;
        Integer cap;
        EventRow(String name, String from, String to, Integer cap) {
            this.name = name;
            this.from = from;
            this.to = to;
            this.cap = cap;
        }
    }

    static class EventRowAdapter extends RecyclerView.Adapter<EventRowAdapter.VH> {
        private final List<EventRow> data;
        EventRowAdapter(List<EventRow> data) { this.data = data; }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvName, tvDates, tvCap;
            VH(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tvEventName);
                tvDates = itemView.findViewById(R.id.tvEventDates);
                tvCap = itemView.findViewById(R.id.tvEventCapacity);
            }
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_event, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            EventRow e = data.get(position);
            holder.tvName.setText(e.name);
            holder.tvDates.setText("Reg: " + e.from + " → " + e.to);
            holder.tvCap.setText(e.cap == null
                    ? "Waiting list cap: Unlimited"
                    : "Waiting list cap: " + e.cap);
        }

        @Override
        public int getItemCount() { return data.size(); }
    }

    private BottomNavigationView findBottomNav(View root) {
        if (root instanceof BottomNavigationView) {
            return (BottomNavigationView) root;
        }
        if (root instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) root;
            for (int i = 0; i < vg.getChildCount(); i++) {
                BottomNavigationView found = findBottomNav(vg.getChildAt(i));
                if (found != null) return found;
            }
        }
        return null;
    }

    private int findMenuItemIdByTitle(android.view.Menu menu, String titleKeyword) {
        if (menu == null) return 0;
        for (int i = 0; i < menu.size(); i++) {
            android.view.MenuItem item = menu.getItem(i);
            CharSequence t = item.getTitle();
            if (t != null && t.toString().toLowerCase().contains(titleKeyword.toLowerCase())) {
                return item.getItemId();
            }
        }
        return 0;
    }
}