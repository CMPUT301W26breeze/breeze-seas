package com.example.breeze_seas;

import android.graphics.Bitmap;
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
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * CreateEventFragment collects organizer input for a new event and submits it through
 * {@link EventDB}.
 */
public class CreateEventFragment extends Fragment {

    private ImageView ivPoster;
    private LinearLayout posterPlaceholder;

    private TextInputEditText etRegFrom;
    private TextInputEditText etRegTo;
    private TextInputEditText etEventName;
    private TextInputEditText etEventDetails;
    private TextInputEditText etCapacity;
    private TextInputEditText etWaitingListCapacity;
    private SwitchMaterial swGeo;
    private SessionViewModel viewModel;

    private Long regFromMillis = null;
    private Long regToMillis = null;
    private String posterBase64 = "";

    private final ActivityResultLauncher<String> pickImage =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri == null) {
                    return;
                }

                handleSelectedPoster(uri);
            });

    /**
     * Creates the organizer event-creation fragment using the shared create-event layout.
     */
    public CreateEventFragment() {
        super(R.layout.fragment_create_event);
    }

    /**
     * Binds organizer form fields and wires the create-event interactions.
     *
     * @param view Inflated create-event root view.
     * @param savedInstanceState Previously saved instance state bundle, or {@code null}.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(SessionViewModel.class);

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());

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
        etWaitingListCapacity = view.findViewById(R.id.etWaitingListCapacity);
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

    /**
     * Handles a poster chosen from the system picker by compressing it into Base64 and
     * updating the on-screen preview.
     *
     * @param uri Selected poster image Uri.
     */
    private void handleSelectedPoster(Uri uri) {
        try {
            posterBase64 = ImageUtils.uriToCompressedBase64(requireContext(), uri);

            Bitmap previewBitmap = ImageUtils.base64ToBitmap(posterBase64);
            if (previewBitmap != null) {
                ivPoster.setImageBitmap(previewBitmap);
                ivPoster.setVisibility(View.VISIBLE);
                posterPlaceholder.setVisibility(View.GONE);
            } else {
                posterBase64 = "";
                Toast.makeText(requireContext(), "Failed to preview image", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            posterBase64 = "";
            Toast.makeText(requireContext(), "Failed to process image", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Opens the registration-date picker and stores the selected date range.
     */
    private void openDateRangePicker() {
        MaterialDatePicker.Builder<androidx.core.util.Pair<Long, Long>> builder =
                MaterialDatePicker.Builder.dateRangePicker()
                        .setTheme(R.style.ThemeOverlay_Breezeseas_DateRangePicker)
                        .setTitleText("Select registration period");

        if (regFromMillis != null && regToMillis != null) {
            builder.setSelection(new androidx.core.util.Pair<>(regFromMillis, regToMillis));
        }

        MaterialDatePicker<androidx.core.util.Pair<Long, Long>> picker = builder.build();

        picker.addOnPositiveButtonClickListener(selection -> {
            if (selection == null) {
                return;
            }

            regFromMillis = selection.first;
            regToMillis = selection.second;

            SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy", Locale.US);
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            etRegFrom.setText(sdf.format(new Date(regFromMillis)));
            etRegTo.setText(sdf.format(new Date(regToMillis)));
        });

        picker.show(getParentFragmentManager(), "reg_range");
    }

    /**
     * Validates organizer input, builds a new {@link Event}, and submits it to {@link EventDB}.
     */
    private void onCreateClicked() {
        String name = etEventName.getText() == null ? "" : etEventName.getText().toString().trim();
        String details = etEventDetails.getText() == null ? "" : etEventDetails.getText().toString().trim();
        String capText = etCapacity.getText() == null ? "" : etCapacity.getText().toString().trim();
        String waitingListText = etWaitingListCapacity.getText() == null ? "" : etWaitingListCapacity.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            etEventName.setError("Required");
            return;
        }

        if (regFromMillis == null || regToMillis == null) {
            Toast.makeText(requireContext(), "Please set registration period", Toast.LENGTH_SHORT).show();
            return;
        }

        String organizerId = viewModel == null ? null : viewModel.getAndroidID().getValue();
        if (organizerId == null || organizerId.trim().isEmpty()) {
            Toast.makeText(requireContext(), "Unable to resolve organizer identity", Toast.LENGTH_SHORT).show();
            return;
        }

        Integer eventCap = null;
        if (TextUtils.isEmpty(capText)) {
            etCapacity.setError("Number required");
            return;
        }

        try {
            eventCap = Integer.parseInt(capText);
        } catch (NumberFormatException e) {
            etCapacity.setError("Enter a valid number");
            return;
        }

        Integer eventWaitingListCap = null;
        if (!TextUtils.isEmpty(waitingListText)) {
            try {
                eventWaitingListCap = Integer.parseInt(waitingListText);
            } catch (NumberFormatException e) {
                etWaitingListCapacity.setError("Enter a valid number");
                return;
            }
        }

        int normalizedCapacity = eventCap == null ? -1 : eventCap;
        int normalizedWaitingListCapacity = eventWaitingListCap == null ? -1 : eventWaitingListCap;

        Event event = new Event(
                false,
                organizerId,
                name,
                details,
                posterBase64 == null ? "" : posterBase64,
                "",
                new Timestamp(new Date(regFromMillis)),
                new Timestamp(new Date(regToMillis)),
                null,
                null,
                swGeo.isChecked(),
                normalizedCapacity,
                normalizedWaitingListCapacity,
                0
        );

        EventDB.addEvent(event, new EventDB.AddEventCallback() {
            @Override
            public void onSuccess(String eventId) {
                Toast.makeText(requireContext(), "Event created", Toast.LENGTH_SHORT).show();

                if (viewModel != null) {
                    viewModel.setEventShown(event);
                }

                Bundle args = new Bundle();
                args.putString("eventId", eventId);

                ViewQrCodeFragment fragment = new ViewQrCodeFragment();
                fragment.setArguments(args);

                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, fragment)
                        .commit();
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(requireContext(), "Failed to create event", Toast.LENGTH_SHORT).show();
            }
        });
    }
}