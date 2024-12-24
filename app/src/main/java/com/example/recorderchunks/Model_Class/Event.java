package com.example.recorderchunks.Model_Class;

// Event Model Class
public class Event {
    private int id;
    private String title;
    private String description;
    private String creationDate;
    private String creationTime;
    private String eventDate;
    private String eventTime;

    public String audioPath;

    // Constructor
    public Event(int id, String title, String description, String creationDate, String creationTime, String eventDate, String eventTime, String audioPath) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.creationDate = creationDate;
        this.creationTime = creationTime;
        this.eventDate = eventDate;
        this.eventTime = eventTime;
        this.audioPath=audioPath;
    }

    // Getters and Setters
    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getCreationDate() { return creationDate; }
    public String getCreationTime() { return creationTime; }
    public String getEventDate() { return eventDate; }
    public String getEventTime() { return eventTime; }
    public String getAudioPath() { return audioPath; }
    public void setId(int id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setCreationDate(String creationDate) { this.creationDate = creationDate; }
    public void setCreationTime(String creationTime) { this.creationTime = creationTime; }
    public void setEventDate(String eventDate) { this.eventDate = eventDate; }
    public void setEventTime(String eventTime) { this.eventTime = eventTime; }
}
