package com.example.breeze_seas;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

public class TermsAndCondition extends DialogFragment {

    public interface TermsListener{
        void onAccept();
    }

    private TermsListener listener;
    private Event event;
    private static final String[] BASE_TERMS = {
            "1. Participant selection is conducted through a random lottery generation process. Joining the waiting list does not guarantee admission to the event.",
            "2. Your registered account name and account number will be recorded for identification and selection purposes.",
            "3. Once the lottery is completed, you will receive a notification informing you of the result.",
            "4. If selected, you may be required to confirm your participation within a specified timeframe. Failure to respond may result in forfeiture of your spot."
    };

    private static final String LOCATION_TERM = "5. The organizer of this particular event has chosen to record your location. If confirmed, the organizer can see where you signed up from.";
    private static final String HEADER = "By signing in to the waiting list, you agree that:\n\n";
    private static final String FOOTER = "\nBy proceeding, you acknowledge and accept these terms.";


    public TermsAndCondition(TermsListener listener,Event event){
        this.listener=listener;
        this.event=event;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState){
        StringBuilder sb = new StringBuilder(HEADER);


        for (String term : BASE_TERMS) {
            sb.append(term).append("\n\n");
        }

        if (event.isGeolocationEnforced()) {
            sb.append(LOCATION_TERM).append("\n\n");
        }

        sb.append(FOOTER);

        return new AlertDialog.Builder(getActivity())
                .setTitle("Terms and Conditions")
                .setMessage(sb.toString())
                .setPositiveButton("Accept", (dialog, id) -> listener.onAccept())
                .setNegativeButton("Decline", (dialog, id) -> dismiss())
                .create();

    }
}
