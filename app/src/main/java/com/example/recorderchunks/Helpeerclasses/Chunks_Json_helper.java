package com.example.recorderchunks.Helpeerclasses;

import android.util.Log;

import com.example.recorderchunks.Model_Class.Chunk_Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class Chunks_Json_helper {


    public static boolean isValidFormat(String jsonString) {
        JSONObject jsonData = parseJson(jsonString);

        // Ensure jsonData is not null before proceeding
        if (jsonData == null) {
            Log.e("Chunks_Json_helper", "Invalid JSON string: Failed to parse JSON");
            return false;
        }

        try {
            // Check for top-level keys
            if (!jsonData.has("recording_name") || !jsonData.has("user_id") || !jsonData.has("chunks")) {
                return false;
            }

            // Check for 'chunks' being a valid array
            JSONArray chunks = jsonData.getJSONArray("chunks");
            for (int i = 0; i < chunks.length(); i++) {
                JSONObject chunk = chunks.getJSONObject(i);
                if (chunk.has("chunk_id") || chunk.has("status") || chunk.has("transcription")) {
                    return true;
                }
            }

            return false; // No valid chunks found
        } catch (JSONException e) {
            Log.e("Chunks_Json_helper", "JSON processing error: " + e.getMessage());
            return false; // Invalid format
        }
    }


    // Function to extract chunk data into a list of ChunkResponse objects
    public static List<Chunk_Response> getChunkList(String jsonString) {
        JSONObject jsonData=parseJson(jsonString);
        List<Chunk_Response> chunkList = new ArrayList<>();
        try {
            JSONArray chunks = jsonData.getJSONArray("chunks");
            for (int i = 0; i < chunks.length(); i++) {
                JSONObject chunk = chunks.getJSONObject(i);
                String chunkId = chunk.getString("chunk_id");
                String status = chunk.getString("status");
                String transcription="";
                try
                {
                    transcription= chunk.getString("transcription");

                }catch (Exception e)
                {
                    transcription= "not available";
                }

                chunkList.add(new Chunk_Response(chunkId, status, transcription));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return chunkList;
    }

    // Function to parse JSON from a string
    public static JSONObject parseJson(String jsonString) {
        try {
            return new JSONObject(jsonString);
        } catch (JSONException e) {
            e.printStackTrace();
            return null; // Return null if parsing fails
        }
    }

    public static int getCompletedChunkCount(String jsonString) {
        JSONObject jsonData=parseJson(jsonString);
        int count = 0;
        try {
            JSONArray chunks = jsonData.getJSONArray("chunks");
            for (int i = 0; i < chunks.length(); i++) {
                JSONObject chunk = chunks.getJSONObject(i);
                String status = chunk.getString("status");
                if ("completed".equalsIgnoreCase(status)) {
                    count++;
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return count;
    }

    public static int getTotalChunkCount(String jsonString) {
        JSONObject jsonData=parseJson(jsonString);
        int count = 0;
        try {
            JSONArray chunks = jsonData.getJSONArray("chunks");
            return chunks.length();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return count;
    }

    // Main method for testing
}
