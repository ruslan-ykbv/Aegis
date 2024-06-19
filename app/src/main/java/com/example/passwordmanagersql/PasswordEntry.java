package com.example.passwordmanagersql;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

// PasswordEntry.java
@Entity(tableName = "password_table")
public class PasswordEntry {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String website;
    public String username; // New field for username
    public String encryptedPassword;

    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public String getWebsite() {
        return website;
    }

    public String getUsername() {
        return username;
    }

    public String getEncryptedPassword() {
        return encryptedPassword;
    }

    public PasswordEntry(String website, String username, String encryptedPassword) {
        this.website = website;
        this.username = username;
        this.encryptedPassword = encryptedPassword;
    }
}
