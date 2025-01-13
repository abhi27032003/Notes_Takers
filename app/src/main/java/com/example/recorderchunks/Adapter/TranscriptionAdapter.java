package com.example.recorderchunks.Adapter;



import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.recorderchunks.Model_Class.TranscriptionHistory;
import com.example.recorderchunks.R;

import java.util.List;

public class TranscriptionAdapter extends RecyclerView.Adapter<TranscriptionAdapter.TranscriptionViewHolder> {

    private List<TranscriptionHistory> transcriptionList;

    public TranscriptionAdapter(List<TranscriptionHistory> transcriptionList) {
        this.transcriptionList = transcriptionList;
    }

    @NonNull
    @Override
    public TranscriptionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_transcription, parent, false);
        return new TranscriptionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TranscriptionViewHolder holder, int position) {
        TranscriptionHistory history = transcriptionList.get(position);
        holder.tvTranscriptionId.setText("ID: " + history.getTranscriptionId());
        holder.tvTranscription.setText("Transcription: " + history.getTranscription());
        holder.tvCreationTime.setText("Created At: " + history.getCreationTime());
    }

    @Override
    public int getItemCount() {
        return transcriptionList.size();
    }

    static class TranscriptionViewHolder extends RecyclerView.ViewHolder {
        TextView tvTranscriptionId, tvTranscription, tvCreationTime;

        public TranscriptionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTranscriptionId = itemView.findViewById(R.id.tvTranscriptionId);
            tvTranscription = itemView.findViewById(R.id.tvTranscription);
            tvCreationTime = itemView.findViewById(R.id.tvCreationTime);
        }
    }
}
