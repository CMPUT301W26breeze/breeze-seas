package com.example.breeze_seas;

import static androidx.core.content.ContentProviderCompat.requireContext;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

/**
 * A DialogFragment that provides organizer actions of removing entrants
 * from status lists and promoting them to co-organizers.
 */
public class ListDialogFragment extends DialogFragment {

    private User user;
    private StatusList currentList; // Pass whichever list the user was clicked in

    /**
     * Constructor for ListDialogFragment.
     * @param user The {@link User} object representing the selected entrant.
     * @param currentList The {@link StatusList} context (e.g., WaitingList) to perform actions against.
     */
    public ListDialogFragment(User user, StatusList currentList) {
        this.user = user;
        this.currentList = currentList;
    }


    /**
     * Creates the selection dialog containing the management options.
     * @param savedInstanceState The last saved instance state of the Fragment, or null.
     * @return A new AlertDialog instance displaying the entrant options.
     */
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        String[] options = {"Remove Entrant"};

        return new AlertDialog.Builder(requireContext())
                .setTitle("Options for " + user.getUserName())
                .setItems(options, (dialog, value) -> {
                    if (value == 0) {
                        deleteDialog();
                    }
                })
                .setNegativeButton("Cancel", null)
                .create();
    }


    /**
     * Displays a confirmation alert before proceeding with a user deletion.
     */
    private void deleteDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Remove Entrant")
                .setMessage("Are you sure you want to remove " + user.getUserName() + "?")
                .setPositiveButton("Remove", (d, w) -> {
                    currentList.removeUserFromDB(user.getDeviceId(), null);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
