package com.example.recorderchunks.AI_Transcription;

import static com.example.recorderchunks.Encryption.AudioEncryptor.getSHA256Hash_of_audiofile;
import static com.example.recorderchunks.Encryption.RSASignature.createPackage;
import static com.example.recorderchunks.Encryption.RSASignature.encryptJsonPackage;
import static com.example.recorderchunks.Encryption.RSASignature.signData;
import static com.example.recorderchunks.Helpeerclasses.Chunks_Json_helper.convertJsonFormat;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.widget.Toast;

import com.example.recorderchunks.Encryption.AudioEncryptor;
import com.example.recorderchunks.Encryption.RSAEncryptor;
import com.example.recorderchunks.Encryption.SHA256HashGenerator;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
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

import javax.crypto.SecretKey;

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
    private static final int MAX_CONCURRENT_REQUESTS = 5; // Adjust based on server capacity
    private static final int QUEUE_CAPACITY = 200; // Prevent memory overload
    private static final BlockingQueue<Runnable> taskQueue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);
    private static final ExecutorService executorService = new ThreadPoolExecutor(
            MAX_CONCURRENT_REQUESTS, // Core pool size
            MAX_CONCURRENT_REQUESTS, // Max pool size
            30, TimeUnit.SECONDS, // Keep-alive time
            new LinkedBlockingQueue<Runnable>() // Queue for waiting tasks
    );
    private static final String SEND_TRANSCRIPTION_URL = "https://notetakers.vipresearch.ca/App_Script/uploads.php";
    private static final String SEND_TRANSCRIPTION_URL_eccoded = "https://notetakers.vipresearch.ca/App_Script/Final_upload_enc_chunk.php";
    static SharedPreferences prefs ;
    // Callback interface for handling success and failure
    public interface TranscriptionCallback {
        void onSuccess(String transcription);
        void onError(String errorMessage);
    }
    public interface Transcription_no_code_Callback {
        void onSuccess(String transcription);
        void onError(String errorMessage);
    }
    public static void logLargeString(String tag, String content) {
        if (content.length() > 4000) {
            for (int i = 0; i < content.length(); i += 4000) {
                if (i + 4000 < content.length()) {
                    Log.d(tag, content.substring(i, i + 4000));
                } else {
                    Log.d(tag, content.substring(i));
                }
            }
        } else {
            Log.d(tag, content);
        }
    }
    public interface TranscriptionStatusCallback {
        void onTranscriptionStatusSuccess(String name, String status, int queuePosition) throws Exception;
        void onTranscriptionStatusError(String errorMessage);
    }
    /**
     * Transcribes the given audio file from its file path.
     *
     * @param filePath The path to the audio file.
     * @param callback The callback to handle success or error.
     */

    public static void send_for_transcription_encrypted(
            String chunk_id,
            String filePath,
            TranscriptionCallback callback,
            String unique_recording_name,
            String language,
            Context context,
            String uuid
            )
    {

        File audioFile = new File(filePath);
        Log.e("recording_name", unique_recording_name);

        if (!audioFile.exists() || !audioFile.isFile()) {
            if (callback != null) {
                callback.onError("Invalid file path or file does not exist.");
            }
            return;
        }

        try {
            prefs = context.getSharedPreferences("preferences", Context.MODE_PRIVATE);
            String stored_private_key = prefs.getString("client_private_key", null);
            String stored_public_key = prefs.getString("client_public_key", null);
            String USER_AES_KEY = prefs.getString("client_AES_key", null);



            //salt
            String salt="ʌᴉԀ ɹǝsǝɐɹƆɥ";

            // Encrypt the audio file using the client private AES key
            byte[] encryptedAudio = AudioEncryptor.encryptFileWithAESkeystring(audioFile,context);
            String encryptedAudiosignature = signData(encryptedAudio,stored_private_key);
            String jsonpackage=createPackage(encryptedAudio,encryptedAudiosignature);
            byte[] encryptedPackage = encryptJsonPackage(jsonpackage, context);
            String finalEncryptedPackage = Base64.encodeToString(encryptedPackage, Base64.DEFAULT);
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File file = new File(downloadsDir, "helloenc.txt");

            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(finalEncryptedPackage.getBytes());
                fos.flush();
                Log.v("TOOOOO","File saved successfully: " + file.getAbsolutePath());
               // Toast.makeText(context, "File saved successfully: " + file.getAbsolutePath(), Toast.LENGTH_SHORT).show();
                System.out.println("File saved successfully: " + file.getAbsolutePath());
            } catch (IOException e) {
                e.printStackTrace();
            }
           //Log.d("EncryptionLog", "Encrypted Audio: " + Base64.encodeToString(encryptedAudio, Base64.DEFAULT));
           // Log.d("EncryptionLog", "Final json : " + finalEncryptedPackage);
            //Log.d("EncryptionLog", "JSON Package: " + jsonpackage);
            Log.d("EncryptionLog", "Signature: " + encryptedAudiosignature);



// Use this method instead of Log.d()
            logLargeString("EncryptionLog", "Final json : " + finalEncryptedPackage.length());







//            Log.e("encrypted audio ",unique_recording_name+" "+audioString);
//            Log.e("encrypted audio hash ",unique_recording_name+" "+chunk_hash);


            // 2. Create the multipart request body with encrypted content
            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart(
                            "audio_file",
                            chunk_id + ".enc",
                            RequestBody.create(encryptedAudio, MediaType.parse("application/octet-stream"))
                    )
                    .build();




            // 5. Build the request
            Request request = new Request.Builder()
                    .url(SEND_TRANSCRIPTION_URL_eccoded)
                    .post(requestBody)
                    .build();

            // 6. Make the network request
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                    .build();

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
                            Log.d("encrypted audio", "Body: " + responseBody);
                            Log.d("encrypted audio", "name: " + chunk_id);

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

        } catch (Exception e) {
            Log.e("EncryptionError", "Error during encryption: " + e.getMessage(), e);
            if (callback != null) {
                callback.onError("Encryption error: " + e.getMessage());
            }
        }
    }

