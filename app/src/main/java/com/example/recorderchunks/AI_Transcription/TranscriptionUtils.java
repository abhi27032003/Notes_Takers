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
    private static final String TRANSCRIPTION_URL = "http://3.210.239.32/transcribe.php";

    // Callback interface for handling success and failure
    public interface TranscriptionCallback {
        void onSuccess(String transcription);
        void onError(String errorMessage);
    }

    /**
     * Transcribes the given audio file from its file path.
     *
     * @param context  The context for resource access.
     * @param filePath The path to the audio file.
     * @param callback The callback to handle success or error.
     */
    public static void transcribeAudio(Context context, String filePath, TranscriptionCallback callback) {
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
                .build();


        // Build the request
        Request request = new Request.Builder()
                .url(TRANSCRIPTION_URL)
                .post(requestBody)
                .build();

        // Make the network request
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (callback != null) {
                    // Run on the main thread
                    new Handler(Looper.getMainLooper()).post(() ->
                            callback.onError("Network request failed: " + e.getMessage())
                    );
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = response.body() != null ? response.body().string() : "";
                    new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess("response:"+responseBody));
                } else {
                    String errorBody = response.body() != null ? response.body().string() : "No response body";
                    new Handler(Looper.getMainLooper()).post(() ->
                            callback.onError("Server error: " + response.message() + " - " + errorBody)
                    );
                }
            }

        });
    }
    public static  String getTranscription(String response) {
        try {
            // Parse the response string into a JSONObject
            JSONObject responseJson = new JSONObject(response);

            // Extract the raw_output field
            String rawOutput = responseJson.getString("raw_output");

            // Locate the start and end of the embedded JSON containing the transcription
            int startIndex = rawOutput.indexOf("{");
            int endIndex = rawOutput.lastIndexOf("}") + 1;

            // Extract and parse the embedded JSON
            if (startIndex != -1 && endIndex != -1) {
                String embeddedJson = rawOutput.substring(startIndex, endIndex);
                JSONObject embeddedJsonObject = new JSONObject(embeddedJson);

                // Return the transcription
                return embeddedJsonObject.getString("transcription");
            } else {
                return "Transcription not found in the response.";
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return "Error parsing response: " + e.getMessage();
        }
    }

}

