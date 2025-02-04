package com.example.recorderchunks.AudioPlayer;

import android.content.Context;
import android.media.MediaPlayer;
import android.os.Handler;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.io.IOException;

public class AudioPlayerManager {
    private static AudioPlayerManager instance;
    private MediaPlayer mediaPlayer;
    private Handler handler = new Handler();
    private Runnable updateSeekBar;

    private final MutableLiveData<String> currentRecordingId = new MutableLiveData<>(null);
    private final MutableLiveData<Boolean> isPlaying = new MutableLiveData<>(false);
    private final MutableLiveData<Integer> currentPosition = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> totalDuration = new MutableLiveData<>(0);

    private AudioPlayerManager() {
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnCompletionListener(mp -> stopAudio());
    }

    public static synchronized AudioPlayerManager getInstance() {
        if (instance == null) {
            instance = new AudioPlayerManager();
        }
        return instance;
    }

    public void playAudio(String filePath, String recordingId) {
        stopAudio();

        try {
            mediaPlayer.setDataSource(filePath);
            mediaPlayer.prepare();
            mediaPlayer.start();

            currentRecordingId.postValue(recordingId);
            isPlaying.postValue(true);
            totalDuration.postValue(mediaPlayer.getDuration());

            updateSeekBar = new Runnable() {
                @Override
                public void run() {
                    currentPosition.postValue(mediaPlayer.getCurrentPosition());
                    handler.postDelayed(this, 500);
                }
            };
            handler.post(updateSeekBar);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void pauseAudio() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            isPlaying.postValue(false);
            handler.removeCallbacks(updateSeekBar);
        }
    }

    public void resumeAudio() {
        if (!mediaPlayer.isPlaying()) {
            mediaPlayer.start();
            isPlaying.postValue(true);
            handler.post(updateSeekBar);
        }
    }

    public void stopAudio() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            mediaPlayer.reset();
            isPlaying.postValue(false);
            currentRecordingId.postValue(null);
            handler.removeCallbacks(updateSeekBar);
        }
    }

    public void seekTo(int position) {
        mediaPlayer.seekTo(position);
        currentPosition.postValue(position);
    }

    public LiveData<String> getCurrentRecordingId() {
        return currentRecordingId;
    }

    public LiveData<Boolean> getIsPlaying() {
        return isPlaying;
    }

    public LiveData<Integer> getCurrentPosition() {
        return currentPosition;
    }

    public LiveData<Integer> getTotalDuration() {
        return totalDuration;
    }
}
