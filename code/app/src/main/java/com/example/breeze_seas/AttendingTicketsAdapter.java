package com.example.breeze_seas;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * AttendingTicketsAdapter renders confirmed attending tickets in a RecyclerView.
 */
public class AttendingTicketsAdapter extends RecyclerView.Adapter<AttendingTicketsAdapter.AttendingTicketViewHolder> {

    /**
     * Listener for taps on attending-ticket cards.
     */
    public interface OnTicketClickListener {
        /**
         * Handles a tap on an attending ticket card.
         *
         * @param ticket The attending ticket that was tapped.
         */
        void onTicketClick(AttendingTicketUIModel ticket);
    }

    private final List<AttendingTicketUIModel> items = new ArrayList<>();
    private final OnTicketClickListener listener;

    /**
     * Creates the adapter used by the Attending Tickets list.
     *
     * @param listener Listener invoked when the user taps a confirmed ticket.
     */
    public AttendingTicketsAdapter(OnTicketClickListener listener) {
        this.listener = listener;
    }

    /**
     * Replaces the current attending-ticket list contents.
     *
     * @param tickets New attending tickets to render.
     */
    public void submitList(List<AttendingTicketUIModel> tickets) {
        items.clear();
        items.addAll(tickets);
        notifyDataSetChanged();
    }

    /**
     * Inflates a single attending-ticket card view holder.
     *
     * @param parent Parent view group that will host the card.
     * @param viewType Adapter view type value.
     * @return A new attending-ticket view holder.
     */
    @NonNull
    @Override
    public AttendingTicketViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_attending_ticket, parent, false);
        return new AttendingTicketViewHolder(view);
    }

    /**
     * Binds the attending-ticket card at the requested position.
     *
     * @param holder View holder receiving the ticket data.
     * @param position Zero-based adapter position being bound.
     */
    @Override
    public void onBindViewHolder(@NonNull AttendingTicketViewHolder holder, int position) {
        AttendingTicketUIModel ticket = items.get(position);

        holder.title.setText(ticket.getTitle());
        holder.date.setText(ticket.getDateLabel());
        String locationLabel = ticket.getLocationLabel();
        if (locationLabel == null || locationLabel.trim().isEmpty()) {
            holder.location.setVisibility(View.GONE);
        } else {
            holder.location.setVisibility(View.VISIBLE);
            holder.location.setText(locationLabel);
        }
        holder.ticketType.setText(ticket.getTicketTypeLabel());
        holder.statusChip.setText("Confirmed");

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            /**
             * Dispatches the tapped attending ticket back to the fragment layer.
             *
             * @param v The ticket card view that was pressed.
             */
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onTicketClick(ticket);
                }
            }
        });
    }

    /**
     * Returns the number of attending-ticket cards currently being rendered.
     *
     * @return The attending-ticket item count.
     */
    @Override
    public int getItemCount() {
        return items.size();
    }

    /**
     * View holder for a single attending-ticket card.
     */
    static class AttendingTicketViewHolder extends RecyclerView.ViewHolder {
        final TextView title;
        final TextView date;
        final TextView location;
        final TextView ticketType;
        final TextView statusChip;

        /**
         * Binds the child views used to render one attending-ticket card.
         *
         * @param itemView Inflated card view backing this holder.
         */
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
