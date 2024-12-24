package com.example.recorderchunks;// MainActivity.java



import static com.example.recorderchunks.Adapter.PromptAdapter.KEY_PROMPT_ID;
import static com.example.recorderchunks.utils.AudioUtils.isWavFormatValid;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.arthenica.mobileffmpeg.FFmpeg;

import com.example.recorderchunks.Adapter.AudioRecyclerAdapter;
import com.example.recorderchunks.Model_Class.Event;
import com.example.recorderchunks.Model_Class.Prompt;
import com.example.recorderchunks.Model_Class.Recording;
import com.example.recorderchunks.utils.AudioUtils;
import com.example.recorderchunks.utils.NotificationUtils;
import com.example.recorderchunks.utils.ZipUtils;
import com.github.file_picker.FilePicker;
import com.github.file_picker.FileType;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import org.json.JSONException;
import org.json.JSONObject;
import org.vosk.Model;
import org.vosk.Recognizer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Add_Event extends AppCompatActivity implements AudioRecyclerAdapter.OnSelectionChangedListener  {
    LinearLayout event_description_view,all_transcription_view;
    private boolean isExpanded = false; // To track collapse/expand state
    private boolean isExpanded2 = false; // To track collapse/expand state
    private TextView selectedDateTime,alltranscription;
    private EditText eventDescription;
    private ImageView listvbutton;
    private int eventId;
    DatabaseHelper databaseHelper;
    Prompt_Database_Helper promptDatabaseHelper;
    private  Button datePickerBtn,timePickerBtn,make_note,stop_recording_animation, saveEventButton,import_button,hide_recording_animation,stop_recording_small_animation;
    private static final int REQUEST_CODE_SPEECH_INPUT = 100;
    Button recordButton;
    private Spinner prompt_spinner;

    /////////////////////timer
    private TextView textViewTimer;
    private Handler handler = new Handler();
    private Runnable runnable;
    private boolean isTimerRunning = true;


    //////////////////////////////////saving audio//////////////////////////////////////////
    private MediaRecorder mediaRecorder;
    private boolean isRecordingCompleted = false;
    private String audioFilePath;
    private long startTime, stopTime;

    CardView recording_small_card;
    TextView textView_small_Timer;

    ////////////////////////////////////playing and pausing audio///////////////////////////
    private CardView recording_animation_card;
    private MediaPlayer mediaPlayer;

    private  boolean recorder=false;

    //////////////////// vosk model

    private static Model voskModel;

    //////////////////////////////for gemini api

    private RequestQueue requestQueue;

    private SharedPreferences sharedPreferences;

    /////////////////////////////////////////////////////
    private RecyclerView recyclerView;
    private AudioRecyclerAdapter recordingAdapter;
    private ArrayList<Recording> recordingList;
    private static final int REQUEST_AUDIO_PICK = 1;
    String prompt_message="";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_event);
        setupModel();
        initializeVoskModel();
        databaseHelper = new DatabaseHelper(this);
        promptDatabaseHelper=new Prompt_Database_Helper(Add_Event.this);

        ///////////////
        prompt_spinner = findViewById(R.id.prompt_spinner);
        String[] languages = promptDatabaseHelper.getAllPromptTexts();



        // Create an ArrayAdapter using the language list
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                languages
        );

        // Set the layout for dropdown items
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Attach the adapter to the Spinner
        prompt_spinner.setAdapter(adapter);

        prompt_spinner.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    // Check if languages array is empty when Spinner is clicked
                    if (languages == null || languages.length == 0) {
                        Intent intent = new Intent(Add_Event.this, Manage_Prompt.class);
                        startActivity(intent);
                        finish(); // Close current activity if needed
                        return true; // Stop further processing of the click
                    }
                }
                return false; // Let the Spinner behave normally
            }
        });

        prompt_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Get the selected language
                prompt_message = languages[position];
               // Toast.makeText(Add_Event.this, "Selected Language: " + selectedLanguage, Toast.LENGTH_SHORT).show();


            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Handle case when no selection is made (optional)
            }
        });



        ////////////////////




        ////////////////////

        import_button=findViewById(R.id.import_button);
        import_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //openAudioPicker();
                new FilePicker.Builder(Add_Event.this)
                        .setLimitItemSelection(1)
                        .setAccentColor(getResources().getColor(R.color.secondary))
                        .setCancellable(true)
                        .setFileType(FileType.AUDIO)
                        .setOnSubmitClickListener(files -> {
                            if (files != null && !files.isEmpty()) {
                                String selectedFilePath = files.get(0).getFile().getAbsolutePath(); // Get the file path
                                //Toast.makeText(Add_Event.this, "Selected File: " + selectedFilePath, Toast.LENGTH_SHORT).show();
                                try {
                                    if(eventId!=-1)
                                    {
                                        saveAudioToDatabase(eventId, selectedFilePath);

                                    }
                                    else
                                    {
                                        saveAudioToDatabase(databaseHelper.getNextEventId(), selectedFilePath);

                                    }
                                   recognizeSpeech(convertToWav(selectedFilePath));

                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            } else {
                                Toast.makeText(Add_Event.this, "No file selected", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setOnItemClickListener((media, pos, adapter) -> {
                            if (!media.getFile().isDirectory()) {
                                adapter.setSelected(pos);
                            }
                        })
                        .buildAndShow();


            }
        });
        make_note=findViewById(R.id.make_note);
        make_note.setVisibility(View.GONE);
        requestQueue = Volley.newRequestQueue(this);
        sharedPreferences = getSharedPreferences("ApiKeysPref", MODE_PRIVATE);
        String prompt_id = sharedPreferences.getString(KEY_PROMPT_ID, "");
        try {
            Prompt prompt=promptDatabaseHelper.getPromptById(Integer.parseInt(prompt_id));
            prompt_message=prompt.getText();
            //
        }
        catch (Exception e)
        {

        }
        // Toast.makeText(this,"the prompt is : "+ prompt_message, Toast.LENGTH_SHORT).show();
        CardView progressBar = findViewById(R.id.transcribing_progress);
        progressBar.setVisibility(View.GONE);
        recording_animation_card=findViewById(R.id.recording_card);
        recording_animation_card.setVisibility(View.GONE);
        stop_recording_animation=findViewById(R.id.stop_recording_animation);
        stop_recording_animation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // References to the ProgressBar and TextView

                recording_animation_card.setVisibility(View.GONE);
                recording_small_card.setVisibility(View.INVISIBLE);
                recorder = false;
                recordButton.setText("Start Recording");

               // progressBar.setVisibility(View.VISIBLE);


// Record the start time for duration calculation
                long startTime = System.currentTimeMillis();

                new Thread(() -> {
                    stopRecording(audioFilePath); // Perform the recording stop logic

                    // Capture the end time and calculate duration
                    long endTime = System.currentTimeMillis();
                    long durationInSeconds = (endTime - startTime) / 1000;

                    // Update UI after completion
                    runOnUiThread(() -> {
                        // Hide progress bar and update the message
                       // progressBar.setVisibility(View.GONE);
                        recordingAdapter.notifyDataSetChanged();


                        // Optional: Display a Toast
                        Toast.makeText(getApplicationContext(), "Transcription completed in " + durationInSeconds + " seconds.", Toast.LENGTH_SHORT).show();
                    });
                }).start();
            }
        });

        ////////////////////////
        datePickerBtn = findViewById(R.id.datePickerBtn);
        timePickerBtn = findViewById(R.id.timePickerBtn);
        recordButton = findViewById(R.id.recordButton);
        selectedDateTime = findViewById(R.id.selectedDateTime);
        eventDescription = findViewById(R.id.eventDescription);
        saveEventButton = findViewById(R.id.saveEventButton);
        event_description_view=findViewById(R.id.summary_text);
        all_transcription_view=findViewById(R.id.all_transcriptions);

        alltranscription =findViewById(R.id.alltranscription);
        listvbutton=findViewById(R.id.listvbutton);
        eventId = getIntent().getIntExtra("eventId", -1);
        List<String> descriptions = databaseHelper.getDescriptionsByEventId(eventId);
        if(!descriptions.isEmpty())
        {
            make_note.setVisibility(View.VISIBLE);
        }




        int nextEventId = databaseHelper.getNextEventId();
        int maxrecording_event=databaseHelper.getMaxEventIdFromRecordings();
        if(nextEventId==maxrecording_event)
        {
            databaseHelper.deleteAllRecordingsByEventId(nextEventId);


        }
