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
            if ( !jsonData.has("chunks")) {
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
                String chunk_unique_name=chunk.getString("unique_name");
                String transcription="";
                try
                {
                    transcription= chunk.getString("transcription");

                }catch (Exception e)
                {
                    transcription= "not available";
                }

                chunkList.add(new Chunk_Response(chunkId, status, transcription,chunk_unique_name));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return chunkList;
    }
    public static JSONObject convertJsonFormat(String jsonString) throws JSONException {
        JSONArray inputArray = new JSONArray(jsonString);
        JSONArray chunksArray = new JSONArray();

        for (int i = 0; i < inputArray.length(); i++) {
            JSONObject fileObj = inputArray.getJSONObject(i);
            String fileName = fileObj.getString("File Name");
            String content = fileObj.getString("Content");

            // Extract chunk_id (number before .txt)
            String[] parts = fileName.split("\\|!~!\\|");
            String chunkNumber = parts[parts.length - 1].replace(".txt", "");

            // Extract unique_name (excluding .txt)
            String uniqueName = parts[0] + "|!~!|" + parts[1] + "|!~!|" + chunkNumber;

            // Create new JSON object
            JSONObject chunkObj = new JSONObject();
            chunkObj.put("chunk_id", chunkNumber);
            chunkObj.put("status", "completed");
            chunkObj.put("unique_name", uniqueName);
            chunkObj.put("transcription", content);

            chunksArray.put(chunkObj);
        }

        // Wrap in the required JSON structure
        JSONObject outputJson = new JSONObject();
        outputJson.put("chunks", chunksArray);

        return outputJson;
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
