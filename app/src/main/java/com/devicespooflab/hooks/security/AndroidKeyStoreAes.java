package com.devicespooflab.hooks.security;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import androidx.annotation.NonNull;

import java.security.KeyStore;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

public final class AndroidKeyStoreAes {

    private static final String KEYSTORE_TYPE = "AndroidKeyStore";

    private AndroidKeyStoreAes() {
    }

    @NonNull
    public static SecretKey getOrCreateAesKey(@NonNull String alias) throws Exception {
        KeyStore keyStore = KeyStore.getInstance(KEYSTORE_TYPE);
        keyStore.load(null);

        KeyStore.Entry entry = keyStore.getEntry(alias, null);
        if (entry instanceof KeyStore.SecretKeyEntry) {
            return ((KeyStore.SecretKeyEntry) entry).getSecretKey();
        }

        KeyGenerator keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            KEYSTORE_TYPE
        );
        KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .build();

        keyGenerator.init(spec);
        return keyGenerator.generateKey();
    }
}
