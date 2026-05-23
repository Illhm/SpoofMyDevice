package com.devicespooflab.hooks.security;

import android.content.Context;

import androidx.annotation.NonNull;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public class EncryptedConfigStore {

    public static final String ENCRYPTED_FILE_NAME = "profile.enc";
    private static final String KEY_ALIAS = "spoofmydevice_profile_aes";

    private final Context appContext;

    public EncryptedConfigStore(@NonNull Context context) {
        this.appContext = context.getApplicationContext();
    }

    public void write(@NonNull String plainJson) throws Exception {
        SecretKey secretKey = AndroidKeyStoreAes.getOrCreateAesKey(KEY_ALIAS);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);

        byte[] iv = cipher.getIV();
        if (iv == null || iv.length < 12) {
            throw new IllegalStateException("Invalid AES-GCM IV");
        }

        byte[] encrypted = cipher.doFinal(plainJson.getBytes(StandardCharsets.UTF_8));

        File outFile = new File(appContext.getFilesDir(), ENCRYPTED_FILE_NAME);
        try (FileOutputStream fos = new FileOutputStream(outFile, false)) {
            fos.write(iv.length);
            fos.write(iv);
            fos.write(encrypted);
            fos.flush();
        }
    }

    @NonNull
    public String read() throws Exception {
        File inFile = new File(appContext.getFilesDir(), ENCRYPTED_FILE_NAME);
        if (!inFile.exists()) {
            throw new IllegalStateException("Encrypted profile not found");
        }

        byte[] payload;
        try (FileInputStream fis = new FileInputStream(inFile);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = fis.read(buffer)) != -1) {
                bos.write(buffer, 0, read);
            }
            payload = bos.toByteArray();
        }

        if (payload.length < 13) {
            throw new IllegalStateException("Encrypted payload too short");
        }

        int ivLength = payload[0] & 0xFF;
        if (ivLength < 12 || ivLength > 16 || payload.length <= (1 + ivLength)) {
            throw new IllegalStateException("Invalid IV length in encrypted payload");
        }

        byte[] iv = new byte[ivLength];
        System.arraycopy(payload, 1, iv, 0, ivLength);

        int cipherOffset = 1 + ivLength;
        byte[] cipherBytes = new byte[payload.length - cipherOffset];
        System.arraycopy(payload, cipherOffset, cipherBytes, 0, cipherBytes.length);

        SecretKey secretKey = AndroidKeyStoreAes.getOrCreateAesKey(KEY_ALIAS);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(128, iv));
        byte[] plainBytes = cipher.doFinal(cipherBytes);
        return new String(plainBytes, StandardCharsets.UTF_8);
    }
}
