package com.example.recorderchunks.Adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.recorderchunks.Background_Allow.Add_notes_Fragment;
import com.example.recorderchunks.Helpeerclasses.DatabaseHelper;
import com.example.recorderchunks.Helpeerclasses.Notes_Database_Helper;
import com.example.recorderchunks.Helpeerclasses.TagStorage;
import com.example.recorderchunks.Model_Class.Event;
import com.example.recorderchunks.Model_Class.current_event;
import com.example.recorderchunks.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.ViewHolder> {
    private Context context;
    private List<Event> events;

    private FragmentManager fragmentManager;

    private TagStorage tagStorage;

    private static SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "PromptSelectionPrefs";

    public EventAdapter(Context context, List<Event> events,FragmentManager fragmentManager) {
        this.fragmentManager=fragmentManager;
        this.context = context;
        this.events = events;
        tagStorage = new TagStorage(context);
        this.sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);


    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_event, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

        Event event = events.get(position);


        if (event.getTitle() != null) {
            holder.titleTextView.setText(event.getTitle());
        } else {
            holder.titleTextView.setText("Not Available"); // Set empty text or a placeholder
        }

        if (event.getDescription() != null) {
            int[] colors = {
                    context.getResources().getColor(R.color.RC1),
                    context.getResources().getColor(R.color.RC2),
                    context.getResources().getColor(R.color.RC3),
                    context.getResources().getColor(R.color.RC4),
                    context.getResources().getColor(R.color.RC5)
            };

            holder.linearLayout.setBackgroundColor(colors[Integer.parseInt(event.getDescription())]);
//            holder.Card_main.setStrokeColor(colors[Integer.parseInt(event.getDescription())]);
        } else {
            holder.descriptionTextView.setText("Not Available");
        }

        if (event.getCreationDate() != null) {
            holder.dateTextView.setText(R.string.created_on);
            holder.dateTextView.setText(holder.dateTextView.getText()+" : "+event.getCreationDate()+" at "+event.getCreationTime());
        } else {
            holder.dateTextView.setText("Not Available");
        }

        if (event.getEventDate() != null) {
            holder.eventDate.setText(event.getEventDate());
        } else {
            holder.eventDate.setText("Not Available");
        }

        if (event.getEventTime() != null) {
            holder.eventTime.setText(event.getEventTime());
        } else {
            holder.eventTime.setText("Not Available");
        }
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                    current_event ce=new current_event();
                    ce.setCurrent_event_no(event.getId());


                fragmentManager.beginTransaction()
                        .replace(R.id.main_item_container,new  Add_notes_Fragment())
                        .addToBackStack(null)
                        .commit();

            }
        });


        holder.recyclerView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent even) {
                current_event ce=new current_event();
                ce.setCurrent_event_no(event.getId());
                fragmentManager.beginTransaction()
                        .replace(R.id.main_item_container,new  Add_notes_Fragment())
                        .addToBackStack(null)
                        .commit();
                return false;
            }
        });

        holder.deleteEvent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Inside the onClickListener for the delete button
                new AlertDialog.Builder(v.getContext())
                        .setTitle("Delete Confirmation")
                        .setMessage("Are you sure you want to delete this Event?")
                        .setPositiveButton("Yes", (dialog, which) -> {
                            // Handle the delete action here
                            DatabaseHelper db=new DatabaseHelper(context);
                            db.deleteEvent(event.getId());
                            db.deleteRecording(event.getId());
                            deleteSelectionState(event.getId(),context);
                            Notes_Database_Helper ndh=new Notes_Database_Helper(context);
                            ndh.deleteNotebyevent_id(event.getId());
                            Toast.makeText(context,"Event Deleted Successfully..",Toast.LENGTH_LONG).show();
                            try {
                                if (position != RecyclerView.NO_POSITION) { // Ensure position is valid
                                    events.remove(position); // Remove the item from the data source
                                    notifyItemRemoved(position); // Notify the adapter of item removal
                                }
                            }
                            catch (Exception e)
                            {
                                Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton("No", (dialog, which) -> {
                            // Dismiss the dialog if 'No' is clicked
                            dialog.dismiss();
                        })
                        .show();

            }
        });

        //set tags
        // Fetch selectedTags (example setup, replace with your actual logic)
        List<String> selectedTags = tagStorage.getTags(String.valueOf(event.getId())) != null
                ? tagStorage.getTags(String.valueOf(event.getId())).get("selected_tags")
                : new ArrayList<>();


        LinearLayoutManager layoutManager = new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false);
        holder.recyclerView.setLayoutManager(layoutManager);

// Set the adapter
        HorizontalRecyclerViewAdapter adapter3 = new HorizontalRecyclerViewAdapter(context, selectedTags);
        holder.recyclerView.setAdapter(adapter3);

    }

    public static void deleteSelectionState(int id, Context context) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove("selected_items_" + id); // Remove the entry for the specific id
        editor.apply(); // Apply the changes
    }



    @Override
    public int getItemCount() {
        return events.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView, descriptionTextView, dateTextView;
        MaterialButton eventDate,eventTime;
        ImageView deleteEvent;
        LinearLayout linearLayout ;
        RecyclerView recyclerView ;
        MaterialCardView Card_main;

        public ViewHolder(View itemView) {
            super(itemView);
            Card_main=itemView.findViewById(R.id.Card_main);

            linearLayout=itemView.findViewById(R.id.linearLayout);
            recyclerView= itemView.findViewById(R.id.horizontalRecyclerView);
            titleTextView = itemView.findViewById(R.id.eventTitle);
            descriptionTextView = itemView.findViewById(R.id.eventDescription);
            dateTextView = itemView.findViewById(R.id.eventCreationDate);
            eventDate = itemView.findViewById(R.id.eventDate);
            eventTime = itemView.findViewById(R.id.eventTime);
            deleteEvent=itemView.findViewById(R.id.deleteButton);


        }
    }
}
