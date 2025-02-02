package com.example.recorderchunks.ManageLogs;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.work.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class AppLogger {
    private static AppLogger instance;
    private final File logDirectory;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    private final SimpleDateFormat fileDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private static SharedPreferences prefs_uuid;
    private final String uuid;

    private AppLogger(Context context) {
        logDirectory = new File(context.getExternalFilesDir(null), "logs");
        if (!logDirectory.exists()) {
            logDirectory.mkdirs();
        }

        this.prefs_uuid = context.getSharedPreferences("preferences", Context.MODE_PRIVATE);
        this.uuid = prefs_uuid.getString("uuid", "UUID.randomUUID().toString()");

        scheduleBackgroundUpload(context);
    }

    public static synchronized AppLogger getInstance(Context context) {
        if (instance == null) {
            instance = new AppLogger(context);
        }
        return instance;
    }

    public void addLog(String message) {
        String timestamp = dateFormat.format(new Date());
        String logFileName = fileDateFormat.format(new Date()) + ".txt";
        File logFile = new File(logDirectory, logFileName);
        String logEntry = timestamp + " - " + message + "\n";

        try (FileWriter writer = new FileWriter(logFile, true)) {
            writer.append(logEntry);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void scheduleBackgroundUpload(Context context) {
        WorkRequest uploadWorkRequest = new PeriodicWorkRequest.Builder(
                LogUploadWorker.class, 15, TimeUnit.MINUTES)
                .setConstraints(new Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED) // Upload only when online
                        .build())
                .build();

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "LogUploadWorker", ExistingPeriodicWorkPolicy.KEEP, (PeriodicWorkRequest) uploadWorkRequest);
    }
}
