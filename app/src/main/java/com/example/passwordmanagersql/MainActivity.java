package com.example.passwordmanagersql;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private PasswordAdapter adapter;
    private PasswordViewModel passwordViewModel;
    private Executor executor;
    private BiometricPrompt biometricPrompt;
    private BiometricPrompt.PromptInfo showPromptInfo;
    private BiometricPrompt.PromptInfo editPromptInfo;
    private PasswordEntry passwordEntryToShow;
    private ProgressBar progressBar;
    private TextView progressText;
    private View progressOverlay;
    private ExecutorService backgroundExecutor;

    private final ActivityResultLauncher<Intent> addPasswordLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    handleAddPasswordResult(result.getData());
                } else {
                    Toast.makeText(this, "Password not saved", Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<Intent> editPasswordLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    handleEditPasswordResult(result.getData());
                } else {
                    Toast.makeText(this, "Password not updated", Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<Intent> restoreFileLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    handleRestoreFile(uri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeComponents();
        setupRecyclerView();
        setupViewModel();
        setupBiometricAuthentication();

        backgroundExecutor = Executors.newSingleThreadExecutor();
    }

    private void initializeComponents() {
        new KeyRotationManager(this).scheduleKeyRotation();

        ImageView buttonBackup = findViewById(R.id.button_backup);
        buttonBackup.setOnClickListener(v -> startBackupProcess());

        ImageView buttonRestore = findViewById(R.id.button_restore);
        buttonRestore.setOnClickListener(v -> performRestore());

        progressBar = findViewById(R.id.progress_bar);
        progressText = findViewById(R.id.progress_text);
        progressOverlay = findViewById(R.id.progress_overlay);
        progressOverlay.setOnTouchListener((v, event) -> true);


        SearchView searchView = findViewById(R.id.search_view);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterPasswordEntries(newText);
                return true;
            }
        });

        ImageView buttonAddPassword = findViewById(R.id.button_add_password);
        buttonAddPassword.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddEditPasswordActivity.class);
            addPasswordLauncher.launch(intent);
        });

        executor = ContextCompat.getMainExecutor(this);
    }

    private void setupRecyclerView() {
        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);

        adapter = new PasswordAdapter();
        recyclerView.setAdapter(adapter);

        adapter.setOnItemClickListener(passwordEntry -> {
            passwordEntryToShow = passwordEntry;
            authenticateUserForShow();
        });

        adapter.setOnEditItemClickListener(passwordEntry -> {
            passwordEntryToShow = passwordEntry;
            authenticateUserForEdit();
        });

        adapter.setOnLongItemClickListener(position -> {
            PasswordEntry passwordEntry = adapter.getPasswordAtPosition(position);
            showDeleteConfirmationDialog(passwordEntry);
        });
    }

    private void setupViewModel() {
        passwordViewModel = new ViewModelProvider(this).get(PasswordViewModel.class);
        passwordViewModel.getAllPasswords().observe(this, adapter::submitList);
    }

    private void setupBiometricAuthentication() {
        showPromptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Biometric authentication")
                .setSubtitle("Log in using your biometric credential or device credential")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK | BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                .build();

        editPromptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Authenticate to edit password")
                .setSubtitle("Use your biometric credential")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK | BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                .build();
    }

    private void performRestore() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");
        restoreFileLauncher.launch(intent);
    }

    private void startBackupProcess() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter Passphrase");

        TextInputLayout textInputLayout = new TextInputLayout(this);
        textInputLayout.setEndIconMode(TextInputLayout.END_ICON_PASSWORD_TOGGLE);

        TextInputEditText input = new TextInputEditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        textInputLayout.addView(input);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        textInputLayout.setLayoutParams(params);

        builder.setView(textInputLayout);
        builder.setPositiveButton("OK", null);
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
            String passphrase = Objects.requireNonNull(input.getText()).toString();
            if (!isPassphraseStrong(passphrase)) {
                input.setError("Passphrase is too weak. It should meet all of the following:\n" +
                        "- At least 12 characters long\n" +
                        "- Contains uppercase and lowercase letters\n" +
                        "- Contains numbers\n" +
                        "- Contains special characters");
                return;
            }

            performBackup(passphrase);

            dialog.dismiss();

        });
    }

    private void performBackup(String passphrase) {
        showProgress("Encrypting your backup");

        backgroundExecutor.execute(() -> {
            List<PasswordEntry> passwordEntries;
            try {
                passwordEntries = passwordViewModel.getAllPasswordsSync();
            } catch (Exception e) {
                Log.e("Error occurred", "in backup");
                runOnUiThread(() -> {
                    hideProgress();
                    Toast.makeText(MainActivity.this, "Error fetching passwords", Toast.LENGTH_SHORT).show();
                });
                return;
            }

            StringBuilder encryptedDataBuilder = new StringBuilder();
            try {
                for (PasswordEntry entry : passwordEntries) {
                    String entryData = entry.getWebsite() + "," + entry.getUsername() + "," + EncryptionUtil.decrypt(entry.getEncryptedPassword());
                    String encryptedData = EncryptionUtil.encryptWithPassphrase(entryData, passphrase);
                    encryptedDataBuilder.append(encryptedData).append("\n");
                }
            } catch (Exception e) {
                Log.e("Error occurred", "in backup");
                runOnUiThread(() -> {
                    hideProgress();
                    Toast.makeText(MainActivity.this, "Backup failed", Toast.LENGTH_SHORT).show();
                });
                return;
            }
            String encryptedData = encryptedDataBuilder.toString();

            ContentValues values = new ContentValues();
            values.put(MediaStore.Files.FileColumns.DISPLAY_NAME, "password_backup.txt");
            values.put(MediaStore.Files.FileColumns.MIME_TYPE, "text/plain");
            values.put(MediaStore.Files.FileColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/PasswordManagerBackups/");

            Uri contentUri = getContentResolver().insert(MediaStore.Files.getContentUri("external"), values);

            if (contentUri != null) {
                try (OutputStream outputStream = getContentResolver().openOutputStream(contentUri)) {
                    assert outputStream != null;
                    outputStream.write(encryptedData.getBytes());
                    outputStream.flush();
                    runOnUiThread(() -> {
                        hideProgress();
                        Toast.makeText(MainActivity.this, "Backup successful", Toast.LENGTH_SHORT).show();
                    });
                } catch (Exception e) {
                    Log.e("Error occurred", "in backup");
                    runOnUiThread(() -> {
                        hideProgress();
                        Toast.makeText(MainActivity.this, "Backup failed", Toast.LENGTH_SHORT).show();
                    });
                }
            } else {
                runOnUiThread(() -> {
                    hideProgress();
                    Toast.makeText(MainActivity.this, "Failed to create backup file", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void restoreFromBackup(Uri uri, String passphrase) {
        showProgress("Decrypting your backup");

        backgroundExecutor.execute(() -> {
            try (InputStream inputStream = getContentResolver().openInputStream(uri);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {

                String line;
                int lineNumber = 0;
                while ((line = reader.readLine()) != null) {
                    lineNumber++;
                    if (line.trim().isEmpty()) {
                        continue;
                    }

                    try {
                        String decryptedEntry = EncryptionUtil.decryptWithPassphrase(line, passphrase);
                        String[] parts = decryptedEntry.split(",");
                        if (parts.length == 3) {
                            String website = parts[0];
                            String username = parts[1];
                            String decryptedPassword = parts[2];

                            String encryptedPassword = EncryptionUtil.encrypt(decryptedPassword);
                            PasswordEntry newEntry = new PasswordEntry(website, username, encryptedPassword);
                            passwordViewModel.insert(newEntry);
                        } else {
                            Log.e("Restore", "Parsing error: Invalid format.");
                        }
                    } catch (Exception e) {
                        Log.e("Error occurred", "in restore");
                    }
                }

                runOnUiThread(() -> {
                    hideProgress();
                    Toast.makeText(this, "Restore complete", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                Log.e("Error occurred", "in restore");
                runOnUiThread(() -> {
                    hideProgress();
                    Toast.makeText(this, "Error reading or decrypting backup", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void showProgress(String message) {
        runOnUiThread(() -> {
            progressOverlay.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.VISIBLE);
            progressText.setVisibility(View.VISIBLE);
            progressText.setText(message);
            progressOverlay.setClickable(true);
            progressOverlay.setFocusable(true);
        });
    }

    private void hideProgress() {
        runOnUiThread(() -> {
            progressOverlay.setVisibility(View.GONE);
            progressBar.setVisibility(View.GONE);
            progressText.setVisibility(View.GONE);
            progressOverlay.setClickable(false);
            progressOverlay.setFocusable(false);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (backgroundExecutor != null) {
            backgroundExecutor.shutdown();
        }
    }

    private boolean isPassphraseStrong(String passphrase) {
        return passphrase.length() >= 12 &&
                passphrase.matches(".*[A-Z].*") &&
                passphrase.matches(".*\\d.*") &&
                passphrase.matches(".*[^A-Za-z0-9].*");
    }

    private void handleRestoreFile(Uri uri) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter Passphrase");

        TextInputLayout textInputLayout = new TextInputLayout(this);
        textInputLayout.setEndIconMode(TextInputLayout.END_ICON_PASSWORD_TOGGLE);

        TextInputEditText input = new TextInputEditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        textInputLayout.addView(input);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        textInputLayout.setLayoutParams(params);

        builder.setView(textInputLayout);

        builder.setPositiveButton("OK", (dialog, which) -> {
            String passphrase = Objects.requireNonNull(input.getText()).toString();
            restoreFromBackup(uri, passphrase);
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }


    private void showPassword() {
        if (passwordEntryToShow != null) {
            try {
                String decryptedPassword = EncryptionUtil.decrypt(passwordEntryToShow.getEncryptedPassword());
                showPasswordDialog(decryptedPassword);
                passwordEntryToShow = null;
            } catch (Exception e) {
                Log.e("Error occurred", "in showing password");
                Toast.makeText(MainActivity.this, "Error decrypting password", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(MainActivity.this, "No password entry selected", Toast.LENGTH_SHORT).show();
        }
    }

    private void filterPasswordEntries(String query) {
        List<PasswordEntry> filteredList = new ArrayList<>();
        try {
            List<PasswordEntry> allPasswordEntries = passwordViewModel.getAllPasswordsSync();
            for (PasswordEntry entry : allPasswordEntries) {
                if (entry.getWebsite().toLowerCase().contains(query.toLowerCase())) {
                    filteredList.add(entry);
                }
            }
        } catch (Exception e) {
            Log.e("Error occurred", "in filtering passwords");
        }
        adapter.submitList(filteredList);
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

    private void authenticateUserForShow() {
        biometricPrompt = new BiometricPrompt(MainActivity.this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                Toast.makeText(getApplicationContext(), "Authentication error: " + errString, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                showPassword();
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                Toast.makeText(getApplicationContext(), "Authentication failed", Toast.LENGTH_SHORT).show();
            }
        });
        biometricPrompt.authenticate(showPromptInfo);
    }

    private void authenticateUserForEdit() {
        biometricPrompt = new BiometricPrompt(MainActivity.this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                launchEditPasswordActivity();
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                Toast.makeText(getApplicationContext(), "Authentication failed", Toast.LENGTH_SHORT).show();
            }
        });
        biometricPrompt.authenticate(editPromptInfo);
    }

    private void launchEditPasswordActivity() {
        if (passwordEntryToShow != null) {
            Intent intent = new Intent(MainActivity.this, AddEditPasswordActivity.class);
            intent.putExtra(AddEditPasswordActivity.EXTRA_ID, passwordEntryToShow.getId());
            intent.putExtra(AddEditPasswordActivity.EXTRA_WEBSITE, passwordEntryToShow.getWebsite());
            intent.putExtra(AddEditPasswordActivity.EXTRA_USERNAME, passwordEntryToShow.getUsername());
            intent.putExtra(AddEditPasswordActivity.EXTRA_PASSWORD, passwordEntryToShow.getEncryptedPassword());
            editPasswordLauncher.launch(intent);
            passwordEntryToShow = null;
        } else {
            Toast.makeText(this, "No password entry selected", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleAddPasswordResult(Intent data) {
        if (data != null) {
            String website = data.getStringExtra(AddEditPasswordActivity.EXTRA_WEBSITE);
            String username = data.getStringExtra(AddEditPasswordActivity.EXTRA_USERNAME);
            String password = data.getStringExtra(AddEditPasswordActivity.EXTRA_PASSWORD);

            try {
                String encryptedPassword = EncryptionUtil.encrypt(password);
                PasswordEntry passwordEntry = new PasswordEntry(website, username, encryptedPassword);
                passwordViewModel.insert(passwordEntry);
                Toast.makeText(this, "Password saved", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Log.e("Error occurred", "in adding password");
                Toast.makeText(this, "Error saving password", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void handleEditPasswordResult(Intent data) {
        if (data != null) {
            int id = data.getIntExtra(AddEditPasswordActivity.EXTRA_ID, -1);
            if (id == -1) {
                Toast.makeText(this, "Password can't be updated", Toast.LENGTH_SHORT).show();
                return;
            }

            String website = data.getStringExtra(AddEditPasswordActivity.EXTRA_WEBSITE);
            String username = data.getStringExtra(AddEditPasswordActivity.EXTRA_USERNAME);
            String password = data.getStringExtra(AddEditPasswordActivity.EXTRA_PASSWORD);

            try {
                String encryptedPassword = EncryptionUtil.encrypt(password);
                PasswordEntry passwordEntry = new PasswordEntry(website, username, encryptedPassword);
                passwordEntry.setId(id);
                passwordViewModel.update(passwordEntry);
                Toast.makeText(this, "Password updated", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Log.e("Error occurred", "in handling passwords");
                Toast.makeText(this, "Error updating password", Toast.LENGTH_SHORT).show();
            }
        }
    }
}