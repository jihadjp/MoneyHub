package com.jptechgenius.moneyhub.data.local.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Represents a financial transaction (Income, Expense, or Transfer).
 * Maps to the 'transactions' table in the Room database.
 */
@Entity(tableName = "transactions")
public class TransactionEntity {

    @PrimaryKey(autoGenerate = true)
    private int id;

    /** Type: "income", "expense", "transfer" */
    @ColumnInfo(name = "type")
    private String type;

    /** Category: "Salary", "Food", "Transport", etc. */
    @ColumnInfo(name = "category")
    private String category;

    /** User-provided description or reason */
    @ColumnInfo(name = "reason")
    private String reason;

    @ColumnInfo(name = "amount")
    private double amount;

    /** Stored as epoch milliseconds (long) for easy sorting & filtering */
    @ColumnInfo(name = "date_millis")
    private long dateMillis;

    /** For transfers: source account name */
    @ColumnInfo(name = "transfer_from")
    private String transferFrom;

    /** For transfers: destination account name */
    @ColumnInfo(name = "transfer_to")
    private String transferTo;

    /** ISO currency code, e.g. "USD", "BDT" */
    @ColumnInfo(name = "currency_code")
    private String currencyCode;

    // ---- Constructors ----

    public TransactionEntity() {}

    public TransactionEntity(String type, String category, String reason,
                             double amount, long dateMillis, String currencyCode) {
        this.type = type;
        this.category = category;
        this.reason = reason;
        this.amount = amount;
        this.dateMillis = dateMillis;
        this.currencyCode = currencyCode;
    }

    // ---- Getters & Setters ----

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public long getDateMillis() { return dateMillis; }
    public void setDateMillis(long dateMillis) { this.dateMillis = dateMillis; }

    public String getTransferFrom() { return transferFrom; }
    public void setTransferFrom(String transferFrom) { this.transferFrom = transferFrom; }

    public String getTransferTo() { return transferTo; }
    public void setTransferTo(String transferTo) { this.transferTo = transferTo; }

    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }
}