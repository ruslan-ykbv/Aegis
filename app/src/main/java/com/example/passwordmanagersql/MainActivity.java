package com.example.passwordmanagersql;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.media.Image;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.InputType;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.SearchView;
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

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

public class MainActivity extends AppCompatActivity {
    public static final int ADD_PASSWORD_REQUEST = 1;
    public static final int EDIT_PASSWORD_REQUEST = 2;

    private static final int REQUEST_RESTORE_FILE = 200;
    PasswordAdapter adapter;
    private PasswordViewModel passwordViewModel;
    private Executor executor;
    private BiometricPrompt biometricPrompt;

    private BiometricPrompt.PromptInfo showPromptInfo;
    private BiometricPrompt.PromptInfo editPromptInfo;
    private SearchView searchView;
    private PasswordEntry passwordEntryToShow; // Store the password entry temporarily
    // Authentication callback for showing password
    private final BiometricPrompt.AuthenticationCallback showAuthenticationCallback = new BiometricPrompt.AuthenticationCallback() {
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
    };
    // Authentication callback for editing password
    private final BiometricPrompt.AuthenticationCallback editAuthenticationCallback = new BiometricPrompt.AuthenticationCallback() {
        @Override
        public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
            super.onAuthenticationSucceeded(result);
            // Authentication succeeded, open AddEditPasswordActivity
            Intent intent = new Intent(MainActivity.this, AddEditPasswordActivity.class);
            intent.putExtra(AddEditPasswordActivity.EXTRA_ID, passwordEntryToShow.getId());
            intent.putExtra(AddEditPasswordActivity.EXTRA_WEBSITE, passwordEntryToShow.getWebsite());
            intent.putExtra(AddEditPasswordActivity.EXTRA_USERNAME, passwordEntryToShow.getUsername());
            intent.putExtra(AddEditPasswordActivity.EXTRA_PASSWORD, passwordEntryToShow.getEncryptedPassword());
            startActivityForResult(intent, EDIT_PASSWORD_REQUEST);
            passwordEntryToShow = null; // Reset after action
        }

