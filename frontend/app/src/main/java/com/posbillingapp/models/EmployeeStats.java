package com.posbillingapp.models;

public class EmployeeStats {
    private int totalReceipts;
    private double totalAmount;

    public EmployeeStats(int totalReceipts, double totalAmount) {
        this.totalReceipts = totalReceipts;
        this.totalAmount = totalAmount;
    }

    public int getTotalReceipts() {
        return totalReceipts;
    }

    public void setTotalReceipts(int totalReceipts) {
        this.totalReceipts = totalReceipts;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }
}
