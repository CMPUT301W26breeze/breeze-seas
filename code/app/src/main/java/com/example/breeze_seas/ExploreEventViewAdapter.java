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

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textview.MaterialTextView;

import java.util.Calendar;
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
    public void onBindViewHolder(@NonNull ExploreEventViewAdapter.ExploreEventViewHolder holder, int position) {
        // assigning values to the views we created in layout file
        holder.eventTitle.setText(eventList.get(position).getName());

        // TODO: helper function to calculate time remaining
        //holder.eventTimeRemaining.setText(endsIn(eventList.get(position).getRegToMillis()))
        holder.eventTimeRemaining.setText("Closes in " + "1" + " day");

        // TODO: helper function to calculate ppl in waiting list
        //holder.eventWaitingListCount.setText(eventList.get(position).getWaitingList().count())
        holder.eventWaitingListCount.setText("0");

        // TODO: helper function to calculate ppl in event capacity
        //holder.eventCapacity.setText(eventList.get(position).getName());
        holder.eventCapacity.setText("60");

        // TODO: helper function to calculate chance of being drawn
        //holder.eventCapacity.setText(chanceCalc(eventList.get(position).getTotalParticipants()));
        holder.eventLuck.setText("100%");

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

    /**
     * Calculates the time remaining that the user has to join the event.
     * Available
     * @return String that describes how much time the user has before registration ends
     */
    private String endsIn(Date endTime) {
        // Get current time
        Date currentTime = Calendar.getInstance().getTime();

        // TODO: Calculate time left
        return "Closes in " + "1" + " day";
    }
}
