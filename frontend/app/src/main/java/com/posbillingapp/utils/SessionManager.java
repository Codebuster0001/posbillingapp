package com.posbillingapp.utils;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;
import android.util.Log;
import java.io.IOException;
import java.security.GeneralSecurityException;

public class SessionManager {
    private static final String PREF_NAME = "EncryptedPOSSession";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_USER_ROLE = "userRole";
    private static final String KEY_COMPANY_ID = "companyId";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_COMPANY_NAME = "companyName";
    private static final String KEY_COMPANY_LOGO = "companyLogo";
    private static final String KEY_CURRENCY_SYMBOL = "currencySymbol";
    private static final String KEY_CURRENCY_CODE = "currencyCode";
    private static final String KEY_TOKEN = "jwtToken";
    private static final String KEY_REFRESH_TOKEN = "refreshToken";
    private static final String KEY_SERVER_IP = "serverIp";
    private static final String KEY_PERMISSIONS = "permissions";

    private SharedPreferences pref;
    private SharedPreferences.Editor editor;
    private Context context;

    public SessionManager(Context context) {
        this.context = context;
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            pref = EncryptedSharedPreferences.create(
                    context,
                    PREF_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
            editor = pref.edit();
        } catch (GeneralSecurityException | IOException e) {
            Log.e("SessionManager", "Encryption Error: " + e.getMessage());
            // Fallback to normal shared preferences if encryption fails (optional, but safer for usability)
            pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            editor = pref.edit();
        }
    }

    public void createLoginSession(String role, long companyId, long userId, String companyName, String companyLogo, String currencySymbol, String currencyCode, String token, String refreshToken, java.util.List<String> permissions) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putString(KEY_USER_ROLE, role);
        editor.putLong(KEY_COMPANY_ID, companyId);
        editor.putLong(KEY_USER_ID, userId);
        editor.putString(KEY_COMPANY_NAME, companyName);
        editor.putString(KEY_COMPANY_LOGO, companyLogo);
        editor.putString(KEY_CURRENCY_SYMBOL, currencySymbol);
        editor.putString(KEY_CURRENCY_CODE, currencyCode);
        editor.putString(KEY_TOKEN, token);
        editor.putString(KEY_REFRESH_TOKEN, refreshToken);
        
        if (permissions != null) {
            String permsString = android.text.TextUtils.join(",", permissions);
            editor.putString(KEY_PERMISSIONS, permsString);
        } else {
            editor.putString(KEY_PERMISSIONS, "");
        }
        
        editor.apply();
    }

    public void updatePermissions(String newToken, java.util.List<String> permissions) {
        if (newToken != null) editor.putString(KEY_TOKEN, newToken);
        if (permissions != null) {
            String permsString = android.text.TextUtils.join(",", permissions);
            editor.putString(KEY_PERMISSIONS, permsString);
        }
        editor.apply();
    }

    public boolean isLoggedIn() {
        return pref.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public String getUserRole() {
        return pref.getString(KEY_USER_ROLE, "").trim();
    }

    public long getCompanyId() {
        return pref.getLong(KEY_COMPANY_ID, -1);
    }

    public long getUserId() {
        return pref.getLong(KEY_USER_ID, -1);
    }

    public String getCompanyName() {
        return pref.getString(KEY_COMPANY_NAME, "POS SYSTEM");
    }

    public String getCompanyLogo() {
        return pref.getString(KEY_COMPANY_LOGO, "");
    }
    
    public String getCurrencySymbol() {
        return pref.getString(KEY_CURRENCY_SYMBOL, "$");
    }

    public String getCurrencyCode() {
        return pref.getString(KEY_CURRENCY_CODE, "USD");
    }

    public String getToken() {
        return pref.getString(KEY_TOKEN, "");
    }

    public String getRefreshToken() {
        return pref.getString(KEY_REFRESH_TOKEN, "");
    }

    public void updateServerIp(String ip) {
        editor.putString(KEY_SERVER_IP, ip);
        editor.apply();
    }

    public String getServerIp() {
        return pref.getString(KEY_SERVER_IP, "192.168.0.102");
    }

    public void logoutUser() {
        editor.clear();
        editor.apply();
    }

    public boolean hasPermission(String permission) {
        if ("Admin".equalsIgnoreCase(getUserRole())) return true;
        String permsString = pref.getString(KEY_PERMISSIONS, "");
        if (permsString.isEmpty()) return false;
        
        String[] permsArray = permsString.split(",");
        for (String p : permsArray) {
            if (p.trim().equalsIgnoreCase(permission)) return true;
        }
        return false;
    }
}
