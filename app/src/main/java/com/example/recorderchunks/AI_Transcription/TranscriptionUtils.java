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
import java.util.HashMap;
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
    private static final String SEND_TRANSCRIPTION_URL = "https://notetakers.vipresearch.ca/App_Script/uploads.php";
    private static final String GET_TRANSCRIPTION_URL = "https://notetakers.vipresearch.ca/App_Script/status.php";

    // Callback interface for handling success and failure
    public interface TranscriptionCallback {
        void onSuccess(String transcription);
        void onError(String errorMessage);
    }
    public interface Transcription_no_code_Callback {
        void onSuccess(String transcription);
        void onError(String errorMessage);
    }

    public interface TranscriptionStatusCallback {
        void onTranscriptionStatusSuccess(String name, String status, int queuePosition) throws JSONException;
        void onTranscriptionStatusError(String errorMessage);
    }
    /**
     * Transcribes the given audio file from its file path.
     *
     * @param context  The context for resource access.
     * @param filePath The path to the audio file.
     * @param callback The callback to handle success or error.
     */

    public static void send_for_transcription_no_uuid(Context context, String filePath, Transcription_no_code_Callback callback,String unique_id,String language) {
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
                        "language", // Key for the additional parameter
                        getLanguageCode(language) // Value of the additional parameter
                )
                .build();


        // Build the request
        Request request = new Request.Builder()
                .url("https://notetakers.vipresearch.ca/App_Script/uploads.php")
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
                                callback.onSuccess(getTranscription(responseBody))
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
                        "recording_name", // Key for the additional parameter
                       unique_id // Value of the additional parameter
                )
                .addFormDataPart(
                        "chunk_id", // Key for the additional parameter
                        extractNumberBeforeDot(filePath)// Value of the additional parameter
                )
                .addFormDataPart(
                        "language", // Key for the additional parameter
                        getLanguageCode(language) // Value of the additional parameter
                )
                .addFormDataPart(
                        "user_id", // Key for the additional parameter
                        "user_id_123" // Value of the additional parameter
                )
                //user_id
                //language
                //chunk_id
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
                                callback.onSuccess(responseBody)
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
    public static void getTranscriptionStatus(String name, TranscriptionStatusCallback callback, String filePath) {
        File audioFile = new File(filePath);
        OkHttpClient client = new OkHttpClient();

        // Build the request URL with query parameter
        String url = "https://notetakers.vipresearch.ca/App_Script/status.php?recording_name=" + name + "&user_id=" + "user_id_123" + "&chunk_id=" +extractNumberBeforeDot(filePath);

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
                    String responseString = response.body().string();

                    // Log the response
                    Log.e("chunk_path", responseString);

                    // Use the response string in the callback
                    try {
                        callback.onTranscriptionStatusSuccess(responseString, "status", 1);
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
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
    public static String getLanguageCode(String language) {
        // Create a map for the 5 languages and their codes
        HashMap<String, String> languageMap = new HashMap<>();
        languageMap.put("English", "en");
        languageMap.put("French", "fr");
        languageMap.put("Chinese", "zh");
        languageMap.put("Hindi", "hi");
        languageMap.put("Spanish", "es");

        // Return the code if it exists, otherwise return "Code not found"
        return languageMap.getOrDefault(language, "en");
    }
    public static String extractNumberBeforeDot(String filePath) {
        StringBuilder number = new StringBuilder();

        // Find the position of the last dot in the file path
        int lastDotIndex = filePath.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return null; // Return null if there's no dot in the path
        }

        // Traverse the string backward from the dot
        for (int i = lastDotIndex - 1; i >= 0; i--) {
            char c = filePath.charAt(i);
            // If it's a digit, add it to the number
            if (Character.isDigit(c)) {
                number.append(c);
            } else if (number.length() > 0) {
                // Break once digits stop (ensures only trailing numbers are captured)
                break;
            }
        }

        // Reverse the collected digits since they were added in reverse order
        return number.reverse().toString();
    }
    public static String getTranscription(String jsonResponse) {
        try {
            // Parse the JSON response
            JSONObject jsonObject = new JSONObject(jsonResponse);

            // Get the raw_output field
            String rawOutput = jsonObject.optString("raw_output", "");

            // Extract the transcription part from raw_output
            if (!rawOutput.isEmpty()) {
                int startIndex = rawOutput.indexOf("{\"transcription\":");
                if (startIndex != -1) {
                    String transcriptionJson = rawOutput.substring(startIndex);
                    JSONObject transcriptionObject = new JSONObject(transcriptionJson);
                    return transcriptionObject.optString("transcription", "No transcription found.");
                }
            }

            return "Transcription not found in the raw output.";
        } catch (Exception e) {
            e.printStackTrace();
            return "Error parsing JSON: " + e.getMessage();
        }
    }

}

