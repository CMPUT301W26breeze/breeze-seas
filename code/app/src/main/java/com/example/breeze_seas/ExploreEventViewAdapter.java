package com.example.breeze_seas;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class ExploreEventViewAdapter extends RecyclerView.Adapter<ExploreEventViewAdapter.ExploreEventViewHolder> {
    private final Context context;
    private final List<Event> eventList;
    private static RecyclerViewClickListener itemListener;

    public ExploreEventViewAdapter(Context context, RecyclerViewClickListener itemListener, List<Event> eventList) {
        this.context = context;
        this.eventList = eventList;
        ExploreEventViewAdapter.itemListener = itemListener;
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
        holder.eventDescription.setText(event.getDetails().trim().isEmpty()
                ? holder.itemView.getContext().getString(R.string.explore_card_no_description)
                : event.getDetails());
        holder.eventTimeRemaining.setText(holder.itemView.getContext().getString(
                R.string.explore_card_registration_closes,
                formatDate(event.getRegToMillis())
        ));
        holder.eventWaitingListCount.setText(R.string.explore_card_lottery);
        holder.eventCapacity.setText(event.getWaitingListCap() == null
                ? holder.itemView.getContext().getString(R.string.explore_card_cap_unlimited)
                : holder.itemView.getContext().getString(R.string.explore_card_cap_limited, event.getWaitingListCap()));
        holder.eventLuck.setText(event.isGeoRequired()
                ? holder.itemView.getContext().getString(R.string.explore_card_geo_required)
                : holder.itemView.getContext().getString(R.string.explore_card_geo_optional));

        holder.eventImage.setImageResource(R.drawable.ic_image_placeholder);
        if (event.getPosterUriString() != null && !event.getPosterUriString().trim().isEmpty()) {
            try {
                holder.eventImage.setImageURI(Uri.parse(event.getPosterUriString()));
            } catch (Exception ignored) {
                holder.eventImage.setImageResource(R.drawable.ic_image_placeholder);
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
            eventLuck = itemView.findViewById(R.id.explore_event_entry_luck_percent);
            eventCard = itemView.findViewById(R.id.explore_event_entry_card);
        }

        @Override
        public void onClick(View v) {
            itemListener.recyclerViewListClicked(v, this.getAbsoluteAdapterPosition());
        }
    }

    private String formatDate(long millis) {
        if (millis <= 0L) {
            return context.getString(R.string.organizer_event_preview_not_set);
        }

        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(new Date(millis));
    }
}
