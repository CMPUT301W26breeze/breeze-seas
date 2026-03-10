package com.example.breeze_seas;

import com.google.firebase.Timestamp;

/*
* Primary model for all the users of the app.
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
        if (!email.contains("@")) {
            throw new IllegalArgumentException("An email has to contain and @ symbol");
        }
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
        this.phoneNumber = phoneNumber;
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
}
