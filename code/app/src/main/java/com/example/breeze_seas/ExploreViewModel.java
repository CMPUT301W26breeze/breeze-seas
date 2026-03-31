package com.example.breeze_seas;

import androidx.lifecycle.ViewModel;

/**
 * This class allows data to be shared between different fragments
 */
public class ExploreViewModel extends ViewModel {
    private EventHandler exploreFragmentEventHandler;

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
}
