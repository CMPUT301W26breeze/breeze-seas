package com.example.breeze_seas;

import static com.example.breeze_seas.DBConnector.getDb;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.Firebase;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

/*** ExploreFragment is a top-level destination accessible via Bottom Navigation.
 *
 * <p>Current state:* - A placeholder screen was created to validate the navigation wiring and theme style.
 ** <p>Outstanding/Future Work:* - Replace the placeholder UI with the Explore feature implementation.
 */
public class ExploreFragment extends Fragment {

    private ExtendedFloatingActionButton ScanQRCodeBtn;

    public ExploreFragment() {
        super(R.layout.fragment_explore);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Fetch events from DB
        FirebaseFirestore db = getDb();
        CollectionReference eventsRef = db.collection("Events");

        // Fetch events
        
    }
}
