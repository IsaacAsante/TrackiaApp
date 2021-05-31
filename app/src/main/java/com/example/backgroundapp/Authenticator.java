package com.example.backgroundapp;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.biometric.BiometricPrompt;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.concurrent.Executor;


public class Authenticator extends Worker {

    private Executor executor;
    private BiometricPrompt biometricPrompt;
    private BiometricPrompt.PromptInfo promptInfo;

    public Authenticator(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        executor = ContextCompat.getMainExecutor(context);
    }

    @NonNull
    @Override
    public Result doWork() {
        displayNotification(getApplicationContext().getString(R.string.auth_notification_title), getApplicationContext().getString(R.string.auth_notification_task));
        return Result.success();
    }

    // Generate notification
    private void displayNotification(String title, String task) {
        NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(Constants.NOTIFICATION_AUTH_TITLE, Constants.NOTIFICATION_AUTH_TITLE, NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder notification = new NotificationCompat.Builder(getApplicationContext(), Constants.NOTIFICATION_AUTH_TITLE)
                .setContentTitle(title)
                .setContentText(task)
                .setSmallIcon(R.mipmap.trackia_logo);

        notificationManager.notify(Constants.NOTIFICATION_AUTH_ID, notification.build());
    }
}