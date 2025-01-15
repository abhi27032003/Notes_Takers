package com.example.recorderchunks.Adapter;

import static android.provider.Settings.System.getString;
import static androidx.core.content.ContextCompat.startActivity;
import static com.example.recorderchunks.AI_Transcription.AudioChunkHelper.splitAudioInBackground;
import static com.example.recorderchunks.Activity.API_Updation.SELECTED_LANGUAGE;
import static com.example.recorderchunks.Activity.API_Updation.getLanguagesFromMetadata;
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
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.example.jean.jcplayer.view.JcPlayerView;

import com.example.recorderchunks.AI_Transcription.AudioChunkHelper;
import com.example.recorderchunks.AI_Transcription.TranscriptionUtils;
import com.example.recorderchunks.Activity.activity_text_display;
import com.example.recorderchunks.Audio_Models.Vosk_Model;
import com.example.recorderchunks.Helpeerclasses.Chunks_Database_Helper;
import com.example.recorderchunks.Helpeerclasses.DatabaseHelper;
import com.example.recorderchunks.Helpeerclasses.Model_Database_Helper;
import com.example.recorderchunks.Model_Class.ChunkTranscription;
import com.example.recorderchunks.Model_Class.Recording;
import com.example.recorderchunks.R;
import com.example.recorderchunks.utils.AudioUtils;

