package com.example.breeze_seas;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;

import java.util.List;
import java.util.Objects;

public class ExploreEventViewAdapter extends RecyclerView.Adapter<ExploreEventViewAdapter.ExploreEventViewHolder> {
    private final Context context;
    private final List<Event> eventList;
    private static RecyclerViewClickListener itemListener;

    public ExploreEventViewAdapter(Context context, RecyclerViewClickListener itemListener, List<Event> eventList) {
        this.context = context;
        this.eventList = eventList;
        ExploreEventViewAdapter.itemListener = itemListener;
    }

    /**
     * Replaces the current Explore list contents.
     *
     * @param newEvents Events that should currently be visible in Explore.
     */
    public void submitList(@NonNull List<Event> newEvents) {
        eventList.clear();
        eventList.addAll(newEvents);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ExploreEventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.item_explore_event_entry, parent, false);
        return new ExploreEventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ExploreEventViewHolder holder, int position) {
        Event event = eventList.get(position);

        holder.eventTitle.setText(event.getName());
        holder.eventDescription.setText(event.getDescription().trim().isEmpty()
                ? holder.itemView.getContext().getString(R.string.explore_card_no_description)
                : event.getDescription());
        holder.eventTimeRemaining.setText(holder.itemView.getContext().getString(
                R.string.explore_card_registration_closes,
                formatDate(event.getRegistrationEndTimestamp())
        ));
        holder.eventWaitingListCount.setText(R.string.explore_card_lottery);
        holder.eventCapacity.setText(event.getWaitingListCap() == null
                ? holder.itemView.getContext().getString(R.string.explore_card_cap_unlimited)
                : holder.itemView.getContext().getString(R.string.explore_card_cap_limited, event.getWaitingListCap()));
        holder.eventLuck.setText(event.isGeolocationEnforced()
                ? holder.itemView.getContext().getString(R.string.explore_card_geo_required)
                : holder.itemView.getContext().getString(R.string.explore_card_geo_optional));

        // Handle Image
        holder.eventImage.setImageResource(R.drawable.ic_image_placeholder);
        holder.eventImage.setVisibility(View.GONE);
        holder.eventImageFrame.setVisibility(View.GONE);
        if (event.getImage() != null && !Objects.equals(event.getImage().getCompressedBase64(), "")) {
            try {
                // Show image if possible
                holder.eventImage.setImageBitmap(event.getImage().display());
                holder.eventImage.setVisibility(View.VISIBLE);
                holder.eventImageFrame.setVisibility(View.VISIBLE);
            } catch (Exception ignored) {
                // Hide image
                holder.eventImage.setImageResource(R.drawable.ic_image_placeholder);
                holder.eventImage.setVisibility(View.GONE);
                holder.eventImageFrame.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public int getItemCount() {
        return eventList.size();
    }

    static class ExploreEventViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView eventTitle;
        TextView eventDescription;
        TextView eventTimeRemaining;
        TextView eventWaitingListCount;
        TextView eventCapacity;
        TextView eventLuck;
        FrameLayout eventImageFrame;
        ImageView eventImage;
        LinearLayout eventCard;

        ExploreEventViewHolder(@NonNull View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);

            eventTitle = itemView.findViewById(R.id.explore_event_entry_title);
            eventDescription = itemView.findViewById(R.id.explore_event_entry_description);
            eventTimeRemaining = itemView.findViewById(R.id.explore_event_entry_time_renaming);
            eventWaitingListCount = itemView.findViewById(R.id.explore_event_entry_waiting_list_count);
            eventCapacity = itemView.findViewById(R.id.explore_event_entry_capacity);
            eventImage = itemView.findViewById(R.id.explore_event_entry_image);
            eventImageFrame = itemView.findViewById(R.id.explore_event_entry_image_frame);
            eventLuck = itemView.findViewById(R.id.explore_event_entry_luck_percent);
            eventCard = itemView.findViewById(R.id.explore_event_entry_card);
        }

        @Override
        public void onClick(View v) {
            itemListener.recyclerViewListClicked(v, this.getAbsoluteAdapterPosition());
        }
    }

    private String formatDate(Timestamp timestamp) {
        return EventMetadataUtils.formatDateTime(timestamp);
    }
}
