package com.example.breeze_seas;

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

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class OrganizeFragment extends Fragment {

    private final List<Event> events = new ArrayList<>();
    private EventAdapter adapter;

    public OrganizeFragment() {
        super(R.layout.fragment_organize);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView rv = view.findViewById(R.id.rvMyEvents);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new EventAdapter(events);
        rv.setAdapter(adapter);

        loadEvents();

        FloatingActionButton fab = view.findViewById(R.id.fabCreateEvent);
        fab.setOnClickListener(v ->
                ((MainActivity) requireActivity()).openSecondaryFragment(new CreateEventFragment())
        );

        TextInputLayout til = view.findViewById(R.id.tilSearch);
        til.setEndIconOnClickListener(v ->
                Toast.makeText(requireContext(), "Notifications (TODO)", Toast.LENGTH_SHORT).show()
        );

        view.findViewById(R.id.btnScanQr).setOnClickListener(v -> {
            View bottomNav = requireActivity().findViewById(R.id.bottom_navigation);

            if (bottomNav == null) {
                Toast.makeText(requireContext(), "Bottom navigation not found", Toast.LENGTH_SHORT).show();
                return;
            }

            Toast.makeText(requireContext(), "Scan QR screen not wired yet", Toast.LENGTH_SHORT).show();

            // bottomNav.setSelectedItemId(R.id.nav_scan);
        });

        view.findViewById(R.id.btnFilter).setOnClickListener(v ->
                ((MainActivity) requireActivity()).openSecondaryFragment(new FilterFragment())
        );
    }

    @Override
    public void onResume() {
        super.onResume();
        loadEvents();
    }

    private void loadEvents() {
        EventDB.getInstance().getAllEvents(new EventDB.LoadEventsCallback() {
            @Override
            public void onSuccess(List<Event> loadedEvents) {
                events.clear();
                events.addAll(loadedEvents);
                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(requireContext(), "Failed to load events", Toast.LENGTH_SHORT).show();
            }
        });
    }

    static class EventAdapter extends RecyclerView.Adapter<EventAdapter.VH> {
        private final List<Event> data;

        EventAdapter(List<Event> data) {
            this.data = data;
        }

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
            Event e = data.get(position);

            holder.tvName.setText(e.getName());

            SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy", Locale.US);
            String from = sdf.format(new Date(e.getRegFromMillis()));
            String to = sdf.format(new Date(e.getRegToMillis()));
            holder.tvDates.setText("Reg: " + from + " → " + to);

            Integer cap = e.getWaitingListCap();
            holder.tvCap.setText(cap == null
                    ? "Waiting list cap: Unlimited"
                    : "Waiting list cap: " + cap);
        }

        @Override
        public int getItemCount() {
            return data.size();
        }
    }
}
