package com.example.recorderchunks.Model_Class;

import java.util.Date;

public class TranscriptionHistory {

    // Fields for the class
    private String recordingId;
    private String transcription;
    private String creationTime;
    private String transcriptionId;

    // Constructor
    public TranscriptionHistory(String recordingId, String transcription, String creationTime, String transcriptionId) {
        this.recordingId = recordingId;
        this.transcription = transcription;
        this.creationTime = creationTime;
        this.transcriptionId = transcriptionId;
    }

    // Getter and Setter for recordingId
    public String getRecordingId() {
        return recordingId;
    }

    public void setRecordingId(String recordingId) {
        this.recordingId = recordingId;
    }

    // Getter and Setter for transcription
    public String getTranscription() {
        return transcription;
    }

    public void setTranscription(String transcription) {
        this.transcription = transcription;
    }

    // Getter and Setter for creationTime
    public String  getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(String creationTime) {
        this.creationTime = creationTime;
    }

    // Getter and Setter for transcriptionId
    public String getTranscriptionId() {
        return transcriptionId;
    }

    public void setTranscriptionId(String transcriptionId) {
        this.transcriptionId = transcriptionId;
    }

    // Override toString method for easy representation
    @Override
    public String toString() {
        return "TranscriptionHistory{" +
                "recordingId='" + recordingId + '\'' +
                ", transcription='" + transcription + '\'' +
                ", creationTime=" + creationTime +
                ", transcriptionId='" + transcriptionId + '\'' +
                '}';
    }
}