        @Override
        public void onAuthenticationFailed() {
            super.onAuthenticationFailed();
            Toast.makeText(getApplicationContext(), "Authentication failed", Toast.LENGTH_SHORT).show();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ImageView buttonBackup = findViewById(R.id.button_backup);
        buttonBackup.setOnClickListener(v -> startBackupProcess());

        ImageView buttonRestore = findViewById(R.id.button_restore);
        buttonRestore.setOnClickListener(v -> performRestore());

        // Initialize searchView for searching password entries
        searchView = findViewById(R.id.search_view);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false; // We handle the query as it is typed
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterPasswordEntries(newText); // Filter password entries as text changes
                return true;
            }
        });

        // Initialize showPromptInfo for biometric authentication when showing passwords
        showPromptInfo = new BiometricPrompt.PromptInfo.Builder().setTitle("Biometric authentication").setSubtitle("Log in using your biometric credential or device credential").setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK | BiometricManager.Authenticators.DEVICE_CREDENTIAL).build();

        // Initialize editPromptInfo for biometric authentication when editing passwords
        editPromptInfo = new BiometricPrompt.PromptInfo.Builder().setTitle("Authenticate to edit password").setSubtitle("Use your biometric credential").setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK | BiometricManager.Authenticators.DEVICE_CREDENTIAL).build();

        // Set up the add password button
        ImageView buttonAddPassword = findViewById(R.id.button_add_password);
        buttonAddPassword.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddEditPasswordActivity.class);
            startActivityForResult(intent, ADD_PASSWORD_REQUEST);
        });

        // Set up the RecyclerView for displaying password entries
        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);

        adapter = new PasswordAdapter();
        recyclerView.setAdapter(adapter);

        // Set up the ViewModel to manage password data
        passwordViewModel = new ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(this.getApplication())).get(PasswordViewModel.class);
        passwordViewModel.getAllPasswords().observe(this, adapter::submitList);

        // Set up item click listener for showing password
        adapter.setOnItemClickListener(passwordEntry -> {
            passwordEntryToShow = passwordEntry;
            authenticateUserForShow(); // Trigger biometric authentication for showing password
        });

        // Set up item edit click listener for editing password
        adapter.setOnEditItemClickListener(passwordEntry -> {
            passwordEntryToShow = passwordEntry;
            authenticateUserForEdit(); // Trigger biometric authentication for editing password
        });

        // Set up long item click listener for deleting password
        adapter.setOnLongItemClickListener(position -> {
            PasswordEntry passwordEntry = adapter.passwords.get(position);
            showDeleteConfirmationDialog(passwordEntry);
        });

        // Initialize executor for biometric prompt
        executor = ContextCompat.getMainExecutor(this);
    }

    private void performRestore() {
        // 1. Create an Intent to pick a file using MediaStore
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain"); // Filter for text files

        startActivityForResult(intent, REQUEST_RESTORE_FILE);
    }

    private void startBackupProcess() {
        // 2. Prompt the user for a passphrase
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter Passphrase");

        TextInputLayout textInputLayout = new TextInputLayout(this, null);
        textInputLayout.setEndIconMode(TextInputLayout.END_ICON_PASSWORD_TOGGLE);

        TextInputEditText input = new TextInputEditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        textInputLayout.addView(input);

        // 4. Apply layout parameters to ensure the TextInputLayout is displayed correctly
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        textInputLayout.setLayoutParams(params);

        // 5. Set TextInputLayout as the view for the AlertDialog
        builder.setView(textInputLayout);

        // Use a custom listener for the positive button
        builder.setPositiveButton("OK", null);
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        AlertDialog dialog = builder.create();
        dialog.show();

        // Override the positive button's listener to keep the dialog open if needed
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
            String passphrase = input.getText().toString();
            Log.d("Backup", "Passphrase entered: " + passphrase);

            // Check passphrase strength
            if (!isPassphraseStrong(passphrase)) {
                // Show error message and keep the dialog open
                input.setError("Passphrase is too weak. It should meet all of the following:\n" +
                        "- At least 12 characters long\n" +
                        "- Contains uppercase and lowercase letters\n" +
                        "- Contains numbers\n" +
                        "- Contains special characters");
                return;
            }

            // Passphrase is strong, proceed with the backup

            // 3. Fetch all passwords from the database
            List<PasswordEntry> passwordEntries = new ArrayList<>();
            try {
                passwordEntries = passwordViewModel.getAllPasswordsSync();
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
                Toast.makeText(MainActivity.this, "Error fetching passwords", Toast.LENGTH_SHORT).show();
                return; // Stop the backup process if there's an error
            }

            // 4. Encrypt and write to a backup file
            StringBuilder encryptedDataBuilder = new StringBuilder();
            try {
                for (PasswordEntry entry : passwordEntries) {
                    String entryData = entry.getWebsite() + "," + entry.getUsername() + "," + EncryptionUtil.decrypt(this, entry.getEncryptedPassword());
                    Log.d("Backup", "Entry to encrypt: " + entryData);
                    String encryptedData = EncryptionUtil.encryptWithPassphrase(entryData, passphrase);
                    encryptedDataBuilder.append(encryptedData).append("\n");
                    Log.d("Backup", "Encrypted entry: " + encryptedData);
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(MainActivity.this, "Backup failed", Toast.LENGTH_SHORT).show();
                return;
            }
            String encryptedData = encryptedDataBuilder.toString();

            // 5. Use MediaStore to create a file and get content URI
            ContentValues values = new ContentValues();
            values.put(MediaStore.Files.FileColumns.DISPLAY_NAME, "password_backup.txt");
            values.put(MediaStore.Files.FileColumns.MIME_TYPE, "text/plain");
            values.put(MediaStore.Files.FileColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/PasswordManagerBackups/");

            Uri contentUri = getContentResolver().insert(MediaStore.Files.getContentUri("external"), values);

            if (contentUri != null) {
                try {
                    // 6. Open an OutputStream and write the encrypted data
                    try (OutputStream outputStream = getContentResolver().openOutputStream(contentUri)) {
                        outputStream.write(encryptedData.getBytes());
                        outputStream.flush();
                    }
                    Toast.makeText(MainActivity.this, "Backup successful", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this, "Backup failed", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(MainActivity.this, "Failed to create backup file", Toast.LENGTH_SHORT).show();
            }

            // Close the dialog after processing the passphrase
            dialog.dismiss();
        });
    }

    // Helper method to check passphrase strength
    private boolean isPassphraseStrong(String passphrase) {
        int strengthPoints = 0;
        if (passphrase.length() >= 12) strengthPoints++;
        if (passphrase.matches(".*[A-Z].*")) strengthPoints++;
        if (passphrase.matches(".*\\d.*")) strengthPoints++;
        if (passphrase.matches(".*[^A-Za-z0-9].*")) strengthPoints++;

        return strengthPoints == 4;
    }


    private void handleRestoreFile(Uri uri) {
        Log.d("Restore", "handleRestoreFile() called with URI: " + uri);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter Passphrase");

        TextInputLayout textInputLayout = new TextInputLayout(this, null);
        textInputLayout.setEndIconMode(TextInputLayout.END_ICON_PASSWORD_TOGGLE);

        TextInputEditText input = new TextInputEditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        textInputLayout.addView(input);

        // 4. Apply layout parameters to ensure the TextInputLayout is displayed correctly
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        textInputLayout.setLayoutParams(params);

        // 5. Set TextInputLayout as the view for the AlertDialog
        builder.setView(textInputLayout);

        builder.setPositiveButton("OK", (dialog, which) -> {
            String passphrase = input.getText().toString();
            Log.d("Restore", "Passphrase entered: " + passphrase);

            try (InputStream inputStream = getContentResolver().openInputStream(uri);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {

                String line;
                int lineNumber = 0;
                while ((line = reader.readLine()) != null) {
                    lineNumber++;
                    Log.d("Restore", "Line " + lineNumber + " read from file: " + line);

                    if (line.trim().isEmpty()) {
                        Log.e("Restore", "Line " + lineNumber + " is empty or contains only whitespace.");
                        continue;
                    }

                    try {
                        Log.d("Decrypt", "Attempting to decrypt line " + lineNumber + " with passphrase: " + passphrase);
                        String decryptedEntry = EncryptionUtil.decryptWithPassphrase(line, passphrase);
                        Log.d("Restore", "Line " + lineNumber + " decrypted: " + decryptedEntry);

                        if (!decryptedEntry.isEmpty()) {
                            String[] parts = decryptedEntry.split(",");
                            if (parts.length == 3) {
                                String website = parts[0];
                                String username = parts[1];
                                String decryptedPassword = parts[2];

                                Log.d("Restore", "Line " + lineNumber + " parsed - Website: " + website +
                                        ", Username: " + username + ", Password: " + decryptedPassword);

                                String encryptedPassword = EncryptionUtil.encrypt(this, decryptedPassword);
                                PasswordEntry newEntry = new PasswordEntry(website, username, encryptedPassword);
                                passwordViewModel.insert(newEntry);
                                Log.d("Restore", "Line " + lineNumber + " inserted into database.");
                            } else {
                                Log.e("Restore", "Line " + lineNumber + " parsing error: Invalid format.");
                            }
                        }

                    } catch (Exception e) {
                        Log.e("Restore", "Line " + lineNumber + " decryption error: " + e.getMessage());
                        e.printStackTrace();
                    }
                }

                Toast.makeText(this, "Restore complete", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Log.e("Restore", "Error reading or decrypting backup: " + e.getMessage());
                e.printStackTrace();
                Toast.makeText(this, "Error reading or decrypting backup", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }






    /**
     * Show the password after successful biometric authentication.
     */
    private void showPassword() {
        if (passwordEntryToShow != null) {
            try {
                String decryptedPassword = EncryptionUtil.decrypt(this, passwordEntryToShow.getEncryptedPassword());
                showPasswordDialog(decryptedPassword);
                passwordEntryToShow = null; // Reset after action
            } catch (Exception e) {
                Log.e("Search exception: ", e.toString());
                Toast.makeText(MainActivity.this, "Error decrypting password", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(MainActivity.this, "No password entry selected", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Filter password entries based on the search query.
     *
     * @param query The search query string.
     */
    private void filterPasswordEntries(String query) {
        new FilterPasswordEntriesTask().execute(query);
    }

    /**
     * Show a dialog displaying the decrypted password.
     *
     * @param password The decrypted password to display.
     */
    private void showPasswordDialog(String password) {
        new AlertDialog.Builder(this).setTitle("Password").setMessage(password).setPositiveButton("OK", null).show();
    }

    /**
     * Show a confirmation dialog to delete a password entry.
     *
     * @param passwordEntry The password entry to delete.
     */
    private void showDeleteConfirmationDialog(PasswordEntry passwordEntry) {
        new AlertDialog.Builder(this).setTitle("Delete Password").setMessage("Are you sure you want to delete this password?").setPositiveButton("Delete", (dialog, which) -> deletePassword(passwordEntry)).setNegativeButton("Cancel", null).show();
    }

    /**
     * Delete the specified password entry.
     *
     * @param passwordEntry The password entry to delete.
     */
    private void deletePassword(PasswordEntry passwordEntry) {
        passwordViewModel.delete(passwordEntry);
        Toast.makeText(this, "Password deleted", Toast.LENGTH_SHORT).show();
    }

    /**
     * Trigger biometric authentication for showing password.
     */
    private void authenticateUserForShow() {
        biometricPrompt = new BiometricPrompt(MainActivity.this, executor, showAuthenticationCallback);
        biometricPrompt.authenticate(showPromptInfo);
    }

    /**
     * Trigger biometric authentication for editing password.
     */
    private void authenticateUserForEdit() {
        biometricPrompt = new BiometricPrompt(MainActivity.this, executor, editAuthenticationCallback);
        biometricPrompt.authenticate(editPromptInfo);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_RESTORE_FILE && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                Uri uri = data.getData(); // Get the URI of the selected file
                Log.e("in restore", data.getData().toString());
                handleRestoreFile(uri);
            }
        } else if (requestCode == ADD_PASSWORD_REQUEST && resultCode == RESULT_OK) {
            String website = data.getStringExtra(AddEditPasswordActivity.EXTRA_WEBSITE);
            String username = data.getStringExtra(AddEditPasswordActivity.EXTRA_USERNAME);
            String password = data.getStringExtra(AddEditPasswordActivity.EXTRA_PASSWORD);

            try {
                String encryptedPassword = EncryptionUtil.encrypt(this, password);
                PasswordEntry passwordEntry = new PasswordEntry(website, username, encryptedPassword);
                passwordViewModel.insert(passwordEntry);
                Toast.makeText(this, "Password saved", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Log.e("Add password exception: ", e.toString());
                Toast.makeText(this, "Error saving password", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == EDIT_PASSWORD_REQUEST && resultCode == RESULT_OK) {
            int id = data.getIntExtra(AddEditPasswordActivity.EXTRA_ID, -1);
            if (id == -1) {
                Toast.makeText(this, "Password can't be updated", Toast.LENGTH_SHORT).show();
                return;
            }

            String website = data.getStringExtra(AddEditPasswordActivity.EXTRA_WEBSITE);
            String username = data.getStringExtra(AddEditPasswordActivity.EXTRA_USERNAME);
            String password = data.getStringExtra(AddEditPasswordActivity.EXTRA_PASSWORD);

            try {
                String encryptedPassword = EncryptionUtil.encrypt(this, password);
                PasswordEntry passwordEntry = new PasswordEntry(website, username, encryptedPassword);
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

    /**
     * AsyncTask for filtering password entries in the background.
     */
    private class FilterPasswordEntriesTask extends AsyncTask<String, Void, List<PasswordEntry>> {
        @Override
        protected List<PasswordEntry> doInBackground(String... params) {
            String query = params[0];
            List<PasswordEntry> filteredList = new ArrayList<>();
            List<PasswordEntry> allPasswordEntries;
            try {
                allPasswordEntries = passwordViewModel.getAllPasswordsSync();
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }

            for (PasswordEntry entry : allPasswordEntries) {
                if (entry.getWebsite().toLowerCase().contains(query.toLowerCase())) {
                    filteredList.add(entry);
                }
            }

            return filteredList;
        }

        @Override
        protected void onPostExecute(List<PasswordEntry> filteredList) {
            adapter.submitList(filteredList);
        }
    }
}