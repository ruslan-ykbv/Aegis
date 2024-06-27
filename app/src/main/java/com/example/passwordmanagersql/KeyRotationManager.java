package com.example.passwordmanagersql;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class KeyRotationManager {
    private static final String TAG = "KeyRotationManager";
    public static final double ROTATION_INTERVAL = 14 * 24 * 60 * 60 * 1000L; // 14 days in milliseconds
    private static final String PREFS_NAME = "KeyRotationPrefs";
    private static final String LAST_ROTATION_TIME = "LastRotationTime";
    private final Context context;

    public KeyRotationManager(Context context) {
        this.context = context;
    }

    public void scheduleKeyRotation() {
        Log.d(TAG, "Scheduling key rotation");
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .build();

        PeriodicWorkRequest keyRotationWork = new PeriodicWorkRequest.Builder(KeyRotationWorker.class, 1, TimeUnit.DAYS)
                .setConstraints(constraints)
                .setInitialDelay(calculateInitialDelay(), TimeUnit.MILLISECONDS)
                .build();

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "keyRotationWork",
                ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
                keyRotationWork
        );
    }

    private long calculateInitialDelay() {
        long lastRotationTime = getLastRotationTime();
        long timeSinceLastRotation = System.currentTimeMillis() - lastRotationTime;
        return (long) Math.max(0, ROTATION_INTERVAL - timeSinceLastRotation);
    }

    public boolean isRotationNeeded() {
        long lastRotationTime = getLastRotationTime();
        return System.currentTimeMillis() - lastRotationTime >= ROTATION_INTERVAL;
    }

    public void rotateKeys(PasswordViewModel passwordViewModel) {
        try {
            List<PasswordEntry> allPasswords = passwordViewModel.getAllPasswordsSync();
            List<String> decryptedPasswords = new ArrayList<>();

            // Step 1: Decrypt all passwords with the old key
            for (PasswordEntry entry : allPasswords) {
                String decryptedPassword = EncryptionUtil.decrypt(entry.getEncryptedPassword());
                decryptedPasswords.add(decryptedPassword);
            }

            // Step 2: Generate a new key
            EncryptionUtil.generateNewKey();

            // Step 3: Re-encrypt all passwords with the new key
            for (int i = 0; i < allPasswords.size(); i++) {
                PasswordEntry entry = allPasswords.get(i);
                String decryptedPassword = decryptedPasswords.get(i);

                String newEncryptedPassword = EncryptionUtil.encrypt(decryptedPassword);
                entry.setEncryptedPassword(newEncryptedPassword);
                passwordViewModel.update(entry);
            }

            // Step 4: Securely clear decrypted passwords from memory
            for (String decryptedPassword : decryptedPasswords) {
                EncryptionUtil.secureDelete(decryptedPassword.getBytes());
            }

            //Step 5: Securely clear all passwords from memory
            for (PasswordEntry entry : allPasswords) {
                EncryptionUtil.secureDelete(entry.getEncryptedPassword().getBytes());
            }

            decryptedPasswords.clear();
            allPasswords.clear();


            updateLastRotationTime();
            Log.i(TAG, "Key rotation completed successfully");
        } catch (Exception e) {
            Log.e(TAG, "Key rotation failed", e);
            throw new RuntimeException("Key rotation failed", e);
        }
    }

    private long getLastRotationTime() {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getLong(LAST_ROTATION_TIME, 0);
    }

    private void updateLastRotationTime() {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putLong(LAST_ROTATION_TIME, System.currentTimeMillis())
                .apply();
    }

    public static class KeyRotationWorker extends Worker {
        public KeyRotationWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
            super(context, workerParams);
        }

        @NonNull
        @Override
        public Result doWork() {
            Log.d(TAG, "doWork in key rotation");
            KeyRotationManager manager = new KeyRotationManager(getApplicationContext());
            PasswordViewModel viewModel = new PasswordViewModel((Application) getApplicationContext());
            try {
                if (manager.isRotationNeeded()) {
                    Log.wtf(TAG, "Key rotation needed");
                    manager.rotateKeys(viewModel);
                    return Result.success();
                }
            } catch (Exception e) {
                Log.e(TAG, "Key rotation failed", e);
                return Result.retry();
            }
            return Result.success();
        }
    }
}