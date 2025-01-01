package com.example.recorderchunks.Adapter;

import static android.content.Context.MODE_PRIVATE;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.InputType;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.recorderchunks.Model_Class.Prompt;
import com.example.recorderchunks.Helpeerclasses.Prompt_Database_Helper;
import com.example.recorderchunks.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class PromptAdapter extends RecyclerView.Adapter<PromptAdapter.PromptViewHolder> {

    private Context context;
    private List<Prompt> promptsList;
    private static final String PREF_NAME = "ApiKeysPref";
    private SharedPreferences sharedPreferences;
    public static final String KEY_PROMPT_ID = "Prompt";


    public  PromptAdapter(Context context, List<Prompt> promptsList) {
        this.context = context;
        this.promptsList = promptsList;
    }

    @NonNull
    @Override
    public PromptViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.prompt_item, parent, false);
        return new PromptViewHolder(view);
    }



    @Override
    public void onBindViewHolder(@NonNull PromptViewHolder holder, int position) {
        Prompt prompt = promptsList.get(position);
        sharedPreferences = context.getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        holder.Prompt_text.setText(prompt.getText());
        holder.Prompt_Title.setText(prompt.getName());
        loadSavedData(holder.save,holder.selectedcard,prompt.getId());
        holder.edit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                holder.Prompt_text.setVisibility(View.VISIBLE);
               // visible=true;
                //showPromptDialog();
            }
        });
        String savedId = getSavedId();
        String currentid=String.valueOf(prompt.getId());
        if (currentid.equals(savedId)) {
            holder.save.setImageResource(R.mipmap.saved); // Icon for saved state
        } else {
            holder.save.setImageResource(R.mipmap.save); // Icon for unsaved state
        }

        holder.save.setOnClickListener(v -> {
            String currentSavedId = getSavedId();

            if (currentid.equals(currentSavedId)) {
                // Remove the ID if it's already saved
                saveId(null);
                holder.save.setImageResource(R.mipmap.save);
            } else {
                // Save the new ID
                saveId(currentid);
               // holder.save.setImageResource(R.mipmap.save);

                notifyDataSetChanged(); // Update the entire list to refresh icons
            }
        });
        holder.share.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareTexts(prompt.getName(),prompt.getText());
            }
        });
        holder.copy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                copyToClipboard(prompt.getName(),prompt.getText());

            }
        });
        holder.delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteValues(prompt.getId(),position);

            }
        });
        holder.edit.setOnClickListener(v -> {
            LinearLayout layout = new LinearLayout(context);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setPadding(5, 5, 5, 5);

            // Create EditTexts for input
            EditText titleInput = new EditText(context);
            titleInput.setHint("Enter prompt title");
            titleInput.setInputType(InputType.TYPE_CLASS_TEXT);
            titleInput.setText(prompt.getName());
            layout.addView(titleInput);

            EditText textInput = new EditText(context);
            textInput.setHint("Enter prompt text");
            textInput.setInputType(InputType.TYPE_CLASS_TEXT);
            textInput.setText(prompt.getText());
            layout.addView(textInput);

            // Fetch previously saved data


            // Create and show the dialog
            new AlertDialog.Builder(context)
                    .setTitle("Update Prompt Details")
                    .setView(layout)
                    .setPositiveButton("Save", (dialog, which) -> {
                        // Save to SharedPreferences
                        String title = titleInput.getText().toString().trim();
                        String text = textInput.getText().toString().trim();

                        if (!title.isEmpty() && !text.isEmpty()) {
                            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy 'at' HH:mm");
                            String date= sdf.format(new Date());
                            Prompt_Database_Helper pdh=new Prompt_Database_Helper(context);
                            int opt=pdh.updatePrompt(prompt.getId(),title,text,date);


                            if(opt>=1)
                            {
                                Toast.makeText(context, "Updated  Successfully!", Toast.LENGTH_SHORT).show();
                                holder.Prompt_Title.setText(title);
                                holder.Prompt_text.setText(text);
                                //notifyDataSetChanged();

                            }
                            else
                            {
                                Toast.makeText(context, "Some Error!", Toast.LENGTH_SHORT).show();

                            }
                        } else {
                            Toast.makeText(context, "Both fields are required!", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();

        });


    }
    private String getSavedId() {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getString(KEY_PROMPT_ID, null);
    }

    private void saveId(String id) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        if (id == null) {
            editor.remove(KEY_PROMPT_ID);
        } else {
            editor.putString(KEY_PROMPT_ID, id);
        }
        editor.apply();
    }

    private void loadSavedData(ImageView save, CardView selectedcard, int id) {
        String prompt_text = sharedPreferences.getString(KEY_PROMPT_ID, "");

        if (!TextUtils.isEmpty(prompt_text) && String.valueOf(prompt_text).equals(String.valueOf(id)))
        {
            save.setImageResource(R.mipmap.saved);
            selectedcard.setVisibility(View.VISIBLE);
        }
        else
        {
            selectedcard.setVisibility(View.GONE);
        }
    }
    private void shareTexts(String savedTitle, String savedText) {


        if (savedTitle.equals("No Title Saved") && savedText.equals("No Text Saved")) {
            Toast.makeText(context, "Nothing to share. Please add texts first!", Toast.LENGTH_SHORT).show();
            return;
        }

        String shareContent = "Title: " + savedTitle + "\nText: " + savedText;

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareContent);

        context.startActivity(Intent.createChooser(shareIntent, "Share via"));
    }
    private void copyToClipboard(String savedTitle, String savedText) {


        if (savedTitle.equals("No Title Saved") && savedText.equals("No Text Saved")) {
            Toast.makeText(context, "Nothing to copy. Please add texts first!", Toast.LENGTH_SHORT).show();
            return;
        }

        String copyContent = "Title: " + savedTitle + "\nText: " + savedText;

        ClipboardManager clipboardManager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Prompt Details", copyContent);
        clipboardManager.setPrimaryClip(clip);

        Toast.makeText(context, "Copied to clipboard!", Toast.LENGTH_SHORT).show();
    }
    private void deleteValues(int deleteid, int position) {
        // Create an AlertDialog to confirm deletion
        Prompt_Database_Helper pdh = new Prompt_Database_Helper(context);

        new AlertDialog.Builder(context)
                .setTitle("Confirm Deletion")
                .setMessage("Are you sure you want to delete the saved values?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    // Perform deletion from database
                    pdh.deletePrompt(deleteid);

                    // Remove the item from the list
                    promptsList.remove(position);

                    // Notify the adapter about the removed item
                    notifyItemRemoved(position);

                    // Optionally notify about changes to the rest of the list
                    notifyItemRangeChanged(position, promptsList.size());

                    Toast.makeText(context, "Prompt Deleted Successfully", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    // Dismiss the dialog
                    dialog.dismiss();
                })
                .show();
    }


    @Override
    public int getItemCount() {
        return promptsList.size();
    }

    public static class PromptViewHolder extends RecyclerView.ViewHolder {
        TextView Prompt_Title , Prompt_text;
        ImageView copy, share,edit,delete,save;

        CardView selectedcard;
        public PromptViewHolder(@NonNull View itemView) {
            super(itemView);

            Prompt_text=itemView.findViewById(R.id.prompt_text);
            Prompt_Title=itemView.findViewById(R.id.title_prompt);

            copy=itemView.findViewById(R.id.copy_p);
            share=itemView.findViewById(R.id.share_p);
            edit=itemView.findViewById(R.id.edit_p);
            selectedcard=itemView.findViewById(R.id.selectedcard);

            delete=itemView.findViewById(R.id.delete_p);
            save=itemView.findViewById(R.id.save_p);

        }
    }

    private void showPromptDialog(int id) {
        // Create a LinearLayout to hold input fields

    }

}
