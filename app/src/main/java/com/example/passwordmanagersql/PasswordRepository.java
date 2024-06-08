package com.example.passwordmanagersql;

import android.app.Application;
import androidx.lifecycle.LiveData;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class PasswordRepository {
    private PasswordDao passwordDao;
    private LiveData<List<PasswordEntry>> allPasswords;

    public PasswordRepository(Application application) {
        PasswordDatabase db = PasswordDatabase.getDatabase(application);
        passwordDao = db.passwordDao();
        allPasswords = passwordDao.getAllPasswords();
    }


    public LiveData<List<PasswordEntry>> getAllPasswords() {
        return allPasswords;
    }

    public List<PasswordEntry> getAllPasswordsSync() throws ExecutionException, InterruptedException {
        return PasswordDatabase.databaseWriteExecutor.submit(() -> passwordDao.getAllPasswordsSync()).get();
    }

    public void insert(PasswordEntry passwordEntry) {
        PasswordDatabase.databaseWriteExecutor.execute(() -> passwordDao.insert(passwordEntry));
    }

    public void update(PasswordEntry passwordEntry) {
        PasswordDatabase.databaseWriteExecutor.execute(() -> passwordDao.update(passwordEntry));
    }

    public void delete(PasswordEntry passwordEntry) {
        PasswordDatabase.databaseWriteExecutor.execute(() -> passwordDao.delete(passwordEntry));
    }
}
