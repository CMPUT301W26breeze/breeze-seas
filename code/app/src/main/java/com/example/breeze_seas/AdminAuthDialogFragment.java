package com.example.breeze_seas;

import android.app.Dialog;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles admin authentication and routing to admin dashboard
 */
public class AdminAuthDialogFragment extends DialogFragment {

    // Hardcoded password
    // TODO: Explore other options for admin auth
    private static final String ADMIN_PASSWORD = "flyingfish";
    private final UserDB userDB = new UserDB();

    /**
     * Creates dialog with a password input field and handles button clicks.
     */
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        final EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        input.setHint("Enter Admin Password");

        LinearLayout layout = new LinearLayout(requireContext());
        layout.setPadding(50, 20, 50, 0);
        layout.addView(input, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        return new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Admin Authentication")
                .setMessage("Enter the password to upgrade this account to Admin.")
                .setView(layout)
                .setPositiveButton("Verify", (dialog, which) -> {
                    String enteredPassword = input.getText().toString();
                    if (enteredPassword.equals(ADMIN_PASSWORD)) {
                        grantAdminAccess();
                    } else {
                        Toast.makeText(getContext(), "Incorrect password", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.cancel())
                .create();
    }

    /**
     * Updates user information in the DB (isAdmin set to true) based on their device ID,
     * then shows the admin dashboard screen.
     */
    private void grantAdminAccess() {
        // Fetches android/user ID to identify user
        String currentDeviceId = Settings.Secure.getString(
                requireContext().getContentResolver(),
                Settings.Secure.ANDROID_ID
        );

        Map<String, Object> updates = new HashMap<>();
        updates.put("isAdmin", true);

        // Update DB
        userDB.updateUser(currentDeviceId, updates);
        Toast.makeText(getContext(), "Admin rights granted!", Toast.LENGTH_SHORT).show();

        // Route to dashboard
        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new AdminDashboardFragment())
                .addToBackStack(null)
                .commit();
    }
}
