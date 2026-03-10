package com.example.breeze_seas;
import android.os.Bundle;
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

        // Toggle first name field when edit icon is clicked
        editFirstNameBtn.setOnClickListener(v -> {
            boolean isEnabled = firstNameLayout.isEnabled();
            firstNameLayout.setEnabled(!isEnabled);
        });

        // Toggle last name field when edit icon is clicked
        editLastNameBtn.setOnClickListener(v -> {
            boolean isEnabled = lastNameLayout.isEnabled();
            lastNameLayout.setEnabled(!isEnabled);
        });

        // Toggle username field when edit icon is clicked
        editUserNameBtn.setOnClickListener(v -> {
            boolean isEnabled = userNameLayout.isEnabled();
            userNameLayout.setEnabled(!isEnabled);
        });

        // Toggle email field when edit icon is clicked
        editEmailBtn.setOnClickListener(v -> {
            boolean isEnabled = emailLayout.isEnabled();
            emailLayout.setEnabled(!isEnabled);
        });

        // Toggle phone number field when edit icon is clicked
        editPhoneBtn.setOnClickListener(v -> {
            boolean isEnabled = phoneLayout.isEnabled();
            phoneLayout.setEnabled(!isEnabled);
        });

        // Save button
        saveBtn.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Profile Saved!", Toast.LENGTH_SHORT).show();
        });

        // Switch
        optOutSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Handle opt-out preference
        });
        
    }
}




