package com.example.recorderchunks.Audio_Models;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Environment;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.example.recorderchunks.utils.ZipUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ModelDownloader {

    private static final String MODEL_DIR = Environment.getExternalStorageDirectory() + "/MyApp/Models/";
    private static final String CHANNEL_ID = "DownloadChannel";

    public static void downloadModel(Context context, String language) {
        try {
            // Fetch all models from metadata
            JSONArray models = ModelMetadata.getModelMetadata();
            boolean modelFound = false;

            // Iterate through models to find the specified language
            for (int i = 0; i < models.length(); i++) {
                JSONObject model = models.getJSONObject(i);
                if (model.getString("language").equals(language)) {
                    modelFound = true;

                    String modelName = model.getString("model_name");
                    String modelUrl = model.getString("download_link");

                    File modelDir = new File(context.getFilesDir(), "models/" + modelName);
                    if (modelDir.exists()) {
                        Log.d("ModelDownloader", "Model for " + language + " already exists.");
                        return;
                    }

                    // Prepare zip file location
                    File zipFile = new File(context.getFilesDir(), "models/" + modelName + ".zip");
                    zipFile.getParentFile().mkdirs();

                    // Create a notification manager for progress updates
                    NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                    String CHANNEL_ID = "model_download_channel";
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Model Download", NotificationManager.IMPORTANCE_LOW);
                        notificationManager.createNotificationChannel(channel);
                    }
                    NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, CHANNEL_ID)
                            .setSmallIcon(android.R.drawable.stat_sys_download)
                            .setContentTitle("Downloading " + language + " Model")
                            .setContentText("Starting download...")
                            .setProgress(100, 0, false)
                            .setOngoing(true);

                    // Download the model file
                    HttpURLConnection connection = (HttpURLConnection) new URL(modelUrl).openConnection();
                    connection.connect();
                    long fileSize = connection.getContentLength();

                    try (InputStream input = connection.getInputStream();
                         FileOutputStream output = new FileOutputStream(zipFile)) {

                        byte[] buffer = new byte[1024];
                        int bytesRead;
                        long totalDownloaded = 0;

                        while ((bytesRead = input.read(buffer)) != -1) {
                            totalDownloaded += bytesRead;
                            output.write(buffer, 0, bytesRead);

                            // Update notification progress
                            int progress = (int) ((totalDownloaded * 100) / fileSize);
                            notificationBuilder.setProgress(100, progress, false)
                                    .setContentText("Downloaded " + progress + "%");
                            notificationManager.notify(1, notificationBuilder.build());
                        }
                    }

                    // Notify completion
                    notificationBuilder.setContentText("Download complete")
                            .setProgress(0, 0, false)
                            .setOngoing(false);
                    notificationManager.notify(1, notificationBuilder.build());

                    // Unzip the downloaded file
                    ZipUtils.unzipFile(zipFile, modelDir);
                    zipFile.delete();

                    Log.d("ModelDownloader", "Model for " + language + " downloaded and extracted.");
                    return;
                }
            }

            if (!modelFound) {
                Log.e("ModelDownloader", "Language model not found: " + language);
            }
        } catch (Exception e) {
            Log.e("ModelDownloader", "Error downloading model: " + e.getMessage(), e);
        }
    }




}
