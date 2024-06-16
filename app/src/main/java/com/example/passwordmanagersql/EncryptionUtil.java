package com.example.passwordmanagersql;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Arrays;

import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class EncryptionUtil {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";
    private static final String KEY_ALIAS = "MyEncryptionKey";
    private static final int GCM_TAG_LENGTH = 128; // Tag length in bits

    private static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int PBKDF2_ITERATIONS = 65536; // Adjust as needed for security/performance
    private static final int PBKDF2_KEY_LENGTH = 256; // In bits

    // Optional configurable parameters for PBKDF2
    private static int pbkdf2Iterations = PBKDF2_ITERATIONS;
    private static int pbkdf2KeyLength = PBKDF2_KEY_LENGTH;

    public static void setPbkdf2Parameters(int iterations, int keyLength) {
        pbkdf2Iterations = iterations;
        pbkdf2KeyLength = keyLength;
    }

    private static SecretKey getSecretKey(Context context) throws Exception {
        // Optionally delete existing key
        // deleteExistingKey();

        KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
        keyStore.load(null);

        if (!keyStore.containsAlias(KEY_ALIAS)) {
            generateKey();
        }

        return (SecretKey) keyStore.getKey(KEY_ALIAS, null);
    }

    // Encryption (with Salt and IV)
    public static String encryptWithPassphrase(String data, String passphrase) throws Exception {
        try {
            byte[] salt = new byte[16];
            SecureRandom secureRandom = new SecureRandom();
            secureRandom.nextBytes(salt);

            SecretKeyFactory factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);
            KeySpec spec = new PBEKeySpec(passphrase.toCharArray(), salt, pbkdf2Iterations, pbkdf2KeyLength);
            SecretKey tmp = factory.generateSecret(spec);
            SecretKeySpec secret = new SecretKeySpec(tmp.getEncoded(), "AES");

            byte[] iv = new byte[12];
            secureRandom.nextBytes(iv);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secret, gcmSpec);
            byte[] encryptedData = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));

            ByteBuffer byteBuffer = ByteBuffer.allocate(salt.length + iv.length + encryptedData.length);
            byteBuffer.put(salt);
            byteBuffer.put(iv);
            byteBuffer.put(encryptedData);
            byte[] cipherMessage = byteBuffer.array();

            return java.util.Base64.getEncoder().encodeToString(cipherMessage);
        } catch (Exception e) {
            Log.e("EncryptionUtil", "Encryption error", e);
            throw e;
        }
    }

    public static String decryptWithPassphrase(String encryptedData, String passphrase) throws Exception {
        try {
            byte[] decodedCipherMessage = java.util.Base64.getDecoder().decode(encryptedData);
            ByteBuffer byteBuffer = ByteBuffer.wrap(decodedCipherMessage);

            byte[] salt = new byte[16];
            byteBuffer.get(salt);

            byte[] iv = new byte[12];
            byteBuffer.get(iv);

            byte[] encryptedBytes = new byte[byteBuffer.remaining()];
            byteBuffer.get(encryptedBytes);

            SecretKeyFactory factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);
            KeySpec spec = new PBEKeySpec(passphrase.toCharArray(), salt, pbkdf2Iterations, pbkdf2KeyLength);
            SecretKey tmp = factory.generateSecret(spec);
            SecretKeySpec secret = new SecretKeySpec(tmp.getEncoded(), "AES");

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secret, gcmSpec);

            byte[] decryptedData = cipher.doFinal(encryptedBytes);
            return new String(decryptedData, StandardCharsets.UTF_8);
        } catch (AEADBadTagException e) {
            Log.e("EncryptionUtil", "Authentication failed during decryption", e);
            throw new SecurityException("Invalid passphrase or corrupted data", e);
        } catch (Exception e) {
            Log.e("EncryptionUtil", "Decryption error", e);
            throw e;
        }
    }

    private static void deleteExistingKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
        keyStore.load(null);

        if (keyStore.containsAlias(KEY_ALIAS)) {
            keyStore.deleteEntry(KEY_ALIAS);
        }
    }

    private static void generateKey() throws Exception {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE);
        KeyGenParameterSpec keyGenParameterSpec = new KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM) // Correct for GCM
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE) // Correct for GCM
                .setKeySize(256)
                .build();

        keyGenerator.init(keyGenParameterSpec);
        keyGenerator.generateKey();
    }

    public static String encrypt(Context context, String value) throws Exception {
        try {
            SecretKey secretKey = getSecretKey(context);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);

            byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            byte[] iv = cipher.getIV();

            // Combine IV and encrypted data
            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);

            return Base64.encodeToString(combined, Base64.DEFAULT);
        } catch (Exception e) {
            Log.e("EncryptionUtil", "Encryption error", e);
            throw e;
        }
    }

    public static String decrypt(Context context, String encrypted) throws Exception {
        try {
            byte[] combined = Base64.decode(encrypted, Base64.DEFAULT);

            // Extract IV and encrypted data
            byte[] iv = new byte[12]; // IV length for GCM is 12 bytes
            byte[] encryptedData = new byte[combined.length - iv.length];

            System.arraycopy(combined, 0, iv, 0, iv.length);
            System.arraycopy(combined, iv.length, encryptedData, 0, encryptedData.length);

            SecretKey secretKey = getSecretKey(context);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);

            byte[] decrypted = cipher.doFinal(encryptedData);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (AEADBadTagException e) {
            Log.e("EncryptionUtil", "Authentication failed during decryption", e);
            throw new SecurityException("Invalid key or corrupted data", e);
        } catch (Exception e) {
            Log.e("EncryptionUtil", "Decryption error", e);
            throw e;
        }
    }
}