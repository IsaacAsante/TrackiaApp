package com.example.backgroundapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import android.Manifest;
import android.app.ActivityManager;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_LOCATION_PERMISSION = 100;
    private Executor executor;
    private BiometricPrompt biometricPrompt;
    private BiometricPrompt.PromptInfo promptInfo;

    private FirebaseFirestore db;

    private String userUID;

    // Menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.Logout:
                FirebaseAuth.getInstance().signOut(); // Sign out the user
                Toast.makeText(this, "You have signed out from Trackia. Please sign in again.", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(this, Login.class); // Return to the Login screen
                startActivity(intent);
            case R.id.EndQuarantine:
                clearNotification(Constants.NOTIFICATION_AUTH_ID); // Clear any authentication notification displayed
                WorkManager.getInstance(this).cancelAllWork(); // Stop all authentication reminders
                stopLocationService(); // Terminate the location tracking service
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Firestore
        db = FirebaseFirestore.getInstance();
        userUID = getIntent().getStringExtra(Constants.CURRENT_USER);
        Log.i(Constants.CURRENT_USER, userUID);

        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences(userUID, Context.MODE_PRIVATE);
        Log.i("LAST LOGIN", sharedPreferences.getString("lastLogin", ""));
        Log.i("FIRSTNAME", sharedPreferences.getString("firstname", ""));
        Log.i("ROLE", sharedPreferences.getString("role", ""));
        Log.i("GENDER", sharedPreferences.getString("gender", ""));
        Log.i("CITY", sharedPreferences.getString("city", ""));
        Log.i("PIN_LOCATION", sharedPreferences.getString("pinLocation", ""));
        Log.i("LOCAL_STATE", sharedPreferences.getString("localState", ""));
        Log.i("LAST_NAME", sharedPreferences.getString("lastname", ""));
        Log.i("HOUSE_ADDRESS", sharedPreferences.getString("houseAddress", ""));
        Log.i("GOVERNMENT_ID", sharedPreferences.getString("governmentID", ""));

        executor = ContextCompat.getMainExecutor(this);
        // TODO: Add termination date to the User schema in the DB (Node JS)

        // Enable Worker
        final PeriodicWorkRequest periodicWorkRequest = new PeriodicWorkRequest.Builder(Authenticator.class, Constants.AUTHENTICATION_INTERVAL, TimeUnit.MINUTES).build();
        WorkManager.getInstance(this).enqueue(periodicWorkRequest);
        // TODO: Implement unique requests https://stackoverflow.com/a/50943231/1536240

        // Initalize biometricprompt
        if (biometricPrompt == null) {
            biometricPrompt = new BiometricPrompt(this, executor, new BiometricPrompt.AuthenticationCallback() {
                @Override
                public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                    super.onAuthenticationError(errorCode, errString);
                    Toast.makeText(getApplicationContext(), "Error: Biometric authentication is not available.", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                    super.onAuthenticationSucceeded(result);
                    // Clear the Authentication notification upon successful fingerprint scanning
                    clearNotification(Constants.NOTIFICATION_AUTH_ID);
                    Toast.makeText(getApplicationContext(), "Success: Biometric authentication worked.", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onAuthenticationFailed() {
                    super.onAuthenticationFailed();
                    Toast.makeText(getApplicationContext(), "Failed: Biometric authentication failed.", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Authentication
        findViewById(R.id.buttonAuthenticateWithFingerprint).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkAndAuthenticate();
            }
        });

        // When the user begins tracking
        findViewById(R.id.buttonStartLocationUpdates).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ContextCompat.checkSelfPermission(
                        getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(
                            MainActivity.this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            REQUEST_CODE_LOCATION_PERMISSION
                    );
                } else {
                    startLocationService();
                }
            }
        });

        // When the user stops tracking
        findViewById(R.id.buttonStopLocationUpdates).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopLocationService();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_LOCATION_PERMISSION && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d("PERMISSION RESULTS:", "RequestCode: " + " GrantResults: " + grantResults[0]);
            startLocationService();
        } else {
            Toast.makeText(this, "You must grant location permissions first.", Toast.LENGTH_SHORT).show();
            stopLocationService();
        }
    }

    private boolean isLocationServiceRunning() {
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (activityManager != null) {
            for (ActivityManager.RunningServiceInfo service : activityManager.getRunningServices(Integer.MAX_VALUE)) {
                if (LocationService.class.getName().equals(service.service.getClassName())) {
                    if (service.foreground)
                        return true;
                }
            }
            return false;
        }
        return false;
    }

    private void startLocationService() {
        if (!isLocationServiceRunning()) {
            Intent intent = new Intent(getApplicationContext(), LocationService.class);
            intent.setAction(Constants.ACTION_START_LOCATION_SERVICE);
            startService(intent);
            Toast.makeText(this, "Tracking started.", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopLocationService() {
        if (isLocationServiceRunning()) {
            Intent intent = new Intent(getApplicationContext(), LocationService.class);
            intent.setAction(Constants.ACTION_STOP_LOCATION_SERVICE);
            startService(intent);
            Toast.makeText(this, "Tracking stopped.", Toast.LENGTH_SHORT).show();
        }
    }


    private BiometricPrompt.PromptInfo createBiometricPrompt() {
        return new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Authenticate Yourself")
                .setSubtitle("Submit biometric info")
                .setDescription("Tap your phone's fingerprint sensor now.")
                .setNegativeButtonText("Cancel")
                .build();
    }

    // Verify if the user's device supports fingerprint authentication.
    private void checkAndAuthenticate() {
        BiometricManager biometricManager = BiometricManager.from(this);
        switch (biometricManager.canAuthenticate()) {
            case BiometricManager.BIOMETRIC_SUCCESS:
                BiometricPrompt.PromptInfo promptInfo = createBiometricPrompt();
                biometricPrompt.authenticate(promptInfo);
                break;
            case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE:
                Toast.makeText(getApplicationContext(), "Error: Biometric authentication is not available.", Toast.LENGTH_SHORT).show();
                break;
            case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
                Toast.makeText(getApplicationContext(), "Error: Fingerprint authentication is not supported.", Toast.LENGTH_SHORT).show();
                break;
            case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED:
                Toast.makeText(getApplicationContext(), "Error: No fingerprint enrolled on device.", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    public void clearNotification(int notification_ID) {
        NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(notification_ID);
    }
}

/*
 * Credit to Dhanesh Shetty for his BiometricPrompt tutorial's code: https://medium.com/acmvit/using-biometricprompt-to-implement-fingerprint-authentication-in-android-apps-ebd9681dc0fa
 */