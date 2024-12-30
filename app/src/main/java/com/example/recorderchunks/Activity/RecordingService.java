package com.example.recorderchunks.Activity;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.recorderchunks.Model_Class.RecordingViewModel;
import com.example.recorderchunks.Model_Class.recording_event_no;
import com.example.recorderchunks.MyApplication;
import com.example.recorderchunks.R;
import com.example.recorderchunks.utils.RecordingManager;
import com.example.recorderchunks.utils.RecordingUtils;

public class RecordingService extends Service {

    public static final String CHANNEL_ID = "RecordingChannel";
    public static final String ACTION_TOGGLE_PAUSE_RESUME = "ACTION_TOGGLE_PAUSE_RESUME";
    public static final String ACTION_STOP = "ACTION_STOP";
    private RecordingUtils recordingUtils;
    private RecordingViewModel recordingViewModel;
    private recording_event_no recordingEventNo;
    private boolean isPaused = false;
    public static  boolean isStopping = false;
    private Handler notificationUpdateHandler = new Handler();
    private Runnable notificationUpdateRunnable;

    @Override
    public void onCreate() {
        super.onCreate();
        MyApplication app = (MyApplication) getApplication();
        recordingViewModel = new ViewModelProvider(
                app,
                ViewModelProvider.AndroidViewModelFactory.getInstance(app)
        ).get(RecordingViewModel.class);
        recordingUtils = RecordingManager.getInstance(this, new RecordingUtils.RecordingCallback() {
            @Override
            public void onRecordingStarted(String filePath) {
                startNotificationUpdates();
            }

            @Override
            public void onRecordingSaved(int eventId) {
                stopNotificationUpdates();
            }

            @Override
            public void onRecordingPaused() {
                stopNotificationUpdates();
            }

            @Override
            public void onRecordingResumed() {
                startNotificationUpdates();
            }
        });
        recordingEventNo = new recording_event_no();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (isStopping) {
            // Prevent restarting if the service is being stopped
            stopSelf();
            return START_NOT_STICKY; // Ensure the service doesn't restart
        }

        if (intent != null && intent.getAction() != null) {
            String action = intent.getAction();
            switch (action) {
                case ACTION_TOGGLE_PAUSE_RESUME:
                    if (isPaused) {
                        resumeRecording();
                    } else {
                        pauseRecording();
                    }
                    break;
                case ACTION_STOP:
                    stopRecording();
                    return START_NOT_STICKY; // Ensure service does not restart
            }
        }

        // Start foreground service and update notification if not stopping
        startForeground(1, createNotification());
        if(!isStopping)
        {
            startNotificationUpdates();
        }
        return START_STICKY;
    }

    private Notification createNotification() {
        createNotificationChannel();

        // Toggle Pause/Resume button
        Intent toggleIntent = new Intent(this, RecordingService.class).setAction(ACTION_TOGGLE_PAUSE_RESUME);
        PendingIntent togglePendingIntent = PendingIntent.getService(
                this, 0, toggleIntent, PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_MUTABLE);

        // Stop button
        Intent stopIntent = new Intent(this, RecordingService.class).setAction(ACTION_STOP);
        PendingIntent stopPendingIntent = PendingIntent.getService(
                this, 1, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_MUTABLE);

        String toggleActionText = isPaused ? "Resume" : "Pause";


        // Timer text using `getElapsedSeconds()`
        String timerText = formatTime(recordingViewModel.getElapsedSeconds().getValue());

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Recording in Progress")
                .setContentText(timerText) // Display the timer
                .setSmallIcon(R.mipmap.collapse) // Replace with your drawable resource
                .addAction(R.drawable.baseline_add_24, toggleActionText,togglePendingIntent) // Play/Pause button
                .addAction(R.drawable.baseline_add_24, "Stop", stopPendingIntent) // Stop button
                .setOngoing(true)
                .build();
    }

    private void createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Recording Channel", NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void pauseRecording() {
        recordingUtils.pauseRecording();
        recordingViewModel.setPaused(true);
        recordingViewModel.setRecording(false);
        recordingViewModel.pauseTimer();
        isPaused = true;
        startNotificationUpdates();
        updateNotification();
    }

    private void resumeRecording() {
        recordingUtils.resumeRecording();
        recordingViewModel.setPaused(false);
        recordingViewModel.setRecording(true);
        recordingViewModel.resumeTimer();
        isPaused = false;
        startNotificationUpdates();
        updateNotification();
    }

    private void stopRecording() {

        recordingUtils.stopRecording(recordingEventNo.getRecording_event_no());
        recordingViewModel.setRecording(false);
        recordingViewModel.setPaused(false);
        recordingViewModel.resetTimer();
        isStopping = true; // Set the flag to indicate stopping
        stopNotificationUpdates();
        stopSelf();
        stopForeground(true); // Stop the foreground service
        cancelNotification(); // Cancel the notification explicitly


    }

    private void updateNotification() {
        if (!isStopping) { // Only update if the service is not stopping
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.notify(1, createNotification());
            }
        }
    }

    private void startNotificationUpdates() {

        notificationUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                updateNotification();
                notificationUpdateHandler.postDelayed(this, 1000); // Update every second
            }
        };
        notificationUpdateHandler.post(notificationUpdateRunnable);
    }

    private void stopNotificationUpdates() {
        notificationUpdateHandler.removeCallbacks(notificationUpdateRunnable);
    }

    private String formatTime(int elapsedSeconds) {
        int seconds = elapsedSeconds % 60;
        int minutes = (elapsedSeconds / 60) % 60;
        int hours = (elapsedSeconds / 3600);
        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%02d:%02d", minutes, seconds);
        }
    }
    private void cancelNotification() {
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.cancel(1); // Cancel the notification with ID 1
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
