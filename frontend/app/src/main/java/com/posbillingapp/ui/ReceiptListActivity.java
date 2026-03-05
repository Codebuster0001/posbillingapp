package com.posbillingapp.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.posbillingapp.adapter.ReceiptListAdapter;
import com.posbillingapp.databinding.ActivityReceiptListBinding;
import com.posbillingapp.models.BillingModels.OrderHistoryResponse;
import com.posbillingapp.network.RetrofitClient;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ReceiptListActivity extends AppCompatActivity {

    private ActivityReceiptListBinding binding;
    private ReceiptListAdapter adapter;
    private long companyId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityReceiptListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        companyId = getIntent().getLongExtra("companyId", -1);

        setupToolbar();
        setupRecyclerView();
        
        if (companyId != -1) {
            fetchReceipts();
        } else {
            Toast.makeText(this, "Company ID missing", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            binding.toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        }
    }

    private void setupRecyclerView() {
        adapter = new ReceiptListAdapter(new ArrayList<>());
        binding.rvReceipts.setLayoutManager(new LinearLayoutManager(this));
        binding.rvReceipts.setAdapter(adapter);
    }

    private void fetchReceipts() {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.rvReceipts.setVisibility(View.GONE);
        binding.tvEmptyState.setVisibility(View.GONE);

        RetrofitClient.getApiService().getOrderHistory(companyId).enqueue(new Callback<List<OrderHistoryResponse>>() {
            @Override
            public void onResponse(Call<List<OrderHistoryResponse>> call, Response<List<OrderHistoryResponse>> response) {
                binding.progressBar.setVisibility(View.GONE);
                
                if (response.isSuccessful() && response.body() != null) {
                    List<OrderHistoryResponse> receipts = response.body();
                    if (receipts.isEmpty()) {
                        binding.tvEmptyState.setVisibility(View.VISIBLE);
                    } else {
                        binding.rvReceipts.setVisibility(View.VISIBLE);
                        adapter.updateList(receipts);
                    }
                } else {
                    Toast.makeText(ReceiptListActivity.this, "Failed to load receipts", Toast.LENGTH_SHORT).show();
                    binding.tvEmptyState.setVisibility(View.VISIBLE);
                    binding.tvEmptyState.setText("Error loading receipts");
                }
            }

            @Override
            public void onFailure(Call<List<OrderHistoryResponse>> call, Throwable t) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(ReceiptListActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                binding.tvEmptyState.setVisibility(View.VISIBLE);
                binding.tvEmptyState.setText("Network error");
            }
        });
    }
}
