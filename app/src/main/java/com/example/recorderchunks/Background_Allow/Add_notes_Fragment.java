package com.example.recorderchunks.Background_Allow;

import static android.content.Context.MODE_PRIVATE;
import static com.example.recorderchunks.AI_Transcription.AI_Notemaking.get_gemini_note;
import static com.example.recorderchunks.AI_Transcription.AI_Notemaking.getoutput_chatgpt;
import static com.example.recorderchunks.Activity.API_Updation.KEY_CHATGPT;
import static com.example.recorderchunks.Activity.API_Updation.KEY_GEMINI;
import static com.example.recorderchunks.Activity.API_Updation.SELECTED_LANGUAGE;
import static com.example.recorderchunks.Activity.API_Updation.SELECTED_TRANSCRIPTION_METHOD;
import static com.example.recorderchunks.utils.AudioUtils.getAudioDuration;
import static com.example.recorderchunks.utils.AudioUtils.getFileExtension;
import static com.example.recorderchunks.utils.AudioUtils.getFileName;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.recorderchunks.AI_Transcription.GeminiCallback;
import com.example.recorderchunks.Activity.RecordingService;
import com.example.recorderchunks.Activity.Show_all_ai_notes;
import com.example.recorderchunks.Adapter.AudioRecyclerAdapter;
import com.example.recorderchunks.Adapter.OnBackPressedListener;
import com.example.recorderchunks.Adapter.TagAdapter;
import com.example.recorderchunks.AudioPlayer.AudioPlayerViewModel;
import com.example.recorderchunks.Helpeerclasses.DatabaseHelper;
import com.example.recorderchunks.Helpeerclasses.Notes_Database_Helper;
import com.example.recorderchunks.Activity.Manage_Prompt;
import com.example.recorderchunks.Helpeerclasses.TagStorage;
import com.example.recorderchunks.ManageLogs.AppLogger;
import com.example.recorderchunks.Model_Class.Event;
import com.example.recorderchunks.Model_Class.Note;
import com.example.recorderchunks.Model_Class.Recording;
import com.example.recorderchunks.Model_Class.RecordingViewModel;
import com.example.recorderchunks.Model_Class.Tag;
import com.example.recorderchunks.Model_Class.current_event;
import com.example.recorderchunks.Model_Class.is_recording;
import com.example.recorderchunks.Model_Class.recording_event_no;
import com.example.recorderchunks.Model_Class.recording_language;
import com.example.recorderchunks.Helpeerclasses.Prompt_Database_Helper;
import com.example.recorderchunks.R;
import com.example.recorderchunks.Activity.activity_text_display;
import com.example.recorderchunks.utils.RecordingManager;
import com.example.recorderchunks.utils.RecordingUtils;
import com.example.recorderchunks.utils.TagUtils;
import com.github.file_picker.FilePicker;
import com.github.file_picker.FileType;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;


public class Add_notes_Fragment extends Fragment implements AudioRecyclerAdapter.OnSelectionChangedListener,RecordingUtils.RecordingCallback, OnBackPressedListener, TagAdapter.OnTagActionListener{
    AppLogger logger ;

    LinearLayout event_description_view, all_transcription_view;
    Toolbar toolbar;
    private boolean isExpanded = false; // To track collapse/expand state
    private boolean isExpanded2 = false; // To track collapse/expand state
    private TextView selectedDateTime, alltranscription,no_item_text;
    private EditText eventDescription, eventTitle;
    ImageView toggleShowHide;
    private ImageView listvbutton;
    DatabaseHelper databaseHelper;
    Prompt_Database_Helper promptDatabaseHelper;
    private Button datePickerBtn, timePickerBtn, make_note, stop_recording_animation, saveEventButton, import_button, hide_recording_animation,showalltranscription,showdescription;
    private static final int REQUEST_CODE_SPEECH_INPUT = 100;
    ImageView stop_recording_small_animation,play_pause_recording_small_animation;

    Button recordButton;
    private Spinner prompt_spinner;
    private RecordingUtils recordingUtils;
    public static RecordingViewModel recordingViewModel;
    public static recording_event_no recording_event_no;



    private boolean isRecordingCompleted = false;
    CardView recording_small_card;
    TextView textView_small_Timer;

    ////////////////////////////////////playing and pausing audio///////////////////////////

    private SharedPreferences sharedPreferences;

    /////////////////////////////////////////////////////
    private RecyclerView recyclerView;
    private AudioRecyclerAdapter recordingAdapter;
    private ArrayList<Recording> recordingList;


    //Requirements
    String prompt_message = "";
    public static int event_id;
    is_recording is_recording;
    //prompt

    String[] languages;
    ArrayAdapter<String> adapter;

    //draging icons
    private static final String PREFS_NAME = "CardViewPosition";
    private static final String PREF_START_MARGIN = "StartMargin";
    private static final String PREF_TOP_MARGIN = "TopMargin";

    private float dX, dY;
    private int lastAction;
    private SharedPreferences sharedPreferences2;
    private ConstraintLayout constraintLayout;
    private Spinner languageSpinner;
     private recording_language recordingLanguage;
    private current_event ce;
    /////tags work

    private RecyclerView tag_recycler;
    private TagAdapter tag_adapter;
    private List<Tag> tagList;
    private TagStorage tagStorage;

