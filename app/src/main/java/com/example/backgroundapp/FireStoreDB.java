package com.example.backgroundapp;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class FireStoreDB {
    private static final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static String currentUserUID;

    private FireStoreDB() {};
    // Retrieve a single user from the DB
    public static DocumentReference getUserRef(String uid) {
        DocumentReference docRef = db.collection("users").document(uid);
        return docRef;
    }

    public static CollectionReference getCollectionRef(String source) {
        CollectionReference collectionRef = db.collection(source);
        return collectionRef;
    }

    public static String getCurrentUserUID() {
        return currentUserUID;
    }

    public static void setCurrentUserUID(String id) {
        currentUserUID = id;
    }
}
