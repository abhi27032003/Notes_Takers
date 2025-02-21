package com.example.recorderchunks.Encryption;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import android.util.Base64;
import android.util.Log;


import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class AudioEncryptor {

    private static final int AES_KEY_SIZE = 256; // 256-bit AES key
    private static final int GCM_TAG_LENGTH = 128; // GCM tag length in bits
    static SharedPreferences prefs ;

    // 1) Generate a random AES key

    public static KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        // Initialize the KeyPairGenerator
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048); // You can choose 2048 or 4096 for stronger security

        // Generate the KeyPair
        return keyPairGenerator.generateKeyPair();
    }


    // 2) Encrypt file (audio) with AES (GCM mode)

    public static byte[] encryptFileWithAESkeystring(File inputFile, Context context) throws Exception {
        prefs = context.getSharedPreferences("preferences", Context.MODE_PRIVATE);
        String strord_key = prefs.getString("client_AES_key", null);

        if (strord_key == null) {
            throw new IllegalStateException("AES Key not found in SharedPreferences");
        }

        // Convert the AES key string into a SecretKey
        SecretKey aesKey = convertStringToSecretKey(strord_key);

        // Read input file into a byte array
        byte[] inputBytes = readFile(inputFile);

        // Initialize Cipher for AES/GCM/NoPadding
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");

        // Generate a secure random IV (12 bytes for AES-GCM)
        byte[] iv = new byte[12]; // 96-bit IV (recommended)
        new SecureRandom().nextBytes(iv);

        // Set up AES-GCM with IV
        GCMParameterSpec gcmSpec = new GCMParameterSpec(128, iv); // 128-bit authentication tag
        cipher.init(Cipher.ENCRYPT_MODE, aesKey, gcmSpec);

        // Encrypt data
        byte[] cipherText = cipher.doFinal(inputBytes);

        // Combine IV and CipherText (IV || CipherText)
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(iv); // Store IV at the beginning
        outputStream.write(cipherText); // Append encrypted content

        return outputStream.toByteArray();
    }


    // Convert AES key string to SecretKey
    public static SecretKey convertStringToSecretKey(String aesKeyString) throws Exception {
        // Ensure the key string is properly trimmed (no unwanted spaces)
        Log.d("secret_key",aesKeyString);
        aesKeyString = aesKeyString.trim();

        try {
            // Decode Base64 (API 24 compatible)
            byte[] decodedKey = Base64.decode(aesKeyString, Base64.NO_WRAP);

            // Generate AES SecretKey from decoded bytes
            return new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid Base64 format for AES key!", e);
        }
    }
    public static String getSHA256Hash_of_audiofile(String filePath) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            InputStream fis = new FileInputStream(new File(filePath));
            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = fis.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
            fis.close();

            // Convert hash bytes to hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : digest.digest()) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Read the file into a byte array
    private static byte[] readFile(File file) throws Exception {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[(int) file.length()];
            fis.read(buffer);
            return buffer;
        }
    }
}
