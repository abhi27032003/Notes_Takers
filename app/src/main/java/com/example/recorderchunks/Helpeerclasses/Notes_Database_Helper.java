package com.example.recorderchunks.Helpeerclasses;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.recorderchunks.Model_Class.Note;

import java.util.ArrayList;

public class Notes_Database_Helper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "AI_Generated_Notes.db";
    private static final int DATABASE_VERSION = 1;

    // Table and Column Names
    private static final String TABLE_NAME = "AI_Generated_notes";
    private static final String COLUMN_NOTE_ID = "note_id";
    private static final String COLUMN_NOTE = "note";
    private static final String COLUMN_RECORDING_ID = "recording_id";
    private static final String COLUMN_CREATED_ON = "created_on";

    // SQL query to create the table
    private static final String CREATE_TABLE_QUERY = "CREATE TABLE " + TABLE_NAME + "("
            + COLUMN_NOTE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + COLUMN_NOTE + " TEXT, "
            + COLUMN_RECORDING_ID + " INTEGER, "
            + COLUMN_CREATED_ON+" DATETIME DEFAULT CURRENT_TIMESTAMP);";

    public Notes_Database_Helper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_QUERY);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    // CRUD Operations

    // Add a new note
    public long addNote(String note, int recordingId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NOTE, note);
        values.put(COLUMN_RECORDING_ID, recordingId);
        return db.insert(TABLE_NAME, null, values);
    }

    // Get all notes with a specific recording ID
    @SuppressLint("Range")
    public ArrayList<Note> getNotesByRecordingId(int recordingId) {
        SQLiteDatabase db = this.getReadableDatabase();
        ArrayList<Note> notesList = new ArrayList<>();

        String query = "SELECT * FROM " + TABLE_NAME + " WHERE " + COLUMN_RECORDING_ID + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(recordingId)});

        if (cursor != null && cursor.moveToFirst()) {

            do {
                int noteId = cursor.getInt(cursor.getColumnIndex(COLUMN_NOTE_ID));
                String noteText = cursor.getString(cursor.getColumnIndex(COLUMN_NOTE));
                int recordingIdFromDb = cursor.getInt(cursor.getColumnIndex(COLUMN_RECORDING_ID));
                String createdOn = cursor.getString(cursor.getColumnIndex(COLUMN_CREATED_ON));  // or use COLUMN_CREATED_ON if defined
                Note note = new Note(noteId, noteText, recordingIdFromDb, createdOn);
                notesList.add(note);

            } while (cursor.moveToNext());
            cursor.close();
        }

        return notesList;
    }


    // Update a note
    public int updateNote(int noteId, String newNote) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NOTE, newNote);
        return db.update(TABLE_NAME, values, COLUMN_NOTE_ID + " = ?", new String[]{String.valueOf(noteId)});
    }

    // Delete a note
    public int deleteNote(int noteId) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_NAME, COLUMN_NOTE_ID + " = ?", new String[]{String.valueOf(noteId)});
    }
    public int deleteNotebyevent_id(int eventid) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_NAME, COLUMN_NOTE_ID + " = ?", new String[]{String.valueOf(eventid)});
    }
}
