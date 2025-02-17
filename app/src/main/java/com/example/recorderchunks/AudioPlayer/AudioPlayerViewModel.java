package com.example.recorderchunks.AudioPlayer;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import android.util.Pair;

public class AudioPlayerViewModel extends AndroidViewModel {

    private final MutableLiveData<Integer> currentRecordingId = new MutableLiveData<>(-1);
    private final MutableLiveData<Boolean> isPlaying = new MutableLiveData<>(false);
    private final MutableLiveData<Integer> seekPosition = new MutableLiveData<>(0);
    private final MediatorLiveData<Pair<Integer, Boolean>> playbackState = new MediatorLiveData<>();

    public AudioPlayerViewModel(Application application) {
        super(application);

        playbackState.addSource(currentRecordingId, id ->
                playbackState.setValue(new Pair<>(id, isPlaying.getValue() != null ? isPlaying.getValue() : false))
        );
        playbackState.addSource(isPlaying, playing ->
                playbackState.setValue(new Pair<>(currentRecordingId.getValue() != null ? currentRecordingId.getValue() : -1, playing))
        );
    }

    public LiveData<Integer> getCurrentRecordingId() {
        return currentRecordingId;
    }

    public LiveData<Boolean> getIsPlaying() {
        return isPlaying;
    }

    public LiveData<Integer> getSeekPosition() {
        return seekPosition;
    }

    public LiveData<Pair<Integer, Boolean>> getPlaybackState() {
        return playbackState;
    }

    public void setCurrentRecording(Integer recordingId) {
        currentRecordingId.setValue(recordingId);
    }

    public void setPlaying(boolean playing) {
        isPlaying.setValue(playing);
    }

    public void setSeekPosition(int position) {
        seekPosition.setValue(position);
    }
}
