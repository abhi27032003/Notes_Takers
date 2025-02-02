package com.example.recorderchunks.Adapter;


import static com.example.recorderchunks.AI_Transcription.AudioChunkHelper.splitAudioInBackground;
import static com.example.recorderchunks.Activity.API_Updation.SELECTED_LANGUAGE;
import static com.example.recorderchunks.utils.AudioUtils.convertToWav;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
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
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;


import com.example.recorderchunks.AI_Transcription.AudioChunkHelper;
import com.example.recorderchunks.AI_Transcription.TranscriptionUtils;
import com.example.recorderchunks.Activity.activity_text_display;
import com.example.recorderchunks.Audio_Models.Vosk_Model;
import com.example.recorderchunks.Helpeerclasses.Chunks_Database_Helper;
import com.example.recorderchunks.Helpeerclasses.Chunks_Json_helper;
import com.example.recorderchunks.Helpeerclasses.DatabaseHelper;
import com.example.recorderchunks.Helpeerclasses.Model_Database_Helper;
import com.example.recorderchunks.ManageLogs.AppLogger;
import com.example.recorderchunks.Model_Class.ChunkTranscription;
import com.example.recorderchunks.Model_Class.Chunk_Response;
import com.example.recorderchunks.Model_Class.Recording;
import com.example.recorderchunks.R;
import com.example.recorderchunks.utils.AudioUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AudioRecyclerAdapter extends RecyclerView.Adapter<AudioRecyclerAdapter.AudioViewHolder> {
    boolean allTranscriptionProgress = true;
    SharedPreferences prefs_uuid ;
    AppLogger logger ;


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
    public static final String SELECTED_TRANSCRIPTION_METHOD = "SelectedTranscriptionMethod";
    String uuid;
    String signature;







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
        this.prefs_uuid = context.getSharedPreferences("preferences", Context.MODE_PRIVATE);
        this.uuid = prefs_uuid.getString("uuid", null);
        this.signature = prefs_uuid.getString("signature", null);
        this.logger= AppLogger.getInstance(context);
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
        holder.playbackProgressBar.setFocusable(false);

        Recording audioItem = recordingList.get(position);
        loadSelectionState(audioItem.getEventId());
        /////////////////////////////////////////////////chunking and api calling logic

        //////////////////////////////////// transcrtoption method work
        server_open_transcription_disabled(holder);
        local_open_transcription_disabled(holder);
        String defaultt_method= sharedPreferences2.getString(SELECTED_TRANSCRIPTION_METHOD, "Local");
        String selectedModel = sharedPreferences2.getString(SELECTED_TRANSCRIPTION_METHOD+String.valueOf(audioItem.getRecordingId()),defaultt_method); // Default to "Use ChatGPT"
        if ("Server".equalsIgnoreCase(selectedModel)) {
            holder.model_switch.setChecked(true); // Assuming btn1 is for ChatGPT
            Serversideselected(holder);
            SharedPreferences.Editor editor = sharedPreferences2.edit();
            editor.putString(SELECTED_TRANSCRIPTION_METHOD+String.valueOf(audioItem.getRecordingId()), "Server");
            editor.apply();
            Transcribe_Server_with_chunks(audioItem,holder,position);
            if (selectedItems.contains(audioItem.getDescription_api())) {
                holder.recordingCard.setCardBackgroundColor(context.getResources().getColor(R.color.third));
                holder.add_to_list.setImageResource(R.drawable.baseline_cancel_24); // Replace with your "remove" icon resource
            } else {
                holder.recordingCard.setCardBackgroundColor(context.getResources().getColor(R.color.fourth));
                holder.add_to_list.setImageResource(R.drawable.baseline_add_24); // Replace with your "add" icon resource
            }

        } else if ("Local".equalsIgnoreCase(selectedModel)) {

            holder.model_switch.setChecked(false); // Assuming btn1 is for ChatGPT
            LocalSideSelected(holder);
            SharedPreferences.Editor editor = sharedPreferences2.edit();
            editor.putString(SELECTED_TRANSCRIPTION_METHOD+String.valueOf(audioItem.getRecordingId()), "Local");
            editor.apply();
            Transcribe_Local(audioItem,holder,position);
            if (selectedItems.contains(audioItem.getDescription())) {
                holder.recordingCard.setCardBackgroundColor(context.getResources().getColor(R.color.third));
                holder.add_to_list.setImageResource(R.drawable.baseline_cancel_24); // Replace with your "remove" icon resource
            } else {
                holder.recordingCard.setCardBackgroundColor(context.getResources().getColor(R.color.fourth));
                holder.add_to_list.setImageResource(R.drawable.baseline_add_24); // Replace with your "add" icon resource
            }
            //local_open_transcription_disabled(holder);

        }
        else
        {
            holder.model_switch.setChecked(false); // Assuming btn1 is for ChatGPT
            LocalSideSelected(holder);

        }
        holder.model_switch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            String selectedmodel="";

            // Use a switch statement to handle the state
            if (isChecked) {
                selectedmodel = "Server"; // API when the switch is ON
                Serversideselected(holder);
                Transcribe_Server_with_chunks(audioItem,holder,position);
                if(selectedItems.contains(audioItem.getDescription())||selectedItems.contains(audioItem.getDescription_api()))
                {
                    selectedItems.remove(audioItem.getDescription());
                    selectedItems.add(audioItem.getDescription_api());
                    saveSelectionState(audioItem.getEventId());
                    notifySelectionChanged();
                }

            } else {
                selectedmodel = "Local"; // API when the switch is OFF
                LocalSideSelected(holder);
                loadSelectionState(audioItem.getEventId());
                Transcribe_Local(audioItem,holder,position);
                if(selectedItems.contains(audioItem.getDescription())||selectedItems.contains(audioItem.getDescription_api()))
                {
                    selectedItems.remove(audioItem.getDescription_api());
                    selectedItems.add(audioItem.getDescription());
                    saveSelectionState(audioItem.getEventId());
                    notifySelectionChanged();
                }


            }

            // Save the selected API to SharedPreferences
            SharedPreferences.Editor editor = sharedPreferences2.edit();
            editor.putString(SELECTED_TRANSCRIPTION_METHOD+String.valueOf(audioItem.getRecordingId()), selectedmodel);
            editor.apply();

        });

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
        final int[] i = {0};

        // Set listener for spinner
        holder.languageSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedLanguage = languagesl[position];
                // Update database only if the language changes
                if (!selectedLanguage.equals(audioItem.getLanguage())) {
                    boolean isUpdated = databaseHelper.updateLanguageByRecordingId(audioItem.getRecordingId(), selectedLanguage);
                    if(i[0] >=0)
                    {
                        audioItem.setLanguage(selectedLanguage);
                        audioItem.setIs_transcribed("no");
                        databaseHelper.updateRecordingStatus(audioItem.getRecordingId(),"no");
                        String selectedModel = sharedPreferences2.getString(SELECTED_TRANSCRIPTION_METHOD+String.valueOf(audioItem.getRecordingId()),defaultt_method);
                        if ("Server".equalsIgnoreCase(selectedModel)) {
                            server_open_transcription_disabled(holder);
                            holder.Description_api.setText("");
                            holder.transcription_progress_api.setVisibility(View.VISIBLE);
                            Transcribe_Server_with_chunks(audioItem,holder,position);
                            // server_open_transcription_disabled(holder);

                        } else if ("Local".equalsIgnoreCase(selectedModel)) {

                            local_open_transcription_disabled(holder);
                            Transcribe_Local(audioItem,holder,position);

                            //local_open_transcription_disabled(holder);

                        }
                        else
                        {
                            holder.model_switch.setChecked(false); // Assuming btn1 is for ChatGPT
                            LocalSideSelected(holder);

                        }




                        i[0] = i[0] +1;
                    }
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




        holder.add_to_list.setOnClickListener(v -> {
            if (selectedItems.contains(audioItem.getDescription())||selectedItems.contains(audioItem.getDescription_api())) {
                selectedItems.remove(audioItem.getDescription());
                selectedItems.remove(audioItem.getDescription_api());
                updated_selcted_card_add(holder);
                saveSelectionState(audioItem.getEventId());
                notifySelectionChanged();

            }
            else {
                String transcription_model = sharedPreferences2.getString(SELECTED_TRANSCRIPTION_METHOD+String.valueOf(audioItem.getRecordingId()),defaultt_method); // Default to "Use ChatGPT"
                if ("Server".equalsIgnoreCase(transcription_model)) {
                    selectedItems.add(audioItem.getDescription_api());
                } else if ("Local".equalsIgnoreCase(transcription_model)) {
                    selectedItems.add(audioItem.getDescription());
                }
                else
                {
                    selectedItems.add(audioItem.getDescription());
                }
                // Add the item to the selected list
                updated_selcted_card_remove(holder);
                saveSelectionState(audioItem.getEventId());
                notifySelectionChanged();
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
                                selectedItems.remove(audioItem.getDescription_api());
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
        holder.ceation_d_and_t.setText(R.string.created_on);
        holder.ceation_d_and_t.setText(holder.ceation_d_and_t.getText()+" : "+audioItem.getDate());
        holder.total_time.setText(" / "+convertSecondsToTime(Integer.parseInt(AudioUtils.getAudioDuration(audioItem.getUrl()))));
//        holder.Description.setText(audioItem.getDescription());
//        holder.Description_api.setText(audioItem.getDescription_api());
//////////////////////////////////////////////////////////////////////////////
        holder.expand_button.setOnClickListener(v -> {
            Intent intent = new Intent(context, activity_text_display.class);
            intent.putExtra("text", holder.Description.getText().toString());
            intent.putExtra("Title", context.getString(R.string.transcription));
            intent.putExtra("R_id",String.valueOf( audioItem.getRecordingId()));
            intent.putExtra("E_id",String.valueOf( audioItem.getEventId()));
            intent.putExtra("T_mode",String.valueOf( sharedPreferences2.getString(SELECTED_TRANSCRIPTION_METHOD+String.valueOf(audioItem.getRecordingId()), "Local")));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        });


        ////////////api///////////////////////////////////////
        holder.expand_button_api.setOnClickListener(v -> {
            Intent intent = new Intent(context, activity_text_display.class);
            intent.putExtra("text", holder.Description_api.getText().toString());
            intent.putExtra("Title", context.getString(R.string.transcription)); // Use context to get string resource
            intent.putExtra("R_id",String.valueOf( audioItem.getRecordingId()));
            intent.putExtra("E_id",String.valueOf( audioItem.getEventId()));
            intent.putExtra("T_mode",String.valueOf( sharedPreferences2.getString(SELECTED_TRANSCRIPTION_METHOD+String.valueOf(audioItem.getRecordingId()), "Server")));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        });




        if(recordingList.get(position).isRecorded())
        {
            holder.Start_Transcription.setText(R.string.recorded);
            holder.Start_Transcription.setBackgroundColor(context.getResources().getColor(R.color.secondary));
        }
        else
        {
            holder.Start_Transcription.setText(R.string.imported);
            holder.Start_Transcription.setBackgroundColor(context.getResources().getColor(R.color.nav));
        }

    }

    //////////////////////------Server Side Transcription------//////////////////////////////////////
    private void Transcribe_Server_with_chunks(Recording audioItem, AudioViewHolder holder, int position) {
        String fileExtension = AudioUtils.getFileExtension(audioItem.getUrl()).toLowerCase();
        String[] supportedFormats = {"wav", "m4a", "mp3", "webm", "mp4", "mpga", "mpeg"};
        String changetowavformat="3gp";
        server_open_transcription_disabled(holder);
        holder.Description_api.setText("");
        holder.transcription_progress_api.setVisibility(View.VISIBLE);
        holder.transcription_status_progress_bar_api.setProgress(0,true);
        holder.progressPercentage.setText(0+" %");
        server_open_transcription_disabled(holder);

        if (Arrays.asList(supportedFormats).contains(fileExtension)) {
            if(audioItem.getIs_transcribed_api().equals("no"))
            {
                logger.addLog("Server Transcription : Sending "+audioItem.getName()+" To server for transcription");
                holder.transcription_status.setText("Sending for chunking..");
                Log.e("chunk_path","sending for chunking");
                server_open_transcription_disabled(holder);

                splitAudioInBackground(audioItem.getUrl(), 60000, new AudioChunkHelper.splitCallback() {
                    @Override
                    public void onChunksGenerated(List<String> chunkPaths) {
                        holder.transcription_status_progress_bar_api.setProgress(10,true);
                        logger.addLog("Server Transcription : Divided "+audioItem.getName()+" into "+chunkPaths.size()+" chunks");
                        holder.progressPercentage.setText(10+" %");
                        transcribe_and_chunkify_audio(chunkPaths,audioItem.getRecordingId(),uuid,holder,audioItem,position);

                        // Do something with the chunkPaths, e.g., update UI
                    }
                });


            }
            else
            {
                holder.transcription_status_progress_bar_api.setProgress(100,true);
                holder.progressPercentage.setText(100+" %");
                holder.transcription_progress_api.setVisibility(View.GONE);
                holder.Description_api.setText(audioItem.getDescription_api());
                server_open_transcription_enabled(holder);



            }
        }
        else if(fileExtension.contains(changetowavformat)) {
            if(audioItem.getIs_transcribed_api().equals("no"))
            {
                logger.addLog("Server Transcription : Sending "+audioItem.getName()+" To server for transcription");
                server_open_transcription_disabled(holder);
                holder.transcription_status.setText("Sending for chunking..");
                Log.e("chunk_path","sending for chunking");
                splitAudioInBackground(convertToWav(audioItem.getUrl(),context), 60000, new AudioChunkHelper.splitCallback() {
                    @Override
                    public void onChunksGenerated(List<String> chunkPaths) {
                        logger.addLog("Server Transcription : Divided "+audioItem.getName()+" into "+chunkPaths.size()+" chunks");
                        holder.transcription_status_progress_bar_api.setProgress(10,true);
                        holder.progressPercentage.setText(10+" %");
                        transcribe_and_chunkify_audio(chunkPaths,audioItem.getRecordingId(),uuid,holder,audioItem,position);

                    }
                });


            }
            else
            {
                holder.transcription_status_progress_bar_api.setProgress(100,true);
                holder.progressPercentage.setText(100+" %");
                holder.transcription_progress_api.setVisibility(View.GONE);
                holder.Description_api.setText(audioItem.getDescription_api());
                server_open_transcription_enabled(holder);

            }

        }
        else
        {
            holder.transcription_status_progress_bar_api.setProgress(100,true);
            holder.progressPercentage.setText(100+" %");
            databaseHelper.updaterecording_details_api(audioItem.getRecordingId(),"Unsupported format");
            audioItem.setIs_transcribed_api("yes");
            audioItem.setDescription_api("Unsupported format :"+fileExtension);
            holder.Description_api.setText("Unsupported format :"+fileExtension);
            holder.transcription_progress_api.setVisibility(View.GONE);
            server_open_transcription_disabled(holder);
        }

    }
    private void transcribe_and_chunkify_audio(List<String> chunkPaths, int recordingId, String uuid, AudioViewHolder holder, Recording audioitem,int position) {
        logger.addLog("Server Transcription : Audio file "+audioitem.getName()+" chunked successfully, sending for transcription to server");
        holder.transcription_status.setText("Audio chunked successfully, sending for transcription to server");
        boolean added_to_db = chunks_database_helper.addChunksBatch(chunkPaths, recordingId, uuid);
        Log.e("chunk_path", "trying to add to database :" + added_to_db);
        allTranscriptionProgress = true;
        int totalChunks = chunkPaths.size(); // Total number of chunks
        final int[] chunksSent = {0}; // Number of chunks sent to the server

        if (added_to_db) {
            logger.addLog("Server Transcription : All chunks data for "+audioitem.getName()+" saved successfully to local SQL server for further transcription related updation");

            holder.transcription_status_progress_bar_api.setProgress(20,true);
            holder.progressPercentage.setText(20+" %");
            List<ChunkTranscription> chunks = chunks_database_helper.getChunksByRecordingId(recordingId);
            for (ChunkTranscription chunkTranscription : chunks) {
                String status = chunkTranscription.getTranscriptionStatus();
                Log.i("chunk_path_status", "i "+chunkTranscription.getTranscriptionStatus());
                Log.i("chunk_recording_code", "i "+chunkTranscription.getChunkId());
                logger.addLog("Server Transcription : "+chunkTranscription.getChunkId()+" for "+audioitem.getName()+" have status "+chunkTranscription.getTranscriptionStatus());



                // If chunk is not transcribed yet, send it for transcription
                if (status.contains("not") || status.contains("started") || status.contains("not_started")) {
                    allTranscriptionProgress = false;
                    logger.addLog("Server Transcription : sending "+chunkTranscription.getChunkId()+" for "+audioitem.getName()+" to server for transcription ");

                    TranscriptionUtils.send_for_transcription(chunkTranscription.getChunkId(),chunkTranscription.getChunkPath(), new TranscriptionUtils.TranscriptionCallback() {
                        @Override
                        public void onSuccess(String status) {
                            Log.d("chunk_path","success "+status+" for "+chunkTranscription.getChunkPath()+"\n");
                            if(isMessageSuccessful(status))
                            {
                                logger.addLog("Server Transcription : sent "+chunkTranscription.getChunkId()+" for "+audioitem.getName()+" to server for transcription ");
                                chunksSent[0]++;
                                chunks_database_helper.updateChunkStatus(chunkTranscription.getChunkId(),chunkTranscription.getUniqueRecordingName(),"in_progress");
                                Integer total_chunks= chunks_database_helper.getTotalChunksByRecordingId(audioitem.getRecordingId());
                                Integer sent_chunks=chunks_database_helper.getChunksCountByRecordingIdAndStatus(audioitem.getRecordingId(),"in_progress");
                                holder.transcription_status.setText("chunks sent : "+sent_chunks+"/"+total_chunks);
                                float progress = 20 + (float) sent_chunks / total_chunks * 50;
                                holder.transcription_status_progress_bar_api.setProgress((int) progress, true);
                                holder.progressPercentage.setText((int)progress+" %");
                                if (total_chunks == sent_chunks) {
                                    holder.transcription_status_progress_bar_api.setProgress(70,true);
                                    holder.progressPercentage.setText(70+" %");
                                    holder.transcription_status.setText("Audio sent for transcription, waiting for responses from API");
                                    get_transcription_single(chunkTranscription.getUniqueRecordingName(), holder,audioitem,position); // Trigger the final transcription step
                                }
                            }
                            else
                            {
                               // chunksSent[0]++;
                                logger.addLog("Server Transcription : send "+chunkTranscription.getChunkId()+" for "+audioitem.getName()+" to server for transcription but got unsuccessful response "+",got response "+status);

                                holder.transcription_status.setText("Chunk may not be uploaded : "+chunkTranscription.getChunkId());

                            }



                        }

                        @Override
                        public void onError(String errorMessage) {
                            logger.addLog("Server Transcription : Unable to send "+chunkTranscription.getChunkId()+" for "+audioitem.getName()+" to server for transcription   "+"got response "+status);

                            Log.d("chunk_path","error "+errorMessage+" for "+chunkTranscription.getChunkPath()+"\n");
                            holder.transcription_status.setText("Some error in sending chunk "+chunkTranscription.getChunkId());
                            allTranscriptionProgress=false;

                        }
                    }, chunkTranscription.getUniqueRecordingName(),audioitem.getLanguage(),uuid,context);



                    if (chunksSent[0] == totalChunks) {
                        // All chunks sent, now wait for responses
                        logger.addLog("Server Transcription : send all "+totalChunks+" for "+audioitem.getName()+" to server for transcription ");

                        Log.i("c_t",chunkTranscription.getChunkId());
                        holder.transcription_status.setText("Audio sent for transcription, waiting for responses from API");
                        get_transcription_single(chunkTranscription.getUniqueRecordingName(), holder,audioitem,position); // Trigger the final transcription step
                    }
                }
                else {
                    logger.addLog("Server Transcription : chunk no "+chunkTranscription.getChunkId()+" for "+audioitem.getName()+" already send to server ");

                    Integer total_chunks= chunks_database_helper.getTotalChunksByRecordingId(audioitem.getRecordingId());
                    Integer sent_chunks=chunks_database_helper.getChunksCountByRecordingIdAndStatus(audioitem.getRecordingId(),"in_progress");
                    float progress = 20 + (float) sent_chunks / total_chunks * 50;
                    holder.transcription_status_progress_bar_api.setProgress((int) progress, true);
                    holder.progressPercentage.setText((int) progress+" %");

                    chunksSent[0]++;
                }
            }
        } else {
            logger.addLog("Server Transcription : Filed to send  for "+audioitem.getName()+" to server for transcription ");

            allTranscriptionProgress = false;
            transcribe_and_chunkify_audio(chunkPaths, recordingId, uuid, holder, audioitem,position); // Retry if failed
        }

        // If all chunks are processed and no further chunks need to be sent, then trigger transcription response fetch
        if (allTranscriptionProgress) {
            holder.transcription_status_progress_bar_api.setProgress(Integer.valueOf(70),true);
            holder.progressPercentage.setText(70+" %");
            holder.transcription_status.setText("Audio sent for transcription, waiting for responses from API");
            logger.addLog("Server Transcription : send all chunks for "+audioitem.getName()+" to server for transcription ");

            get_transcription_single(chunks_database_helper.getUniqueRecordingNameByRecordingId(audioitem.getRecordingId()), holder,audioitem,position); // Trigger the final transcription step
        }
    }
    private void get_transcription_single(String unique_recordingId, AudioViewHolder holder,Recording audioitem,int position) {
        Handler mainHandler = new Handler(Looper.getMainLooper());
        logger.addLog("Server Transcription : Trying to get Transcription for "+audioitem.getName()+" to server for transcription ");

        holder.transcription_status.setText("Sent all chunks to API, waiting for transcription...");
        TranscriptionUtils.getTranscriptionStatus_All_At_once(unique_recordingId, new TranscriptionUtils.TranscriptionStatusCallback() {
            @Override
            public void onTranscriptionStatusSuccess(String response, String statuss, int queuePosition) throws JSONException {
                Log.i("got_chunks",response);
                logger.addLog("Server Transcription : Got Response from server for "+audioitem.getName()+" of transcriptions ");

                Map<String, String> chunkTranscriptionMap = new TreeMap<>(); // To maintain order by chunk ID

                if(Chunks_Json_helper.isValidFormat(response))
                {
                    logger.addLog("Server Transcription :  Response from server for "+audioitem.getName()+" is valid ");

                    List<Chunk_Response> all_chunks_status= Chunks_Json_helper.getChunkList(response);
                    for (Chunk_Response chunk : all_chunks_status) {
                        String chunkId = chunk.getChunkId();
                        String status = chunk.getStatus();
                        String transcription = chunk.getTranscription();

                        if (status.contains("completed")) {
                            Log.i("got_chunks", "completed for " + chunkId);
                            chunks_database_helper.updateChunkTranscription(chunkId, unique_recordingId, transcription);
                            chunks_database_helper.updateChunkStatus(chunkId, unique_recordingId, "completed");
                            Log.i("got_chunks_2",chunkId+transcription);

                            chunkTranscriptionMap.put(chunkId, transcription); // Add transcription to the map
                        } else {
                            chunkTranscriptionMap.put(chunkId, " ... ");
                            Log.i("got_chunks_2",chunkId+" >>>");
                        }

                        Log.i("got_chunks",chunkId.toString());
                    }
                    Integer total_chunks= Chunks_Json_helper.getTotalChunkCount(response);
                    Integer transcribed_chunks=Chunks_Json_helper.getCompletedChunkCount(response);
                    Log.i("got_chunks",transcribed_chunks+"/"+total_chunks);
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            holder.transcription_status.setText("Chunks Transcribed : "+transcribed_chunks+" / "+total_chunks);
                            logger.addLog("Server Transcription : Chunks Transcribed : "+transcribed_chunks+" / "+total_chunks+" for "+audioitem.getName());

                        }
                    });
                    if(transcribed_chunks>=1)
                    {
                        String combined=combineChunkTranscriptions(chunkTranscriptionMap);
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                if(transcribed_chunks!=total_chunks)
                                {
                                    holder.Description_api.setVisibility(View.INVISIBLE);
                                }
                                holder.Description_api.setText(combined);
                                server_open_transcription_enabled(holder);
                            }
                        });


                        Log.e("got_chunks",combined);
                    }
                    if(transcribed_chunks==total_chunks)
                    {
                        String combined=combineChunkTranscriptions(chunkTranscriptionMap);
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                logger.addLog("Server Transcription :  All chunks transcribed for "+audioitem.getName());

                                server_open_transcription_enabled(holder);
                                databaseHelper.updaterecording_details_api(audioitem.getRecordingId(),combined);
                                audioitem.setIs_transcribed_api("yes");
                                audioitem.setDescription_api(combined);
                                AudioRecyclerAdapter.selectedItems.add(combined);
                                AudioRecyclerAdapter.selectedItems.remove(audioitem.getDescription());
                                saveSelectionStatet(audioitem.getEventId(),position);
                                notifySelectionChanged();
                                Transcribe_Server_with_chunks(audioitem,holder,position);

                                new Handler(Looper.getMainLooper()).post(new Runnable() {
                                    @Override
                                    public void run() {
                                        holder.Description_api.setVisibility(View.VISIBLE);
                                        holder.Description_api.setText(combined);
                                        notifyDataSetChanged();
                                        notifyItemChanged(position);

                                    }
                                });
                            }
                        });


                    }

                    float progress = 70 + (float) transcribed_chunks/total_chunks * 30;

                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            holder.transcription_status_progress_bar_api.setProgress((int) progress, true);
                            holder.progressPercentage.setText((int) progress + " %");
                        }
                    });
                    if (total_chunks > transcribed_chunks) {
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                logger.addLog("Server Transcription :  All chunks are not transcribed for "+audioitem.getName()+" retrying to check if all are transcribed");

                                Log.i("got_chunks","rechecking....");
                                holder.transcription_status.setText("Refreshing Status");

                                get_transcription_single(unique_recordingId, holder,audioitem,position);
                            }
                        }, 30000); // 60000 milliseconds = 1 minute
                    }


                }
                else {
                    logger.addLog("Server Transcription : Response from server for "+audioitem.getName()+" is invalid ");
                    Log.i("got_chunks","invalid json format");
                }

            }

            @Override
            public void onTranscriptionStatusError(String errorMessage) {
                //callsDone[0]++;
                Log.e("transcription_error", "Error in chunk transcription: " + errorMessage);
                logger.addLog("Server Transcription :  Error in fetching transcription for "+audioitem.getName()+"  "+errorMessage);


                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        // Call the method again after 1 minute
                        logger.addLog("Server Transcription :  Error in fetching transcription Retrying for "+audioitem.getName()+"  "+errorMessage);

                        Log.i("got_chunks","retriggering due to error");

                        get_transcription_single(unique_recordingId, holder,audioitem,position); // Trigger the final transcription step

                    }
                }, 30000);

            }
        }, uuid,context);



    }

    private String combineChunkTranscriptions(Map<String, String> chunkTranscriptionMap) {
        StringBuilder combinedTranscription = new StringBuilder();

        for (Map.Entry<String, String> entry : chunkTranscriptionMap.entrySet()) {
            combinedTranscription.append(entry.getValue()).append(" ");
        }

        return combinedTranscription.toString().trim(); // Remove trailing space
    }

    ///////////////////////////////////-----Transcribe Local-----------//////////////////////////////////////


    private void Transcribe_Local(Recording audioItem,AudioViewHolder holder,int position) {
        logger.addLog("Local Transcription :  Starting transcription for "+audioItem.getName());

        local_open_transcription_disabled(holder);
        if(audioItem.getIs_transcribed().equals("no"))
        {
            Model_Database_Helper modelDatabaseHelper=new Model_Database_Helper(context);
            if(modelDatabaseHelper.checkModelDownloadedByLanguage(audioItem.getLanguage()))
            {
                logger.addLog("Local Transcription :  Starting transcription for "+audioItem.getName());
                //Vosk_Model.initializeVoskModel(context,modelDatabaseHelper.getModelNameByLanguage(selectedLanguage));
                holder.Description.setText("");
                holder.transcription_progress.setVisibility(View.VISIBLE);
                try {
                    handleTranscription(audioItem,position,holder,audioItem.getLanguage());

                }catch (Exception e)
                {
                    Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
                    holder.Description.setText("Met with some error try after sometime");
                    holder.transcription_progress.setVisibility(View.VISIBLE);
                }

            }
            else
            {
                logger.addLog("Local Transcription :  for "+audioItem.getName()+"Local Model for "+audioItem.getLanguage()+" is not available");
                local_open_transcription_disabled(holder);
                holder.Description.setText("Local Model for "+audioItem.getLanguage()+" is not available");
                if(!audioItem.getDescription().isEmpty())
                {
                    AudioRecyclerAdapter.selectedItems.remove(audioItem.getDescription());
                    saveSelectionStatet(audioItem.getEventId(),position);
                    notifySelectionChanged();
                }

                holder.transcription_progress.setVisibility(View.GONE);

                //boolean updateSuccess = databaseHelper.updateRecordingDetails(audioItem.getRecordingId(), "Local Model for "+selectedLanguage+" is not available");


                // Toast.makeText(context, "Local Model for "+selectedLanguage+" is not available", Toast.LENGTH_SHORT).show();
            }



        }
        else
        {
            logger.addLog("Local Transcription :  Already Transcribed "+audioItem.getName());
            local_open_transcription_enabled(holder);
            holder.Description.setText(audioItem.getDescription());
            holder.transcription_progress.setVisibility(View.GONE);

        }
    }
    private void handleTranscription(Recording audioItem, int position, AudioViewHolder holder,String language) {
        String fileExtension = AudioUtils.getFileExtension(audioItem.getUrl()).toLowerCase();
        holder.Description.setText("");
        String[] supportedFormats = {"wav", "3gp"};
        if (!Arrays.asList(supportedFormats).contains(fileExtension)) {
            logger.addLog("Local Transcription : Unsupported format for "+audioItem.getName()+" supported format are wav and 3gp");
            boolean updateSuccess = databaseHelper.updateRecordingDetails(audioItem.getRecordingId(), "Unsupported Format");
            holder.Description.setText("");
            // Switch back to main thread to update UI or notify adapter
            new Handler(Looper.getMainLooper()).post(() -> {
                if (updateSuccess) {
                    Log.d("hellorecorder", "Recording updated successfully.");
                    audioItem.setDescription("Unsupported Format"); // Assuming the setDescription method updates COL_DES
                    audioItem.setIs_transcribed("yes"); // Assuming this is the flag for COL_IS_TRANSCRIBED
                    holder.Description.setText("Unsupported Format");
                    local_open_transcription_enabled(holder);
                    holder.transcription_progress.setVisibility(View.GONE);
                } else {
                    Log.d("hellorecorder", "Failed to update the recording.+Unsupported Format");
                }
            });
            return ;
        }

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> {
            try {
                String transcription = Vosk_Model.recognizeSpeech(context,convertToWav(audioItem.getUrl(), context),language);

                if (transcription != null && !transcription.isEmpty()) {
                    logger.addLog("Local Transcription :  Successfully transcribed for "+audioItem.getName()+"and also found some transcription");
                    // Update the database
                    boolean updateSuccess = databaseHelper.updateRecordingDetails(audioItem.getRecordingId(), transcription);

                    // Switch back to main thread to update UI or notify adapter
                    new Handler(Looper.getMainLooper()).post(() -> {
                        if (updateSuccess) {
                            AudioRecyclerAdapter.selectedItems.remove(audioItem.getDescription());
                            Log.d("hellorecorder", "Recording updated successfully.");
                            audioItem.setDescription(transcription); // Assuming the setDescription method updates COL_DES
                            audioItem.setIs_transcribed("yes"); // Assuming this is the flag for COL_IS_TRANSCRIBED
                            local_open_transcription_enabled(holder);
                            holder.Description.setText(transcription);
                            holder.transcription_progress.setVisibility(View.GONE);
                            AudioRecyclerAdapter.selectedItems.add(transcription);
                            AudioRecyclerAdapter.selectedItems.remove(audioItem.getDescription_api());
                            saveSelectionStatet(audioItem.getEventId(),position);
                            notifySelectionChanged();
                            //Transcribe_Local(audioItem,holder,position);
                        } else {
                            Log.d("hellorecorder", "Failed to update the recording.");
                        }
                    });
                }
                else {
                    boolean updateSuccess = databaseHelper.updateRecordingDetails(audioItem.getRecordingId(), "Recording Transcribed successfully No text found");
                    logger.addLog("Local Transcription :  Successfully transcribed for "+audioItem.getName()+" but not found any transcription");

                    // Switch back to main thread to update UI or notify adapter
                    new Handler(Looper.getMainLooper()).post(() -> {
                        if (updateSuccess) {
                            Log.d("hellorecorder", "Recording updated successfully.");
                            holder.Description.setText("Recording Transcribed successfully No text found");
                            holder.transcription_progress.setVisibility(View.GONE);
                            local_open_transcription_enabled(holder);
                            audioItem.setDescription("Recording Transcribed successfully No text found"); // Assuming the setDescription method updates COL_DES
                            audioItem.setIs_transcribed("yes"); // Assuming this is the flag for COL_IS_TRANSCRIBED
                        } else {
                            Log.d("hellorecorder", "Failed to update the recording.");
                        }
                    });
                }
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() ->

                        Log.d("hellorecorder", "Error during speech recognition: " + e.getMessage())
                );
                local_open_transcription_enabled(holder);
                logger.addLog("Local Transcription :  Error during Transcription for "+audioItem.getName()+" possible reason "+e.getMessage());


                new Handler(Looper.getMainLooper()).post(() ->
                        holder.transcription_progress.setVisibility(View.GONE)
                );
                audioItem.setDescription("Error during speech recognition: " + e.getMessage());
                audioItem.setIs_transcribed("yes"); // Assuming this is the flag for COL_IS_TRANSCRIBED
                Transcribe_Local(audioItem,holder,position);
               // recordingList.set(position, audioItem);
//                notifyItemChanged(position);
            }
        });

        executorService.shutdown();
    }




    // Helper Function: Check if the status is successful
    private static boolean isStatusSuccessful(String status) {
        return "completed".equalsIgnoreCase(status);
    }

    public boolean isMessageSuccessful(String jsonResponse) {//Encrypted file uploaded and queued successfully
        try {

            if (jsonResponse.contains("message") && (jsonResponse.contains("successfully")||jsonResponse.contains("already exists")||jsonResponse.contains("successful"))) {
                return true;
            }
        } catch (Exception e) {
            // Handle any parsing exceptions
            e.printStackTrace();
        }
        return false;
    }
    public void server_open_transcription_disabled(AudioViewHolder holder) {
        new Handler(Looper.getMainLooper()).post(() ->
                holder.expand_button_api.setVisibility(View.GONE)
        );
    }

    public void server_open_transcription_enabled(AudioViewHolder holder) {
        new Handler(Looper.getMainLooper()).post(() ->
                holder.expand_button_api.setVisibility(View.VISIBLE)
        );
    }

    public void local_open_transcription_disabled(AudioViewHolder holder) {
        new Handler(Looper.getMainLooper()).post(() ->
                holder.expand_button.setVisibility(View.GONE)
        );
    }

    public void local_open_transcription_enabled(AudioViewHolder holder) {
        new Handler(Looper.getMainLooper()).post(() ->
                holder.expand_button.setVisibility(View.VISIBLE)
        );
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
            holder.playPauseButton.setImageResource(R.mipmap.pause);
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
                        holder.playPauseButton.setImageResource(R.mipmap.play);

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
    public  void saveSelectionStatet(int id,int position) {
        try
        {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putStringSet("selected_items_"+id, new HashSet<>(selectedItems));
            editor.apply();
            loadSelectionState(id);
            notifyItemChanged(position);
        }
        catch (Exception e)
        {

        }


    }
    public static void updateSelectionState(int id, String stringToRemove, String stringToAdd,Recording audioitem,Context context,AudioViewHolder holder) {

        selectedItems.remove(stringToRemove);
        selectedItems.add(stringToAdd);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putStringSet("selected_items_" + id,new HashSet<>(selectedItems));
        editor.apply();
        if (selectedItems.contains(stringToAdd)) {
            holder.recordingCard.setCardBackgroundColor(context.getResources().getColor(R.color.third));
            holder.add_to_list.setImageResource(R.drawable.baseline_cancel_24); // Replace with your "remove" icon resource
        } else {
            holder.recordingCard.setCardBackgroundColor(context.getResources().getColor(R.color.fourth));
            holder.add_to_list.setImageResource(R.drawable.baseline_add_24); // Replace with your "add" icon resource
        }

    }

    private  void loadSelectionState(int id) {
        Set<String> savedItems = sharedPreferences.getStringSet("selected_items_"+id, new HashSet<>());
        selectedItems.clear();
        if (savedItems != null) {
            selectedItems.addAll(savedItems);
        }
        notifySelectionChanged();
    }
    private static void loadSelectionState_fort(int id) {
        Set<String> savedItems = sharedPreferences.getStringSet("selected_items_"+id, new HashSet<>());
        selectedItems.clear();
        if (savedItems != null) {
            selectedItems.addAll(savedItems);
        }
    }


    private void stopSeekBarUpdates() {
        if (updateSeekBarRunnable != null) {
            handler.removeCallbacks(updateSeekBarRunnable);
        }
    }

    private void resetPlayerUI(AudioViewHolder holder,int currentPlayingPosition) {
        holder.playbackProgressBar.setProgress(0);
        holder.playbackTimer.setText("00:00");
        holder.playPauseButton.setImageResource(R.mipmap.play);


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
    public static String getStatus(String jsonString) {
        try {
            JSONObject jsonObject = new JSONObject(jsonString);
            return jsonObject.optString("status", "unknown");
        } catch (Exception e) {
            e.printStackTrace();
            return "error";
        }
    }

    // Function to check if the status is "completed" (successful)


    // Function to get the transcription if the status is "completed"
    public static String getTranscriptionIfSuccessful(String jsonString) {
        try {
            if (isStatusSuccessful(jsonString)) {
                JSONObject jsonObject = new JSONObject(jsonString);
                return jsonObject.optString("transcription", "No transcription available.");
            } else {
                return "Status is not successful.";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "Error processing the JSON.";
        }
    }
    public void updated_selcted_card_add(AudioViewHolder holder)
    {

            holder.recordingCard.setCardBackgroundColor(context.getResources().getColor(R.color.fourth));
            holder.add_to_list.setImageResource(R.drawable.baseline_add_24); // Replace with your "add" icon resource


    }
    public void updated_selcted_card_remove(AudioViewHolder holder)
    {

            holder.recordingCard.setCardBackgroundColor(context.getResources().getColor(R.color.third));
            holder.add_to_list.setImageResource(R.drawable.baseline_cancel_24); // Replace with your "remove" icon resource


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

        TextView recordingLabel,ceation_d_and_t,total_time,Description,Description_api,transcription_status;
        ImageView expand_button,add_to_list,delete,expand_button_api;

        CardView recordingCard;
        LinearLayout transcription_progress,transcription_progress_api;
        Spinner languageSpinner;

        Switch model_switch;
        TextView Local_t,Server_t,progressPercentage;
        ConstraintLayout local_c,server_C;

        ProgressBar transcription_status_progress_bar_api;
        public AudioViewHolder(@NonNull View itemView) {
            super(itemView);
            progressPercentage=itemView.findViewById(R.id.progressPercentage);
            transcription_status_progress_bar_api=itemView.findViewById(R.id.horizontalProgressBar);
            transcription_progress=itemView.findViewById(R.id.transcription_progress);
            total_time=itemView.findViewById(R.id.total_time);
            Description=itemView.findViewById(R.id.Description);
            ceation_d_and_t=itemView.findViewById(R.id.ceation_d_and_t);
            recordingLabel=itemView.findViewById(R.id.recordingLabel);
            playPauseButton = itemView.findViewById(R.id.playPauseButton);
            playbackProgressBar = itemView.findViewById(R.id.playbackProgressBar);
            playbackTimer = itemView.findViewById(R.id.playbackTimer);

            Start_Transcription=itemView.findViewById(R.id.Start_Transcription);
            recordingCard=itemView.findViewById(R.id.recordingCard);
            expand_button=itemView.findViewById(R.id.expand_btn);
            delete=itemView.findViewById(R.id.deleteButton);
            add_to_list=itemView.findViewById(R.id.add_to_list);
            expand_button_api=itemView.findViewById(R.id.expand_btn_api);
            transcription_progress_api=itemView.findViewById(R.id.transcription_progress_api);
            Description_api=itemView.findViewById(R.id.Description_api);
            languageSpinner=itemView.findViewById(R.id.language_spinner);
            Local_t=itemView.findViewById(R.id.Loc);
            Server_t=itemView.findViewById(R.id.Ser);
            model_switch=itemView.findViewById(R.id.transcription_switch);
            local_c=itemView.findViewById(R.id.local_c);
            server_C=itemView.findViewById(R.id.Server_c);
            transcription_status=itemView.findViewById(R.id.transcription_status);

        }
        public void bindAudioData(Recording audioItem, int position) {
            playbackTimer.setText("00:00");
            playbackProgressBar.setProgress(0);
            playbackProgressBar.setMax(0); // Reset seek bar until new audio is loaded
            playPauseButton.setImageResource(ir.one_developer.file_picker.R.drawable.ic_play);
        }

    }
    private void Serversideselected(AudioViewHolder holder)
    {

        holder.Local_t.setTypeface(null, Typeface.NORMAL);
        holder.Server_t.setTypeface(null, Typeface.BOLD);
        holder.server_C.setVisibility(View.VISIBLE);
        holder.local_c.setVisibility(View.GONE);
    }
    private  void LocalSideSelected(AudioViewHolder holder)
    {
        holder.Server_t.setTypeface(null, Typeface.NORMAL);
        holder.Local_t.setTypeface(null, Typeface.BOLD);
        holder.local_c.setVisibility(View.VISIBLE);
        holder.server_C.setVisibility(View.GONE);
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
