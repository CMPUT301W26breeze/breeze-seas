package com.example.breeze_seas;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class OrganizerDashboardActivity extends AppCompatActivity {

    private final List<EventRow> events = new ArrayList<>();
    private EventRowAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organizer_dashboard);

        RecyclerView rv = findViewById(R.id.rvMyEvents);
        FloatingActionButton fab = findViewById(R.id.fabCreateEvent);

        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new EventRowAdapter(events);
        rv.setAdapter(adapter);

        seedDummy();

        fab.setOnClickListener(v -> {
            Intent i = new Intent(OrganizerDashboardActivity.this, CreateEventActivity.class);
            startActivity(i);
        });
    }

    private void seedDummy() {
        events.clear();
        events.add(new EventRow("CMPUT 301 Meetup", "Mar 5, 2026", "Mar 12, 2026", null));
        events.add(new EventRow("Hack Night", "Mar 8, 2026", "Mar 10, 2026", 50));
        adapter.notifyDataSetChanged();
    }

    static class EventRow {
        String name;
        String regFrom;
        String regTo;
        Integer cap;

        EventRow(String name, String regFrom, String regTo, Integer cap) {
            this.name = name;
            this.regFrom = regFrom;
            this.regTo = regTo;
            this.cap = cap;
        }
    }

    static class EventRowAdapter extends RecyclerView.Adapter<EventRowAdapter.VH> {
        private final List<EventRow> data;

        EventRowAdapter(List<EventRow> data) {
            this.data = data;
        }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvName, tvDates, tvCapacity;

            VH(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tvEventName);
                tvDates = itemView.findViewById(R.id.tvEventDates);
                tvCapacity = itemView.findViewById(R.id.tvEventCapacity);
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
            holder.tvDates.setText("Reg: " + e.regFrom + " → " + e.regTo);
            holder.tvCapacity.setText(e.cap == null
                    ? "Waiting list cap: Unlimited"
                    : "Waiting list cap: " + e.cap);
        }

        @Override
        public int getItemCount() {
            return data.size();
        }
    }
}