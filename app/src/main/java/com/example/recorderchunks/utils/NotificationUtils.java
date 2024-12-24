package com.example.recorderchunks.utils;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.core.app.NotificationCompat;

public class NotificationUtils {

    private static final String CHANNEL_ID = "RECORDING_CHANNEL";
    private static final String CHANNEL_NAME = "Recording Notifications";

    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Notification when recording starts");
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    public static void showRecordingNotification(Context context) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setContentTitle("Recording Started")
                .setContentText("Your audio recording is in progress.")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setOngoing(true) // Prevent the user from swiping it away
                .build();

        if (notificationManager != null) {
            notificationManager.notify(1, notification);
        }
    }

    public static void cancelRecordingNotification(Context context) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.cancel(1);
        }
    }
}
