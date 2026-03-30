package com.example.breeze_seas;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

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

    private final List<Event> eventList;
    private final OnEventClickListener listener;
    private final OnEventDeleteListener deleteListener;

    /**
     * Constructs a new AdminBrowseEventsAdapter.
     */
    public AdminBrowseEventsAdapter(List<Event> eventList, OnEventClickListener listener, OnEventDeleteListener deleteListener) {
        this.eventList = eventList;
        this.listener = listener;
        this.deleteListener = deleteListener;
    }

    /**
     * Removes an event from the list and refreshes the view.
     *
     * @param event The event to remove.
     */
    public void removeEvent(Event event) {
        int pos = eventList.indexOf(event);
        if (pos != -1) {
            eventList.remove(pos);
            notifyItemRemoved(pos);
            notifyItemRangeChanged(pos, eventList.size());
        }
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
        Event event = eventList.get(position);

        // Event title
        holder.tvEventTitle.setText(event.getName());

        // Event details/description
        holder.tvEventDetails.setText(event.getDescription().isEmpty() ? "No details provided" : event.getDescription());

        // Format and show reg dates
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d", Locale.getDefault());
        String startDate = (event.getRegistrationStartTimestamp() != null)
                ? sdf.format(event.getRegistrationStartTimestamp().toDate())
                : "TBD";
        String endDate = (event.getRegistrationEndTimestamp() != null)
                ? sdf.format(event.getRegistrationEndTimestamp().toDate())
                : "TBD";

        holder.tvEventDates.setText("Reg: " + startDate + " → " + endDate);

        // capacity
        int cap = event.getWaitingListCapacity();
        String capText = (cap <= -1) ? "Unlimited" : String.valueOf(cap);
        holder.tvEventCapacity.setText("Waiting list cap: " + capText);

        // TODO: Image handling

        // item clicks to go to event details screen
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEventClick(event);
            }
        });

        // delete button
        holder.btnDelete.setOnClickListener(v -> {
            int currentPosition = holder.getBindingAdapterPosition();
            if (currentPosition != RecyclerView.NO_POSITION && deleteListener != null) {
                deleteListener.onEventDelete(eventList.get(currentPosition));
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
        return eventList.size();
    }

    /**
     * Holds references to the UI components for a single event row to avoid repeated findViewById calls.
     */
    public static class EventViewHolder extends RecyclerView.ViewHolder {
        ImageView ivEventPoster;
        TextView tvEventTitle;
        TextView tvEventDates;
        TextView tvEventCapacity;
        TextView tvEventDetails;
        ImageView btnDelete;

        /**
         * Constructor for EventViewHolder.
         */
        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            ivEventPoster = itemView.findViewById(R.id.iae_iv_event_poster);
            tvEventTitle = itemView.findViewById(R.id.iae_tv_event_title);
            tvEventDates = itemView.findViewById(R.id.iae_tv_event_dates);
            tvEventCapacity = itemView.findViewById(R.id.iae_tv_event_capacity);
            tvEventDetails = itemView.findViewById(R.id.iae_tv_event_details);
            btnDelete = itemView.findViewById(R.id.iae_btn_delete_event);
        }
    }
}