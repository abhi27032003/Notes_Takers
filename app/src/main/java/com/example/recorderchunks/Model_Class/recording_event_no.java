package com.example.recorderchunks.Model_Class;


public class recording_event_no {
    private static recording_event_no instance; // Single instance of the class
    private int recording_event_no = -1; // Default value

    // Private constructor to prevent instantiation
    private recording_event_no() {
    }

    // Public method to provide access to the single instance
    public static recording_event_no getInstance() {
        if (instance == null) {
            synchronized (recording_event_no.class) {
                if (instance == null) {
                    instance = new recording_event_no();
                }
            }
        }
        return instance;
    }

    // Getter for recording_event_no
    public int getRecording_event_no() {
        return recording_event_no;
    }

    // Setter for recording_event_no
    public void setRecording_event_no(int recording_event_no) {
        this.recording_event_no = recording_event_no;
    }
}

