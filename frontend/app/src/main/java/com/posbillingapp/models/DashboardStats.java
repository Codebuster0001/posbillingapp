package com.posbillingapp.models;

public class DashboardStats {
    private boolean success;
    private int employeeCount;
    private double totalCollection;
    private int receiptCount;

    // Getters and Setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public int getEmployeeCount() { return employeeCount; }
    public void setEmployeeCount(int employeeCount) { this.employeeCount = employeeCount; }
    public double getTotalCollection() { return totalCollection; }
    public void setTotalCollection(double totalCollection) { this.totalCollection = totalCollection; }
    public int getReceiptCount() { return receiptCount; }
    public void setReceiptCount(int receiptCount) { this.receiptCount = receiptCount; }
}
