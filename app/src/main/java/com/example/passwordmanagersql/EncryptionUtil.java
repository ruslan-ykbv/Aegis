package com.example.passwordmanagersql;

import android.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class EncryptionUtil {

    private static final String ALGORITHM = "AES/CBC/PKCS5PADDING";
    private static final String KEY = "MySuperSecretKey"; // Ideally, use a securely generated key

    public static String encrypt(String value) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        SecretKey secretKey = getSecretKey();
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] iv = cipher.getIV();
        byte[] encrypted = cipher.doFinal(value.getBytes());

        byte[] combined = new byte[iv.length + encrypted.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);

        return Base64.encodeToString(combined, Base64.DEFAULT);
    }

    public static String decrypt(String encrypted) throws Exception {
        byte[] combined = Base64.decode(encrypted, Base64.DEFAULT);

        byte[] iv = new byte[16];
        byte[] encryptedBytes = new byte[combined.length - iv.length];
        System.arraycopy(combined, 0, iv, 0, iv.length);
        System.arraycopy(combined, iv.length, encryptedBytes, 0, encryptedBytes.length);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        SecretKey secretKey = getSecretKey();
        cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));

        byte[] original = cipher.doFinal(encryptedBytes);
        return new String(original);
    }

    private static SecretKey getSecretKey() throws Exception {
        byte[] key = KEY.getBytes("UTF-8");
        return new SecretKeySpec(key, "AES");
    }
}