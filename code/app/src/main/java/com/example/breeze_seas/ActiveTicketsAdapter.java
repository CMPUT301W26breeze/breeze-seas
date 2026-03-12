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
    public interface OnTicketClickListener {
        void onTicketClick(TicketUIModel ticket);
    }

    private final List<TicketUIModel> items = new ArrayList<>();
    private final OnTicketClickListener listener;

    public ActiveTicketsAdapter(OnTicketClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<TicketUIModel> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TicketViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ticket_history, parent, false);
        return new TicketViewHolder(v);
    }

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
                holder.chip.setText("Action Required");
                holder.icon.setImageResource(R.drawable.ic_star);
                holder.supporting.setText("You were selected and your spot is waiting. Accept now to move this event into your attending tickets.");
                holder.footer.setText("Tap to accept or decline");

                holder.chip.setBackgroundResource(R.drawable.bg_ticket_status_solid);
                holder.chip.setTextColor(white);
                holder.actionDot.setVisibility(View.VISIBLE);
                break;
        }

        holder.footer.setTextColor(secondary);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onTicketClick(ticket);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class TicketViewHolder extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView title;
        TextView date;
        TextView supporting;
        TextView footer;
        TextView chip;
        View actionDot;

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
