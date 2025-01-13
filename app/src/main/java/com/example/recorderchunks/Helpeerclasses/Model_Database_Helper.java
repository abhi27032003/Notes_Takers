package com.example.recorderchunks.Helpeerclasses;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class Model_Database_Helper extends SQLiteOpenHelper {

    // Database Name and Version
    private static final String DATABASE_NAME = "models.db";
    private static final int DATABASE_VERSION = 2; // Incremented version

    // Table Name
    private static final String TABLE_NAME = "model_info";

    // Column Names
    private static final String COLUMN_ID = "id"; // Primary Key
    private static final String COLUMN_NAME = "model_name";
    private static final String COLUMN_LANGUAGE = "model_language";
    private static final String COLUMN_URL = "download_url";
    private static final String COLUMN_IS_DOWNLOADED = "is_downloaded";
    private static final String COLUMN_SAVED_PATH = "model_saved_path";

    // Create Table Query
    private static final String CREATE_TABLE =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_NAME + " TEXT NOT NULL, " +
                    COLUMN_LANGUAGE + " TEXT NOT NULL, " +
                    COLUMN_URL + " TEXT NOT NULL, " +
                    COLUMN_IS_DOWNLOADED + " TEXT NOT NULL DEFAULT 'No', " +
                    COLUMN_SAVED_PATH + " TEXT NOT NULL DEFAULT 'Not Available');";

    public Model_Database_Helper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create the table
        db.execSQL(CREATE_TABLE);

        // Insert default records
        insertDefaultRecords(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN " + COLUMN_NAME + " TEXT NOT NULL DEFAULT '';");
        }
    }

    private void insertDefaultRecords(SQLiteDatabase db) {
        insertDefaultRecord(db, "vosk-model-small-en-us-0.15", "English",
                "https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip");
        insertDefaultRecord(db, "vosk-model-small-cn-0.22", "Chinese",
                "https://alphacephei.com/vosk/models/vosk-model-small-cn-0.22.zip");
        insertDefaultRecord(db, "vosk-model-small-fr-0.22", "French",
                "https://alphacephei.com/vosk/models/vosk-model-small-fr-0.22.zip");
        insertDefaultRecord(db, "vosk-model-small-es-0.42", "Spanish",
                "https://alphacephei.com/vosk/models/vosk-model-small-es-0.42.zip");
        insertDefaultRecord(db, "vosk-model-small-hi-0.22", "Hindi",
                "https://alphacephei.com/vosk/models/vosk-model-small-hi-0.22.zip");
    }

    private void insertDefaultRecord(SQLiteDatabase db, String name, String language, String url) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, name);
        values.put(COLUMN_LANGUAGE, language);
        values.put(COLUMN_URL, url);
        values.put(COLUMN_IS_DOWNLOADED, "No"); // Default to "No"
        values.put(COLUMN_SAVED_PATH, "Not Available"); // Default path
        db.insert(TABLE_NAME, null, values);
    }
    public boolean checkModelDownloadedByLanguage(String language) {
        SQLiteDatabase db = this.getReadableDatabase();

        // Query to check if the model exists and its download status by language
        String query = "SELECT " + COLUMN_IS_DOWNLOADED + " FROM " + TABLE_NAME +
                " WHERE " + COLUMN_LANGUAGE + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{language});

        boolean isDownloaded = false;
        if (cursor != null && cursor.moveToFirst()) {
            @SuppressLint("Range") String downloadStatus = cursor.getString(cursor.getColumnIndex(COLUMN_IS_DOWNLOADED));
            isDownloaded = downloadStatus.equals("yes");
            cursor.close();
        }
        return isDownloaded;
    }

    // Method to get model saved path by language
    @SuppressLint("Range")
    public String getModelSavedPathByLanguage(String language) {
        SQLiteDatabase db = this.getReadableDatabase();

        // Query to get the saved path by language
        String query = "SELECT " + COLUMN_SAVED_PATH + " FROM " + TABLE_NAME +
                " WHERE " + COLUMN_LANGUAGE + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{language});

        String savedPath = "Not Available";
        if (cursor != null && cursor.moveToFirst()) {
            savedPath = cursor.getString(cursor.getColumnIndex(COLUMN_SAVED_PATH));
            cursor.close();
        }
        return savedPath;
    }
    @SuppressLint("Range")
    public String getModelDownloadLinkByLanguage(String language) {
        SQLiteDatabase db = this.getReadableDatabase();

        // Query to get the download link by language
        String query = "SELECT " + COLUMN_URL + " FROM " + TABLE_NAME +
                " WHERE " + COLUMN_LANGUAGE + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{language});

        String downloadLink = "Not Available";
        if (cursor != null && cursor.moveToFirst()) {
            downloadLink = cursor.getString(cursor.getColumnIndex(COLUMN_URL));
            cursor.close();
        }
        return downloadLink;
    }
    public void updateModelDownloadStatusAndPath(String language, String downloadStatus, String savedPath) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_IS_DOWNLOADED, downloadStatus);
        values.put(COLUMN_SAVED_PATH, savedPath);

        // Update only the selected model's record by language
        db.update(TABLE_NAME, values, COLUMN_LANGUAGE + " = ?", new String[]{language});
    }
    @SuppressLint("Range")
    public String getModelNameByLanguage(String language) {
        SQLiteDatabase db = this.getReadableDatabase();

        // Query to get the model name by language
        String query = "SELECT " + COLUMN_NAME + " FROM " + TABLE_NAME +
                " WHERE " + COLUMN_LANGUAGE + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{language});

        String modelName = "Not Available";
        if (cursor != null && cursor.moveToFirst()) {
            modelName = cursor.getString(cursor.getColumnIndex(COLUMN_NAME));
            cursor.close();
        }
        return modelName;
    }
    // Other methods (insertModel, updateModel, deleteModel, getAllModels, etc.) remain unchanged
}