//    public static void send_for_transcription(String chunk_id, String filePath, TranscriptionCallback callback,String unique_recording_name,String language,String uuid,Context context) {
//        File audioFile = new File(filePath);
//        Log.e("recording_name",unique_recording_name);
//        if (!audioFile.exists() || !audioFile.isFile()) {
//            if (callback != null) {
//                callback.onError("Invalid file path or file does not exist.");
//            }
//            return;
//        }
//
//        OkHttpClient client = new OkHttpClient.Builder()
//                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS) // Set connection timeout
//                .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)   // Set write timeout (for file upload)
//                .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)    // Set read timeout (for server response)
//                .build();
//
//        // Create a request body with the audio file
//        RequestBody requestBody = new MultipartBody.Builder()
//                .setType(MultipartBody.FORM) // Set the request type as form-data
//                .addFormDataPart(
//                        "audio_file",  // Key for the form field expected by the server
//                        audioFile.getName(), // The file's name
//                        RequestBody.create(audioFile, MediaType.parse("audio/*")) // File content with the correct MIME type
//                )
//                .addFormDataPart(
//                        "recording_name", // Key for the additional parameter
//                       unique_recording_name // Value of the additional parameter
//                )
//                .addFormDataPart(
//                        "chunk_id", // Key for the additional parameter
//                        chunk_id// Value of the additional parameter
//                )
//                .addFormDataPart(
//                        "language", // Key for the additional parameter
//                        getLanguageCode(language) // Value of the additional parameter
//                )
//                .addFormDataPart(
//                        "user_id", // Key for the additional parameter
//                       uuid // Value of the additional parameter
//                )
//
//                //user_id
//                //language
//                //chunk_id
//                .build();
//
//        Toast.makeText(context, uuid, Toast.LENGTH_SHORT).show();
//
//        // Build the request
//        Request request = new Request.Builder()
//                .url(SEND_TRANSCRIPTION_URL)
//                .post(requestBody)
//                .build();
//
//        // Make the network request
//        client.newCall(request).enqueue(new Callback() {
//            @Override
//            public void onFailure(Call call, IOException e) {
//                Log.e("NetworkError", "Request failed: " + e.getMessage(), e);
//                if (callback != null) {
//                    new Handler(Looper.getMainLooper()).post(() ->
//                            callback.onError("Network request failed: " + e.getMessage())
//                    );
//                }
//            }
//
//            @Override
//            public void onResponse(Call call, Response response) throws IOException {
//                try {
//                    if (response.isSuccessful()) {
//                        String responseBody = response.body() != null ? response.body().string() : "";
//                        Log.d("ResponseSuccess", "Body: " + responseBody);
//                        new Handler(Looper.getMainLooper()).post(() ->
//                                callback.onSuccess(responseBody)
//                        );
//                    } else {
//                        String errorBody = response.body() != null ? response.body().string() : "No response body";
//                        Log.e("ServerError", "Code: " + response.code() + ", Message: " + response.message());
//                        new Handler(Looper.getMainLooper()).post(() ->
//                                callback.onError("Server error: " + response.message() + " - " + errorBody)
//                        );
//                    }
//                } catch (Exception e) {
//                    Log.e("ResponseError", "Error processing response: " + e.getMessage(), e);
//                    new Handler(Looper.getMainLooper()).post(() ->
//                            callback.onError("Error processing response: " + e.getMessage())
//                    );
//                }
//            }
//        });
//
//    }

    public static void getTranscriptionStatus_All_At_once(String unique_recording_name, TranscriptionStatusCallback callback,String user_id,Context context) {
        OkHttpClient client = new OkHttpClient();
        //Toast.makeText(context, user_id, Toast.LENGTH_SHORT).show();

        Log.e("transcription_response",unique_recording_name +" _");
        String encodedParam="";
       // String encodedParam = null;
        try
        {
             encodedParam = URLEncoder.encode(unique_recording_name, "UTF-8");

        }catch (Exception e)
        {

        }


        Log.e("transcription_response", encodedParam);

        // Build the request URL with query parameter
        String url = "https://notetakers.vipresearch.ca/App_Script/enc_status.php?search=" + encodedParam ;

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
                    String response_text=response.body().string();
                    String responseString = null;
                    try {
                        responseString = convertJsonFormat(response_text).toString();
                        Log.e("transcription_response", response_text);
                        Log.e("transcription_response", responseString);
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }

                    // Log the response
                    Log.e("chunk_path", responseString);

                    // Use the response string in the callback
                    try {
                        callback.onTranscriptionStatusSuccess(responseString, "status", 1);
                    } catch (JSONException e) {
                        Log.e("chunk_path_i", responseString);

                        Log.e("chunk_path_I",e.getMessage());
                    } catch (Exception e) {
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

    public static void send_for_transcription(String chunk_id, String filePath, TranscriptionCallback callback,
                                              String unique_recording_name, String language, String uuid, Context context) {
        try {
            taskQueue.put(() -> send_for_transcription_encrypted(chunk_id, filePath, callback, unique_recording_name, language,  context,uuid));
            executorService.execute(taskQueue.poll()); // Pick tasks from queue and execute them
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Log.e("TaskQueueError", "Failed to queue request: " + e.getMessage());
        }
    }

    private static void processRequest(String chunk_id, String filePath, TranscriptionCallback callback,
                                       String unique_recording_name, String language, String uuid, Context context) {
        File audioFile = new File(filePath);
        if (!audioFile.exists() || !audioFile.isFile()) {
            new Handler(Looper.getMainLooper()).post(() -> callback.onError("Invalid file path or file does not exist."));
            return;
        }

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("audio_file", audioFile.getName(), RequestBody.create(audioFile, MediaType.parse("audio/*")))
                .addFormDataPart("recording_name", unique_recording_name)
                .addFormDataPart("chunk_id", chunk_id)
                .addFormDataPart("language", getLanguageCode(language))
                .addFormDataPart("user_id", uuid)
                .build();

        Request request = new Request.Builder()
                .url(SEND_TRANSCRIPTION_URL)
                .post(requestBody)
                .build();

        try {
            Response response = client.newCall(request).execute();
            if (response.isSuccessful()) {
                String responseBody = response.body() != null ? response.body().string() : "";
                new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(responseBody));
            } else {
                String errorBody = response.body() != null ? response.body().string() : "No response body";
                new Handler(Looper.getMainLooper()).post(() -> callback.onError("Server error: " + response.message() + " - " + errorBody));
            }
        } catch (IOException e) {
            new Handler(Looper.getMainLooper()).post(() -> callback.onError("Network request failed: " + e.getMessage()));
        }
    }
}
