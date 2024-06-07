package com.example.passwordmanagersql;

import android.app.Application;
import androidx.annotation.NonNull;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import java.util.List;

public class PasswordViewModel extends AndroidViewModel {
    private PasswordRepository repository;
    private LiveData<List<PasswordEntry>> allPasswords;

    public PasswordViewModel(@NonNull Application application) {
        super(application);
        repository = new PasswordRepository(application);
        allPasswords = repository.getAllPasswords();
    }

    public LiveData<List<PasswordEntry>> getAllPasswords() {
        return allPasswords;
    }

    public void insert(PasswordEntry passwordEntry) {
        repository.insert(passwordEntry);
    }

    public void update(PasswordEntry passwordEntry) {
        repository.update(passwordEntry);
    }

    public void delete(PasswordEntry passwordEntry) {
        repository.delete(passwordEntry);
    }
}
