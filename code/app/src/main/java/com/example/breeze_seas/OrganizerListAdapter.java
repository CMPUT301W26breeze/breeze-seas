package com.example.breeze_seas;

import static java.security.AccessController.getContext;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;

public class OrganizerListAdapter extends ArrayAdapter<User> {
    private int layoutResource;
    public OrganizerListAdapter(Context context, int resource, ArrayList<User> entrants){
        super(context,resource,entrants);
        this.layoutResource = resource;
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
            nameView.setText(entrant.getUserName());
        }
        return convertView;
    }
}
