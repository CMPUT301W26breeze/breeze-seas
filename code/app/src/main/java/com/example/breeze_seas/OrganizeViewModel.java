package com.example.breeze_seas;

import androidx.lifecycle.ViewModel;

/**
 * This class allows data to be shared between different fragments
 */
public class OrganizeViewModel extends ViewModel {
    private EventHandler eventHandler;

    /**
     * Set the organize fragment EventHandler object.
     * @param eventHandler The EventHandler object pertaining to the organize fragment.
     */
    public void setEventHandler(EventHandler eventHandler) {
        this.eventHandler = eventHandler;
    }

    /**
     * Gets the EventHandler object to be used in the organize fragment.
     * @return The EventHandler object pertaining to the organize fragment.
     */
    public EventHandler getEventHandler() {
        return eventHandler;
    }

    /**
     * Returns the initialization state of the EventHandler object for the organize fragment.
     * @return Boolean value describing the initialization state of EventHandler
     */
    public boolean eventHandlerIsInitialized() {
        return eventHandler != null;
    }
}