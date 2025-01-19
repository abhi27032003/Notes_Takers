package com.example.recorderchunks.Helpeerclasses;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TagStorage {

    private static final String PREFS_NAME = "TagsStorage";
    private SharedPreferences sharedPreferences;
    private Gson gson;

    public TagStorage(Context context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    // Save tags for a specific event ID
    public void saveTags(String eventId, List<String> availableTags, List<String> selectedTags) {
        // Save available tags
        String availableTagsKey = "available_tags_" + eventId;
        String availableTagsJson = gson.toJson(availableTags);
        sharedPreferences.edit().putString(availableTagsKey, availableTagsJson).apply();

        // Save selected tags
        String selectedTagsKey = "selected_tags_" + eventId;
        String selectedTagsJson = gson.toJson(selectedTags);
        sharedPreferences.edit().putString(selectedTagsKey, selectedTagsJson).apply();
    }

    // Get all tags for a specific event ID
    public Map<String, List<String>> getTags(String eventId) {
        Map<String, List<String>> tagData = new HashMap<>();

        // Retrieve available tags
        String availableTagsKey = "available_tags_" + eventId;
        String availableTagsJson = sharedPreferences.getString(availableTagsKey, null);
        List<String> availableTags = availableTagsJson != null ? gson.fromJson(availableTagsJson, getType()) : new ArrayList<>();
        tagData.put("available_tags", availableTags);

        // Retrieve selected tags
        String selectedTagsKey = "selected_tags_" + eventId;
        String selectedTagsJson = sharedPreferences.getString(selectedTagsKey, null);
        List<String> selectedTags = selectedTagsJson != null ? gson.fromJson(selectedTagsJson, getType()) : new ArrayList<>();
        tagData.put("selected_tags", selectedTags);

        return tagData;
    }

    // Helper method to get a generic type for deserialization
    private Type getType() {
        return new TypeToken<List<String>>() {}.getType();
    }
}
