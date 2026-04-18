package com.jptechgenius.moneyhub.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.*;
import com.jptechgenius.moneyhub.data.local.entity.TransactionEntity;
import java.util.List;

/**
 * Data Access Object for financial transactions.
 * All queries returning LiveData are observed on the main thread;
 * write operations must be called from a background thread (ExecutorService).
 */
@Dao
public interface TransactionDao {

    // ---- INSERT ----

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(TransactionEntity transaction);

    // ---- UPDATE ----

    @Update
    void update(TransactionEntity transaction);

    // ---- DELETE ----

    @Delete
    void delete(TransactionEntity transaction);

    @Query("DELETE FROM transactions WHERE id = :id")
    void deleteById(int id);

    // ---- SELECT: All Transactions ----

    @Query("SELECT * FROM transactions ORDER BY date_millis DESC")
    LiveData<List<TransactionEntity>> getAllTransactions();

    /** For the Home screen: most recent N transactions */
    @Query("SELECT * FROM transactions ORDER BY date_millis DESC LIMIT :limit")
    LiveData<List<TransactionEntity>> getRecentTransactions(int limit);

    // ---- SELECT: By Type ----

    @Query("SELECT * FROM transactions WHERE type = 'income' ORDER BY date_millis DESC")
    LiveData<List<TransactionEntity>> getAllIncome();

    @Query("SELECT * FROM transactions WHERE type = 'expense' ORDER BY date_millis DESC")
    LiveData<List<TransactionEntity>> getAllExpenses();

    @Query("SELECT * FROM transactions WHERE type = 'transfer' ORDER BY date_millis DESC")
    LiveData<List<TransactionEntity>> getAllTransfers();

    // ---- SELECT: Summary Totals ----

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'income'")
    LiveData<Double> getTotalIncome();

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'expense'")
    LiveData<Double> getTotalExpenses();

    // ---- SELECT: Monthly Filter ----

    /**
     * Fetches all income transactions within a specific month.
     * @param startMillis  Start of the month (epoch ms)
     * @param endMillis    End of the month (epoch ms)
     */
    @Query("SELECT * FROM transactions WHERE type = 'income' " +
            "AND date_millis >= :startMillis AND date_millis <= :endMillis " +
            "ORDER BY date_millis DESC")
    LiveData<List<TransactionEntity>> getIncomeByMonth(long startMillis, long endMillis);

    @Query("SELECT * FROM transactions WHERE type = 'expense' " +
            "AND date_millis >= :startMillis AND date_millis <= :endMillis " +
            "ORDER BY date_millis DESC")
    LiveData<List<TransactionEntity>> getExpensesByMonth(long startMillis, long endMillis);

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'income' " +
            "AND date_millis >= :startMillis AND date_millis <= :endMillis")
    LiveData<Double> getTotalIncomeByMonth(long startMillis, long endMillis);

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'expense' " +
            "AND date_millis >= :startMillis AND date_millis <= :endMillis")
    LiveData<Double> getTotalExpensesByMonth(long startMillis, long endMillis);

    // ---- SELECT: Today's Transactions ----

    @Query("SELECT * FROM transactions WHERE date_millis >= :startOfDay " +
            "AND date_millis <= :endOfDay ORDER BY date_millis DESC")
    LiveData<List<TransactionEntity>> getTodayTransactions(long startOfDay, long endOfDay);

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'income' " +
            "AND date_millis >= :startOfDay AND date_millis <= :endOfDay")
    LiveData<Double> getTodayTotalIncome(long startOfDay, long endOfDay);

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'expense' " +
            "AND date_millis >= :startOfDay AND date_millis <= :endOfDay")
    LiveData<Double> getTodayTotalExpenses(long startOfDay, long endOfDay);

    // ---- SELECT: Search ----

    @Query("SELECT * FROM transactions WHERE " +
            "reason LIKE '%' || :query || '%' OR " +
            "category LIKE '%' || :query || '%' " +
            "ORDER BY date_millis DESC")
    LiveData<List<TransactionEntity>> searchTransactions(String query);

    // ---- SELECT: Yearly ----

    @Query("SELECT * FROM transactions WHERE " +
            "date_millis >= :startOfYear AND date_millis <= :endOfYear " +
            "ORDER BY date_millis DESC")
    LiveData<List<TransactionEntity>> getTransactionsByYear(long startOfYear, long endOfYear);
}