package com.example.recorderchunks.Background_Allow;

import static android.content.Context.MODE_PRIVATE;
import static com.example.recorderchunks.AI_Transcription.AI_Notemaking.get_gemini_note;
import static com.example.recorderchunks.AI_Transcription.AI_Notemaking.getoutput_chatgpt;
import static com.example.recorderchunks.utils.AudioUtils.getAudioDuration;
import static com.example.recorderchunks.utils.AudioUtils.getFileExtension;
import static com.example.recorderchunks.utils.AudioUtils.getFileName;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;

import androidx.annotation.NonNull;
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

import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
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

import com.android.volley.RequestQueue;
import com.example.recorderchunks.AI_Transcription.GeminiCallback;
import com.example.recorderchunks.Activity.RecordingService;
import com.example.recorderchunks.Adapter.AudioRecyclerAdapter;
import com.example.recorderchunks.Add_Event;
import com.example.recorderchunks.DatabaseHelper;
import com.example.recorderchunks.MainActivity;
import com.example.recorderchunks.Manage_Prompt;
import com.example.recorderchunks.Model_Class.Event;
import com.example.recorderchunks.Model_Class.Recording;
import com.example.recorderchunks.Model_Class.RecordingViewModel;
import com.example.recorderchunks.Model_Class.SharedViewModel;
import com.example.recorderchunks.Model_Class.current_event;
import com.example.recorderchunks.Model_Class.is_recording;
import com.example.recorderchunks.Model_Class.recording_event_no;
import com.example.recorderchunks.Prompt_Database_Helper;
import com.example.recorderchunks.R;
import com.example.recorderchunks.activity_text_display;
import com.example.recorderchunks.utils.RecordingManager;
import com.example.recorderchunks.utils.RecordingUtils;
import com.github.file_picker.FilePicker;
import com.github.file_picker.FileType;

import org.vosk.Model;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;


public class Add_notes_Fragment extends Fragment implements AudioRecyclerAdapter.OnSelectionChangedListener,RecordingUtils.RecordingCallback {
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
    private Button datePickerBtn, timePickerBtn, make_note, stop_recording_animation, saveEventButton, import_button, hide_recording_animation;
    private static final int REQUEST_CODE_SPEECH_INPUT = 100;
    ImageView stop_recording_small_animation,play_pause_recording_small_animation;

    Button recordButton;
    private Spinner prompt_spinner;
    private RecordingUtils recordingUtils;
    public static RecordingViewModel recordingViewModel;
    private recording_event_no recording_event_no;



    private boolean isRecordingCompleted = false;
    CardView recording_small_card;
    TextView textView_small_Timer;

