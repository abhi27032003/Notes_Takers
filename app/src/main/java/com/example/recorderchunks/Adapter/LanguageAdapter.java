package com.example.recorderchunks.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.recorderchunks.R;

import java.util.List;

public class LanguageAdapter extends RecyclerView.Adapter<LanguageAdapter.LanguageViewHolder> {

    private final List<String> languages;

    public LanguageAdapter(List<String> languages) {
        this.languages = languages;
    }

    @NonNull
    @Override
    public LanguageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_language, parent, false);
        return new LanguageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LanguageViewHolder holder, int position) {
        String language = languages.get(position);
        holder.languageTextView.setText(language);
    }

    @Override
    public int getItemCount() {
        return languages.size();
    }

    public static class LanguageViewHolder extends RecyclerView.ViewHolder {
        TextView languageTextView;

        public LanguageViewHolder(@NonNull View itemView) {
            super(itemView);
            languageTextView = itemView.findViewById(R.id.text_language);
        }
    }
}
