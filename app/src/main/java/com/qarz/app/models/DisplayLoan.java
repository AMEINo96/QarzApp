package com.qarz.app.models;

public class DisplayLoan {
    private String loanId;
    private String displayTitle;
    private double amount;
    private String description;
    private long createdAt;

    public DisplayLoan(String loanId, String displayTitle, double amount, String description, long createdAt) {
        this.loanId = loanId;
        this.displayTitle = displayTitle;
        this.amount = amount;
        this.description = description;
        this.createdAt = createdAt;
    }

    public String getLoanId() { return loanId; }
    public String getDisplayTitle() { return displayTitle; }
    public double getAmount() { return amount; }
    public String getDescription() { return description; }
    public long getCreatedAt() { return createdAt; }
}
