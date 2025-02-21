package com.example.recorderchunks.Encryption;

import static com.example.recorderchunks.Encryption.AudioEncryptor.convertStringToSecretKey;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import org.json.JSONObject;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public class RSASignature {

    static SharedPreferences prefs ;
    private static PrivateKey loadPrivateKey(String privateKeyPEM) throws Exception {
        // Remove PEM header/footer and decode Base64
        String privateKeyPEMStripped = privateKeyPEM
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", ""); // Remove spaces and newlines

        byte[] keyBytes = Base64.decode(privateKeyPEMStripped, Base64.DEFAULT);

        // Convert to PrivateKey object
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(spec);
    }
    public static String createPackage(byte[] encryptedAudio, String signature) throws Exception {
        JSONObject packageJson = new JSONObject();

        // Convert encrypted audio to Base64 string
        String encodedAudio = Base64.encodeToString(encryptedAudio, Base64.DEFAULT);

        // Add encrypted audio
        packageJson.put("encrypted_audio", encodedAudio);

        // Add size of the encrypted audio (in bytes before Base64 encoding)
       // packageJson.put("encrypted_audio_size", encryptedAudio.length);

        // Add signature
        packageJson.put("signature", signature);
        return packageJson.toString(); // Returns a JSON string
    }
    public static String signData(byte[] data, String privatekeyuser) throws Exception {
        // Hash the data using SHA-256
        PrivateKey privateKey=loadPrivateKey(privatekeyuser);
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(data);

        // Sign the hash using RSA private key
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);
        signature.update(hash);
        byte[] signatureBytes = signature.sign();

        // Encode the signature using Android Base64
        return Base64.encodeToString(signatureBytes, Base64.DEFAULT);
    }
    public static byte[] encryptJsonPackage(String jsonPackage, Context context) throws Exception {
        prefs = context.getSharedPreferences("preferences", Context.MODE_PRIVATE);
        String strord_key = prefs.getString("client_AES_key", null);

        if (strord_key == null) {
            throw new IllegalStateException("AES Key not found in SharedPreferences");
        }

        // Convert the AES key string into a SecretKey
        SecretKey aesKey = convertStringToSecretKey(strord_key);

        // Convert JSON string to bytes
        byte[] inputBytes = jsonPackage.getBytes(StandardCharsets.UTF_8);

        // Initialize Cipher for AES/GCM/NoPadding
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");

        // Generate a secure random IV (12 bytes for AES-GCM)
        byte[] iv = new byte[12]; // 96-bit IV (recommended)
        new SecureRandom().nextBytes(iv);

        // Set up AES-GCM with IV
        GCMParameterSpec gcmSpec = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.ENCRYPT_MODE, aesKey, gcmSpec);

        // Encrypt JSON package
        byte[] cipherText = cipher.doFinal(inputBytes);

        // Combine IV and CipherText (IV || CipherText)
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(iv); // Store IV at the beginning
        outputStream.write(cipherText); // Append encrypted JSON

        return outputStream.toByteArray();
    }

}
