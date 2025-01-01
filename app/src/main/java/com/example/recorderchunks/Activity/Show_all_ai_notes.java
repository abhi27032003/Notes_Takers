package com.example.recorderchunks.Activity;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.recorderchunks.Adapter.NotesAdapter;
import com.example.recorderchunks.Helpeerclasses.Notes_Database_Helper;
import com.example.recorderchunks.Model_Class.Note;
import com.example.recorderchunks.R;

import java.util.ArrayList;

public class Show_all_ai_notes extends AppCompatActivity {
    private RecyclerView recyclerView;
    private NotesAdapter adapter;
    private ArrayList<Note> noteList;
    private Notes_Database_Helper dbHelper;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_all_ai_notes);
        Toolbar toolbar = findViewById(R.id.appBar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        String recording_id = getIntent().getStringExtra("text");
        String tit = getIntent().getStringExtra("Title");
        TextView Title = findViewById(R.id.title);
        if (tit != null) {
            Title.setText(tit);
        }
        /////////////////////////////////////////////////////////////////////////////////////////////////

        recyclerView = findViewById(R.id.notes_recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        dbHelper = new Notes_Database_Helper(this);
        noteList = new ArrayList<>();

        loadNotes(Integer.parseInt(recording_id));


    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    private void loadNotes(int recordingId) {
        // Fetch notes by recording ID (using the new method that returns an ArrayList)
        noteList = dbHelper.getNotesByRecordingId(recordingId);

        if (noteList != null && !noteList.isEmpty()) {
            adapter = new NotesAdapter(this, noteList);
            recyclerView.setAdapter(adapter);
        } else {
            Toast.makeText(this, "No notes found for this recording ID", Toast.LENGTH_SHORT).show();
        }

    }}
