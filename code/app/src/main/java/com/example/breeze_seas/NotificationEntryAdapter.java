package com.example.breeze_seas;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Renders inbox-style notification rows for the Alerts tab.
 */
public class NotificationEntryAdapter extends RecyclerView.Adapter<NotificationEntryAdapter.NotificationViewHolder> {

    /**
     * Receives taps on one notification row.
     */
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

        int fallbackIconRes = bindTypePresentation(holder, notification);
        bindFallbackIcon(holder.icon, fallbackIconRes);
        UiImageBinder.bindEventPoster(holder.icon, notification.getEventId(),
                () -> bindFallbackIcon(holder.icon, fallbackIconRes));
        holder.unreadDot.setVisibility(notification.isSeen() ? View.GONE : View.VISIBLE);
        holder.card.setAlpha(notification.isSeen() ? 0.74f : 1f);

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

    /**
     * Holds the bound views for one notification row.
     */
    public static class NotificationViewHolder extends RecyclerView.ViewHolder {
        TextView messageText, timeText, typeChip;
        ImageView icon;
        View card;
        View unreadDot;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.notification_entry_text);
            timeText = itemView.findViewById(R.id.notification_time_text);
            typeChip = itemView.findViewById(R.id.notification_entry_type_chip);
            icon = itemView.findViewById(R.id.notification_entry_icon);
            card = itemView.findViewById(R.id.notification_entry_card);
            unreadDot = itemView.findViewById(R.id.notification_unread_dot);
        }
    }

    private int bindTypePresentation(@NonNull NotificationViewHolder holder, @NonNull Notification notification) {
        NotificationType type = notification.getType();
        int white = ContextCompat.getColor(holder.itemView.getContext(), android.R.color.white);
        int black = ContextCompat.getColor(holder.itemView.getContext(), android.R.color.black);

        if (type == NotificationType.WIN) {
            holder.typeChip.setText("Selected");
            holder.typeChip.setBackgroundResource(R.drawable.bg_ticket_status_solid);
            holder.typeChip.setTextColor(white);
            return R.drawable.ic_star;
        }

        if (type == NotificationType.LOSS) {
            holder.typeChip.setText("Draw result");
            holder.typeChip.setBackgroundResource(R.drawable.bg_ticket_status_outline);
            holder.typeChip.setTextColor(black);
            return R.drawable.ic_luck;
        }

        if (type == NotificationType.PRIVATE_EVENT_INVITE) {
            holder.typeChip.setText("Private invite");
            holder.typeChip.setBackgroundResource(R.drawable.bg_ticket_status_solid);
            holder.typeChip.setTextColor(white);
            return R.drawable.ic_ticket;
        }

        if (type == NotificationType.CO_ORG_INVITE) {
            holder.typeChip.setText("Co-organizer");
            holder.typeChip.setBackgroundResource(R.drawable.bg_ticket_status_outline);
            holder.typeChip.setTextColor(black);
            return R.drawable.ic_star;
        }

        holder.typeChip.setText("Announcement");
        holder.typeChip.setBackgroundResource(R.drawable.bg_ticket_status_outline);
        holder.typeChip.setTextColor(black);
        return R.drawable.ic_notification;
    }

    private void bindFallbackIcon(@NonNull ImageView imageView, int drawableResId) {
        int padding = (int) (imageView.getResources().getDisplayMetrics().density * 14);
        imageView.setImageResource(drawableResId);
        imageView.setImageTintList(ColorStateList.valueOf(
                ContextCompat.getColor(imageView.getContext(), R.color.text_primary)));
        imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        imageView.setPadding(padding, padding, padding, padding);
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
