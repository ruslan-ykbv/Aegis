package com.example.passwordmanagersql;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class KeyRotationWorker extends Worker {
    private static final String TAG = "KeyRotationWorker";

    public KeyRotationWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        KeyRotationManager keyRotationManager = new KeyRotationManager(getApplicationContext());

        if (keyRotationManager.isRotationNeeded()) {
            try {
                PasswordViewModel passwordViewModel = new PasswordViewModel((Application) getApplicationContext());
                keyRotationManager.rotateKeys(passwordViewModel);
                Log.d(TAG, "Key rotation completed successfully");
                return Result.success();
            } catch (Exception e) {
                Log.e(TAG, "Error during key rotation", e);
                return Result.retry();
            }
        } else {
            Log.d(TAG, "Key rotation not needed at this time");
            return Result.success();
        }
    }
}