    AudioPlayerViewModel viewModel ;




    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_add_notes_, container, false);

        //check if have mic access
        viewModel = new ViewModelProvider(requireActivity()).get(AudioPlayerViewModel.class);
        //database helpers and other initializations

        logger= AppLogger.getInstance(getContext());
        logger.addLog("Add Notes Fragment : User Opened Add Notes Activity");
        recording_event_no=recording_event_no.getInstance();
        ce = new current_event();
        recordingLanguage=new recording_language();
        databaseHelper = new DatabaseHelper(getContext());
        promptDatabaseHelper=new Prompt_Database_Helper(getContext());
        is_recording=new is_recording();
        sharedPreferences = getActivity().getSharedPreferences("ApiKeysPref", MODE_PRIVATE);
        recordingViewModel = new ViewModelProvider(
                (ViewModelStoreOwner) requireActivity().getApplication(),
                ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().getApplication())
        ).get(RecordingViewModel.class);

        recordingUtils = RecordingManager.getInstance(
                requireContext(),this

        );
        recordingList = new ArrayList<>();
        sharedPreferences2 = getActivity().getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        //onbackpressed handler




        //all elements binding
        selectedDateTime = view.findViewById(R.id.selectedDateTime);
        eventDescription = view.findViewById(R.id.eventDescription);
        eventTitle = view.findViewById(R.id.eventTitle);
        no_item_text=view.findViewById(R.id.no_item_text);
        alltranscription = view.findViewById(R.id.alltranscription);
        saveEventButton = view.findViewById(R.id.saveEventButton);
        datePickerBtn = view.findViewById(R.id.datePickerBtn);
        timePickerBtn = view.findViewById(R.id.timePickerBtn);
        recordButton = view.findViewById(R.id.recordButton);
        listvbutton = view.findViewById(R.id.listvbutton);
        event_description_view = view.findViewById(R.id.summary_text);
        all_transcription_view = view.findViewById(R.id.all_transcriptions);
        recyclerView = view.findViewById(R.id.recordings_recycler);
        toolbar = view.findViewById(R.id.appBar);
        prompt_spinner = view.findViewById(R.id.prompt_spinner);
        import_button=view.findViewById(R.id.import_button);
        make_note=view.findViewById(R.id.make_note);
        toggleShowHide = view.findViewById(R.id.toggle_show_hide);
        recording_small_card=view.findViewById(R.id.recording_small_card);
        textView_small_Timer=view.findViewById(R.id.textView_small_Timer);
        stop_recording_small_animation=view.findViewById(R.id.stop_recording_small_animation);
        play_pause_recording_small_animation=view.findViewById(R.id.play_pause_recording_small_animation);
        constraintLayout = view.findViewById(R.id.constraint);
        showalltranscription=view.findViewById(R.id.showalltranscription);
        showdescription = view.findViewById(R.id.showdescription);



// Get the current texts (converted to lowercase for a case-insensitive check)
        String dateButtonText = datePickerBtn.getText().toString().toLowerCase();
        String timeButtonText = timePickerBtn.getText().toString().toLowerCase();

// Check if the datePickerBtn text contains "pick" (you can adjust the condition as needed)
        if (dateButtonText.contains("pick")) {
            // Get the current date
            Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH) + 1; // Note: Calendar.MONTH is 0-indexed
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            // Format the date string (adjust the format as you prefer)
            String currentDate = String.format(Locale.getDefault(), "%02d/%02d/%04d", day, month, year);

            // Set the text of the button to the current date
            datePickerBtn.setText(currentDate);
        }

