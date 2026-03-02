package com.posbillingapp.models;

public class AuthModels {
    
    public static class RegisterRequest {
        @com.google.gson.annotations.SerializedName("CompanyName")
        public String companyName;
        @com.google.gson.annotations.SerializedName("PhoneNumber")
        public String phoneNumber;
        @com.google.gson.annotations.SerializedName("Email")
        public String email;
        @com.google.gson.annotations.SerializedName("Password")
        public String password;

        @com.google.gson.annotations.SerializedName("CountryId")
        public int countryId;
        @com.google.gson.annotations.SerializedName("StateId")
        public int stateId;
        @com.google.gson.annotations.SerializedName("CityId")
        public int cityId;

        public RegisterRequest(String companyName, String phoneNumber, String email, String password, int countryId, int stateId, int cityId) {
            this.companyName = companyName;
            this.phoneNumber = phoneNumber;
            this.email = email;
            this.password = password;
            this.countryId = countryId;
            this.stateId = stateId;
            this.cityId = cityId;
        }
    }

    public static class LoginRequest {
        public String email;
        public String password;

        public LoginRequest(String email, String password) {
            this.email = email;
            this.password = password;
        }
    }

    public static class ForgotPasswordRequest {
        public String email;
        public ForgotPasswordRequest(String email) { this.email = email; }
    }

    public static class ResetPasswordRequest {
        public String email;
        public String otpCode;
        public String newPassword;

        public ResetPasswordRequest(String email, String otpCode, String newPassword) {
            this.email = email;
            this.otpCode = otpCode;
            this.newPassword = newPassword;
        }
    }

    public static class RefreshRequest {
        public String refreshToken;
        public RefreshRequest(String refreshToken) { this.refreshToken = refreshToken; }
    }

    public static class AuthResponse {
        @com.google.gson.annotations.SerializedName("success")
        public boolean success;
        @com.google.gson.annotations.SerializedName("message")
        public String message;
        @com.google.gson.annotations.SerializedName("token")
        public String token;
        @com.google.gson.annotations.SerializedName("role")
        public String role;
        @com.google.gson.annotations.SerializedName("companyId")
        public Long companyId;
        @com.google.gson.annotations.SerializedName("userId")
        public Long userId;
        @com.google.gson.annotations.SerializedName("companyName")
        public String companyName;
        @com.google.gson.annotations.SerializedName("companyLogo")
        public String companyLogo;
        @com.google.gson.annotations.SerializedName("currencySymbol")
        public String currencySymbol;
        @com.google.gson.annotations.SerializedName("currencyCode")
        public String currencyCode;
        @com.google.gson.annotations.SerializedName("permissions")
        public java.util.List<String> permissions;
    }
}
