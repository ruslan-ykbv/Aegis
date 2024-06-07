package com.example.passwordmanagersql;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "password_table")
public class PasswordEntry {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String website;
    public String encryptedPassword;

    public PasswordEntry(String website, String encryptedPassword) {
        this.website = website;
        this.encryptedPassword = encryptedPassword;
    }
}
