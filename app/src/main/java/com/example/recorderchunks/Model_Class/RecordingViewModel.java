package com.example.recorderchunks.Model_Class;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class RecordingViewModel extends ViewModel {
    private final MutableLiveData<Boolean> isRecording = new MutableLiveData<>(false);
    private final MutableLiveData<Integer> elapsedSeconds = new MutableLiveData<>(0);
    private final MutableLiveData<String> recordingFilePath = new MutableLiveData<>("");
    private final MutableLiveData<Boolean> isPaused = new MutableLiveData<>(false);
    private final MutableLiveData<Integer> pausedDuration = new MutableLiveData<>(0);

    private int totalElapsedTime = 0;  // To track total elapsed time including paused time
    private boolean isTimerRunning = false;  // To track if the timer is running
    private Thread timerThread;  // Thread to handle the timer updates

    public LiveData<Boolean> getIsRecording() {
        return isRecording;
    }

    public LiveData<Integer> getElapsedSeconds() {
        return elapsedSeconds;
    }

    public LiveData<String> getRecordingFilePath() {
        return recordingFilePath;
    }

    public void setRecording(boolean recording) {
        isRecording.setValue(recording);
    }

    public void updateElapsedSeconds(int seconds) {
        elapsedSeconds.postValue(seconds);
    }

    public void setRecordingFilePath(String path) {
        recordingFilePath.setValue(path);
    }

    public LiveData<Boolean> getIsPaused() {
        return isPaused;
    }

    public void setPaused(boolean paused) {
        isPaused.setValue(paused);
    }

    public LiveData<Integer> getPausedDuration() {
        return pausedDuration;
    }

    public void setPausedDuration(int duration) {
        pausedDuration.setValue(duration);
    }

    // Start the timer
    public void startTimer() {
        if (isTimerRunning) return; // Prevent starting multiple threads

        isTimerRunning = true;

        timerThread = new Thread(() -> {
            int seconds = totalElapsedTime;

            while (isRecording.getValue() != null && isRecording.getValue()) {
                try {
                    Thread.sleep(1000); // Wait for 1 second
                    seconds++;
                    totalElapsedTime = seconds; // Update total elapsed time
                    elapsedSeconds.postValue(seconds); // Update LiveData
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }
            }
            isTimerRunning = false; // Timer stops when recording stops
        });
        timerThread.start();
    }

    // Pause the timer
    public void pauseTimer() {
        isTimerRunning = false; // Stop the timer thread
        if (timerThread != null && timerThread.isAlive()) {
            timerThread.interrupt(); // Safely interrupt the thread
        }

        // Store the elapsed time when paused
        setPausedDuration(totalElapsedTime);
    }

    // Resume the timer from the paused point
    public void resumeTimer() {
        totalElapsedTime = pausedDuration.getValue() != null ? pausedDuration.getValue() : totalElapsedTime;
        startTimer(); // Start the timer from the paused point
    }

    // Reset the timer
    public void resetTimer() {
        pauseTimer(); // Pause the timer
        totalElapsedTime = 0; // Reset total elapsed time
        elapsedSeconds.setValue(0); // Reset elapsed time LiveData
    }
}
