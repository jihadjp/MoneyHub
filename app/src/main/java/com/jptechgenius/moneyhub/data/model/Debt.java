package com.jptechgenius.moneyhub.data.model;

public class Debt {
    private long id;
    private String personName;
    private double amount;
    private String type; // DEBT or CREDIT
    private long date;
    private long dueDate;
    private boolean isPaid;

    public Debt(long id, String personName, double amount, String type, long date, long dueDate, boolean isPaid) {
        this.id = id;
        this.personName = personName;
        this.amount = amount;
        this.type = type;
        this.date = date;
        this.dueDate = dueDate;
        this.isPaid = isPaid;
    }

    public long getId() { return id; }
    public String getPersonName() { return personName; }
    public double getAmount() { return amount; }
    public String getType() { return type; }
    public long getDate() { return date; }
    public long getDueDate() { return dueDate; }
    public boolean isPaid() { return isPaid; }
}
