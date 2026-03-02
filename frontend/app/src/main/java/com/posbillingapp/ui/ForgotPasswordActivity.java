package com.posbillingapp.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.posbillingapp.databinding.ActivityForgotPasswordBinding;
import com.posbillingapp.models.AuthModels.*;
import com.posbillingapp.network.RetrofitClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ForgotPasswordActivity extends AppCompatActivity {

    private ActivityForgotPasswordBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityForgotPasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnReset.setOnClickListener(v -> {
            String email = binding.etEmail.getText().toString();
            sendResetLink(email);
        });

        binding.btnSubmitNewPassword.setOnClickListener(v -> {
            String email = binding.etEmail.getText().toString();
            String otp = binding.etOtp.getText().toString();
            String pass = binding.etNewPassword.getText().toString();
            resetPassword(email, otp, pass);
        });
    }

    private void sendResetLink(String email) {
        RetrofitClient.getApiService().forgotPassword(new ForgotPasswordRequest(email)).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                if (response.isSuccessful() && response.body().success) {
                    Toast.makeText(ForgotPasswordActivity.this, "OTP sent!", Toast.LENGTH_SHORT).show();
                    binding.layoutResetProcess.setVisibility(View.VISIBLE);
                } else {
                    Toast.makeText(ForgotPasswordActivity.this, "Error sending link", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                Toast.makeText(ForgotPasswordActivity.this, t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void resetPassword(String email, String otp, String newPassword) {
        RetrofitClient.getApiService().resetPassword(new ResetPasswordRequest(email, otp, newPassword)).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                if (response.isSuccessful() && response.body().success) {
                    Toast.makeText(ForgotPasswordActivity.this, "Password updated!", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(ForgotPasswordActivity.this, "Reset failed", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                Toast.makeText(ForgotPasswordActivity.this, t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
