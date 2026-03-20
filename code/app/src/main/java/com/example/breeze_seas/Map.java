package com.example.breeze_seas;

import com.google.firebase.Firebase;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.osmdroid.util.GeoPoint;

import java.util.ArrayList;

public class Map {
    private ArrayList<GeoPoint> location;
    private FirebaseFirestore db;
    private Event event;


    public interface FetchedLocationListener{
        void onLocationFetched(ArrayList<GeoPoint> location);
        void onFailure(Error e);
    }

    public Map(Event event){
        this.event=event;
        this.db=DBConnector.getDb();
        this.location=new ArrayList<>();
    }

    public void fetchLocation(FetchedLocationListener listener){
        CollectionReference listRef=db.collection("events").document(event.getEventId()).collection("participants");
        listRef.get().addOnSuccessListener(task->{
            if(!task.isEmpty()){
                for(QueryDocumentSnapshot doc:task){
                    com.google.firebase.firestore.GeoPoint firestoreLocation=doc.getGeoPoint("location");
                    if(firestoreLocation!=null){
                        org.osmdroid.util.GeoPoint mapPoint= new org.osmdroid.util.GeoPoint(firestoreLocation.getLatitude(),firestoreLocation.getLongitude());
                        this.location.add(mapPoint);
                    }
                }
            }
            listener.onLocationFetched(this.location);
        }).addOnFailureListener(e->{
                listener.onFailure(new Error(e.getMessage()));}
        );

    }

}
