package com.example.recorderchunks.Helpeerclasses;

import static com.example.recorderchunks.Encryption.RSAKeyGenerator.generateSHA256Hash;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.example.recorderchunks.Model_Class.ChunkTranscription;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Chunks_Database_Helper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "transcription.db";
    private static final int DATABASE_VERSION = 2;

    private static final String TABLE_CHUNKS = "ChunkTranscription";
    private static final String COL_CHUNK_ID = "chunkId"; // String-based ID
    private static final String COL_RECORDING_ID = "recordingId";
    private static final String COL_STATUS = "transcriptionStatus";
    private static final String COL_TRANSCRIPTION = "transcription";
    private static final String COL_CHUNK_PATH = "chunkPath";
    private static final String COL_UNIQUE_RECORDING_NAME = "uniqueRecordingName"; // New column

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
                + COL_CHUNK_PATH + " TEXT, "
                + COL_UNIQUE_RECORDING_NAME + " TEXT)";
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CHUNKS);
        onCreate(db);
    }
    @SuppressLint("Range")
    public String getUniqueRecordingNameByRecordingId(int recordingId) {
        String uniqueRecordingName = null;
        SQLiteDatabase db = null;
        Cursor cursor = null;

        try {
            // Open the database for reading
            db = this.getReadableDatabase();

            // Query to fetch the unique recording name for the given recording ID
            String query = "SELECT " + COL_UNIQUE_RECORDING_NAME + " FROM " + TABLE_CHUNKS +
                    " WHERE " + COL_RECORDING_ID + " = ? LIMIT 1";

            // Execute the query
            cursor = db.rawQuery(query, new String[]{String.valueOf(recordingId)});

            // Check if a result exists and retrieve the unique recording name
            if (cursor != null && cursor.moveToFirst()) {
                uniqueRecordingName = cursor.getString(cursor.getColumnIndex(COL_UNIQUE_RECORDING_NAME));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // Ensure the cursor and database are closed to avoid memory leaks
            if (cursor != null) {
                cursor.close();
            }
            if (db != null) {
                db.close();
            }
        }

        return uniqueRecordingName;
    }

    public long addChunk(String chunkId, int recordingId, String status, String path, String uniqueRecordingName) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_CHUNK_ID, chunkId);
        values.put(COL_RECORDING_ID, recordingId);
        values.put(COL_STATUS, status);
        values.put(COL_CHUNK_PATH, path);
        values.put(COL_UNIQUE_RECORDING_NAME, uniqueRecordingName); // Include new column
        return db.insert(TABLE_CHUNKS, null, values);
    }
    public void updateChunkStatus(String chunkId, String uniqueRecordingName, String status) {
        SQLiteDatabase db = null;

        try {
            // Open the database for writing
            db = this.getWritableDatabase();

            // Create a ContentValues object to hold the new status
            ContentValues values = new ContentValues();
            values.put(COL_STATUS, status);

            // Define the WHERE clause to match both chunk_id and unique_recording_name
            String whereClause = COL_CHUNK_ID + " = ? AND " + COL_UNIQUE_RECORDING_NAME + " = ?";
            String[] whereArgs = {chunkId, uniqueRecordingName};

            // Perform the update
            int rowsAffected = db.update(TABLE_CHUNKS, values, whereClause, whereArgs);

            // Log the result
            if (rowsAffected > 0) {
                Log.d("DatabaseUpdate", "Status updated to '" + status + "' for chunk_id: " + chunkId + " and unique_recording_name: " + uniqueRecordingName);
            } else {
                Log.e("DatabaseUpdate", "Failed to update status for chunk_id: " + chunkId + " and unique_recording_name: " + uniqueRecordingName);
            }
        } catch (Exception e) {
            Log.e("DatabaseError", "Error updating chunk status", e);
        } finally {
            // Ensure the database is closed to prevent memory leaks
            if (db != null && db.isOpen()) {
                db.close();
            }
        }
    }
    public void updateChunkTranscription(String chunkId, String uniqueRecordingName, String transcription) {
        SQLiteDatabase db = null;

        try {
            // Open the database for writing
            db = this.getWritableDatabase();

            // Create a ContentValues object to hold the new values
            ContentValues values = new ContentValues();
            values.put(COL_TRANSCRIPTION, transcription);
            values.put(COL_STATUS, "completed");

            // Update the database where both chunk_id and unique_recording_name match
            String whereClause = COL_CHUNK_ID + " = ? AND " + COL_UNIQUE_RECORDING_NAME + " = ?";
            String[] whereArgs = {chunkId, uniqueRecordingName};

            db.update(TABLE_CHUNKS, values, whereClause, whereArgs);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // Ensure the database is closed
            if (db != null) {
                db.close();
            }
        }
    }

    public ChunkTranscription getChunkByPath(String chunkPath) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_CHUNKS, null, COL_CHUNK_PATH + " = ?", new String[]{chunkPath}, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            ChunkTranscription chunk = new ChunkTranscription();
            chunk.setChunkId(cursor.getString(cursor.getColumnIndexOrThrow(COL_CHUNK_ID)));
            chunk.setRecordingId(cursor.getInt(cursor.getColumnIndexOrThrow(COL_RECORDING_ID)));
            chunk.setTranscriptionStatus(cursor.getString(cursor.getColumnIndexOrThrow(COL_STATUS)));
            chunk.setTranscription(cursor.getString(cursor.getColumnIndexOrThrow(COL_TRANSCRIPTION)));
            chunk.setChunkPath(cursor.getString(cursor.getColumnIndexOrThrow(COL_CHUNK_PATH)));
            chunk.setUniqueRecordingName(cursor.getString(cursor.getColumnIndexOrThrow(COL_UNIQUE_RECORDING_NAME)));
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
                chunk.setUniqueRecordingName(cursor.getString(cursor.getColumnIndexOrThrow(COL_UNIQUE_RECORDING_NAME)));
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
                                ", Path: " + chunk.getChunkPath()+
                                ", Unique Recording name: " + chunk.getUniqueRecordingName());

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
                chunk.setUniqueRecordingName(cursor.getString(cursor.getColumnIndexOrThrow(COL_UNIQUE_RECORDING_NAME)));
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

    public void deleteChunksByPath(String chunkPath) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_CHUNKS, COL_CHUNK_PATH + " = ?", new String[]{chunkPath});
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


                // Check if chunk_id already exists
                Cursor cursor = db.query(
                        TABLE_CHUNKS,
                        new String[]{COL_CHUNK_PATH},
                        COL_CHUNK_PATH + " = ?",
                        new String[]{chunkPath},
                        null, null, null
                );

                if (cursor != null && cursor.getCount() > 0) {
                    // Chunk ID already exists, skip insertion
                    Log.d("chunk_path", "Chunk path already exists, skipping: " + chunkPath);
                    cursor.close();
                    continue;
                }

                if (cursor != null) {
                    cursor.close();
                }

                // Insert new chunk
                String unique_recording_hash=replaceSlashes(generateSHA256Hash(uuid + getParentFolder(chunkPath)));
                String Unique_recording_name = replaceSlashes(generateSHA256Hash(uuid + getParentFolder(chunkPath))+"|!~!|"+uuid+"|!~!|"+extractNumberBeforeDot(chunkPath)); // Generate unique chunk ID
                Log.d("chunk_unique_name","unique recording name : "+Unique_recording_name);
                Log.d("chunk_unique_name","search parameter : "+unique_recording_hash);


                ContentValues values = new ContentValues();
                values.put(COL_UNIQUE_RECORDING_NAME, unique_recording_hash);
                values.put(COL_CHUNK_ID,replaceSlashes(Unique_recording_name)); // Unique chunk ID
                values.put(COL_RECORDING_ID, recordingId);
                values.put(COL_STATUS, "not_started"); // Default status
                values.put(COL_CHUNK_PATH, chunkPath);
                values.put(COL_TRANSCRIPTION, "-");
                db.insert(TABLE_CHUNKS, null, values); // Insert chunk
                Log.d("chunk_path", "Inserted new chunk: " + chunkPath);
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
    public static String replaceSlashes(String input) {
        return input.replaceAll("[/\\\\]", "|");
        // [/] matches forward slash
        // [\\\\] matches backslash (escaped because \ is a special character)
    }
    public static String extractNumberBeforeDot(String filePath) {
        StringBuilder number = new StringBuilder();

        // Find the position of the last dot in the file path
        int lastDotIndex = filePath.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return null; // Return null if there's no dot in the path
        }

        // Traverse the string backward from the dot
        for (int i = lastDotIndex - 1; i >= 0; i--) {
            char c = filePath.charAt(i);
            // If it's a digit, add it to the number
            if (Character.isDigit(c)) {
                number.append(c);
            } else if (number.length() > 0) {
                // Break once digits stop (ensures only trailing numbers are captured)
                break;
            }
        }
        return number.reverse().toString();
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

    public static String getParentFolder(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return null;
        }

        File file = new File(filePath);
        return file.getParent();
    }

    public void deleteChunksByRecordingId(int recordingId) {
        SQLiteDatabase db = this.getWritableDatabase();
        List<String> chunkPaths = new ArrayList<>();

        // Step 1: Retrieve all chunk paths for the given recording ID
        Cursor cursor = db.query(TABLE_CHUNKS, new String[]{COL_CHUNK_PATH},
                COL_RECORDING_ID + " = ?", new String[]{String.valueOf(recordingId)},
                null, null, null);

        if (cursor.moveToFirst()) {
            do {
                String chunkPath = cursor.getString(cursor.getColumnIndexOrThrow(COL_CHUNK_PATH));
                chunkPaths.add(chunkPath);
            } while (cursor.moveToNext());
        }
        cursor.close();

        // Step 2: Delete files from storage
        for (String path : chunkPaths) {
            File file = new File(path);
            if (file.exists()) {
                boolean deleted = file.delete();
                if (deleted) {
                    Log.d("ChunkDeletion", "Deleted chunk file: " + path);
                } else {
                    Log.e("ChunkDeletion", "Failed to delete chunk file: " + path);
                }
            } else {
                Log.w("ChunkDeletion", "File does not exist: " + path);
            }
        }

        // Step 3: Delete chunk records from the database
        int rowsDeleted = db.delete(TABLE_CHUNKS, COL_RECORDING_ID + " = ?",
                new String[]{String.valueOf(recordingId)});

        Log.d("ChunkDeletion", "Deleted " + rowsDeleted + " chunk records from database for recording ID: " + recordingId);

        db.close();
    }

}
