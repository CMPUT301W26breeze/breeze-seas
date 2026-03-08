package com.example.breeze_seas;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class AdminBrowseEventsAdapter extends RecyclerView.Adapter<AdminBrowseEventsAdapter.EventViewHolder> {

    public static class EventItem {
        public String title;
        public String organizer;

        public EventItem(String title, String organizer) {
            this.title = title;
            this.organizer = organizer;
        }
    }

    private List<EventItem> eventList;

    public AdminBrowseEventsAdapter(List<EventItem> eventList) {
        this.eventList = eventList;
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_event, parent, false);

        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        EventItem event = eventList.get(position);

        holder.tvEventTitle.setText(event.title);
        holder.tvEventOrganizer.setText(event.organizer);

        holder.btnDelete.setOnClickListener(v -> {
            int currentPosition = holder.getBindingAdapterPosition();

            if (currentPosition != RecyclerView.NO_POSITION) {
                eventList.remove(currentPosition);
                notifyItemRemoved(currentPosition);
                notifyItemRangeChanged(currentPosition, eventList.size());
            }
        });
    }

    @Override
    public int getItemCount() {
        return eventList.size();
    }

    public static class EventViewHolder extends RecyclerView.ViewHolder {
        TextView tvEventTitle;
        TextView tvEventOrganizer;
        ImageView btnDelete;

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEventTitle = itemView.findViewById(R.id.abe_tv_event_title);
            tvEventOrganizer = itemView.findViewById(R.id.abe_tv_event_organizer);
            btnDelete = itemView.findViewById(R.id.abe_btn_delete_event);
        }
    }
}