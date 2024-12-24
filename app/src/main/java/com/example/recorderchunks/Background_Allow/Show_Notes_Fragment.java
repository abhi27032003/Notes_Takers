package com.example.recorderchunks.Background_Allow;


import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.recorderchunks.API_Updation;
import com.example.recorderchunks.Adapter.EventAdapter;
import com.example.recorderchunks.DatabaseHelper;
import com.example.recorderchunks.Model_Class.Event;
import com.example.recorderchunks.Model_Class.RecordingViewModel;
import com.example.recorderchunks.Model_Class.current_event;
import com.example.recorderchunks.Model_Class.recording_event_no;
import com.example.recorderchunks.R;
import com.example.recorderchunks.utils.RecordingManager;
import com.example.recorderchunks.utils.RecordingUtils;

import java.util.List;


public class Show_Notes_Fragment extends Fragment implements RecordingUtils.RecordingCallback  {



    public recording_event_no recording_event_no;
    public RecyclerView recordingRecyclerView;
    private ImageView goTo_Add_event_Page,add_api;
    EventAdapter eventAdapter;
    DatabaseHelper databaseHelper;
    private RecordingViewModel recordingViewModel;
    CardView recording_small_card;
    TextView textView_small_Timer;
    ImageView stop_recording_small_animation,play_pause_recording_small_animation;

    RecordingUtils recordingUtils;



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view=inflater.inflate(R.layout.fragment_show_notes, container, false);
        ///////////////////////////////////////////////////////////////////////
        recording_event_no=new recording_event_no();
        current_event ce=new current_event();
        recordingViewModel = new ViewModelProvider(requireActivity()).get(RecordingViewModel.class);
        databaseHelper = new DatabaseHelper(getContext());
        recordingUtils = RecordingManager.getInstance(
                requireContext(),this

        );
        ///////////////////////////////////////////////////////////////////////
        ce.setCurrent_event_no(-1);
        stop_recording_small_animation=view.findViewById(R.id.stop_recording_small_animation);
        play_pause_recording_small_animation=view.findViewById(R.id.play_pause_recording_small_animation);
        recordingRecyclerView =view.findViewById(R.id.recordingRecyclerView);
        goTo_Add_event_Page = view.findViewById(R.id.add_event);
        add_api=view.findViewById(R.id.add_api);
        recording_small_card=view.findViewById(R.id.recording_small_card);
        textView_small_Timer=view.findViewById(R.id.textView_small_Timer);
        recordingViewModel.getElapsedSeconds().observe(getViewLifecycleOwner(), elapsedTime -> {
            // Update UI when elapsed time changes
            updateTimerText(elapsedTime);
        });
        play_pause_recording_small_animation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Boolean.TRUE.equals(recordingViewModel.getIsRecording().getValue())) {
                    // Pause recording
                    recordingUtils.pauseRecording();
                    recordingViewModel.setRecording(false);  // Set recording state to false
                    recordingViewModel.setPaused(true);     // Set the paused state to true
                    recordingViewModel.pauseTimer();        // Pause the timer

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
                    Intent i = new Intent(getContext(), API_Updation.class);
                    startActivity(i);
                }

            }
        });

        if (Boolean.TRUE.equals(recordingViewModel.getIsRecording().getValue())) {
            // Recording in progress: Show the recording UI
            recording_small_card.setVisibility(View.VISIBLE);
            play_pause_recording_small_animation.setImageResource(R.mipmap.pause); // Set pause icon for recording
        } else if (Boolean.TRUE.equals(recordingViewModel.getIsPaused().getValue())) {
            // Recording is paused: Keep the UI visible, but show play icon
            recording_small_card.setVisibility(View.VISIBLE);
            play_pause_recording_small_animation.setImageResource(R.mipmap.play); // Set play icon for paused state
        } else {
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
        List<Event> eventsList = databaseHelper.getAllEvents();

        // Set up the adapter and attach it to the RecyclerView
        eventAdapter = new EventAdapter(getContext(), eventsList,getActivity().getSupportFragmentManager());
        recordingRecyclerView.setAdapter(eventAdapter);
        eventAdapter.notifyDataSetChanged();

        return view;
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
}