//        Toast.makeText(this, "Max Recording is : "+maxrecording_event, Toast.LENGTH_SHORT).show();
//        Toast.makeText(this, "Max next event is : "+nextEventId, Toast.LENGTH_SHORT).show();
//        // Set up the toolbar as the app bar
        Toolbar toolbar = findViewById(R.id.appBar);
        setSupportActionBar(toolbar);

        recyclerView = findViewById(R.id.recordings_recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recordingList = new ArrayList<>();

        // Enable back button in toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        ////////////////////////////////////////////////////////////////////


        //////////////////////// Incase we are coming from previous activity




        if (eventId != -1) {
            // Fetch event details from the database
            DatabaseHelper db = new DatabaseHelper(this);
            Event event = db.getEventById(eventId);

            if (event != null) {
                toolbar.setTitle("Recording Details");
                saveEventButton.setText("Update Event");

                // Populate the views with event details
                ((EditText) findViewById(R.id.eventTitle)).setText(event.getTitle());
                eventDescription.setText(event.getDescription());
                selectedDateTime.setText("created on : " +
                        event.getCreationDate() + " at  " + event.getCreationTime());
                datePickerBtn.setText(event.getEventDate());
                timePickerBtn.setText(event.getEventTime());

                //
                recordingList.clear();
                recordingList=databaseHelper.getRecordingsByEventId(eventId);

                recordingAdapter = new AudioRecyclerAdapter(recordingList,this,this );
                updateSelectedItemsDisplay(new ArrayList<>());

                recyclerView.setAdapter(recordingAdapter);
                recordingAdapter.notifyDataSetChanged();

            } else {
                Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show();
                finish(); // Close the activity if the event is not found
            }
        } else {
            //Toast.makeText(this, "Invalid Event ID", Toast.LENGTH_SHORT).show();
            //finish(); // Close the activity if no valid ID is passed
        }





        ///////////////////////////


        if(eventDescription.getText().toString().isEmpty())
        {
            event_description_view.setVisibility(View.GONE);
        }
        if(alltranscription.getText().toString().contains("No items selected"))
        {
            all_transcription_view.setVisibility(View.GONE);
        }
        ///////////////////////////

        saveEventButton.setOnClickListener(view -> {
            if(eventId!=-1)
            {
                String title = ((EditText) findViewById(R.id.eventTitle)).getText().toString();
                String eventdescription = eventDescription.getText().toString();
                String selectedDate = datePickerBtn.getText().toString();
                String selectedTime = timePickerBtn.getText().toString();

                updateEventData(eventId,title, eventdescription, selectedDate, selectedTime);

            }
            else
            {
                isRecordingCompleted=true;
                // Get the event title, description, selected date, and selected time
                if (isRecordingCompleted) {  // Show only when recording is completed
                    String title = ((EditText) findViewById(R.id.eventTitle)).getText().toString();
                    String eventdescription = eventDescription.getText().toString();
                    String selectedDate = datePickerBtn.getText().toString();
                    String selectedTime = timePickerBtn.getText().toString();

                    saveEventData(title, eventdescription, selectedDate, selectedTime,nextEventId);
                } else {
                    Toast.makeText(this, "Please complete the recording first", Toast.LENGTH_SHORT).show();
                }
            }

        });


        ////////////////

        make_note.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String prompt="";// If the EditText is empty, load the saved prompt from SharedPreferences
                if (TextUtils.isEmpty(prompt)) {
                    prompt = prompt_message;

                    if (TextUtils.isEmpty(prompt)) {
                        //   Toast.makeText(this, , Toast.LENGTH_SHORT).show();
                        Toast.makeText(Add_Event.this, "Prompt is required", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }

                // Check which API is selected
                String selectedApi = sharedPreferences.getString("SelectedApi", "use ChatGpt");
                event_description_view.setVisibility(View.VISIBLE);

                switch (selectedApi) {
                    case "use ChatGpt":
                        getoutput_chatgpt(prompt+":"+alltranscription.getText().toString());
                        break;

                    case "use Gemini Ai":
                        getoutput_gemini(prompt+":"+alltranscription.getText().toString());
                        break;

                    default:
                        getoutput_gemini(prompt+":"+alltranscription.getText().toString());



                        //Toast.makeText(Add_Event.this, "No API selected : "+selectedApi, Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        });





        // Display Current Date and Time
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss", Locale.getDefault());
        String currentDateTime = dateFormat.format(new Date());
        selectedDateTime.setText("Current Date and Time: " + currentDateTime);

        // Date Picker
        datePickerBtn.setOnClickListener(view -> showDatePicker());

        // Time Picker
        timePickerBtn.setOnClickListener(view -> showTimePicker());

        // Speech to Text
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 1);
        recordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!recorder)
                {

                    textViewTimer = findViewById(R.id.textViewTimer);

                    // Initialize the start time when the activity is created
                    startTime = System.currentTimeMillis();

                    // Define the runnable to update the timer continuously
                    runnable = new Runnable() {
                        @Override
                        public void run() {
                            if (isTimerRunning) {
                                // Calculate the elapsed time
                                long elapsedTime = System.currentTimeMillis() - startTime;

                                // Convert milliseconds to minutes and seconds
                                int seconds = (int) (elapsedTime / 1000);
                                int minutes = seconds / 60;
                                seconds = seconds % 60;

                                // Format the time as mm:ss
                                String time = String.format("%02d:%02d", minutes, seconds);

                                // Update the UI with the current time
                                textViewTimer.setText(time);
                                textView_small_Timer.setText(time);


                                // Repeat this runnable every 1 second
                                handler.postDelayed(this, 1000);
                            }
                        }
                    };
                    runnable.run();
                    recorder=true;
                    recordButton.setText("Stop Recording");
                    recording_animation_card.setVisibility(View.VISIBLE);
                    startRecording();
                }
                else
                {
                    recording_animation_card.setVisibility(View.GONE);
                    recording_small_card.setVisibility(View.GONE);
                    recorder = false;
                    recordButton.setText("Start Recording");

                    // progressBar.setVisibility(View.VISIBLE);


// Record the start time for duration calculation
                    long startTime = System.currentTimeMillis();

                    new Thread(() -> {
                        stopRecording(audioFilePath); // Perform the recording stop logic

                        // Capture the end time and calculate duration
                        long endTime = System.currentTimeMillis();
                        long durationInSeconds = (endTime - startTime) / 1000;

                        // Update UI after completion
                        runOnUiThread(() -> {
                            // Hide progress bar and update the message
                            // progressBar.setVisibility(View.GONE);
                            recordingAdapter.notifyDataSetChanged();


                            // Optional: Display a Toast
                            Toast.makeText(getApplicationContext(), "Transcription completed in " + durationInSeconds + " seconds.", Toast.LENGTH_SHORT).show();
                        });
                    }).start();


                }

//
            }
        });

        ImageView toggleShowHide = findViewById(R.id.toggle_show_hide);

        // Set initial state
       // textInputLayout.setVisibility(View.GONE);

        // Toggle behavior
        toggleShowHide.setOnClickListener(v -> {
            if (isExpanded) {
                // Collapse: Show only one line with ellipsis
                eventDescription.setMaxLines(1);
                eventDescription.setEllipsize(android.text.TextUtils.TruncateAt.END);
                toggleShowHide.setImageResource(R.mipmap.expand); // Change to expand icon
                isExpanded = false;
            } else {
                // Expand: Show all lines
                if (eventDescription.getText().toString().length() > 200) { // Define your threshold
                    Intent intent = new Intent(Add_Event.this, activity_text_display.class);
                    intent.putExtra("text", eventDescription.getText().toString());
                    intent.putExtra("Title","Description");

                    startActivity(intent);
                }
                else
                {
                    eventDescription.setMaxLines(Integer.MAX_VALUE);
                    eventDescription.setEllipsize(null); // Remove ellipsis
                    toggleShowHide.setImageResource(R.mipmap.collapse); // Change to collapse icon
                    isExpanded = true;
                }

            }
        });
        // Toggle behavior
        listvbutton.setOnClickListener(v -> {
            if (isExpanded2) {
                // Collapse: Show only one line with ellipsis
                alltranscription.setMaxLines(1);
                alltranscription.setEllipsize(android.text.TextUtils.TruncateAt.END);
                listvbutton.setImageResource(R.drawable.baseline_description_24); // Change to expand icon
                isExpanded2 = false;
            } else {
                // Expand: Show all lines
                if (alltranscription.getText().toString().length() > 200) { // Define your threshold
                    Intent intent = new Intent(Add_Event.this, activity_text_display.class);
                    intent.putExtra("text", alltranscription.getText().toString());
                    intent.putExtra("Title","All Transcription");

                    startActivity(intent);
                }
                else
                {
                    alltranscription.setMaxLines(Integer.MAX_VALUE);
                    alltranscription.setEllipsize(null); // Remove ellipsis
                    listvbutton.setImageResource(R.drawable.baseline_cancel_24); // Change to collapse icon
                    isExpanded2 = true;
                }

            }
        });

         /////////////////////////////////////
        recording_small_card=findViewById(R.id.recording_small_card);
        textView_small_Timer=findViewById(R.id.textView_small_Timer);
        stop_recording_small_animation=findViewById(R.id.stop_recording_small_animation);
        stop_recording_small_animation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                recording_animation_card.setVisibility(View.GONE);
                recording_small_card.setVisibility(View.INVISIBLE);
                recorder = false;
                recordButton.setText("Start Recording");

                // progressBar.setVisibility(View.VISIBLE);