    ////////////////////////////////////playing and pausing audio///////////////////////////
    private CardView recording_animation_card;

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


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_add_notes_, container, false);

        //check if have mic access

        //database helpers and other initializations
        recording_event_no=new recording_event_no();
        current_event ce = new current_event();
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
//        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);



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
        recording_animation_card=view.findViewById(R.id.recording_card);
        recording_small_card=view.findViewById(R.id.recording_small_card);
        textView_small_Timer=view.findViewById(R.id.textView_small_Timer);
        stop_recording_small_animation=view.findViewById(R.id.stop_recording_small_animation);
        play_pause_recording_small_animation=view.findViewById(R.id.play_pause_recording_small_animation);
        constraintLayout = view.findViewById(R.id.constraint);



        // Set OnTouchListener for the CardView
        reloadPosition(recording_small_card);
        recording_small_card.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
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
            databaseHelper.deleteAllRecordingsByEventId(nextEventId);


        }

        //Recordings setup
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        //Loading data if opened from a note
        event_id = ce.getCurrent_event_no();
        if (event_id != -1) {
            load_data();
        }
        else{
            event_id=databaseHelper.getNextEventId();
        }

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
                prompt_message = languages[position];
                // Toast.makeText(Add_Event.this, "Selected Language: " + selectedLanguage, Toast.LENGTH_SHORT).show();


            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Handle case when no selection is made (optional)
            }
        });

        //Event Description and summary hide if fields empty
        if(eventDescription.getText().toString().isEmpty())
        {
            event_description_view.setVisibility(View.GONE);
        }
        if(alltranscription.getText().toString().contains("No items selected") || alltranscription.getText().toString().isEmpty())
        {
            all_transcription_view.setVisibility(View.GONE);
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

        //make notes button
        make_note.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String prompt="";// If the EditText is empty, load the saved prompt from SharedPreferences
                if (TextUtils.isEmpty(prompt)) {
                    prompt = prompt_message;

                    if (TextUtils.isEmpty(prompt)) {
                        //   Toast.makeText(this, , Toast.LENGTH_SHORT).show();
                        Toast.makeText(getContext(), "Prompt is required", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
                String selectedApi = sharedPreferences.getString("SelectedApi", "use Gemini Ai");
                event_description_view.setVisibility(View.VISIBLE);

                switch (selectedApi) {
                    case "use ChatGpt":
                        eventDescription.setText(getoutput_chatgpt(prompt+":"+alltranscription.getText().toString(),getContext()));
                        event_description_view.setVisibility(View.VISIBLE);
                        break;
                    default:
                        get_gemini_note(getContext(),prompt+":"+alltranscription.getText().toString(),new GeminiCallback() {
                            @Override
                            public void onSuccess(String result) {
                                eventDescription.setText(result);
                            }

                            @Override
                            public void onFailure(String error) {
                                eventDescription.setText(error);

                            }
                        });
                        event_description_view.setVisibility(View.VISIBLE);
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
                            if (files != null && !files.isEmpty()) {
                                String selectedFilePath = files.get(0).getFile().getAbsolutePath(); // Get the file path
                                try {
                                    if(event_id!=-1)
                                    {
                                        saveAudioToDatabase(event_id, selectedFilePath);

                                    }

                                } catch (IOException e) {
                                    throw new RuntimeException(e);
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
            recordButton.setText("Recording Disabled");
            recordButton.setBackgroundColor(getContext().getResources().getColor(R.color.black));
            recordButton.setEnabled(false);
        }
        else {
            if ((Boolean.TRUE.equals(recordingViewModel.getIsRecording().getValue())||Boolean.TRUE.equals(recordingViewModel.getIsPaused().getValue())) && recording_event_no.getRecording_event_no()==event_id) {
                // Timer setup
                recordButton.setText("Stop Recording");
                recordButton.setBackgroundColor(getContext().getResources().getColor(R.color.nav));
                recording_event_no.setRecording_event_no(event_id);
                recording_small_card.setVisibility(View.VISIBLE);
                reloadPosition(recording_small_card);
            } else {
                // Stop recording
                recordButton.setText("Start Recording");
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
                    recording_event_no.setRecording_event_no(event_id);
                    recordButton.setText("Stop Recording");
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
                    recordButton.setText("Start Recording");
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
                    }
                    else
                    {
                        recording_small_card.setVisibility(View.VISIBLE);
                        play_pause_recording_small_animation.setImageResource(R.mipmap.pause);

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
                    }
                    else
                    {
                        recording_small_card.setVisibility(View.GONE);
                        play_pause_recording_small_animation.setImageResource(R.mipmap.pause);
                        recordingList.clear();
                        recordingList = databaseHelper.getRecordingsByEventId(event_id);
                        recordingAdapter = new AudioRecyclerAdapter(recordingList, getContext(), this);
                        updateSelectedItemsDisplay(new ArrayList<>());
                        recyclerView.setAdapter(recordingAdapter);
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
                    // Pause recording
                    recordingUtils.pauseRecording();
                    recordingViewModel.setRecording(false);  // Set recording state to false
                    recordingViewModel.setPaused(true);     // Set the paused state to true
                    recordingViewModel.pauseTimer();        // Pause the timer
                    play_pause_recording_small_animation.setImageResource(R.mipmap.play);
                } else if (Boolean.TRUE.equals(recordingViewModel.getIsPaused().getValue())) {
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
                recordingUtils.stopRecording(recording_event_no.getRecording_event_no());
                recordingViewModel.setRecording(false);
                recordingViewModel.setPaused(false); // Reset paused state
                recordingViewModel.resetTimer();     // Stop and reset the timer
                updateTimerText(0); // Reset the timer text to 0
                recordingViewModel.updateElapsedSeconds(0); // Reset elapsed time in ViewModel
                recording_small_card.setVisibility(View.GONE); // Hide the recording card
                play_pause_recording_small_animation.setImageResource(R.mipmap.pause); // Set the icon to 'pause'
                recordButton.setText("Start Recording");
                recordButton.setBackgroundColor(getContext().getResources().getColor(R.color.secondary));
                load_recycler(event_id);

            }
        });

        //save recording
        saveEventButton.setOnClickListener(view2 -> {
            if(event_id==ce.getCurrent_event_no())
            {
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
        selectedDateTime.setText("Current Date and Time: " + current_date_and_time());
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
        super.onResume();

        recordingList.clear();
        recordingList.addAll(databaseHelper.getRecordingsByEventId(event_id));

        if (recordingAdapter == null) {
            recordingAdapter = new AudioRecyclerAdapter(recordingList, requireContext(), this);
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
        recordingViewModel.updateElapsedSeconds(0);
        recordingList.clear();
        recordingList = databaseHelper.getRecordingsByEventId(event_id);
        if(getContext() !=null)
        {
            recordingAdapter = new AudioRecyclerAdapter(recordingList,getActivity(), this);

        }
        else
        {
            // Toast.makeText(getContext(), "null Activity", Toast.LENGTH_SHORT).show();
        }
        updateSelectedItemsDisplay(new ArrayList<>());
        recyclerView.setAdapter(recordingAdapter);
        recordingAdapter.notifyDataSetChanged();
    }
    private void updateTimerText(int elapsedTime) {
        // Format the time and set it to the TextView
        int minutes = elapsedTime / 60;
        int seconds = elapsedTime % 60;
        String formattedTime = String.format("%02d:%02d", minutes, seconds);
        textView_small_Timer.setText(formattedTime);

    }
    private void startTimer() {


        // Get the current elapsed time if already recorded
        int initialSeconds = recordingViewModel.getElapsedSeconds().getValue() != null
                ? recordingViewModel.getElapsedSeconds().getValue()
                : 0;

        // Start a new thread for updating the timer
        new Thread(() -> {
            int seconds = initialSeconds; // Start from the last elapsed time
            while (Boolean.TRUE.equals(recordingViewModel.getIsRecording().getValue())) {
                try {
                    Thread.sleep(1000); // Wait for 1 second
                    seconds++;
                    recordingViewModel.updateElapsedSeconds(seconds); // Update elapsed time
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }).start();
    }

    private void stopTimer() {
        recording_small_card.setVisibility(View.GONE);

        // Stop the timer by resetting the elapsed time
        recordingViewModel.updateElapsedSeconds(0);
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
            toolbar.setTitle("Recording Details");
            saveEventButton.setText("Update Event");

            eventTitle.setText(event.getTitle());
            eventDescription.setText(event.getDescription());
            selectedDateTime.setText("created on : " + event.getCreationDate() + " at  " + event.getCreationTime());
            datePickerBtn.setText(event.getEventDate());
            timePickerBtn.setText(event.getEventTime());
            recordingList.clear();
            recordingList = db.getRecordingsByEventId(event_id);
            recordingAdapter = new AudioRecyclerAdapter(recordingList, getContext(), this);
            updateSelectedItemsDisplay(new ArrayList<>());
            recyclerView.setAdapter(recordingAdapter);
            recordingAdapter.notifyDataSetChanged();

        } else {
            Toast.makeText(getContext(), "Event not found", Toast.LENGTH_SHORT).show();
            if (getActivity() instanceof Show_Add_notes_Activity) {
                ((Show_Add_notes_Activity) getActivity()).setFragment(new Show_Notes_Fragment());
            }
        }
    }
    private void saveAudioToDatabase(int eventId_c, String audioPath) throws IOException {
        DatabaseHelper databaseHelper = new DatabaseHelper(getContext());
        String name = getFileName(audioPath);         // Extract file name from the path
        String format = getFileExtension(audioPath);  // Extract file extension as format
        String length = getAudioDuration(audioPath);  // Get audio duration in seconds
        boolean isRecorded = false;                   // Set to false since it's imported
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yy 'at' HH:mm");
        String description =".";
        // Get the current date and time
        Date date = new Date();


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
            AudioRecyclerAdapter.saveSelectionState(event_id);
            Toast.makeText(getContext(), "Audio saved to database", Toast.LENGTH_SHORT).show();
            recordingList.clear();
            recordingList=databaseHelper.getRecordingsByEventId(event_id);
            recordingAdapter = new AudioRecyclerAdapter(recordingList,getContext() ,this);
            recyclerView.setAdapter(recordingAdapter);
            recordingAdapter.notifyDataSetChanged();
        } else {
            Toast.makeText(getContext(), "Failed to save audio", Toast.LENGTH_SHORT).show();
        }
    }
    private void updateEventData(int eventId, String title, String eventDescription, String selectedDate, String selectedTime) {
        if (title == null || title.trim().isEmpty()) {
            Toast.makeText(getContext(), "Event title cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        if (eventDescription == null || eventDescription.trim().isEmpty()) {
            eventDescription="";
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
                if (getActivity() instanceof Show_Add_notes_Activity) {
                    ((Show_Add_notes_Activity) getActivity()).setFragment(new Show_Notes_Fragment());
                }
            } catch (Exception e) {
                Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
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
            eventDescription="";
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
    private void updateSelectedItemsDisplay(ArrayList<String> selectedItems) {
        if (selectedItems.isEmpty()) {
            alltranscription.setText("No items selected");
            all_transcription_view.setVisibility(View.GONE);
            make_note.setVisibility(View.GONE);

        } else {
            String  des= TextUtils.join(", ", selectedItems);
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
    @Override
    public void onSelectionChanged(ArrayList<String> updatedSelection) {
        updateSelectedItemsDisplay(updatedSelection);

    }



}

