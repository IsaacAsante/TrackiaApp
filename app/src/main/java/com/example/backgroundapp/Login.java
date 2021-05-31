package com.example.backgroundapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;

public class Login extends AppCompatActivity {

    private SharedPreferences sharedPreferences;

    private FirebaseAuth mAuth;

    private EditText email, password;
    private Button submit;

    private String emailVal, passwordVal;

    @Override
    protected void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra(Constants.CURRENT_USER, currentUser.getUid());
            startActivity(intent);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Login Form fields
        email = findViewById(R.id.EditText_username);
        password = findViewById(R.id.EditText_password);
        submit = findViewById(R.id.Button_submit);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Login Form field values
                emailVal = email.getText().toString().trim();
                passwordVal = password.getText().toString().trim();

                if (emailVal.isEmpty()) {
                    Toast.makeText(Login.this, R.string.enter_email_again, Toast.LENGTH_SHORT).show();
                } else if
                (passwordVal.isEmpty()) {
                    Toast.makeText(Login.this, R.string.enter_password_again, Toast.LENGTH_SHORT).show();
                } else {
                    mAuth.signInWithEmailAndPassword(emailVal, passwordVal)
                            .addOnCompleteListener(Login.this, new OnCompleteListener<AuthResult>() {
                                @Override
                                public void onComplete(@NonNull Task<AuthResult> task) {
                                    if (task.isSuccessful()) {
                                        // Sign in success, update UI with the signed-in user's information
                                        FirebaseUser user = mAuth.getCurrentUser();
                                        Log.i("LOGIN_RESPONSE", user.toString());
                                        Toast.makeText(Login.this, R.string.enter_password_again, Toast.LENGTH_SHORT).show();
                                        final Intent intent = new Intent(Login.this, MainActivity.class);
                                        intent.putExtra(Constants.CURRENT_USER, user.getUid());
                                        DocumentReference docRef = FireStoreDB.getUserRef(user.getUid());
                                        // Load details from DB
                                        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                            @Override
                                            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                                if (task.isSuccessful()) {
                                                    DocumentSnapshot document = task.getResult();
                                                    if (document.exists()) {
                                                        Log.i("USER FOUND", "DocumentSnapshot data: " + document.getData());
                                                        // Save data in SharedPreferences
                                                        sharedPreferences = getSharedPreferences(user.getUid(), Context.MODE_PRIVATE);
                                                        SharedPreferences.Editor editor = sharedPreferences.edit();
                                                        editor.putString("lastLogin", Long.toString(System.currentTimeMillis()));
                                                        editor.putString("firstname", document.getString("firstname"));
                                                        editor.putString("role", document.getString("role"));
                                                        editor.putString("gender",document.getString("gender"));
                                                        editor.putString("city", document.getString("city"));
                                                        editor.putString("pinLocation", document.getString("pinLocation"));
                                                        editor.putString("localState", document.getString("localState"));
                                                        editor.putString("lastname", document.getString("lastname"));
                                                        editor.putString("houseAddress", document.getString("houseAddress"));
                                                        editor.putString("governmentID", document.getString("governmentID"));
                                                        editor.putString("contact", document.getString("contact"));
                                                        editor.putString("email", document.getString("email"));
                                                        editor.commit();
                                                        // Once the user's data's been saved locally, go to the main screen.
                                                        startActivity(intent);
                                                    } else {
                                                        Log.i("USER FOUND", "No such document");
                                                    }
                                                } else {
                                                    Log.d("USER FOUND", "get failed with ", task.getException());
                                                }
                                            }
                                        });
                                    } else {
                                        // If sign in fails, display a message to the user.
                                        Log.w("LOGIN_RESPONSE", "signInWithEmail:failure", task.getException());
                                        Toast.makeText(Login.this, R.string.authentication_failed_try_again, Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                }
            }
        });
    }
}