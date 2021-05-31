package com.example.backgroundapp;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Map;

public class FireStoreDB {
    private FireStoreDB() {};
    private static final FirebaseFirestore db = FirebaseFirestore.getInstance();

    // Retrieve a single user from the DB
    private static void getUser(String uid) {
        DocumentReference docRef = db.collection("cities").document("SF");
        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        Log.i("USER FOUND", "DocumentSnapshot data: " + document.getData());
                    } else {
                        Log.i("USER FOUND", "No such document");
                    }
                } else {
                    Log.d("USER FOUND", "get failed with ", task.getException());
                }
            }
        });
    }
}