import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AudioRecyclerAdapter extends RecyclerView.Adapter<AudioRecyclerAdapter.AudioViewHolder> {
    boolean allTranscriptionProgress = true;

    public static ArrayList<Recording> recordingList;
    private final Context context;
    private static MediaPlayer mediaPlayer;
    private static int currentPlayingPosition = -1;
    private static final Handler handler = new Handler();
    private static Runnable updateSeekBarRunnable;
    private static boolean isExpanded = false;
    private static boolean isExpanded2 = false;
    public static ArrayList<String> selectedItems;
    private static SharedPreferences sharedPreferences,sharedPreferences2;
    private static final String PREFS_NAME = "PromptSelectionPrefs";
    private OnSelectionChangedListener selectionChangedListener;

    private DatabaseHelper databaseHelper;
    private Chunks_Database_Helper chunks_database_helper;

    Vosk_Model vm;
    public static final String SELECTED_TRANSCRIPTION_METHOD = "SelectedTranscriptionMethod";







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
        holder.playbackProgressBar.setFocusable(false);

        Recording audioItem = recordingList.get(position);
        loadSelectionState(audioItem.getEventId());
        /////////////////////////////////////////////////chunking and api calling logic


        //////////////////////////////////// transcrtoption method work
        String defaultt_method= sharedPreferences2.getString(SELECTED_TRANSCRIPTION_METHOD, "Local");
        String selectedModel = sharedPreferences2.getString(SELECTED_TRANSCRIPTION_METHOD+String.valueOf(audioItem.getRecordingId()),defaultt_method); // Default to "Use ChatGPT"
        if ("Server".equalsIgnoreCase(selectedModel)) {
            holder.model_switch.setChecked(true); // Assuming btn1 is for ChatGPT
            Serversideselected(holder);
            SharedPreferences.Editor editor = sharedPreferences2.edit();
            editor.putString(SELECTED_TRANSCRIPTION_METHOD+String.valueOf(audioItem.getRecordingId()), "Server");
            editor.apply();
            Transcribe_Server_with_chunks(audioItem,holder,position);

        } else if ("Local".equalsIgnoreCase(selectedModel)) {

            holder.model_switch.setChecked(false); // Assuming btn1 is for ChatGPT
            LocalSideSelected(holder);
            SharedPreferences.Editor editor = sharedPreferences2.edit();
            editor.putString(SELECTED_TRANSCRIPTION_METHOD+String.valueOf(audioItem.getRecordingId()), "Local");
            editor.apply();
            Transcribe_Local(audioItem,holder,position);

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

            } else {
                selectedmodel = "Local"; // API when the switch is OFF
                LocalSideSelected(holder);
                Transcribe_Local(audioItem,holder,position);

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
                        Model_Database_Helper modelDatabaseHelper=new Model_Database_Helper(context);
                        if(modelDatabaseHelper.checkModelDownloadedByLanguage(selectedLanguage))
                        {
                            //Vosk_Model.initializeVoskModel(context,modelDatabaseHelper.getModelNameByLanguage(selectedLanguage));
                            holder.Description.setText("");
                            holder.transcription_progress.setVisibility(View.VISIBLE);
                            handleTranscription(audioItem,position,holder,selectedLanguage);
                        }
                        else
                        {
                            holder.Description.setText("Local Model for "+selectedLanguage+" is not available");
                            //boolean updateSuccess = databaseHelper.updateRecordingDetails(audioItem.getRecordingId(), "Local Model for "+selectedLanguage+" is not available");


                           // Toast.makeText(context, "Local Model for "+selectedLanguage+" is not available", Toast.LENGTH_SHORT).show();
                        }

                        holder.Description_api.setText("");
                        holder.transcription_progress_api.setVisibility(View.VISIBLE);



                        TranscriptionUtils.send_for_transcription_no_uuid(context, audioItem.getUrl(), new TranscriptionUtils.Transcription_no_code_Callback() {
                            @Override
                            public void onSuccess(String message) {
                                // Handle success response
                                holder.Description_api.setText(message);
                                holder.transcription_progress_api.setVisibility(View.GONE);
                                databaseHelper.updaterecording_details_api(audioItem.getRecordingId(),message);


                            }

                            @Override
                            public void onError(String errorMessage) {
                                // Handle error response
                                holder.Description_api.setText(errorMessage);
                                holder.transcription_progress_api.setVisibility(View.GONE);

                            }
                        }, audioItem.getLanguage(),  selectedLanguage);
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
        holder.ceation_d_and_t.setText(R.string.created_on);
        holder.ceation_d_and_t.setText(holder.ceation_d_and_t.getText()+" : "+audioItem.getDate());
        holder.total_time.setText(" / "+convertSecondsToTime(Integer.parseInt(AudioUtils.getAudioDuration(audioItem.getUrl()))));
        holder.Description.setText(audioItem.getDescription());
        holder.Description_api.setText(audioItem.getDescription_api());
//////////////////////////////////////////////////////////////////////////////
        holder.expand_button.setOnClickListener(v -> {
            if (isExpanded) {
                // Collapse the TextView
                holder.Description.setMaxLines(1);
                holder.Description.setEllipsize(android.text.TextUtils.TruncateAt.END);
                holder.expand_button.setImageResource(R.mipmap.expand); // Change to expand icon
                isExpanded = false;
            } else {
                // Expand: Show all lines or trigger activity if text exceeds threshold
                if (holder.Description.getText().toString().length() > 60) { // Define your threshold
                    Intent intent = new Intent(context, activity_text_display.class);
                    intent.putExtra("text", holder.Description.getText().toString());
                    intent.putExtra("Title", context.getString(R.string.transcription));
                    intent.putExtra("R_id",String.valueOf( audioItem.getRecordingId()));
                    intent.putExtra("E_id",String.valueOf( audioItem.getEventId()));
                    intent.putExtra("T_mode",String.valueOf( sharedPreferences2.getString(SELECTED_TRANSCRIPTION_METHOD+String.valueOf(audioItem.getRecordingId()), "Local")));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                } else {
                    holder.Description.setMaxLines(Integer.MAX_VALUE);
                    holder.Description.setEllipsize(null); // Remove ellipsis
                    holder.expand_button.setImageResource(R.mipmap.collapse); // Change to collapse icon
                    isExpanded = true;
                }
            }
        });


        ////////////api///////////////////////////////////////
        holder.expand_button_api.setOnClickListener(v -> {
            if (isExpanded2) {
                // Collapse the TextView
                holder.Description_api.setMaxLines(1);
                holder.Description_api.setEllipsize(android.text.TextUtils.TruncateAt.END);
                holder.expand_button_api.setImageResource(R.mipmap.expand); // Change to expand icon
                isExpanded2 = false;
            } else {
                // Expand: Show all lines or trigger activity if text exceeds threshold
                if (holder.Description_api.getText().toString().length() > 60) { // Define your threshold
                    Intent intent = new Intent(context, activity_text_display.class);
                    intent.putExtra("text", holder.Description_api.getText().toString());
                    intent.putExtra("Title", context.getString(R.string.transcription)); // Use context to get string resource
                    intent.putExtra("R_id",String.valueOf( audioItem.getRecordingId()));
                    intent.putExtra("E_id",String.valueOf( audioItem.getEventId()));
                    intent.putExtra("T_mode",String.valueOf( sharedPreferences2.getString(SELECTED_TRANSCRIPTION_METHOD+String.valueOf(audioItem.getRecordingId()), "Local")));

                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                } else {
                    holder.Description_api.setMaxLines(Integer.MAX_VALUE);
                    holder.Description_api.setEllipsize(null); // Remove ellipsis
                    holder.expand_button_api.setImageResource(R.mipmap.collapse); // Change to collapse icon
                    isExpanded2 = true;
                }
            }
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

    private void Transcribe_Server(Recording audioItem, AudioViewHolder holder, int position) {

        if(audioItem.getIs_transcribed_api().equals("no"))
        {
            holder.Description_api.setText("");

            holder.transcription_progress_api.setVisibility(View.VISIBLE);
            TranscriptionUtils.send_for_transcription_no_uuid(context, audioItem.getUrl(), new TranscriptionUtils.Transcription_no_code_Callback() {
                @Override
                public void onSuccess(String message) {
                    // Handle success response
                    holder.Description_api.setText(message);
                    holder.transcription_progress_api.setVisibility(View.GONE);
                    databaseHelper.updaterecording_details_api(audioItem.getRecordingId(),message);


                }

                @Override
                public void onError(String errorMessage) {
                    // Handle error response
                    holder.Description_api.setText(errorMessage);
                    holder.transcription_progress_api.setVisibility(View.GONE);

                }
            }, audioItem.getLanguage(),  audioItem.getLanguage());
//            chunks_database_helper.logAllChunks();
//            Log.e("chunk_path","sending for chunking");
//            List<String> chunkPaths = AudioChunkHelper.splitAudioIntoChunks(audioItem.getUrl(), 2000);
//            transcribe_and_chunkify_audio(chunkPaths,audioItem.getRecordingId(),"sdfgrtfhi7tyhb671987fgytdtf",holder,audioItem);


        }
        else
        {
            holder.transcription_progress_api.setVisibility(View.GONE);

        }
    }
    private void Transcribe_Server_with_chunks(Recording audioItem, AudioViewHolder holder, int position) {

        if(audioItem.getIs_transcribed_api().equals("no"))
        {
            holder.Description_api.setText("");

            holder.transcription_progress_api.setVisibility(View.VISIBLE);
            holder.transcription_status.setText("Sending for chunking..");
            Log.e("chunk_path","sending for chunking");
            splitAudioInBackground(audioItem.getUrl(), 20000, new AudioChunkHelper.splitCallback() {
                @Override
                public void onChunksGenerated(List<String> chunkPaths) {
                    transcribe_and_chunkify_audio(chunkPaths,audioItem.getRecordingId(),"sdfgrtfhi7tyhb671987fgytdtf",holder,audioItem);

                    // Do something with the chunkPaths, e.g., update UI
                }
            });


        }
        else
        {
            holder.transcription_progress_api.setVisibility(View.GONE);

        }
    }

    private void Transcribe_Local(Recording audioItem,AudioViewHolder holder,int position) {
        //holder.Description.setText("");
        if(audioItem.getIs_transcribed().equals("no"))
        {
            holder.Description_api.setText("");
            holder.transcription_progress.setVisibility(View.VISIBLE);

            try {
                handleTranscription(audioItem,position,holder,audioItem.getLanguage());

            }catch (Exception e)
            {
                Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
            }

        }
        else
        {
            holder.Description_api.setText("");
            holder.transcription_progress.setVisibility(View.GONE);

        }
    }

    private void transcribe_and_chunkify_audio(List<String> chunkPaths, int recordingId, String uuid,AudioViewHolder holder,Recording audioitem) {
        Log.e("chunk_path",chunkPaths.toString());
        holder.transcription_status.setText("Audio chunked..");
        boolean added_to_db =chunks_database_helper.addChunksBatch(chunkPaths,recordingId,uuid);
        Log.e("chunk_path","trying to add to database :"+added_to_db);

        allTranscriptionProgress = true;
        if(added_to_db)
        {
            List<ChunkTranscription> chunks = chunks_database_helper.getChunksByRecordingId(recordingId);
            //get total chunks and get transcribed chunks no

            Log.e("chunk_path",chunks.toString());
            int i=0;
            for (ChunkTranscription chunkTranscription:chunks) {
                String status=chunkTranscription.getTranscriptionStatus();
                Log.i("chunk_path_status",chunkTranscription.getTranscriptionStatus());
                Log.i("chunk_path_status", String.valueOf(i++));
                if(status.contains("not")||status.contains("started")||status.contains("not_started"))
                {
                    allTranscriptionProgress=false;
                    send_chunk_for_transcription(chunkTranscription.getChunkPath(),chunkTranscription.getChunkId(),"",holder,audioitem);

                }

            }


        }
        else {
            allTranscriptionProgress=false;

            transcribe_and_chunkify_audio(chunkPaths,recordingId,uuid,holder,audioitem);

        }
        if(allTranscriptionProgress)
        {
            holder.transcription_progress_api.setVisibility(View.GONE);
            holder.transcription_status.setText("Audio sent for transcription waiting for response from API");
            get_transcription(recordingId,holder);
        }
    }

    private void get_transcription(int recordingId,AudioViewHolder holder) {
        //holder.transcription_progress_api.setVisibility(View.GONE);
        holder.transcription_status.setText("Sent all chunk to api waiting for transscription..");

        List<ChunkTranscription> chunks = chunks_database_helper.getChunksByRecordingId(recordingId);
        Log.e("chunk_path",chunks.toString());
        int i=0;
        final String[] s = {""};
        for (ChunkTranscription chunkTranscription:chunks) {
            String status=chunkTranscription.getTranscriptionStatus();


            Log.i("chunk_path_status",chunkTranscription.getTranscriptionStatus());
            Log.i("chunk_path_status", String.valueOf(i++));
            if(status.contains("in")||status.contains("progress")||status.contains("in_progress"))
            {
                TranscriptionUtils.getTranscriptionStatus(chunkTranscription.getChunkId(), new TranscriptionUtils.TranscriptionStatusCallback() {
                    @Override
                    public void onTranscriptionStatusSuccess(String name, String status, int queuePosition) {
                        Log.e("chunk_path","status by transcription_api "+name.toString());
                        holder.transcription_status.setText("getting response from api");

                        s[0] = s[0] +name;
                        //update transcription_status and increase it by 1;

                        //if received transcription then update chunks status to done

                    }

                    @Override
                    public void onTranscriptionStatusError(String errorMessage) {
                        // Handle error response

                        holder.transcription_status.setText("error in response of some api");

                    }
                },chunkTranscription.getChunkPath());
                holder.Description_api.setText(s[0]);
            }

        }

    }
    public boolean isMessageSuccessful(String jsonResponse) {
        try {
            // Parse the JSON response


            // Check if the "message" field matches the expected value
            String alreadyexist="Recording already exists. Please call the status API to see the progress";
            String expectedMessage = "Recording uploaded successfully and queued for transcription.";
            if (jsonResponse.contains("message") && (jsonResponse.contains("uploaded successfully")||jsonResponse.contains("already exists"))) {
                return true;
            }
        } catch (Exception e) {
            // Handle any parsing exceptions
            e.printStackTrace();
        }
        return false;
    }
    private void send_chunk_for_transcription(String chunkPath, String chunkId, String language,AudioViewHolder holder,Recording audioItem) {
        holder.transcription_progress_api.setVisibility(View.VISIBLE);
        holder.Description_api.setText("");

        try {
            String fileExtension = AudioUtils.getFileExtension(chunkPath).toLowerCase();
            String[] supportedFormats = {"wav", "m4a", "mp3", "webm", "mp4", "mpga", "mpeg","3gp"};
            Log.e("chunk_path",chunkPath+" sending for transcription to server");


            if (Arrays.asList(supportedFormats).contains(fileExtension)) {
                TranscriptionUtils.send_for_transcription(context,chunkPath, new TranscriptionUtils.TranscriptionCallback() {
                    @Override
                    public void onSuccess(String transcription) {
                        Log.d("chunk_path","success "+transcription+" for "+chunkPath+"\n");
                        if(isMessageSuccessful(transcription))
                        {
                            //Toast.makeText(context, "updated chunk status", Toast.LENGTH_SHORT).show();
                            chunks_database_helper.updateChunkStatus(chunkId,"in_progress");
                            holder.transcription_status.setText(chunks_database_helper.getTotalChunksByRecordingId(audioItem.getRecordingId())+" chunks sent to api of "+chunks_database_helper.getChunksCountByRecordingIdAndStatus(audioItem.getRecordingId(),"in_progress"));
                        }
                        else
                        {
                            //Toast.makeText(context, "unable to update chunk status", Toast.LENGTH_SHORT).show();
                            holder.transcription_status.setText("Some error in sending chunk..");


                        }



                    }

                    @Override
                    public void onError(String errorMessage) {
                        Log.d("chunk_path","error "+errorMessage+" for "+chunkPath+"\n");
                        holder.transcription_status.setText("Some error in sending chunk..");

                        allTranscriptionProgress=false;

                    }
                },chunkId,language);

            }
            else {
                allTranscriptionProgress=false;
                databaseHelper.updaterecording_details_api(audioItem.getRecordingId(),"Unsupported format");
                audioItem.setIs_transcribed_api("yes");
                audioItem.setDescription_api("Unsupported format "+fileExtension);
                holder.Description_api.setText("Unsupported format");
                holder.transcription_progress_api.setVisibility(View.GONE);

            }


        }catch (Exception e)
        {
            allTranscriptionProgress=false;
            Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
            holder.Description_api.setText("error "+e.getMessage());
            holder.transcription_status.setText("Some error in sending chunk..");
            holder.transcription_progress_api.setVisibility(View.GONE);


            //  Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
        }

    }


    private void handleTranscription(Recording audioItem, int position, AudioViewHolder holder,String language) {
        String fileExtension = AudioUtils.getFileExtension(audioItem.getUrl()).toLowerCase();
        holder.Description_api.setText("");
        String[] supportedFormats = {"wav", "3gp"};
        if (!Arrays.asList(supportedFormats).contains(fileExtension)) {
            boolean updateSuccess = databaseHelper.updateRecordingDetails(audioItem.getRecordingId(), "Unsupported Format");
            holder.Description_api.setText("");
            // Switch back to main thread to update UI or notify adapter
            new Handler(Looper.getMainLooper()).post(() -> {
                if (updateSuccess) {
                    Log.d("hellorecorder", "Recording updated successfully.");
                    audioItem.setDescription("Unsupported Format"); // Assuming the setDescription method updates COL_DES
                    audioItem.setIs_transcribed("yes"); // Assuming this is the flag for COL_IS_TRANSCRIBED
                    holder.Description.setText("Unsupported Format");
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
                    // Update the database
                    boolean updateSuccess = databaseHelper.updateRecordingDetails(audioItem.getRecordingId(), transcription);

                    // Switch back to main thread to update UI or notify adapter
                    new Handler(Looper.getMainLooper()).post(() -> {
                        if (updateSuccess) {
                            Log.d("hellorecorder", "Recording updated successfully.");
                            audioItem.setDescription(transcription); // Assuming the setDescription method updates COL_DES
                            audioItem.setIs_transcribed("yes"); // Assuming this is the flag for COL_IS_TRANSCRIBED
                            holder.Description.setText(transcription);
                            holder.transcription_progress.setVisibility(View.GONE);
                            //recordingList.set(position, audioItem);

                            // Notify adapter about the item change
                            //notifyItemChanged(position);
                            AudioRecyclerAdapter.selectedItems.add(transcription);
                            saveSelectionStatet(audioItem.getEventId(),position);
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
//                notifyItemChanged(position);
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
    public  void saveSelectionStatet(int id,int position) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putStringSet("selected_items_"+id, new HashSet<>(selectedItems));
        editor.apply();
        loadSelectionState(id);
        notifyItemChanged(position);

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
        TextView recordingLabel,ceation_d_and_t,total_time,Description,Description_api,transcription_status;
        ImageView expand_button,add_to_list,delete,expand_button_api;

        CardView recordingCard;
        LinearLayout transcription_progress,transcription_progress_api;
        Spinner languageSpinner;

        Switch model_switch;
        TextView Local_t,Server_t;
        ConstraintLayout local_c,server_C;

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
