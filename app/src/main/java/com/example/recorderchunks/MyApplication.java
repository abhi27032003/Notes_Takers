package com.example.recorderchunks;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Build;

import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStore;
import androidx.lifecycle.ViewModelStoreOwner;

import com.example.recorderchunks.Activity.CrashReportActivity;
import com.example.recorderchunks.utils.CustomExceptionHandler;
import com.github.anrwatchdog.ANRError;
import com.github.anrwatchdog.ANRWatchDog;

public class MyApplication extends Application implements ViewModelStoreOwner {

    private ViewModelStore viewModelStore;
    private ViewModelProvider viewModelProvider;
    private static MyApplication instance;


    @Override
    public void onCreate() {
        super.onCreate();

        new ANRWatchDog(5000)
                .setANRListener(new ANRWatchDog.ANRListener() {
                    @Override
                    public void onAppNotResponding(ANRError error) {
                        // Log the error, save it, or attempt to start a reporting activity
                        // Note: The app may be in an unstable state here.
                        error.printStackTrace();

                        // If you wish to start your CrashReportActivity, you can try:
                        Intent intent = new Intent(getApplicationContext(), CrashReportActivity.class);
                        intent.putExtra("anr_error", error.getMessage());
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        getApplicationContext().startActivity(intent);

                        // Optionally, kill the process if you want to prevent further issues.
                        // android.os.Process.killProcess(android.os.Process.myPid());
                        // System.exit(10);
                    }
                })
                .start();
        Thread.setDefaultUncaughtExceptionHandler(
                new CustomExceptionHandler(getApplicationContext(), CrashReportActivity.class)
        );
        // Initialize the ViewModelStore for app-level scope
        instance = this; // Set the global instance

        viewModelStore = new ViewModelStore();
        viewModelProvider = new ViewModelProvider(this, new ViewModelProvider.AndroidViewModelFactory(this));
        createNotificationChannel();
    }
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "AUDIO_CHANNEL",
                    "Audio Playback",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
    public ViewModelProvider getViewModelProvider() {
        return viewModelProvider;
    }

    @Override
    public ViewModelStore getViewModelStore() {
        return viewModelStore;
    }

    public static MyApplication getInstance() {
        return instance;
    }
}
