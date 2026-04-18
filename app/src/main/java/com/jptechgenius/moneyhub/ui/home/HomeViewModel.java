package com.jptechgenius.moneyhub.ui.home;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.*;
import com.jptechgenius.moneyhub.data.local.entity.TransactionEntity;
import com.jptechgenius.moneyhub.data.repository.TransactionRepository;
import java.util.List;
import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * ViewModel for the Home (Dashboard) screen.
 * Survives configuration changes. Exposes LiveData to the Fragment.
 */
@HiltViewModel
public class HomeViewModel extends ViewModel {

    private final TransactionRepository repository;

    // Exposed to UI
    public final LiveData<List<TransactionEntity>> recentTransactions;
    public final LiveData<Double> totalIncome;
    public final LiveData<Double> totalExpenses;

    /**
     * Computed balance: Income - Expenses.
     * Uses MediatorLiveData to combine two sources.
     */
    private final MediatorLiveData<Double> currentBalance = new MediatorLiveData<>();

    @Inject
    public HomeViewModel(@NonNull TransactionRepository repository) {
        this.repository = repository;

        recentTransactions = repository.getRecentTransactions(20);
        totalIncome        = repository.totalIncome;
        totalExpenses      = repository.totalExpenses;

        // Observe both income and expense totals to compute balance
        currentBalance.addSource(totalIncome, income -> {
            Double expense = totalExpenses.getValue();
            double i = income != null ? income : 0.0;
            double e = expense != null ? expense : 0.0;
            currentBalance.setValue(i - e);
        });

        currentBalance.addSource(totalExpenses, expense -> {
            Double income = totalIncome.getValue();
            double i = income != null ? income : 0.0;
            double e = expense != null ? expense : 0.0;
            currentBalance.setValue(i - e);
        });
    }

    public LiveData<Double> getCurrentBalance() {
        return currentBalance;
    }

    public LiveData<TransactionEntity> getTransactionById(int id) {
        return repository.getTransactionById(id);
    }

    public void insert(TransactionEntity transaction) {
        repository.insert(transaction);
    }

    public void delete(TransactionEntity transaction) {
        repository.delete(transaction);
    }
}