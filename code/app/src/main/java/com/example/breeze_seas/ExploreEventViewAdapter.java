package com.example.breeze_seas;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textview.MaterialTextView;

import java.util.List;
import java.util.Locale;

public class ExploreEventViewAdapter extends RecyclerView.Adapter<ExploreEventViewAdapter.ExploreEventViewHolder> {
    Context context;
    private List<Event> eventList;
    private static RecyclerViewClickListener itemListener;

    public ExploreEventViewAdapter(Context context, RecyclerViewClickListener itemListener, List<Event> eventList) {
        this.context = context;
        this.eventList = eventList;
        this.itemListener = itemListener;
    }

    @NonNull
    @Override
    public ExploreEventViewAdapter.ExploreEventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // inflating layouts, giving looks to rows
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.item_explore_event_entry, parent, false);

        return new ExploreEventViewAdapter.ExploreEventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ExploreEventViewAdapter.ExploreEventViewHolder holder, int position) {
        // assigning values to the views we created in layout file
        holder.eventTitle.setText(eventList.get(position).getName());

        // TODO: helper function to calculate time remaining
        //holder.eventTimeRemaining.setText(eventList.get(position).getRegToMillis())
        holder.eventTimeRemaining.setText("Closes in 1 day");

        // TODO: helper function to calculate ppl in waiting list
        //holder.eventWaitingListCount.setText(eventList.get(position).getWaitingList().count())
        holder.eventWaitingListCount.setText("60%");

        // TODO: helper function to calculate ppl in event capacity
        //holder.eventCapacity.setText(eventList.get(position).getName());
        holder.eventCapacity.setText("0");

        // TODO: helper function to calculate chance of being drawn
        //holder.eventCapacity.setText(chanceCalc(eventList.get(position).getTotalParticipants()));
        holder.eventLuck.setText("100%");

        // TODO: load images
        //holder.eventImage.setImageDrawable(eventList.get(position).getPosterUriString());
        holder.eventImage.setImageResource(R.drawable.ic_image_placeholder);

    }

    @Override
    public int getItemCount() {
        return eventList.size();
    }

    public static class ExploreEventViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        MaterialTextView eventTitle;
        MaterialTextView eventTimeRemaining;
        MaterialTextView eventWaitingListCount;
        MaterialTextView eventCapacity;
        MaterialTextView eventLuck;
        ShapeableImageView eventImage;
        MaterialCardView eventCard;

        public ExploreEventViewHolder(@NonNull View itemView) {
            // setup for viewholder (item/row)
            super(itemView);
            itemView.setOnClickListener(this);

            eventTitle = itemView.findViewById(R.id.explore_event_entry_title);
            eventTimeRemaining = itemView.findViewById(R.id.explore_event_entry_time_renaming);
            eventWaitingListCount = itemView.findViewById(R.id.explore_event_entry_waiting_list_count);
            eventCapacity = itemView.findViewById(R.id.explore_event_entry_capacity);
            eventImage = itemView.findViewById(R.id.explore_event_entry_image);
            eventLuck = itemView.findViewById(R.id.explore_event_entry_luck_percent);

            // Bind action upon clicking card
            eventCard = itemView.findViewById(R.id.explore_event_entry_card);

        }

        @Override
        public void onClick(View v) {
            itemListener.recyclerViewListClicked(v, this.getAbsoluteAdapterPosition());
        }
    }


    /**
     * Calculates and constructs a string representing the chance the user will win the lottery.
     * @param total_participants Integer describing all participants pertaining to the event
     * @return String that contains a percentage, describing chance of winning
     */
    private String chanceCalc(int total_participants) {
        // Assuming user isn't part of event
        total_participants = total_participants + 1;
        float percent = ((float) 1 / total_participants) * 100;
        if (percent < 10) {
            return String.format(Locale.US, "%.2f", percent) + "%";
        } else {  // no decimal precision for 10% or higher
            return String.format(Locale.US, "%.0f", percent) + "%";
        }
    }
}
