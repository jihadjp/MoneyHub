package com.jptechgenius.moneyhub.data.local;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import com.jptechgenius.moneyhub.data.local.dao.*;
import com.jptechgenius.moneyhub.data.local.entity.*;

/**
 * Room database singleton for Money Hub.
 * Version must be incremented when schema changes, accompanied by a Migration.
 */
@Database(
        entities = {
                TransactionEntity.class,
                DebtEntity.class,
                NoteEntity.class
        },
        version = 1,
        exportSchema = true
)
public abstract class AppDatabase extends RoomDatabase {

    private static final String DB_NAME = "money_hub_db";
    private static volatile AppDatabase INSTANCE;

    // Abstract DAO accessors
    public abstract TransactionDao transactionDao();
    public abstract DebtDao debtDao();
    public abstract NoteDao noteDao();

    /**
     * Thread-safe singleton accessor using double-checked locking.
     */
    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    DB_NAME
                            )
                            .fallbackToDestructiveMigration() // Replace with proper Migrations in prod
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}