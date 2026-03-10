package com.example.breeze_seas;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.firestore.FirebaseFirestoreException;

/**
 * SignUpFragment is a {@link Fragment} subclass.
 * This fragment contains views for the user to fill in their details.
 * Checks values before proceeding to ExploreFragment.
 */
public class SignUpFragment extends Fragment {

    private SessionViewModel viewModel;
    private String androidID;
    private String firstName;
    private String lastName;
    private String userName;
    private String email;
    private String phoneNumber;

    public SignUpFragment() {
        super(R.layout.fragment_sign_up);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Setup viewModel
        viewModel = new ViewModelProvider(requireActivity()).get(SessionViewModel.class);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Grab android ID
        this.androidID = viewModel.getAndroidID().getValue();

        // Bind views
        EditText firstNameInput = view.findViewById(R.id.signup_firstname_input);
        EditText lastNameInput = view.findViewById(R.id.signup_lastname_input);
        EditText userNameInput = view.findViewById(R.id.signup_username_input);
        EditText emailInput = view.findViewById(R.id.signup_email_input);
        EditText phoneNumberInput = view.findViewById(R.id.signup_phone_number_input);

        // Bind button
        Button confirmButton = view.findViewById(R.id.signup_confirm_button);
        confirmButton.setOnClickListener(onClick -> {
            // Grab and update strings
            this.firstName = firstNameInput.getText().toString();
            this.lastName = lastNameInput.getText().toString();
            this.userName = userNameInput.getText().toString();
            this.email = emailInput.getText().toString();
            this.phoneNumber = phoneNumberInput.getText().toString();

            // If details are okay
            if (verifyDetails()) {
                // Create User
                User user = new User(this.androidID, this.firstName, this.lastName, this.userName,
                        this.email, this.phoneNumber, false);

                confirmButton.setEnabled(false);
                FirebaseSession.ensureAuthenticated(new FirebaseSession.OnReadyListener() {
                    @Override
                    public void onReady() {
                        // Grab userDBInstance from viewModel
                        UserDB userDBInstance = viewModel.getUserDBInstance().getValue();
                        userDBInstance.createUser(user)
                                .addOnSuccessListener(unused -> {
                                    if (!isAdded()) {
                                        return;
                                    }

                                    // Add user to view model
                                    viewModel.setUser(user);

                                    // Switch to ExploreFragment only after the user write succeeds.
                                    ((MainActivity) requireActivity()).showBottomNav(true);
                                    requireActivity().getSupportFragmentManager()
                                            .beginTransaction()
                                            .replace(R.id.fragment_container, new ExploreFragment())
                                            .commit();
                                })
                                .addOnFailureListener(e -> {
                                    if (!isAdded()) {
                                        return;
                                    }

                                    confirmButton.setEnabled(true);
                                    dialogMsg(buildSignUpErrorMessage(e));
                                });
                    }

                    @Override
                    public void onError(@NonNull Exception e) {
                        if (!isAdded()) {
                            return;
                        }

                        confirmButton.setEnabled(true);
                        dialogMsg(buildSignUpErrorMessage(e));
                    }
                });
            }
        });

    }

    /**
     * Checks the details are valid before allowing user to continue.
     * @return True or False whether details meet requirements
     */
    private boolean verifyDetails() {
        // Verifies all fields
        if (!verifyFirstName()) {
            return false;
        } else if (!verifyLastName()) {
            return false;
        } else if (!verifyUserName()) {
            return false;
        } else if (!verifyEmail()) {
            return false;
        } else if (!verifyPhoneNumber()) {
            return false;
        }
        return true;
    }

    /**
     * Checks first name
     * @return True if first name is valid, otherwise False
     */
    private boolean verifyFirstName() {
        if (this.firstName == null || this.firstName.isBlank()) {
            dialogMsg("First Name cannot be empty");
            return false;
        }
        return true;
    }

    /**
     * Checks last name
     * @return True if last name is valid, otherwise False
     */
    private boolean verifyLastName() {
        if (this.lastName == null || this.lastName.isBlank()) {
            dialogMsg("Last Name cannot be empty");
            return false;
        }
        return true;
    }

    /**
     * Checks user name
     * @return True if user name is valid, otherwise False
     */
    private boolean verifyUserName() {
        if (this.userName == null || this.userName.isBlank()) {
            dialogMsg("User Name cannot be empty");
            return false;
        }
        return true;
    }

    /**
     * Checks email address
     * @return True if email address is valid, otherwise False
     */
    private boolean verifyEmail() {
        if (this.email == null || this.email.isBlank()) {
            dialogMsg("Email cannot be empty");
            return false;
        } else if (!this.email.contains("@")) {
            dialogMsg("Email address must contain @");
            return false;
        }
        return true;
    }

    /**
     * Checks phone number
     * @return True if phone number is valid, otherwise False
     */
    private boolean verifyPhoneNumber() {
        // Phone Number is optional, can be blank
        return true;
    }

    /**
     * Takes a string a builds an alert dialogue displaying the message.
     * @param msg Message to show
     */
    private void dialogMsg(String msg) {
        // Source - https://stackoverflow.com/a/66298602
        // Posted by Marwa Eltayeb, modified by community. See post 'Timeline' for change history
        // Retrieved 2026-03-09, License - CC BY-SA 4.0

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setMessage(msg);
        builder.setPositiveButton("I understand", (dialogInterface, i) -> {
            dialogInterface.dismiss();
        });

        // Show the dialog
        final AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private String buildSignUpErrorMessage(Exception e) {
        String baseMessage = "Failed to create your account.";

        if (e instanceof FirebaseFirestoreException) {
            FirebaseFirestoreException firestoreException = (FirebaseFirestoreException) e;
            if (firestoreException.getCode() == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                return baseMessage + "\n\nFirestore denied the write. Check your Firestore rules.";
            }
            if (firestoreException.getCode() == FirebaseFirestoreException.Code.UNAVAILABLE) {
                return baseMessage + "\n\nFirestore is unavailable right now. Check your connection and try again.";
            }
        }

        if (e instanceof FirebaseAuthException) {
            return baseMessage + "\n\nFirebase authentication failed: " + e.getMessage();
        }

        if (e instanceof FirebaseNetworkException) {
            return baseMessage + "\n\nNetwork error: " + e.getMessage();
        }

        if (e != null && e.getMessage() != null && !e.getMessage().trim().isEmpty()) {
            return baseMessage + "\n\nDetails: " + e.getMessage();
        }

        return baseMessage + "\n\nCheck Firebase setup and Firestore rules.";
    }
}
