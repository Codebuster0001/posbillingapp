package com.posbillingapp.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.posbillingapp.adapter.PermissionManagementAdapter;
import com.posbillingapp.databinding.ActivityAccessControlBinding;
import com.posbillingapp.models.PermissionModels;
import com.posbillingapp.network.RetrofitClient;
import com.posbillingapp.utils.SessionManager;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AccessControlActivity extends AppCompatActivity {

    private ActivityAccessControlBinding binding;
    private PermissionManagementAdapter adapter;
    private SessionManager session;
    private List<PermissionModels.Role> rolesList = new ArrayList<>();
    private List<PermissionModels.Permission> allPermissionsList = new ArrayList<>();
    private int selectedRoleId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAccessControlBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        session = new SessionManager(this);

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Access Control");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        setupUI();
        loadInitialData();
    }

    private void setupUI() {
        binding.rvPermissions.setLayoutManager(new LinearLayoutManager(this));
        
        binding.spinnerRoles.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                PermissionModels.Role selectedRole = rolesList.get(position);
                selectedRoleId = selectedRole.id;
                loadRolePermissions(selectedRoleId);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        binding.btnSave.setOnClickListener(v -> savePermissions());
    }

    private void loadInitialData() {
        // 1. Fetch all available permissions
        RetrofitClient.getApiService().getAllPermissions().enqueue(new Callback<List<PermissionModels.Permission>>() {
            @Override
            public void onResponse(Call<List<PermissionModels.Permission>> call, Response<List<PermissionModels.Permission>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    allPermissionsList = response.body();
                    // 2. Fetch roles
                    fetchRoles();
                } else {
                    Toast.makeText(AccessControlActivity.this, "Failed to load permissions", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<PermissionModels.Permission>> call, Throwable t) {
                Toast.makeText(AccessControlActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchRoles() {
        RetrofitClient.getApiService().getRoles().enqueue(new Callback<List<PermissionModels.Role>>() {
            @Override
            public void onResponse(Call<List<PermissionModels.Role>> call, Response<List<PermissionModels.Role>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // Filter out Admin (ID 0) as it's not manageable
                    rolesList.clear();
                    for (PermissionModels.Role r : response.body()) {
                        if (!"Admin".equalsIgnoreCase(r.roleName)) {
                            rolesList.add(r);
                        }
                    }

                    ArrayAdapter<PermissionModels.Role> roleAdapter = new ArrayAdapter<>(
                            AccessControlActivity.this, android.R.layout.simple_spinner_item, rolesList);
                    roleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    binding.spinnerRoles.setAdapter(roleAdapter);
                }
            }

            @Override
            public void onFailure(Call<List<PermissionModels.Role>> call, Throwable t) {
                Toast.makeText(AccessControlActivity.this, "Failed to load roles", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadRolePermissions(int roleId) {
        RetrofitClient.getApiService().getRolePermissions(roleId, session.getCompanyId())
            .enqueue(new Callback<List<String>>() {
                @Override
                public void onResponse(Call<List<String>> call, Response<List<String>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        if (adapter == null) {
                            adapter = new PermissionManagementAdapter(allPermissionsList, new HashSet<>(response.body()));
                            binding.rvPermissions.setAdapter(adapter);
                        } else {
                            adapter.updateActivePermissions(new HashSet<>(response.body()));
                        }
                    }
                }

                @Override
                public void onFailure(Call<List<String>> call, Throwable t) {
                    Toast.makeText(AccessControlActivity.this, "Failed to load role settings", Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void savePermissions() {
        if (adapter == null || selectedRoleId == -1) return;

        PermissionModels.UpdateRolePermissionsRequest request = new PermissionModels.UpdateRolePermissionsRequest(
                selectedRoleId, session.getCompanyId(), adapter.getSelectedKeys()
        );

        binding.btnSave.setEnabled(false);
        binding.btnSave.setText("Saving...");

        RetrofitClient.getApiService().updateRolePermissions(request).enqueue(new Callback<com.posbillingapp.models.AuthModels.AuthResponse>() {
            @Override
            public void onResponse(Call<com.posbillingapp.models.AuthModels.AuthResponse> call, Response<com.posbillingapp.models.AuthModels.AuthResponse> response) {
                binding.btnSave.setEnabled(true);
                binding.btnSave.setText("Save Changes");
                
                if (response.isSuccessful() && response.body() != null && response.body().success) {
                    Toast.makeText(AccessControlActivity.this, "Permissions updated successfully!", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(AccessControlActivity.this, "Update failed", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<com.posbillingapp.models.AuthModels.AuthResponse> call, Throwable t) {
                binding.btnSave.setEnabled(true);
                binding.btnSave.setText("Save Changes");
                Toast.makeText(AccessControlActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
