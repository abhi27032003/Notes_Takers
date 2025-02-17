package com.example.recorderchunks.AudioPlayer;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;

import com.example.recorderchunks.MyApplication;
import com.example.recorderchunks.R;

import java.io.File;

public class AudioPlaybackService extends Service {
    private static final String CHANNEL_ID = "AudioPlaybackChannel";
    private static final int NOTIFICATION_ID = 1;

    private AudioPlayerManager playerManager;

    @Override
    public void onCreate() {
        super.onCreate();



        playerManager = AudioPlayerManager.getInstance();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        String recordingId = intent.getStringExtra("RECORDING_ID");
        String filePath = intent.getStringExtra("FILE_PATH");

        if ("PLAY".equals(action)) {
            Log.v("playback_test", "Recording played");
            playerManager.playRecording(recordingId, filePath);
            showNotification("Playing Recording...",filePath);
        } else if ("PAUSE".equals(action)) {
            Log.v("playback_test", "Recording paused");
            playerManager.pauseRecording();

        } else if ("STOP".equals(action)) {
            Log.v("playback_test", "Recording stopped");
            playerManager.stopRecording();
            stopservice();
            stopSelf();
        }
        else if ("CANCEL_NOTIFICATION".equals(action)) {
            Log.v("playback_test", "Notification canceled");
            playerManager.stopRecording();
            stopservice();
            stopForeground(true);  // Removes the notification
            stopSelf();  // Stops the service completely
        }

        return START_STICKY;
    }

    private void showNotification(String contentText, String filePath) {
        Intent stopIntent = new Intent(this, AudioPlaybackService.class);
        stopIntent.setAction("STOP");
        PendingIntent stopPendingIntent = PendingIntent.getService(
                this, 2, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Intent cancelIntent = new Intent(this, AudioPlaybackService.class);
        cancelIntent.setAction("CANCEL_NOTIFICATION");
        PendingIntent cancelPendingIntent = PendingIntent.getService(
                this, 3, cancelIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.mic) // Replace with your own icon
                .setContentTitle(contentText)
                .setContentText("Playing " + getFileName(filePath))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .addAction(R.mipmap.stop, "Stop", stopPendingIntent)
                .addAction(R.mipmap.delete, "Remove Notification", cancelPendingIntent); // Add cancel button

        startForeground(NOTIFICATION_ID, notificationBuilder.build());
    }
    public static String getFileName(String filePath) {
        File file = new File(filePath);
        return file.getName();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Audio Playback Service",
                    NotificationManager.IMPORTANCE_LOW
            );

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    public  void stopservice(){

        // Stop the player manager
        if (playerManager != null) {
            playerManager.stopRecording();
        }
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        playerManager.stopRecording();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
