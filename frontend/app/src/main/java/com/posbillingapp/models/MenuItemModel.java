package com.posbillingapp.models;

public class MenuItemModel {
    @com.google.gson.annotations.SerializedName("id")
    private long id;
    @com.google.gson.annotations.SerializedName("companyId")
    private long companyId;
    @com.google.gson.annotations.SerializedName("name")
    private String name;
    @com.google.gson.annotations.SerializedName("category")
    private String category;
    @com.google.gson.annotations.SerializedName("price")
    private double price;
    @com.google.gson.annotations.SerializedName("description")
    private String description;
    @com.google.gson.annotations.SerializedName("imageUrl")
    private String imageUrl;
    @com.google.gson.annotations.SerializedName("isAvailable")
    private boolean isAvailable;

    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getCompanyId() { return companyId; }
    public void setCompanyId(long companyId) { this.companyId = companyId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public boolean isAvailable() { return isAvailable; }
    public void setAvailable(boolean available) { isAvailable = available; }

    public static class AddMenuItemRequest {
        public long companyId;
        public String name;
        public String category;
        public double price;
        public String description;
        public String imageUrl;
        public boolean isAvailable;

        public AddMenuItemRequest(long companyId, String name, String category, double price, String description, String imageUrl, boolean isAvailable) {
            this.companyId = companyId;
            this.name = name;
            this.category = category;
            this.price = price;
            this.description = description != null ? description : "";
            this.imageUrl = imageUrl != null ? imageUrl : "";
            this.isAvailable = isAvailable;
        }
    }
}
