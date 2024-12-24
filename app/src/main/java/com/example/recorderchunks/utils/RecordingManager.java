package com.example.recorderchunks.utils;

import android.content.Context;
import android.view.View;

public class RecordingManager {
    private static RecordingUtils recordingUtils;

    public static RecordingUtils getInstance(Context context, RecordingUtils.RecordingCallback recordingCallback) {
        if (recordingUtils == null) {
            recordingUtils = new RecordingUtils(context, recordingCallback);
        }
        return recordingUtils;
    }

    public static RecordingUtils getExistingInstance() {
        return recordingUtils;
    }

}
