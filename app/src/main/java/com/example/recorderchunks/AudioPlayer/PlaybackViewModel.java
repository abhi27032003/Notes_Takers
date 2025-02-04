package com.example.recorderchunks.AudioPlayer;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class PlaybackViewModel extends ViewModel {
    private final AudioPlayerManager audioPlayerManager = AudioPlayerManager.getInstance();

    public LiveData<String> getCurrentRecordingId() {
        return audioPlayerManager.getCurrentRecordingId();
    }

    public LiveData<Boolean> getIsPlaying() {
        return audioPlayerManager.getIsPlaying();
    }

    public LiveData<Integer> getCurrentPosition() {
        return audioPlayerManager.getCurrentPosition();
    }

    public LiveData<Integer> getTotalDuration() {
        return audioPlayerManager.getTotalDuration();
    }

    public void playAudio(String filePath, String recordingId) {
        audioPlayerManager.playAudio(filePath, recordingId);
    }

    public void pauseAudio() {
        audioPlayerManager.pauseAudio();
    }

    public void resumeAudio() {
        audioPlayerManager.resumeAudio();
    }

    public void seekTo(int position) {
        audioPlayerManager.seekTo(position);
    }
}

