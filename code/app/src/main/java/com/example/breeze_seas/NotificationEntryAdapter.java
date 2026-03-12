package com.example.breeze_seas;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textview.MaterialTextView;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

// TODO: (Optional) combine this with other adapter classes
public class NotificationEntryAdapter extends RecyclerView.Adapter<NotificationEntryAdapter.NotificationViewHolder> {

    private List<Notification> notificationList;

    public NotificationEntryAdapter(List<Notification> notificationList) {
        this.notificationList = notificationList;
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

        // Set the message
        holder.messageText.setText(notification.getDisplayMessage());

        // Format and set the time
        String timeString = notification.getSentAt().toString();
        long timestamp = Long.parseLong(timeString);
        timeString = formatTimestamp(timestamp);
        holder.timeText.setText(timeString);

    }

    @Override
    public int getItemCount() {
        return notificationList.size();
    }

    public static class NotificationViewHolder extends RecyclerView.ViewHolder {
        MaterialTextView messageText, timeText;
        MaterialCardView card;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.notification_entry_text);
            timeText = itemView.findViewById(R.id.notification_time_text);
            card = itemView.findViewById(R.id.notification_entry_card);
        }
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
