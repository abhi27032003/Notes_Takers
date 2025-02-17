package com.example.recorderchunks.Background_Allow;


import static android.content.Context.MODE_PRIVATE;

import static com.example.recorderchunks.Adapter.HorizontalRecyclerViewAdapter.getWord;
import static com.example.recorderchunks.Encryption.RSAKeyGenerator.divideString;
import static com.example.recorderchunks.Encryption.RSAKeyGenerator.encrypt;
import static com.example.recorderchunks.Encryption.RSAKeyGenerator.generateSHA256HashWithSalt;
import static com.example.recorderchunks.Encryption.RSAKeyGenerator.getPublicKeyFromString;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.ImageView;

import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.SearchView;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.recorderchunks.Activity.API_Updation;
import com.example.recorderchunks.Activity.RecordingService;
import com.example.recorderchunks.Activity.activity_text_display;
import com.example.recorderchunks.Adapter.EventAdapter;
import com.example.recorderchunks.AudioPlayer.AudioPlaybackService;
import com.example.recorderchunks.AudioPlayer.AudioPlayerManager;
import com.example.recorderchunks.AudioPlayer.AudioPlayerViewModel;
import com.example.recorderchunks.Helpeerclasses.DatabaseHelper;
import com.example.recorderchunks.Helpeerclasses.TagStorage;
import com.example.recorderchunks.Model_Class.Event;
import com.example.recorderchunks.Model_Class.RecordingViewModel;
import com.example.recorderchunks.Model_Class.current_event;
import com.example.recorderchunks.Model_Class.recording_event_no;
import com.example.recorderchunks.MyApplication;
import com.example.recorderchunks.R;
import com.example.recorderchunks.utils.RecordingManager;
import com.example.recorderchunks.utils.RecordingUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class Show_Notes_Fragment extends Fragment implements RecordingUtils.RecordingCallback  {



    public recording_event_no recording_event_no;
    public RecyclerView recordingRecyclerView;
    private SearchView searchView;

    private CardView goTo_Add_event_Page,add_api;
    EventAdapter eventAdapter;
    DatabaseHelper databaseHelper;
    private RecordingViewModel recordingViewModel;
    CardView recording_small_card;
    TextView textView_small_Timer;
    ImageView stop_recording_small_animation,play_pause_recording_small_animation, filter_button;

    RecordingUtils recordingUtils;

    //draging icons
    private static final String PREFS_NAME = "CardViewPosition";
    private static final String PREF_START_MARGIN = "StartMargin";
    private static final String PREF_TOP_MARGIN = "TopMargin";

    private float dX, dY;
    private int lastAction;
    private SharedPreferences sharedPreferences2;
    private ConstraintLayout constraintLayout;
    Set<String> allTags = new HashSet<>();
    private TagStorage tagStorage;
    Set<String> selectedTags = new HashSet<>();
    List<Event> eventsList,tempeventlist;

    List<Event> realtimeeventsList;

    ///
    AudioPlayerViewModel viewModel ;
    public  AudioPlayerManager playerManager;





    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view=inflater.inflate(R.layout.fragment_show_notes, container, false);
        ///////////////////////////////////////////////////////////////////////
        viewModel = new ViewModelProvider(requireActivity()).get(AudioPlayerViewModel.class);
        playerManager=AudioPlayerManager.getInstance();

        ////////////////////////////////
        Button button1 = view.findViewById(R.id.button_1);
        Button button2 = view.findViewById(R.id.button_2);

        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendUserPublicKey();
            }
        });

        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendEncryptedAESKey();
            }
        });


        //////
        recording_event_no= com.example.recorderchunks.Model_Class.recording_event_no.getInstance();
        current_event ce=new current_event();
        recordingViewModel = new ViewModelProvider(
                (ViewModelStoreOwner) requireActivity().getApplication(),
                ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().getApplication())
        ).get(RecordingViewModel.class);

        databaseHelper = new DatabaseHelper(getContext());
        recordingUtils = RecordingManager.getInstance(
                requireContext(),this

        );

        sharedPreferences2 = getActivity().getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        tagStorage = new TagStorage(getContext());


        ///////////////////////////////////////////////////////////////////////
        ce.setCurrent_event_no(-1);
        stop_recording_small_animation=view.findViewById(R.id.stop_recording_small_animation);
        play_pause_recording_small_animation=view.findViewById(R.id.play_pause_recording_small_animation);
        recordingRecyclerView =view.findViewById(R.id.recordingRecyclerView);
        goTo_Add_event_Page = view.findViewById(R.id.add_event);
        add_api=view.findViewById(R.id.add_api);
        recording_small_card=view.findViewById(R.id.recording_small_card);
        textView_small_Timer=view.findViewById(R.id.textView_small_Timer);
        constraintLayout = view.findViewById(R.id.constraint);
        filter_button=view.findViewById(R.id.filter_button);
        searchView = view.findViewById(R.id.searchView);

        //search view
        searchView.setIconifiedByDefault(false);
        searchView.setFocusable(true);
        searchView.setFocusableInTouchMode(true);
       // searchView.requestFocus();

        // Set query hint programmatically (fix for some Android versions)
        searchView.setQueryHint("Search by title");


        //update the ui based on the recording view model
        recordingViewModel.getElapsedSeconds().observe(getViewLifecycleOwner(), elapsedTime -> {
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

                    }
                });

            }
        });




        // Enable dragging for a card
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
        play_pause_recording_small_animation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RecordingService r=new RecordingService();
                if (Boolean.TRUE.equals(recordingViewModel.getIsRecording().getValue())) {
                    // Pause recording
                    recordingUtils.pauseRecording();
                    recordingViewModel.setRecording(false);  // Set recording state to false
                    recordingViewModel.setPaused(true);     // Set the paused state to true
                    recordingViewModel.pauseTimer();

                    // Pause the timer

                    // Update the play/pause button icon to 'play'
                    play_pause_recording_small_animation.setImageResource(R.mipmap.play);
                } else if (Boolean.TRUE.equals(recordingViewModel.getIsPaused().getValue())) {
                    // Resume recording
                    recordingUtils.resumeRecording();
                    recordingViewModel.setRecording(true);  // Set recording state to true
                    recordingViewModel.setPaused(false);   // Clear the paused state
                    recordingViewModel.resumeTimer();      // Resume the timer

                    // Update the play/pause button icon to 'pause'
                    play_pause_recording_small_animation.setImageResource(R.mipmap.pause);
                }
            }
        });
        stop_recording_small_animation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Stop recording entirely
                recordingUtils.stopRecording(recording_event_no.getRecording_event_no());

                // Reset the states in ViewModel
                recordingViewModel.setRecording(false);
                recordingViewModel.setPaused(false); // Reset paused state
                recordingViewModel.resetTimer();     // Stop and reset the timer

                // Update UI elements
                updateTimerText(0); // Reset the timer text to 0
                recordingViewModel.updateElapsedSeconds(0); // Reset elapsed time in ViewModel
                recording_small_card.setVisibility(View.GONE); // Hide the recording card
                play_pause_recording_small_animation.setImageResource(R.mipmap.pause); // Set the icon to 'pause'
                stop_recording_small_animation.setVisibility(View.GONE); // Hide the stop button (or reset UI as needed)
            }
        });
        add_api.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Boolean.TRUE.equals(recordingViewModel.getIsRecording().getValue())) {
                    // Timer setup
                    showFinishRecordingDialog("Finish Recording First to update the Prompt Page !!!");
                } else {
                    // Stop recording
                    if(playerManager!=null)
                    {
                      // resetPlayback();
                    }
                    Intent i = new Intent(getContext(), API_Updation.class);

                    startActivity(i);
                }

            }
        });
        if (Boolean.TRUE.equals(recordingViewModel.getIsRecording().getValue())) {
            // Recording in progress: Show the recording UI
            recording_small_card.setVisibility(View.VISIBLE);
            reloadPosition(recording_small_card);
            play_pause_recording_small_animation.setImageResource(R.mipmap.pause); // Set pause icon for recording
        }
        else if (Boolean.TRUE.equals(recordingViewModel.getIsPaused().getValue())) {
            // Recording is paused: Keep the UI visible, but show play icon
            recording_small_card.setVisibility(View.VISIBLE);
            reloadPosition(recording_small_card);
            play_pause_recording_small_animation.setImageResource(R.mipmap.play); // Set play icon for paused state
        }
        else {
            // Recording has stopped or is not active
            recording_small_card.setVisibility(View.GONE);
        }

        // Set up OnClickListener for "Add Event" button
        goTo_Add_event_Page.setOnClickListener(v -> {
            if (Boolean.TRUE.equals(recordingViewModel.getIsRecording().getValue())) {
                // Timer setup
               showFinishRecordingDialog("Finish Recording First to Create a event again !!!");
            } else {
                recording_event_no.setRecording_event_no(-1);
                if (getActivity() instanceof Show_Add_notes_Activity) {
                    ((Show_Add_notes_Activity) getActivity()).setFragment(new Add_notes_Fragment());
                }
            }



        });



        // Set up RecyclerView
        recordingRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Get all events from the database
        eventsList = databaseHelper.getAllEvents();
        tempeventlist=eventsList;

        // Set up the adapter and attach it to the RecyclerView
        eventAdapter = new EventAdapter(getContext(), eventsList,getActivity().getSupportFragmentManager());
        recordingRecyclerView.setAdapter(eventAdapter);
        eventAdapter.notifyDataSetChanged();


        //search filter
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                eventAdapter.getFilter().filter(query, new Filter.FilterListener() {
                    @Override
                    public void onFilterComplete(int count) {
                        // Delay execution of filterRecyclerView() by 2 seconds
                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                filterRecyclerView();
                            }
                        }, 0); // 2000 milliseconds = 2 seconds
                    }
                });
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                eventAdapter.getFilter().filter(newText, new Filter.FilterListener() {
                    @Override
                    public void onFilterComplete(int count) {
                        // Delay execution of filterRecyclerView() by 2 seconds
                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                filterRecyclerView();
                            }
                        }, 0); // 2000 milliseconds = 2 seconds
                    }
                });
                return false;
            }
        });


        ///////filter

        filter_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTagSelectionDialog();

            }
        });


        return view;
    }
    private void filterRecyclerView() {
       // Toast.makeText(getContext(), selectedTags.toString(), Toast.LENGTH_SHORT).show();
        if(selectedTags.size()<=0)
        {
            return;
        }
        List<Event> filteredList2 = new ArrayList<>();
        realtimeeventsList=EventAdapter.filteredList;
        for (Event event : realtimeeventsList) {
            List<String> tags = tagStorage.getTags(String.valueOf(event.getId())).get("selected_tags");
            if (tags != null) {
                for (String tag : tags) {
                    if (selectedTags.contains(getWord(tag))) {
                        filteredList2.add(event);
                        break;
                    }
                }
            }
        }

       // Toast.makeText(getContext(), "sixe :"+filteredList.size(), Toast.LENGTH_SHORT).show();
        //Toast.makeText(getContext(), "filtered list size : "+filteredList2.size(), Toast.LENGTH_SHORT).show();
        eventAdapter.updateList(filteredList2);



    }
    private void showTagSelectionDialog() {
        Dialog dialog = new Dialog(getContext());
        dialog.setContentView(R.layout.dialog_tag_selection);
        dialog.setTitle("Select Tags");

        EditText searchBar = dialog.findViewById(R.id.searchBar);
        ListView tagListView = dialog.findViewById(R.id.tagListView);
        Button applyButton = dialog.findViewById(R.id.applyButton);

        // Extract all unique tags from RecyclerView
        Set<String> allTags = new HashSet<>();
        for (Event event : eventsList) {
            List<String> tags = tagStorage.getTags(String.valueOf(event.getId())).get("selected_tags");
            if (tags != null) {
                for (String tag : tags) {
                    try {
                        allTags.add(getWord(tag)); // Apply getWord() before adding to the set
                    } catch (IllegalArgumentException e) {
                        // Handle invalid format (Optional: Log or ignore)
                        System.err.println("Skipping invalid tag: " + tag);
                    }
                }
            }
        }

        List<String> tagList = new ArrayList<>(allTags);
        Collections.sort(tagList); // Sort alphabetically

        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_multiple_choice, tagList);
        tagListView.setAdapter(adapter);
        tagListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

        // Restore previously selected tags
        for (int i = 0; i < tagList.size(); i++) {
            if (selectedTags.contains(tagList.get(i))) {
                tagListView.setItemChecked(i, true);
            }
        }

        // Search functionality
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.getFilter().filter(s);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Apply filter on button click
        applyButton.setOnClickListener(v -> {
            selectedTags.clear();
            for (int i = 0; i < tagListView.getCount(); i++) {
                if (tagListView.isItemChecked(i)) {
                    selectedTags.add(tagList.get(i));
                }
            }
            dialog.dismiss();
            if(selectedTags.size()>0)
            {
                if(!searchView.getQuery().toString().isEmpty())
                {
                    //Toast.makeText(getContext(), searchView.getQuery().toString(), Toast.LENGTH_SHORT).show();
                    eventAdapter.getFilter().filter(searchView.getQuery().toString(), new Filter.FilterListener() {
                        @Override
                        public void onFilterComplete(int count) {
                            // Delay execution of filterRecyclerView() by 2 seconds
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    filterRecyclerView();
                                }
                            }, 0); // 2000 milliseconds = 2 seconds
                        }
                    });

                }
                else
                {
                    try {

                        eventAdapter.updateList(eventsList);
                    }
                    catch (Exception e)
                    {

                    }
                    finally {
                        filterRecyclerView();
                    }
                }



            }
            else
            {
                eventAdapter.updateList(eventsList);

            }

        });

        dialog.show();
    }
    private void updateTimerText(int elapsedTime) {
        // Format the time and set it to the TextView
        int minutes = elapsedTime / 60;
        int seconds = elapsedTime % 60;
        String formattedTime = String.format("%02d:%02d", minutes, seconds);
        textView_small_Timer.setText(formattedTime);

    }





    @Override
    public void onRecordingStarted(String filePath) {

    }

    @Override
    public void onRecordingSaved(int eventId) {

    }

    @Override
    public void onRecordingPaused() {

    }

    @Override
    public void onRecordingResumed() {

    }

    @Override
    public void onResume() {
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

                    }
                });

            }
        });

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

    private void showFinishRecordingDialog(String s) {
        new AlertDialog.Builder(getContext())
                .setTitle("Finish Recording")
                .setMessage(s)
                .setCancelable(false)  // Prevent dialog from being dismissed when touching outside
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Dismiss the dialog and stay on this fragment
                    }
                })
                .create()
                .show();
    }
    private void resetPlayback() {
        if (getActivity() == null) return;

        // Get the global ViewModel instance from MyApplication
        MyApplication application = (MyApplication) requireActivity().getApplication();
        AudioPlayerViewModel viewModel = new ViewModelProvider(application).get(AudioPlayerViewModel.class);

        // Reset ViewModel state
        viewModel.setPlaying(false);
        viewModel.setCurrentRecording(-1);
        viewModel.setSeekPosition(0);

        // Stop AudioPlaybackService
        Intent stopIntent = new Intent(requireContext(), AudioPlaybackService.class);
        stopIntent.setAction("STOP");
        requireContext().startService(stopIntent);

    }
    private void sendUserPublicKey() {
        SharedPreferences prefs = getContext().getSharedPreferences("preferences", Context.MODE_PRIVATE);
        String uuid = prefs.getString("uuid", null);
        String clientPublicKey = prefs.getString("client_public_key", null);

        if (uuid == null || clientPublicKey == null) {
            Toast.makeText(getContext(), "Missing required data in SharedPreferences", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] clientKeyParts = divideString(clientPublicKey);

        JSONObject jsonPayload = new JSONObject();
        try {
            jsonPayload.put("encrypted_clientpublic", "NA");
            jsonPayload.put("uuid", uuid);
            jsonPayload.put("encrypted_clientpublic_part1", clientKeyParts[0]);
            jsonPayload.put("encrypted_clientpublic_part2", clientKeyParts[1]);
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Error creating JSON payload", Toast.LENGTH_SHORT).show();
            return;
        }

        RequestQueue requestQueue = Volley.newRequestQueue(getContext());
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.POST,
                "https://notetakers.vipresearch.ca/App_Script/send_client_public.php",
                jsonPayload,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Toast.makeText(getContext(), "Public Key Sent: " + response.toString(), Toast.LENGTH_LONG).show();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(getContext(), "Error Sending Public Key: " + error.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }
        );

        requestQueue.add(jsonObjectRequest);
    }

    private void sendEncryptedAESKey() {
        SharedPreferences prefs = getContext().getSharedPreferences("preferences", Context.MODE_PRIVATE);
        String uuid = prefs.getString("uuid", null);
        String serverPublicKey = prefs.getString("server_public_key", null);
        String clientPublicKey = prefs.getString("client_public_key", null);
        String userAESKey = prefs.getString("client_AES_key", null);

        if (uuid == null || serverPublicKey == null || clientPublicKey == null || userAESKey == null) {
            Toast.makeText(getContext(), "Missing required data in SharedPreferences", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            PublicKey pkServer = getPublicKeyFromString(serverPublicKey);
            String encryptedKeyBase64 = encrypt(generateSHA256HashWithSalt(userAESKey, clientPublicKey), pkServer);

            JSONObject payload = new JSONObject();
            payload.put("uuid", uuid);
            payload.put("encrypted_aes_key", encryptedKeyBase64);

            RequestQueue requestQueue = Volley.newRequestQueue(getContext());
            JsonObjectRequest jsonRequest = new JsonObjectRequest(
                    Request.Method.POST,
                    "https://notetakers.vipresearch.ca/App_Script/send_aes_key.php",
                    payload,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            Toast.makeText(getContext(), "AES Key Sent: " + response.toString(), Toast.LENGTH_LONG).show();
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Toast.makeText(getContext(), "Error Sending AES Key: " + error.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
            );

            requestQueue.add(jsonRequest);
        } catch (Exception e) {
            Toast.makeText(getContext(), "Encryption error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

}