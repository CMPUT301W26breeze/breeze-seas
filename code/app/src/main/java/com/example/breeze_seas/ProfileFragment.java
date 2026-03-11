package com.example.breeze_seas;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/*** ProfileFragment is a top-level destination accessed via Bottom Navigation.
 *
 * <p>Current state:* - A placeholder screen was created to validate the navigation wiring and theme style.
 ** <p>Outstanding/Future Work:* - Set up entrant profile viewing/editing and notification choices.
 */
public class ProfileFragment extends Fragment {

    private ShapeableImageView profileImageView;
    private TextInputLayout firstNameLayout, lastNameLayout,
            userNameLayout, emailLayout, phoneLayout;
    private ImageButton editFirstNameBtn, editLastNameBtn,
            editUserNameBtn, editEmailBtn, editPhoneBtn;
    private MaterialButton saveBtn, deleteBtn;
    private MaterialSwitch optOutSwitch;

    // Stores tap count for profile pic
    private int secretTapCount = 0;
    private final UserDB userDB = new UserDB();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        profileImageView = view.findViewById(R.id.profile_image);

        firstNameLayout = view.findViewById(R.id.first_name_filled_text_field);
        lastNameLayout = view.findViewById(R.id.last_name_filled_text_field);
        userNameLayout = view.findViewById(R.id.user_name_filled_text_field);
        emailLayout = view.findViewById(R.id.email_filled_text_field);
        phoneLayout = view.findViewById(R.id.phone_number_filled_text_field);

        editFirstNameBtn = view.findViewById(R.id.edit_first_name_button);
        editLastNameBtn = view.findViewById(R.id.edit_last_name_button);
        editUserNameBtn = view.findViewById(R.id.edit_user_name_button);
        editEmailBtn = view.findViewById(R.id.edit_email_button);
        editPhoneBtn = view.findViewById(R.id.edit_phone_number_button);

        saveBtn = view.findViewById(R.id.save_button);
        deleteBtn = view.findViewById(R.id.delete_profile_button);
        optOutSwitch = view.findViewById(R.id.opt_out_switch);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Click on profile picture 5 times to show admin password dialogue
        View profileImage = view.findViewById(R.id.profile_image);

        profileImage.setOnClickListener(v -> {
            secretTapCount++;

            if (secretTapCount >= 5) {
                secretTapCount = 0;

                verifyAdminStatus();
            }
        });

        // Toggle first name field when edit icon is clicked
        editFirstNameBtn.setOnClickListener(v -> {
            toggleEditField(firstNameLayout);
        });

        // Toggle last name field when edit icon is clicked
        editLastNameBtn.setOnClickListener(v -> {
            toggleEditField(lastNameLayout);
        });

        // Toggle username field when edit icon is clicked
        editUserNameBtn.setOnClickListener(v -> {
            toggleEditField(userNameLayout);
        });

        // Toggle email field when edit icon is clicked
        editEmailBtn.setOnClickListener(v -> {
            toggleEditField(emailLayout);
        });

        // Toggle phone number field when edit icon is clicked
        editPhoneBtn.setOnClickListener(v -> {
            toggleEditField(phoneLayout);
        });

        // Save button
        saveBtn.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Profile Saved!", Toast.LENGTH_SHORT).show();
        });

        deleteBtn.setOnClickListener(v ->
                Toast.makeText(getContext(), "Delete Profile (TODO)", Toast.LENGTH_SHORT).show()
        );

        optOutSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            String msg = isChecked ? "Notifications turned off" : "Notifications turned on";
            Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
        });
    }

    private void toggleEditField(TextInputLayout layout) {
        if (layout.getEditText() == null) return;

        boolean isEnabled = layout.getEditText().isEnabled();
        layout.getEditText().setEnabled(!isEnabled);

        if (!isEnabled) {
            layout.getEditText().requestFocus();
            layout.getEditText().setSelection(layout.getEditText().getText().length());
        }
    }

    private void verifyAdminStatus() {
        String currentDeviceId = Settings.Secure.getString(
                requireContext().getContentResolver(),
                Settings.Secure.ANDROID_ID
        );

        userDB.getUser(currentDeviceId, new UserDB.OnUserLoadedListener() {
            @Override
            public void onUserLoaded(User user) {
                if (user != null && user.isAdmin()) {
                    Toast.makeText(getContext(), "Successfully verified!", Toast.LENGTH_SHORT).show();
                    navigateToAdminProfile();
                } else {
                    new AdminAuthDialogFragment().show(getParentFragmentManager(), "AdminAuth");
                }
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(getContext(), "Error verifying account status", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void navigateToAdminProfile() {
        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new AdminProfileFragment())
                .addToBackStack(null)
                .commit();
    }
}


