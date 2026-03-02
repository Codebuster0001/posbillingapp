package com.posbillingapp.utils;

import java.text.NumberFormat;
import java.util.Locale;

public class CurrencyUtils {
    
    private static String currentSymbol = "$";

    public static void setCurrencySymbol(String symbol) {
        if (symbol != null) {
            currentSymbol = symbol;
        }
    }

    public static String getCurrencySymbol() {
        return currentSymbol;
    }

    // Method to format amount with the set currency symbol
    public static String format(double amount) {
        try {
            // If using standard locale formatting, it might force the locale's symbol.
            // We want to force OUR symbol but keep the locale's number formatting (commas, decimals).
            NumberFormat format = NumberFormat.getNumberInstance(Locale.getDefault());
            format.setMinimumFractionDigits(2);
            format.setMaximumFractionDigits(2);
            return currentSymbol + format.format(amount);
        } catch (Exception e) {
            return String.format("%s%.2f", currentSymbol, amount); // Fallback
        }
    }
}
