package com.example.recorderchunks.Model_Class;


public class ChunkTranscription {

    private String chunkId;
    private int recordingId;
    private String transcriptionStatus;
    private String chunkPath;
    private String transcription;

    // Constructor
    public ChunkTranscription(String chunkId, int recordingId, String transcriptionStatus, String chunkPath, String transcription) {
        this.chunkId = chunkId;
        this.recordingId = recordingId;
        this.transcriptionStatus = transcriptionStatus;
        this.chunkPath = chunkPath;
        this.transcription = transcription;
    }

    // Default Constructor
    public ChunkTranscription() {
    }

    // Getters and Setters
    public String getChunkId() {
        return chunkId;
    }

    public void setChunkId(String chunkId) {
        this.chunkId = chunkId;
    }

    public int getRecordingId() {
        return recordingId;
    }

    public void setRecordingId(int recordingId) {
        this.recordingId = recordingId;
    }

    public String getTranscriptionStatus() {
        return transcriptionStatus;
    }

    public void setTranscriptionStatus(String transcriptionStatus) {
        this.transcriptionStatus = transcriptionStatus;
    }

    public String getChunkPath() {
        return chunkPath;
    }

    public void setChunkPath(String chunkPath) {
        this.chunkPath = chunkPath;
    }

    public String getTranscription() {
        return transcription;
    }

    public void setTranscription(String transcription) {
        this.transcription = transcription;
    }

    // toString method for debugging/logging
    @Override
    public String toString() {
        return "ChunkTranscription{" +
                "chunkId='" + chunkId + '\'' +
                ", recordingId=" + recordingId +
                ", transcriptionStatus='" + transcriptionStatus + '\'' +
                ", chunkPath='" + chunkPath + '\'' +
                ", transcription='" + transcription + '\'' +
                '}';
    }
}