// Check if the timePickerBtn text contains "pick"
        if (timeButtonText.contains("pick")) {
            // Get the current time
            Calendar calendar = Calendar.getInstance();
            int hour = calendar.get(Calendar.HOUR_OF_DAY);
            int minute = calendar.get(Calendar.MINUTE);

            // Format the time string (HH:mm format)
            String currentTime = String.format(Locale.getDefault(), "%02d:%02d", hour, minute);

            // Set the text of the button to the current time
            timePickerBtn.setText(currentTime);
        }

        //set default recording language
        String savedLanguage = sharedPreferences.getString(SELECTED_LANGUAGE, "English");  // Default value is null
        recordingLanguage.setRecording_language(savedLanguage);


        //show transcription and description on click
        showalltranscription.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getContext(), activity_text_display.class);
                intent.putExtra("text", alltranscription.getText().toString());
                intent.putExtra("Title",getString(R.string.all_transcriptions));
                logger.addLog("Add Notes Fragment : User Clicked on Show All transcriptions");
                startActivity(intent);
            }
        });
        showdescription.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getContext(), Show_all_ai_notes.class);
                intent.putExtra("text", String.valueOf(event_id));
                intent.putExtra("Title",getString(R.string.description));
                logger.addLog("Add Notes Fragment : User Clicked on Show AI generated Notes");
                startActivity(intent);
            }
        });

        // Set OnTouchListener for the CardView
        reloadPosition(recording_small_card);
        recording_small_card.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                logger.addLog("Add Notes Fragment : User Changed position of the small recording card");
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        dX = v.getX() - event.getRawX();
                        dY = v.getY() - event.getRawY();
                        lastAction = MotionEvent.ACTION_DOWN;
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        float newX = event.getRawX() + dX;
                        float newY = event.getRawY() + dY;

                        // Prevent the view from going off-screen
                        newX = Math.max(0, Math.min(newX, constraintLayout.getWidth() - v.getWidth()));
                        newY = Math.max(0, Math.min(newY, constraintLayout.getHeight() - v.getHeight()));

                        v.setX(newX);
                        v.setY(newY);
                        lastAction = MotionEvent.ACTION_MOVE;
                        return true;

                    case MotionEvent.ACTION_UP:
                        if (lastAction == MotionEvent.ACTION_MOVE) {
                            savePosition(recording_small_card); // Save the position when drag ends
                        }
                        return true;

                    default:
                        return false;
                }
            }
        });


        //delete all recordings if already exists
        int nextEventId = databaseHelper.getNextEventId();
        int maxrecording_event=databaseHelper.getMaxEventIdFromRecordings();
        if(nextEventId==maxrecording_event)
        {
            logger.addLog("Add Notes Fragment : Opened Page to add new event");
            databaseHelper.deleteAllRecordingsByEventId(nextEventId);
            deleteSelectionState(nextEventId);

        }

        //Recordings setup
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        //Loading data if opened from a note
        event_id = ce.getCurrent_event_no();
        if (event_id != -1) {
            load_data();
            logger.addLog("Add Notes Fragment : Loaded data of this page");

        }
        else{
            event_id=databaseHelper.getNextEventId();
            logger.addLog("Add Notes Fragment : set default recording language "+savedLanguage);
            recordingLanguage.setRecording_language(savedLanguage);

        }
        ////tag work
        tag_recycler = view.findViewById(R.id.tags_recycler);
        tagStorage = new TagStorage(getContext());
        tagList = loadTags(event_id);
        if (tagList.size() <= 0) {
            // Create and add two default tags
            logger.addLog("Add Notes Fragment : Added default tags of meeting and lecture");
            tagList.add(new Tag("Meeting|~|" + getRandomNumber(), false));
            tagList.add(new Tag("Lecture|~|" + getRandomNumber(), false));
            saveTags();
        }
        tag_recycler.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));

        tag_adapter = new TagAdapter(tagList, getContext(), this);
        tag_recycler.setAdapter(tag_adapter);

        //Setup all Prompts Spinner
        languages = promptDatabaseHelper.getAllPromptTexts();
        adapter = new ArrayAdapter<>(
                getActivity(),
                android.R.layout.simple_spinner_item,
                languages
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        prompt_spinner.setAdapter(adapter);
        if (languages == null || languages.length == 0) {
            no_item_text.setVisibility(View.VISIBLE);
        }
        else {
            no_item_text.setVisibility(View.GONE);
        }
        no_item_text.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getContext(), Manage_Prompt.class);
                startActivity(intent);
            }
        });
        prompt_spinner.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    if (languages == null || languages.length == 0) {
                        logger.addLog("Add Notes Fragment : No prompt present moving to manage prompt");
                        Intent intent = new Intent(getContext(), Manage_Prompt.class);
                        startActivity(intent);
                        return true;
                    }
                }
                return false;
            }
        });
        prompt_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Get the selected language
                logger.addLog("Add Notes Fragment : Selected a prompt message");
                prompt_message = languages[position];
                // Toast.makeText(Add_Event.this, "Selected Language: " + selectedLanguage, Toast.LENGTH_SHORT).show();


            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Handle case when no selection is made (optional)
            }
        });

        //Event Description and summary hide if fields empty
        Notes_Database_Helper notesDatabaseHelper=new Notes_Database_Helper(getContext());
        ArrayList<Note> notes = notesDatabaseHelper.getNotesByRecordingId(event_id);
        if(notes.size()<=0)
        {
            event_description_view.setVisibility(View.GONE);
            showdescription.setVisibility(View.GONE);
            logger.addLog("Add Notes Fragment : No AI generated notes present for this event");
        }
        if(alltranscription.getText().toString().contains("No items selected") || alltranscription.getText().toString().isEmpty())
        {
            all_transcription_view.setVisibility(View.GONE);
            showalltranscription.setVisibility(View.GONE);
            logger.addLog("Add Notes Fragment : No no transcription added  for this event");

        }

        //Event Description and Summary button event
        toggleShowHide.setOnClickListener(v -> {
            if (isExpanded) {
                eventDescription.setMaxLines(1);
                eventDescription.setEllipsize(android.text.TextUtils.TruncateAt.END);
                toggleShowHide.setImageResource(R.mipmap.expand); // Change to expand icon
                isExpanded = false;
            } else {
                // Expand: Show all lines
                if (eventDescription.getText().toString().length() > 200) { // Define your threshold
                    Intent intent = new Intent(getContext(), activity_text_display.class);
                    intent.putExtra("text", eventDescription.getText().toString());
                    intent.putExtra("Title",getString(R.string.description));

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
                    Intent intent = new Intent(getContext(), activity_text_display.class);
                    intent.putExtra("text", alltranscription.getText().toString());
                    intent.putExtra("Title",getString(R.string.all_transcriptions));

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

        //make notes button
        make_note.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String chatGptApi = sharedPreferences.getString(KEY_CHATGPT, "");
                String geminiApi = sharedPreferences.getString(KEY_GEMINI, "");
                logger.addLog("Add Notes Fragment : User Tried to make AI generateed notes");

                Notes_Database_Helper notesDatabaseHelper=new Notes_Database_Helper(getContext());
                String prompt="";// If the EditText is empty, load the saved prompt from SharedPreferences
                if (TextUtils.isEmpty(prompt)) {
                    prompt = promptDatabaseHelper.getPromptTextByName(prompt_message);

                    if (TextUtils.isEmpty(prompt)) {
                        logger.addLog("Add Notes Fragment : No prompt selected");

                        Toast.makeText(getContext(), "Prompt is required", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
                String selectedApi = sharedPreferences.getString("SelectedApi", "use Gemini Ai");
               // event_description_view.setVisibility(View.VISIBLE);
                showdescription.setVisibility(View.VISIBLE);

                switch (selectedApi) {
                    case "use ChatGpt":
                        logger.addLog("Add Notes Fragment : API Switched to Chatgpt");

                        getoutput_chatgpt(getContext(),prompt+":"+alltranscription.getText().toString(),new GeminiCallback() {
                            @Override
                            public void onSuccess(String result) {
                                notesDatabaseHelper.addNote(result,event_id, "ChatGpt");
                                logger.addLog("Add Notes Fragment : Gemini Note making successful");

                            }

                            @Override
                            public void onFailure(String error) {
                                notesDatabaseHelper.addNote(error,event_id,"ChatGpt");
                                logger.addLog("Add Notes Fragment : Unable to create note with the help of gemini error : "+error);

                            }
                        },chatGptApi);
                        //event_description_view.setVisibility(View.VISIBLE);
                        showdescription.setVisibility(View.VISIBLE);
                        break;
                    default:
                        logger.addLog("Add Notes Fragment : API Switched to Gemini");

                        get_gemini_note(getContext(),prompt+":"+alltranscription.getText().toString(),new GeminiCallback() {
                            @Override
                            public void onSuccess(String result) {
                                notesDatabaseHelper.addNote(result,event_id,"Gemini");
                                logger.addLog("Add Notes Fragment : Gemini Note making successful");

                            }

                            @Override
                            public void onFailure(String error) {
                                notesDatabaseHelper.addNote(error,event_id,"Gemini");
                                logger.addLog("Add Notes Fragment : Unable to create note with the help of gemini error : "+error);

                            }
                        },geminiApi);
                        //event_description_view.setVisibility(View.VISIBLE);
                        showdescription.setVisibility(View.VISIBLE);
                        break;
                }
            }
        });


        //import Button Setup
        import_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //openAudioPicker();
                new FilePicker.Builder((AppCompatActivity) getContext())
                        .setLimitItemSelection(1)
                        .setAccentColor(getResources().getColor(R.color.secondary))
                        .setCancellable(true)
                        .setFileType(FileType.AUDIO)
                        .setOnSubmitClickListener(files -> {
                            logger.addLog("Add Notes Fragment : File imported "+files.get(0).getFile().getAbsolutePath());
                            if (files != null && !files.isEmpty()) {
                                String selectedFilePath = files.get(0).getFile().getAbsolutePath(); // Get the file path
                                try {
                                    if(event_id!=-1)
                                    {
                                        logger.addLog("Add Notes Fragment : Saved imported audio to database");
                                        saveAudioToDatabase(event_id, selectedFilePath,savedLanguage);

                                    }

                                } catch (IOException e) {
                                    logger.addLog("Add Notes Fragment : error saving imported audio "+e.getMessage());

                                }
                            } else {
                                Toast.makeText(getContext(), "No file selected", Toast.LENGTH_SHORT).show();
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

        //if this is not the event for which we are recording audio then make all the buttons disable
        if(recording_event_no.getRecording_event_no()!=event_id && recording_event_no.getRecording_event_no()!=-1)
        {
            recordButton.setText(getString(R.string.recording_disabled));
            recordButton.setBackgroundColor(getContext().getResources().getColor(R.color.black));
            recordButton.setEnabled(false);
        }
        else {
            if ((Boolean.TRUE.equals(recordingViewModel.getIsRecording().getValue())||Boolean.TRUE.equals(recordingViewModel.getIsPaused().getValue())) && recording_event_no.getRecording_event_no()==event_id) {
                // Timer setup
                recordButton.setText(getString(R.string.stop_recording));
                recordButton.setBackgroundColor(getContext().getResources().getColor(R.color.nav));
                recording_event_no.setRecording_event_no(event_id);
                recording_small_card.setVisibility(View.VISIBLE);
                reloadPosition(recording_small_card);
            } else {
                // Stop recording
                recordButton.setText(getString(R.string.start_recording));
                recordButton.setBackgroundColor(getContext().getResources().getColor(R.color.secondary));
                recording_small_card.setVisibility(View.GONE);
            }

        }
        if(Boolean.TRUE.equals(recordingViewModel.getIsPaused().getValue()))
        {
            play_pause_recording_small_animation.setImageResource(R.mipmap.play); // Set the icon to 'pause'
        }
        else
        {
            play_pause_recording_small_animation.setImageResource(R.mipmap.pause); // Set the icon to 'pause'
        }


        //Recording button
        recordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!Boolean.TRUE.equals(recordingViewModel.getIsRecording().getValue())) {
                    // Timer setup
                    logger.addLog("Add Notes Fragment : Recording started");

                    recording_event_no.setRecording_event_no(event_id);
                    recordButton.setText(getString(R.string.stop_recording));
                    recordButton.setBackgroundColor(getContext().getResources().getColor(R.color.nav));
                    recordingViewModel.setPaused(false);     // Set the paused state to true
                    recordingUtils.startRecording();
                    recordingViewModel.setRecording(true);
                    recordingViewModel.startTimer();
                    recording_small_card.setVisibility(View.VISIBLE);
                    reloadPosition(recording_small_card);

                    Intent serviceIntent = new Intent(getContext(), RecordingService.class);
                    getContext().startService(serviceIntent);

                } else {
                    logger.addLog("Add Notes Fragment : Recording stopped");

                    recordButton.setText(getString(R.string.start_recording));
                    recordButton.setBackgroundColor(getContext().getResources().getColor(R.color.secondary));
                    recordingUtils.stopRecording(recording_event_no.getRecording_event_no());
                    recordingViewModel.setRecording(false);
                    recordingViewModel.setPaused(false); // Reset paused state
                    recordingViewModel.resetTimer();     // Stop and reset the timer
                    updateTimerText(0); // Reset the timer text to 0
                    recordingViewModel.updateElapsedSeconds(0); // Reset elapsed time in ViewModel
                    recording_small_card.setVisibility(View.GONE); // Hide the recording card
                    play_pause_recording_small_animation.setImageResource(R.mipmap.pause); // Set the icon to 'pause'
                    load_recycler(event_id);
                }
            }
        });

        //Updating Timer
        recordingViewModel.getElapsedSeconds().observe(getViewLifecycleOwner(), elapsedTime -> {
            // Update UI when elapsed time changes
            updateTimerText(elapsedTime);
        });
        recordingViewModel.getIsRecording().observe(getViewLifecycleOwner(), isrecording -> {
            if(isrecording)
            {
                recordingViewModel.getIsPaused().observe(getViewLifecycleOwner(), ispaused -> {
                    if(ispaused)
                    {

                        recording_small_card.setVisibility(View.VISIBLE);
                        play_pause_recording_small_animation.setImageResource(R.mipmap.play);
                        recordButton.setText(getString(R.string.stop_recording));
                        recordButton.setBackgroundColor(getContext().getResources().getColor(R.color.nav));
                    }
                    else
                    {
                        recording_small_card.setVisibility(View.VISIBLE);
                        play_pause_recording_small_animation.setImageResource(R.mipmap.pause);
                        recordButton.setText(getString(R.string.stop_recording));
                        recordButton.setBackgroundColor(getContext().getResources().getColor(R.color.nav));

                    }
                });
            }
            else
            {
                recordingViewModel.getIsPaused().observe(getViewLifecycleOwner(), ispaused -> {
                    if(ispaused)
                    {
                        recording_small_card.setVisibility(View.VISIBLE);
                        play_pause_recording_small_animation.setImageResource(R.mipmap.play);
                        recordButton.setText(getString(R.string.stop_recording));
                        recordButton.setBackgroundColor(getContext().getResources().getColor(R.color.nav));
                    }
                    else
                    {

                        recording_small_card.setVisibility(View.GONE);
                        play_pause_recording_small_animation.setImageResource(R.mipmap.pause);
                        recordingList.clear();
                        recordingList = databaseHelper.getRecordingsByEventId(event_id);
                        recordingAdapter = new AudioRecyclerAdapter(recordingList, getContext(), this,getViewLifecycleOwner(),viewModel);
                        updateSelectedItemsDisplay(new ArrayList<>());
                        recyclerView.setAdapter(recordingAdapter);
                        recordButton.setText(getString(R.string.start_recording));
                        recordButton.setBackgroundColor(getContext().getResources().getColor(R.color.secondary));
                        recordingAdapter.notifyDataSetChanged();

                    }
                });

            }
        });


        //Stop Recording via small Card
        play_pause_recording_small_animation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Boolean.TRUE.equals(recordingViewModel.getIsRecording().getValue())) {
                    logger.addLog("Add Notes Fragment : Recording paused");

                    // Pause recording
                    recordingUtils.pauseRecording();
                    recordingViewModel.setRecording(false);  // Set recording state to false
                    recordingViewModel.setPaused(true);     // Set the paused state to true
                    recordingViewModel.pauseTimer();        // Pause the timer
                    play_pause_recording_small_animation.setImageResource(R.mipmap.play);
                } else if (Boolean.TRUE.equals(recordingViewModel.getIsPaused().getValue())) {
                    logger.addLog("Add Notes Fragment : Recording Resumed");

                    // Resume recording
                    recordingUtils.resumeRecording();
                    recordingViewModel.setRecording(true);  // Set recording state to true
                    recordingViewModel.setPaused(false);   // Clear the paused state
                    recordingViewModel.resumeTimer();      // Resume the timer
                    play_pause_recording_small_animation.setImageResource(R.mipmap.pause);
                }
            }
        });
        stop_recording_small_animation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Stop recording entirely
                logger.addLog("Add Notes Fragment : Recording stopped");

                recordingUtils.stopRecording(recording_event_no.getRecording_event_no());
                recordingViewModel.setRecording(false);
                recordingViewModel.setPaused(false); // Reset paused state
                recordingViewModel.resetTimer();     // Stop and reset the timer
                updateTimerText(0); // Reset the timer text to 0
                recordingViewModel.updateElapsedSeconds(0); // Reset elapsed time in ViewModel
                recording_small_card.setVisibility(View.GONE); // Hide the recording card
                play_pause_recording_small_animation.setImageResource(R.mipmap.pause); // Set the icon to 'pause'
                recordButton.setText(getString(R.string.start_recording));
                recordButton.setBackgroundColor(getContext().getResources().getColor(R.color.secondary));
                load_recycler(event_id);

            }
        });

        //save recording
        saveEventButton.setOnClickListener(view2 -> {
            if(event_id==ce.getCurrent_event_no())
            {
                logger.addLog("Add Notes Fragment : Trying to update Event");

                String title = eventTitle.getText().toString();
                String eventdescription = eventDescription.getText().toString();
                String selectedDate = datePickerBtn.getText().toString();
                String selectedTime = timePickerBtn.getText().toString();
                updateEventData(event_id,title, eventdescription, selectedDate, selectedTime);

            }
            else
            {
                isRecordingCompleted=is_recording.getIs_Recording();
                if (!isRecordingCompleted) {
                    logger.addLog("Add Notes Fragment : Trying to save event");
                    String title = eventTitle.getText().toString();
                    String eventdescription = eventDescription.getText().toString();
                    String selectedDate = datePickerBtn.getText().toString();
                    String selectedTime = timePickerBtn.getText().toString();
                    saveEventData(title, eventdescription, selectedDate, selectedTime,event_id);
                } else {
                    Toast.makeText(getContext(), "Please complete the recording first", Toast.LENGTH_SHORT).show();
                }
            }

        });

        //date picker time picker and current date and time
        selectedDateTime.setText(getString(R.string.current_date_time) + current_date_and_time());
        datePickerBtn.setOnClickListener(view2 -> showDatePicker());
        timePickerBtn.setOnClickListener(view2 -> showTimePicker());


        return view;
    }

    @Override
    public void onRecordingStarted(String filePath) {
        recordingViewModel.setRecordingFilePath(filePath);
    }
    @Override
    public void onResume() {
        if(recording_event_no.getRecording_event_no()!=event_id && recording_event_no.getRecording_event_no()!=-1)
        {

            recordButton.setText(getString(R.string.recording_disabled));
            recordButton.setBackgroundColor(getContext().getResources().getColor(R.color.black));
            recordButton.setEnabled(false);
        }
        else {
            recordingViewModel.getIsRecording().observe(getViewLifecycleOwner(), isrecording -> {
                if(isrecording)
                {
                    recordingViewModel.getIsPaused().observe(getViewLifecycleOwner(), ispaused -> {
                        if(ispaused)
                        {

                            recording_small_card.setVisibility(View.VISIBLE);
                            play_pause_recording_small_animation.setImageResource(R.mipmap.play);
                            recordButton.setText(getString(R.string.stop_recording));
                            recordButton.setBackgroundColor(getContext().getResources().getColor(R.color.nav));
                        }
                        else
                        {
                            recording_small_card.setVisibility(View.VISIBLE);
                            play_pause_recording_small_animation.setImageResource(R.mipmap.pause);
                            recordButton.setText(getString(R.string.stop_recording));
                            recordButton.setBackgroundColor(getContext().getResources().getColor(R.color.nav));

                        }
                    });
                }
                else
                {
                    recordingViewModel.getIsPaused().observe(getViewLifecycleOwner(), ispaused -> {
                        if(ispaused)
                        {
                            recording_small_card.setVisibility(View.VISIBLE);
                            play_pause_recording_small_animation.setImageResource(R.mipmap.play);
                            recordButton.setText(getString(R.string.stop_recording));
                            recordButton.setBackgroundColor(getContext().getResources().getColor(R.color.nav));
                        }
                        else
                        {
                            viewModel= new ViewModelProvider(requireActivity()).get(AudioPlayerViewModel.class);

                            recording_small_card.setVisibility(View.GONE);
                            play_pause_recording_small_animation.setImageResource(R.mipmap.pause);
                            recordingList.clear();
                            recordingList = databaseHelper.getRecordingsByEventId(event_id);
                            recordingAdapter = new AudioRecyclerAdapter(recordingList, getContext(), this,getViewLifecycleOwner(),viewModel);
                            updateSelectedItemsDisplay(new ArrayList<>());
                            recyclerView.setAdapter(recordingAdapter);
                            recordButton.setText(getString(R.string.start_recording));
                            recordButton.setBackgroundColor(getContext().getResources().getColor(R.color.secondary));
                            recordingAdapter.notifyDataSetChanged();

                        }
                    });

                }
            });


        }


        recordingList.clear();
        recordingList.addAll(databaseHelper.getRecordingsByEventId(event_id));

        if (recordingAdapter == null) {
            recordingAdapter = new AudioRecyclerAdapter(recordingList, requireContext(), this,getViewLifecycleOwner(),viewModel);
            recyclerView.setAdapter(recordingAdapter);
        } else {
            recordingAdapter.notifyDataSetChanged();
        }
        languages = promptDatabaseHelper.getAllPromptTexts();
        adapter = new ArrayAdapter<>(
                getActivity(),
                android.R.layout.simple_spinner_item,
                languages
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        prompt_spinner.setAdapter(adapter);
        if (languages == null || languages.length == 0) {
            no_item_text.setVisibility(View.VISIBLE);
        }
        else {
            no_item_text.setVisibility(View.GONE);
        }
        super.onResume();
    }
    private void savePosition(CardView cardView) {
        ConstraintLayout.LayoutParams layoutParams = (ConstraintLayout.LayoutParams) cardView.getLayoutParams();
        int startMargin = layoutParams.leftMargin;
        int topMargin = layoutParams.topMargin;

        SharedPreferences.Editor editor = sharedPreferences2.edit();
        editor.putInt(PREF_START_MARGIN, startMargin);
        editor.putInt(PREF_TOP_MARGIN, topMargin);
        editor.apply();
    }
    private void reloadPosition(CardView cardView) {
        int startMargin = sharedPreferences2.getInt(PREF_START_MARGIN, -1);
        int topMargin = sharedPreferences2.getInt(PREF_TOP_MARGIN, -1);

        // If a position was saved, apply it
        if (startMargin != -1 && topMargin != -1) {
            ConstraintSet constraintSet = new ConstraintSet();
            constraintSet.clone(constraintLayout);

            constraintSet.connect(cardView.getId(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, startMargin);
            constraintSet.connect(cardView.getId(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, topMargin);

            constraintSet.applyTo(constraintLayout);
        }
    }
    @Override
    public void onRecordingSaved(int eventId) {
        load_recycler(eventId);
    }

    @Override
    public void onRecordingPaused() {

    }

    @Override
    public void onRecordingResumed() {

    }

    private  void load_recycler(int event_id)
    {
        try {
            recordingViewModel.updateElapsedSeconds(0);
            recordingList.clear();
            recordingList = databaseHelper.getRecordingsByEventId(event_id);
            if(getContext() !=null)
            {
                recordingAdapter = new AudioRecyclerAdapter(recordingList,getActivity(), this,getViewLifecycleOwner(),viewModel);

            }
            else
            {
                // Toast.makeText(getContext(), "null Activity", Toast.LENGTH_SHORT).show();
            }
            updateSelectedItemsDisplay(new ArrayList<>());
            recyclerView.setAdapter(recordingAdapter);
            recordingAdapter.notifyDataSetChanged();
        } catch (RuntimeException e) {

        }

    }
    private void updateTimerText(int elapsedTime) {
        // Format the time and set it to the TextView
        int minutes = elapsedTime / 60;
        int seconds = elapsedTime % 60;
        String formattedTime = String.format("%02d:%02d", minutes, seconds);
        textView_small_Timer.setText(formattedTime);

    }

    private String current_date_and_time(){
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss", Locale.getDefault());
        String currentDateTime = dateFormat.format(new Date());

        return currentDateTime;
    }
    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        new android.app.DatePickerDialog(getContext(), (view, year, month, dayOfMonth) -> {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

            // Update the button text to the selected date
            datePickerBtn.setText(dayOfMonth + "/" + (month + 1) + "/" + year);
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }
    private void showTimePicker() {
        Calendar calendar = Calendar.getInstance();
        new android.app.TimePickerDialog(getContext(), (view, hourOfDay, minute) -> {
            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
            calendar.set(Calendar.MINUTE, minute);

            // Update the button text to the selected time
            timePickerBtn.setText(String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute));

            // Update the date and time display
        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show();
    }
    private void load_data() {
        DatabaseHelper db = new DatabaseHelper(getContext());
        Event event = db.getEventById(event_id);

        if (event != null) {
            toolbar.setTitle(getString(R.string.notes_details));
            saveEventButton.setText("Update Event");

            eventTitle.setText(event.getTitle());
            eventDescription.setText(event.getDescription());
            selectedDateTime.setText(getString(R.string.created_on) + event.getCreationDate() + " at  " + event.getCreationTime());
            datePickerBtn.setText(event.getEventDate());
            timePickerBtn.setText(event.getEventTime());
            recordingList.clear();
            recordingList = db.getRecordingsByEventId(event_id);
            recordingAdapter = new AudioRecyclerAdapter(recordingList, getContext(), this,getViewLifecycleOwner(),viewModel);
            updateSelectedItemsDisplay(new ArrayList<>());
            recyclerView.setAdapter(recordingAdapter);
            recordingAdapter.notifyDataSetChanged();
            logger.addLog("Add Notes Fragment : Loaded data for event");

        } else {
            Toast.makeText(getContext(), "Event not found", Toast.LENGTH_SHORT).show();
            logger.addLog("Add Notes Fragment : Event not found");
            if (getActivity() instanceof Show_Add_notes_Activity) {
                ((Show_Add_notes_Activity) getActivity()).setFragment(new Show_Notes_Fragment());
            }
        }
    }
    private void saveAudioToDatabase(int eventId_c, String audioPath,String language) throws IOException {
        DatabaseHelper databaseHelper = new DatabaseHelper(getContext());
        String name = getFileName(audioPath);         // Extract file name from the path
        String format = getFileExtension(audioPath);  // Extract file extension as format
        String length = getAudioDuration(audioPath);  // Get audio duration in seconds
        boolean isRecorded = false;                   // Set to false since it's imported
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yy 'at' HH:mm");
        String description ="";
        // Get the current date and time
        Date date = new Date();
        String selectedModel = sharedPreferences.getString(SELECTED_TRANSCRIPTION_METHOD, "Local"); // Default to "Use ChatGPT"


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
                "no",
                description,
                "no",
                language

        );


        if (isInserted) {
            AudioRecyclerAdapter.saveSelectionState(event_id);
            logger.addLog("Add Notes Fragment : Audio saved to database");
            Toast.makeText(getContext(), "Audio saved to database", Toast.LENGTH_SHORT).show();
            recordingList.clear();
            recordingList=databaseHelper.getRecordingsByEventId(event_id);
            recordingAdapter = new AudioRecyclerAdapter(recordingList,getContext() ,this,getViewLifecycleOwner(),viewModel);
            recyclerView.setAdapter(recordingAdapter);
            recordingAdapter.notifyDataSetChanged();
        } else {
            logger.addLog("Add Notes Fragment : Failed to save audio");
            Toast.makeText(getContext(), "Failed to save audio", Toast.LENGTH_SHORT).show();
        }
    }
    public int getRandomNumber5() {
        Random random = new Random();
        return random.nextInt(5); // Generates a random number between 1 and 6
    }
    private void updateEventData(int eventId, String title, String eventDescription, String selectedDate, String selectedTime) {
        if (title == null || title.trim().isEmpty()) {
            Toast.makeText(getContext(), "Event title cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        if (eventDescription == null || eventDescription.trim().isEmpty()) {
            eventDescription= String.valueOf(getRandomNumber5());
        }

        if (selectedDate == null || selectedDate.trim().isEmpty() || selectedDate.contains("Pick")) {
            Toast.makeText(getContext(), "Event date cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedTime == null || selectedTime.trim().isEmpty() || selectedTime.contains("Pick")) {
            Toast.makeText(getContext(), "Event time cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

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
                "audioFilePath"
        );

        // Show success or failure message
        if (isUpdated) {
            try {
                logger.addLog("Add Notes Fragment : Event updated successfully");
                if (getActivity() instanceof Show_Add_notes_Activity) {
                    ((Show_Add_notes_Activity) getActivity()).setFragment(new Show_Notes_Fragment());
                }
            } catch (Exception e) {
                logger.addLog("Add Notes Fragment : Event error updating :"+ e.getMessage());

                Toast.makeText(getContext(),"error updating :"+ e.getMessage(), Toast.LENGTH_SHORT).show();
            }

            Toast.makeText(getContext(), "Note updated successfully!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), "Failed to update event", Toast.LENGTH_SHORT).show();
        }
    }
    private void saveEventData(String title, String eventDescription, String selectedDate, String selectedTime,int eventId_m) {
        if (title == null || title.trim().isEmpty()) {
            Toast.makeText(getContext(), "Event title cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        if (eventDescription == null || eventDescription.trim().isEmpty()) {
            eventDescription=String.valueOf(getRandomNumber5());
        }

        if (selectedDate == null || selectedDate.trim().isEmpty()||selectedDate.contains("Pick")) {
            Toast.makeText(getContext(), "Event date cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedTime == null || selectedTime.trim().isEmpty()||selectedTime.contains("Pick")) {
            Toast.makeText(getContext(), "Event time cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

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
                "audioFilePath"
        );

        if (isInserted) {
            try {
                if (getActivity() instanceof Show_Add_notes_Activity) {
                    ((Show_Add_notes_Activity) getActivity()).setFragment(new Show_Notes_Fragment());
                }

            }
            catch (Exception e)
            {
                Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
            }

            Toast.makeText(getContext(), "Event saved successfully!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), "Failed to save event", Toast.LENGTH_SHORT).show();
        }
    }
    private void saveEventData_fragment_d(String title, String eventDescription, String selectedDate, String selectedTime,int eventId_m) {
        logger.addLog("Add Notes Fragment : Event auto save initiated");

        if (title == null || title.trim().isEmpty()) {
           title="not available";
        }

        if (eventDescription == null || eventDescription.trim().isEmpty()) {
            eventDescription=String.valueOf(getRandomNumber5());
        }

        if (selectedDate == null || selectedDate.trim().isEmpty()||selectedDate.contains("Pick")) {
            selectedDate="--";
        }
        if (selectedTime == null || selectedTime.trim().isEmpty()||selectedTime.contains("Pick")) {
           selectedTime="--";
        }

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
                "audioFilePath"
        );

        if (isInserted) {
            try {
                logger.addLog("Add Notes Fragment : Event auto save successful");
                if (getActivity() instanceof Show_Add_notes_Activity) {
                    ((Show_Add_notes_Activity) getActivity()).setFragment(new Show_Notes_Fragment());
                }

            }
            catch (Exception e)
            {
                logger.addLog("Add Notes Fragment : Event auto save failed");
                Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
            }

            //Toast.makeText(getContext(), "Event saved successfully!", Toast.LENGTH_SHORT).show();
        } else {
            //Toast.makeText(getContext(), "Failed to save event", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateSelectedItemsDisplay(ArrayList<String> selectedItems) {
        if (selectedItems.isEmpty()) {
            alltranscription.setText("No items selected");
            all_transcription_view.setVisibility(View.GONE);
            showalltranscription.setVisibility(View.GONE);
            make_note.setVisibility(View.GONE);

        } else {
            String  des= TextUtils.join(", ", selectedItems);
            if(des.equals(""))
            {
                all_transcription_view.setVisibility(View.GONE);
                showalltranscription.setVisibility(View.GONE);
                make_note.setVisibility(View.GONE);

            }
            else
            {
                alltranscription.setText(TextUtils.join(", ", selectedItems));
                //all_transcription_view.setVisibility(View.VISIBLE);
                showalltranscription.setVisibility(View.VISIBLE);
                make_note.setVisibility(View.VISIBLE);
            }



        }
    }
    public void deleteSelectionState(int id) {
        // Get the key corresponding to the given ID
        String key = "selected_items_" + id;
        SharedPreferences sharedPreferences=getActivity().getSharedPreferences("PromptSelectionPrefs", MODE_PRIVATE);
        // Check if the key exists before attempting to delete
        if (sharedPreferences.contains(key)) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.remove(key); // Remove the specific selection state
            editor.apply(); // Save changes
        }
    }

    @Override
    public void onSelectionChanged(ArrayList<String> updatedSelection) {
        updateSelectedItemsDisplay(updatedSelection);

    }
    private boolean shouldInterceptBackPress() {
        Toast.makeText(getContext(), "usaved changes", Toast.LENGTH_SHORT).show();
        return true; // Replace with actual condition
    }




    @Override
    public boolean onBackPressed() {
        usd();

        return true;
    }
    private void usd() {
        if(event_id==ce.getCurrent_event_no())
        {
            ((Show_Add_notes_Activity) getActivity()).setFragment(new Show_Notes_Fragment());



        }
        else
        {
            boolean isRecordingCompleted = is_recording.getIs_Recording();
            boolean hasUnsavedChanges = !eventTitle.getText().toString().isEmpty();

            if (hasUnsavedChanges || isRecordingCompleted || !recordingList.isEmpty()) {
                recordingViewModel.getIsRecording().observe(getViewLifecycleOwner(), isrecording -> {
                    if(isrecording)
                    {
                        String title = eventTitle.getText().toString();
                        String eventDescriptiont = eventDescription.getText().toString();
                        String selectedDate = datePickerBtn.getText().toString();
                        String selectedTime = timePickerBtn.getText().toString();
                        saveEventData_fragment_d(title, eventDescriptiont, selectedDate, selectedTime, event_id);

                    }
                    else
                    {
                        String title = eventTitle.getText().toString();
                        String eventDescriptiont = eventDescription.getText().toString();
                        String selectedDate = datePickerBtn.getText().toString();
                        String selectedTime = timePickerBtn.getText().toString();
                        saveEventData_fragment_d(title, eventDescriptiont, selectedDate, selectedTime, event_id);



                    }
                });
            } else {

                ((Show_Add_notes_Activity) getActivity()).setFragment(new Show_Notes_Fragment());
            }
        }


    }
    private void showRecordingInProgressDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.recording_in_progress))
                .setMessage(getString(R.string.recording_save_prompt))
                .setPositiveButton(getString(R.string.save), (dialog, which) -> handleSaveRecording())
                .setNegativeButton(getString(R.string.discard), (dialog, which) ->
                        {
                            handleStopRecording();
                            resetRecordingState();
                            discardChangesAndNavigateBack();
                        }

                        )
                .setNeutralButton(getString(R.string.cancel), (dialog, which) -> dialog.dismiss())
                .create()
                .show();
    }

    /**
     * Show a dialog for unsaved changes.
     */
    private void showUnsavedChangesDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.unsaved_changes))
                .setMessage(getString(R.string.unsaved_changes_prompt))
                .setPositiveButton(getString(R.string.save), (dialog, which) -> handleSaveChanges())
                .setNegativeButton(getString(R.string.discard), (dialog, which) -> discardChangesAndNavigateBack())
                .setNeutralButton(getString(R.string.cancel), (dialog, which) -> dialog.dismiss())
                .create()
                .show();
    }

    /**
     * Handle saving the recording and associated data.
     */
    private void handleSaveRecording() {
        recordingUtils.stopRecording(recording_event_no.getRecording_event_no());
        resetRecordingState();
        if (!is_recording.getIs_Recording()) {
            saveEventDetails();
        } else {
            Toast.makeText(getContext(), "Please complete the recording first", Toast.LENGTH_SHORT).show();
        }
    }
    private void handleStopRecording() {
        recordingUtils.stopRecording(recording_event_no.getRecording_event_no());

    }

    /**
     * Handle saving unsaved changes.
     */
    private void handleSaveChanges() {
        if (!is_recording.getIs_Recording()) {
            saveEventDetails();
        } else {
            Toast.makeText(getContext(), "Please complete the recording first", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Save event details to the database.
     */
    private void saveEventDetails() {
        logger.addLog("Add Notes Fragment : Event  saved successful");
        String title = eventTitle.getText().toString();
        String eventDescriptiont = eventDescription.getText().toString();
        String selectedDate = datePickerBtn.getText().toString();
        String selectedTime = timePickerBtn.getText().toString();
        saveEventData(title, eventDescriptiont, selectedDate, selectedTime, event_id);
    }

    @Override
    public void onPause() {
        recordingViewModel.getIsRecording().observe(getViewLifecycleOwner(), isrecording -> {
            if(isrecording)
            {
                String title = eventTitle.getText().toString();
                String eventDescriptiont = eventDescription.getText().toString();
                String selectedDate = datePickerBtn.getText().toString();
                String selectedTime = timePickerBtn.getText().toString();
                saveEventData_fragment_d(title, eventDescriptiont, selectedDate, selectedTime, event_id);
            }
            else
            {


            }
        });


        super.onPause();
    }

    /**
     * Discard changes and navigate back to the previous fragment.
     */
    private void discardChangesAndNavigateBack() {
        databaseHelper.deleteAllRecordingsByEventId(event_id);
        ((Show_Add_notes_Activity) getActivity()).setFragment(new Show_Notes_Fragment());
    }

    /**
     * Reset the recording state.
     */
    private void resetRecordingState() {
        recordingViewModel.setRecording(false);
        recordingViewModel.setPaused(false);
        recordingViewModel.resetTimer();
        updateTimerText(0);
        recordingViewModel.updateElapsedSeconds(0);
        recording_small_card.setVisibility(View.GONE);
        play_pause_recording_small_animation.setImageResource(R.mipmap.pause);
        recordButton.setText(getString(R.string.start_recording));
        recordButton.setBackgroundColor(getContext().getResources().getColor(R.color.secondary));
    }
    private List<Tag> loadTags(Integer event_id) {
        List<String> availableTags = tagStorage.getTags(String.valueOf(event_id)) != null
                ? tagStorage.getTags(String.valueOf(event_id)).get("available_tags")
                : TagUtils.DEFAULT_TAGS;

        List<String> selectedTags = tagStorage.getTags(String.valueOf(event_id)) != null
                ? tagStorage.getTags(String.valueOf(event_id)).get("selected_tags")
                : new ArrayList<>();

        List<Tag> tags = new ArrayList<>();
        for (String tag : availableTags) {
            tags.add(new Tag(tag, selectedTags.contains(tag)));
        }
        return tags;
    }

    @Override
    public void onTagSelected(int position, boolean isSelected) {
        tagList.get(position).setSelected(isSelected);
        saveTags();
    }

    @Override
    public void onAddNewTag() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Add New Tag");

        final EditText input = new EditText(getContext());
        builder.setView(input);

        builder.setPositiveButton("Add", (dialog, which) -> {
            String newTag = input.getText().toString();
            if (!newTag.isEmpty()) {
                tagList.add(new Tag(newTag+"|~|"+getRandomNumber(), false));
                saveTags();
                adapter.notifyDataSetChanged();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }
    public int getRandomNumber() {
        Random random = new Random();
        return random.nextInt(6) ; // Generates a random number between 1 and 6
    }
    private void saveTags() {
        List<String> availableTags = new ArrayList<>();
        List<String> selectedTags = new ArrayList<>();

        for (Tag tag : tagList) {
            availableTags.add(tag.getName());
            if (tag.isSelected()) {
                selectedTags.add(tag.getName());
            }
        }

        tagStorage.saveTags(String.valueOf(event_id), availableTags, selectedTags);
    }
}

