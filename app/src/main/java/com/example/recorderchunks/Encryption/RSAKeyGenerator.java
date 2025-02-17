package com.example.recorderchunks.Encryption;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import android.util.Base64;
import android.util.Log;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class RSAKeyGenerator {

    private static final String TAG = "RSAKeyGenerator";

    public static KeyPairPEM generateAndFormatRSAKeys() throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        PublicKey publicKey = keyPair.getPublic();
        PrivateKey privateKey = keyPair.getPrivate();

        String publicKeyPEM = formatKey(publicKey.getEncoded(), "PUBLIC KEY");
        String privateKeyPEM = formatKey(privateKey.getEncoded(), "PRIVATE KEY");

        return new KeyPairPEM(publicKeyPEM, privateKeyPEM);
    }

    private static String formatKey(byte[] keyBytes, String keyType) {
        String base64Key = Base64.encodeToString(keyBytes, Base64.NO_WRAP);
        StringBuilder pemFormattedKey = new StringBuilder();
        pemFormattedKey.append("-----BEGIN ").append(keyType).append("-----\n");
        int index = 0;
        while (index < base64Key.length()) {
            pemFormattedKey.append(base64Key, index, Math.min(index + 64, base64Key.length()));
            pemFormattedKey.append("\n");
            index += 64;
        }
        pemFormattedKey.append("-----END ").append(keyType).append("-----");
        return pemFormattedKey.toString();
    }

    public static PublicKey getPublicKeyFromPEM(String pem) throws Exception {
        String base64Key = pem.replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        Log.d(TAG, "Base64 Public Key: " + base64Key);
        byte[] keyBytes = Base64.decode(base64Key, Base64.DEFAULT);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(keySpec);
    }

    public static PrivateKey getPrivateKeyFromPEM(String pem) throws Exception {
        String base64Key = pem.replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        Log.d(TAG, "Base64 Private Key: " + base64Key);
        byte[] keyBytes = Base64.decode(base64Key, Base64.DEFAULT);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(keySpec);
    }
    public static PublicKey getPublicKeyFromString(String keyString) {
        try {
            // Remove any unnecessary new lines or headers
            keyString = keyString.replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", ""); // Remove all whitespaces

            // Decode Base64 to get the byte array
            byte[] keyBytes = Base64.decode(keyString, Base64.DEFAULT);

            // Generate PublicKey from encoded key bytes
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA"); // Change to "EC", "DSA" if needed
            return keyFactory.generatePublic(keySpec);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    public static String encrypt(String plainText, PublicKey publicKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] encryptedBytes = cipher.doFinal(plainText.getBytes());
        return Base64.encodeToString(encryptedBytes, Base64.DEFAULT);
    }

    public static String decrypt(String encryptedText, PrivateKey privateKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] decryptedBytes = cipher.doFinal(Base64.decode(encryptedText, Base64.DEFAULT));
        return new String(decryptedBytes);
    }

    public static SecretKey generateAESKey_local(String serverPublicKeyPEM, String userPublicKeyPEM, String additionalString) throws Exception {
        // Convert PEM strings to PublicKey objects
        PublicKey serverPublicKey = getPublicKeyFromPEM(serverPublicKeyPEM);
        PublicKey userPublicKey = getPublicKeyFromPEM(userPublicKeyPEM);

        // Concatenate the encoded public keys and the additional string
        byte[] serverKeyBytes = serverPublicKey.getEncoded();
        byte[] userKeyBytes = userPublicKey.getEncoded();
        byte[] additionalBytes = additionalString.getBytes();

        byte[] combined = new byte[serverKeyBytes.length + userKeyBytes.length + additionalBytes.length];
        System.arraycopy(serverKeyBytes, 0, combined, 0, serverKeyBytes.length);
        System.arraycopy(userKeyBytes, 0, combined, serverKeyBytes.length, userKeyBytes.length);
        System.arraycopy(additionalBytes, 0, combined, serverKeyBytes.length + userKeyBytes.length, additionalBytes.length);

        // Hash the combined data
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(combined);

        // Use the first 16 bytes (128 bits) for AES key
        return new SecretKeySpec(hash, 0, 16, "AES");
    }
    public static String generateSHA256Hash(String input) throws NoSuchAlgorithmException {
        // Create a MessageDigest instance for SHA-256
        MessageDigest digest = MessageDigest.getInstance("SHA-256");

        // Convert the input string to bytes and compute the hash
        byte[] hashBytes = digest.digest(input.getBytes());

        // Encode the hash bytes to a Base64 string
        return Base64.encodeToString(hashBytes, Base64.NO_WRAP);
    }
    public static String[] divideString(String input) {
        byte[] inputBytes = input.getBytes();
        int mid = inputBytes.length / 2;

        byte[] part1 = new byte[mid];
        byte[] part2 = new byte[inputBytes.length - mid];

        System.arraycopy(inputBytes, 0, part1, 0, mid);
        System.arraycopy(inputBytes, mid, part2, 0, inputBytes.length - mid);

        return new String[] { new String(part1), new String(part2) };
    }

    public static boolean verifyDivision(String original, String[] parts) {
        String combined = parts[0] + parts[1];
        return original.equals(combined);
    }

    public static String generateSHA256HashWithSalt(String input, String salt) throws NoSuchAlgorithmException {
        // Create a MessageDigest instance for SHA-256
        MessageDigest digest = MessageDigest.getInstance("SHA-256");

        // Convert the salt to bytes and add it to the digest
        digest.update(salt.getBytes());

        // Compute the hash
        byte[] hashBytes = digest.digest(input.getBytes());

        // Encode the hash bytes to a Base64 string
        return Base64.encodeToString(hashBytes, Base64.NO_WRAP);
    }

}

