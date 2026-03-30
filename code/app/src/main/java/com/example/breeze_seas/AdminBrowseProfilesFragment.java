package com.example.breeze_seas;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A fragment that allows administrators to browse all user profiles.
 * Displays a searchable, scrollable list of every registered user.
 * Admins can delete profiles from this screen (still needs polishing).
 */
public class AdminBrowseProfilesFragment extends Fragment {

    private AdminBrowseProfilesAdapter adapter;
    // Master list of all users — the adapter's filter reads from this
    private final List<User> userList = new ArrayList<>();
    private UserDB userDB;
    // Tracks what the admin has typed in the search box so we can re-apply it after each update
    private String currentQuery = "";

    public AdminBrowseProfilesFragment() { super(R.layout.fragment_admin_browse_profiles); }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        userDB = new UserDB();

        MaterialToolbar toolbar = view.findViewById(R.id.abp_top_app_bar);
        toolbar.setNavigationOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new AdminDashboardFragment())
                    .commit();
        });

        RecyclerView recyclerView = view.findViewById(R.id.abp_rv_profiles_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new AdminBrowseProfilesAdapter(userList, (user, position) -> {
            // Kick off async deletion — this runs a multi-step batch (participant records,
            // owned events, then the user document itself) before anything is actually removed.
            // We do NOT show a toast here because nothing has been deleted yet at this point.
            userDB.deleteUser(user.getDeviceId());
        });

        recyclerView.setAdapter(adapter);

        TextInputEditText searchEditText = view.findViewById(R.id.abp_search_edit_text);
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Save the query so listener callbacks can re-apply it after each change
                currentQuery = s.toString();
                adapter.filter(currentQuery);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Start the real-time listener — populates the list and keeps it in sync automatically
        startListening();
    }

    /**
     * Attaches a real-time listener to the "users" collection via {@link UserDB}.
     * Firestore pushes each individual change (add / modify / remove) as it happens,
     * so the list stays up to date without needing a manual refresh.
     */
    private void startListening() {
        userList.clear();
        userDB.startUsersListen(new UserDB.UsersChangedCallback() {

            @Override
            public void onUserAdded(User user) {
                // New user document appeared in Firestore — add to master list and refresh view
                userList.add(user);
                adapter.filter(currentQuery);
            }

            @Override
            public void onUserModified(User user) {
                // An existing user's details changed — find them in the list and swap the object
                for (int i = 0; i < userList.size(); i++) {
                    if (user.getDeviceId().equals(userList.get(i).getDeviceId())) {
                        userList.set(i, user);
                        break;
                    }
                }
                adapter.filter(currentQuery);
            }

            @Override
            public void onUserRemoved(User user) {
                // The batch commit in deleteUser succeeded and Firestore confirmed the removal.
                // This is the right place to show the toast — we know the deletion actually happened.
                Iterator<User> it = userList.iterator();
                while (it.hasNext()) {
                    if (user.getDeviceId().equals(it.next().getDeviceId())) {
                        it.remove();
                        break;
                    }
                }
                adapter.filter(currentQuery);
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Profile deleted", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Exception e) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Failed to load profiles", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Detach the Firestore listener when the view is gone to prevent memory leaks
        userDB.stopUsersListen();
    }
}
