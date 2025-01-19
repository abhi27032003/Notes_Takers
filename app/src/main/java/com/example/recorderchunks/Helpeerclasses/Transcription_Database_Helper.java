package com.example.recorderchunks.Helpeerclasses;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.example.recorderchunks.Model_Class.TranscriptionHistory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Transcription_Database_Helper extends SQLiteOpenHelper {

    // Database Name and Version
    private static final String DATABASE_NAME = "transcription_history.db";
    private static final int DATABASE_VERSION = 1;

    // Table Name
    private static final String TABLE_NAME = "transcription_history";

    // Column Names
    private static final String COLUMN_RECORDING_ID = "recording_id";
    private static final String COLUMN_TRANSCRIPTION = "transcription";
    private static final String COLUMN_CREATION_TIME = "creation_time";
    private static final String COLUMN_TRANSCRIPTION_ID = "transcription_id";

    // Create Table Query
    private static final String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " (" +
            COLUMN_TRANSCRIPTION_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COLUMN_RECORDING_ID + " TEXT, " +
            COLUMN_TRANSCRIPTION + " TEXT, " +
            COLUMN_CREATION_TIME + " TEXT);";

    public Transcription_Database_Helper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    // Insert a new transcription history record without using a model class
    public long insertTranscription(String recordingId, String transcription, String creationTime) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_RECORDING_ID, recordingId);
        values.put(COLUMN_TRANSCRIPTION, transcription);
        values.put(COLUMN_CREATION_TIME, creationTime);

        return db.insert(TABLE_NAME, null, values);
    }

    // Retrieve a transcription history record by recording ID
    public TranscriptionHistory getTranscriptionById(String recordingId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_NAME, null, COLUMN_RECORDING_ID + "=?",
                new String[]{recordingId}, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            TranscriptionHistory transcriptionHistory = new TranscriptionHistory(
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_RECORDING_ID)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TRANSCRIPTION)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CREATION_TIME)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TRANSCRIPTION_ID))
            );
            cursor.close();
            return transcriptionHistory;
        }

        if (cursor != null) {
            cursor.close();
        }
        return null;
    }

    // Retrieve all transcription history records
    public List<TranscriptionHistory> getAllTranscriptions() {
        List<TranscriptionHistory> transcriptionHistories = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_NAME, null, null, null, null, null, null);

        if (cursor != null) {
            while (cursor.moveToNext()) {
                TranscriptionHistory transcriptionHistory = new TranscriptionHistory(
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_RECORDING_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TRANSCRIPTION)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CREATION_TIME)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TRANSCRIPTION_ID))
                );
                transcriptionHistories.add(transcriptionHistory);
            }
            cursor.close();
        }

        return transcriptionHistories;
    }

    public List<TranscriptionHistory> getAllTranscriptionsByRecordingId(String recordingId) {
        List<TranscriptionHistory> transcriptionHistories = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(
                TABLE_NAME,
                new String[]{COLUMN_TRANSCRIPTION_ID, COLUMN_RECORDING_ID, COLUMN_TRANSCRIPTION, COLUMN_CREATION_TIME},
                COLUMN_RECORDING_ID + "=?",
                new String[]{recordingId},
                null,
                null,
                COLUMN_TRANSCRIPTION_ID + " DESC"
        );

        if (cursor != null) {
            while (cursor.moveToNext()) {
                int transcriptionId = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TRANSCRIPTION_ID));
                String recordingIdValue = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_RECORDING_ID));
                String transcription = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TRANSCRIPTION));
                String creationTime = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CREATION_TIME));

                TranscriptionHistory history = new TranscriptionHistory(recordingIdValue,transcription,creationTime,String.valueOf(transcriptionId) );
                transcriptionHistories.add(history);
            }
            cursor.close();
        }
        else
        {
            Log.d("t_histroy","no transcription history fi=ound");
        }

        Log.d("t_histroy",transcriptionHistories.toString());
        return transcriptionHistories;
    }

    // Update a transcription history record
    public int updateTranscription(String recordingId, String transcription, long creationTime) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TRANSCRIPTION, transcription);
        values.put(COLUMN_CREATION_TIME, creationTime);

        return db.update(TABLE_NAME, values, COLUMN_RECORDING_ID + "=?",
                new String[]{recordingId});
    }

    // Delete a transcription history record
    public int deleteTranscription(String recordingId) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_NAME, COLUMN_RECORDING_ID + "=?", new String[]{recordingId});
    }
}
