package com.example.passwordmanagersql;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    public static final int ADD_PASSWORD_REQUEST = 1;
    public static final int EDIT_PASSWORD_REQUEST = 2;

    private PasswordViewModel passwordViewModel;

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
            Intent intent = new Intent(MainActivity.this, AddEditPasswordActivity.class);
            intent.putExtra(AddEditPasswordActivity.EXTRA_ID, passwordEntry.id);
            intent.putExtra(AddEditPasswordActivity.EXTRA_WEBSITE, passwordEntry.website);
            intent.putExtra(AddEditPasswordActivity.EXTRA_PASSWORD, passwordEntry.encryptedPassword);
            startActivityForResult(intent, EDIT_PASSWORD_REQUEST);
        });
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
