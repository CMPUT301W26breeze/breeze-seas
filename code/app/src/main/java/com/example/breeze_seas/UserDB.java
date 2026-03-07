package com.example.breeze_seas;

import android.util.Log;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class UserDB {

    private FirebaseFirestore db;
    private CollectionReference usersRef;

    public UserDB() {
        // Access the shared Firestore instance from your connector
        this.db = DBConnector.getDb();
        // Point to your "users" collection
        this.usersRef = db.collection("User");
    }

    /* Adds the newly created user to the database.*/
    public void saveUser(User user) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("Name",user.getUserName());
        userData.put("Email",user.getEmail());
        userData.put("Phone Number", user.getPhoneNumber());
        userData.put("DeviceId",user.getDeviceId());
        userData.put("IsAdmin", user.isAdmin());
        userData.put("Timestamp", FieldValue.serverTimestamp());
        usersRef.document(user.getDeviceId()).set(userData)
                .addOnSuccessListener(aVoid ->
                        Log.d("DB_UPDATE", "Update successful"))
                .addOnFailureListener(e ->
                        Log.e("DB_UPDATE", "Update failed", e));
    }

    public void deleteUser(String deviceId) {
        usersRef.document(deviceId).delete()
                .addOnSuccessListener(aVoid ->
                        Log.d("DB_UPDATE", "Update successful"))
                .addOnFailureListener(e ->
                        Log.e("DB_UPDATE", "Update failed", e));

    }





}
