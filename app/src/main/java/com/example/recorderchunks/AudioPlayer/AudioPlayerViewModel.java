package com.example.recorderchunks.AudioPlayer;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class AudioPlayerViewModel extends ViewModel {
    private final MutableLiveData<String> currentRecordingId = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isPlaying = new MutableLiveData<>(false);
    private final MutableLiveData<Integer> seekPosition = new MutableLiveData<>(0);

    public LiveData<String> getCurrentRecordingId() {
        return currentRecordingId;
    }

    public LiveData<Boolean> getIsPlaying() {
        return isPlaying;
    }

    public LiveData<Integer> getSeekPosition() {
        return seekPosition;
    }

    public void setCurrentRecording(String recordingId) {
        currentRecordingId.setValue(recordingId);
    }

    public void setPlaying(boolean playing) {
        isPlaying.setValue(playing);
    }

    public void setSeekPosition(int position) {
        seekPosition.setValue(position);
    }
}
