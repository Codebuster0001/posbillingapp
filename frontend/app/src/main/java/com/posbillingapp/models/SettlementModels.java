package com.posbillingapp.models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class SettlementModels {

    public static class BankDetails implements Serializable {
        @SerializedName("id")
        public Long id;
        @SerializedName("accountHolderName")
        public String accountHolderName;
        @SerializedName("accountNumber")
        public String accountNumber;
        @SerializedName("ifscCode")
        public String ifscCode;
        @SerializedName("bankName")
        public String bankName;
        @SerializedName("isPrimary")
        public boolean isPrimary;
        @SerializedName("status")
        public String status;
        @SerializedName("registeredName")
        public String registeredName;
        @SerializedName("matchScore")
        public int matchScore;
        @SerializedName("branch")
        public String branch;
        @SerializedName("bankType")
        public String bankType;

        public BankDetails() {}
        
        public BankDetails(String name, String acc, String ifsc, String bank) {
            this.accountHolderName = name;
            this.accountNumber = acc;
            this.ifscCode = ifsc;
            this.bankName = bank;
        }
    }

    public static class CardDetails implements Serializable {
        @SerializedName("id")
        public Long id;
        @SerializedName("cardHolderName")
        public String cardHolderName;
        @SerializedName("cardNumber")
        public String cardNumber;
        @SerializedName("expiryMonth")
        public String expiryMonth;
        @SerializedName("expiryYear")
        public String expiryYear;
        @SerializedName("cvv") // Optional for validation only
        public String cvv;
        @SerializedName("isPrimary")
        public boolean isPrimary;
        @SerializedName("cardType")
        public String cardType;

        public CardDetails() {}
    }

    public static class ValidationResult implements Serializable {
        @SerializedName("verified")
        public boolean verified;
        @SerializedName("registeredName")
        public String registeredName;
        @SerializedName("matchScore")
        public int matchScore;
        @SerializedName("bankName")
        public String bankName;
        @SerializedName("branch")
        public String branch;
        @SerializedName("bankType")
        public String bankType;
        @SerializedName("verificationMessage")
        public String verificationMessage;
    }
}
