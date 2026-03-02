package com.posbillingapp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import com.google.android.material.navigation.NavigationView;
import com.posbillingapp.R;
import com.posbillingapp.databinding.ActivityCalculatorBillingBinding;
import com.posbillingapp.models.BillingModels;
import com.posbillingapp.models.MenuItemModel;
import com.posbillingapp.network.RetrofitClient;
import com.posbillingapp.utils.PrinterService;
import com.posbillingapp.utils.SessionManager;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CalculatorBillingActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private ActivityCalculatorBillingBinding binding;
    private SessionManager sessionManager;
    private List<MenuItemModel> assignedItems = new ArrayList<>();
    private StringBuilder expression = new StringBuilder();
    private double currentResult = 0.0;
    private long companyId;
    private long userId;
    private boolean isNewNumber = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCalculatorBillingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);
        companyId = sessionManager.getCompanyId();
        userId = sessionManager.getUserId();

        setupToolbarAndDrawer();
        setupCalculator();
        checkPrinterPermissions(); // New
        
        fetchAssignedItems();
    }

    private void setupToolbarAndDrawer() {
        setSupportActionBar(binding.toolbar);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, binding.drawerLayout, binding.toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        binding.drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        binding.navViewLimited.setNavigationItemSelectedListener(this);
    }

    private void setupCalculator() {
        // Numeric buttons
        View.OnClickListener numListener = v -> {
            Button b = (Button) v;
            if (isNewNumber) {
                expression.setLength(0);
                isNewNumber = false;
            }
            expression.append(b.getText().toString());
            updateDisplay();
        };

        binding.btn0.setOnClickListener(numListener);
        binding.btn1.setOnClickListener(numListener);
        binding.btn2.setOnClickListener(numListener);
        binding.btn3.setOnClickListener(numListener);
        binding.btn4.setOnClickListener(numListener);
        binding.btn5.setOnClickListener(numListener);
        binding.btn6.setOnClickListener(numListener);
        binding.btn7.setOnClickListener(numListener);
        binding.btn8.setOnClickListener(numListener);
        binding.btn9.setOnClickListener(numListener);

        // Dot button
        binding.btnDot.setOnClickListener(v -> {
            if (isNewNumber) {
                expression.setLength(0);
                expression.append("0.");
                isNewNumber = false;
            } else {
                String currentExpr = expression.toString();
                if (!currentExpr.contains(".")) {
                    if (currentExpr.isEmpty()) expression.append("0");
                    expression.append(".");
                }
            }
            updateDisplay();
        });

        // Delete button (Backspace)
        binding.btnDel.setOnClickListener(v -> {
            if (expression.length() > 0) {
                expression.deleteCharAt(expression.length() - 1);
                if (expression.length() == 0) {
                    isNewNumber = true;
                    currentResult = 0.0;
                }
                updateDisplay();
            }
        });

        // Clear button
        binding.btnC.setOnClickListener(v -> {
            expression.setLength(0);
            currentResult = 0.0;
            isNewNumber = true;
            binding.tvCalculation.setText("");
            binding.tvResult.setText("0");
        });

        // Submit button
        binding.btnSubmit.setOnClickListener(v -> {
            calculateResult();
            if (currentResult > 0) {
                binding.btnSubmit.setEnabled(false); // Disable to prevent double click
                submitAndPrint();
            } else {
                Toast.makeText(this, "Enter valid amount", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateDisplay() {
        binding.tvCalculation.setText(expression.toString());
        try {
            double res = evaluate(expression.toString());
            currentResult = res;
            if (res == (long) res) {
                binding.tvResult.setText(String.format("%d", (long) res));
            } else {
                binding.tvResult.setText(String.format("%.2f", res));
            }
        } catch (Exception e) {
            // Ignore errors
        }
    }

    private double evaluate(String str) {
        if (str == null || str.isEmpty() || str.equals(".")) return 0;
        try {
            return Double.parseDouble(str);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void calculateResult() {
        try {
            if (expression.length() > 0) {
                currentResult = Double.parseDouble(expression.toString());
            } else {
                currentResult = 0.0;
            }
        } catch (Exception e) {
            currentResult = 0.0;
        }
    }

    private void fetchAssignedItems() {
        RetrofitClient.getApiService().getAssignedMenuDetails(userId)
            .enqueue(new Callback<List<MenuItemModel>>() {
                @Override
                public void onResponse(Call<List<MenuItemModel>> call, Response<List<MenuItemModel>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        assignedItems = response.body();
                        if (!assignedItems.isEmpty()) {
                            binding.tvItemNameDisplay.setText("ITEM: " + assignedItems.get(0).getName().toUpperCase());
                        } else {
                            binding.tvItemNameDisplay.setText("ITEM: NO ACCESS GRANTED");
                        }
                    } else {
                        binding.tvItemNameDisplay.setText("ITEM: API ERROR");
                    }
                }

                @Override
                public void onFailure(Call<List<MenuItemModel>> call, Throwable t) {
                    binding.tvItemNameDisplay.setText("ITEM: OFFLINE");
                }
            });
    }

    private void submitAndPrint() {
        if (assignedItems.isEmpty()) {
            Toast.makeText(this, "Admin has not assigned any items to you", Toast.LENGTH_LONG).show();
            binding.btnSubmit.setEnabled(true);
            return;
        }

        // Just use a temporary number for printing
        String tempBillNumber = "#" + System.currentTimeMillis();
        
        // Prepare data for printing
        MenuItemModel autoItem = assignedItems.get(0);
        List<PrinterService.ReceiptItem> printItems = new ArrayList<>();
        printItems.add(new PrinterService.ReceiptItem(autoItem.getName(), 1, currentResult));

        // NEW FLOW: Print First
        Toast.makeText(this, "Checking Printer...", Toast.LENGTH_SHORT).show();
        
        PrinterService.printReceipt(
            this,
            sessionManager.getCompanyName(),
            sessionManager.getUserRole(),
            tempBillNumber, 
            currentResult,
            printItems,
            new PrinterService.PrintCallback() {
                @Override
                public void onSuccess() {
                    runOnUiThread(() -> {
                        Toast.makeText(CalculatorBillingActivity.this, "Print Success! Creating Order...", Toast.LENGTH_SHORT).show();
                        // Proceed to create order
                        checkBillNumberAndOrder(autoItem);
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        Toast.makeText(CalculatorBillingActivity.this, "Print Failed: " + error, Toast.LENGTH_LONG).show();
                        logPrinterError(error);
                        binding.btnSubmit.setEnabled(true); // Re-enable button
                    });
                }
            }
        );
    }

    private void checkBillNumberAndOrder(MenuItemModel autoItem) {
        // Fetch real bill number then create order
         RetrofitClient.getApiService().getNextBillNumber(companyId).enqueue(new Callback<com.posbillingapp.models.AuthModels.AuthResponse>() {
            @Override
            public void onResponse(Call<com.posbillingapp.models.AuthModels.AuthResponse> call, Response<com.posbillingapp.models.AuthModels.AuthResponse> response) {
                String finalBillNumber = "#" + System.currentTimeMillis();
                if (response.isSuccessful() && response.body() != null && response.body().success) {
                    finalBillNumber = "#" + response.body().message;
                }
                createOrderInBackend(finalBillNumber, autoItem);
            }

            @Override
            public void onFailure(Call<com.posbillingapp.models.AuthModels.AuthResponse> call, Throwable t) {
                createOrderInBackend("#" + System.currentTimeMillis(), autoItem);
            }
        });
    }

    private void createOrderInBackend(String billNumber, MenuItemModel autoItem) {
        List<BillingModels.OrderItemRequest> apiItems = new ArrayList<>();
        apiItems.add(new BillingModels.OrderItemRequest(
                autoItem.getId(),
                autoItem.getName(),
                currentResult,
                1
        ));

        BillingModels.OrderRequest request = new BillingModels.OrderRequest(
                companyId, userId, billNumber, currentResult, apiItems
        );

        RetrofitClient.getApiService().createOrder(request).enqueue(new Callback<BillingModels.OrderResponse>() {
            @Override
            public void onResponse(Call<BillingModels.OrderResponse> call, Response<BillingModels.OrderResponse> response) {
                if (response.isSuccessful()) {
                    // Navigate to Receipt (Done screen)
                    List<PrinterService.ReceiptItem> receiptItems = new ArrayList<>();
                    receiptItems.add(new PrinterService.ReceiptItem(autoItem.getName(), 1, currentResult));
                    
                    Intent intent = new Intent(CalculatorBillingActivity.this, ReceiptActivity.class);
                    intent.putExtra("billNumber", billNumber);
                    intent.putExtra("total", currentResult);
                    intent.putExtra("items", (java.io.Serializable) receiptItems);
                    intent.putExtra("alreadyPrinted", true); // Important!
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(CalculatorBillingActivity.this, "Failed to save bill", Toast.LENGTH_SHORT).show();
                    binding.btnSubmit.setEnabled(true);
                }
            }

            @Override
            public void onFailure(Call<BillingModels.OrderResponse> call, Throwable t) {
                Toast.makeText(CalculatorBillingActivity.this, "Network error", Toast.LENGTH_SHORT).show();
                binding.btnSubmit.setEnabled(true);
            }
        });
    }

    private void logPrinterError(String errorMsg) {
        String deviceInfo = android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL + " (Android " + android.os.Build.VERSION.RELEASE + ")";
        com.posbillingapp.models.ErrorLogRequest request = new com.posbillingapp.models.ErrorLogRequest(
            "PrinterError",
            "CalculatorBillingActivity",
            errorMsg,
            "",
            deviceInfo,
            String.valueOf(sessionManager.getUserId())
        );

        RetrofitClient.getApiService().logError(request).enqueue(new Callback<com.posbillingapp.models.AuthModels.AuthResponse>() {
            @Override
            public void onResponse(Call<com.posbillingapp.models.AuthModels.AuthResponse> call, Response<com.posbillingapp.models.AuthModels.AuthResponse> response) {}
            @Override
            public void onFailure(Call<com.posbillingapp.models.AuthModels.AuthResponse> call, Throwable t) {}
        });
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_logout) {
            sessionManager.logoutUser();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }
        binding.drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    private void checkPrinterPermissions() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != android.content.pm.PackageManager.PERMISSION_GRANTED ||
                androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_SCAN) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                
                androidx.core.app.ActivityCompat.requestPermissions(this, 
                    new String[]{android.Manifest.permission.BLUETOOTH_CONNECT, android.Manifest.permission.BLUETOOTH_SCAN}, 101);
            }
        }
    }
}
