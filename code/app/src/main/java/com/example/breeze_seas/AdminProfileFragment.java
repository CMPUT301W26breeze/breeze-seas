package com.example.breeze_seas;

import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.HashMap;
import java.util.Map;
// may no longer be needed
public class AdminProfileFragment extends Fragment {

    private static final String ADMIN_PASSWORD = "flyingfish";

    private final UserDB userDB = new UserDB();

    public AdminProfileFragment() { super(R.layout.fragment_admin_profile); }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Button btnAdminDashboard = view.findViewById(R.id.ap_admin_dashboard_button);

        btnAdminDashboard.setOnClickListener(v -> {
            String currentDeviceId = Settings.Secure.getString(
                    requireContext().getContentResolver(),
                    Settings.Secure.ANDROID_ID
            );

            checkAdminStatus(currentDeviceId);
        });
    }

    private void checkAdminStatus(String deviceId) {
        userDB.getUser(deviceId, new UserDB.OnUserLoadedListener() {
            @Override
            public void onUserLoaded(User user) {
                if (user != null && user.isAdmin()) {
                    navigateToDashboard();
                } else {
                    showPasswordPrompt(deviceId);
                }
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(getContext(), "Error verifying account", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showPasswordPrompt(String deviceId) {
        final EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        input.setHint("Enter Admin Password");

        LinearLayout layout = new LinearLayout(requireContext());
        layout.setPadding(50, 20, 50, 0);
        layout.addView(input, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Admin Authentication")
                .setMessage("Please enter the password to access the dashboard.")
                .setView(layout)
                .setPositiveButton("Verify", (dialog, which) -> {
                    String enteredPassword = input.getText().toString();

                    if (enteredPassword.equals(ADMIN_PASSWORD)) {
                        grantAdminAccess(deviceId);
                    } else {
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("isAdmin", false);
                        Toast.makeText(getContext(), "Incorrect password", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.cancel())
                .show();
    }

    private void grantAdminAccess(String deviceId) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("isAdmin", true);

        userDB.updateUser(deviceId, updates);
        Toast.makeText(getContext(), "Admin rights granted!", Toast.LENGTH_SHORT).show();

        navigateToDashboard();
    }

    private void navigateToDashboard() {
        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new AdminDashboardFragment())
                .addToBackStack(null)
                .commit();
    }
}
