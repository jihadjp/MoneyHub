package com.jptechgenius.moneyhub.di;

import android.content.Context;
import androidx.room.Room;
import com.jptechgenius.moneyhub.data.local.AppDatabase;
import com.jptechgenius.moneyhub.data.local.dao.DebtDao;
import com.jptechgenius.moneyhub.data.local.dao.NoteDao;
import com.jptechgenius.moneyhub.data.local.dao.TransactionDao;
import com.jptechgenius.moneyhub.util.Constants;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;
import javax.inject.Singleton;

@Module
@InstallIn(SingletonComponent.class)
public class DatabaseModule {

    @Provides
    @Singleton
    public AppDatabase provideDatabase(@ApplicationContext Context context) {
        return Room.databaseBuilder(context, AppDatabase.class, Constants.DATABASE_NAME)
                .fallbackToDestructiveMigration()
                .build();
    }

    @Provides
    @Singleton
    public TransactionDao provideTransactionDao(AppDatabase database) {
        return database.transactionDao();
    }

    @Provides
    @Singleton
    public DebtDao provideDebtDao(AppDatabase database) {
        return database.debtDao();
    }

    @Provides
    @Singleton
    public NoteDao provideNoteDao(AppDatabase database) {
        return database.noteDao();
    }
}
