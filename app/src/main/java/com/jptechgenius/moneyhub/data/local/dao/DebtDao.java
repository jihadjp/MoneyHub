package com.jptechgenius.moneyhub.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.*;
import com.jptechgenius.moneyhub.data.local.entity.DebtEntity;
import java.util.List;

/**
 * Data Access Object for debt/credit records.
 */
@Dao
public interface DebtDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(DebtEntity debt);

    @Update
    void update(DebtEntity debt);

    @Delete
    void delete(DebtEntity debt);

    @Query("DELETE FROM debts WHERE id = :id")
    void deleteById(int id);

    /** All debts: both given and taken, sorted by date descending */
    @Query("SELECT * FROM debts ORDER BY taken_date_millis DESC")
    LiveData<List<DebtEntity>> getAllDebts();

    @Query("SELECT * FROM debts WHERE debt_type = :type ORDER BY taken_date_millis DESC")
    LiveData<List<DebtEntity>> getDebtsByType(String type);

    /** Money you have GIVEN (lent out) */
    @Query("SELECT * FROM debts WHERE debt_type = 'given' ORDER BY taken_date_millis DESC")
    LiveData<List<DebtEntity>> getGivenDebts();

    /** Money you have TAKEN (borrowed) */
    @Query("SELECT * FROM debts WHERE debt_type = 'taken' ORDER BY taken_date_millis DESC")
    LiveData<List<DebtEntity>> getTakenDebts();

    /** Sum of all given (un-settled) amounts */
    @Query("SELECT SUM(amount) FROM debts WHERE debt_type = 'given' AND is_settled = 0")
    LiveData<Double> getTotalGiven();

    /** Sum of all taken (un-settled) amounts */
    @Query("SELECT SUM(amount) FROM debts WHERE debt_type = 'taken' AND is_settled = 0")
    LiveData<Double> getTotalTaken();

    /** Unsettled debts only */
    @Query("SELECT * FROM debts WHERE is_settled = 0 ORDER BY due_date_millis ASC")
    LiveData<List<DebtEntity>> getOutstandingDebts();

    /** Mark as settled without deleting the record */
    @Query("UPDATE debts SET is_settled = 1 WHERE id = :id")
    void markAsSettled(int id);
}