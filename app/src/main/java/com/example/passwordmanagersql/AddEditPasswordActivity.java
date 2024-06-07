package com.example.passwordmanagersql;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class AddEditPasswordActivity extends AppCompatActivity {
    public static final String EXTRA_ID = "com.example.passwordmanager.EXTRA_ID";
    public static final String EXTRA_WEBSITE = "com.example.passwordmanager.EXTRA_WEBSITE";
    public static final String EXTRA_PASSWORD = "com.example.passwordmanager.EXTRA_PASSWORD";

    private EditText editTextWebsite;
    private EditText editTextPassword;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_password);

        editTextWebsite = findViewById(R.id.edit_text_website);
        editTextPassword = findViewById(R.id.edit_text_password);
        Button buttonSavePassword = findViewById(R.id.button_save_password);

        Intent intent = getIntent();
        if (intent.hasExtra(EXTRA_ID)) {
            setTitle("Edit Password");
            editTextWebsite.setText(intent.getStringExtra(EXTRA_WEBSITE));
            try {
                String decryptedPassword = EncryptionUtil.decrypt(intent.getStringExtra(EXTRA_PASSWORD));
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
        String password = editTextPassword.getText().toString().trim();

        if (TextUtils.isEmpty(website) || TextUtils.isEmpty(password)) {
            setResult(RESULT_CANCELED);
        } else {
            Intent data = new Intent();
            data.putExtra(EXTRA_WEBSITE, website);
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
