package com.posbillingapp.models;

public class EmployeeModel {
    private long id;
    private long companyId;
    private String name;
    private String email;
    private String phoneNumber;
    private String role;
    private String photoUrl;
    private boolean isActive;

    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getCompanyId() { return companyId; }
    public void setCompanyId(long companyId) { this.companyId = companyId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    
    // For Add Request
    public static class AddEmployeeRequest {
        public long companyId;
        public String name;
        public String email;
        public String phoneNumber;
        public String role;
        
        public AddEmployeeRequest(long companyId, String name, String email, String phoneNumber, String role) {
            this.companyId = companyId;
            this.name = name;
            this.email = email;
            this.phoneNumber = phoneNumber;
            this.role = role;
        }
    }
}
