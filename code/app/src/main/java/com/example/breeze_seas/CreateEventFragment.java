package com.example.breeze_seas;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class CreateEventFragment extends Fragment {

    private ImageView ivPoster;
    private LinearLayout posterPlaceholder;

    private TextInputEditText etRegFrom, etRegTo, etEventName, etEventDetails, etCapacity, etEventCapacity;
    private SwitchMaterial swGeo;

    private Long regFromMillis = null;
    private Long regToMillis = null;
    private Uri posterUri = null;

    private final ActivityResultLauncher<String> pickImage =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    posterUri = uri;
                    ivPoster.setImageURI(uri);
                    ivPoster.setVisibility(View.VISIBLE);
                    posterPlaceholder.setVisibility(View.GONE);
                }
            });

    public CreateEventFragment() {
        super(R.layout.fragment_create_event);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack()
        );

        ivPoster = view.findViewById(R.id.ivPoster);
        posterPlaceholder = view.findViewById(R.id.posterPlaceholder);

        View cardPoster = view.findViewById(R.id.cardPoster);
        View btnAddImage = view.findViewById(R.id.btnAddImage);
        View btnRegPeriod = view.findViewById(R.id.btnRegPeriod);

        etRegFrom = view.findViewById(R.id.etRegFrom);
        etRegTo = view.findViewById(R.id.etRegTo);
        etEventName = view.findViewById(R.id.etEventName);
        etEventDetails = view.findViewById(R.id.etEventDetails);
        etCapacity = view.findViewById(R.id.etCapacity);
        etEventCapacity = view.findViewById(R.id.etEventCapacity);
        swGeo = view.findViewById(R.id.swGeo);

        View.OnClickListener pickPoster = v -> pickImage.launch("image/*");
        cardPoster.setOnClickListener(pickPoster);
        btnAddImage.setOnClickListener(pickPoster);

        View.OnClickListener pickRange = v -> openDateRangePicker();
        btnRegPeriod.setOnClickListener(pickRange);
        etRegFrom.setOnClickListener(pickRange);
        etRegTo.setOnClickListener(pickRange);

        view.findViewById(R.id.btnCreate).setOnClickListener(v -> onCreateClicked());
    }

    private void openDateRangePicker() {
        MaterialDatePicker.Builder<androidx.core.util.Pair<Long, Long>> builder =
                MaterialDatePicker.Builder.dateRangePicker()
                        .setTitleText("Select registration period");

        MaterialDatePicker<androidx.core.util.Pair<Long, Long>> picker = builder.build();

        picker.addOnPositiveButtonClickListener(selection -> {
            if (selection == null) return;
            regFromMillis = selection.first;
            regToMillis = selection.second;

            SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy", Locale.US);
            etRegFrom.setText(sdf.format(new Date(regFromMillis)));
            etRegTo.setText(sdf.format(new Date(regToMillis)));
        });

        picker.show(getParentFragmentManager(), "reg_range");
    }

    private void onCreateClicked() {
        String name = etEventName.getText() == null ? "" : etEventName.getText().toString().trim();
        String details = etEventDetails.getText() == null ? "" : etEventDetails.getText().toString().trim();
        String waitingCapText = etCapacity.getText() == null ? "" : etCapacity.getText().toString().trim();
        String eventCapText = etEventCapacity.getText() == null ? "" : etEventCapacity.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            etEventName.setError("Required");
            return;
        }

        if (regFromMillis == null || regToMillis == null) {
            Toast.makeText(requireContext(), "Please set registration period", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(requireContext(), "User not signed in", Toast.LENGTH_SHORT).show();
            return;
        }

        Integer waitingListCap = null;
        if (!TextUtils.isEmpty(waitingCapText)) {
            try {
                waitingListCap = Integer.parseInt(waitingCapText);
            } catch (NumberFormatException e) {
                etCapacity.setError("Enter a valid number");
                return;
            }
        }

        Integer eventCapacity = null;
        if (!TextUtils.isEmpty(eventCapText)) {
            try {
                eventCapacity = Integer.parseInt(eventCapText);
            } catch (NumberFormatException e) {
                etEventCapacity.setError("Enter a valid number");
                return;
            }
        }

        Event event = new Event(
                "",
                currentUser.getUid(),
                name,
                details,
                posterUri == null ? null : posterUri.toString(),
                new Timestamp(new Date(regFromMillis)),
                new Timestamp(new Date(regToMillis)),
                Timestamp.now(),
                eventCapacity,
                waitingListCap,
                swGeo.isChecked(),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>()
        );

        EventDB.getInstance().addEvent(event, new EventDB.AddEventCallback() {
            @Override
            public void onSuccess(String eventId) {
                Toast.makeText(requireContext(), "Event created", Toast.LENGTH_SHORT).show();

                Bundle args = new Bundle();
                args.putString("eventId", eventId);

                ViewQrCodeFragment fragment = new ViewQrCodeFragment();
                fragment.setArguments(args);

                ((MainActivity) requireActivity()).openSecondaryFragment(fragment);
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(requireContext(), "Failed to create event", Toast.LENGTH_SHORT).show();
            }
        });
    }
}