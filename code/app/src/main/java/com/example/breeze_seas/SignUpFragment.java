package com.example.breeze_seas;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * SignUpFragment is a {@link Fragment} subclass.
 * This fragment contains views for the user to fill in their details.
 * Checks values before proceeding to ExploreFragment.
 */
public class SignUpFragment extends Fragment {

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
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Grab android ID
        this.androidID = getArguments().getString("androidID");

        // Bind views
        EditText firstNameInput = view.findViewById(R.id.signupFirstnameInput);
        EditText lastNameInput = view.findViewById(R.id.signupLastnameInput);
        EditText userNameInput = view.findViewById(R.id.signupUsernameInput);
        EditText emailInput = view.findViewById(R.id.signupEmailInput);
        EditText phoneNumberInput = view.findViewById(R.id.signupPhoneNumberInput);

        // Bind button
        Button confirmButton = view.findViewById(R.id.signupConfirmButton);
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

                // Add User to DB
                UserDB userDBInstance = new UserDB();
                userDBInstance.createUser(user);

                // Switch to ExploreFragment
                // TODO: Cleaner implementation?
                ((MainActivity) requireActivity()).showBottomNav(true);
                getActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new ExploreFragment())
                        .commit();
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
}
