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

    private static final String ADMIN_PASSWORD = "flyingfish";
    private static final String ARG_CONTAINER_ID = "containerId";

    private UserDB userDB = new UserDB();

    /**
     * Creates an instance with the container ID to navigate into after successful auth.
     * Passing the container ID avoids hardcoding R.id.fragment_container, allowing
     * this dialog to work inside both MainActivity and FragmentScenario.
     */
    public static AdminAuthDialogFragment newInstance(int containerId) {
        Bundle args = new Bundle();
        args.putInt(ARG_CONTAINER_ID, containerId);
        AdminAuthDialogFragment fragment = new AdminAuthDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        final EditText input = new EditText(requireContext());
        input.setId(R.id.admin_password_input);
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

    private void grantAdminAccess() {
        String currentDeviceId = Settings.Secure.getString(
                requireContext().getContentResolver(),
                Settings.Secure.ANDROID_ID
        );

        Map<String, Object> updates = new HashMap<>();
        updates.put("isAdmin", true);

        userDB.updateUser(currentDeviceId, updates);
        Toast.makeText(getContext(), "Admin rights granted!", Toast.LENGTH_SHORT).show();

        int containerId = getArguments() != null
                ? getArguments().getInt(ARG_CONTAINER_ID, R.id.fragment_container)
                : R.id.fragment_container;

        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(containerId, new AdminDashboardFragment())
                .addToBackStack(null)
                .commit();
    }

    public void setUserDB(UserDB userDB) {
        this.userDB = userDB;
    }
}
