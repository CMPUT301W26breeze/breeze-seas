package com.example.breeze_seas;
import android.content.Context;

import android.content.Context;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputLayout;
import java.util.HashMap;
import java.util.Map;

/**
 * * ProfileFragment is a top-level destination accessed via Bottom Navigation.
 * It is the fragment for users to view their account details and update them
 * as needed.
 */
public class ProfileFragment extends Fragment {

    UserDB userDBInstance;
    User currentUser;
    private ShapeableImageView profileImageView;
    private TextInputLayout firstNameLayout, lastNameLayout,
            userNameLayout, emailLayout, phoneLayout;
    private ImageButton editFirstNameBtn, editLastNameBtn,
            editUserNameBtn, editEmailBtn, editPhoneBtn;
    private MaterialButton saveBtn, deleteBtn;
    private MaterialSwitch optOutSwitch;

    // Stores tap count for profile pic
    private int secretTapCount = 0;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Only create a real one if a test hasn't provided a mock yet
        if (userDBInstance == null) {
            userDBInstance = new UserDB();
        }
    }

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

        // Initialize the view model, get the deviceId, and fetch user data
        SessionViewModel viewModel;
        viewModel = new ViewModelProvider(requireActivity()).get(SessionViewModel.class);
        viewModel.getAndroidID().observe(getViewLifecycleOwner(), deviceId -> {
            if (deviceId != null) {
                Log.d("BreezeSeas", "Observed ID: " + deviceId);
                fetchUserData(deviceId);
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
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Save Changes")
                    .setMessage("Are you sure you want to save your changes?")
                    .setPositiveButton("Yes", (dialog, which) -> {

                        String firstNameInput = getInput(firstNameLayout);
                        currentUser.setFirstName(firstNameInput);
                        String lastNameInput = getInput(lastNameLayout);
                        currentUser.setLastName(lastNameInput);
                        String userNameInput = getInput(userNameLayout);
                        currentUser.setUserName(userNameInput);
                        String emailInput = getInput(emailLayout);
                        if (!emailInput.contains("@")){
                            Toast.makeText(getContext(), "Incorrect Email!",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }
                        currentUser.setEmail(emailInput);
                        String phoneInput = getInput(phoneLayout);
                        if (!phoneInput.matches("^[0-9 ]*$")) {
                            Toast.makeText(getContext(), "Incorrect Phone Number!",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }
                        currentUser.setPhoneNumber(phoneInput);
                        currentUser.setNotificationEnabled(optOutSwitch.isChecked());



                        String deviceId = currentUser.getDeviceId();
                        Map<String,Object> updates = mapUpdates();
                        userDBInstance.updateUser(deviceId, updates);
                        Toast.makeText(getContext(), "Profile Saved!",
                                Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("No", (dialog, which) -> {
                        dialog.dismiss();
                    })
                    .show();
        });

        // Delete profile button
        deleteBtn.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Delete Profile")
                    .setMessage("Are you sure you want to delete your profile?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        userDBInstance.deleteUser(currentUser.getDeviceId());
                        ((MainActivity) getActivity()).showBottomNav(false);
                        WelcomeScreenFragment welcomeScreenFragment = new WelcomeScreenFragment();
                        getActivity().getSupportFragmentManager()
                                .popBackStackImmediate(null,
                                        FragmentManager.POP_BACK_STACK_INCLUSIVE);
                        getActivity().getSupportFragmentManager()
                                .beginTransaction()
                                .replace(R.id.fragment_container, welcomeScreenFragment)
                                .commit();
                    })
                    .setNegativeButton("No", (dialog, which) -> {
                        dialog.dismiss();
                    })
                    .show();


            Toast.makeText(getContext(), "Delete Profile (TODO)", Toast.LENGTH_SHORT).show();

        });
    }

    /**
     * This method handles the input process for a {@link TextInputLayout}
     *
     * @param layout the layout user is entering its input in.
     */
    private void toggleEditField(TextInputLayout layout) {

        if (layout.getEditText() == null) {
            return;
        }

        boolean isEnabled = layout.getEditText().isEnabled();
        layout.getEditText().setEnabled(!isEnabled);

        if (!isEnabled) {
            layout.getEditText().requestFocus();
            layout.getEditText().setSelection(layout.getEditText().getText().length());

            InputMethodManager imm = (InputMethodManager)
                    requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(layout.getEditText(),0);
            }
        }
    }

    /**
     * Gets and returns the input written by the user inside a
     * {@link com.google.android.material.textfield.TextInputEditText}
     *
     * @param layout the layout that
     * {@link com.google.android.material.textfield.TextInputEditText} lives in.
     *
     * @return input
     */
    public String getInput(TextInputLayout layout){
            String input;
            input = layout.getEditText().
                getText().toString();
            return input;
    }

    /**
     * Fetches the data of the user from the database. Then fills the text
     * fields and toggles on toggles off the notification switch.
     *
     * @param deviceId Used to fetch the logged-in user from the database.
     */
    private void fetchUserData(String deviceId) {
        userDBInstance.getUser(deviceId, new UserDB.OnUserLoadedListener() {
            @Override
            public void onUserLoaded(User user) {

                currentUser = user;

                // Fill the text fields
                firstNameLayout.getEditText().setText(user.getFirstName());
                lastNameLayout.getEditText().setText(user.getLastName());
                userNameLayout.getEditText().setText(user.getUserName());
                emailLayout.getEditText().setText(user.getEmail());
                phoneLayout.getEditText().setText(user.getPhoneNumber());

                // Set the current state of the switch
                optOutSwitch.setChecked(user.notificationEnabled());


            }

            @Override
            public void onError(Exception e) {
            }
        });
    }

    /**
     * Creates a hash map that stores the database updates that will be made to
     * user details.
     * @return updates
     */
    private Map<String,Object> mapUpdates() {

        Map<String,Object> updates = new HashMap<String,Object>();
        updates.put("firstName", currentUser.getFirstName());
        updates.put("lastName", currentUser.getLastName());
        updates.put("userName", currentUser.getUserName());
        updates.put("email", currentUser.getEmail());
        updates.put("phoneNumber", currentUser.getPhoneNumber());
        updates.put("notificationEnabled", currentUser.notificationEnabled());

        return updates;
    }

    /**
     * Verifies that the logged-in user is an admin and navigates them to admin
     * dashboard.
     */
    private void verifyAdminStatus() {
        String currentDeviceId = Settings.Secure.getString(
                requireContext().getContentResolver(),
                Settings.Secure.ANDROID_ID
        );

        userDBInstance.getUser(currentDeviceId, new UserDB.OnUserLoadedListener() {
            @Override
            public void onUserLoaded(User user) {
                if (user != null && user.isAdmin()) {
                    Toast.makeText(getContext(), "Successfully verified!", Toast.LENGTH_SHORT).show();
                    navigateToAdminDashboard();
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

    /**
     * Navigates the user to admin dashboard interface.
     */
    private void navigateToAdminDashboard() {
        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new AdminDashboardFragment())
                .addToBackStack(null)
                .commit();
    }

    public void setUserDB(UserDB userDB) {
        this.userDBInstance = userDB;
    }

}



