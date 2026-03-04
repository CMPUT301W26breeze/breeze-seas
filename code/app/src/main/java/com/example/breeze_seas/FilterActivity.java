package com.example.breeze_seas;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FilterActivity extends AppCompatActivity {

    public static final String EXTRA_STATUS = "status";
    public static final String EXTRA_LIMITED_CAP = "limitedCap";
    public static final String EXTRA_GEO_REQUIRED = "geoRequired";
    public static final String EXTRA_FROM_MILLIS = "fromMillis";
    public static final String EXTRA_TO_MILLIS = "toMillis";
    public static final String EXTRA_SORT = "sort";

    private ChipGroup chipGroupStatus;
    private SwitchMaterial swLimitedCap, swGeoRequired;
    private TextInputEditText etFrom, etTo;

    private Long fromMillis = null;
    private Long toMillis = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filter);

        MaterialToolbar toolbar = findViewById(R.id.toolbarFilter);
        toolbar.setNavigationOnClickListener(v -> finish());

        chipGroupStatus = findViewById(R.id.chipGroupStatus);
        swLimitedCap = findViewById(R.id.swLimitedCap);
        swGeoRequired = findViewById(R.id.swGeoRequired);
        etFrom = findViewById(R.id.etFrom);
        etTo = findViewById(R.id.etTo);

        // default selections
        findViewById(R.id.chipAll).performClick();
        findViewById(R.id.rbNewest).performClick();

        // Date range picker
        etFrom.setOnClickListener(v -> openDateRangePicker());
        etTo.setOnClickListener(v -> openDateRangePicker());

        findViewById(R.id.btnReset).setOnClickListener(v -> resetAll());
        findViewById(R.id.btnApply).setOnClickListener(v -> applyAndReturn());
    }

    private void openDateRangePicker() {
        MaterialDatePicker.Builder<androidx.core.util.Pair<Long, Long>> builder =
                MaterialDatePicker.Builder.dateRangePicker()
                        .setTitleText("Select registration period");

        MaterialDatePicker<androidx.core.util.Pair<Long, Long>> picker = builder.build();

        picker.addOnPositiveButtonClickListener(selection -> {
            if (selection == null) return;
            fromMillis = selection.first;
            toMillis = selection.second;

            SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy", Locale.US);
            etFrom.setText(sdf.format(new Date(fromMillis)));
            etTo.setText(sdf.format(new Date(toMillis)));
        });

        picker.show(getSupportFragmentManager(), "filter_range");
    }

    private void resetAll() {
        findViewById(R.id.chipAll).performClick();
        swLimitedCap.setChecked(false);
        swGeoRequired.setChecked(false);

        fromMillis = null;
        toMillis = null;
        etFrom.setText("");
        etTo.setText("");

        findViewById(R.id.rbNewest).performClick();
    }

    private void applyAndReturn() {
        String status = "All";
        int checkedChipId = chipGroupStatus.getCheckedChipId();
        if (checkedChipId == R.id.chipUpcoming) status = "Upcoming";
        else if (checkedChipId == R.id.chipRegOpen) status = "Registration Open";
        else if (checkedChipId == R.id.chipPast) status = "Past";

        String sort = "Newest";
        int checkedSort = ((android.widget.RadioGroup) findViewById(R.id.rgSort)).getCheckedRadioButtonId();
        if (checkedSort == R.id.rbName) sort = "Name";
        else if (checkedSort == R.id.rbClosingSoon) sort = "ClosingSoon";

        Intent data = new Intent();
        data.putExtra(EXTRA_STATUS, status);
        data.putExtra(EXTRA_LIMITED_CAP, swLimitedCap.isChecked());
        data.putExtra(EXTRA_GEO_REQUIRED, swGeoRequired.isChecked());
        data.putExtra(EXTRA_FROM_MILLIS, fromMillis == null ? -1L : fromMillis);
        data.putExtra(EXTRA_TO_MILLIS, toMillis == null ? -1L : toMillis);
        data.putExtra(EXTRA_SORT, sort);

        setResult(RESULT_OK, data);
        finish();
    }
}