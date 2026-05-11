package com.qarz.app.models;

public class Loan {
    private String loanId;
    private String lenderId;
    private String borrowerId;
    private double amount;
    private String description;
    private long timestamp;
    private String status;
    private long loanDate;
    private long dueDate;
    private boolean sharedLoan;
    private String sharedGroupId;
    private int splitCount;
    private double originalTotalAmount;
    private boolean settlementRequested;
    private String settlementRequestedBy;
    private long settlementRequestedAt;

    public Loan() {
        // Default constructor required for calls to DataSnapshot.getValue(Loan.class) or Firestore parsing
    }

    public Loan(String loanId, String lenderId, String borrowerId, double amount, String description, long timestamp, String status, long loanDate, long dueDate) {
        this.loanId = loanId;
        this.lenderId = lenderId;
        this.borrowerId = borrowerId;
        this.amount = amount;
        this.description = description;
        this.timestamp = timestamp;
        this.status = status;
        this.loanDate = loanDate;
        this.dueDate = dueDate;
        this.sharedLoan = false;
        this.sharedGroupId = null;
        this.splitCount = 1;
        this.originalTotalAmount = amount;
        this.settlementRequested = false;
        this.settlementRequestedBy = null;
        this.settlementRequestedAt = 0L;
    }

    public String getLoanId() { return loanId; }
    public void setLoanId(String loanId) { this.loanId = loanId; }

    public String getLenderId() { return lenderId; }
    public void setLenderId(String lenderId) { this.lenderId = lenderId; }

    public String getBorrowerId() { return borrowerId; }
    public void setBorrowerId(String borrowerId) { this.borrowerId = borrowerId; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getLoanDate() { return loanDate; }
    public void setLoanDate(long loanDate) { this.loanDate = loanDate; }

    public long getDueDate() { return dueDate; }
    public void setDueDate(long dueDate) { this.dueDate = dueDate; }

    public boolean isSharedLoan() { return sharedLoan; }
    public void setSharedLoan(boolean sharedLoan) { this.sharedLoan = sharedLoan; }

    public String getSharedGroupId() { return sharedGroupId; }
    public void setSharedGroupId(String sharedGroupId) { this.sharedGroupId = sharedGroupId; }

    public int getSplitCount() { return splitCount; }
    public void setSplitCount(int splitCount) { this.splitCount = splitCount; }

    public double getOriginalTotalAmount() { return originalTotalAmount; }
    public void setOriginalTotalAmount(double originalTotalAmount) { this.originalTotalAmount = originalTotalAmount; }

    public boolean isSettlementRequested() { return settlementRequested; }
    public void setSettlementRequested(boolean settlementRequested) { this.settlementRequested = settlementRequested; }

    public String getSettlementRequestedBy() { return settlementRequestedBy; }
    public void setSettlementRequestedBy(String settlementRequestedBy) { this.settlementRequestedBy = settlementRequestedBy; }

    public long getSettlementRequestedAt() { return settlementRequestedAt; }
    public void setSettlementRequestedAt(long settlementRequestedAt) { this.settlementRequestedAt = settlementRequestedAt; }
}
