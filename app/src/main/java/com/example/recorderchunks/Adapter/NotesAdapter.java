package com.example.recorderchunks.Adapter;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.recorderchunks.Model_Class.Note;
import com.example.recorderchunks.R;

import java.util.List;

public class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.NoteViewHolder> {

    private Context context;
    private List<Note> noteList;


    public NotesAdapter(Context context, List<Note> noteList) {
        this.context = context;
        this.noteList = noteList;

    }

    @Override
    public NoteViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_note, parent, false);
        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(NoteViewHolder holder, int position) {
        Note note = noteList.get(position);
        holder.noteTextView.setText(note.getNote());
        holder.createdOnTextView.setText(R.string.created_on);
        holder.createdOnTextView.setText(holder.createdOnTextView.getText()+" : " + note.getCreatedOn());
        holder.readMore.setVisibility(View.VISIBLE);
        holder.readMore.setText(R.string.read_more);
        holder.noteTextView.setMaxLines(4);
        holder.noteTextView.setEllipsize(android.text.TextUtils.TruncateAt.END);

        // Set the listener for "Click to read more"
        holder.readMore.setOnClickListener(new View.OnClickListener() {
            private boolean isExpanded = false; // Flag to track the state

            @Override
            public void onClick(View v) {
                if (isExpanded) {
                    // Collapse the text to a limited number of lines
                    holder.noteTextView.setMaxLines(3); // Set the desired number of lines when collapsed
                    holder.readMore.setText(R.string.read_more); // Change the text to "Read More"
                } else {
                    // Expand the text to show the full note
                    holder.noteTextView.setMaxLines(Integer.MAX_VALUE);
                    holder.readMore.setText(R.string.read_less); // Change the text to "Show Less"
                }
                isExpanded = !isExpanded; // Toggle the flag
            }
        });



        // Set listeners for the buttons
        holder.shareButton.setOnClickListener(v -> shareTexts("Note",note.getNote()));
        holder.copyButton.setOnClickListener(v -> copyToClipboard("Note",note.getNote()));

    }

    @Override
    public int getItemCount() {
        return noteList.size();
    }

    public class NoteViewHolder extends RecyclerView.ViewHolder {
        TextView noteTextView;
        TextView createdOnTextView;
        ImageView saveButton, shareButton, copyButton;
        CardView selectedCard;
        TextView readMore;
        public NoteViewHolder(View itemView) {
            super(itemView);
            noteTextView = itemView.findViewById(R.id.fullText);
            createdOnTextView = itemView.findViewById(R.id.createdon);
            saveButton = itemView.findViewById(R.id.save_p);
            shareButton = itemView.findViewById(R.id.share_p);
            copyButton = itemView.findViewById(R.id.copy_p);
            selectedCard = itemView.findViewById(R.id.selectedcard);
            readMore = itemView.findViewById(R.id.read_more);

        }
    }

    // Interface for handling clicks on notes

    private void copyToClipboard(String savedTitle, String savedText) {


        if (savedTitle.equals("No Title Saved") && savedText.equals("No Text Saved")) {
            Toast.makeText(context, "Nothing to copy. Please add texts first!", Toast.LENGTH_SHORT).show();
            return;
        }

        String copyContent = savedTitle + "\n" + savedText;

        ClipboardManager clipboardManager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Prompt Details", copyContent);
        clipboardManager.setPrimaryClip(clip);

        Toast.makeText(context, "Copied to clipboard!", Toast.LENGTH_SHORT).show();
    }
    private void shareTexts(String savedTitle, String savedText) {


        if (savedTitle.equals("No Title Saved") && savedText.equals("No Text Saved")) {
            Toast.makeText(context, "Nothing to share. Please add texts first!", Toast.LENGTH_SHORT).show();
            return;
        }

        String shareContent =   savedTitle + "\n" + savedText;

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareContent);

        context.startActivity(Intent.createChooser(shareIntent, "Share via"));
    }
}
