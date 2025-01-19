package com.example.recorderchunks.Adapter;

import android.content.Context;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.recorderchunks.Model_Class.Tag;
import com.example.recorderchunks.R;

import java.util.List;

public class TagAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_TAG = 0;
    private static final int VIEW_TYPE_ADD_BUTTON = 1;

    private List<Tag> tagList;
    private Context context;
    private OnTagActionListener onTagActionListener;

    public interface OnTagActionListener {
        void onTagSelected(int position, boolean isSelected);
        void onAddNewTag();
    }

    public TagAdapter(List<Tag> tagList, Context context, OnTagActionListener listener) {
        this.tagList = tagList;
        this.context = context;
        this.onTagActionListener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        return position == tagList.size() ? VIEW_TYPE_ADD_BUTTON : VIEW_TYPE_TAG;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_TAG) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_tag, parent, false);
            return new TagViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.item_add_tag, parent, false);
            return new AddTagViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof TagViewHolder) {
            Tag tag = tagList.get(position);
            ((TagViewHolder) holder).bind(tag, position);
        } else if (holder instanceof AddTagViewHolder) {
            ((AddTagViewHolder) holder).bind();
        }
    }

    @Override
    public int getItemCount() {
        return tagList.size() + 1; // Extra item for the "Add Tag" button
    }

    class TagViewHolder extends RecyclerView.ViewHolder {
        TextView tagName;
        CheckBox tagCheckBox;

        public TagViewHolder(@NonNull View itemView) {
            super(itemView);
            tagName = itemView.findViewById(R.id.tag_name);
            tagCheckBox = itemView.findViewById(R.id.tag_checkbox);
        }

        public void bind(Tag tag, int position) {
            tagName.setText(tag.getName());
            tagCheckBox.setChecked(tag.isSelected());

            tagCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                onTagActionListener.onTagSelected(position, isChecked);
            });
        }
    }

    class AddTagViewHolder extends RecyclerView.ViewHolder {
        Button addTagButton;

        public AddTagViewHolder(@NonNull View itemView) {
            super(itemView);
            addTagButton = itemView.findViewById(R.id.add_tag_button);
        }

        public void bind() {
            addTagButton.setOnClickListener(v -> onTagActionListener.onAddNewTag());
        }
    }
}
