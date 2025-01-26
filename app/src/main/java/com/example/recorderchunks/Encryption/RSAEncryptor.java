package com.example.recorderchunks.Encryption;

import android.util.Base64;
import android.util.Log;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import javax.crypto.Cipher;

public class RSAEncryptor {

    // Load server's public key from a PEM or another resource
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
    // RSA encrypt the AES key
    public static byte[] encryptAESKeyWithRSAPublicKey(PublicKey pubKey, byte[] aesKeyBytes) throws Exception {
        // Initialize the Cipher with RSA and OAEP padding
        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, pubKey);

        // Encrypt the AES key bytes
        return cipher.doFinal(aesKeyBytes);
    }
}
