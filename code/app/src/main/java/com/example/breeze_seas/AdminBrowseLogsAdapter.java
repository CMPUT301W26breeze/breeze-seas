package com.example.breeze_seas;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class AdminBrowseLogsAdapter extends RecyclerView.Adapter<AdminBrowseLogsAdapter.LogViewHolder> {
    private final List<Notification> logList;

    public AdminBrowseLogsAdapter(List<Notification> logList) {
        this.logList = logList;
    }

    @NonNull
    @Override
    public LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_notification, parent, false);
        return new LogViewHolder(view);
    }

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

        // Format and set Time (Year Month Day Time)
        if (log.getSentAt() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy MMM dd h:mm a", Locale.getDefault());
            String formattedTime = sdf.format(log.getSentAt().toDate());
            holder.tvTime.setText(formattedTime);
        } else {
            holder.tvTime.setText("Unknown Time");
        }
    }

    @Override
    public int getItemCount() {
        return logList.size();
    }

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
