package com.example.breeze_seas;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class AttendingTicketsAdapter extends RecyclerView.Adapter<AttendingTicketsAdapter.AttendingTicketViewHolder> {

    public interface OnTicketClickListener {
        void onTicketClick(AttendingTicketUIModel ticket);
    }

    private final List<AttendingTicketUIModel> items = new ArrayList<>();
    private final OnTicketClickListener listener;

    public AttendingTicketsAdapter(OnTicketClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<AttendingTicketUIModel> tickets) {
        items.clear();
        items.addAll(tickets);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AttendingTicketViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_attending_ticket, parent, false);
        return new AttendingTicketViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AttendingTicketViewHolder holder, int position) {
        AttendingTicketUIModel ticket = items.get(position);

        holder.title.setText(ticket.getTitle());
        holder.date.setText(ticket.getDateLabel());
        holder.location.setText(ticket.getLocationLabel());
        holder.ticketType.setText(ticket.getTicketTypeLabel());
        holder.statusChip.setText("Confirmed");

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onTicketClick(ticket);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class AttendingTicketViewHolder extends RecyclerView.ViewHolder {
        final TextView title;
        final TextView date;
        final TextView location;
        final TextView ticketType;
        final TextView statusChip;

        AttendingTicketViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.attending_ticket_title);
            date = itemView.findViewById(R.id.attending_ticket_date);
            location = itemView.findViewById(R.id.attending_ticket_location);
            ticketType = itemView.findViewById(R.id.attending_ticket_type_value);
            statusChip = itemView.findViewById(R.id.attending_ticket_status_chip);
        }
    }
}
