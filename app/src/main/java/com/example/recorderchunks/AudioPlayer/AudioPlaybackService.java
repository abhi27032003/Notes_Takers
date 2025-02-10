package com.example.recorderchunks.AudioPlayer;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;

import com.example.recorderchunks.R;

public class AudioPlaybackService extends Service {
    private AudioPlayerManager playerManager;
    private AudioPlayerViewModel viewModel;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        viewModel = new ViewModelProvider((ViewModelStoreOwner) getApplicationContext()).get(AudioPlayerViewModel.class);
        playerManager = AudioPlayerManager.getInstance(viewModel);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        String recordingId = intent.getStringExtra("RECORDING_ID");
        String filePath = intent.getStringExtra("FILE_PATH");

        if ("PLAY".equals(action)) {
            playerManager.playRecording(recordingId, filePath);
        } else if ("PAUSE".equals(action)) {
            playerManager.pauseRecording();
        } else if ("STOP".equals(action)) {
            playerManager.stopRecording();
            stopSelf();
        }

        showNotification(recordingId);
        return START_STICKY;
    }

    private void createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            CharSequence name = "Audio Playback";
            String description = "Notification for audio playback service";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel("audio_channel", name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void showNotification(String recordingId) {
        String playbackState = playerManager.isPlaying() ? "Playing" : "Paused";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "audio_channel")
                .setSmallIcon(R.mipmap.mic)
                .setContentTitle("Audio Playback")
                .setContentText(playbackState + "current audio")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .addAction(R.mipmap.pause, "Pause",
                        getPendingIntent("PAUSE", recordingId))
                .addAction(R.mipmap.play, "Stop",
                        getPendingIntent("STOP", recordingId))
                .setOngoing(true);

        startForeground(1, builder.build());
    }

    private PendingIntent getPendingIntent(String action, String recordingId) {
        Intent intent = new Intent(this, AudioPlaybackService.class);
        intent.setAction(action);
        intent.putExtra("RECORDING_ID", recordingId);
        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
