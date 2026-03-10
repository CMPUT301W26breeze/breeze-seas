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
import com.google.android.material.textfield.TextInputLayout;

public class ProfileFragment extends Fragment {

    private TextInputLayout nameLayout, emailLayout, phoneLayout;
    private ImageButton editNameBtn, editEmailBtn, editPhoneBtn;
    private MaterialButton saveBtn, deleteBtn;
    private MaterialSwitch optOutSwitch;

    public ProfileFragment() {
        super();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        nameLayout = view.findViewById(R.id.name_filled_text_field);
        emailLayout = view.findViewById(R.id.email_filled_text_field);
        phoneLayout = view.findViewById(R.id.phone_number_filled_text_field);

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
        editNameBtn.setOnClickListener(v -> toggleEditField(nameLayout));
        editEmailBtn.setOnClickListener(v -> toggleEditField(emailLayout));
        editPhoneBtn.setOnClickListener(v -> toggleEditField(phoneLayout));

        saveBtn.setOnClickListener(v -> {
            String name = nameLayout.getEditText() != null
                    ? nameLayout.getEditText().getText().toString().trim()
                    : "";
            String email = emailLayout.getEditText() != null
                    ? emailLayout.getEditText().getText().toString().trim()
                    : "";
            String phone = phoneLayout.getEditText() != null
                    ? phoneLayout.getEditText().getText().toString().trim()
                    : "";

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
}