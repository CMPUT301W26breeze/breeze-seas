package com.example.breeze_seas;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;

/**
 * Ensures the app has a Firebase auth session before Firestore access.
 *
 * <p>The project currently does not expose a user-facing Firebase Auth flow, so
 * anonymous auth is used as the minimum viable session for Firestore rules that
 * require {@code request.auth != null}.
 */
public final class FirebaseSession {

    private static final String TAG = "FirebaseSession";

    private FirebaseSession() {
    }

    /**
     * Receives the result of the anonymous-auth bootstrap used before Firestore access.
     */
    public interface OnReadyListener {
        void onReady();
        void onError(@NonNull Exception e);
    }

    public static void ensureAuthenticated(@NonNull OnReadyListener listener) {
        FirebaseAuth auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() != null) {
            listener.onReady();
            return;
        }

        auth.signInAnonymously()
                .addOnSuccessListener(authResult -> {
                    Log.d(TAG, "Anonymous Firebase auth successful");
                    listener.onReady();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Anonymous Firebase auth failed", e);
                    listener.onError(e);
                });
    }
}
