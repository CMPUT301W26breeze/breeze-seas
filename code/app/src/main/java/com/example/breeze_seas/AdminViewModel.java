package com.example.breeze_seas;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * ViewModel for admin screens.
 *
 * <p>A separate ViewModel from {@link SessionViewModel} because admin data (images, events,
 * profiles) is unrelated to the current-user session state. Keeping them together would give
 * SessionViewModel too many responsibilities.</p>
 *
 * <p>This ViewModel is scoped to the activity. Listeners are started only when an
 * admin browse screen is first opened and remain active for the rest of the activity's
 * lifetime, even when the user navigates away from admin screens. This means data stays fresh
 * and no re-fetch is needed when returning to an admin screen. Listeners are only stopped
 * when the activity finishes and {@link #onCleared()} is called.</p>
 */
public class AdminViewModel extends ViewModel {

    // Images

    private final MutableLiveData<List<Image>> images = new MutableLiveData<>(new ArrayList<>());
    private boolean imagesListening = false;

    /** Returns the live image list. */
    public MutableLiveData<List<Image>> getImages() {
        return images;
    }

    /** Starts the realtime images listener if not already running. */
    public void startImagesListen() {
        if (imagesListening) return;
        imagesListening = true;

        ImageDB.startImagesListen(new ImageDB.ImagesChangedCallback() {
            @Override
            public void onImageAdded(Image image) {
                List<Image> current = images.getValue();
                current.add(image);
                images.postValue(current);
            }

            @Override
            public void onImageModified(Image image) {
                List<Image> current = images.getValue();
                for (int i = 0; i < current.size(); i++) {
                    if (image.getImageId().equals(current.get(i).getImageId())) {
                        current.set(i, image);
                        break;
                    }
                }
                images.postValue(current);
            }

            @Override
            public void onImageRemoved(Image image) {
                List<Image> current = images.getValue();
                current.removeIf(img -> img.getImageId().equals(image.getImageId()));
                images.postValue(current);
            }

            @Override
            public void onFailure(Exception e) {

            }
        });
    }

    /**
     * Deletes an image from Firestore. The listener removes it from the live list automatically.
     */
    public void deleteImage(Image image, ImageDB.ImageMutationCallback callback) {
        ImageDB.deleteImage(image.getImageId(), callback);
    }

    // Events

    private final MutableLiveData<List<Event>> events = new MutableLiveData<>(new ArrayList<>());
    private boolean eventsListening = false;

    /** Returns the live event list. */
    public MutableLiveData<List<Event>> getEvents() {
        return events;
    }

    /** Starts the realtime events listener if not already running. */
    public void startEventsListen() {
        if (eventsListening) return;
        eventsListening = true;

        EventDB.startEventsListen(new EventDB.EventsChangedCallback() {
            @Override
            public void onEventAdded(Event event) {
                List<Event> current = events.getValue();
                current.add(event);
                events.postValue(current);
            }

            @Override
            public void onEventModified(Event event) {
                List<Event> current = events.getValue();
                for (int i = 0; i < current.size(); i++) {
                    if (event.getEventId().equals(current.get(i).getEventId())) {
                        current.set(i, event);
                        break;
                    }
                }
                events.postValue(current);
            }

            @Override
            public void onEventRemoved(Event event) {
                List<Event> current = events.getValue();
                current.removeIf(e -> e.getEventId().equals(event.getEventId()));
                events.postValue(current);
            }

            @Override
            public void onFailure(Exception e) {

            }
        });
    }

    /**
     * Deletes an event and its participants subcollection from Firestore.
     * The listener removes it from the live list automatically once confirmed.
     */
    public void deleteEvent(Event event, EventDB.EventMutationCallback callback) {
        EventDB.deleteEvent(event, callback);
    }

    // Users

    private final MutableLiveData<List<User>> users = new MutableLiveData<>(new ArrayList<>());
    private boolean usersListening = false;
    private UserDB userDB;

    /** Returns the live user list. */
    public MutableLiveData<List<User>> getUsers() {
        return users;
    }

    /** Starts the realtime users listener if not already running. */
    public void startUsersListen() {
        if (usersListening) return;
        usersListening = true;
        userDB = new UserDB();

        userDB.startUsersListen(new UserDB.UsersChangedCallback() {
            @Override
            public void onUserAdded(User user) {
                List<User> current = users.getValue();
                current.add(user);
                users.postValue(current);
            }

            @Override
            public void onUserModified(User user) {
                List<User> current = users.getValue();
                for (int i = 0; i < current.size(); i++) {
                    if (user.getDeviceId().equals(current.get(i).getDeviceId())) {
                        current.set(i, user);
                        break;
                    }
                }
                users.postValue(current);
            }

            @Override
            public void onUserRemoved(User user) {
                List<User> current = users.getValue();
                current.removeIf(u -> u.getDeviceId().equals(user.getDeviceId()));
                users.postValue(current);
            }

            @Override
            public void onFailure(Exception e) {

            }
        });
    }

    /**
     * Deletes a user and all associated data from Firestore.
     * The listener removes them from the live list automatically once confirmed.
     */
    public void deleteUser(String deviceId) {
        if (userDB != null) {
            userDB.deleteUser(deviceId);
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        ImageDB.stopImagesListen();
        EventDB.stopEventsListen();
        if (userDB != null) userDB.stopUsersListen();
    }
}
