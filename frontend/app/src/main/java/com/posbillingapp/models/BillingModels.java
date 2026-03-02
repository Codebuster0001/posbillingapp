package com.posbillingapp.models;

import java.util.List;

public class BillingModels {

    public static class OrderRequest {
        public long companyId;
        public long userId;
        public String billNumber;
        public double totalAmount;
        public List<OrderItemRequest> items;

        public OrderRequest(long companyId, long userId, String billNumber, double totalAmount, List<OrderItemRequest> items) {
            this.companyId = companyId;
            this.userId = userId;
            this.billNumber = billNumber;
            this.totalAmount = totalAmount;
            this.items = items;
        }
    }

    public static class OrderItemRequest {
        public long menuItemId;
        public String itemName;
        public double price;
        public int quantity;

        public OrderItemRequest(long menuItemId, String itemName, double price, int quantity) {
            this.menuItemId = menuItemId;
            this.itemName = itemName;
            this.price = price;
            this.quantity = quantity;
        }
    }

    public static class OrderResponse {
        public boolean success;
        public long orderId;
        public String message;
    }
}
