package com.example.recorderchunks.Encryption;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import android.util.Base64;

public class SHA256HashGenerator {

    // Generate SHA-256 hash from encrypted file and salt string
    public static String generateSHA256Hash(byte[] encryptedAudio, String saltString) throws Exception {
        // Convert salt string to bytes
        byte[] salt = saltString.getBytes(StandardCharsets.UTF_8);

        // Combine salt and encrypted audio (Salt || Encrypted Audio)
        byte[] combinedData = new byte[salt.length + encryptedAudio.length];
        System.arraycopy(salt, 0, combinedData, 0, salt.length);
        System.arraycopy(encryptedAudio, 0, combinedData, salt.length, encryptedAudio.length);

        // Generate SHA-256 hash
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(combinedData);

        // Convert hash to Base64 string (API 24 compatible)
        return Base64.encodeToString(hash, Base64.NO_WRAP);
    }


    // Read file content into byte array
    private static byte[] readFile(File file) throws Exception {
        FileInputStream fis = new FileInputStream(file);
        byte[] data = new byte[(int) file.length()];
        fis.read(data);
        fis.close();
        return data;
    }
    }
