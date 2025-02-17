package com.example.recorderchunks.utils;

import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.arthenica.mobileffmpeg.Config;
import com.arthenica.mobileffmpeg.FFmpeg;
import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Audio_Converter_util {

    // Define supported input formats
    private static final Set<String> SUPPORTED_FORMATS = new HashSet<>(Arrays.asList("wav", "3gp", "mp4", "m4a", "aac", "ogg", "flac", "amr"));

    // Executor for running tasks in the background
    private static final ExecutorService executorService = Executors.newSingleThreadExecutor();

    // Handler to run callbacks on UI thread
    private static final Handler uiHandler = new Handler(Looper.getMainLooper());

    // Callback interface to notify conversion status
    public interface AudioConversionCallback {
        void onSuccess(String outputFilePath);
        void onFailure(String errorMessage);
    }

    /**
     * Checks if the given audio format is supported.
     * @param filePath The path of the audio file.
     * @return true if the format is supported, false otherwise.
     */
    public static boolean isSupportedFormat(String filePath) {
        String fileExtension = getFileExtension(filePath);
        return SUPPORTED_FORMATS.contains(fileExtension.toLowerCase());
    }

    /**
     * Converts an audio file to MP3 on a background thread and notifies via callback on UI thread.
     * @param context The application context.
     * @param inputFilePath The path of the input audio file.
     * @param callback The callback interface to notify the result.
     */
    public static void convertToMp3(Context context, String inputFilePath, AudioConversionCallback callback) {
        executorService.execute(() -> {
            File inputFile = new File(inputFilePath);
            if (!inputFile.exists()) {
                uiHandler.post(() -> callback.onFailure("Input file does not exist: " + inputFilePath));
                return;
            }

            // Check if format is supported
            if (!isSupportedFormat(inputFilePath)) {
                uiHandler.post(() -> callback.onFailure("Unsupported file format: " + getFileExtension(inputFilePath)));
                return;
            }

            // Define output directory
            File outputDir = new File(context.getExternalFilesDir(null), "SharedAudios");
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }

            // Define output file path
            String outputFilePath = new File(outputDir,
                    inputFile.getName().replaceFirst("[.][^.]+$", "") + ".mp3").getAbsolutePath();

            File outputFile = new File(outputFilePath);

            // If the output file already exists, return it after 3 seconds
            if (outputFile.exists()) {
                try {
                    Thread.sleep(3000); // Wait for 3 seconds
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                uiHandler.post(() -> callback.onSuccess(outputFilePath));
                return;
            }

            Log.d("FFmpegConversion", "Input: " + inputFilePath);
            Log.d("FFmpegConversion", "Output: " + outputFilePath);

            // FFmpeg command to convert audio to MP3
            String cmd = "-i " + inputFilePath + " -vn -ar 44100 -ac 2 -b:a 192k " + outputFilePath;

            // Execute FFmpeg command
            int rc = FFmpeg.execute(cmd);

            if (rc == Config.RETURN_CODE_SUCCESS) {
                // Verify input file still exists
                if (!inputFile.exists()) {
                    Log.e("FFmpegConversion", "Original file was deleted!");
                    uiHandler.post(() -> callback.onFailure("Unexpected deletion of input file."));
                    return;
                }

                uiHandler.post(() -> callback.onSuccess(outputFilePath)); // Notify success on UI thread
            } else {
                uiHandler.post(() -> callback.onFailure("Conversion failed with return code: " + rc)); // Notify failure on UI thread
            }
        });
    }

    // Helper function to get file extension
    private static String getFileExtension(String filePath) {
        int lastIndexOfDot = filePath.lastIndexOf(".");
        if (lastIndexOfDot == -1) {
            return ""; // No extension found
        }
        return filePath.substring(lastIndexOfDot + 1);
    }
}
