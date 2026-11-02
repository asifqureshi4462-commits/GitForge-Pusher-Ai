package com.asifqureshi.gitforge.data.local;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Encrypts small secrets (GitHub token, AI API keys) with an AES-256-GCM key that lives in the Android Keystore;
 * only ciphertext is written to app-private SharedPreferences. The entry name is bound to the ciphertext as
 * authenticated data, so entries cannot be swapped.
 *
 * <p>Limits (see docs/SECURITY.md): the Keystore key cannot be extracted on devices with hardware-backed storage,
 * but code running as this app (or a rooted device) can still ask the Keystore to decrypt. If decryption fails
 * (for example the key was invalidated by a lock-screen reset) the entry is dropped and the user signs in again;
 * there is never a fallback to plaintext.
 */
public final class SecureStore {

    /** Thrown when a secret cannot be encrypted or stored. */
    public static final class SecureStoreException extends Exception {
        public SecureStoreException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";
    private static final String KEY_ALIAS = "gitforge_secure_store_v1";
    private static final String PREFS_NAME = "gitforge_secure";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_BITS = 128;

    private final SharedPreferences prefs;

    public SecureStore(@NonNull Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public synchronized void putString(@NonNull String name, @NonNull String value) throws SecureStoreException {
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey());
            cipher.updateAAD(name.getBytes(StandardCharsets.UTF_8));
            byte[] iv = cipher.getIV();
            byte[] ct = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            byte[] blob = new byte[iv.length + ct.length];
            System.arraycopy(iv, 0, blob, 0, iv.length);
            System.arraycopy(ct, 0, blob, iv.length, ct.length);
            boolean ok = prefs.edit().putString(name, Base64.encodeToString(blob, Base64.NO_WRAP)).commit();
            if (!ok) {
                throw new SecureStoreException("Could not write the encrypted value", null);
            }
        } catch (GeneralSecurityException | IOException e) {
            throw new SecureStoreException("Could not encrypt the value with the Android Keystore", e);
        }
    }

    /** Returns the decrypted value, or null if absent or undecryptable (an undecryptable entry is removed). */
    @Nullable
    public synchronized String getString(@NonNull String name) {
        String stored = prefs.getString(name, null);
        if (stored == null) {
            return null;
        }
        try {
            byte[] blob = Base64.decode(stored, Base64.NO_WRAP);
            if (blob.length <= IV_LENGTH) {
                throw new GeneralSecurityException("Corrupt entry");
            }
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), new GCMParameterSpec(TAG_BITS, blob, 0, IV_LENGTH));
            cipher.updateAAD(name.getBytes(StandardCharsets.UTF_8));
            byte[] plain = cipher.doFinal(blob, IV_LENGTH, blob.length - IV_LENGTH);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IOException | IllegalArgumentException e) {
            prefs.edit().remove(name).apply();
            return null;
        }
    }

    public synchronized void remove(@NonNull String name) {
        prefs.edit().remove(name).apply();
    }

    private SecretKey getOrCreateKey() throws GeneralSecurityException, IOException {
        KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
        keyStore.load(null);
        KeyStore.Entry entry = keyStore.getEntry(KEY_ALIAS, null);
        if (entry instanceof KeyStore.SecretKeyEntry) {
            return ((KeyStore.SecretKeyEntry) entry).getSecretKey();
        }
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE);
        generator.init(new KeyGenParameterSpec.Builder(KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build());
        return generator.generateKey();
    }
}
