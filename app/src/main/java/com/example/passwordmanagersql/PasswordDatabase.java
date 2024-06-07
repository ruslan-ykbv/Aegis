package com.example.passwordmanagersql;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {PasswordEntry.class}, version = 1, exportSchema = false)
public abstract class PasswordDatabase extends RoomDatabase {
    public abstract PasswordDao passwordDao();

    private static volatile PasswordDatabase INSTANCE;
    private static final int NUMBER_OF_THREADS = 4;
    static final ExecutorService databaseWriteExecutor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    static PasswordDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (PasswordDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    PasswordDatabase.class, "password_database")
                            .fallbackToDestructiveMigration()
                            .addCallback(sRoomDatabaseCallback)
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    private static RoomDatabase.Callback sRoomDatabaseCallback = new RoomDatabase.Callback() {
        @Override
        public void onOpen(SupportSQLiteDatabase db) {
            super.onOpen(db);
            // Optional: Add code to populate the database when it's created or opened
        }
    };
}
