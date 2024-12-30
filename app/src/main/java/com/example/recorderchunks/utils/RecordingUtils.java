package com.example.recorderchunks.utils;

import android.content.Context;
import android.content.Intent;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;

import com.example.recorderchunks.Activity.RecordingService;
import com.example.recorderchunks.Background_Allow.Add_notes_Fragment;
import com.example.recorderchunks.DatabaseHelper;
import com.example.recorderchunks.Model_Class.RecordingViewModel;
import com.example.recorderchunks.Model_Class.is_recording;
import com.example.recorderchunks.Model_Class.recording_language;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class RecordingUtils {
    private boolean isPaused = false;

    private MediaRecorder mediaRecorder;
    private String audioFilePath;
    private long startTime;
    private long stopTime;
    private Context context;
    private DatabaseHelper databaseHelper;
    private RecordingCallback recordingCallback;
    private boolean isRecordingCompleted = false;

    public is_recording is_recording;
    private recording_language recordingLanguage;


    public RecordingUtils(Context context,  RecordingCallback recordingCallback) {
        this.context = context;

        this.databaseHelper = new DatabaseHelper(context);
        this.recordingCallback = recordingCallback;
        this.recordingLanguage =new recording_language();
        this.is_recording=new is_recording();


    }
    public interface RecordingCallback {
        void onRecordingStarted(String filePath);
        void onRecordingSaved(int eventId);
        void onRecordingPaused(); // New
        void onRecordingResumed(); // New
    }


    public void startRecording() {
        is_recording.setIs_recording(true);


        // Set up the audio file path
        File audioDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC);
        if (audioDir != null) {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            audioFilePath = audioDir.getAbsolutePath() + "/recording_" + timeStamp + ".3gp";
        } else {
            Toast.makeText(context, "Failed to get storage directory", Toast.LENGTH_SHORT).show();
            return;
        }

        // MediaRecorder configuration
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mediaRecorder.setOutputFile(audioFilePath);

        try {
            mediaRecorder.prepare();
            mediaRecorder.start();
            startTime = System.currentTimeMillis();
            Log.d("AudioRecorder", "Recording started: " + audioFilePath);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "Error starting recording: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
        if (recordingCallback != null) {
            recordingCallback.onRecordingStarted(audioFilePath);
        }
        isRecordingCompleted = false;
        Intent serviceIntent = new Intent(context, RecordingService.class);
        context.startService(serviceIntent);
    }

    public void pauseRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && mediaRecorder != null && !isPaused) {
            try {
                mediaRecorder.pause();
                isPaused = true;

                Log.d("AudioRecorder", "Recording paused");
                if (recordingCallback != null) {
                    recordingCallback.onRecordingPaused();
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(context, "Error pausing recording: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(context, "Pause not supported on this device or already paused", Toast.LENGTH_SHORT).show();
        }
    }
    public void resumeRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && mediaRecorder != null && isPaused) {
            try {
                mediaRecorder.resume();
                isPaused = false;

                Log.d("AudioRecorder", "Recording resumed");
                if (recordingCallback != null) {
                    recordingCallback.onRecordingResumed();
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(context, "Error resuming recording: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(context, "Resume not supported on this device or already recording", Toast.LENGTH_SHORT).show();
        }
    }


    public void stopRecording(int eventId) {
        RecordingService.isStopping=true;
        is_recording.setIs_recording(false);

        if (mediaRecorder != null) {
            try {
                mediaRecorder.stop();
                mediaRecorder.release();
                mediaRecorder = null;
                stopTime = System.currentTimeMillis();
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(context, "Error stopping recording: " + e.getMessage(), Toast.LENGTH_LONG).show();
                return;
            }

            // Calculate duration and generate recording details
            long duration = (stopTime - startTime) / 1000;
            String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date(startTime));
            String time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date(startTime));
            String uniqueCode = date.replace("-", "") + time.replace(":", "") + duration;
            String recordingName = "Recording_" + uniqueCode;
            String format = audioFilePath.substring(audioFilePath.lastIndexOf('.') + 1);
            String description = "";

            // Get next event ID
            int nextEventId = databaseHelper.getNextEventId();
            boolean isSaved;

            // Save recording in database
            String formattedDate = new SimpleDateFormat("dd/MM/yy 'at' HH:mm", Locale.getDefault()).format(new Date());
            if (eventId != -1) {
                isSaved = databaseHelper.insertRecording(
                        eventId,
                        formattedDate,
                        description,
                        recordingName,
                        format,
                        String.valueOf(duration),
                        audioFilePath,
                        true,
                        "no",
                        description,
                        "no",
                        recordingLanguage.getRecording_language()
                );
            } else {
                isSaved = databaseHelper.insertRecording(
                        nextEventId,
                        formattedDate,
                        description,
                        recordingName,
                        format,
                        String.valueOf(duration),
                        audioFilePath,
                        true,
                        "no",
                        description,
                        "no",
                        recordingLanguage.getRecording_language()
                );
            }

            // Notify the fragment through the callback
            if (isSaved) {
                if (recordingCallback != null) {
                    recordingCallback.onRecordingSaved(eventId != -1 ? eventId : nextEventId);
                }
                Toast.makeText(context, "Recording saved successfully", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "Failed to save recording", Toast.LENGTH_SHORT).show();
            }
        }
        isRecordingCompleted = true;
        if (recordingCallback != null && isRecordingCompleted) {
            recordingCallback.onRecordingSaved(eventId);
        }
        RecordingService.isStopping=true;
        Intent serviceIntent = new Intent(context, RecordingService.class);
        context.stopService(serviceIntent);


    }


}
