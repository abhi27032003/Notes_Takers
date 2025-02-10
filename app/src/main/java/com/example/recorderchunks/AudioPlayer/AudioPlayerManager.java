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
    private String currentFilePath;
    private String currentRecordingId;
    private AudioPlayerViewModel viewModel;
    private Handler handler;
    private Runnable seekRunnable;

    private AudioPlayerManager(AudioPlayerViewModel viewModel) {
        if (viewModel == null) {
            throw new IllegalArgumentException("ViewModel cannot be null");
        }
        this.viewModel = viewModel;
        mediaPlayer = new MediaPlayer();
        handler = new Handler();
    }

    public static synchronized AudioPlayerManager getInstance(AudioPlayerViewModel viewModel) {
        if (instance == null) {
            instance = new AudioPlayerManager(viewModel);
        }
        return instance;
    }

    public void playRecording(String recordingId, String filePath) {
        if (mediaPlayer.isPlaying() && filePath.equals(currentFilePath)) {
            pauseRecording();
            return;
        }
        stopRecording();

        try {
            mediaPlayer.reset();
            mediaPlayer.setDataSource(filePath);
            mediaPlayer.prepare();
            mediaPlayer.start();

            currentFilePath = filePath;
            currentRecordingId = recordingId;

            viewModel.setCurrentRecording(recordingId);
            viewModel.setPlaying(true);

            // Start updating seek position periodically
            startSeekUpdate();

            mediaPlayer.setOnCompletionListener(mp -> {
                viewModel.setPlaying(false);
                viewModel.setCurrentRecording("null");
                stopSeekUpdate();
            });

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void pauseRecording() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            viewModel.setPlaying(false);
        }
        stopSeekUpdate();
    }

    public void stopRecording() {
        if (mediaPlayer.isPlaying() || mediaPlayer.getCurrentPosition() > 0) {
            mediaPlayer.stop();
            viewModel.setPlaying(false);
            viewModel.setCurrentRecording("null");
        }
        stopSeekUpdate();
    }

    public void seekTo(int position) {
        if (mediaPlayer != null) {
            mediaPlayer.seekTo(position);
            viewModel.setSeekPosition(position);
        }
    }

    public boolean isPlaying() {
        return mediaPlayer.isPlaying();
    }

    private void startSeekUpdate() {
        seekRunnable = new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer.isPlaying()) {
                    int currentPosition = mediaPlayer.getCurrentPosition();
                    viewModel.setSeekPosition(currentPosition/1000);
                    handler.postDelayed(this, 1000); // Update every second
                }
            }
        };
        handler.post(seekRunnable);
    }

    private void stopSeekUpdate() {
        if (seekRunnable != null) {
            handler.removeCallbacks(seekRunnable);
        }
    }
}
