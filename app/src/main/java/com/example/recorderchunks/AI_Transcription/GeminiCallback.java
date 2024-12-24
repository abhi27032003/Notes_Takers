package com.example.recorderchunks.AI_Transcription;

public interface GeminiCallback {
    void onSuccess(String result);
    void onFailure(String error);
}
