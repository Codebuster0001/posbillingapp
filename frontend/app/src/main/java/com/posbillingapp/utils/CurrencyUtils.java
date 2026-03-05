package com.posbillingapp.utils;

import java.text.NumberFormat;
import java.util.Locale;

public class CurrencyUtils {
    
    private static String currentSymbol = "$";

    /**
     * Sets the currency display prefix.
     * Priority: use currencySymbol (e.g. ₹, $, €) if available,
     * otherwise fall back to currencyCode (e.g. INR, USD, EUR).
     * 
     * @param symbol  The currency symbol (e.g. "₹")
     * @param code    The currency code (e.g. "INR")
     */
    public static void setCurrency(String symbol, String code) {
        if (symbol != null && !symbol.trim().isEmpty()) {
            currentSymbol = symbol.trim();
        } else if (code != null && !code.trim().isEmpty()) {
            currentSymbol = code.trim();
        }
        // else keep previous value
    }

    /**
     * Simple setter - kept for backward compatibility.
     * Prefers the given value if non-empty.
     */
    public static void setCurrencySymbol(String symbol) {
        if (symbol != null && !symbol.trim().isEmpty()) {
            currentSymbol = symbol.trim();
        }
    }

    public static String getCurrencySymbol() {
        return currentSymbol;
    }

    /**
     * Formats an amount with the currency prefix.
     * If the prefix is a multi-character code (like "INR"), adds a space: "INR 1,234.56"
     * If the prefix is a symbol (like "₹"), no space: "₹1,234.56"
     */
    public static String format(double amount) {
        try {
            NumberFormat format = NumberFormat.getNumberInstance(Locale.getDefault());
            format.setMinimumFractionDigits(2);
            format.setMaximumFractionDigits(2);
            String formattedNumber = format.format(amount);
            
            // If the symbol is all alphabetic (like a currency code), add a space
            if (currentSymbol.length() > 1 && currentSymbol.matches("[A-Za-z]+")) {
                return currentSymbol + " " + formattedNumber;
            }
            return currentSymbol + formattedNumber;
        } catch (Exception e) {
            return String.format("%s%.2f", currentSymbol, amount);
        }
    }
}
