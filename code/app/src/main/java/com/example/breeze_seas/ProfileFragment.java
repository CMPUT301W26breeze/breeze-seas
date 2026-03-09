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
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.w3c.dom.Text;

/*** ProfileFragment is a top-level destination accessed via Bottom Navigation.
 *
 * <p>Current state:* - A placeholder screen was created to validate the navigation wiring and theme style.
 ** <p>Outstanding/Future Work:* - Set up entrant profile viewing/editing and notification choices.
 */
public class ProfileFragment extends Fragment {

    private TextInputEditText editName;

    private TextInputLayout nameLayout, emailLayout, phoneLayout;
    private ImageButton editNameBtn, editEmailBtn, editPhoneBtn;
    private MaterialButton saveBtn, deleteBtn;
    private MaterialSwitch optOutSwitch;


    public ProfileFragment() {
        super(R.layout.fragment_profile);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        nameLayout = view.findViewById(R.id.name_filled_text_field);
        emailLayout = view.findViewById(R.id.email_filled_text_field);
        phoneLayout = view.findViewById(R.id.phone_number_filled_text_field);

        editNameBtn = view.findViewById(R.id.edit_name_button);
        editEmailBtn = view.findViewById(R.id.edit_email_button);
        editPhoneBtn = view.findViewById(R.id.edit_phone_number_button);

        editNameBtn = view.findViewById(R.id.edit_name_button);
        editEmailBtn = view.findViewById(R.id.edit_email_button);
        editPhoneBtn = view.findViewById(R.id.edit_phone_number_button);

        saveBtn = view.findViewById(R.id.save_button);
        deleteBtn = view.findViewById(R.id.delete_profile_button);
        optOutSwitch = view.findViewById(R.id.opt_out_switch);

        setupListeners();

        return view;
    }



    private void setupListeners() {
        // Toggle name field when edit icon is clicked
        editNameBtn.setOnClickListener(v -> {
            boolean isEnabled = nameLayout.isEnabled();
            nameLayout.setEnabled(!isEnabled);
        });

        // Toggle name field when edit icon is clicked
        editEmailBtn.setOnClickListener(v -> {
            boolean isEnabled = emailLayout.isEnabled();
            emailLayout.setEnabled(!isEnabled);
        });

        // Save button
        saveBtn.setOnClickListener(v -> {
            String name = nameLayout.getEditText().getText().toString();
            Toast.makeText(getContext(), "Profile Saved!", Toast.LENGTH_SHORT).show();
        });

        // Switch
        optOutSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Handle opt-out preference
        });
    }
}




