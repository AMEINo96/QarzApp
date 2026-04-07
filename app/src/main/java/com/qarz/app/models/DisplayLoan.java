package com.qarz.app.models;

public class DisplayLoan {
    private String loanId;
    private String displayTitle;
    private double amount;
    private String description;

    public DisplayLoan(String loanId, String displayTitle, double amount, String description) {
        this.loanId = loanId;
        this.displayTitle = displayTitle;
        this.amount = amount;
        this.description = description;
    }

    public String getLoanId() { return loanId; }
    public String getDisplayTitle() { return displayTitle; }
    public double getAmount() { return amount; }
    public String getDescription() { return description; }
}
