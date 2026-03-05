package com.posbillingapp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import com.google.android.material.navigation.NavigationView;
import com.posbillingapp.R;
import com.posbillingapp.databinding.ActivityDashboardBinding;

public class DashboardActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private ActivityDashboardBinding binding;
    private String userRole;
    private long companyId;
    private boolean isAdmin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        userRole = getIntent().getStringExtra("role");
        companyId = getIntent().getLongExtra("companyId", -1);

        setSupportActionBar(binding.toolbar);
        setupDrawer();
        setupUI();

        binding.btnOpenBilling.setOnClickListener(v -> openBilling());
        binding.btnManageEmployees.setOnClickListener(v -> openEmployees());
        binding.btnManageMenu.setOnClickListener(v -> openMenu());

        // Initialize Currency Symbol from Session
        com.posbillingapp.utils.SessionManager session = new com.posbillingapp.utils.SessionManager(this);
        com.posbillingapp.utils.CurrencyUtils.setCurrency(session.getCurrencySymbol(), session.getCurrencyCode());
        
        syncPermissions();

        fetchDashboardStats();
    }

    private void syncPermissions() {
        com.posbillingapp.utils.SessionManager session = new com.posbillingapp.utils.SessionManager(this);
        String refreshToken = session.getRefreshToken();
        if (refreshToken == null || refreshToken.isEmpty()) return;

        com.posbillingapp.network.RetrofitClient.getApiService()
            .refresh(new com.posbillingapp.models.AuthModels.RefreshRequest(refreshToken))
            .enqueue(new retrofit2.Callback<com.posbillingapp.models.AuthModels.AuthResponse>() {
                @Override
                public void onResponse(retrofit2.Call<com.posbillingapp.models.AuthModels.AuthResponse> call, 
                                      retrofit2.Response<com.posbillingapp.models.AuthModels.AuthResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        com.posbillingapp.models.AuthModels.AuthResponse auth = response.body();
                        if (auth.success) {
                            session.updatePermissions(auth.token, auth.permissions);
                            // Refresh UI after syncing
                            setupDrawer();
                            setupUI();
                        }
                    }
                }

                @Override
                public void onFailure(retrofit2.Call<com.posbillingapp.models.AuthModels.AuthResponse> call, Throwable t) {
                    // Fail silently
                }
            });
    }

    private void fetchDashboardStats() {
        com.posbillingapp.network.RetrofitClient.getApiService().getDashboardStats(companyId)
            .enqueue(new retrofit2.Callback<com.posbillingapp.models.DashboardStats>() {
                @Override
                public void onResponse(retrofit2.Call<com.posbillingapp.models.DashboardStats> call, retrofit2.Response<com.posbillingapp.models.DashboardStats> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        com.posbillingapp.models.DashboardStats stats = response.body();
                        binding.tvStatEmployees.setText(String.valueOf(stats.getEmployeeCount()));
                        binding.tvStatReceipts.setText(String.valueOf(stats.getReceiptCount()));
                        binding.tvStatCollection.setText(com.posbillingapp.utils.CurrencyUtils.format(stats.getTotalCollection()));
                    }
                }

                @Override
                public void onFailure(retrofit2.Call<com.posbillingapp.models.DashboardStats> call, Throwable t) {
                    // Silent fail or log
                }
            });
    }

    private void setupDrawer() {
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, binding.drawerLayout, binding.toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        binding.drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        binding.navView.setNavigationItemSelectedListener(this);
        
        // Granular Permission-based visibility
        com.posbillingapp.utils.SessionManager session = new com.posbillingapp.utils.SessionManager(this);
        android.view.Menu menu = binding.navView.getMenu();
        isAdmin = "Admin".equalsIgnoreCase(userRole);
        boolean hasEmployees = isAdmin || session.hasPermission("VIEW_EMPLOYEES");
        boolean hasProducts = isAdmin || session.hasPermission("VIEW_PRODUCTS");
        boolean hasSettings = isAdmin; // Removed VIEW_SETTINGS check
        boolean hasCalculator = isAdmin || session.hasPermission("VIEW_CALCULATOR");
        boolean hasPermissions = isAdmin; // Removed MANAGE_PERMISSIONS check

        setMenuVisibility(menu, R.id.nav_dashboard, true); // Everyone can see dashboard
        setMenuVisibility(menu, R.id.nav_billing, isAdmin || session.hasPermission("VIEW_BILLING"));
        setMenuVisibility(menu, R.id.nav_manage_employees, hasEmployees);
        setMenuVisibility(menu, R.id.nav_manage_menu, hasProducts);
        setMenuVisibility(menu, R.id.nav_access_control, hasPermissions);
        setMenuVisibility(menu, R.id.nav_settings, hasSettings);

        // Hide the parent Admin section if NO children are visible
        setMenuVisibility(menu, R.id.nav_admin_section, (hasEmployees || hasProducts || hasSettings || hasPermissions));

        // Redirection logic simplified
        if ("Limited Access".equalsIgnoreCase(userRole) && !session.hasPermission("VIEW_BILLING")) {
             // Fallback for limited users with no billing access? 
             // Keeping original logic but removing the Dashboard permission check
        }
    }

    private void setMenuVisibility(android.view.Menu menu, int id, boolean visible) {
        android.view.MenuItem item = menu.findItem(id);
        if (item != null) item.setVisible(visible);
    }

    private void setupUI() {
        // Initialize SessionManager for UI logic
        com.posbillingapp.utils.SessionManager session = new com.posbillingapp.utils.SessionManager(this);

        // Hide role info as requested, and let tvWelcome default to "Welcome Back"
        binding.tvRoleInfo.setVisibility(View.GONE);
        
        // Populate Navigation Header
        View headerView = binding.navView.getHeaderView(0);
        if (headerView != null) {
            android.widget.TextView tvNavCompany = headerView.findViewById(R.id.tvNavCompanyName);
            android.widget.TextView tvNavRole = headerView.findViewById(R.id.tvNavRole);
            
            tvNavCompany.setText(session.getCompanyName());
            tvNavRole.setText("Role :- " + userRole);
        }

        if ("Admin".equalsIgnoreCase(userRole)) {
            binding.layoutAdmin.setVisibility(View.VISIBLE);
        } else {
            // Employee Dashboard (EmployeeFull or EmployeeLimited)
            binding.layoutAdmin.setVisibility(View.GONE);
            binding.tvWelcome.setText("Employee Dashboard");
            
            // Only inflate if not already there or we need fresh start
            if (binding.containerDashboard.getChildCount() == 0 || !(binding.containerDashboard.getChildAt(0).getTag() instanceof String && "employee".equals(binding.containerDashboard.getChildAt(0).getTag()))) {
                View employeeView = getLayoutInflater().inflate(R.layout.layout_employee_dashboard, 
                    binding.containerDashboard, false);
                employeeView.setTag("employee");
                binding.containerDashboard.removeAllViews();
                binding.containerDashboard.addView(employeeView);
                setupEmployeeDashboard(employeeView);
            } else {
                setupEmployeeDashboard(binding.containerDashboard.getChildAt(0));
            }
        }

        // Quick Action Button visibility based on roles/simplified permissions
        binding.btnOpenBilling.setVisibility((isAdmin || session.hasPermission("VIEW_BILLING")) ? View.VISIBLE : View.GONE);
        binding.btnManageEmployees.setVisibility((isAdmin || session.hasPermission("VIEW_EMPLOYEES")) ? View.VISIBLE : View.GONE);
        binding.btnManageMenu.setVisibility((isAdmin || session.hasPermission("VIEW_PRODUCTS")) ? View.VISIBLE : View.GONE);
        binding.btnCalculator.setVisibility(View.GONE);
        binding.btnAccessControl.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
        binding.btnAccessControl.setOnClickListener(v -> openAccessControl());
        
        // Add click listeners to Dashboard Cards
        if (binding.cardEmployees != null) {
            binding.cardEmployees.setOnClickListener(v -> openEmployees());
        }
        if (binding.cardReceipts != null) {
            binding.cardReceipts.setOnClickListener(v -> openMyOrders());
        }
    }

    private void setupEmployeeDashboard(View view) {
        com.posbillingapp.utils.SessionManager session = new com.posbillingapp.utils.SessionManager(this);
        
        // Setup billing card visibility based on permissions
        boolean canBill = isAdmin || session.hasPermission("VIEW_BILLING");
        view.findViewById(R.id.cardBilling).setVisibility(canBill ? View.VISIBLE : View.GONE);
        view.findViewById(R.id.cardBilling).setOnClickListener(v -> openBilling());
        
        // Setup profile card click  
        view.findViewById(R.id.cardProfile).setOnClickListener(v -> {
            // TODO: Open Profile Edit Activity
            android.widget.Toast.makeText(this, "Profile editing coming soon!", android.widget.Toast.LENGTH_SHORT).show();
        });
        
        // Setup Receipts card click
        View cardReceipts = view.findViewById(R.id.cardReceipts);
        if (cardReceipts != null) {
            cardReceipts.setOnClickListener(v -> openMyOrders());
        }
        
        // Load employee stats and receipts
        fetchEmployeeStats(view);
    }

    private void fetchEmployeeStats(View view) {
        long userId = new com.posbillingapp.utils.SessionManager(this).getUserId();
        
        com.posbillingapp.network.RetrofitClient.getApiService()
            .getEmployeeStats(userId)
            .enqueue(new retrofit2.Callback<com.posbillingapp.models.EmployeeStats>() {
                @Override
                public void onResponse(retrofit2.Call<com.posbillingapp.models.EmployeeStats> call, 
                                      retrofit2.Response<com.posbillingapp.models.EmployeeStats> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        com.posbillingapp.models.EmployeeStats stats = response.body();
                        android.widget.TextView tvTotalReceipts = view.findViewById(R.id.tvTotalReceipts);
                        android.widget.TextView tvTotalAmount = view.findViewById(R.id.tvTotalAmount);
                        
                        tvTotalReceipts.setText(String.valueOf(stats.getTotalReceipts()));
                        tvTotalAmount.setText(com.posbillingapp.utils.CurrencyUtils.format(stats.getTotalAmount()));
                        
                        // Load recent receipts
                        // TODO: Setup RecyclerView with receipts
                    }
                }

                @Override
                public void onFailure(retrofit2.Call<com.posbillingapp.models.EmployeeStats> call, Throwable t) {
                    // Silent fail or show default values
                }
            });
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_dashboard) {
            // Already here
        } else if (id == R.id.nav_billing) {
            openBilling();
        } else if (id == R.id.nav_my_profile) {
            openMyProfile();
        } else if (id == R.id.nav_my_orders) {
            openMyOrders();
        } else if (id == R.id.nav_manage_employees) {
            openEmployees();
        } else if (id == R.id.nav_manage_menu) {
            openMenu();
        } else if (id == R.id.nav_access_control) {
            openAccessControl();
        } else if (id == R.id.nav_settings) {
            openSettings();
        } else if (id == R.id.nav_logout) {
            logout();
        }

        binding.drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void openBilling() {
        Intent intent = new Intent(this, BillingActivity.class);
        intent.putExtra("role", userRole);
        intent.putExtra("companyId", companyId);
        startActivity(intent);
    }

    private void openEmployees() {
        Intent intent = new Intent(this, EmployeeActivity.class);
        intent.putExtra("companyId", companyId);
        startActivity(intent);
    }

    private void openMenu() {
        Intent intent = new Intent(this, MenuActivity.class);
        intent.putExtra("companyId", companyId);
        startActivity(intent);
    }

    private void openMyProfile() {
        // TODO: Create Profile Activity
        Toast.makeText(this, "Profile editing coming soon!", Toast.LENGTH_SHORT).show();
    }

    private void openMyOrders() {
        Intent intent = new Intent(this, ReceiptListActivity.class);
        intent.putExtra("companyId", companyId);
        startActivity(intent);
    }

    private void openSettings() {
        Intent intent = new Intent(this, SettlementActivity.class);
        intent.putExtra("companyId", companyId);
        startActivity(intent);
    }

    private void openAccessControl() {
        startActivity(new Intent(this, AccessControlActivity.class));
    }

    private void logout() {
        new com.posbillingapp.utils.SessionManager(this).logoutUser();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    @Override
    public void onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            getOnBackPressedDispatcher().onBackPressed();
        }
    }
}
