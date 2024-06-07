package com.example.passwordmanagersql;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.concurrent.Executor;

public class MainActivity extends AppCompatActivity {
    public static final int ADD_PASSWORD_REQUEST = 1;
    public static final int EDIT_PASSWORD_REQUEST = 2;

    private PasswordViewModel passwordViewModel;
    private Executor executor;
    private BiometricPrompt biometricPrompt;
    private BiometricPrompt.PromptInfo promptInfo;

    private PasswordEntry passwordEntryToShow; // Store the password entry temporarily

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FloatingActionButton buttonAddPassword = findViewById(R.id.button_add_password);
        buttonAddPassword.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddEditPasswordActivity.class);
            startActivityForResult(intent, ADD_PASSWORD_REQUEST);
        });

        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);

        PasswordAdapter adapter = new PasswordAdapter();
        recyclerView.setAdapter(adapter);

        passwordViewModel = new ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(this.getApplication())).get(PasswordViewModel.class);
        passwordViewModel.getAllPasswords().observe(this, adapter::submitList);

        adapter.setOnItemClickListener(passwordEntry -> {
            passwordEntryToShow = passwordEntry;
            authenticateUser(); // Trigger biometric authentication
        });

        adapter.setOnLongItemClickListener(position -> {
            PasswordEntry passwordEntry = adapter.passwords.get(position);
            showDeleteConfirmationDialog(passwordEntry);
        });



        executor = ContextCompat.getMainExecutor(this);
        biometricPrompt = new BiometricPrompt(MainActivity.this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                Toast.makeText(getApplicationContext(), "Authentication error: " + errString, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                showPassword(); // Show the password after successful authentication
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                Toast.makeText(getApplicationContext(), "Authentication failed", Toast.LENGTH_SHORT).show();
            }
        });

        promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Biometric authentication")
                .setSubtitle("Log in using your biometric credential or device credential")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK | BiometricManager.Authenticators.DEVICE_CREDENTIAL) // Allows biometric and device credential authentication
                .build();

    }

    private void authenticateUser() {
        biometricPrompt.authenticate(promptInfo);
    }

    private void showPassword() {
        if (passwordEntryToShow != null) {
            try {
                String decryptedPassword = EncryptionUtil.decrypt(passwordEntryToShow.getEncryptedPassword());
                showPasswordDialog(decryptedPassword);
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(MainActivity.this, "Error decrypting password", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showPasswordDialog(String password) {
        new AlertDialog.Builder(this)
                .setTitle("Password")
                .setMessage(password)
                .setPositiveButton("OK", null)
                .show();
    }


    private void showDeleteConfirmationDialog(PasswordEntry passwordEntry) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Password")
                .setMessage("Are you sure you want to delete this password?")
                .setPositiveButton("Delete", (dialog, which) -> deletePassword(passwordEntry))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deletePassword(PasswordEntry passwordEntry) {
        passwordViewModel.delete(passwordEntry);
        Toast.makeText(this, "Password deleted", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == ADD_PASSWORD_REQUEST && resultCode == RESULT_OK) {
            String website = data.getStringExtra(AddEditPasswordActivity.EXTRA_WEBSITE);
            String password = data.getStringExtra(AddEditPasswordActivity.EXTRA_PASSWORD);

            try {
                String encryptedPassword = EncryptionUtil.encrypt(password);
                PasswordEntry passwordEntry = new PasswordEntry(website, encryptedPassword);
                passwordViewModel.insert(passwordEntry);
                Toast.makeText(this, "Password saved", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Error saving password", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == EDIT_PASSWORD_REQUEST && resultCode == RESULT_OK) {
            int id = data.getIntExtra(AddEditPasswordActivity.EXTRA_ID, -1);
            if (id == -1) {
                Toast.makeText(this, "Password can't be updated", Toast.LENGTH_SHORT).show();
                return;
            }

            String website = data.getStringExtra(AddEditPasswordActivity.EXTRA_WEBSITE);
            String password = data.getStringExtra(AddEditPasswordActivity.EXTRA_PASSWORD);

            try {
                String encryptedPassword = EncryptionUtil.encrypt(password);
                PasswordEntry passwordEntry = new PasswordEntry(website, encryptedPassword);
                passwordEntry.id = id;
                passwordViewModel.update(passwordEntry);
                Toast.makeText(this, "Password updated", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Error updating password", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Password not saved", Toast.LENGTH_SHORT).show();
        }
    }
}