// Record the start time for duration calculation
                long startTime = System.currentTimeMillis();

                new Thread(() -> {
                    stopRecording(audioFilePath); // Perform the recording stop logic

                    // Capture the end time and calculate duration
                    long endTime = System.currentTimeMillis();
                    long durationInSeconds = (endTime - startTime) / 1000;

                    // Update UI after completion
                    runOnUiThread(() -> {
                        // Hide progress bar and update the message
                        // progressBar.setVisibility(View.GONE);
                        recordingAdapter.notifyDataSetChanged();


                        // Optional: Display a Toast
                        Toast.makeText(getApplicationContext(), "Transcription completed in " + durationInSeconds + " seconds.", Toast.LENGTH_SHORT).show();
                    });
                }).start();

            }
        });
        recording_small_card.setVisibility(View.GONE);


        hide_recording_animation=findViewById(R.id.hide_recording_animation);
        hide_recording_animation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                recording_animation_card.setVisibility(View.GONE);
                recording_small_card.setVisibility(View.VISIBLE);
            }
        });
        recording_small_card.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                recording_animation_card.setVisibility(View.VISIBLE);
                recording_small_card.setVisibility(View.GONE);
            }
        });




    }

    //------------------------------------
    private  void  getoutput_chatgpt(String prompt)
    {
        String apiKey = sharedPreferences.getString("ChatGptApiKey", "");
        String apiUrl = "https://api.openai.com/v1/completions"; // Replace with ChatGPT endpoint

        if (TextUtils.isEmpty(apiKey)) {
            Toast.makeText(this, "ChatGPT API key is missing", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            JSONObject jsonRequest = new JSONObject();
            jsonRequest.put("model", "text-davinci-003"); // Replace with your model
            jsonRequest.put("prompt", prompt);
            jsonRequest.put("max_tokens", 100);

            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                    Request.Method.POST,
                    apiUrl,
                    jsonRequest,
                    response -> {
                        try {
                            String output = response.getString("choices");
                            Toast.makeText(this, "ChatGPT Output: " + output, Toast.LENGTH_LONG).show();
                        } catch (JSONException e) {
                            Toast.makeText(this, "Error parsing response", Toast.LENGTH_SHORT).show();
                        }
                    },
                    error -> Toast.makeText(this, "ChatGPT API Error: " + error.getMessage(), Toast.LENGTH_SHORT).show()
            ) {
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Authorization", "Bearer " + apiKey);
                    headers.put("Content-Type", "application/json");
                    return headers;
                }
            };

            requestQueue.add(jsonObjectRequest);

        } catch (JSONException e) {
            Toast.makeText(this, "Error creating request", Toast.LENGTH_SHORT).show();
        }
    }
    //------------------------------------
    private void getoutput_gemini(String prompt) {
        // Set up the URL for the Gemini API endpoint
       get_opt(prompt);
    }
    //------------------------------------
    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        new android.app.DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

            // Update the button text to the selected date
            datePickerBtn.setText(dayOfMonth + "/" + (month + 1) + "/" + year);
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }
    private void showTimePicker() {
        Calendar calendar = Calendar.getInstance();
        new android.app.TimePickerDialog(this, (view, hourOfDay, minute) -> {
            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
            calendar.set(Calendar.MINUTE, minute);

            // Update the button text to the selected time
            timePickerBtn.setText(String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute));

            // Update the date and time display
        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show();
    }

    public  void get_opt(String prompt)
    {
        GenerativeModel gm = new GenerativeModel(
                "gemini-1.5-flash-001",
               "AIzaSyDp4QqV17XLUsZsSjgCLKdZdVTcWCZqeUk"
        );

        GenerativeModelFutures model = GenerativeModelFutures.from(gm);

        Toast.makeText(this, "'"+prompt+"'", Toast.LENGTH_SHORT).show();
        Content content = new Content.Builder()
                .addText("'"+prompt+"'")
                .build();


        ListenableFuture<GenerateContentResponse> response = model.generateContent(content);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
                @Override
                public void onSuccess(GenerateContentResponse result) {
                    String resultText = result.getText();
                    Toast.makeText(Add_Event.this, resultText, Toast.LENGTH_SHORT).show();
                    eventDescription.setText(resultText);
                }

                @Override
                public void onFailure(Throwable t) {
                    t.printStackTrace();
                }
            }, this.getMainExecutor());
        }
    }
    private void updateSelectedItemsDisplay(ArrayList<String> selectedItems) {
        if (selectedItems.isEmpty()) {
            alltranscription.setText("No items selected");
            all_transcription_view.setVisibility(View.GONE);
            make_note.setVisibility(View.GONE);

        } else {
            String  des=TextUtils.join(", ", selectedItems);
            if(des.equals(""))
            {
                all_transcription_view.setVisibility(View.GONE);
                make_note.setVisibility(View.GONE);

            }
            else
            {
                alltranscription.setText(TextUtils.join(", ", selectedItems));
                all_transcription_view.setVisibility(View.VISIBLE);
                make_note.setVisibility(View.VISIBLE);
            }



        }
    }
    private void setupModel() {
        File modelDir = new File(getExternalFilesDir(null), "vosk-model");
        if (!modelDir.exists()) {

            modelDir.mkdirs();
            try (InputStream zipInputStream = getAssets().open("model.zip")) {
                ZipUtils.extractZip(zipInputStream, modelDir);
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "error in extracting zip", Toast.LENGTH_SHORT).show();
            }

        }
    }
    private void initializeVoskModel() {
        try {
            // Ensure model extraction
           setupModel();
           try {

               File modelDir = new File(getExternalFilesDir(null), "vosk-model/vosk-model-small-en-in-0.4");

               // Load the Vosk model
               voskModel = new Model(modelDir.getPath());
           }
           catch (Exception e)
           {
               Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show();
           }
            //Toast.makeText(this, "Vosk model loaded successfully", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Vosk model failed to load", Toast.LENGTH_SHORT).show();
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SPEECH_INPUT && resultCode == RESULT_OK && data != null) {
            ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (result != null && !result.isEmpty()) {
                eventDescription.setText(result.get(0));
            }
        }


       // stopRecording();
    }
    private String getAudioDuration(String audioPath) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();

        try {
            retriever.setDataSource(audioPath); // Pass the resolved file path
            String duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            retriever.release();

            if (duration != null) {
                long durationMs = Long.parseLong(duration);
                return String.valueOf(durationMs / 1000); // Convert to seconds
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "0"; // Default if duration is unavailable
    }

    private String getFileExtension(String filePath) {
        return filePath.substring(filePath.lastIndexOf(".") + 1);
    }
    private String getFileName(String filePath) {
        return filePath.substring(filePath.lastIndexOf("/") + 1);
    }
    private void saveAudioToDatabase(int eventId_c, String audioPath) throws IOException {
        DatabaseHelper databaseHelper = new DatabaseHelper(this);

        // Extract metadata (name, format, length) from the audio file
        String name = getFileName(audioPath);         // Extract file name from the path
        String format = getFileExtension(audioPath);  // Extract file extension as format
        String length = getAudioDuration(audioPath);  // Get audio duration in seconds
        boolean isRecorded = false;                   // Set to false since it's imported
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yy 'at' HH:mm");
        String description =recognizeSpeech(convertToWav(audioPath));
        // Get the current date and time
        Date date = new Date();
        eventId=eventId_c;

        // Format the date
        String formattedDate = formatter.format(date);
        boolean isInserted = databaseHelper.insertRecording(
                eventId_c,

                formattedDate,
                description,// Associated event ID
                name,       // Recording name
                format,     // Recording format (e.g., mp3)
                length,     // Duration of the recording
                audioPath,  // Full path of the audio file
                isRecorded,
                "no"// Imported, not recorded
        );


        if (isInserted) {

            AudioRecyclerAdapter.selectedItems.add(description);
            AudioRecyclerAdapter.saveSelectionState(eventId);
            Toast.makeText(this, "Audio saved to database", Toast.LENGTH_SHORT).show();
            recordingList.clear();
            recordingList=databaseHelper.getRecordingsByEventId(eventId);
            recordingAdapter = new AudioRecyclerAdapter(recordingList,this ,this);

            recyclerView.setAdapter(recordingAdapter);
            recordingAdapter.notifyDataSetChanged();
        } else {
            Toast.makeText(this, "Failed to save audio", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveEventData(String title, String eventDescription, String selectedDate, String selectedTime,int eventId_m) {
        if (title == null || title.trim().isEmpty()) {
            Toast.makeText(this, "Event title cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        if (eventDescription == null || eventDescription.trim().isEmpty()) {
            eventDescription="";
        }

        if (selectedDate == null || selectedDate.trim().isEmpty()||selectedDate.contains("Pick")) {
            Toast.makeText(this, "Event date cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedTime == null || selectedTime.trim().isEmpty()||selectedTime.contains("Pick")) {
            Toast.makeText(this, "Event time cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        // Assuming `DatabaseHelper` is your class that interacts with the SQLite database
        DatabaseHelper databaseHelper = new DatabaseHelper(this);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String creationDate = dateFormat.format(new Date());
        String creationTime = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());

        // Save event to database
        boolean isInserted = databaseHelper.insertEvent(
                eventId_m,
                title,
                eventDescription,
                creationDate,
                creationTime,
                selectedDate,
                selectedTime,
                audioFilePath
        );

        // Show success or failure message
        if (isInserted) {
            try {
                Intent i=new Intent(Add_Event.this,MainActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(i);
                finish();

            }
            catch (Exception e)
            {
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }

            Toast.makeText(this, "Event saved successfully!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Failed to save event", Toast.LENGTH_SHORT).show();
        }
    }
    private void updateEventData(int eventId, String title, String eventDescription, String selectedDate, String selectedTime) {
        if (title == null || title.trim().isEmpty()) {
            Toast.makeText(this, "Event title cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        if (eventDescription == null || eventDescription.trim().isEmpty()) {
            Toast.makeText(this, "Event description cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedDate == null || selectedDate.trim().isEmpty() || selectedDate.contains("Pick")) {
            Toast.makeText(this, "Event date cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedTime == null || selectedTime.trim().isEmpty() || selectedTime.contains("Pick")) {
            Toast.makeText(this, "Event time cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        // Assuming `DatabaseHelper` is your class that interacts with the SQLite database
        DatabaseHelper databaseHelper = new DatabaseHelper(this);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String creationDate = dateFormat.format(new Date());
        String creationTime = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());

        // Update event in database
        boolean isUpdated = databaseHelper.updateEvent(
                eventId, // ID of the event to update
                title,
                eventDescription,
                creationDate,
                creationTime,
                selectedDate,
                selectedTime,
                audioFilePath // Assuming `audioFilePath` is already set in your activity
        );

        // Show success or failure message
        if (isUpdated) {
            try {
                Intent i = new Intent(Add_Event.this, MainActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(i);
                finish();
            } catch (Exception e) {
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }

            Toast.makeText(this, "Event updated successfully!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Failed to update event", Toast.LENGTH_SHORT).show();
        }
    }

    //////////////////////////////////functions to save audio///////////////////////////////////////////
    private void startRecording() {
        NotificationUtils.createNotificationChannel(Add_Event.this);
        NotificationUtils.showRecordingNotification(Add_Event.this);
        recording_animation_card.setVisibility(View.VISIBLE);
        File audioDir = getExternalFilesDir(Environment.DIRECTORY_MUSIC);
        if (audioDir != null) {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            audioFilePath = audioDir.getAbsolutePath() + "/recording_" + timeStamp + ".3gp";
        } else {
            Toast.makeText(this, "Failed to get storage directory", Toast.LENGTH_SHORT).show();
            return;
        }

        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mediaRecorder.setOutputFile(audioFilePath);

        try {
            mediaRecorder.prepare();
            mediaRecorder.start();
            startTime = System.currentTimeMillis();
           // Toast.makeText(this, "Recording started...", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error preparing MediaRecorder: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    public String convertToWav(String inputPath) {
//        recording_animation_card.setVisibility(View.GONE);
        File inputFile = new File(inputPath);
        if (!inputFile.exists()) {
            runOnUiThread(() ->Toast.makeText(this, "Input file not found", Toast.LENGTH_SHORT).show());

            return null;
        }

        // Validate the input file format
        String fileExtension = getFileExtension(inputPath).toLowerCase();
        String[] supportedFormats = {"wav", "3gp"};
        if (!Arrays.asList(supportedFormats).contains(fileExtension)) {
            runOnUiThread(() ->Toast.makeText(this, "Unsupported file format: " + fileExtension, Toast.LENGTH_SHORT).show());

            return null;
        }

        // Check if input WAV file needs conversion
        if (fileExtension.equals("wav")) {
            if (isWavFormatValid(inputPath,this)) {
                runOnUiThread(() ->Toast.makeText(this, "Input file is already in the desired WAV format", Toast.LENGTH_SHORT).show());

                return inputPath; // No conversion needed
            } else {
                // Adjust the sample rate of the WAV file
                return adjustSampleRate(inputPath);
            }
        }

        // For non-WAV files, convert them to WAV
        String outputPath = AudioUtils.convertToWavFilePath(inputPath);
        return performConversion(inputPath, outputPath);
    }
    public String performConversion(String inputPath, String outputPath) {
        File outputFile = new File(outputPath);
        if (!outputFile.getParentFile().exists()) {
            outputFile.getParentFile().mkdirs();
        }

        // Construct the FFmpeg command for conversion
        String command = String.format(
                "-i \"%s\" -ar 16000 -ac 1 -c:a pcm_s16le -y \"%s\"",
                inputPath, outputPath
        );

        int rc = FFmpeg.execute(command);
        if (rc == 0) {
            runOnUiThread(() ->Toast.makeText(this, "Conversion to WAV successful ", Toast.LENGTH_SHORT).show());

            return outputPath;
        } else {
            runOnUiThread(() ->Toast.makeText(this, "Conversion failed: " + rc, Toast.LENGTH_SHORT).show());

            return null;
        }
    }
    public String adjustSampleRate(String inputPath) {
        // Generate a new output file name with '_adjusted_sample_rate' appended
        File inputFile = new File(inputPath);
        String inputFileName = inputFile.getName();
        String parentDirectory = inputFile.getParent();
        String newFileName = inputFileName.replace(".wav", "_adjusted_sample_rate.wav");
        String outputPath = new File(parentDirectory, newFileName).getAbsolutePath();

        // Construct the FFmpeg command to adjust the sample rate
        String command = String.format(
                "-i \"%s\" -ar 16000 -ac 1 -c:a pcm_s16le -y \"%s\"",
                inputPath, outputPath
        );

        int rc = FFmpeg.execute(command);


        // String ffmpegLogs = FFmpeg.getLastCommandOutput();
        if (rc == 0) {
            runOnUiThread(() ->Toast.makeText(this, "Sample rate adjustment successful: ", Toast.LENGTH_SHORT).show());

            return outputPath;
        } else {
            runOnUiThread(() ->Toast.makeText(this, "Sample rate adjustment failed: " + rc, Toast.LENGTH_SHORT).show());

           // Log.e("FFmpegError", "FFmpeg command failed: " + ffmpegLogs);
            return null;
        }
    }
    private void stopRecording(String audioPath) {
        NotificationUtils.cancelRecordingNotification(Add_Event.this);
        try {
            recording_animation_card.setVisibility(View.INVISIBLE);
            recording_small_card.setVisibility(View.INVISIBLE);


        }catch (Exception e)
        {

        }

        if (mediaRecorder != null) {
            mediaRecorder.stop();
            mediaRecorder.release();
            mediaRecorder = null;
            stopTime = System.currentTimeMillis();
            DatabaseHelper databaseHelper = new DatabaseHelper(this);
            int nextEventId = databaseHelper.getNextEventId();
            long duration = (stopTime - startTime) / 1000; // Duration in seconds

            // Generate details for the recording
            String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date(startTime));
            String time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date(startTime));
            String uniqueCode = date.replace("-", "") + time.replace(":", "") + duration;

            String recordingName = "Recording_" + uniqueCode; // Example naming
            String format = audioPath.substring(audioPath.lastIndexOf('.') + 1); // Extract format from file path
            String description ="description";

            // Save audio details in the database
            SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yy 'at' HH:mm");

            // Get the current date and time
            Date date2 = new Date();

            // Format the date
            String formattedDate = formatter.format(date2);
            boolean isSaved=false;

            if(eventId!=-1)
            {

                isSaved = databaseHelper.insertRecording(
                        eventId,
                        formattedDate,
                        description,// Replace with the actual event ID
                        recordingName,
                        format,
                        String.valueOf(duration),
                        audioPath, // Use the passed audioPath parameter
                        true,
                        "no"// Indicating this is a recorded file
                );
                if(isSaved)
                {
                    AudioRecyclerAdapter.selectedItems.add(description);
                    AudioRecyclerAdapter.saveSelectionState(eventId);
                }
            }
            else
            {

                isSaved = databaseHelper.insertRecording(
                        nextEventId,
                        formattedDate,
                        description,// Replace with the actual event ID
                        recordingName,
                        format,
                        String.valueOf(duration),
                        audioPath, // Use the passed audioPath parameter
                        true,
                        "no"// Indicating this is a recorded file
                );
                if(isSaved)
                {
                    AudioRecyclerAdapter.selectedItems.add(description);
                    AudioRecyclerAdapter.saveSelectionState(eventId);
                }
            }

            if (isSaved) {


                //runOnUiThread(() -> Toast.makeText(getApplicationContext(), "Recording saved successfully", Toast.LENGTH_SHORT).show());

                recordingList.clear();
                if(eventId!=-1)
                {
                    recordingList=databaseHelper.getRecordingsByEventId(eventId);

                }
                else
                {
                    recordingList=databaseHelper.getRecordingsByEventId(nextEventId);

                }
                runOnUiThread(()->{
                    recordingAdapter = new AudioRecyclerAdapter(recordingList,this,this );

                    recyclerView.setAdapter(recordingAdapter);
                    recordingAdapter.notifyDataSetChanged();});

            } else {
                //runOnUiThread(() -> Toast.makeText(getApplicationContext(), "Failed to save recording", Toast.LENGTH_SHORT).show());

            }
        }
        isRecordingCompleted = true;
    }

    public static String recognizeSpeech(String wavFilePath) {

        try (FileInputStream fis = new FileInputStream(wavFilePath)) {
            Recognizer recognizer = new Recognizer(voskModel, 16000);
            byte[] buffer = new byte[4000]; // Try doubling the size
            int bytesRead;

            while ((bytesRead = fis.read(buffer)) != -1) {
                if (recognizer.acceptWaveForm(buffer, bytesRead)) {

                } else {
                }
            }
            String transcription=extractTextFromResult(recognizer.getFinalResult());




            return transcription;

        } catch (IOException e) {
            e.printStackTrace();

           // Toast.makeText(this, "Error during recognition"+e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        return "Unable to save Transcription";
    }
    public static String extractTextFromResult(String jsonResult) {
        try {
            JSONObject result = new JSONObject(jsonResult);
            return result.optString("text", ""); // Extract the "text" field
        } catch (JSONException e) {
            e.printStackTrace();
            return "";
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSelectionChanged(ArrayList<String> updatedSelection) {
        updateSelectedItemsDisplay(updatedSelection);
    }
}
