package com.example.recorderchunks.Helpeerclasses;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.widget.Toast;

import com.example.recorderchunks.Model_Class.Event;
import com.example.recorderchunks.Model_Class.Recording;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

// Database Helper Class
public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "EventDatabase.db";
    private static final int DATABASE_VERSION = 6;

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////       Events       /////////////////////////////////////////////////////////
    public static final String TABLE_NAME = "events";
    public static final String COL_ID = "id";
    public static final String COL_TITLE = "title";
    public static final String COL_DESCRIPTION = "description";
    public static final String COL_CREATION_DATE = "creation_date";
    public static final String COL_CREATION_TIME = "creation_time";
    public static final String COL_EVENT_DATE = "event_date";
    public static final String COL_EVENT_TIME = "event_time";
    public static final String COL_AUDIO_PATH = "audio_path";




    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////     Recordings     /////////////////////////////////////////////////////////
    public static final String TABLE_RECORDINGS = "recordings";
    public static final String COL_RECORDING_ID = "recording_id";
    public static final String COL_EVENT_ID = "event_id"; // Foreign key
    public static final String COL_RECORDING_NAME = "recording_name";
    public static final String COL_FORMAT = "format";
    public static final String COL_LENGTH = "length";
    public static final String COL_URL = "url";
    public static final String COL_DES = "description";
    public static final String COL_DATE = "creation_d_and_t";
    public static final String COL_IS_RECORDED = "is_recorded";
    public static final String COL_IS_TRANSCRIBED= "is_transcribed";
    public static final String COL_IS_TRANSCRIBED_API= "is_transcribed_api";
    public static final String COL_DES_API = "description_api";
    public static final String COL_LANGUAGE = "language";




    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + "("
                + COL_ID + " INTEGER PRIMARY KEY ,"
                + COL_TITLE + " TEXT,"
                + COL_DESCRIPTION + " TEXT,"
                + COL_CREATION_DATE + " TEXT,"
                + COL_CREATION_TIME + " TEXT,"
                + COL_EVENT_DATE + " TEXT,"
                + COL_EVENT_TIME + " TEXT,"

                + COL_AUDIO_PATH + " TEXT" +
                ")";
        db.execSQL(CREATE_TABLE);

        ////////////////////////////
        String CREATE_RECORDINGS_TABLE = "CREATE TABLE " + TABLE_RECORDINGS + "("
                + COL_RECORDING_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COL_EVENT_ID + " INTEGER,"
                + COL_RECORDING_NAME + " TEXT,"
                + COL_FORMAT + " TEXT,"
                + COL_LENGTH + " TEXT,"
                + COL_URL + " TEXT,"
                + COL_DATE + " TEXT,"
                + COL_DES + " TEXT,"
                + COL_IS_RECORDED + " INTEGER,"
                + COL_IS_TRANSCRIBED + " TEXT,"
                + COL_DES_API + " TEXT,"
                + COL_IS_TRANSCRIBED_API+ " TEXT,"
                + COL_LANGUAGE+ " TEXT,"



                + "FOREIGN KEY(" + COL_EVENT_ID + ") REFERENCES " + TABLE_NAME + "(" + COL_ID + ")"
                + ")";
        db.execSQL(CREATE_RECORDINGS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_RECORDINGS);

        onCreate(db);
    }

    public boolean insertEvent(int eventid,String title, String description, String creationDate, String creationTime, String eventDate, String eventTime,String audioPath) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_ID, eventid);
        values.put(COL_TITLE, title);
        values.put(COL_DESCRIPTION, description);
        values.put(COL_CREATION_DATE, creationDate);
        values.put(COL_CREATION_TIME, creationTime);
        values.put(COL_EVENT_DATE, eventDate);
        values.put(COL_EVENT_TIME, eventTime);
        values.put(COL_AUDIO_PATH, audioPath);

        long result = db.insert(TABLE_NAME, null, values);
        return result != -1;
    }
    public boolean updateEvent(int eventId, String title, String description, String creationDate, String creationTime, String eventDate, String eventTime, String audioPath) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_TITLE, title);
        values.put(COL_DESCRIPTION, description);
        values.put(COL_CREATION_DATE, creationDate);
        values.put(COL_CREATION_TIME, creationTime);
        values.put(COL_EVENT_DATE, eventDate);
        values.put(COL_EVENT_TIME, eventTime);
        values.put(COL_AUDIO_PATH, audioPath);

        int rowsAffected = db.update(TABLE_NAME, values, COL_ID + " = ?", new String[]{String.valueOf(eventId)});
        return rowsAffected > 0; // Return true if at least one row was updated
    }


    // Retrieve all events
    public List<Event> getAllEvents() {
        List<Event> eventList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_NAME, null, null, null, null, null, COL_ID + " DESC");

        if (cursor.moveToFirst()) {
            do {
                @SuppressLint("Range") Event event = new Event(
                        cursor.getInt(cursor.getColumnIndex(COL_ID)),
                        cursor.getString(cursor.getColumnIndex(COL_TITLE)),
                        cursor.getString(cursor.getColumnIndex(COL_DESCRIPTION)),
                        cursor.getString(cursor.getColumnIndex(COL_CREATION_DATE)),
                        cursor.getString(cursor.getColumnIndex(COL_CREATION_TIME)),
                        cursor.getString(cursor.getColumnIndex(COL_EVENT_DATE)),
                        cursor.getString(cursor.getColumnIndex(COL_EVENT_TIME)),
                        cursor.getString(cursor.getColumnIndex(COL_AUDIO_PATH))

                );
                eventList.add(event);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return eventList;
    }
    public Event getEventById(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(
                TABLE_NAME,
                null,
                COL_ID + " = ?",
                new String[]{String.valueOf(id)},
                null,
                null,
                null
        );

        if (cursor != null && cursor.moveToFirst()) {
            @SuppressLint("Range") Event event = new Event(
                    cursor.getInt(cursor.getColumnIndex(COL_ID)),
                    cursor.getString(cursor.getColumnIndex(COL_TITLE)),
                    cursor.getString(cursor.getColumnIndex(COL_DESCRIPTION)),
                    cursor.getString(cursor.getColumnIndex(COL_CREATION_DATE)),
                    cursor.getString(cursor.getColumnIndex(COL_CREATION_TIME)),
                    cursor.getString(cursor.getColumnIndex(COL_EVENT_DATE)),
                    cursor.getString(cursor.getColumnIndex(COL_EVENT_TIME)),
                    cursor.getString(cursor.getColumnIndex(COL_AUDIO_PATH))
            );
            cursor.close();
            return event;
        }
        return null;
    }
    // Delete an event by ID
    public boolean deleteEvent(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rowsDeleted = db.delete(TABLE_NAME, COL_ID + " = ?", new String[]{String.valueOf(id)});
        return rowsDeleted > 0;
    }
    public List<String> getDescriptionsByEventId(int eventId) {
        SQLiteDatabase db = this.getReadableDatabase();
        List<String> descriptions = new ArrayList<>();
        String query = "SELECT " + COL_DES + " FROM " + TABLE_RECORDINGS + " WHERE " + COL_EVENT_ID + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(eventId)});

        if (cursor.moveToFirst()) {
            do {
                descriptions.add(cursor.getString(0)); // Retrieve the description from the first column
            } while (cursor.moveToNext());
        }
        cursor.close();
        return descriptions;
    }
    @SuppressLint("Range")
    public boolean deleteRecordingById(int recordingId) {
        SQLiteDatabase db = this.getWritableDatabase();

        // Query to get the file path of the recording
        Cursor cursor = db.query(TABLE_RECORDINGS,
                new String[]{COL_URL},
                COL_RECORDING_ID + "=?",
                new String[]{String.valueOf(recordingId)},
                null, null, null);

        String filePath = null;
        if (cursor != null && cursor.moveToFirst()) {
            filePath = cursor.getString(cursor.getColumnIndex(COL_URL));
            cursor.close();
        }

        // If file path exists, delete the file
        if (filePath != null) {
            File file = new File(filePath);
            if (file.exists()) {
                file.delete();
            }
        }

        // Delete the record from the database
        int result = db.delete(TABLE_RECORDINGS, COL_RECORDING_ID + "=?", new String[]{String.valueOf(recordingId)});
        return result > 0;
    }

    public boolean insertRecording(int eventId,String d_and_t, String description,String name, String format, String length, String url, boolean isRecorded,String is_transcribed,String description_api,String is_transcriptes_api,String language) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_EVENT_ID, eventId);
        values.put(COL_RECORDING_NAME, name);
        values.put(COL_FORMAT, format);
        values.put(COL_LENGTH, length);
        values.put(COL_URL, url);
        values.put(COL_DATE, d_and_t);
        values.put(COL_DES, description);


        values.put(COL_IS_RECORDED, isRecorded ? 1 : 0);
        values.put(COL_IS_TRANSCRIBED,is_transcribed);
        values.put(COL_DES_API, description_api);
        values.put(COL_IS_TRANSCRIBED_API,is_transcriptes_api);
        values.put(COL_LANGUAGE,language);

        long result = db.insert(TABLE_RECORDINGS, null, values);
        return result != -1;
    }
    public ArrayList<Recording> getRecordingsByEventId(int eventId) {
        ArrayList<Recording> recordingsList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(
                TABLE_RECORDINGS,
                null,
                COL_EVENT_ID + " = ?",
                new String[]{String.valueOf(eventId)},
                null,
                null,
                COL_RECORDING_ID + " DESC"
        );

        if (cursor.moveToFirst()) {
            do {
                @SuppressLint("Range") Recording recording = new Recording(
                        cursor.getInt(cursor.getColumnIndex(COL_RECORDING_ID)),
                        cursor.getInt(cursor.getColumnIndex(COL_EVENT_ID)),
                        cursor.getString(cursor.getColumnIndex(COL_DATE)),
                        cursor.getString(cursor.getColumnIndex(COL_DES)),
                        cursor.getString(cursor.getColumnIndex(COL_RECORDING_NAME)),
                        cursor.getString(cursor.getColumnIndex(COL_FORMAT)),
                        cursor.getString(cursor.getColumnIndex(COL_LENGTH)),
                        cursor.getString(cursor.getColumnIndex(COL_URL)),
                        cursor.getInt(cursor.getColumnIndex(COL_IS_RECORDED)) == 1,
                        cursor.getString(cursor.getColumnIndex(COL_IS_TRANSCRIBED)),
                        cursor.getString(cursor.getColumnIndex(COL_DES_API)),
                        cursor.getString(cursor.getColumnIndex(COL_IS_TRANSCRIBED_API)),
                        cursor.getString(cursor.getColumnIndex(COL_LANGUAGE))



                );
                recordingsList.add(recording);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return recordingsList;
    }
    public boolean deleteRecording(int recordingId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rowsDeleted = db.delete(TABLE_RECORDINGS, COL_EVENT_ID + " = ?", new String[]{String.valueOf(recordingId)});
        return rowsDeleted > 0;
    }
    public boolean updateLanguageByRecordingId(int recordingId, String newLanguage) {
        SQLiteDatabase db = null;

        try {

            db = this.getWritableDatabase();

            ContentValues values = new ContentValues();
            values.put(COL_LANGUAGE, newLanguage);
            int rowsAffected = db.update(
                    TABLE_RECORDINGS,        // Table name
                    values,                  // Values to update
                    COL_RECORDING_ID + "=?", // WHERE clause
                    new String[]{String.valueOf(recordingId)} // WHERE arguments
            );

            return rowsAffected > 0;

        } catch (Exception e) {
            Log.e("DatabaseError", "Error updating language: " + e.getMessage());
            return false;
        } finally {
            if (db != null) {
                db.close();
            }
        }
    }

    @SuppressLint("Range")
    public int getNextEventId() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT MAX(" + COL_ID + ") AS max_id FROM " + TABLE_NAME, null);

        int nextId = 1; // Default ID if the table is empty
        if (cursor.moveToFirst() && cursor.getInt(cursor.getColumnIndex("max_id")) >= 0) {
            nextId = cursor.getInt(cursor.getColumnIndex("max_id")) + 1;
        }

        cursor.close();
        return nextId;
    }
    public int getMaxEventIdFromRecordings() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT MAX(" + COL_EVENT_ID + ") FROM " + TABLE_RECORDINGS, null);

        if (cursor != null && cursor.moveToFirst()) {
            int maxEventId = cursor.getInt(0);  // Get the maximum event_id
            cursor.close();
            return maxEventId;  // Return the max event_id found
        }

        cursor.close();
        return 0;  // Return 0 if no records are found
    }
    public boolean deleteAllRecordingsByEventId(int eventId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rowsDeleted = db.delete(TABLE_RECORDINGS, COL_EVENT_ID + " = ?", new String[]{String.valueOf(eventId)});

        return rowsDeleted > 0;  // Returns true if rows were deleted, otherwise false
    }
    public boolean updateRecordingDetails( int recordingId, String description) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_IS_TRANSCRIBED, "yes");
        values.put(COL_DES, description);

        // Define the where clause and arguments
        String whereClause = "recording_id = ?";
        String[] whereArgs = new String[]{String.valueOf(recordingId)};

        // Perform the update
        int rowsAffected = db.update(TABLE_RECORDINGS, values, whereClause, whereArgs);

        // Return true if at least one row is affected, false otherwise
        return rowsAffected > 0;
    }
    public boolean updaterecording_details_api( int recordingId, String description) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_IS_TRANSCRIBED_API, "yes");
        values.put(COL_DES_API, description);

        // Define the where clause and arguments
        String whereClause = "recording_id = ?";
        String[] whereArgs = new String[]{String.valueOf(recordingId)};

        // Perform the update
        int rowsAffected = db.update(TABLE_RECORDINGS, values, whereClause, whereArgs);

        // Return true if at least one row is affected, false otherwise
        return rowsAffected > 0;
    }

}
