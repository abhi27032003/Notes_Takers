package com.example.recorderchunks;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.recorderchunks.Adapter.EventAdapter;
import com.example.recorderchunks.Helpeerclasses.DatabaseHelper;
import com.example.recorderchunks.Model_Class.Event;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    
    public RecyclerView recordingRecyclerView;
    private ImageView goTo_Add_event_Page,add_api;
    EventAdapter eventAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
       load_data();


    }
    public void load_data()
    {
        recordingRecyclerView = findViewById(R.id.recordingRecyclerView);
        goTo_Add_event_Page = findViewById(R.id.add_event);
        add_api=findViewById(R.id.add_api);
        add_api.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(MainActivity.this, API_Updation.class);
                startActivity(i);
            }
        });

        // Set up OnClickListener for "Add Event" button
        goTo_Add_event_Page.setOnClickListener(v -> {

        });

        // Initialize Database Helper
        DatabaseHelper databaseHelper = new DatabaseHelper(this);

        // Set up RecyclerView
        recordingRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Get all events from the database
        List<Event> eventsList = databaseHelper.getAllEvents();

        // Set up the adapter and attach it to the RecyclerView
        eventAdapter = new EventAdapter(this, eventsList,getSupportFragmentManager());
        recordingRecyclerView.setAdapter(eventAdapter);
        eventAdapter.notifyDataSetChanged();



    }






}
