package com.example.recorderchunks.AudioPlayer;

import android.media.MediaPlayer;
import android.os.Handler;
import android.util.Log;

import com.example.recorderchunks.MyApplication;

import java.io.IOException;

public class AudioPlayerManager {
    private static AudioPlayerManager instance;
    private MediaPlayer mediaPlayer;
    private String currentFilePath="-1";
    private String currentRecordingId="-1";
    private AudioPlayerViewModel viewModel;
    private Handler handler;
    private Runnable seekRunnable;

    private AudioPlayerManager() {
        this.viewModel = MyApplication.getInstance().getViewModelProvider().get(AudioPlayerViewModel.class); // Get global ViewModel
        if (viewModel == null) {
            throw new IllegalArgumentException("ViewModel cannot be null");
        }

        mediaPlayer = new MediaPlayer();
        handler = new Handler();
    }

    public static synchronized AudioPlayerManager getInstance() {
        if (instance == null) {
            instance = new AudioPlayerManager();
        }
        return instance;
    }

    public void playRecording(String recordingId, String filePath) {
        if ( filePath.equals(currentFilePath)) {
            resumeRecording();
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

            viewModel.setCurrentRecording(Integer.parseInt(recordingId));
            viewModel.setPlaying(true);

            // Start updating seek position periodically
            startSeekUpdate();

            mediaPlayer.setOnCompletionListener(mp -> {
                viewModel.setPlaying(false);
                viewModel.setCurrentRecording(-1);
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
            Log.v("playback_test", "Recording Paused");
        }



        stopSeekUpdate();
    }

    public void stopRecording() {
        if (mediaPlayer.isPlaying() || mediaPlayer.getCurrentPosition() > 0) {
            mediaPlayer.stop();
            viewModel.setPlaying(false);
            viewModel.setSeekPosition(0);
            viewModel.setCurrentRecording(-1);
            currentRecordingId="-1";
            currentFilePath="-1";

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

    public String current_playing_recording_id() {
        return currentRecordingId;
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
    public void resumeRecording() {
        if (!mediaPlayer.isPlaying() && currentFilePath != null) {
            mediaPlayer.start();
            viewModel.setPlaying(true);
            startSeekUpdate();
            Log.v("playback_test", "Recording Resumed");
        }
        else
        {            Log.v("playback_test", "Recording Resumed 2");



        }
    }
    private void stopSeekUpdate() {
        if (seekRunnable != null) {
            handler.removeCallbacks(seekRunnable);
        }
    }
}
