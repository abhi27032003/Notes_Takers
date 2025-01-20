package com.example.recorderchunks.Audio_Models;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.recorderchunks.Activity.NotificationActionReceiver;
import com.example.recorderchunks.Helpeerclasses.Model_Database_Helper;
import com.example.recorderchunks.utils.ZipUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.os.Build;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import android.content.pm.PackageManager;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ModelDownloader {

    private static boolean isDownloading = false;
    private static String model_langage = "not available";
    private static final ConcurrentHashMap<String, Boolean> downloadStatusMap = new ConcurrentHashMap<>();
    private static final ExecutorService downloadExecutor = Executors.newFixedThreadPool(5); // Limit parallel downloads

    private static void createNotificationChannel(Context context, String language) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            String channelId = "model_download_channel" + language;
            CharSequence channelName = "Model Download";
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel(channelId, channelName, importance);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private static void updateModelDownloadStatus(Context context, String language, String savedPath, String modelName) {
        Model_Database_Helper modelDatabaseHelper = new Model_Database_Helper(context);
        modelDatabaseHelper.updateModelDownloadStatusAndPath(language, "yes", savedPath);
        Log.d("ModelDownloader", "Model download status updated for " + language);
    }

    private static boolean isNotificationPermissionGranted(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private static void requestNotificationPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!isNotificationPermissionGranted(context)) {
                Toast.makeText(context, "Please grant notification permission", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public static void downloadModelFast(Context context, String language, String modelUrl) {
        if (downloadStatusMap.getOrDefault(language, false)) {
            Log.d("ModelDownloader", "Download already in progress for: " + language);
            return;
        }
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        downloadStatusMap.put(language, true);
        createNotificationChannel(context, language);
        requestNotificationPermission(context);

        downloadExecutor.execute(() -> {
            PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ModelDownloader::DownloadWakeLock" + language);
            wakeLock.acquire(20 * 60 * 1000L); // Timeout of 10 minutes

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            String channelId = "model_download_channel" + language;

            // Create Intent for cancellation action
            Intent cancelIntent = new Intent(context, NotificationActionReceiver.class);
            cancelIntent.setAction("CANCEL_DOWNLOAD");
            cancelIntent.putExtra("language", language);
            PendingIntent cancelPendingIntent = PendingIntent.getBroadcast(
                    context,
                    language.hashCode(),
                    cancelIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(android.R.drawable.stat_sys_download)
                    .setContentTitle("Downloading " + language + " Model")
                    .setContentText("Starting download...")
                    .setProgress(100, 0, false)
                    .setOngoing(true);


            notificationManager.notify(language.hashCode(), notificationBuilder.build());

            Response response = null;
            InputStream input = null;
            FileOutputStream output = null;

            try {
                String modelName;
                switch (language.toLowerCase()) {
                    case "english":
                        modelName = "vosk-model-small-en-us-0.15";
                        break;
                    case "chinese":
                        modelName = "vosk-model-small-cn-0.22";
                        break;
                    case "french":
                        modelName = "vosk-model-small-fr-0.22";
                        break;
                    case "spanish":
                        modelName = "vosk-model-small-es-0.42";
                        break;
                    case "hindi":
                        modelName = "vosk-model-small-hi-0.22";
                        break;
                    default:
                        Log.e("ModelDownloader", "Unsupported language: " + language);
                        return;
                }

                File modelDir = new File(context.getExternalFilesDir(null), "models");
                File modelFile = new File(modelDir, modelName);
                if (modelFile.exists()) {
                    Log.d("ModelDownloader", "Model for " + language + " already exists.");
                    updateModelDownloadStatus(context, language, modelDir.getAbsolutePath(), modelName);
                    return;
                }

                File zipFile = new File(modelDir, modelName + ".zip");
                zipFile.getParentFile().mkdirs();

                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder().url(modelUrl).build();
                Call call = client.newCall(request);
                response = call.execute();

                long fileSize = response.body().contentLength();
                input = response.body().byteStream();
                output = new FileOutputStream(zipFile);

                byte[] buffer = new byte[1024];
                int bytesRead;
                long totalDownloaded = 0;

                while ((bytesRead = input.read(buffer)) != -1) {
                    totalDownloaded += bytesRead;
                    output.write(buffer, 0, bytesRead);

                    int progress = (int) ((totalDownloaded * 100) / fileSize);
                    if(progress%10==0)
                    {
                        notificationBuilder.setProgress(100, progress, false)
                                .setContentText("Downloaded " + progress + "%");
                        notificationManager.notify(language.hashCode(), notificationBuilder.build());
                    }

                }

                notificationBuilder.setContentTitle("Download Complete")
                        .setContentText("Downloaded " + language + " Model")
                        .setProgress(0, 0, false);
                notificationManager.notify(language.hashCode(), notificationBuilder.build());

                ZipUtils.unzipFile(zipFile, modelDir);

                notificationBuilder.setContentTitle("Download Complete")
                        .setContentText("Downloaded " + language + " Model")
                        .setProgress(0, 0, false);
                notificationManager.notify(language.hashCode(), notificationBuilder.build());
                zipFile.delete();
                updateModelDownloadStatus(context, language, modelDir.getAbsolutePath(), modelName);

                notificationBuilder.setContentTitle("Download Complete")
                        .setContentText("Downloaded " + language + " Model")
                        .setProgress(0, 0, false);
                notificationManager.notify(language.hashCode(), notificationBuilder.build());
                notificationManager.cancel(language.hashCode());
                notificationManager.cancel(language.hashCode());
            } catch (Exception e) {
                Log.e("ModelDownloader", "Error downloading model: " + e.getMessage(), e);
            } finally {
                downloadStatusMap.put(language, false);

                try {
                    if (input != null) input.close();
                    if (output != null) output.close();
                    if (response != null) response.close();
                } catch (IOException e) {
                    Log.e("ModelDownloader", "Error closing streams: " + e.getMessage(), e);
                }

                if (wakeLock.isHeld()) {
                    wakeLock.release();
                }
            }
        });
    }

    public static void cancelDownload(String language) {
        downloadStatusMap.put(language, false);
        Log.d("ModelDownloader", "Download cancelled for: " + language);
    }

    public static boolean isModelDownloading(String language) {
        return downloadStatusMap.getOrDefault(language, false);
    }
    public static List<String> getCurrentlyDownloadingModels() {
        List<String> downloadingModels = new ArrayList<>();
        for (String language : downloadStatusMap.keySet()) {
            if (downloadStatusMap.get(language)) {
                downloadingModels.add(language);
            }
        }
        return downloadingModels;
    }
    public static String getCurrentlyDownloadingModelsAsString() {
        StringBuilder downloadingModels = new StringBuilder();
        for (String language : downloadStatusMap.keySet()) {
            if (downloadStatusMap.get(language)) {
                if (downloadingModels.length() > 0) {
                    downloadingModels.append(", ");
                }
                downloadingModels.append(language);
            }
        }
        return downloadingModels.toString();
    }

}
