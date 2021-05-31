package com.example.backgroundapp;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class FireStoreDB {
    private FireStoreDB() {};
    private static final FirebaseFirestore db = FirebaseFirestore.getInstance();

    // Retrieve a single user from the DB
    public static DocumentReference getUserRef(String uid) {
        DocumentReference docRef = db.collection("users").document(uid);
        return docRef;
    }
}
