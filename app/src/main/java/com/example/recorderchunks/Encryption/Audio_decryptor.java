package com.example.recorderchunks.Encryption;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;

import javax.crypto.Cipher;

public class Audio_decryptor {
    static SharedPreferences prefs ;
    public static PrivateKey convertPEMToPrivateKey(String privateKeyPEM) throws Exception {
        // Remove PEM headers and newlines
        privateKeyPEM = privateKeyPEM
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");

        // Decode Base64
        byte[] decodedKey = Base64.decode(privateKeyPEM, Base64.DEFAULT);

        // Generate PrivateKey
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decodedKey);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(keySpec);
    }
    public static PublicKey loadPublicKey(String pemKey) throws Exception {
        String publicKeyPEM = pemKey.replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", ""); // Remove whitespace/newlines
        Log.d("encryption",publicKeyPEM);

        byte[] encoded = Base64.decode(publicKeyPEM, Base64.DEFAULT);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encoded);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(keySpec);
    }

    // Decrypts the encrypted text using the private key string
    public static String decryptText(String encryptedText, Context context) throws Exception {
        prefs = context.getSharedPreferences("preferences", Context.MODE_PRIVATE);
        String strord_key = prefs.getString("server_public_key", null);
        PublicKey publicKey = loadPublicKey(strord_key);

        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.DECRYPT_MODE, publicKey);

        byte[] decodedBytes = Base64.decode(encryptedText, Base64.DEFAULT); // Fix for API 24
        byte[] decryptedBytes = cipher.doFinal(decodedBytes);

        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }
}
