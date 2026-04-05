package com.example.breeze_seas;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;

/**
 * A fragment that allows administrators to browse all user profiles.
 * Displays a searchable, scrollable list of every registered user.
 * Uses {@link AdminViewModel} so the user list and listener persist across navigation
 * without requiring a re-fetch each time this fragment is recreated.
 * TODO: Implement popup before deletion
 */
public class AdminBrowseProfilesFragment extends Fragment {

    private AdminBrowseProfilesAdapter adapter;
    private AdminViewModel adminViewModel;
    private String currentQuery = "";

    public AdminBrowseProfilesFragment() { super(R.layout.fragment_admin_browse_profiles); }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        adminViewModel = new ViewModelProvider(requireActivity()).get(AdminViewModel.class);

        MaterialToolbar toolbar = view.findViewById(R.id.abp_top_app_bar);
        toolbar.setNavigationOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new AdminDashboardFragment())
                    .commit();
        });

        RecyclerView recyclerView = view.findViewById(R.id.abp_rv_profiles_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new AdminBrowseProfilesAdapter(new ArrayList<>(), (user, position) -> {
            String name = (user.getFirstName() + " " + user.getLastName()).trim();
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Delete profile")
                    .setMessage("\"" + name + "\" and all associated data will be permanently deleted.")
                    .setPositiveButton("Delete", (dialog, which) ->
                            // Runs a multistep batch (participant records, owned events, then the user document).
                            // The listener removes the user from the live list once Firestore confirms deletion.
                            adminViewModel.deleteUser(user.getDeviceId()))
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        recyclerView.setAdapter(adapter);

        TextInputEditText searchEditText = view.findViewById(R.id.abp_search_edit_text);
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentQuery = s.toString();
                adapter.filter(currentQuery);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Observe live list — admins are filtered out inside the adapter
        adminViewModel.getUsers().observe(getViewLifecycleOwner(), users -> {
            int previousCount = adapter.getItemCount();
            adapter.updateList(users);
            adapter.filter(currentQuery);
            if (adapter.getItemCount() < previousCount) {
                Toast.makeText(getContext(), "Profile deleted", Toast.LENGTH_SHORT).show();
            }
        });

        adminViewModel.startUsersListen();
    }
}
