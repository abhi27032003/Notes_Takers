package com.example.recorderchunks.Adapter;

import static com.example.recorderchunks.Activity.API_Updation.SELECTED_LANGUAGE;
import static com.example.recorderchunks.Activity.API_Updation.getLanguagesFromMetadata;
import static com.example.recorderchunks.utils.AudioUtils.convertToWav;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.jean.jcplayer.view.JcPlayerView;

import com.example.recorderchunks.AI_Transcription.AudioChunkHelper;
import com.example.recorderchunks.AI_Transcription.TranscriptionUtils;
import com.example.recorderchunks.Audio_Models.Vosk_Model;
import com.example.recorderchunks.Helpeerclasses.Chunks_Database_Helper;
import com.example.recorderchunks.Helpeerclasses.DatabaseHelper;
import com.example.recorderchunks.Model_Class.ChunkTranscription;
import com.example.recorderchunks.Model_Class.Recording;
import com.example.recorderchunks.R;
import com.example.recorderchunks.utils.AudioUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AudioRecyclerAdapter extends RecyclerView.Adapter<AudioRecyclerAdapter.AudioViewHolder> {
    public static ArrayList<Recording> recordingList;
    private final Context context;
    private static MediaPlayer mediaPlayer;
    private static int currentPlayingPosition = -1;
    private static final Handler handler = new Handler();
    private static Runnable updateSeekBarRunnable;
    public static ArrayList<String> selectedItems;
    private static SharedPreferences sharedPreferences,sharedPreferences2;
    private static final String PREFS_NAME = "PromptSelectionPrefs";
    private OnSelectionChangedListener selectionChangedListener;

    private DatabaseHelper databaseHelper;
    private Chunks_Database_Helper chunks_database_helper;

    Vosk_Model vm;






    public AudioRecyclerAdapter(ArrayList<Recording> recordingList, Context context, OnSelectionChangedListener listener) {
        this.recordingList = recordingList;
        if (context == null) {
            throw new IllegalArgumentException("Context cannot be null");
        }
        this.context =  context.getApplicationContext();;
        this.selectedItems = new ArrayList<>();
        this.sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.selectionChangedListener = listener;
        this.vm=new Vosk_Model();
        this.databaseHelper=new DatabaseHelper(context);
        this.chunks_database_helper=new Chunks_Database_Helper(context);
        this.sharedPreferences2 = context.getSharedPreferences("ApiKeysPref", Context.MODE_PRIVATE);
    }

    @NonNull
    @Override
    public AudioViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recorder, parent, false);
        return new AudioViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AudioViewHolder holder, int position) {
        holder.itemView.setFocusable(false);
        Vosk_Model.initializeVoskModel(context);
        holder.playbackProgressBar.setFocusable(false);

        Recording audioItem = recordingList.get(position);
        loadSelectionState(audioItem.getEventId());
        /////////////////////////////////////////////////chunking and api calling logic

        List<String> chunkPaths = AudioChunkHelper.splitAudioIntoChunks(audioItem.getUrl(), 2000);
        transcribe_and_chunkify_audio(chunkPaths,audioItem.getRecordingId(),"sdfgrtfhi7tyhb671987fgytdtf");


        if(audioItem.getIs_transcribed().equals("no"))
        {
            holder.transcription_progress.setVisibility(View.VISIBLE);
            holder.transcription_progress_api.setVisibility(View.VISIBLE);
            try {
                handleTranscription(audioItem,position);

            }catch (Exception e)
            {
                Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
            }

        }
        else
        {
            holder.transcription_progress.setVisibility(View.GONE);

        }
        if(audioItem.getIs_transcribed_api().equals("no"))
        {
            holder.transcription_progress_api.setVisibility(View.VISIBLE);
            holder.Description_api.setText("");

            try {
                String fileExtension = AudioUtils.getFileExtension(audioItem.getUrl()).toLowerCase();
                String[] supportedFormats = {"wav", "3gp", "m4a", "mp3", "webm", "mp4", "mpga", "mpeg"};

                if (Arrays.asList(supportedFormats).contains(fileExtension)) {
                    TranscriptionUtils.transcribeAudio(context,audioItem.getUrl(), new TranscriptionUtils.TranscriptionCallback() {
                        @Override
                        public void onSuccess(String transcription) {
                            holder.transcription_progress_api.setVisibility(View.GONE);
                            holder.Description_api.setText(transcription);
                            databaseHelper.updaterecording_details_api(audioItem.getRecordingId(),transcription);
                            audioItem.setIs_transcribed_api("yes");
                            audioItem.setDescription_api(transcription);

                        }

                        @Override
                        public void onError(String errorMessage) {
                            holder.transcription_progress_api.setVisibility(View.GONE);
                            holder.Description_api.setText(errorMessage.toString());
                            audioItem.setDescription_api("error "+errorMessage.toString());
                            audioItem.setIs_transcribed_api("yes");

                        }
                    });

                }
                else {
                    databaseHelper.updaterecording_details_api(audioItem.getRecordingId(),"Unsupported format");
                    audioItem.setIs_transcribed_api("yes");
                    audioItem.setDescription_api("Unsupported format");
                    holder.Description_api.setText("Unsupported format");
                }


            }catch (Exception e)
            {
              //  Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
            }

        }
        else
        {
            holder.transcription_progress_api.setVisibility(View.GONE);

        }
        ////////////////////////////////////language spinner
        String[] languagesl = {
                "English",
                "French",
                "Chinese",
                "Hindi",
                "Spanish"
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                context,
                android.R.layout.simple_spinner_item,
                languagesl
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        holder.languageSpinner.setAdapter(adapter);

        // Fetch the saved language from shared preferences
        String savedLanguage = sharedPreferences2.getString(SELECTED_LANGUAGE, null);
       // Toast.makeText(context, "Saved Language: " + savedLanguage, Toast.LENGTH_SHORT).show();

        if (savedLanguage != null) {
            // Find the index of the saved language in the array
            for (int i = 0; i < languagesl.length; i++) {
                if (languagesl[i].equals(audioItem.getLanguage())) {
                    holder.languageSpinner.setSelection(i);
                    break;
                }
            }
        }

        // Set listener for spinner
        holder.languageSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedLanguage = languagesl[position];
                // Update database only if the language changes
                if (!selectedLanguage.equals(audioItem.getLanguage())) {
                    boolean isUpdated = databaseHelper.updateLanguageByRecordingId(audioItem.getRecordingId(), selectedLanguage);
                    if (isUpdated) {
                        // Update local data
                        audioItem.setLanguage(selectedLanguage);
                    } else {
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Handle case when no selection is made (optional)
            }
        });

        ///////////////////////////////////////////////////////////



        if (selectedItems.contains(audioItem.getDescription())) {
            holder.recordingCard.setCardBackgroundColor(context.getResources().getColor(R.color.third));
            holder.add_to_list.setImageResource(R.drawable.baseline_cancel_24); // Replace with your "remove" icon resource
        } else {
            holder.recordingCard.setCardBackgroundColor(context.getResources().getColor(R.color.fourth));
            holder.add_to_list.setImageResource(R.drawable.baseline_add_24); // Replace with your "add" icon resource
        }

        holder.add_to_list.setOnClickListener(v -> {
            if (selectedItems.contains(audioItem.getDescription())) {
                // Remove the item from the selected list
                selectedItems.remove(audioItem.getDescription());
                holder.add_to_list.setImageResource(R.drawable.baseline_add_24); // Change to "add" icon
                holder.recordingCard.setCardBackgroundColor(context.getResources().getColor(R.color.fourth));
//           holder.recordingCard.setCardBackgroundColor(context.getResources().getColor(R.color.fourth));

                saveSelectionState(audioItem.getEventId());
                notifySelectionChanged();
               // Toast.makeText(context, "Removed from selection", Toast.LENGTH_SHORT).show();
            } else {
                // Add the item to the selected list
                selectedItems.add(audioItem.getDescription());
                holder.add_to_list.setImageResource(R.drawable.baseline_cancel_24);
                holder.recordingCard.setCardBackgroundColor(context.getResources().getColor(R.color.third));
// Change to "remove" icon
                saveSelectionState(audioItem.getEventId());
                notifySelectionChanged();
               // Toast.makeText(context, "Added to selection", Toast.LENGTH_SHORT).show();
            }
        });
       holder.bindAudioData(audioItem, position);
       if(position!=currentPlayingPosition)
       {
           //resetPlayerUI(holder);
       }

        //
        /////////////////////////////////////////
        holder.playPauseButton.setOnClickListener(v -> {
            if (currentPlayingPosition == position) {
                togglePlayPause(holder);
            } else {

                playNewAudio(holder, audioItem, position);
            }
        });

        // Handle seek bar changes
        holder.playbackProgressBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && mediaPlayer != null) {
                    mediaPlayer.seekTo(progress);
                    updateElapsedTime(holder);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        holder.delete.setOnClickListener(v -> {
            Context activityContext = v.getContext(); // Get the context from the view

            if (activityContext instanceof Activity && !((Activity) activityContext).isFinishing()) {
                new AlertDialog.Builder(activityContext)
                        .setTitle("Delete Recording")
                        .setMessage("Are you sure you want to delete this recording?")
                        .setPositiveButton("Yes", (dialog, which) -> {
                            DatabaseHelper databaseHelper = new DatabaseHelper(activityContext);

                            // Delete the recording from database and file storage
                            boolean isDeleted = databaseHelper.deleteRecordingById(audioItem.getRecordingId());
                            if (isDeleted) {
                                selectedItems.remove(audioItem.getDescription());
                                saveSelectionState(audioItem.getEventId());
                                notifySelectionChanged();

                                // Remove from the list and notify adapter
                                if (position != RecyclerView.NO_POSITION) { // Ensure position is valid
                                    recordingList.remove(position); // Remove the item from the data source
                                    notifyItemRemoved(position); // Notify the adapter of item removal
                                }
                            } else {
                                Toast.makeText(activityContext, "Failed to delete recording.", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton("No", null)
                        .show();
            } else {
                Toast.makeText(activityContext, "Cannot display dialog, activity is not valid.", Toast.LENGTH_SHORT).show();
            }
        });


        holder.recordingLabel.setText(audioItem.getName());
        holder.ceation_d_and_t.setText("Created at : "+audioItem.getDate()+" in "+audioItem.getLanguage());
        holder.total_time.setText(" / "+convertSecondsToTime(Integer.parseInt(AudioUtils.getAudioDuration(audioItem.getUrl()))));
        holder.Description.setText(audioItem.getDescription());
        holder.expand_button.setOnClickListener(v -> {
            if (holder.Description.getMaxLines() == 1) {
                // Expand the TextView
                holder.Description.setMaxLines(Integer.MAX_VALUE);
                holder.expand_button.setImageResource(R.mipmap.collapse);
            } else {
                // Collapse the TextView
                holder.Description.setMaxLines(1);
                holder.expand_button.setImageResource(R.mipmap.expand);
            }
        });

        ////////////api///////////////////////////////////////
        holder.expand_button_api.setOnClickListener(v -> {
            if (holder.Description_api.getMaxLines() == 1) {
                // Expand the TextView
                holder.Description_api.setMaxLines(Integer.MAX_VALUE);
                holder.expand_button_api.setImageResource(R.mipmap.collapse);
            } else {
                // Collapse the TextView
                holder.Description_api.setMaxLines(1);
                holder.expand_button_api.setImageResource(R.mipmap.expand);
            }
        });




        if(recordingList.get(position).isRecorded())
        {
            holder.Start_Transcription.setText("Recorded");
            holder.Start_Transcription.setBackgroundColor(context.getResources().getColor(R.color.secondary));
        }
        else
        {
            holder.Start_Transcription.setText("Imported");
            holder.Start_Transcription.setBackgroundColor(context.getResources().getColor(R.color.nav));
        }

    }

    private void transcribe_and_chunkify_audio(List<String> chunkPaths, int recordingId, String uuid) {
        boolean added_to_db =chunks_database_helper.addChunksBatch(chunkPaths,recordingId,uuid);
        boolean allInProgress = true;
        if(added_to_db)
        {
            List<ChunkTranscription> chunks = chunks_database_helper.getChunksByRecordingId(recordingId);
            for (ChunkTranscription chunkTranscription:chunks) {
                String status=chunkTranscription.getTranscriptionStatus();
                if(status.contains("not")||status.contains("started")||status.contains("not_started"))
                {
                    allInProgress=false;
                    send_chunk_for_transcription(chunkTranscription.getChunkPath(),chunkTranscription.getChunkId());
                }

            }


        }
        else {
            transcribe_and_chunkify_audio(chunkPaths,recordingId,uuid);

        }
        if(allInProgress)
        {
            get_transcription(recordingId);
        }
    }

    private void get_transcription(int recordingId) {

    }

    private void send_chunk_for_transcription(String chunkPath, String chunkId) {

    }


    private void handleTranscription(Recording audioItem, int position) {
        String fileExtension = AudioUtils.getFileExtension(audioItem.getUrl()).toLowerCase();
        String[] supportedFormats = {"wav", "3gp"};
        if (!Arrays.asList(supportedFormats).contains(fileExtension)) {
            boolean updateSuccess = databaseHelper.updateRecordingDetails(audioItem.getRecordingId(), "Unsupported Format");

            // Switch back to main thread to update UI or notify adapter
            new Handler(Looper.getMainLooper()).post(() -> {
                if (updateSuccess) {
                    Log.d("hellorecorder", "Recording updated successfully.");
                    audioItem.setDescription("Unsupported Format"); // Assuming the setDescription method updates COL_DES
                    audioItem.setIs_transcribed("yes"); // Assuming this is the flag for COL_IS_TRANSCRIBED

                    recordingList.set(position, audioItem);

                    // Notify adapter about the item change
                    notifyItemChanged(position);
                } else {
                    Log.d("hellorecorder", "Failed to update the recording.+Unsupported Format");
                }
            });
            return ;
        }
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> {
            try {
                String transcription = Vosk_Model.recognizeSpeech(convertToWav(audioItem.getUrl(), context));

                if (transcription != null && !transcription.isEmpty()) {
                    // Update the database
                    boolean updateSuccess = databaseHelper.updateRecordingDetails(audioItem.getRecordingId(), transcription);

                    // Switch back to main thread to update UI or notify adapter
                    new Handler(Looper.getMainLooper()).post(() -> {
                        if (updateSuccess) {
                            Log.d("hellorecorder", "Recording updated successfully.");
                            audioItem.setDescription(transcription); // Assuming the setDescription method updates COL_DES
                            audioItem.setIs_transcribed("yes"); // Assuming this is the flag for COL_IS_TRANSCRIBED

                            recordingList.set(position, audioItem);

                            // Notify adapter about the item change
                            notifyItemChanged(position);
                            AudioRecyclerAdapter.selectedItems.add(transcription);
                            saveSelectionState(audioItem.getEventId());
                            notifySelectionChanged();
                        } else {
                            Log.d("hellorecorder", "Failed to update the recording.");
                        }
                    });
                } else {
                    boolean updateSuccess = databaseHelper.updateRecordingDetails(audioItem.getRecordingId(), "Unable to generate Transcription");

                    // Switch back to main thread to update UI or notify adapter
                    new Handler(Looper.getMainLooper()).post(() -> {
                        if (updateSuccess) {
                            Log.d("hellorecorder", "Recording updated successfully.");
                            audioItem.setDescription("Unable to generate Transcription"); // Assuming the setDescription method updates COL_DES
                            audioItem.setIs_transcribed("yes"); // Assuming this is the flag for COL_IS_TRANSCRIBED
                            recordingList.set(position, audioItem);
                            notifyItemChanged(position);
                        } else {
                            Log.d("hellorecorder", "Failed to update the recording.");
                        }
                    });
                }
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() ->

                        Log.d("hellorecorder", "Error during speech recognition: " + e.getMessage())
                );
                audioItem.setDescription("Error during speech recognition: " + e.getMessage());
                audioItem.setIs_transcribed("yes"); // Assuming this is the flag for COL_IS_TRANSCRIBED
                recordingList.set(position, audioItem);
                notifyItemChanged(position);
            }
        });

        executorService.shutdown();
    }
    private void togglePlayPause(AudioViewHolder holder) {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            holder.playPauseButton.setImageResource(com.example.jean.jcplayer.R.drawable.ic_play);
            stopSeekBarUpdates();
        } else {
            mediaPlayer.start();
            holder.playPauseButton.setImageResource(com.example.jean.jcplayer.R.drawable.ic_pause);
            startSeekBarUpdates(holder);
        }
    }

    private void playNewAudio(AudioViewHolder holder, Recording audioItem, int position) {

        // Stop the previous playback
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            stopSeekBarUpdates();
        }
        int previousPlayingPosition = currentPlayingPosition;
        currentPlayingPosition = position;
        if(previousPlayingPosition!=-1)
        {
            notifyItemChanged(previousPlayingPosition); // Reset the previous item's view

        }
        //notifyItemChanged(position);

        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(audioItem.getUrl());
            mediaPlayer.prepare(); // Prepares the player synchronously


            // Start playback
            mediaPlayer.start();
            holder.playPauseButton.setImageResource(com.example.jean.jcplayer.R.drawable.ic_pause);
            holder.playbackProgressBar.setMax(mediaPlayer.getDuration());
            mediaPlayer.setOnCompletionListener(mp -> resetPlayerUI(holder,position));
            currentPlayingPosition = position;

            // Start updating the SeekBar
            startSeekBarUpdates(holder);

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(context, "Failed to play audio", Toast.LENGTH_SHORT).show();
        }
    }


    private void startSeekBarUpdates(AudioViewHolder holder) {
        updateSeekBarRunnable = new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null) {
                    holder.playbackProgressBar.setProgress(mediaPlayer.getCurrentPosition());
                    updateElapsedTime(holder);
                    handler.postDelayed(this, 10);
                    if(mediaPlayer.getDuration()==holder.playbackProgressBar.getProgress())
                    {
                        holder.playbackProgressBar.setProgress(0);
                        holder.playbackTimer.setText("00:00");
                        holder.playPauseButton.setImageResource(com.example.jean.jcplayer.R.drawable.ic_play);

                    }
                }
            }
        };
        handler.post(updateSeekBarRunnable);
    }
    public static void saveSelectionState(int id) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putStringSet("selected_items_"+id, new HashSet<>(selectedItems));
        editor.apply();
    }

    private void loadSelectionState(int id) {
        Set<String> savedItems = sharedPreferences.getStringSet("selected_items_"+id, new HashSet<>());
        selectedItems.clear();
        if (savedItems != null) {
            selectedItems.addAll(savedItems);
        }
        notifySelectionChanged();
    }

    private void stopSeekBarUpdates() {
        if (updateSeekBarRunnable != null) {
            handler.removeCallbacks(updateSeekBarRunnable);
        }
    }

    private void resetPlayerUI(AudioViewHolder holder,int currentPlayingPosition) {
        holder.playbackProgressBar.setProgress(0);
        holder.playbackTimer.setText("00:00");
        holder.playPauseButton.setImageResource(com.example.jean.jcplayer.R.drawable.ic_play);


    }

    private void updateElapsedTime(AudioViewHolder holder) {
        if (mediaPlayer != null) {
            int currentPosition = mediaPlayer.getCurrentPosition();
            holder.playbackTimer.setText(formatTime(currentPosition));
        }
    }




    private String formatTime(int millis) {
        int seconds = (millis / 1000) % 60;
        int minutes = (millis / (1000 * 60)) % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
    public static String convertSecondsToTime(int totalSeconds) {
        // Convert seconds to hours, minutes, and seconds
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;

        // Return formatted time
        if (hours > 0) {
            // Include hours if applicable
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            // Only minutes and seconds
            return String.format("%02d:%02d", minutes, seconds);
        }
    }
    @Override
    public int getItemCount() {
        return recordingList.size();
    }

    static class AudioViewHolder extends RecyclerView.ViewHolder {
        ImageView playPauseButton;
        SeekBar playbackProgressBar;
        TextView playbackTimer;
        Button Start_Transcription;
        JcPlayerView JcPlayerView;
        TextView recordingLabel,ceation_d_and_t,total_time,Description,Description_api;
        ImageView expand_button,add_to_list,delete,expand_button_api;

        CardView recordingCard;
        LinearLayout transcription_progress,transcription_progress_api;
        Spinner languageSpinner;

        public AudioViewHolder(@NonNull View itemView) {
            super(itemView);
            transcription_progress=itemView.findViewById(R.id.transcription_progress);
            total_time=itemView.findViewById(R.id.total_time);
            Description=itemView.findViewById(R.id.Description);
            ceation_d_and_t=itemView.findViewById(R.id.ceation_d_and_t);
            recordingLabel=itemView.findViewById(R.id.recordingLabel);
            playPauseButton = itemView.findViewById(R.id.playPauseButton);
            playbackProgressBar = itemView.findViewById(R.id.playbackProgressBar);
            playbackTimer = itemView.findViewById(R.id.playbackTimer);
            JcPlayerView =  itemView.findViewById(R.id.jcplayer);
            Start_Transcription=itemView.findViewById(R.id.Start_Transcription);
            recordingCard=itemView.findViewById(R.id.recordingCard);
            expand_button=itemView.findViewById(R.id.expand_btn);
            delete=itemView.findViewById(R.id.deleteButton);
            add_to_list=itemView.findViewById(R.id.add_to_list);
            expand_button_api=itemView.findViewById(R.id.expand_btn_api);
            transcription_progress_api=itemView.findViewById(R.id.transcription_progress_api);
            Description_api=itemView.findViewById(R.id.Description_api);
            languageSpinner=itemView.findViewById(R.id.language_spinner);



        }
        public void bindAudioData(Recording audioItem, int position) {
            playbackTimer.setText("00:00");
            playbackProgressBar.setProgress(0);
            playbackProgressBar.setMax(0); // Reset seek bar until new audio is loaded
            playPauseButton.setImageResource(ir.one_developer.file_picker.R.drawable.ic_play);
        }

    }
    private void notifySelectionChanged() {
        if (selectionChangedListener != null) {
            selectionChangedListener.onSelectionChanged(selectedItems);
        }
    }

    public interface OnSelectionChangedListener {
        void onSelectionChanged(ArrayList<String> updatedSelection);
    }
}
