package com.example.recorderchunks.Helpeerclasses;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.example.recorderchunks.Model_Class.ChunkTranscription;

import java.util.ArrayList;
import java.util.List;

public class Chunks_Database_Helper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "transcription.db";
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_CHUNKS = "ChunkTranscription";
    private static final String COL_CHUNK_ID = "chunkId"; // String-based ID
    private static final String COL_RECORDING_ID = "recordingId";
    private static final String COL_STATUS = "transcriptionStatus";
    private static final String COL_TRANSCRIPTION = "transcription";
    private static final String COL_CHUNK_PATH = "chunkPath";

    public Chunks_Database_Helper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_TABLE = "CREATE TABLE " + TABLE_CHUNKS + " ("
                + COL_CHUNK_ID + " TEXT, "
                + COL_RECORDING_ID + " INTEGER, "
                + COL_STATUS + " TEXT, "
                + COL_TRANSCRIPTION + " TEXT, "
                + COL_CHUNK_PATH + " TEXT)";
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CHUNKS);
        onCreate(db);
    }

    public long addChunk(String chunkId, int recordingId, String status, String path) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_CHUNK_ID, chunkId);
        values.put(COL_RECORDING_ID, recordingId);
        values.put(COL_STATUS, status);
        values.put(COL_CHUNK_PATH, path);
        return db.insert(TABLE_CHUNKS, null, values);
    }

    public void updateChunkStatus(String chunkId, String status) {
        SQLiteDatabase db = null;
        try {
            db = this.getWritableDatabase(); // Ensure we are working with an open database
            ContentValues values = new ContentValues();
            values.put(COL_STATUS, status);

            // Update the database
            int rowsAffected = db.update(TABLE_CHUNKS, values, COL_CHUNK_ID + " = ?", new String[]{chunkId});

            // Log the result
            if (rowsAffected > 0) {
                Log.d("DatabaseUpdate", "Status updated to '" + status + "' for chunk ID: " + chunkId);
            } else {
                Log.e("DatabaseUpdate", "Failed to update status for chunk ID: " + chunkId);
            }
        } catch (Exception e) {
            Log.e("DatabaseError", "Error updating chunk status", e);
        } finally {
            if (db != null && db.isOpen()) {
                db.close();  // Close database only if it is open
            }
        }
    }

    public void updateChunkTranscription(String chunkId, String transcription) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_TRANSCRIPTION, transcription);
        values.put(COL_STATUS, "completed");
        db.update(TABLE_CHUNKS, values, COL_CHUNK_ID + " = ?", new String[]{chunkId});
        db.close();
    }

    public ChunkTranscription getChunkById(String chunkId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_CHUNKS, null, COL_CHUNK_ID + " = ?", new String[]{chunkId}, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            ChunkTranscription chunk = new ChunkTranscription();
            chunk.setChunkId(cursor.getString(cursor.getColumnIndexOrThrow(COL_CHUNK_ID)));
            chunk.setRecordingId(cursor.getInt(cursor.getColumnIndexOrThrow(COL_RECORDING_ID)));
            chunk.setTranscriptionStatus(cursor.getString(cursor.getColumnIndexOrThrow(COL_STATUS)));
            chunk.setTranscription(cursor.getString(cursor.getColumnIndexOrThrow(COL_TRANSCRIPTION)));
            chunk.setChunkPath(cursor.getString(cursor.getColumnIndexOrThrow(COL_CHUNK_PATH)));
            cursor.close();
            return chunk;
        }
        db.close();
        return null;
    }

    public List<ChunkTranscription> getAllChunks() {
        List<ChunkTranscription> chunks = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_CHUNKS, null, null, null, null, null, null);

        if (cursor.moveToFirst()) {
            do {
                ChunkTranscription chunk = new ChunkTranscription();
                chunk.setChunkId(cursor.getString(cursor.getColumnIndexOrThrow(COL_CHUNK_ID)));
                chunk.setRecordingId(cursor.getInt(cursor.getColumnIndexOrThrow(COL_RECORDING_ID)));
                chunk.setTranscriptionStatus(cursor.getString(cursor.getColumnIndexOrThrow(COL_STATUS)));
                chunk.setTranscription(cursor.getString(cursor.getColumnIndexOrThrow(COL_TRANSCRIPTION)));
                chunk.setChunkPath(cursor.getString(cursor.getColumnIndexOrThrow(COL_CHUNK_PATH)));
                chunks.add(chunk);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return chunks;
    }
    public void logAllChunks() {
        List<ChunkTranscription> chunks = getAllChunks(); // Retrieve all chunks from the database

        if (chunks.isEmpty()) {
            Log.d("ChunkTable", "No chunks found in the database.");
        } else {
            for (ChunkTranscription chunk : chunks) {
                Log.d("ChunkTable",
                        "Chunk ID: " + chunk.getChunkId() +
                                ", Recording ID: " + chunk.getRecordingId() +
                                ", Status: " + chunk.getTranscriptionStatus() +
                                ", Transcription: " + chunk.getTranscription() +
                                ", Path: " + chunk.getChunkPath());
            }
        }
    }

    // New: Get all chunks by recording ID
    public List<ChunkTranscription> getChunksByRecordingId(int recordingId) {
        List<ChunkTranscription> chunks = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Log.d("getChunksByRecordingId", "Querying for recording ID: " + recordingId);

        Cursor cursor = db.query(TABLE_CHUNKS, null, COL_RECORDING_ID + " = ?",
                new String[]{String.valueOf(recordingId)}, null, null, null);

        Log.d("getChunksByRecordingId", "Cursor count: " + cursor.getCount());

        if (cursor.moveToFirst()) {
            do {
                ChunkTranscription chunk = new ChunkTranscription();
                chunk.setChunkId(cursor.getString(cursor.getColumnIndexOrThrow(COL_CHUNK_ID)));
                chunk.setRecordingId(cursor.getInt(cursor.getColumnIndexOrThrow(COL_RECORDING_ID)));
                chunk.setTranscriptionStatus(cursor.getString(cursor.getColumnIndexOrThrow(COL_STATUS)));
                chunk.setTranscription(cursor.getString(cursor.getColumnIndexOrThrow(COL_TRANSCRIPTION)));
                chunk.setChunkPath(cursor.getString(cursor.getColumnIndexOrThrow(COL_CHUNK_PATH)));
                chunks.add(chunk);

                // Log each chunk
                Log.d("getChunksByRecordingId", "Retrieved chunk: " + chunk.getChunkId());
            } while (cursor.moveToNext());
        } else {
            Log.d("getChunksByRecordingId", "No chunks found for recording ID: " + recordingId);
        }

        cursor.close();
        return chunks;
    }

    // New: Delete all chunks by recording ID
    public void deleteChunksByRecordingId(int recordingId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_CHUNKS, COL_RECORDING_ID + " = ?", new String[]{String.valueOf(recordingId)});
        db.close();
    }

    public boolean addChunksBatch(List<String> chunkPaths, int recordingId, String uuid) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction(); // Start transaction
        try {
            // Step 2: Insert new chunks
            for (String chunkPath : chunkPaths) {
                if (chunkPath.contains("chunks already created") || chunkPath.equals("chunks already created")) {
                    // Skip this chunk if the path indicates no new chunks
                    continue;
                }

                String chunkId = uuid + chunkPath; // Generate unique chunk ID

                // Check if chunk_id already exists
                Cursor cursor = db.query(
                        TABLE_CHUNKS,
                        new String[]{COL_CHUNK_ID},
                        COL_CHUNK_ID + " = ?",
                        new String[]{chunkId},
                        null, null, null
                );

                if (cursor != null && cursor.getCount() > 0) {
                    // Chunk ID already exists, skip insertion
                    Log.d("chunk_path", "Chunk ID already exists, skipping: " + chunkId);
                    cursor.close();
                    continue;
                }

                if (cursor != null) {
                    cursor.close();
                }

                // Insert new chunk
                ContentValues values = new ContentValues();
                values.put(COL_CHUNK_ID, chunkId); // Unique chunk ID
                values.put(COL_RECORDING_ID, recordingId);
                values.put(COL_STATUS, "not_started"); // Default status
                values.put(COL_CHUNK_PATH, chunkPath);
                values.put(COL_TRANSCRIPTION, "-");
                db.insert(TABLE_CHUNKS, null, values); // Insert chunk
                Log.d("chunk_path", "Inserted new chunk: " + chunkId);
            }

            db.setTransactionSuccessful(); // Commit transaction
            return true; // Return true if all chunks are processed successfully
        } catch (Exception e) {
            Log.e("chunk_path", "Error in batch insertion: " + e.getMessage());
            return false; // Return false if an error occurs
        } finally {
            db.endTransaction(); // Ensure transaction ends
            db.close(); // Close the database
        }
    }
    public int getChunksCountByRecordingIdAndStatus(int recordingId, String status) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT COUNT(*) FROM " + TABLE_CHUNKS +
                " WHERE " + COL_RECORDING_ID + " = ? AND " + COL_STATUS + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(recordingId), status});
        int count = 0;

        if (cursor != null && cursor.moveToFirst()) {
            count = cursor.getInt(0);
            cursor.close();
        }

        db.close();
        return count;
    }
    public int getTotalChunksByRecordingId(int recordingId) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT COUNT(*) FROM " + TABLE_CHUNKS + " WHERE " + COL_RECORDING_ID + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(recordingId)});
        int totalChunks = 0;

        if (cursor != null && cursor.moveToFirst()) {
            totalChunks = cursor.getInt(0);
            cursor.close();
        }

        db.close();
        return totalChunks;
    }



}
