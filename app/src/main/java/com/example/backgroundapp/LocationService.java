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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

public class LocationService extends Service {
    private static double pinLatitude, pinLongitude;
    private static String userUID;
    private LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(@NonNull LocationResult locationResult) {
            super.onLocationResult(locationResult);
            if (locationResult != null && locationResult.getLastLocation() != null) {
                double currentLatitude = locationResult.getLastLocation().getLatitude();
                double currentLongitude = locationResult.getLastLocation().getLongitude();
                Log.i("GPS", "Latitude: " + currentLatitude + " / Longitude: " + currentLongitude);

                Log.i("GPS", "PinLatitude: " + pinLatitude + " / PinLongitude: " + pinLongitude);

                if (pinLatitude > 0.0d && pinLongitude > 0.0d) {
                    double distance = Haversine.getDistance(pinLatitude, pinLongitude, currentLatitude, currentLongitude);
                    Log.i("HAVERSINE DISTANCE", String.valueOf(distance) + " kilometers");
                    if (distance > 0.1) {
                        Log.i("VIOLATION", "User detected outside the boundaries of their Quarantine Zone." +
                                "\nEmailing monitors now with the following details:\n" +
                                "User name: ")
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
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setContentTitle("Tracking Your Location");
        builder.setDefaults((NotificationCompat.DEFAULT_ALL));
        builder.setContentIntent(pendingIntent);
        builder.setAutoCancel(false);
        builder.setPriority(NotificationCompat.PRIORITY_MAX);

        // Notification channels required for Android Oreo and above.
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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
                    String pinLocation = intent.getStringExtra("pinLocation");
                    pinLatitude = Double.parseDouble(pinLocation.split(", ")[0]);
                    pinLongitude = Double.parseDouble(pinLocation.split(", ")[1]);
                    startLocationService();
                } else if (action.equals(Constants.ACTION_STOP_LOCATION_SERVICE)) {
                    stopLocationService();
                }
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }
}
