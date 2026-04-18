package com.jptechgenius.moneyhub.di;

import com.jptechgenius.moneyhub.data.local.dao.DebtDao;
import com.jptechgenius.moneyhub.data.local.dao.NoteDao;
import com.jptechgenius.moneyhub.data.local.dao.TransactionDao;
import com.jptechgenius.moneyhub.data.repository.DebtRepository;
import com.jptechgenius.moneyhub.data.repository.NoteRepository;
import com.jptechgenius.moneyhub.data.repository.TransactionRepository;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import javax.inject.Singleton;

@Module
@InstallIn(SingletonComponent.class)
public class RepositoryModule {

    @Provides
    @Singleton
    public TransactionRepository provideTransactionRepository(TransactionDao transactionDao) {
        return new TransactionRepository(transactionDao);
    }

    @Provides
    @Singleton
    public DebtRepository provideDebtRepository(DebtDao debtDao) {
        return new DebtRepository(debtDao);
    }

    @Provides
    @Singleton
    public NoteRepository provideNoteRepository(NoteDao noteDao) {
        return new NoteRepository(noteDao);
    }
}
