package com.example.recorderchunks.Model_Class;

public class Recording {
    private int recordingId;
    private int eventId;
    private String name;
    private String format;
    private String length;
    private String url;
    private boolean isRecorded;

    private  String date;
    private  String description;
    private  String is_transcribed;



    public Recording(int recordingId, int eventId, String date,String description,String name, String format, String length, String url, boolean isRecorded,String is_transcribed) {
        this.recordingId = recordingId;
        this.date=date;
        this.eventId = eventId;
        this.name = name;
        this.format = format;
        this.length = length;
        this.url = url;
        this.isRecorded = isRecorded;
        this.description=description;
        this.is_transcribed=is_transcribed;
    }

    // Getters
    public String getDescription()
    {
        return  description;
    }
    public String getIs_transcribed()
    {
        return  is_transcribed;
    }
    public int getRecordingId() {
        return recordingId;
    }

    public int getEventId() {
        return eventId;
    }
    public String getDate() {
        return date;
    }

    public String getName() {
        return name;
    }

    public String getFormat() {
        return format;
    }

    public String getLength() {
        return length;
    }

    public String getUrl() {
        return url;
    }

    public boolean isRecorded() {
        return isRecorded;
    }

    public void setRecordingId(int recordingId) {
        this.recordingId = recordingId;
    }

    public void setEventId(int eventId) {
        this.eventId = eventId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public void setLength(String length) {
        this.length = length;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setRecorded(boolean recorded) {
        isRecorded = recorded;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setIs_transcribed(String is_transcribed) {
        this.is_transcribed = is_transcribed;
    }
}
