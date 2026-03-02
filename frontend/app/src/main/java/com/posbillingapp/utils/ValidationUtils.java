package com.posbillingapp.utils;

import java.util.regex.Pattern;

public class ValidationUtils {

    // Bank Validation
    private static final String IFSC_REGEX = "^[A-Z]{4}0[A-Z0-9]{6}$";
    private static final String ACCOUNT_NUMBER_REGEX = "^\\d{8,18}$";

    public static boolean isValidIFSC(String ifsc) {
        if (ifsc == null) return false;
        return Pattern.compile(IFSC_REGEX).matcher(ifsc.toUpperCase()).matches();
    }

    public static boolean isValidAccountNumber(String accountNumber) {
        if (accountNumber == null) return false;
        return Pattern.compile(ACCOUNT_NUMBER_REGEX).matcher(accountNumber).matches();
    }

    // Card Validation (Luhn Algorithm)
    public static boolean isValidCardNumber(String cardNumber) {
        if (cardNumber == null) return false;
        String digits = cardNumber.replaceAll("\\D", "");
        if (digits.length() < 13 || digits.length() > 19) return false;

        int sum = 0;
        boolean alternate = false;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int n = Integer.parseInt(digits.substring(i, i + 1));
            if (alternate) {
                n *= 2;
                if (n > 9) {
                    n = (n % 10) + 1;
                }
            }
            sum += n;
            alternate = !alternate;
        }
        return (sum % 10 == 0);
    }

    public static String getCardType(String cardNumber) {
        if (cardNumber == null) return "Unknown";
        String digits = cardNumber.replaceAll("\\D", "");
        if (digits.startsWith("4")) return "Visa";
        if (digits.matches("^(5[1-5]|2[2-7]).*")) return "Mastercard";
        if (digits.matches("^(60|65|81|82).*")) return "RuPay";
        return "Generic Card";
    }

    public static boolean isValidExpiry(String month, String year) {
        try {
            int m = Integer.parseInt(month);
            int y = Integer.parseInt(year);
            if (m < 1 || m > 12) return false;
            
            // Assuming 20YY format
            int currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) % 100;
            int currentMonth = java.util.Calendar.getInstance().get(java.util.Calendar.MONTH) + 1;
            
            if (y < currentYear) return false;
            if (y == currentYear && m < currentMonth) return false;
            
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
