package com.example.recorderchunks.Activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.recorderchunks.Audio_Models.ModelDownloader;

public class NotificationActionReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if ("CANCEL_DOWNLOAD".equals(intent.getAction())) {
            String language = intent.getStringExtra("language");
            if (language != null) {
                ModelDownloader.cancelDownload(language);
                Log.d("NotificationActionReceiver", "Download cancelled for: " + language);
            }
        }
    }
}
