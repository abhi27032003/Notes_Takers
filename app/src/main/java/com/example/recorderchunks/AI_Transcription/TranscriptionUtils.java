package com.example.recorderchunks.AI_Transcription;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.widget.Toast;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


import android.content.Context;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class TranscriptionUtils {

    private static final String TAG = "TranscriptionUtils";
    private static final String SEND_TRANSCRIPTION_URL = "http://3.210.239.32/upload.php";
    private static final String GET_TRANSCRIPTION_URL = "http://3.210.239.32/upload.php";

    // Callback interface for handling success and failure
    public interface TranscriptionCallback {
        void onSuccess(String transcription);
        void onError(String errorMessage);
    }

    public interface TranscriptionStatusCallback {
        void onTranscriptionStatusSuccess(String name, String status, int queuePosition);
        void onTranscriptionStatusError(String errorMessage);
    }
    /**
     * Transcribes the given audio file from its file path.
     *
     * @param context  The context for resource access.
     * @param filePath The path to the audio file.
     * @param callback The callback to handle success or error.
     */
    public static void send_for_transcription(Context context, String filePath, TranscriptionCallback callback,String unique_id,String language) {
        File audioFile = new File(filePath);

        if (!audioFile.exists() || !audioFile.isFile()) {
            if (callback != null) {
                callback.onError("Invalid file path or file does not exist.");
            }
            return;
        }

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS) // Set connection timeout
                .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)   // Set write timeout (for file upload)
                .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)    // Set read timeout (for server response)
                .build();

        // Create a request body with the audio file
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM) // Set the request type as form-data
                .addFormDataPart(
                        "audio_file",  // Key for the form field expected by the server
                        audioFile.getName(), // The file's name
                        RequestBody.create(audioFile, MediaType.parse("audio/*")) // File content with the correct MIME type
                )
                .addFormDataPart(
                        "name", // Key for the additional parameter
                       unique_id // Value of the additional parameter
                )
                .build();


        // Build the request
        Request request = new Request.Builder()
                .url(SEND_TRANSCRIPTION_URL)
                .post(requestBody)
                .build();

        // Make the network request
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("NetworkError", "Request failed: " + e.getMessage(), e);
                if (callback != null) {
                    new Handler(Looper.getMainLooper()).post(() ->
                            callback.onError("Network request failed: " + e.getMessage())
                    );
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    if (response.isSuccessful()) {
                        String responseBody = response.body() != null ? response.body().string() : "";
                        Log.d("ResponseSuccess", "Body: " + responseBody);
                        new Handler(Looper.getMainLooper()).post(() ->
                                callback.onSuccess("Response: " + responseBody)
                        );
                    } else {
                        String errorBody = response.body() != null ? response.body().string() : "No response body";
                        Log.e("ServerError", "Code: " + response.code() + ", Message: " + response.message());
                        new Handler(Looper.getMainLooper()).post(() ->
                                callback.onError("Server error: " + response.message() + " - " + errorBody)
                        );
                    }
                } catch (Exception e) {
                    Log.e("ResponseError", "Error processing response: " + e.getMessage(), e);
                    new Handler(Looper.getMainLooper()).post(() ->
                            callback.onError("Error processing response: " + e.getMessage())
                    );
                }
            }
        });

    }
    public static void getTranscriptionStatus(String name, TranscriptionStatusCallback callback) {
        OkHttpClient client = new OkHttpClient();

        // Build the request URL with query parameter
        String url = GET_TRANSCRIPTION_URL + "?name=" + name;

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // Call the callback with an error
                if (callback != null) {
                    callback.onTranscriptionStatusError("Network request failed: " + e.getMessage());
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = response.body() != null ? response.body().string() : "";

                    try {
                        // Parse JSON response
                        JSONObject jsonResponse = new JSONObject(responseBody);
                        String name = jsonResponse.getString("name");
                        String status = jsonResponse.getString("status");
                        int queuePosition = jsonResponse.getInt("queue_position");

                        // Call the callback with success data
                        if (callback != null) {
                            callback.onTranscriptionStatusSuccess(name, status, queuePosition);
                        }
                    } catch (Exception e) {
                        if (callback != null) {
                            callback.onTranscriptionStatusError("Failed to parse response: " + e.getMessage());
                        }
                    }
                } else {
                    // Call the callback with an error
                    if (callback != null) {
                        callback.onTranscriptionStatusError("Server error: " + response.message());
                    }
                }
            }
        });
    }

}

