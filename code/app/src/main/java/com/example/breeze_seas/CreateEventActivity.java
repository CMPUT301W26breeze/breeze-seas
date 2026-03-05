package com.example.breeze_seas;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public class CreateEventActivity extends AppCompatActivity {

    private ImageView ivPoster;
    private LinearLayout posterPlaceholder;

    private TextInputEditText etRegFrom, etRegTo, etEventName;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_event);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        ivPoster = findViewById(R.id.ivPoster);
        posterPlaceholder = findViewById(R.id.posterPlaceholder);

        View cardPoster = findViewById(R.id.cardPoster);
        View btnAddImage = findViewById(R.id.btnAddImage);
        View btnRegPeriod = findViewById(R.id.btnRegPeriod);

        etRegFrom = findViewById(R.id.etRegFrom);
        etRegTo = findViewById(R.id.etRegTo);
        etEventName = findViewById(R.id.etEventName);

        View.OnClickListener pickPoster = v -> pickImage.launch("image/*");
        cardPoster.setOnClickListener(pickPoster);
        btnAddImage.setOnClickListener(pickPoster);

        View.OnClickListener pickRange = v -> openDateRangePicker();
        btnRegPeriod.setOnClickListener(pickRange);
        etRegFrom.setOnClickListener(pickRange);
        etRegTo.setOnClickListener(pickRange);

        findViewById(R.id.btnCreate).setOnClickListener(v -> onCreateClicked());
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

        picker.show(getSupportFragmentManager(), "reg_range");
    }

    private void onCreateClicked() {
        String name = etEventName.getText() == null ? "" : etEventName.getText().toString().trim();

        if (name.isEmpty()) {
            etEventName.setError("Required");
            return;
        }
        if (regFromMillis == null || regToMillis == null) {
            Toast.makeText(this, "Please set registration period", Toast.LENGTH_SHORT).show();
            return;
        }

        String eventId = UUID.randomUUID().toString();

        Intent i = new Intent(this, ViewQrCodeActivity.class);
        i.putExtra("eventId", eventId);
        i.putExtra("eventName", name);
        startActivity(i);

        finish();
    }
}