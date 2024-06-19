package com.example.passwordmanagersql;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;

import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.params.Argon2Parameters;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class EncryptionUtil {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";
    private static final String KEY_ALIAS = "MyEncryptionKey";
    private static final int GCM_TAG_LENGTH = 128; // Tag length in bits

    // Argon2 parameters
    private static int argon2Memory = 65536; // 64 MB
    private static int argon2Iterations = 3;
    private static int argon2Parallelism = 1;
    private static final int ARGON2_SALT_LENGTH = 16;
    private static final int ARGON2_HASH_LENGTH = 32; // 256 bits

    // Setter methods for Argon2 parameters
    public static void setArgon2Memory(int memory) {
        argon2Memory = memory;
    }

    public static void setArgon2Iterations(int iterations) {
        argon2Iterations = iterations;
    }

    public static void setArgon2Parallelism(int parallelism) {
        argon2Parallelism = parallelism;
    }

    /**
     * Securely wipes the contents of a byte array.
     * @param data The byte array to be wiped.
     */
    public static void secureDelete(byte[] data) {
        if (data == null) return;

        // Overwrite with zeros
        Arrays.fill(data, (byte) 0);

        // Overwrite with ones
        Arrays.fill(data, (byte) 0xFF);

        // Overwrite with random data
        new SecureRandom().nextBytes(data);
    }

    private static SecretKey getSecretKey(Context context) throws Exception {
        KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
        keyStore.load(null);

        if (!keyStore.containsAlias(KEY_ALIAS)) {
            generateKey();
        }

        return (SecretKey) keyStore.getKey(KEY_ALIAS, null);
    }

    public static void generateNewKey(Context context) throws Exception {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE);
        KeyGenParameterSpec keyGenParameterSpec = new KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build();

        keyGenerator.init(keyGenParameterSpec);
        keyGenerator.generateKey();
    }


    /**
     * Encrypts data with a passphrase.
     * @param data The data to encrypt.
     * @param passphrase The passphrase to use for encryption.
     * @return The encrypted data as a Base64 encoded string.
     * @throws Exception if encryption fails.
     * @implNote This method assumes that the passphrase has been validated externally.
     */
    public static String encryptWithPassphrase(String data, String passphrase) throws Exception {
        byte[] salt = null;
        byte[] derivedKey = null;
        byte[] iv = null;
        byte[] encryptedData = null;
        try {
            salt = new byte[ARGON2_SALT_LENGTH];
            SecureRandom secureRandom = new SecureRandom();
            secureRandom.nextBytes(salt);

            derivedKey = deriveKeyArgon2(passphrase, salt);
            SecretKeySpec secret = new SecretKeySpec(derivedKey, "AES");

            iv = new byte[12];
            secureRandom.nextBytes(iv);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secret, gcmSpec);
            encryptedData = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));

            ByteBuffer byteBuffer = ByteBuffer.allocate(salt.length + iv.length + encryptedData.length);
            byteBuffer.put(salt);
            byteBuffer.put(iv);
            byteBuffer.put(encryptedData);
            byte[] cipherMessage = byteBuffer.array();

            return java.util.Base64.getEncoder().encodeToString(cipherMessage);
        } catch (Exception e) {
            Log.e("EncryptionUtil", "Encryption error", e);
            throw e;
        } finally {
            secureDelete(salt);
            secureDelete(derivedKey);
            secureDelete(iv);
            secureDelete(encryptedData);
        }
    }

    /**
     * Decrypts data with a passphrase.
     * @param encryptedData The encrypted data as a Base64 encoded string.
     * @param passphrase The passphrase to use for decryption.
     * @return The decrypted data as a string.
     * @throws Exception if decryption fails.
     * @implNote This method assumes that the passphrase has been validated externally.
     */
    public static String decryptWithPassphrase(String encryptedData, String passphrase) throws Exception {
        byte[] decodedCipherMessage = null;
        byte[] salt = null;
        byte[] iv = null;
        byte[] encryptedBytes = null;
        byte[] derivedKey = null;
        byte[] decryptedData = null;
        try {
            decodedCipherMessage = java.util.Base64.getDecoder().decode(encryptedData);
            ByteBuffer byteBuffer = ByteBuffer.wrap(decodedCipherMessage);

            salt = new byte[ARGON2_SALT_LENGTH];
            byteBuffer.get(salt);

            iv = new byte[12];
            byteBuffer.get(iv);

            encryptedBytes = new byte[byteBuffer.remaining()];
            byteBuffer.get(encryptedBytes);

            derivedKey = deriveKeyArgon2(passphrase, salt);
            SecretKeySpec secret = new SecretKeySpec(derivedKey, "AES");

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secret, gcmSpec);

            decryptedData = cipher.doFinal(encryptedBytes);
            return new String(decryptedData, StandardCharsets.UTF_8);
        } catch (AEADBadTagException e) {
            Log.e("EncryptionUtil", "Authentication failed during decryption", e);
            throw new SecurityException("Invalid passphrase or corrupted data", e);
        } catch (Exception e) {
            Log.e("EncryptionUtil", "Decryption error", e);
            throw e;
        } finally {
            secureDelete(decodedCipherMessage);
            secureDelete(salt);
            secureDelete(iv);
            secureDelete(encryptedBytes);
            secureDelete(derivedKey);
            secureDelete(decryptedData);
        }
    }

    private static byte[] deriveKeyArgon2(String passphrase, byte[] salt) {
        Argon2Parameters params = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
                .withSalt(salt)
                .withParallelism(argon2Parallelism)
                .withMemoryAsKB(argon2Memory)
                .withIterations(argon2Iterations)
                .build();

        Argon2BytesGenerator generator = new Argon2BytesGenerator();
        generator.init(params);

        byte[] result = new byte[ARGON2_HASH_LENGTH];
        generator.generateBytes(passphrase.getBytes(StandardCharsets.UTF_8), result);
        return result;
    }

    private static void generateKey() throws Exception {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE);
        KeyGenParameterSpec keyGenParameterSpec = new KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build();

        keyGenerator.init(keyGenParameterSpec);
        keyGenerator.generateKey();
    }

    /**
     * Encrypts data using the Android KeyStore.
     * @param context The Android context.
     * @param value The string to encrypt.
     * @return The encrypted data as a Base64 encoded string.
     * @throws Exception if encryption fails.
     */
    public static String encrypt(Context context, String value) throws Exception {
        byte[] encrypted = null;
        byte[] iv = null;
        try {
            SecretKey secretKey = getSecretKey(context);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);

            encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            iv = cipher.getIV();

            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);

            return Base64.encodeToString(combined, Base64.DEFAULT);
        } catch (Exception e) {
            Log.e("EncryptionUtil", "Encryption error", e);
            throw e;
        } finally {
            secureDelete(encrypted);
            secureDelete(iv);
        }
    }

    /**
     * Decrypts data using the Android KeyStore.
     * @param context The Android context.
     * @param encrypted The encrypted data as a Base64 encoded string.
     * @return The decrypted string.
     * @throws Exception if decryption fails.
     */
    public static String decrypt(Context context, String encrypted) throws Exception {
        byte[] combined = null;
        byte[] iv = null;
        byte[] encryptedData = null;
        byte[] decrypted = null;
        try {
            combined = Base64.decode(encrypted, Base64.DEFAULT);

            iv = new byte[12];
            encryptedData = new byte[combined.length - iv.length];

            System.arraycopy(combined, 0, iv, 0, iv.length);
            System.arraycopy(combined, iv.length, encryptedData, 0, encryptedData.length);

            SecretKey secretKey = getSecretKey(context);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);

            decrypted = cipher.doFinal(encryptedData);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (AEADBadTagException e) {
            Log.e("EncryptionUtil", "Authentication failed during decryption", e);
            throw new SecurityException("Invalid key or corrupted data", e);
        } catch (Exception e) {
            Log.e("EncryptionUtil", "Decryption error", e);
            throw e;
        } finally {
            secureDelete(combined);
            secureDelete(iv);
            secureDelete(encryptedData);
            secureDelete(decrypted);
        }
    }
}