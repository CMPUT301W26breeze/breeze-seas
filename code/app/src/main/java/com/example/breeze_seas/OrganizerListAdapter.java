package com.example.breeze_seas;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;

public class OrganizerListAdapter extends ArrayAdapter<User> {
    private final String statusLabel;
    private final boolean highlightedStatus;

    public OrganizerListAdapter(Context context, int resource, ArrayList<User> entrants, @NonNull String statusLabel, boolean highlightedStatus){
        super(context,resource,entrants);
        this.statusLabel = statusLabel;
        this.highlightedStatus = highlightedStatus;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_organizer_list, parent, false);
        }
        User entrant = getItem(position);

        if (entrant != null) {
            TextView nameView = convertView.findViewById(R.id.entrant_name_text);
            TextView detailView = convertView.findViewById(R.id.entrant_detail_text);
            TextView statusView = convertView.findViewById(R.id.entrant_status_chip);

            String userName = entrant.getUserName();
            String firstName = entrant.getFirstName();
            String lastName = entrant.getLastName();
            String fullName = ((firstName == null ? "" : firstName.trim()) + " "
                    + (lastName == null ? "" : lastName.trim())).trim();

            if (fullName != null && !fullName.trim().isEmpty()) {
                nameView.setText(fullName);
            } else if (userName != null && !userName.trim().isEmpty()) {
                nameView.setText(userName);
            } else if ((firstName != null && !firstName.trim().isEmpty())
                    || (lastName != null && !lastName.trim().isEmpty())) {
                nameView.setText(((firstName == null ? "" : firstName) + " " + (lastName == null ? "" : lastName)).trim());
            } else {
                nameView.setText(entrant.getDeviceId());
            }

            String secondaryText = entrant.getEmail();
            if (secondaryText == null || secondaryText.trim().isEmpty()) {
                secondaryText = entrant.getDeviceId();
            }
            detailView.setText(secondaryText);
            statusView.setText(statusLabel);
            statusView.setBackgroundResource(highlightedStatus
                    ? R.drawable.bg_ticket_status_solid
                    : R.drawable.bg_ticket_status_outline);
            statusView.setTextColor(ContextCompat.getColor(getContext(), highlightedStatus
                    ? R.color.white
                    : R.color.text_primary));
        }
        return convertView;
    }
}
