package com.example.breeze_seas;

/**
 * UI-facing model for archived past event history.
 *
 * <p>This is intentionally presentation-focused and does not define a Firestore schema.
 * Demo UI model for archived past event history.
 */
public class PastEventUIModel {

    private final String title;
    private final String dateLabel;
    private final String locationLabel;
    private final String statusLabel;
    private final String detailLabel;
    private final int iconResId;

    public PastEventUIModel(
            String title,
            String dateLabel,
            String locationLabel,
            String statusLabel,
            String detailLabel,
            int iconResId
    ) {
        this.title = title;
        this.dateLabel = dateLabel;
        this.locationLabel = locationLabel;
        this.statusLabel = statusLabel;
        this.detailLabel = detailLabel;
        this.iconResId = iconResId;
    }

    public String getTitle() {
        return title;
    }

    public String getDateLabel() {
        return dateLabel;
    }

    public String getLocationLabel() {
        return locationLabel;
    }

    public String getStatusLabel() {
        return statusLabel;
    }

    public String getDetailLabel() {
        return detailLabel;
    }

    public int getIconResId() {
        return iconResId;
    }
}
