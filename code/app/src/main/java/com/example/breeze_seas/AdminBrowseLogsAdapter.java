package com.example.breeze_seas;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for displaying list of all notifications sent to users.
 * Binds {@link Notification} data to the corresponding UI layout.
 */
public class AdminBrowseLogsAdapter extends RecyclerView.Adapter<AdminBrowseLogsAdapter.LogViewHolder> {
    private final List<Notification> logList;

    /**
     * Constructor for the adapter
     *
     * @param logList The list of {@link Notification} objects to be displayed.
     */
    public AdminBrowseLogsAdapter(List<Notification> logList) {
        this.logList = logList;
    }

    /**
     * Inflates the layout for individual log items in the RecyclerView.
     */
    @NonNull
    @Override
    public LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_notification, parent, false);
        return new LogViewHolder(view);
    }

    /**
     * Binds the data from a {@link Notification} object to the views within the
     * {@link LogViewHolder}.
     */
    @Override
    public void onBindViewHolder(@NonNull LogViewHolder holder, int position) {
        Notification log = logList.get(position);

        // Event Name
        holder.tvEventName.setText("Event: " + (log.getEventName() != null ? log.getEventName() : "System Message"));

        // Notification Type
        holder.tvType.setText("Type: " + (log.getType() != null ? log.getType().toString() : "UNKNOWN"));

        // Get username and ID
        holder.tvSentTo.setText("Sent to: " + (log.getUserId() != null ? log.getUserId() : "Unknown User"));

        // Content
        holder.tvContent.setText("Message: \"" + log.getDisplayMessage() + "\"");

        // Format time
        if (log.getSentAt() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy MMM dd h:mm a", Locale.getDefault());
            String formattedTime = sdf.format(log.getSentAt().toDate());
            holder.tvTime.setText(formattedTime);
        } else {
            holder.tvTime.setText("Unknown Time");
        }
    }

    /**
     * Returns the total number of notifications/logs in the current data set.
     */
    @Override
    public int getItemCount() {
        return logList.size();
    }

    /**
     * Holds references to the UI components for a single log/notification to avoid repeated findViewById calls.
     */
    public static class LogViewHolder extends RecyclerView.ViewHolder {
        TextView tvEventName;
        TextView tvTime;
        TextView tvType;
        TextView tvSentTo;
        TextView tvContent;

        public LogViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEventName = itemView.findViewById(R.id.ian_event_name_text);
            tvTime = itemView.findViewById(R.id.ian_notification_time_text);
            tvType = itemView.findViewById(R.id.ian_notification_type_text);
            tvSentTo = itemView.findViewById(R.id.ian_notification_sent_to_text);
            tvContent = itemView.findViewById(R.id.ian_notification_content_text);
        }
    }
}
