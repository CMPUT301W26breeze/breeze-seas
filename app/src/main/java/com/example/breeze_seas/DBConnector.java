package com.example.breeze_seas;

import com.google.firebase.firestore.FirebaseFirestore;

/**
 * DBConnector provides centralized access to the Firebase Firestore database instance.
 *
 * <p>Role in architecture:
 * - Implements the Singleton design pattern to ensure that only one instance
 *   exists and is shared across the application.
 * - Prevents other classes from directly instantiating or accessing Firestore,
 *   enforcing separation of concerns and improving maintainability.
 * - Acts as the single entry point for all Firestore database operations.
 *
 * <p> Outstanding/future work:
 * - Add error handling for database initialization failures.
 * - Extend support for dependency injection if needed for testing or scalability.
 * - Monitor thread-safety if accessed concurrently from multiple components.
 */
public class DBConnector {
    private static FirebaseFirestore db;

    //No one can instantiate DBConnect
    private DBConnector() {}

    // the only way other classes call to talk to Firestore.
    public static FirebaseFirestore getDb() {
        if (db == null) {
            db = FirebaseFirestore.getInstance();
        }
        return db;
    }
}
