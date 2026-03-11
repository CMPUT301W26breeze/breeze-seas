package com.example.breeze_seas;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;


/**
 * This class allows data to be shared between different fragments
 */
public class SessionViewModel extends ViewModel {
    private final MutableLiveData<String> androidID = new MutableLiveData<>();
    private final MutableLiveData<UserDB> userDBInstance = new MutableLiveData<>();
    private final MutableLiveData<User> user = new MutableLiveData<>();
    private final MutableLiveData<Event> eventShown = new MutableLiveData<>();

    /**
     * Returns the current android ID
     *
     * @return String of current device AndroidID
     */
    public MutableLiveData<String> getAndroidID() {
        return androidID;
    }

    /**
     * Set android ID
     * @param id The id to set the Android ID as
     */
    public void setAndroidID(String id) {
        androidID.setValue(id);
    }

    /**
     * Returns the UserDB object for query/update operations
     * @return The UserDB instance
     */
    public MutableLiveData<UserDB> getUserDBInstance() {
        return userDBInstance;
    }

    /**
     * Set UserDB
     * @param dbInstance The UserDB Object to be stored
     */
    public void setUserDBInstance(UserDB dbInstance) {
        userDBInstance.setValue(dbInstance);
    }

    /**
     * Return the curernt user object
     * @return The user object to set as current user
     */
    public MutableLiveData<User> getUser() {
        return user;
    }

    /**
     * Set the current user object
     * @param userInstance The user object pertaining to the current user
     */
    public void setUser(User userInstance) {
        user.setValue(userInstance);
    }

    /**
     * Gets the event object that will be presented in the EventDetails Fragment
     * @return The event object to be displayed
     */
    public MutableLiveData<Event> getEventShown() {
        return eventShown;
    }

    /**
     * Sets the event object for EventDetailsFragment to display
     * @param eventShown The event object to show
     */
    public void setEventShown(Event eventShown) {
        this.eventShown.setValue(eventShown);
    }
}
