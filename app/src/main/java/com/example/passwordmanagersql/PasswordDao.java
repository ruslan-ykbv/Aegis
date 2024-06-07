package com.example.passwordmanagersql;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface PasswordDao {
    @Insert
    void insert(PasswordEntry passwordEntry);

    @Update
    void update(PasswordEntry passwordEntry);

    @Delete
    void delete(PasswordEntry passwordEntry);

    @Query("DELETE FROM password_table")
    void deleteAllPasswords();

    @Query("SELECT * FROM password_table ORDER BY website ASC")
    LiveData<List<PasswordEntry>> getAllPasswords();

    @Query("SELECT * FROM password_table ORDER BY website ASC")
    List<PasswordEntry> getAllPasswordsSync();
}
