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
                + COL_CHUNK_ID + " TEXT PRIMARY KEY, "
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
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_STATUS, status);
        db.update(TABLE_CHUNKS, values, COL_CHUNK_ID + " = ?", new String[]{chunkId});
    }

    public void updateChunkTranscription(String chunkId, String transcription) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_TRANSCRIPTION, transcription);
        values.put(COL_STATUS, "done");
        db.update(TABLE_CHUNKS, values, COL_CHUNK_ID + " = ?", new String[]{chunkId});
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
        return chunks;
    }

    // New: Get all chunks by recording ID
    public List<ChunkTranscription> getChunksByRecordingId(int recordingId) {
        List<ChunkTranscription> chunks = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_CHUNKS, null, COL_RECORDING_ID + " = ?", new String[]{String.valueOf(recordingId)}, null, null, null);

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
        return chunks;
    }

    // New: Delete all chunks by recording ID
    public void deleteChunksByRecordingId(int recordingId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_CHUNKS, COL_RECORDING_ID + " = ?", new String[]{String.valueOf(recordingId)});
    }

    public boolean addChunksBatch(List<String> chunkPaths, int recordingId, String uuid) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction(); // Start transaction
        try {
            for (String chunkPath : chunkPaths) {
                if (chunkPath.contains("chunks already created") || chunkPath.equals("chunks already created")) {
                    // Skip this chunk if the path indicates no new chunks
                    continue;
                }

                // Check if the chunk already exists
                String query = "SELECT COUNT(*) FROM " + TABLE_CHUNKS +
                        " WHERE " + COL_CHUNK_PATH + " = ? AND " + COL_RECORDING_ID + " = ?";
                Cursor cursor = db.rawQuery(query, new String[]{chunkPath, String.valueOf(recordingId)});

                boolean chunkExists = false;
                if (cursor.moveToFirst()) {
                    chunkExists = cursor.getInt(0) > 0; // Check if count > 0
                }
                cursor.close();

                // If the chunk does not exist, insert it
                if (!chunkExists) {
                    ContentValues values = new ContentValues();
                    values.put(COL_CHUNK_ID, uuid + chunkPath); // Generate unique chunk ID
                    values.put(COL_RECORDING_ID, recordingId);
                    values.put(COL_STATUS, "not_started"); // Default status
                    values.put(COL_CHUNK_PATH, chunkPath);
                    db.insert(TABLE_CHUNKS, null, values); // Insert each chunk
                }
            }
            db.setTransactionSuccessful(); // Commit transaction
            return true; // Return true if all chunks are processed successfully
        } catch (Exception e) {
            Log.e("DBHelper", "Error in batch insertion: " + e.getMessage());
            return false; // Return false if an error occurs
        } finally {
            db.endTransaction(); // End transaction
            db.close();
        }
    }


}
