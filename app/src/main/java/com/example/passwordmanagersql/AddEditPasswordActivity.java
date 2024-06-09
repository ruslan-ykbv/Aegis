package com.example.passwordmanagersql;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;

public class AddEditPasswordActivity extends AppCompatActivity {
    public static final String EXTRA_ID = "com.example.passwordmanager.EXTRA_ID";
    public static final String EXTRA_WEBSITE = "com.example.passwordmanager.EXTRA_WEBSITE";
    public static final String EXTRA_PASSWORD = "com.example.passwordmanager.EXTRA_PASSWORD";
    public static final String EXTRA_USERNAME = "com.example.passwordmanager.EXTRA_USERNAME";

    private TextInputEditText editTextWebsite;
    private TextInputEditText editTextPassword;

    private TextInputEditText editTextUsername;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_password);

        // Hide the status bar
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);


        editTextWebsite = findViewById(R.id.edit_text_website);
        editTextPassword = findViewById(R.id.edit_text_password);
        editTextUsername = findViewById(R.id.edit_text_username);
        ImageView buttonSavePassword = findViewById(R.id.button_save_password);

        Intent intent = getIntent();
        if (intent.hasExtra(EXTRA_ID)) {
            setTitle("Edit Password");
            editTextWebsite.setText(intent.getStringExtra(EXTRA_WEBSITE));
            editTextUsername.setText(intent.getStringExtra(EXTRA_USERNAME)); // Set the username
            try {
                String decryptedPassword = EncryptionUtil.decrypt(getApplicationContext(), intent.getStringExtra(EXTRA_PASSWORD));
                editTextPassword.setText(decryptedPassword);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            setTitle("Add Password");
        }

        buttonSavePassword.setOnClickListener(v -> savePassword());
    }

    private void savePassword() {
        String website = editTextWebsite.getText().toString().trim();
        String username = editTextUsername.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        if (TextUtils.isEmpty(website) || TextUtils.isEmpty(password)) {
            setResult(RESULT_CANCELED);
        } else {
            Intent data = new Intent();
            data.putExtra(EXTRA_WEBSITE, website);
            data.putExtra(EXTRA_USERNAME, username);
            data.putExtra(EXTRA_PASSWORD, password);

            int id = getIntent().getIntExtra(EXTRA_ID, -1);
            if (id != -1) {
                data.putExtra(EXTRA_ID, id);
            }

            setResult(RESULT_OK, data);
        }
        finish();
    }
}
