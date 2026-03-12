package com.example.breeze_seas;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

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
        adapter = new EventAdapter(events, event ->
                ((MainActivity) requireActivity()).openSecondaryFragment(
                        OrganizerEventPreviewFragment.newInstance(event.getId())
                )
        );
        rv.setAdapter(adapter);

        loadEvents();

        View createButton = view.findViewById(R.id.fabCreateEvent);
        createButton.setOnClickListener(v ->
                ((MainActivity) requireActivity()).openSecondaryFragment(new CreateEventFragment())
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
        interface OnEventClickListener {
            void onEventClick(Event event);
        }

        private final List<Event> data;
        private final OnEventClickListener onEventClickListener;

        EventAdapter(List<Event> data, OnEventClickListener onEventClickListener) {
            this.data = data;
            this.onEventClickListener = onEventClickListener;
        }

        static class VH extends RecyclerView.ViewHolder {
            ImageView ivPoster;
            TextView tvName, tvDates, tvCap, tvDetails, tvAction;

            VH(@NonNull View itemView) {
                super(itemView);
                ivPoster = itemView.findViewById(R.id.ivEventPoster);
                tvName = itemView.findViewById(R.id.tvEventName);
                tvDates = itemView.findViewById(R.id.tvEventDates);
                tvCap = itemView.findViewById(R.id.tvEventCapacity);
                tvDetails = itemView.findViewById(R.id.tvEventDetails);
                tvAction = itemView.findViewById(R.id.tvEventAction);
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

            holder.ivPoster.setImageResource(R.drawable.ic_image_placeholder);
            if (e.getPosterUriString() != null && !e.getPosterUriString().trim().isEmpty()) {
                try {
                    holder.ivPoster.setImageURI(Uri.parse(e.getPosterUriString()));
                } catch (Exception ignored) {
                    holder.ivPoster.setImageResource(R.drawable.ic_image_placeholder);
                }
            }

            holder.tvName.setText(e.getName());

            SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy", Locale.US);
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            String from = sdf.format(new Date(e.getRegFromMillis()));
            String to = sdf.format(new Date(e.getRegToMillis()));
            holder.tvDates.setText("Reg: " + from + " → " + to);

            Integer cap = e.getWaitingListCap();
            holder.tvCap.setText(cap == null
                    ? "Waiting list cap: Unlimited"
                    : "Waiting list cap: " + cap);
            holder.tvDetails.setText(e.getDetails().trim().isEmpty()
                    ? holder.itemView.getContext().getString(R.string.organize_event_no_description)
                    : e.getDetails());
            holder.tvAction.setText(R.string.organize_event_open_preview);
            holder.itemView.setOnClickListener(v -> onEventClickListener.onEventClick(e));
        }

        @Override
        public int getItemCount() {
            return data.size();
        }
    }
}
