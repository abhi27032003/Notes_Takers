package com.example.recorderchunks.AI_Transcription;

import static android.content.Context.MODE_PRIVATE;


import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.text.TextUtils;
import android.widget.Toast;

import com.android.volley.Request;
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

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


public class AI_Notemaking {
    public static void get_gemini_note(Context context, String prompt, GeminiCallback callback)
    {
        try {
            GenerativeModel gm = new GenerativeModel(
                    "gemini-1.5-flash-001",
                    "AIzaSyDp4QqV17XLUsZsSjgCLKdZdVTcWCZqeUk"
            );

            GenerativeModelFutures model = GenerativeModelFutures.from(gm);

            Toast.makeText(context, "'" + prompt + "'", Toast.LENGTH_SHORT).show();

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
    public static String  getoutput_chatgpt(String prompt,Context context)
    {
        SharedPreferences sharedPreferences;
        RequestQueue requestQueue;
        requestQueue = Volley.newRequestQueue(context);
        sharedPreferences = context.getSharedPreferences("ApiKeysPref", MODE_PRIVATE);
        final String[] output = {"some error"};
        String apiKey = sharedPreferences.getString("ChatGptApiKey", "");
        String apiUrl = "https://api.openai.com/v1/completions"; // Replace with ChatGPT endpoint

        if (TextUtils.isEmpty(apiKey)) {

            return "ChatGpt API Key Missing please add it";
        }

        try {
            JSONObject jsonRequest = new JSONObject();
            jsonRequest.put("model", "text-davinci-003"); // Replace with your model
            jsonRequest.put("prompt", prompt);
            jsonRequest.put("max_tokens", 100);

            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                    Request.Method.POST,
                    apiUrl,
                    jsonRequest,
                    response -> {
                        try {
                            output[0] = response.getString("choices");
                        } catch (JSONException e) {
                            output[0]= e.getMessage();
                        }
                    },
                    error -> output[0]=error.getMessage()
            ) {
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Authorization", "Bearer " + apiKey);
                    headers.put("Content-Type", "application/json");
                    return headers;
                }
            };

            requestQueue.add(jsonObjectRequest);

        } catch (JSONException e) {
            output[0]="Error creating request";
        }
        return output[0];
    }
}
