package com.posbillingapp.ui;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.posbillingapp.adapter.MenuAssignmentAdapter;
import com.posbillingapp.databinding.ActivityAssignMenuBinding;
import com.posbillingapp.models.MenuItemModel;
import com.posbillingapp.network.RetrofitClient;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AssignMenuActivity extends AppCompatActivity {

    private ActivityAssignMenuBinding binding;
    private MenuAssignmentAdapter adapter;
    private long userId;
    private long companyId;
    private String employeeName;
    private String employeeRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAssignMenuBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Get employee data from intent
        userId = getIntent().getLongExtra("userId", -1);
        companyId = getIntent().getLongExtra("companyId", -1);
        employeeName = getIntent().getStringExtra("employeeName");
        employeeRole = getIntent().getStringExtra("employeeRole");

        setupUI();
        loadMenuItems();
    }

    private void setupUI() {
        binding.toolbar.setNavigationOnClickListener(v -> finish());
        
        binding.tvEmployeeName.setText(employeeName);
        binding.tvEmployeeRole.setText(employeeRole);

        binding.btnSave.setOnClickListener(v -> saveAssignments());
    }

    private void loadMenuItems() {
        // Load all menu items
        RetrofitClient.getApiService().getMenu(companyId)
            .enqueue(new Callback<List<MenuItemModel>>() {
                @Override
                public void onResponse(Call<List<MenuItemModel>> call, Response<List<MenuItemModel>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        List<MenuItemModel> allItems = response.body();
                        loadAssignedItems(allItems);
                    } else {
                        Toast.makeText(AssignMenuActivity.this, "Failed to load menu items", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<List<MenuItemModel>> call, Throwable t) {
                    Toast.makeText(AssignMenuActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void loadAssignedItems(List<MenuItemModel> allItems) {
        // Load already assigned items
        RetrofitClient.getApiService().getAssignedMenuItems(userId)
            .enqueue(new Callback<List<Long>>() {
                @Override
                public void onResponse(Call<List<Long>> call, Response<List<Long>> response) {
                    Set<Long> assignedIds = new HashSet<>();
                    if (response.isSuccessful() && response.body() != null) {
                        assignedIds.addAll(response.body());
                    }
                    
                    // Setup RecyclerView with all items and pre-selected assignments
                    boolean isSingleSelection = "Limited Access".equalsIgnoreCase(employeeRole);
                    adapter = new MenuAssignmentAdapter(allItems, assignedIds, isSingleSelection);
                    binding.rvMenuItems.setLayoutManager(new LinearLayoutManager(AssignMenuActivity.this));
                    binding.rvMenuItems.setAdapter(adapter);
                }

                @Override
                public void onFailure(Call<List<Long>> call, Throwable t) {
                    // If error, just show all items with no pre-selections
                    boolean isSingleSelection = "Limited Access".equalsIgnoreCase(employeeRole);
                    adapter = new MenuAssignmentAdapter(allItems, new HashSet<>(), isSingleSelection);
                    binding.rvMenuItems.setLayoutManager(new LinearLayoutManager(AssignMenuActivity.this));
                    binding.rvMenuItems.setAdapter(adapter);
                }
            });
    }

    private void saveAssignments() {
        if (adapter == null) return;

        Set<Long> selectedItems = adapter.getSelectedMenuItems();
        List<Long> menuItemIds = new ArrayList<>(selectedItems);

        // Create request body
        MenuAssignmentRequest request = new MenuAssignmentRequest(userId, menuItemIds);

        RetrofitClient.getApiService().updateMenuAssignments(request)
            .enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(AssignMenuActivity.this, "Assignments saved successfully!", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(AssignMenuActivity.this, "Failed to save assignments", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    Toast.makeText(AssignMenuActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
    }

    // Request model
    public static class MenuAssignmentRequest {
        @com.google.gson.annotations.SerializedName("userId")
        private long userId;
        @com.google.gson.annotations.SerializedName("menuItemIds")
        private List<Long> menuItemIds;

        public MenuAssignmentRequest(long userId, List<Long> menuItemIds) {
            this.userId = userId;
            this.menuItemIds = menuItemIds;
        }

        public long getUserId() {
            return userId;
        }

        public List<Long> getMenuItemIds() {
            return menuItemIds;
        }
    }
}
