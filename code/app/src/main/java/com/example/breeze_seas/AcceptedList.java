package com.example.breeze_seas;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import com.opencsv.CSVWriter;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

/**
 * Represents the "accepted" status for an event.
 * Handles users who have accepted invite for the event.
 */

public class AcceptedList extends StatusList{

    /**
     * Constructs a new AcceptedList for a specific event.
     * @param event {@link Event} object this accepted list belongs to.
     * @param capacity The maximum number of entrants allowed on this accepted list.
     */

    public AcceptedList(Event event, int capacity) {
        super(event, capacity);
    }

    /**
     * Defines the unique status identifier for this list type in Firestore.
     * @return A string literal "accepted" used to filter participant documents.
     */

    @Override
    protected String getStatusName() {
        return "accepted";
    }

    public void exportCsv(Context context, Uri uri){
        try (OutputStream stream = context.getContentResolver().openOutputStream(uri);
             CSVWriter writer = new CSVWriter(new OutputStreamWriter(stream))) {
            writer.writeNext(new String[]{"S.N","First Name", "Last Name",
                    "Username", "Email"});
            int sn=1;
            for (User u : this.userList) {
                writer.writeNext(new String[]{String.valueOf(sn++),u.getFirstName(),u.getLastName(),u.getUserName(),
                        u.getEmail()});
            }
        } catch (IOException e) {
            Log.e("CSV", "Could not export CSV", e);
        }

    }
}
