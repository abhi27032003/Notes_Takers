package com.example.recorderchunks.Audio_Models;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.PowerManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.recorderchunks.Helpeerclasses.Model_Database_Helper;
import com.example.recorderchunks.utils.ZipUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

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
    private static Thread downloadThread;
    private static PowerManager.WakeLock wakeLock;  // Declare WakeLock

    private static void createNotificationChannel(Context context, String language) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            String channelId = "model_download_channel"+language; // Same as in your notification
            CharSequence channelName = "Model Download";
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel(channelId, channelName, importance);
            notificationManager.createNotificationChannel(channel);
        }
    }
    private static void updateModelDownloadStatus(Context context, String language, String savedPath,String model_name) {
        // Assuming you have a Model_Database_Helper class for database operations
        Model_Database_Helper modelDatabaseHelper = new Model_Database_Helper(context);
        modelDatabaseHelper.updateModelDownloadStatusAndPath(language,"yes", savedPath);
        Log.d("ModelDownloader", "Model download status updated for " + language);
       // Vosk_Model.initializeVoskModel(context,model_name);

    }

    // Function to check if notification permission is granted
    private static boolean isNotificationPermissionGranted(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return true;  // No permission needed for Android versions below 13
    }

    // Request notification permission for Android 13 and above
    private static void requestNotificationPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!isNotificationPermissionGranted(context)) {
                // Request permission if not granted (typically via a dialog or activity)
                // For simplicity, showing a toast in this example
                Toast.makeText(context, "Please grant notification permission", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public static void downloadModelFast(Context context, String language, String modelUrl) {
        if (isDownloading) {
            Log.d("ModelDownloader", "Download already in progress.");
            return;
        }
        createNotificationChannel(context,language);
        // Request notification permission if needed
        requestNotificationPermission(context);


        isDownloading = true;
        model_langage=language;

        // Acquire WakeLock to keep CPU running even when screen is off
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK, "ModelDownloader::DownloadWakeLock");
        wakeLock.acquire(10 * 600 * 1000L /*10 minutes*/);  // Set the timeout to 10 minutes, adjust as needed
        Log.e("ModelDownloader", "Starting Download for"+language);

        downloadThread = new Thread(() -> {
            Log.e("ModelDownloader", "Inside Download Thread for"+language);

            Response response = null;
            InputStream input = null;
            FileOutputStream output = null;
            try {
                // Fetch the model name based on the language and URL
                String modelName = "UnknownModel"; // Default model name if not found
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

                File modelp = new File(context.getExternalFilesDir(null), "models/" + modelName);
                File modelDir = new File(context.getExternalFilesDir(null), "models");
                if (modelp.exists()) {
                    Log.d("ModelDownloader", "Model for " + language + " already exists.");
                    updateModelDownloadStatus(context, language, modelDir.getAbsolutePath(),modelName);
                    return;
                }

                // Prepare zip file location
                File zipFile = new File(context.getExternalFilesDir(null), "models/" + modelName + ".zip");
                zipFile.getParentFile().mkdirs();

                // Create a notification manager for progress updates
                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
                String CHANNEL_ID = "model_download_channel"+language;
                NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, CHANNEL_ID)
                        .setSmallIcon(android.R.drawable.stat_sys_download)
                        .setContentTitle("Downloading " + language + " Model")
                        .setContentText("Starting download...")
                        .setProgress(100, 0, false)
                        .setOngoing(true);

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
                notificationManager.notify(1, notificationBuilder.build());

                // Initialize OkHttpClient
                OkHttpClient client = new OkHttpClient();

                Request request = new Request.Builder()
                        .url(modelUrl)
                        .build();

                // Make an asynchronous download request
                Call call = client.newCall(request);
                response = call.execute();
                long fileSize = response.body().contentLength();
                input = response.body().byteStream();
                output = new FileOutputStream(zipFile);
                try  {

                    Log.e("ModelDownloader", "Started Download"+language);

                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    long totalDownloaded = 0;

                    while ((bytesRead = input.read(buffer)) != -1) {
                        totalDownloaded += bytesRead;
                        output.write(buffer, 0, bytesRead);

                        // Check for cancellation
                        if (!isDownloading) {
                            Log.d("ModelDownloader", "Download cancelled.");
                            zipFile.delete();
                            return;
                        }

                        // Update notification progress
                        int progress = (int) ((totalDownloaded * 100) / fileSize);
                        Log.d("ModelDownloader",String.valueOf(progress));

                        if (progress % 10 == 0 || progress == 100) { // Log at 10% intervals and at 100%
                            notificationBuilder.setProgress(100, progress, false)
                                    .setContentText("Downloaded " + progress + "%");
                            notificationManager.notify(1, notificationBuilder.build());
                        }


                    }
                    notificationBuilder.setContentTitle("Download Complete")
                            .setContentText("Downloaded " + language + " Model")
                            .setProgress(0, 0, false); // Clear the progress bar
                    notificationManager.notify(1, notificationBuilder.build());
                    ZipUtils.unzipFile(zipFile, modelDir);
                    zipFile.delete();
                    model_langage = "not available";
                    updateModelDownloadStatus(context, language, modelDir.getAbsolutePath(),modelName);
                }
                catch (Exception e)
                {
                    Log.e("ModelDownloader", "Error downloading model: " + e.getMessage(), e);

                }
                finally {

                }

                Log.d("ModelDownloader", "Model for " + language + " downloaded and extracted.");
            } catch (Exception e) {
                Log.e("ModelDownloader", "Error downloading model: " + e.getMessage(), e);
            } finally {
                isDownloading = false;
                model_langage = "not available";
                try {
                    if (input != null) input.close();
                    if (output != null) output.close();
                    if (response != null) response.close();
                } catch (IOException e) {
                    Log.e("ModelDownloader", "Error closing streams: " + e.getMessage(), e);
                }

                if (wakeLock != null && wakeLock.isHeld()) {
                    wakeLock.release();
                }

                // Release WakeLock when done
                if (wakeLock != null && wakeLock.isHeld()) {
                    wakeLock.release();
                }
            }
        });

        downloadThread.start();
    }

    public static void cancelDownload() {
        isDownloading = false;
        model_langage = "not available";
        if (isDownloading) {
            isDownloading = false;
            model_langage = "not available";
            if (downloadThread != null && downloadThread.isAlive()) {
                downloadThread.interrupt();
                Log.d("ModelDownloader", "Download cancelled.");
            }
        } else {
            Log.d("ModelDownloader", "No download in progress.");
        }
    }
    public static  boolean isModeldownloading()
    {
        return isDownloading;
    }
    public static  String current_downloading_language()
    {
        return model_langage;
    }
}
