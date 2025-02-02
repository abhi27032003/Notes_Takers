package com.example.recorderchunks.Activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.icu.text.SimpleDateFormat;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;

import com.example.recorderchunks.Background_Allow.Show_Add_notes_Activity;
import com.example.recorderchunks.R;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CrashReportActivity extends Activity {

    private String crashDetails; // To store crash details for dialog
    private static SharedPreferences prefs_uuid;
    private String uuid;
    private static final String SERVER_URL = "https://notetakers.vipresearch.ca/App_Script/sendlogs.php";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crash_report);

        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.tool);
        toolbar.setTitle("Crash Report");
        toolbar.setNavigationIcon(R.drawable.baseline_arrow_back_24); // Back icon
        toolbar.setNavigationOnClickListener(v -> finish());

        TextView crashReasonTextView = findViewById(R.id.crashReasonTextView);
        EditText feedbackEditText = findViewById(R.id.feedbackEditText);
        Button sendFeedbackButton = findViewById(R.id.sendFeedbackButton);
        Button showDetailsButton = findViewById(R.id.showDetailsButton);
        Button backToMainButton = findViewById(R.id.backToMainButton);

        // Get crash details from intent
        String anrError = getIntent().getStringExtra("anr_error");
        String stackTrace = getIntent().getStringExtra("stack_trace");

        if (anrError != null && !anrError.isEmpty()) {
            crashDetails = "Oops, the app became unresponsive!\n\nANR Details:\n" + anrError;
        } else if (stackTrace != null && !stackTrace.isEmpty()) {
            crashDetails = "Oops, something went wrong!\n\nCrash Details:\n" + stackTrace;
        } else {
            crashDetails = "An error occurred but no details are available.";
        }

        crashReasonTextView.setText(crashDetails);

        // Show full crash details in a dialog
        showDetailsButton.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Crash Details")
                .setMessage(crashDetails)
                .setPositiveButton("OK", null)
                .show());
        sendFeedback("This is a machine automated crash report and not send by user", crashDetails);

        // Send feedback button
        sendFeedbackButton.setOnClickListener(v -> {
            String feedback = feedbackEditText.getText().toString().trim();
            if (!feedback.isEmpty()) {
                sendFeedback(feedback, crashDetails);
            } else {
                feedbackEditText.setError("Please enter your feedback");
            }
        });

        // Back to Main Activity button
        backToMainButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, Show_Add_notes_Activity.class);
            startActivity(intent);
            finish();
        });
    }

    private void sendFeedback(String feedback, String crashInfo) { String emailBody = "User Feedback:\n" + feedback + "\n\nCrash Details:\n" + crashInfo;
        prefs_uuid = getApplicationContext().getSharedPreferences("preferences", Context.MODE_PRIVATE);
        uuid = prefs_uuid.getString("uuid", UUID.randomUUID().toString());
        uploadFeedback(feedback,crashInfo,uuid);
        Toast.makeText(getApplicationContext(), uuid, Toast.LENGTH_SHORT).show();

    }
    private void uploadFeedback(String feedbackText, String crashDetails, String uuid) {
        Executors.newSingleThreadExecutor().execute(() -> {
            // Generate timestamped file name
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            File logFile = new File(getCacheDir(), "Crash_Report_" + timestamp + ".txt");

            try {
                // Write feedback and crash details to the file
                FileWriter writer = new FileWriter(logFile);
                writer.write("Feedback:\n" + feedbackText + "\n\n");
                writer.write("Crash Details:\n" + crashDetails);
                writer.close();

                // Upload the file
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

                Response response = client.newCall(request).execute();
                if (response.isSuccessful()) {
                    Log.d("LogUploadWorker", "File uploaded successfully: " + response.body().string());

                    // Delete the file after successful upload
                    if (logFile.delete()) {
                        Log.d("LogUploadWorker", "Log file deleted after upload.");
                    } else {
                        Log.e("LogUploadWorker", "Failed to delete log file.");
                    }

                    // Show success message on UI
                    runOnUiThread(() -> Toast.makeText(CrashReportActivity.this, "Feedback sent successfully!", Toast.LENGTH_SHORT).show());

                } else {
                    Log.e("LogUploadWorker", "Upload failed with code: " + response.code());
                    runOnUiThread(() -> Toast.makeText(CrashReportActivity.this, "Upload failed!", Toast.LENGTH_SHORT).show());
                }
            } catch (IOException e) {
                Log.e("LogUploadWorker", "Error handling file: " + e.getMessage());
                runOnUiThread(() -> Toast.makeText(CrashReportActivity.this, "Error uploading file", Toast.LENGTH_SHORT).show());
            }
        });
    }

}
