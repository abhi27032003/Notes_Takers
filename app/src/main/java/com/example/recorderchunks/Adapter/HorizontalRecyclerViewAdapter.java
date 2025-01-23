package com.example.recorderchunks.Adapter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.recorderchunks.R;

import java.util.List;
import java.util.Random;

public class HorizontalRecyclerViewAdapter extends RecyclerView.Adapter<HorizontalRecyclerViewAdapter.ViewHolder> {
    private List<String> items;
    private Context context;

    // Array of colors for the ImageView
    private int[] colors = {
            Color.RED,    // Red
            Color.GREEN,  // Green
            Color.BLUE,   // Blue
            Color.YELLOW, // Yellow
            Color.CYAN,   // Cyan
            Color.MAGENTA,// Magenta
               // Black
    };

    public HorizontalRecyclerViewAdapter(Context context, List<String> items) {
        this.context = context;
        this.items = items;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // Inflate the item layout for each view in the RecyclerView
        View view = LayoutInflater.from(context).inflate(R.layout.list_item_tag, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        // Bind the data (String) to the TextView
        holder.textView.setText(getWord(items.get(position)));

        // Set a random color for the ImageView's drawable background
        Random random = new Random();
        int color = colors[getNumber(items.get(position))]; // Get a random color from the array

        // Get the drawable and apply the color filter
        Drawable drawable = context.getDrawable(R.drawable.tag);
        if (drawable != null) {
            drawable.setColorFilter(color, PorterDuff.Mode.SRC_IN); // Apply color filter to the drawable
            holder.tag.setImageDrawable(drawable); // Set the modified drawable
        }
    }
    public static String getWord(String input) {
        if (input == null || !input.contains("|~|")) {
            throw new IllegalArgumentException("Invalid input format");
        }
        return input.split("\\|~\\|")[0];
    }

    // Function to extract the number
    public static int getNumber(String input) {
        if (input == null || !input.contains("|~|")) {
            throw new IllegalArgumentException("Invalid input format");
        }
        String numberPart = input.split("\\|~\\|")[1];
        return Integer.parseInt(numberPart); // Convert number to int
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textView;
        ImageView tag;

        public ViewHolder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.tag_name); // Initialize TextView
            tag = itemView.findViewById(R.id.tag); // Initialize ImageView
        }
    }
}
