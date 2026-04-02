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

    /**
     * Creates a reusable organizer-side user list adapter.
     *
     * @param context Row inflation context.
     * @param resource Layout resource for each row.
     * @param entrants Mutable list of users shown in the list.
     * @param statusLabel Label rendered in the trailing status chip.
     * @param highlightedStatus Whether the chip should use the emphasized style.
     */
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

            String secondaryText = buildSecondaryText(entrant);
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

    /**
     * Builds the secondary row text shown under the user's name.
     *
     * <p>This prefers email and phone together when both are present so organizer pickers can
     * disambiguate similarly named users more easily.
     *
     * @param entrant User currently being rendered.
     * @return One-line secondary label for the row.
     */
    @NonNull
    private String buildSecondaryText(@NonNull User entrant) {
        String email = clean(entrant.getEmail());
        String phoneNumber = clean(entrant.getPhoneNumber());

        if (!email.isEmpty() && !phoneNumber.isEmpty()) {
            return email + " • " + phoneNumber;
        }
        if (!email.isEmpty()) {
            return email;
        }
        if (!phoneNumber.isEmpty()) {
            return phoneNumber;
        }
        return entrant.getDeviceId() == null ? "" : entrant.getDeviceId();
    }

    /**
     * Normalizes optional text values for display logic.
     *
     * @param value Raw optional text.
     * @return Trimmed text or an empty string when unavailable.
     */
    @NonNull
    private String clean(@Nullable String value) {
        return value == null ? "" : value.trim();
    }
}
