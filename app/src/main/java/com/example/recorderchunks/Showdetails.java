package com.example.recorderchunks;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.view.MenuItem;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.recorderchunks.Model_Class.Event;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class Showdetails extends AppCompatActivity {

    private TextInputEditText eventTitleEditText, eventDescriptionEditText;
    private TextView selectedDateTimeTextView, playbackTimerTextView;
    private MaterialButton datePickerBtn, timePickerBtn;
    private FloatingActionButton playPauseButton;
    private SeekBar playbackProgressBar;

    private MediaPlayer mediaPlayer;
    private Handler handler = new Handler();
    private Runnable updateSeekBarRunnable;
    private boolean isPlaying = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_showdetails);
        Toolbar toolbar = findViewById(R.id.appBar);
        setSupportActionBar(toolbar);

        // Enable back button in toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        // Initialize views
        eventTitleEditText = findViewById(R.id.eventTitle);
        eventDescriptionEditText = findViewById(R.id.eventDescription);
        selectedDateTimeTextView = findViewById(R.id.selectedDateTime);
        playbackTimerTextView = findViewById(R.id.playbackTimer);
        datePickerBtn = findViewById(R.id.datePickerBtn);
        timePickerBtn = findViewById(R.id.timePickerBtn);
        playPauseButton = findViewById(R.id.playPauseButton);
        playbackProgressBar = findViewById(R.id.playbackProgressBar);

        // Get the event ID passed from the previous activity
        int eventId = getIntent().getIntExtra("eventId", -1);

        if (eventId != -1) {
            // Fetch event details from the database
            DatabaseHelper db = new DatabaseHelper(this);
            Event event = db.getEventById(eventId);

            if (event != null) {
                // Populate the views with event details
                eventTitleEditText.setText(event.getTitle());
                eventDescriptionEditText.setText(event.getDescription());
                selectedDateTimeTextView.setText("created on : " +
                        event.getCreationDate() + " at  " + event.getCreationTime());
                datePickerBtn.setText(event.getEventDate());
                timePickerBtn.setText(event.getEventTime());

                // Make the EditText fields uneditable
                eventTitleEditText.setFocusable(false);
                eventTitleEditText.setClickable(false);
                eventDescriptionEditText.setFocusable(false);
                eventDescriptionEditText.setClickable(false);

                // Initialize audio playback
                String audioFilePath = event.getAudioPath();
                if (audioFilePath != null && !audioFilePath.isEmpty()) {
                    setupAudioPlayback(audioFilePath);
                } else {
                    Toast.makeText(this, "No audio file available", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show();
                finish(); // Close the activity if the event is not found
            }
        } else {
            Toast.makeText(this, "Invalid Event ID", Toast.LENGTH_SHORT).show();
            finish(); // Close the activity if no valid ID is passed
        }
    }

    private void setupAudioPlayback(String audioFilePath) {
        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(audioFilePath);
            mediaPlayer.prepare();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error loading audio", Toast.LENGTH_SHORT).show();
            return;
        }

        playbackProgressBar.setMax(mediaPlayer.getDuration());
        playbackProgressBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    mediaPlayer.seekTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                handler.removeCallbacks(updateSeekBarRunnable);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                handler.post(updateSeekBarRunnable);
            }
        });

        playPauseButton.setOnClickListener(view -> togglePlayPause());

        updateSeekBarRunnable = new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    playbackProgressBar.setProgress(mediaPlayer.getCurrentPosition());
                    updatePlaybackTimer(mediaPlayer.getCurrentPosition());
                    handler.postDelayed(this, 1000);
                }
            }
        };
    }

    private void togglePlayPause() {
        if (isPlaying) {
            pauseAudio();
        } else {
            playAudio();
        }
    }

    private void playAudio() {
        mediaPlayer.start();
        isPlaying = true;
        playPauseButton.setImageResource(R.drawable.baseline_play_circle_outline_24);
        handler.post(updateSeekBarRunnable);
    }

    private void pauseAudio() {
        mediaPlayer.pause();
        isPlaying = false;
        playPauseButton.setImageResource(R.drawable.baseline_play_circle_24);
        handler.removeCallbacks(updateSeekBarRunnable);
    }

    private void updatePlaybackTimer(int currentPosition) {
        String time = String.format("%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(currentPosition),
                TimeUnit.MILLISECONDS.toSeconds(currentPosition) % 60);
        playbackTimerTextView.setText(time);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        handler.removeCallbacks(updateSeekBarRunnable);
        finish();
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
