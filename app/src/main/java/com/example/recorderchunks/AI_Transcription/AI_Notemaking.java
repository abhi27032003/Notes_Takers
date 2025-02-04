package com.example.recorderchunks.AI_Transcription;

import static android.content.Context.MODE_PRIVATE;


import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;


import androidx.annotation.NonNull;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class AI_Notemaking {
    private static final String API_URL = "https://api.openai.com/v1/chat/completions";
    private static final String API_KEY = "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"; // Replace with your API key
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final OkHttpClient client = new OkHttpClient();
    public static void get_gemini_note(Context context, String prompt, GeminiCallback callback)
    {
        try {
            GenerativeModel gm = new GenerativeModel(
                    "gemini-1.5-flash-001",
                    "AIzaSyDp4QqV17XLUsZsSjgCLKdZdVTcWCZqeUk"
            );

            GenerativeModelFutures model = GenerativeModelFutures.from(gm);

            Toast.makeText(context, "Message passed to Gemini content will be updated soon", Toast.LENGTH_SHORT).show();

            Content content = new Content.Builder()
                    .addText("'" + prompt + "'")
                    .build();

            ListenableFuture<GenerateContentResponse> response = model.generateContent(content);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
                    @Override
                    public void onSuccess(GenerateContentResponse result) {
                        callback.onSuccess(result.getText()); // Pass result to the callback
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        callback.onFailure("Error: " + t.getMessage()); // Pass error to the callback
                    }
                }, context.getMainExecutor());
            } else {
                callback.onFailure("API level not supported");
            }
        } catch (Exception e) {
            callback.onFailure("Error: " + e.getMessage());
        }

    }
    public static void getoutput_chatgpt(Context context, String question, GeminiCallback callback) {
        OkHttpClient client = new OkHttpClient();

        // Create JSON body
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("model", "gpt-3.5-turbo");

            JSONArray messages = new JSONArray();
            JSONObject systemMessage = new JSONObject();
            systemMessage.put("role", "system");
            systemMessage.put("content", "You are a helpful assistant.");

            JSONObject userMessage = new JSONObject();
            userMessage.put("role", "user");
            userMessage.put("content", question);

            messages.put(systemMessage);
            messages.put(userMessage);
            jsonBody.put("messages", messages);

            jsonBody.put("temperature", 0.7);
        } catch (JSONException e) {
            callback.onFailure("JSON Error: " + e.getMessage());
            return;
        }
        Toast.makeText(context, "Message passed to ChatGpt content will be updated soon", Toast.LENGTH_SHORT).show();

        // Create request body
        RequestBody body = RequestBody.create(jsonBody.toString(), MediaType.get("application/json; charset=utf-8"));

        // Create request
        Request request = new Request.Builder()
                .url(API_URL)
                .header("Authorization", "Bearer " + API_KEY)
                .header("Content-Type", "application/json")
                .post(body)
                .build();

        // Execute request asynchronously
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure("Request Failed: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    callback.onFailure("Error: " + response.code() + " - " + response.message());
                    return;
                }

                String responseData = response.body().string();
                try {
                    JSONObject jsonResponse = new JSONObject(responseData);
                    JSONArray choices = jsonResponse.getJSONArray("choices");
                    String reply = choices.getJSONObject(0).getJSONObject("message").getString("content");

                    callback.onSuccess(reply);
                } catch (JSONException e) {
                    callback.onFailure("Parsing Error: " + e.getMessage());
                }
            }
        });
    }
}
