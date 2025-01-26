package com.example.recorderchunks.Encryption;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public class AudioEncryptor {

    private static final int AES_KEY_SIZE = 256; // 256-bit AES key
    private static final int GCM_TAG_LENGTH = 128; // GCM tag length in bits

    // 1) Generate a random AES key
    public static SecretKey generateAESKey() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(AES_KEY_SIZE, new SecureRandom());
        return keyGen.generateKey();
    }

    // 2) Encrypt file (audio) with AES (GCM mode)
    public static byte[] encryptFileWithAES(File inputFile, SecretKey aesKey) throws Exception {
        // Read input file into a byte array
        byte[] inputBytes = readFile(inputFile);

        // Initialize Cipher for AES/GCM/NoPadding
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        byte[] iv = new byte[12]; // 96-bit IV for GCM is typical
        new SecureRandom().nextBytes(iv); // Securely generate random IV

        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, aesKey, gcmSpec);

        // Encrypt the input bytes
        byte[] cipherText = cipher.doFinal(inputBytes);

        // Combine IV and ciphertext (IV || cipherText)
        byte[] output = new byte[iv.length + cipherText.length];
        System.arraycopy(iv, 0, output, 0, iv.length);
        System.arraycopy(cipherText, 0, output, iv.length, cipherText.length);

        return output;
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
