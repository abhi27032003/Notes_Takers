package com.example.recorderchunks;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

import com.example.recorderchunks.Model_Class.Prompt;

import java.util.ArrayList;
import java.util.List;

public class Prompt_Database_Helper extends SQLiteOpenHelper {

    // Database and Table Information
    private static final String DATABASE_NAME = "PromptsDatabase";
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_PROMPTS = "Prompts";

    // Columns
    private static final String COLUMN_ID = "prompt_id";
    private static final String COLUMN_NAME = "promptname";
    private static final String COLUMN_TEXT = "prompt_text";
    private static final String COLUMN_DATE = "creation_date";



    public Prompt_Database_Helper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);

    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_TABLE = "CREATE TABLE " + TABLE_PROMPTS + " ("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_NAME + " TEXT, "
                + COLUMN_TEXT + " TEXT, "
                + COLUMN_DATE + " TEXT" + ")";
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PROMPTS);
        onCreate(db);
    }

    // Add a new prompt
    public long addPrompt(String name, String text, String date) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, name);
        values.put(COLUMN_TEXT, text);
        values.put(COLUMN_DATE, date);

        long id = db.insert(TABLE_PROMPTS, null, values);
        db.close();
        return id;
    }
    public String[] getAllPromptTexts() {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT " + COLUMN_TEXT + " FROM " + TABLE_PROMPTS;
        Cursor cursor = db.rawQuery(query, null);

        // Create a list to store the prompt texts
        List<String> promptTexts = new ArrayList<>();

        if (cursor.moveToFirst()) {
            do {
                String text = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TEXT));
                promptTexts.add(text);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();

        // Convert the list to an array and return
        return promptTexts.toArray(new String[0]);
    }

    public int updatePrompt(long id, String name, String text, String date) {
        // Get a writable database instance
        SQLiteDatabase db = this.getWritableDatabase();

        // Prepare the new values to be updated
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, name);
        values.put(COLUMN_TEXT, text);
        values.put(COLUMN_DATE, date);

        // Update the prompt in the database where the ID matches
        int rowsAffected = db.update(
                TABLE_PROMPTS,              // Table name
                values,                     // New values
                COLUMN_ID + " = ?",        // WHERE clause
                new String[]{String.valueOf(id)} // WHERE arguments
        );

        // Close the database connection
        db.close();

        // Return the number of rows affected
        return rowsAffected;
    }

    // Delete a prompt by ID
    public int deletePrompt(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rowsDeleted = db.delete(TABLE_PROMPTS, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
        return rowsDeleted;
    }

    // Get prompt details by ID
    public Prompt getPromptById(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_PROMPTS,
                new String[]{COLUMN_ID, COLUMN_NAME, COLUMN_TEXT, COLUMN_DATE},
                COLUMN_ID + " = ?",
                new String[]{String.valueOf(id)},
                null, null, null);

        if (cursor != null) {
            cursor.moveToFirst();
            Prompt prompt = new Prompt(
                    cursor.getInt(0),
                    cursor.getString(1),
                    cursor.getString(2),
                    cursor.getString(3));
            cursor.close();
            return prompt;
        }
        return null;
    }

    // Get all prompts
    public List<Prompt> getAllPrompts() {
        List<Prompt> promptList = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_PROMPTS;

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                Prompt prompt = new Prompt(
                        cursor.getInt(0),
                        cursor.getString(1),
                        cursor.getString(2),
                        cursor.getString(3));
                promptList.add(prompt);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return promptList;
    }

    // Prompt class to represent a single prompt entry

}
