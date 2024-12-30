package com.example.recorderchunks;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

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
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
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
}
