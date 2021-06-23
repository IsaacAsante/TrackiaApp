package com.example.backgroundapp;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;

import java.util.HashMap;
import java.util.Map;

public class LocationService extends Service {
    private static double pinLatitude, pinLongitude;
    private static String userUID, user_firstname, user_lastname, user_contact, user_violationTime, user_email, user_governmentID;
    private static boolean isMock;
    private static SharedPreferences sharedPreferences;
    private LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(@NonNull LocationResult locationResult) {
            super.onLocationResult(locationResult);
            sharedPreferences = getApplicationContext().getSharedPreferences(FireStoreDB.getCurrentUserUID(), Context.MODE_PRIVATE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                isMock = locationResult.getLastLocation().isFromMockProvider();
                if (isMock) {
                    Toast.makeText(getApplicationContext(), "Mock Location detected.", Toast.LENGTH_SHORT).show();
                    Log.i("MOCK PROVIDER", String.valueOf(isMock));
                    // Get user details for the spoofing violation
                    Map<String, Object> violation_spoofing = new HashMap<>();
                    violation_spoofing.put("governmentID", sharedPreferences.getString("governmentID", ""));
                    violation_spoofing.put("firstname", sharedPreferences.getString("firstname", ""));
                    violation_spoofing.put("lastname", sharedPreferences.getString("lastname", ""));
                    violation_spoofing.put("contact", sharedPreferences.getString("contact", ""));
                    violation_spoofing.put("email", sharedPreferences.getString("email", ""));
                    violation_spoofing.put("houseAddress", sharedPreferences.getString("houseAddress", ""));
                    violation_spoofing.put("pinLocation", sharedPreferences.getString("pinLocation", ""));
                    violation_spoofing.put("localState", sharedPreferences.getString("localState", ""));
                    violation_spoofing.put("city", sharedPreferences.getString("city", ""));
                    violation_spoofing.put("gender", sharedPreferences.getString("gender", ""));
                    violation_spoofing.put("detection", System.currentTimeMillis());

                    Log.i("GPS SPOOFING DETECTED", violation_spoofing.toString());

                    // Add violation to the DB
                    FireStoreDB.getCollectionRef(Constants.FIRESTORE_VIOLATIONS_SPOOFING_COLLECTIONS)
                            .add(violation_spoofing)
                            .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                @Override
                                public void onSuccess(DocumentReference documentReference) {
                                    Log.i("SUCCESS", "Spoofing violation updated in the Database");
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Log.i("RETRY", "DB error");
                                }
                            });
                } else {
                    if (locationResult != null && locationResult.getLastLocation() != null) {
                        double currentLatitude = locationResult.getLastLocation().getLatitude();
                        double currentLongitude = locationResult.getLastLocation().getLongitude();
                        Log.i("GPS", "Latitude: " + currentLatitude + " || Longitude: " + currentLongitude);

                        Log.i("GPS", "PinLatitude: " + pinLatitude + " || PinLongitude: " + pinLongitude);

                        if (pinLatitude > 0.0d && pinLongitude > 0.0d) {
                            double distance = Haversine.getDistance(pinLatitude, pinLongitude, currentLatitude, currentLongitude);
                            Log.i("HAVERSINE DISTANCE", String.valueOf(distance) + " kilometers");
                            if (distance > 0.1) {
                                Log.i("VIOLATION", "User detected outside the boundaries of their Quarantine Zone." +
                                        "\nGenerating alert in the Trackia system with following details: " +
                                        "\nName: " + user_firstname + " " + user_lastname +
                                        "\nEmail: " + user_email +
                                        "\nContact number: " + user_contact +
                                        "\nViolation recorded at: " + System.currentTimeMillis() + " (Current time in milliseconds, to be converted in Date format)" +
                                        "\nViolation location on Google Maps: " + " https://www.google.com/maps/search/" + currentLatitude + "," + currentLongitude +
                                        "\nAllowed Quarantine Zone: " + "https://www.google.com/maps/search/" + pinLatitude + "," + pinLongitude);
                            }
                        }
                    }
                }
            }
        }
    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void startLocationService() {
        String channelID = "Location_Notification_Channel";
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Intent resultIntent = new Intent();
        PendingIntent pendingIntent = PendingIntent.getActivity(
                getApplicationContext(),
                0,
                resultIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
        );
        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                getApplicationContext(),
                channelID
        );
        builder.setSmallIcon(R.mipmap.trackia_logo);
        builder.setContentTitle("Tracking Your Location");
        builder.setDefaults((NotificationCompat.DEFAULT_ALL));
        builder.setContentIntent(pendingIntent);
        builder.setAutoCancel(false);
        builder.setPriority(NotificationCompat.PRIORITY_MAX);

        // Notification channels required for Android Oreo and above.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (notificationManager != null
                    && notificationManager.getNotificationChannel(channelID) == null) {
                NotificationChannel notificationChannel = new NotificationChannel(
                        channelID,
                        "Location_Notification_Channel",
                        notificationManager.IMPORTANCE_HIGH
                );
                notificationChannel.setDescription("Tracking your location. Please remain self-quarantined.");
                notificationManager.createNotificationChannel(notificationChannel);
            }
        }

        // Location request details
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(2000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationServices.getFusedLocationProviderClient(this)
                .requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());

        startForeground(Constants.LOCATION_SERVICE_ID, builder.build());
    }

    private void stopLocationService() {
        LocationServices.getFusedLocationProviderClient(this)
                .removeLocationUpdates(locationCallback);
        stopForeground(true);
        stopSelf();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (action != null) {
                if (action.equals(Constants.ACTION_START_LOCATION_SERVICE)) {
                    SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences(userUID, Context.MODE_PRIVATE);
                    // Get Quarantine Zone location
                    String pinLocation = intent.getStringExtra("pinLocation");
                    pinLatitude = Double.parseDouble(pinLocation.split(", ")[0]);
                    pinLongitude = Double.parseDouble(pinLocation.split(", ")[1]);
                    startLocationService();
                    // Save user details;
                    user_contact = intent.getStringExtra("contact");
                    user_email = intent.getStringExtra("email");
                    user_firstname = intent.getStringExtra("firstname");
                    user_lastname = intent.getStringExtra("lastname");
                    user_governmentID = intent.getStringExtra("governmentID");
                } else if (action.equals(Constants.ACTION_STOP_LOCATION_SERVICE)) {
                    stopLocationService();
                }
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }
}
