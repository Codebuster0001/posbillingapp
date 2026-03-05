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

    public static class OrderHistoryResponse {
        @com.google.gson.annotations.SerializedName(value="id", alternate={"Id"})
        public long id;
        @com.google.gson.annotations.SerializedName(value="companyId", alternate={"CompanyId"})
        public long companyId;
        @com.google.gson.annotations.SerializedName(value="userId", alternate={"UserId"})
        public Long userId;
        @com.google.gson.annotations.SerializedName(value="userName", alternate={"UserName"})
        public String userName;
        @com.google.gson.annotations.SerializedName(value="userRole", alternate={"UserRole"})
        public String userRole;
        @com.google.gson.annotations.SerializedName(value="billNumber", alternate={"BillNumber"})
        public String billNumber;
        @com.google.gson.annotations.SerializedName(value="totalAmount", alternate={"TotalAmount"})
        public double totalAmount;
        @com.google.gson.annotations.SerializedName(value="createdAt", alternate={"CreatedAt"})
        public String createdAt;
        @com.google.gson.annotations.SerializedName(value="items", alternate={"Items"})
        public List<OrderItemResponse> items;
    }

    public static class OrderItemResponse {
        @com.google.gson.annotations.SerializedName(value="id", alternate={"Id"})
        public long id;
        @com.google.gson.annotations.SerializedName(value="orderId", alternate={"OrderId"})
        public long orderId;
        @com.google.gson.annotations.SerializedName(value="itemName", alternate={"ItemName"})
        public String itemName;
        @com.google.gson.annotations.SerializedName(value="price", alternate={"Price"})
        public double price;
        @com.google.gson.annotations.SerializedName(value="quantity", alternate={"Quantity"})
        public int quantity;
    }
}
