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
import java.util.List;

/**
 * A fragment that allows administrators to browse all user profiles.
 * Displays a searchable, scrollable list of every registered user.
 * Admins can delete profiles from this screen (still needs polishing).
 */
public class AdminBrowseProfilesFragment extends Fragment {

    private AdminBrowseProfilesAdapter adapter;
    private final List<User> userList = new ArrayList<>();
    private UserDB userDB;

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
            userDB.deleteUser(user.getDeviceId());
            adapter.removeUser(user);
            Toast.makeText(getContext(), "Profile deleted", Toast.LENGTH_SHORT).show();
        });

        recyclerView.setAdapter(adapter);

        TextInputEditText searchEditText = view.findViewById(R.id.abp_search_edit_text);
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        fetchUsers();
    }

    /**
     * Fetches all users from Firestore via {@link UserDB}.
     * Populates the RecyclerView on success; shows a toast on failure.
     */
    private void fetchUsers() {
        userDB.getAllUsers(new UserDB.LoadUsersCallback() {
            @Override
            public void onSuccess(ArrayList<User> users) {
                userList.clear();
                if (users != null) {
                    userList.addAll(users);
                }
                adapter.filter("");
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(getContext(), "Failed to load profiles", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
