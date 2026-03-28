package com.example.breeze_seas;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;


/**
 * This class allows data to be shared between different fragments
 */
public class SessionViewModel extends ViewModel {
    private final MutableLiveData<String> androidID = new MutableLiveData<>();
    private final MutableLiveData<User> user = new MutableLiveData<>();
    private EventHandler exploreFragmentEventHandler;
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
     * Set the explore fragment EventHandler object.
     * @param eventHandler The EventHandler object pertaining to the explore fragment.
     */
    public void setExploreFragmentEventHandler(EventHandler eventHandler) {
        this.exploreFragmentEventHandler = eventHandler;
    }

    /**
     * Gets the EventHandler object to be used in the explore fragment.
     * @return The EventHandler object pertaining to the explore fragment.
     */
    public EventHandler getExploreFragmentEventHandler() {
        return exploreFragmentEventHandler;
    }

    /**
     * Returns the initialization state of the EventHandler object for the explore fragment.
     * @return Boolean value describing the initialization state of EventHandler
     */
    public boolean exploreFragmentEventHandlerIsInitialized() {
        return exploreFragmentEventHandler != null;
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
