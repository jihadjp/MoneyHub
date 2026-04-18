package com.jptechgenius.moneyhub.data.model;

public class Transaction {
    private long id;
    private String title;
    private double amount;
    private String type; // INCOME or EXPENSE
    private String category;
    private long date;
    private String note;

    public Transaction(long id, String title, double amount, String type, String category, long date, String note) {
        this.id = id;
        this.title = title;
        this.amount = amount;
        this.type = type;
        this.category = category;
        this.date = date;
        this.note = note;
    }

    public long getId() { return id; }
    public String getTitle() { return title; }
    public double getAmount() { return amount; }
    public String getType() { return type; }
    public String getCategory() { return category; }
    public long getDate() { return date; }
    public String getNote() { return note; }
}
