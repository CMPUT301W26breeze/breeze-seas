package com.example.breeze_seas;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * ActiveTicketsAdapter renders the list of "Active" ticket cards.
 *
 * <p>Role:
 * - Binds {@link TicketUIModel} data to the ticket card layout.
 *
 * <p>Outstanding:
 * - Keep binding logic stable while TicketDB expands to more ticket states.
 * - Add stable IDs when using real Firestore document IDs.
 */
public class ActiveTicketsAdapter extends RecyclerView.Adapter<ActiveTicketsAdapter.TicketViewHolder> {
    // The following adapter implementation was generated with assistance from ChatGPT,
    // "How to implement a RecyclerView Adapter in Android using java", 2026-03-03.

    /**
     * Listener for taps on active-ticket cards.
     */
    public interface OnTicketClickListener {
        /**
         * Handles a tap on an active ticket card.
         *
         * @param ticket The active ticket that was tapped.
         */
        void onTicketClick(TicketUIModel ticket);
    }

    private final List<TicketUIModel> items = new ArrayList<>();
    private final OnTicketClickListener listener;

    /**
     * Creates the adapter used by the Active Tickets list.
     *
     * @param listener Listener invoked when the user taps a ticket card.
     */
    public ActiveTicketsAdapter(OnTicketClickListener listener) {
        this.listener = listener;
    }

    /**
     * Replaces the current active-ticket list contents.
     *
     * @param newItems New active tickets to render.
     */
    public void submitList(List<TicketUIModel> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    /**
     * Inflates a single active-ticket card view holder.
     *
     * @param parent Parent view group that will host the card.
     * @param viewType Adapter view type value.
     * @return A new active-ticket view holder.
     */
    @NonNull
    @Override
    public TicketViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ticket_history, parent, false);
        return new TicketViewHolder(v);
    }

    /**
     * Binds the active-ticket card at the requested position.
     *
     * @param holder View holder receiving the ticket data.
     * @param position Zero-based adapter position being bound.
     */
    @Override
    public void onBindViewHolder(@NonNull TicketViewHolder holder, int position) {
        TicketUIModel ticket = items.get(position);

        holder.title.setText(ticket.getTitle());
        holder.date.setText(ticket.getDateLabel());
        holder.actionDot.setVisibility(View.GONE);

        final int white = ContextCompat.getColor(holder.itemView.getContext(), android.R.color.white);
        final int black = ContextCompat.getColor(holder.itemView.getContext(), android.R.color.black);
        final int secondary = ContextCompat.getColor(holder.itemView.getContext(), R.color.text_secondary);

        switch (ticket.getStatus()) {

            case PENDING:
                holder.chip.setText("Waiting");
                holder.icon.setImageResource(R.drawable.ic_clock);
                holder.supporting.setText("Your entry is in the waiting list. We will notify you when the organizer updates the draw outcome.");
                holder.footer.setText("Awaiting selection outcome");

                holder.chip.setBackgroundResource(R.drawable.bg_ticket_status_outline);
                holder.chip.setTextColor(black);
                break;

            case BACKUP:
                holder.chip.setText("Backup pool");
                holder.icon.setImageResource(R.drawable.ic_info);
                holder.supporting.setText("The first draw has finished. Your entry stays active in case any confirmed spots are released.");
                holder.footer.setText("Tap to view backup pool details");

                holder.chip.setBackgroundResource(R.drawable.bg_ticket_status_outline);
                holder.chip.setTextColor(black);
                break;

            case ACTION_REQUIRED:
                holder.chip.setText(ticket.isPrivateEvent() ? "Private Invite" : "Action Required");
                holder.icon.setImageResource(R.drawable.ic_star);
                if (ticket.isPrivateEvent()) {
                    holder.supporting.setText("You were invited to join the waitlist for this private event. Accept to join the waitlist, or decline to dismiss the invite.");
                    holder.footer.setText("Tap to join or decline");
                } else {
                    holder.supporting.setText("You were selected and your spot is waiting. Accept now to move this event into your attending tickets.");
                    holder.footer.setText("Tap to accept or decline");
                }

                holder.chip.setBackgroundResource(R.drawable.bg_ticket_status_solid);
                holder.chip.setTextColor(white);
                holder.actionDot.setVisibility(View.VISIBLE);
                break;
        }

        holder.footer.setTextColor(secondary);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            /**
             * Dispatches the tapped active ticket back to the fragment layer.
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
     * Returns the number of active-ticket cards currently being rendered.
     *
     * @return The active-ticket item count.
     */
    @Override
    public int getItemCount() {
        return items.size();
    }

    /**
     * View holder for a single active-ticket card.
     */
    static class TicketViewHolder extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView title;
        TextView date;
        TextView supporting;
        TextView footer;
        TextView chip;
        View actionDot;

        /**
         * Binds the child views used to render one active-ticket card.
         *
         * @param itemView Inflated card view backing this holder.
         */
        TicketViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.ticket_thumbnail);
            title = itemView.findViewById(R.id.ticket_event_title);
            date = itemView.findViewById(R.id.ticket_event_date);
            supporting = itemView.findViewById(R.id.ticket_supporting_copy);
            footer = itemView.findViewById(R.id.ticket_footer_hint);
            chip = itemView.findViewById(R.id.ticket_status_chip);
            actionDot = itemView.findViewById(R.id.action_dot);
        }
    }
}
