package com.example.recorderchunks.Encryption;

public class KeyPairPEM {
    private final String publicKey;
    private final String privateKey;

    public KeyPairPEM(String publicKey, String privateKey) {
        this.publicKey = publicKey;
        this.privateKey = privateKey;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public String getPrivateKey() {
        return privateKey;
    }
}