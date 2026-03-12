package com.example.breeze_seas;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class PastTicketsAdapter extends RecyclerView.Adapter<PastTicketsAdapter.PastTicketViewHolder> {

    public interface OnPastEventClickListener {
        void onPastEventClick(PastEventUIModel event);
    }

    private final List<PastEventUIModel> items = new ArrayList<>();
    private final OnPastEventClickListener listener;

    public PastTicketsAdapter(OnPastEventClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<PastEventUIModel> events) {
        items.clear();
        items.addAll(events);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PastTicketViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_past_event_card, parent, false);
        return new PastTicketViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PastTicketViewHolder holder, int position) {
        PastEventUIModel event = items.get(position);

        holder.icon.setImageResource(event.getIconResId());
        holder.title.setText(event.getTitle());
        holder.date.setText(event.getDateLabel());
        holder.location.setText(event.getLocationLabel());
        holder.detail.setText(event.getDetailLabel());
        holder.statusChip.setText(event.getStatusLabel());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onPastEventClick(event);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class PastTicketViewHolder extends RecyclerView.ViewHolder {
        final ImageView icon;
        final TextView title;
        final TextView date;
        final TextView location;
        final TextView detail;
        final TextView statusChip;

        PastTicketViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.past_event_icon);
            title = itemView.findViewById(R.id.past_event_title);
            date = itemView.findViewById(R.id.past_event_date);
            location = itemView.findViewById(R.id.past_event_location);
            detail = itemView.findViewById(R.id.past_event_detail);
            statusChip = itemView.findViewById(R.id.past_event_status_chip);
        }
    }
}
