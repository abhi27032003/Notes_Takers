package com.example.recorderchunks;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class activity_text_display extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Toolbar toolbar = findViewById(R.id.appBar);
        setSupportActionBar(toolbar);

        // Enable back button in toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        setContentView(R.layout.activity_text_display);

        // Get the TextView for displaying the full text
        TextView fullText = findViewById(R.id.fullText);
        TextView Title = findViewById(R.id.title);

        // Retrieve the text passed from the MainActivity
        String text = getIntent().getStringExtra("text");
        String tit = getIntent().getStringExtra("Title");

        // Display the text in the TextView
        if (text != null) {
            fullText.setText(text);
        }
        if (tit != null) {
            Title.setText(tit);
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
}
