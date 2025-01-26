package com.example.recorderchunks.Activity;

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.icu.text.SimpleDateFormat;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.recorderchunks.Adapter.TranscriptionAdapter;
import com.example.recorderchunks.Background_Allow.Show_Add_notes_Activity;
import com.example.recorderchunks.Helpeerclasses.DatabaseHelper;
import com.example.recorderchunks.Helpeerclasses.Transcription_Database_Helper;
import com.example.recorderchunks.Model_Class.TranscriptionHistory;
import com.example.recorderchunks.R;
import com.google.android.material.button.MaterialButton;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class activity_text_display extends AppCompatActivity {
    MaterialButton update;
    private static SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "PromptSelectionPrefs";
    private RecyclerView rvTranscriptionHistory;
    private TranscriptionAdapter adapter;
    private ImageView show_full_transcription;
    EditText fullText;
    String Recording_Recycler_id,Transcrition_mode,Event_id,Recording_id,tit,text;
    private Transcription_Database_Helper dbHelper;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Toolbar toolbar = findViewById(R.id.appBar);
        this.sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        update=findViewById(R.id.update_btn);
        rvTranscriptionHistory = findViewById(R.id.rvTranscriptionHistory);
        show_full_transcription=findViewById(R.id.show_full_transcription);
        DatabaseHelper databaseHelper=new DatabaseHelper(this);
         dbHelper=new Transcription_Database_Helper(this);

        setSupportActionBar(toolbar);

        // Enable back button in toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        setContentView(R.layout.activity_text_display);

        // Get the TextView for displaying the full text
        fullText = findViewById(R.id.fullText);
        TextView Title = findViewById(R.id.title);

        // Retrieve the text passed from the MainActivity
         text = getIntent().getStringExtra("text");
         tit = getIntent().getStringExtra("Title");
         Recording_id = getIntent().getStringExtra("R_id");
         Event_id = getIntent().getStringExtra("E_id");
         Transcrition_mode = getIntent().getStringExtra("T_mode");

         Recording_Recycler_id=Recording_id+Transcrition_mode;
        // Display the text in the TextView
        if (text != null) {
            fullText.setText(text);
        }
        if (tit != null) {
            Title.setText(tit);
            update=findViewById(R.id.update_btn);
            rvTranscriptionHistory = findViewById(R.id.rvTranscriptionHistory);

            if(tit.equals("Transcription"))
            {
                update.setVisibility(View.VISIBLE);
                rvTranscriptionHistory.setVisibility(View.VISIBLE);
                fullText.setFocusableInTouchMode(true);  // Make it focusable in touch mode
                fullText.setFocusable(true);  // Allow it to receive focus
                fullText.setEnabled(true);

                rvTranscriptionHistory.setLayoutManager(new LinearLayoutManager(this));

                List<TranscriptionHistory> transcriptionHistories = dbHelper.getAllTranscriptionsByRecordingId(Recording_Recycler_id);
                adapter = new TranscriptionAdapter(transcriptionHistories);
                rvTranscriptionHistory.setAdapter(adapter);


            }
            else
            {
                rvTranscriptionHistory.setVisibility(View.GONE);
                update.setVisibility(View.GONE);
                fullText.setEnabled(false);
            }

        }
        show_full_transcription=findViewById(R.id.show_full_transcription);
        show_full_transcription.setOnClickListener(v -> {
            // Create and configure the dialog
            Dialog dialog = new Dialog(v.getContext());
            dialog.setContentView(R.layout.dialog_full_transcription);
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

            // Get references to the dialog views
            TextView tvFullTranscription = dialog.findViewById(R.id.tvFullTranscription);
            ImageView btnCancel = dialog.findViewById(R.id.btnCancel);

            // Set the transcription text
            tvFullTranscription.setText(text);

            // Handle the cancel button click
            btnCancel.setOnClickListener(cancel -> dialog.dismiss());

            // Show the dialog
            dialog.show();
        });
        update=findViewById(R.id.update_btn);
        update.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(Transcrition_mode.contains("Local"))
                {
                    databaseHelper.updateRecordingDetails(Integer.parseInt(Recording_id),fullText.getText().toString());
                    updateSelectionState(Integer.parseInt(Event_id),text,fullText.getText().toString());
                }
                else
                {
                    databaseHelper.updaterecording_details_api(Integer.parseInt(Recording_id),fullText.getText().toString());
                    updateSelectionState(Integer.parseInt(Event_id),text,fullText.getText().toString());

                }
                saveStateoftranscription();

                Toast.makeText(activity_text_display.this, "Transcription updated successfully", Toast.LENGTH_SHORT).show();

                finish();
            };
        });



        findViewById(R.id.copy_p).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                copyToClipboard(tit,text);

            }
        });

        findViewById(R.id.share_p).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareTexts(tit,text);

            }
        });
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            saveStateoftranscription();
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void saveStateoftranscription() {
        if(!text.equals(fullText.getText().toString()))
        {
            String creationTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

            dbHelper.insertTranscription(Recording_Recycler_id,fullText.getText().toString(),creationTime);
        }
    }

    @Override
    public void onBackPressed() {
        saveStateoftranscription();
        super.onBackPressed();
    }

    private void copyToClipboard(String savedTitle, String savedText) {


        if (savedTitle.equals("No Title Saved") && savedText.equals("No Text Saved")) {
            Toast.makeText(this, "Nothing to copy. Please add texts first!", Toast.LENGTH_SHORT).show();
            return;
        }

        String copyContent = savedTitle + "\n" + savedText;

        ClipboardManager clipboardManager = (ClipboardManager) this.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Prompt Details", copyContent);
        clipboardManager.setPrimaryClip(clip);

        Toast.makeText(this, "Copied to clipboard!", Toast.LENGTH_SHORT).show();
    }
    private void shareTexts(String savedTitle, String savedText) {


        if (savedTitle.equals("No Title Saved") && savedText.equals("No Text Saved")) {
            Toast.makeText(this, "Nothing to share. Please add texts first!", Toast.LENGTH_SHORT).show();
            return;
        }

        String shareContent =   savedTitle + "\n" + savedText;

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareContent);

        this.startActivity(Intent.createChooser(shareIntent, "Share via"));
    }
    public static void updateSelectionState(int id, String stringToRemove, String stringToAdd) {
        // Retrieve the current set of selected items
        Set<String> selectedItemsSet = sharedPreferences.getStringSet("selected_items_" + id, new HashSet<>());

        // Create a new modifiable set to avoid modifying the immutable set returned by SharedPreferences
        Set<String> updatedSet = new HashSet<>(selectedItemsSet);

        // Remove the specified string
        updatedSet.remove(stringToRemove);

        // Add the new string
        updatedSet.add(stringToAdd);

        // Save the updated set back to SharedPreferences
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putStringSet("selected_items_" + id, updatedSet);
        editor.apply();
    }

}
