package com.posbillingapp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.posbillingapp.databinding.ActivityLoginBinding;
import com.posbillingapp.models.AuthModels.*;
import com.posbillingapp.network.RetrofitClient;
import com.posbillingapp.utils.SessionManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        sessionManager = new SessionManager(this);
        
        // Check for existing session
        if (sessionManager.isLoggedIn()) {
            // Initialize formatter on auto-login
            com.posbillingapp.utils.CurrencyUtils.setCurrency(sessionManager.getCurrencySymbol(), sessionManager.getCurrencyCode());
            goToDashboard(sessionManager.getUserRole(), sessionManager.getCompanyId());
            return;
        }

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnLogin.setOnClickListener(v -> {
            String email = binding.etEmail.getText().toString();
            String password = binding.etPassword.getText().toString();
            login(email, password);
        });

        binding.tvRegister.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
        });

        binding.tvForgotPassword.setOnClickListener(v -> {
            startActivity(new Intent(this, ForgotPasswordActivity.class));
        });

        // HIDDEN FEATURE: Long press title to change Server IP
        binding.tvTitle.setOnLongClickListener(v -> {
            showServerIpDialog();
            return true;
        });
    }

    private void showServerIpDialog() {
        android.widget.EditText et = new android.widget.EditText(this);
        et.setText(sessionManager.getServerIp());
        et.setHint("e.g. 192.168.1.18");
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        et.setPadding(padding, padding, padding, padding);

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Server Configuration")
                .setMessage("Enter the Backend Server IP Address (without http/port)")
                .setView(et)
                .setPositiveButton("Save", (dialog, which) -> {
                    String ip = et.getText().toString().trim();
                    if (!ip.isEmpty()) {
                        sessionManager.updateServerIp(ip);
                        RetrofitClient.resetClient();
                        Toast.makeText(this, "Server IP updated to: " + ip, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void login(String email, String password) {
        LoginRequest request = new LoginRequest(email, password);
        RetrofitClient.getApiService().login(request).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    AuthResponse authResponse = response.body();
                    if (authResponse.success) {
                        Toast.makeText(LoginActivity.this, "Login Successful!", Toast.LENGTH_SHORT).show();
                        
                        // Currency Priority: Symbol (₹, $) first, Code (INR, USD) as fallback
                        String currencySymbol = authResponse.currencySymbol;
                        String currencyCode = authResponse.currencyCode;
                        
                        // Use the new priority-based setter
                        com.posbillingapp.utils.CurrencyUtils.setCurrency(currencySymbol, currencyCode);
                        // Store the resolved symbol for session persistence
                        String resolvedSymbol = com.posbillingapp.utils.CurrencyUtils.getCurrencySymbol();

                        // Save session
                        sessionManager.createLoginSession(
                            authResponse.role, 
                            authResponse.companyId != null ? authResponse.companyId : -1,
                            authResponse.userId != null ? authResponse.userId : -1,
                            authResponse.companyName,
                            authResponse.companyLogo,
                            resolvedSymbol,
                            currencyCode != null ? currencyCode : "USD",
                            authResponse.token,
                            authResponse.refreshToken,
                            authResponse.permissions
                        );

                        goToDashboard(authResponse.role, authResponse.companyId != null ? authResponse.companyId : -1L);
                    } else {
                        Toast.makeText(LoginActivity.this, authResponse.message, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    String error = "Login failed: ";
                    try {
                        if (response.errorBody() != null) error += response.errorBody().string();
                        else error += "Invalid Credentials (" + response.code() + ")";
                    } catch (Exception e) {
                        error += "Code " + response.code();
                    }
                    Toast.makeText(LoginActivity.this, error, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                android.util.Log.e("LOGIN", "Fail: " + t.getMessage());
                String msg = "Connection Failed. Check Server IP (Long press 'POS Billing' title to update).";
                Toast.makeText(LoginActivity.this, msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void goToDashboard(String role, long companyId) {
        if ("Limited Access".equalsIgnoreCase(role)) {
            Intent intent = new Intent(this, CalculatorBillingActivity.class);
            intent.putExtra("role", role);
            intent.putExtra("companyId", companyId);
            startActivity(intent);
        } else {
            Intent intent = new Intent(this, DashboardActivity.class);
            intent.putExtra("role", role);
            intent.putExtra("companyId", companyId);
            startActivity(intent);
        }
        finish();
    }
}
