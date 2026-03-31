package com.example.breeze_seas;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Adapter for the RecyclerView in the AdminBrowseEvents screen.
 * Binds a list of {@link Event} objects to corresponding UI elements,
 * including event details, dates, and capacities.
 * Also handles user interactions such as clicking on an event to view details
 * or clicking the delete button to remove an event.
 */
public class AdminBrowseEventsAdapter extends RecyclerView.Adapter<AdminBrowseEventsAdapter.EventViewHolder> {

    /**
     * Interface definition for a callback to be invoked when an event item is clicked.
     */
    public interface OnEventClickListener {
        /**
         * Called when an event in the list has been clicked.
         */
        void onEventClick(Event event);
    }

    /**
     * Interface definition for a callback to be invoked when an event's delete button is pressed.
     */
    public interface OnEventDeleteListener {
        void onEventDelete(Event event);
    }

    private final List<Event> fullList;     // all events from LiveData
    private final List<Event> filteredList; // subset shown after search filter
    private final OnEventClickListener listener;
    private final OnEventDeleteListener deleteListener;

    /**
     * Constructs a new AdminBrowseEventsAdapter.
     */
    public AdminBrowseEventsAdapter(List<Event> eventList, OnEventClickListener listener, OnEventDeleteListener deleteListener) {
        this.fullList = eventList;
        this.filteredList = new ArrayList<>(eventList);
        this.listener = listener;
        this.deleteListener = deleteListener;
    }

    /**
     * Replaces the full event list from LiveData, sorts newest first, then reapplies the
     * current search filter. Called by the fragment whenever the LiveData updates.
     *
     * @param newEvents The updated list of events.
     */
    public void setEvents(List<Event> newEvents) {
        fullList.clear();
        fullList.addAll(newEvents);
        // Sort newest first by createdTimestamp; events without a timestamp go to the end
        fullList.sort((a, b) -> {
            if (a.getCreatedTimestamp() == null) return 1;
            if (b.getCreatedTimestamp() == null) return -1;
            return b.getCreatedTimestamp().compareTo(a.getCreatedTimestamp());
        });
        applyFilter(currentQuery);
    }

    private String currentQuery = "";

    /**
     * Filters the displayed list to events whose name or description contains the query.
     * Reapplies automatically after every LiveData update via {@link #setEvents}.
     *
     * @param query The search string (case-insensitive).
     */
    public void filter(String query) {
        currentQuery = query;
        applyFilter(query);
    }

    private void applyFilter(String query) {
        filteredList.clear();
        if (query == null || query.trim().isEmpty()) {
            filteredList.addAll(fullList);
        } else {
            String lower = query.trim().toLowerCase();
            for (Event event : fullList) {
                String name = event.getName() != null ? event.getName().toLowerCase() : "";
                String desc = event.getDescription() != null ? event.getDescription().toLowerCase() : "";
                if (name.contains(lower) || desc.contains(lower)) {
                    filteredList.add(event);
                }
            }
        }
        notifyDataSetChanged();
    }

    /**
     * Called when the RecyclerView needs a new {@link EventViewHolder} of the given type to represent an item.
     * Inflates the layout resource.
     *
     * @return A new EventViewHolder that holds a View of the given view type.
     */
    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_event, parent, false);

        return new EventViewHolder(view);
    }

    /**
     * Called by the RecyclerView to display the data at the specified position.
     * This method updates the contents of the {@link EventViewHolder} to reflect the item at the given position.
     */
    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event event = filteredList.get(position);

        holder.tvEventTitle.setText(event.getName());

        holder.tvEventDetails.setText(event.getDescription().trim().isEmpty()
                ? "No details provided" : event.getDescription());

        // Registration closes pill
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
        String endDate = (event.getRegistrationEndTimestamp() != null)
                ? sdf.format(event.getRegistrationEndTimestamp().toDate())
                : "TBD";
        holder.tvEventDates.setText("Registration closes " + endDate);

        // Capacity pill
        int cap = event.getWaitingListCapacity();
        holder.tvEventCapacity.setText(cap <= -1 ? "Unlimited waitlist" : cap + " waitlist spots");

        // Geo pill
        holder.tvEventGeo.setText(event.isGeolocationEnforced() ? "Geo required" : "Geo optional");

        // Image
        holder.ivImageFrame.setVisibility(View.GONE);
        holder.ivEventPoster.setImageResource(R.drawable.ic_image_placeholder);
        if (event.getImage() != null && !Objects.equals(event.getImage().getCompressedBase64(), "")) {
            try {
                holder.ivEventPoster.setImageTintList(null);
                holder.ivEventPoster.setImageBitmap(event.getImage().display());
                holder.ivImageFrame.setVisibility(View.VISIBLE);
            } catch (Exception ignored) {
                holder.ivImageFrame.setVisibility(View.GONE);
            }
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onEventClick(event);
        });

        holder.btnDelete.setOnClickListener(v -> {
            int currentPosition = holder.getBindingAdapterPosition();
            if (currentPosition != RecyclerView.NO_POSITION && deleteListener != null) {
                deleteListener.onEventDelete(filteredList.get(currentPosition));
            }
        });
    }

    /**
     * Returns the total number of items in the data set held by the adapter.
     *
     * @return Total number of items in adapter.
     */
    @Override
    public int getItemCount() {
        return filteredList.size();
    }

    /**
     * Holds references to the UI components for a single event row to avoid repeated findViewById calls.
     */
    public static class EventViewHolder extends RecyclerView.ViewHolder {
        FrameLayout ivImageFrame;
        ImageView ivEventPoster;
        TextView tvEventTitle;
        TextView tvEventDates;
        TextView tvEventCapacity;
        TextView tvEventGeo;
        TextView tvEventDetails;
        ImageView btnDelete;

        /**
         * Constructor for EventViewHolder.
         */
        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImageFrame = itemView.findViewById(R.id.iae_iv_image_frame);
            ivEventPoster = itemView.findViewById(R.id.iae_iv_event_poster);
            tvEventTitle = itemView.findViewById(R.id.iae_tv_event_title);
            tvEventDates = itemView.findViewById(R.id.iae_tv_event_dates);
            tvEventCapacity = itemView.findViewById(R.id.iae_tv_event_capacity);
            tvEventGeo = itemView.findViewById(R.id.iae_tv_event_geo);
            tvEventDetails = itemView.findViewById(R.id.iae_tv_event_details);
            btnDelete = itemView.findViewById(R.id.iae_btn_delete_event);
        }
    }
}