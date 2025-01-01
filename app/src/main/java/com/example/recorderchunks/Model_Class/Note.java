package com.example.recorderchunks.Model_Class;
public class Note {

    private int noteId;
    private String note;
    private int recordingId;
    private String createdOn;

    public Note(int noteId, String note, int recordingId, String createdOn) {
        this.noteId = noteId;
        this.note = note;
        this.recordingId = recordingId;
        this.createdOn = createdOn;
    }

    public int getNoteId() {
        return noteId;
    }

    public void setNoteId(int noteId) {
        this.noteId = noteId;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public int getRecordingId() {
        return recordingId;
    }

    public void setRecordingId(int recordingId) {
        this.recordingId = recordingId;
    }

    public String getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(String createdOn) {
        this.createdOn = createdOn;
    }
}
