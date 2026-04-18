package com.jptechgenius.moneyhub.data.local.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Represents a Debt or Credit record (money given or taken).
 * Maps to the 'debts' table in the Room database.
 */
@Entity(tableName = "debts")
public class DebtEntity {

    @PrimaryKey(autoGenerate = true)
    private int id;

    /** "given" (lent money) or "taken" (borrowed money) */
    @ColumnInfo(name = "debt_type")
    private String debtType;

    /** Name of the person involved */
    @ColumnInfo(name = "person_name")
    private String personName;

    /** Reason or note for this debt */
    @ColumnInfo(name = "reason")
    private String reason;

    @ColumnInfo(name = "amount")
    private double amount;

    /** Date the money was given/taken (epoch millis) */
    @ColumnInfo(name = "taken_date_millis")
    private long takenDateMillis;

    /** Due date for repayment (epoch millis) */
    @ColumnInfo(name = "due_date_millis")
    private long dueDateMillis;

    @ColumnInfo(name = "phone_number")
    private String phoneNumber;

    /** ISO currency code */
    @ColumnInfo(name = "currency_code")
    private String currencyCode;

    /** true = settled/paid, false = outstanding */
    @ColumnInfo(name = "is_settled")
    private boolean isSettled;

    // ---- Constructors ----

    public DebtEntity() {}

    public DebtEntity(String debtType, String personName, String reason,
                      double amount, long takenDateMillis, long dueDateMillis,
                      String phoneNumber, String currencyCode) {
        this.debtType = debtType;
        this.personName = personName;
        this.reason = reason;
        this.amount = amount;
        this.takenDateMillis = takenDateMillis;
        this.dueDateMillis = dueDateMillis;
        this.phoneNumber = phoneNumber;
        this.currencyCode = currencyCode;
        this.isSettled = false;
    }

    // ---- Getters & Setters ----

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getDebtType() { return debtType; }
    public void setDebtType(String debtType) { this.debtType = debtType; }

    public String getPersonName() { return personName; }
    public void setPersonName(String personName) { this.personName = personName; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public long getTakenDateMillis() { return takenDateMillis; }
    public void setTakenDateMillis(long takenDateMillis) { this.takenDateMillis = takenDateMillis; }

    public long getDueDateMillis() { return dueDateMillis; }
    public void setDueDateMillis(long dueDateMillis) { this.dueDateMillis = dueDateMillis; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }

    public boolean isSettled() { return isSettled; }
    public void setSettled(boolean settled) { isSettled = settled; }
}