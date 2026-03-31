package com.example.breeze_seas;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

// TODO: (Optional) combine this with other adapter classes
public class NotificationEntryAdapter extends RecyclerView.Adapter<NotificationEntryAdapter.NotificationViewHolder> {

    public interface OnNotificationClickListener {
        void onNotificationClick(Notification notification);
    }


    private OnNotificationClickListener listener;

    private List<Notification> notificationList;

    public NotificationEntryAdapter(List<Notification> notificationList,OnNotificationClickListener listener) {
        this.notificationList = notificationList;
        this.listener=listener;
    }

    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification_entry, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        Notification notification = notificationList.get(position);

        holder.messageText.setText(notification.getDisplayMessage());
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onNotificationClick(notification);
        });

        bindTypePresentation(holder, notification);

        if (notification.getSentAt() != null) {
            long timestampMillis = notification.getSentAt().toDate().getTime();

            String timeString = formatTimestamp(timestampMillis);
            holder.timeText.setText(timeString);
        } else {
            holder.timeText.setText("");
        }

    }

    @Override
    public int getItemCount() {
        return notificationList.size();
    }

    public static class NotificationViewHolder extends RecyclerView.ViewHolder {
        TextView messageText, timeText, typeChip;
        ImageView icon;
        View card;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.notification_entry_text);
            timeText = itemView.findViewById(R.id.notification_time_text);
            typeChip = itemView.findViewById(R.id.notification_entry_type_chip);
            icon = itemView.findViewById(R.id.notification_entry_icon);
            card = itemView.findViewById(R.id.notification_entry_card);
        }
    }

    private void bindTypePresentation(@NonNull NotificationViewHolder holder, @NonNull Notification notification) {
        NotificationType type = notification.getType();

        if (type == NotificationType.WIN) {
            holder.typeChip.setText("Selected");
            holder.typeChip.setBackgroundResource(R.drawable.bg_ticket_status_solid);
            holder.typeChip.setTextColor(holder.itemView.getContext().getColor(android.R.color.white));
            holder.icon.setImageResource(R.drawable.ic_star);
            return;
        }

        if (type == NotificationType.LOSS) {
            holder.typeChip.setText("Lottery");
            holder.typeChip.setBackgroundResource(R.drawable.bg_ticket_status_outline);
            holder.typeChip.setTextColor(holder.itemView.getContext().getColor(android.R.color.black));
            holder.icon.setImageResource(R.drawable.ic_clock);
            return;
        }

        holder.typeChip.setText("Announcement");
        holder.typeChip.setBackgroundResource(R.drawable.bg_ticket_status_outline);
        holder.typeChip.setTextColor(holder.itemView.getContext().getColor(android.R.color.black));
        holder.icon.setImageResource(R.drawable.ic_notification);
    }

    private String formatTimestamp(long timestamp) {
        Calendar now = Calendar.getInstance();
        Calendar notificationTime = Calendar.getInstance();
        notificationTime.setTimeInMillis(timestamp);

        // Check if it's the same day, month, and year
        boolean isSameDay = now.get(Calendar.YEAR) == notificationTime
                .get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) == notificationTime
                        .get(Calendar.DAY_OF_YEAR);

        if (isSameDay) {
            return new SimpleDateFormat("h:mm a", Locale.getDefault())
                    .format(new Date(timestamp));
        } else {
            return new SimpleDateFormat("MMM d", Locale.getDefault())
                    .format(new Date(timestamp));
        }
    }

}
