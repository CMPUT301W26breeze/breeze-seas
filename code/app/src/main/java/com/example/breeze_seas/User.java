package com.example.breeze_seas;

import com.google.firebase.Timestamp;

import java.util.Objects;

/**
* Primary model for the users of the app.
 * Stores user data.
 */
public class User {

    private String deviceId;
    private String firstName;
    private String lastName;
    private String userName;
    private String email;
    private String phoneNumber;
    private boolean isAdmin;
    private boolean notificationEnabled;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private String imageDocId;
    private Image profileImage;


    public User() {

        this.deviceId = null;
        this.firstName = null;
        this.lastName = null;
        this.userName = null;
        this.email = null;
        this.phoneNumber = null;
        this.isAdmin = false;
        this.notificationEnabled = true;
        this.createdAt = null;
        this.updatedAt = null;
        this.imageDocId = null;
        this.profileImage = null;
    }

    public User(String deviceId, String firstName, String lastName,
                String userName, String email, boolean isAdmin) {
        this.deviceId = deviceId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.userName = userName;
        this.email = email;
        this.phoneNumber = null;
        this.isAdmin = isAdmin;
        this.notificationEnabled = true;
        this.createdAt = null;
        this.updatedAt = null;
        this.imageDocId = null;
        this.profileImage = null;
    }

    public User(String deviceId, String firstName, String lastName,
                String userName, String email, String phoneNumber, boolean isAdmin) {
        this.deviceId = deviceId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.userName = userName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.isAdmin = isAdmin;
        this.notificationEnabled = true;
        this.createdAt = null;
        this.updatedAt = null;
        this.imageDocId = null;
        this.profileImage = null;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        if (phoneNumber == null) {
            this.phoneNumber = null;
            return;
        }
        this.phoneNumber = phoneNumber.replaceAll("[^0-9]", "");
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    public void setAdmin(boolean admin) {
        isAdmin = admin;
    }

    public boolean notificationEnabled() {
        return notificationEnabled;
    }

    public void setNotificationEnabled(boolean notificationEnabled) {
        this.notificationEnabled = notificationEnabled;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public String getImageDocId() {
        return imageDocId;
    }

    public void setImageDocId(String imageDocId) {
        this.imageDocId = imageDocId;
        if (imageDocId == null || imageDocId.trim().isEmpty()) {
            this.profileImage = null;
        } else if (profileImage != null && !imageDocId.equals(profileImage.getImageId())) {
            this.profileImage = null;
        }
    }

    public Image getProfileImage() {
        return profileImage;
    }

    public void setProfileImage(Image profileImage) {
        this.profileImage = profileImage;
        this.imageDocId = profileImage == null ? null : profileImage.getImageId();
    }

    /**
     * Builds a full name string for the user by combining
    * the first name and last name
    */
    private String buildFullName() {
        String firstName = getFirstName() == null ? "" : getFirstName().trim();
        String lastName = getLastName() == null ? "" : getLastName().trim();
        String fullName = (firstName + " " + lastName).trim();

        if (!fullName.isEmpty()) {
            return fullName;
        }

        return fullName;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof User)) return false;
        User user = (User) o;
        return Objects.equals(deviceId, user.deviceId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(deviceId);
    }
}
