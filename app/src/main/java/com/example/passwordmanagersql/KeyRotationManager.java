package com.example.passwordmanagersql;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class KeyRotationManager {
    private static final String PREFS_NAME = "KeyRotationPrefs";
    private static final String LAST_ROTATION_TIME = "LastRotationTime";
    public static final long ROTATION_INTERVAL = 30 * 24 * 60 * 60 * 1000L; // 30 days in milliseconds

    private final Context context;

    public KeyRotationManager(Context context) {
        this.context = context;
    }

    public void scheduleKeyRotation() {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .setRequiresCharging(true)
                .build();

        PeriodicWorkRequest keyRotationWork =
                new PeriodicWorkRequest.Builder(KeyRotationWorker.class, 1, TimeUnit.DAYS)
                        .setConstraints(constraints)
                        .build();

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "keyRotationWork",
                ExistingPeriodicWorkPolicy.KEEP,
                keyRotationWork);
    }

    public boolean isRotationNeeded() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long lastRotationTime = prefs.getLong(LAST_ROTATION_TIME, 0);
        return System.currentTimeMillis() - lastRotationTime >= ROTATION_INTERVAL;
    }

    public void rotateKeys(PasswordViewModel passwordViewModel) throws Exception {
        EncryptionUtil.generateNewKey();
        List<PasswordEntry> allPasswords = passwordViewModel.getAllPasswordsSync();

        for (PasswordEntry entry : allPasswords) {
            String decryptedPassword = EncryptionUtil.decrypt(entry.getEncryptedPassword());

            entry.encryptedPassword = EncryptionUtil.encrypt(decryptedPassword);
            passwordViewModel.update(entry);
        }

        updateLastRotationTime();
    }

    private void updateLastRotationTime() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putLong(LAST_ROTATION_TIME, System.currentTimeMillis()).apply();
    }
}