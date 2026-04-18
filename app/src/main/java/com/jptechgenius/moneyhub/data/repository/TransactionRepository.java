package com.jptechgenius.moneyhub.data.repository;

import android.app.Application;
import androidx.lifecycle.LiveData;
import com.jptechgenius.moneyhub.data.local.AppDatabase;
import com.jptechgenius.moneyhub.data.local.dao.TransactionDao;
import com.jptechgenius.moneyhub.data.local.entity.TransactionEntity;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Single source of truth for transaction data.
 * Mediates between the ViewModel and the Room DAO.
 * All write operations are dispatched to a background ExecutorService.
 */
public class TransactionRepository {

    private final TransactionDao transactionDao;
    private final ExecutorService executorService;

    // Cached LiveData observed by ViewModel
    public final LiveData<List<TransactionEntity>> allTransactions;
    public final LiveData<Double> totalIncome;
    public final LiveData<Double> totalExpenses;

    public TransactionRepository(TransactionDao transactionDao) {
        this.transactionDao = transactionDao;
        executorService = Executors.newSingleThreadExecutor();

        allTransactions = transactionDao.getAllTransactions();
        totalIncome     = transactionDao.getTotalIncome();
        totalExpenses   = transactionDao.getTotalExpenses();
    }

    // ---- Write Operations (background thread) ----

    public void insert(TransactionEntity transaction) {
        executorService.execute(() -> transactionDao.insert(transaction));
    }

    public void update(TransactionEntity transaction) {
        executorService.execute(() -> transactionDao.update(transaction));
    }

    public void delete(TransactionEntity transaction) {
        executorService.execute(() -> transactionDao.delete(transaction));
    }

    // ---- Read Operations (LiveData, observed on main thread) ----

    public LiveData<List<TransactionEntity>> getRecentTransactions(int limit) {
        return transactionDao.getRecentTransactions(limit);
    }

    public LiveData<List<TransactionEntity>> getTodayTransactions(long start, long end) {
        return transactionDao.getTodayTransactions(start, end);
    }

    public LiveData<Double> getTodayTotalIncome(long start, long end) {
        return transactionDao.getTodayTotalIncome(start, end);
    }

    public LiveData<Double> getTodayTotalExpenses(long start, long end) {
        return transactionDao.getTodayTotalExpenses(start, end);
    }

    public LiveData<List<TransactionEntity>> getIncomeByMonth(long start, long end) {
        return transactionDao.getIncomeByMonth(start, end);
    }

    public LiveData<List<TransactionEntity>> getExpensesByMonth(long start, long end) {
        return transactionDao.getExpensesByMonth(start, end);
    }

    public LiveData<Double> getTotalIncomeByMonth(long start, long end) {
        return transactionDao.getTotalIncomeByMonth(start, end);
    }

    public LiveData<Double> getTotalExpensesByMonth(long start, long end) {
        return transactionDao.getTotalExpensesByMonth(start, end);
    }

    public LiveData<List<TransactionEntity>> searchTransactions(String query) {
        return transactionDao.searchTransactions(query);
    }

    public LiveData<List<TransactionEntity>> getTransactionsBetweenDates(long start, long end) {
        return transactionDao.getTodayTransactions(start, end);
    }
}