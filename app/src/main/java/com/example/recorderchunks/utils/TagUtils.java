package com.example.recorderchunks.utils;

import java.util.Arrays;
import java.util.List;

public class TagUtils {
    // List of default tags
    public static final List<String> DEFAULT_TAGS = Arrays.asList(
            "Default Tag 1",
            "Default Tag 2"
    );

    // Method to get default tags
    public static List<String> getDefaultTags() {
        return DEFAULT_TAGS;
    }
}
