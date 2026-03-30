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

/**
 * Adapter for the RecyclerView in the AdminBrowseProfiles screen.
 * Binds a list of {@link User} objects to corresponding UI elements.
 * Supports search filtering and item deletion.
 */
public class AdminBrowseProfilesAdapter extends RecyclerView.Adapter<AdminBrowseProfilesAdapter.ProfileViewHolder> {

    /**
     * Interface definition for a callback to be invoked when a profile is deleted.
     */
    public interface OnProfileDeleteListener {
        void onProfileDelete(User user, int position);
    }

    private final List<User> fullList;
    private final List<User> filteredList;
    private final OnProfileDeleteListener deleteListener;

    /**
     * Constructs a new AdminBrowseProfilesAdapter.
     *
     * @param userList The full list of users to display.
     * @param deleteListener Callback invoked when the delete button is pressed.
     */
    public AdminBrowseProfilesAdapter(List<User> userList, OnProfileDeleteListener deleteListener) {
        this.fullList = userList;
        this.filteredList = new ArrayList<>(userList);
        this.deleteListener = deleteListener;
    }

    /**
     * Filters the displayed list based on a search query.
     * Matches against display name, username, and device ID (case-insensitive).
     *
     * @param query The search string to filter by.
     */
    public void filter(String query) {
        filteredList.clear();
        if (query == null || query.trim().isEmpty()) {
            filteredList.addAll(fullList);
        } else {
            String lower = query.trim().toLowerCase();
            for (User user : fullList) {
                String displayName = getDisplayName(user).toLowerCase();
                String userName = user.getUserName() != null ? user.getUserName().toLowerCase() : "";
                String deviceId = user.getDeviceId() != null ? user.getDeviceId().toLowerCase() : "";
                if (displayName.contains(lower) || userName.contains(lower) || deviceId.contains(lower)) {
                    filteredList.add(user);
                }
            }
        }
        notifyDataSetChanged();
    }

    /**
     * Removes a user from both lists and refreshes the view.
     *
     * @param user The user to remove.
     */
    public void removeUser(User user) {
        int filteredPos = filteredList.indexOf(user);
        fullList.remove(user);
        if (filteredPos != RecyclerView.NO_ID && filteredPos < filteredList.size()) {
            filteredList.remove(user);
            notifyItemRemoved(filteredPos);
            notifyItemRangeChanged(filteredPos, filteredList.size());
        }
    }

    @NonNull
    @Override
    public ProfileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_user_profile, parent, false);
        return new ProfileViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProfileViewHolder holder, int position) {
        User user = filteredList.get(position);
        holder.tvUserName.setText("Name: " + getDisplayName(user));
        holder.tvUsername.setText("User Name: " + (user.getUserName() != null ? user.getUserName() : "-"));
        holder.tvDeviceId.setText("Device ID: " + (user.getDeviceId() != null ? user.getDeviceId() : "-"));

        holder.btnDelete.setOnClickListener(v -> {
            int currentPosition = holder.getBindingAdapterPosition();
            if (currentPosition != RecyclerView.NO_POSITION && deleteListener != null) {
                deleteListener.onProfileDelete(filteredList.get(currentPosition), currentPosition);
            }
        });
    }

    @Override
    public int getItemCount() {
        return filteredList.size();
    }

    private String getDisplayName(User user) {
        String first = user.getFirstName() != null ? user.getFirstName().trim() : "";
        String last = user.getLastName() != null ? user.getLastName().trim() : "";
        String fullName = (first + " " + last).trim();
        if (!fullName.isEmpty()) return fullName;
        if (user.getUserName() != null && !user.getUserName().trim().isEmpty()) return user.getUserName();
        return user.getDeviceId() != null ? user.getDeviceId() : "Unknown";
    }

    /**
     * Holds references to the UI components for a single profile row.
     */
    public static class ProfileViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAvatar;
        TextView tvUserName;
        TextView tvUsername;
        TextView tvDeviceId;
        ImageView btnDelete;

        public ProfileViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.aup_iv_avatar);
            tvUserName = itemView.findViewById(R.id.aup_tv_user_name);
            tvUsername = itemView.findViewById(R.id.aup_tv_username);
            tvDeviceId = itemView.findViewById(R.id.aup_tv_device_id);
            btnDelete = itemView.findViewById(R.id.aup_btn_delete_profile);
        }
    }
}
