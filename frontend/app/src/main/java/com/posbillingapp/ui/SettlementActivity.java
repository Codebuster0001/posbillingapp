package com.posbillingapp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.posbillingapp.R;
import com.posbillingapp.databinding.ActivitySettlementBinding;
import com.posbillingapp.databinding.ItemBankAccountBinding;
import com.posbillingapp.databinding.ItemCardBinding;
import com.posbillingapp.models.SettlementModels.*;
import com.posbillingapp.network.RetrofitClient;
import com.posbillingapp.utils.SessionManager;
import com.posbillingapp.utils.ValidationUtils;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SettlementActivity extends AppCompatActivity {

    private ActivitySettlementBinding binding;
    private long companyId;
    private BankAdapter bankAdapter;
    private CardAdapter cardAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettlementBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        SessionManager session = new SessionManager(this);
        companyId = session.getCompanyId();

        setupToolbar();
        setupRecyclerViews();
        setupListeners();
        fetchData();
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerViews() {
        bankAdapter = new BankAdapter(new ArrayList<>());
        binding.rvBankAccounts.setLayoutManager(new LinearLayoutManager(this));
        binding.rvBankAccounts.setAdapter(bankAdapter);

        cardAdapter = new CardAdapter(new ArrayList<>());
        binding.rvCards.setLayoutManager(new LinearLayoutManager(this));
        binding.rvCards.setAdapter(cardAdapter);
    }

    private void setupListeners() {
        binding.btnAddBank.setOnClickListener(v -> showAddBankDialog());
        binding.btnAddCard.setOnClickListener(v -> showAddCardDialog());
    }

    private void fetchData() {
        RetrofitClient.getApiService().getBankDetails(companyId).enqueue(new Callback<List<BankDetails>>() {
            @Override
            public void onResponse(Call<List<BankDetails>> call, Response<List<BankDetails>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    bankAdapter.updateData(response.body());
                }
            }

            @Override
            public void onFailure(Call<List<BankDetails>> call, Throwable t) {
                Toast.makeText(SettlementActivity.this, "Failed to load bank accounts", Toast.LENGTH_SHORT).show();
            }
        });

        RetrofitClient.getApiService().getCardDetails(companyId).enqueue(new Callback<List<CardDetails>>() {
            @Override
            public void onResponse(Call<List<CardDetails>> call, Response<List<CardDetails>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    cardAdapter.updateData(response.body());
                }
            }

            @Override
            public void onFailure(Call<List<CardDetails>> call, Throwable t) {
                Toast.makeText(SettlementActivity.this, "Failed to load cards", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAddBankDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_add_bank, null);
        EditText etName = view.findViewById(R.id.etHolderName);
        EditText etAcc = view.findViewById(R.id.etAccountNumber);
        EditText etIfsc = view.findViewById(R.id.etIfscCode);

        new AlertDialog.Builder(this)
                .setTitle("Link Bank Account")
                .setView(view)
                .setPositiveButton("Verify & Link", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    String acc = etAcc.getText().toString().trim();
                    String ifsc = etIfsc.getText().toString().trim();

                    if (name.isEmpty() || acc.isEmpty() || ifsc.isEmpty()) {
                        Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (!ValidationUtils.isValidIFSC(ifsc)) {
                        Toast.makeText(this, "Invalid IFSC Code", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (!ValidationUtils.isValidAccountNumber(acc)) {
                        Toast.makeText(this, "Invalid Account Number (8-18 digits)", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    performBankVerification(new BankDetails(name, acc, ifsc, "Searching..."));
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void performBankVerification(BankDetails bank) {
        binding.verificationOverlay.setVisibility(View.VISIBLE);
        binding.tvVerificationStatus.setText("Discovering Bank Details...");

        RetrofitClient.getApiService().addBankDetails(companyId, bank).enqueue(new Callback<ValidationResult>() {
            @Override
            public void onResponse(Call<ValidationResult> call, Response<ValidationResult> response) {
                binding.verificationOverlay.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    ValidationResult result = response.body();
                    if (result.verified) {
                        Toast.makeText(SettlementActivity.this, "Verified as " + result.registeredName, Toast.LENGTH_LONG).show();
                        fetchData();
                    } else {
                        Toast.makeText(SettlementActivity.this, result.verificationMessage, Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(SettlementActivity.this, "Verification failed", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ValidationResult> call, Throwable t) {
                binding.verificationOverlay.setVisibility(View.GONE);
                Toast.makeText(SettlementActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAddCardDialog() {
        // Simple card dialog implementation
        Toast.makeText(this, "Card tokenization initialized...", Toast.LENGTH_SHORT).show();
        // In a real app, this would be a specialized masked input
    }

    // --- Adapters ---

    private class BankAdapter extends RecyclerView.Adapter<BankAdapter.ViewHolder> {
        private List<BankDetails> list;

        public BankAdapter(List<BankDetails> list) { this.list = list; }
        public void updateData(List<BankDetails> newList) { this.list = newList; notifyDataSetChanged(); }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(ItemBankAccountBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            BankDetails item = list.get(position);
            holder.binding.tvBankName.setText(item.bankName);
            holder.binding.tvAccountInfo.setText("XXXX " + (item.accountNumber != null && item.accountNumber.length() > 4 ? item.accountNumber.substring(item.accountNumber.length()-4) : "****") + " • " + item.accountHolderName);
            holder.binding.tvIfsc.setText("IFSC: " + item.ifscCode);
            holder.binding.tvPrimaryBadge.setVisibility(item.isPrimary ? View.VISIBLE : View.GONE);
            holder.binding.tvBankLogo.setText(item.bankName != null && !item.bankName.isEmpty() ? item.bankName.substring(0, 1) : "B");
            
            holder.binding.getRoot().setOnClickListener(v -> {
                if (!item.isPrimary) {
                    RetrofitClient.getApiService().setPrimaryBank(item.id).enqueue(new Callback<Void>() {
                        @Override public void onResponse(Call<Void> call, Response<Void> response) { fetchData(); }
                        @Override public void onFailure(Call<Void> call, Throwable t) {}
                    });
                }
            });

            holder.binding.btnDelete.setOnClickListener(v -> {
                RetrofitClient.getApiService().deleteBank(item.id).enqueue(new Callback<Void>() {
                    @Override public void onResponse(Call<Void> call, Response<Void> response) { fetchData(); }
                    @Override public void onFailure(Call<Void> call, Throwable t) {}
                });
            });
        }

        @Override public int getItemCount() { return list.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            ItemBankAccountBinding binding;
            ViewHolder(ItemBankAccountBinding binding) { super(binding.getRoot()); this.binding = binding; }
        }
    }

    private class CardAdapter extends RecyclerView.Adapter<CardAdapter.ViewHolder> {
        private List<CardDetails> list;

        public CardAdapter(List<CardDetails> list) { this.list = list; }
        public void updateData(List<CardDetails> newList) { this.list = newList; notifyDataSetChanged(); }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(ItemCardBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            CardDetails item = list.get(position);
            holder.binding.tvCardNumber.setText(item.cardNumber);
            holder.binding.tvCardHolder.setText(item.cardHolderName);
            holder.binding.tvCardExpiry.setText(item.expiryMonth + "/" + item.expiryYear);
            holder.binding.tvCardType.setText(item.cardType);
            holder.binding.ivPrimaryCheck.setVisibility(item.isPrimary ? View.VISIBLE : View.GONE);

            holder.binding.getRoot().setOnClickListener(v -> {
                if (!item.isPrimary) {
                    RetrofitClient.getApiService().setPrimaryCard(item.id).enqueue(new Callback<Void>() {
                        @Override public void onResponse(Call<Void> call, Response<Void> response) { fetchData(); }
                        @Override public void onFailure(Call<Void> call, Throwable t) {}
                    });
                }
            });

            holder.binding.btnDeleteCard.setOnClickListener(v -> {
                RetrofitClient.getApiService().deleteCard(item.id).enqueue(new Callback<Void>() {
                    @Override public void onResponse(Call<Void> call, Response<Void> response) { fetchData(); }
                    @Override public void onFailure(Call<Void> call, Throwable t) {}
                });
            });
        }

        @Override public int getItemCount() { return list.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            ItemCardBinding binding;
            ViewHolder(ItemCardBinding binding) { super(binding.getRoot()); this.binding = binding; }
        }
    }
}
