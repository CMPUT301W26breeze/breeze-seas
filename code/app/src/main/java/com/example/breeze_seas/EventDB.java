package com.example.breeze_seas;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class EventDB {

    private static EventDB instance;
    private final FirebaseFirestore db;

    private EventDB() {
        db = FirebaseFirestore.getInstance();
    }

    public static EventDB getInstance() {
        if (instance == null) {
            instance = new EventDB();
        }
        return instance;
    }

    public interface LoadEventsCallback {
        void onSuccess(List<Event> events);
        void onFailure(Exception e);
    }

    public interface AddEventCallback {
        void onSuccess(String eventId);
        void onFailure(Exception e);
    }

    public void addEvent(@NonNull Event event, @NonNull AddEventCallback callback) {
        db.collection("events")
                .add(event.toMap())
                .addOnSuccessListener(documentReference ->
                        callback.onSuccess(documentReference.getId()))
                .addOnFailureListener(callback::onFailure);
    }

    public void getAllEvents(@NonNull LoadEventsCallback callback) {
        db.collection("events")
                .orderBy("regFromMillis")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Event> events = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Event event = Event.fromDocument(doc);
                        if (event != null) {
                            events.add(event);
                        }
                    }
                    callback.onSuccess(events);
                })
                .addOnFailureListener(callback::onFailure);
    }

    public void getEventById(@NonNull String id, @NonNull LoadSingleEventCallback callback) {
        db.collection("events")
                .document(id)
                .get()
                .addOnSuccessListener(doc -> callback.onSuccess(Event.fromDocument(doc)))
                .addOnFailureListener(callback::onFailure);
    }

    public interface LoadSingleEventCallback {
        void onSuccess(Event event);
        void onFailure(Exception e);
    }
}