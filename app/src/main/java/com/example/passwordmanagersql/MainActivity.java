package com.example.passwordmanagersql;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

public class MainActivity extends AppCompatActivity {
    public static final int ADD_PASSWORD_REQUEST = 1;
    public static final int EDIT_PASSWORD_REQUEST = 2;
    PasswordAdapter adapter;
    private PasswordViewModel passwordViewModel;
    private Executor executor;
    private BiometricPrompt biometricPrompt;

    private BiometricPrompt.PromptInfo showPromptInfo;
    private BiometricPrompt.PromptInfo editPromptInfo;
    private SearchView searchView;
    private PasswordEntry passwordEntryToShow; // Store the password entry temporarily

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        searchView = findViewById(R.id.search_view);
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

        // Initialize showPromptInfo
        showPromptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Biometric authentication")
                .setSubtitle("Log in using your biometric credential or device credential")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK | BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                .build();

        // Initialize editPromptInfo
        editPromptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Authenticate to edit password")
                .setSubtitle("Use your biometric credential")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK | BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                .build();

        ImageView buttonAddPassword = findViewById(R.id.button_add_password);
        buttonAddPassword.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddEditPasswordActivity.class);
            startActivityForResult(intent, ADD_PASSWORD_REQUEST);
        });

        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);

        adapter = new PasswordAdapter();
        recyclerView.setAdapter(adapter);

        passwordViewModel = new ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(this.getApplication())).get(PasswordViewModel.class);
        passwordViewModel.getAllPasswords().observe(this, adapter::submitList);

        adapter.setOnItemClickListener(passwordEntry -> {
            passwordEntryToShow = passwordEntry;
            authenticateUserForShow(); // Trigger biometric authentication for show
        });

        adapter.setOnEditItemClickListener(passwordEntry -> {
            passwordEntryToShow = passwordEntry;
            authenticateUserForEdit(); // Trigger biometric authentication for edit
        });

        adapter.setOnLongItemClickListener(position -> {
            PasswordEntry passwordEntry = adapter.passwords.get(position);
            showDeleteConfirmationDialog(passwordEntry);
        });

        executor = ContextCompat.getMainExecutor(this);
    }

    private void showPassword() {
        if (passwordEntryToShow != null) {
            try {
                String decryptedPassword = EncryptionUtil.decrypt(getApplicationContext(), passwordEntryToShow.getEncryptedPassword());
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

    private void filterPasswordEntries(String query) {
        new FilterPasswordEntriesTask().execute(query);
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
        biometricPrompt = new BiometricPrompt(MainActivity.this, executor, showAuthenticationCallback);
        biometricPrompt.authenticate(showPromptInfo);
    }

    private void authenticateUserForEdit() {
        biometricPrompt = new BiometricPrompt(MainActivity.this, executor, editAuthenticationCallback);
        biometricPrompt.authenticate(editPromptInfo);
    }

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
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == ADD_PASSWORD_REQUEST && resultCode == RESULT_OK) {
            String website = data.getStringExtra(AddEditPasswordActivity.EXTRA_WEBSITE);
            String username = data.getStringExtra(AddEditPasswordActivity.EXTRA_USERNAME);
            String password = data.getStringExtra(AddEditPasswordActivity.EXTRA_PASSWORD);

            try {
                String encryptedPassword = EncryptionUtil.encrypt(getApplicationContext(), password);
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
                String encryptedPassword = EncryptionUtil.encrypt(getApplicationContext(), password);
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
