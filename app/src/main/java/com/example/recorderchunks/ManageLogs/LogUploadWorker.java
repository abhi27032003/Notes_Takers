package com.example.recorderchunks.ManageLogs;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import okhttp3.*;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public class LogUploadWorker extends Worker {
    private static SharedPreferences prefs_uuid;
    private String uuid;
    private static final String SERVER_URL = "https://notetakers.vipresearch.ca/App_Script/sendlogs.php";
    private static final long FIVE_HOURS_MILLIS = 5 * 60 * 60 * 1000; // 5 hours in milliseconds

    public LogUploadWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        File logDirectory = new File(getApplicationContext().getExternalFilesDir(null), "logs");
        prefs_uuid = getApplicationContext().getSharedPreferences("preferences", Context.MODE_PRIVATE);
        uuid = prefs_uuid.getString("uuid", UUID.randomUUID().toString());


        if (!logDirectory.exists() || !logDirectory.isDirectory()) {
            Log.e("LogUploadWorker", "Log directory does not exist.");
            return Result.failure();
        }

        File[] files = logDirectory.listFiles();
        if (files == null || files.length == 0) {
            Log.d("LogUploadWorker", "No logs to upload.");
            return Result.success();
        }

        long currentTime = System.currentTimeMillis();

        for (File file : files) {
            if (currentTime - file.lastModified() <= FIVE_HOURS_MILLIS) {
                String timestampedName = generateTimestampedFileName();
                File renamedFile = new File(logDirectory, timestampedName);

                if (file.renameTo(renamedFile)) {
                    Log.d("LogUploadWorker", "Renamed file to: " + renamedFile.getName());
                    if (uploadFile(renamedFile,uuid)) {
                        renamedFile.delete(); // Delete after successful upload
                    }
                } else {
                    Log.e("LogUploadWorker", "Failed to rename file.");
                }
            }
        }

        return Result.success();
    }

    private boolean uploadFile(File logFile,String uuid) {
        OkHttpClient client = new OkHttpClient();

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("logfile", logFile.getName(),
                        RequestBody.create(logFile, MediaType.parse("text/plain")))
                .addFormDataPart("uuid", uuid)
                .addFormDataPart("submit", "true")
                .build();

        Request request = new Request.Builder()
                .url(SERVER_URL)
                .post(requestBody)
                .build();

        try {
            Response response = client.newCall(request).execute();
            if (response.isSuccessful()) {
                Log.d("LogUploadWorker", "File uploaded successfully: " + response.body().string());
                return true;
            } else {
                Log.e("LogUploadWorker", "Upload failed with code: " + response.code());
            }
        } catch (IOException e) {
            Log.e("LogUploadWorker", "Error uploading file: " + e.getMessage());
        }

        return false;
    }

    private String generateTimestampedFileName() {
        return new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.getDefault()).format(new Date()) + ".txt";
    }
}
