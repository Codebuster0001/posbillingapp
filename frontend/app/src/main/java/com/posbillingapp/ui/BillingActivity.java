package com.posbillingapp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.posbillingapp.R;
import com.posbillingapp.adapter.BillingProductAdapter;
import com.posbillingapp.databinding.ActivityBillingBinding;
import com.posbillingapp.models.MenuItemModel;
import com.posbillingapp.network.RetrofitClient;
import com.posbillingapp.utils.SessionManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BillingActivity extends AppCompatActivity {

    private ActivityBillingBinding binding;
    private SessionManager sessionManager;
    private long companyId;
    private String userRole;
    private BillingProductAdapter productAdapter;
    private List<MenuItemModel> activeProducts = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBillingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);
        companyId = sessionManager.getCompanyId();
        userRole = getIntent().getStringExtra("role");

        loadNextBillNumber();
        setupBillingView();

        binding.toolbarBilling.setNavigationOnClickListener(v -> finish());
        
        setupSwipeButton();
        checkPrinterPermissions();
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @androidx.annotation.NonNull String[] permissions, @androidx.annotation.NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101) {
            if (grantResults.length > 0 && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Printer Permission Granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Printer Permission Denied. Printing may fail.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void setupSwipeButton() {
        binding.btnSwipeThumb.setOnTouchListener(new View.OnTouchListener() {
            float startX;
            float initialX;
            int maxSwipeDistance;
            boolean isDragging = false;

            @Override
            public boolean onTouch(View v, android.view.MotionEvent event) {
                if (!v.isEnabled()) return false;
                
                switch (event.getAction()) {
                    case android.view.MotionEvent.ACTION_DOWN:
                        startX = event.getRawX();
                        initialX = v.getX();
                        // Calculate max distance dynamically
                        maxSwipeDistance = binding.layoutSwipe.getWidth() - v.getWidth() - 8; 
                        isDragging = true;
                        return true;
                        // ... rest of logic
                    case android.view.MotionEvent.ACTION_MOVE:
                        if (!isDragging) return false;
                        float dX = event.getRawX() - startX;
                        float newX = initialX + dX;

                        if (newX < 4) newX = 4;
                        if (newX > maxSwipeDistance) newX = maxSwipeDistance;

                        v.setX(newX);
                        
                        float progress = (newX - 4) / (float)maxSwipeDistance;
                        binding.tvSwipeText.setAlpha(1.0f - progress);
                        return true;

                    case android.view.MotionEvent.ACTION_UP:
                        isDragging = false;
                        float finalX = v.getX();
                        if (finalX > maxSwipeDistance * 0.85) { 
                            v.setX(maxSwipeDistance);
                            binding.tvSwipeText.setAlpha(0f);
                            submitOrder();
                            v.setOnTouchListener(null); // Disable
                        } else {
                            v.animate().x(4).setDuration(200).start();
                            binding.tvSwipeText.animate().alpha(1.0f).setDuration(200).start();
                        }
                        return true;
                }
                return false;
            }
        });
    }

    private void enableSwipeButton(boolean enable) {
        binding.layoutSwipe.setEnabled(enable);
        binding.btnSwipeThumb.setEnabled(enable);
        binding.btnSwipeThumb.setAlpha(enable ? 1.0f : 0.5f);
        binding.tvSwipeText.setAlpha(enable ? 1.0f : 0.5f);
        if (!enable) {
             // Reset position if disabled
             binding.btnSwipeThumb.animate().x(4).setDuration(200).start();
             binding.tvSwipeText.animate().alpha(0.5f).setDuration(200).start();
        }
    }

    private void setupBillingView() {
        LayoutInflater inflater = getLayoutInflater();
        if ("Limited Access".equalsIgnoreCase(userRole)) {
            View limitedView = inflater.inflate(R.layout.layout_billing_limited, binding.containerBilling, false);
            binding.containerBilling.addView(limitedView);
            // Setup calculator logic here
        } else {
            // Full Access View
            View fullView = inflater.inflate(R.layout.layout_billing_full, binding.containerBilling, false);
            binding.containerBilling.addView(fullView);
            
            RecyclerView rvMenuItems = fullView.findViewById(R.id.rvMenuItems);
            setupProductList(rvMenuItems, fullView);
            fetchActiveProducts();
        }
        enableSwipeButton(false); // Initially disabled
    }

    private void setupProductList(RecyclerView rv, View fullView) {
        TextView tvTotal = fullView.findViewById(R.id.tvTotalAmount);
        if (tvTotal != null) {
            tvTotal.setText("Total: " + com.posbillingapp.utils.CurrencyUtils.format(0.0));
        }
        
        productAdapter = new BillingProductAdapter(new ArrayList<>(), (product, quantity) -> {
            updateCartTotal(fullView, activeProducts, productAdapter.getCartItems());
        });
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(productAdapter);
    }

    private void updateCartTotal(View view, List<MenuItemModel> products, java.util.Map<Long, Integer> cartItems) {
        double total = 0;
        int totalQty = 0;
        for (MenuItemModel item : products) {
            int qty = cartItems.getOrDefault(item.getId(), 0);
            total += (item.getPrice() * qty);
            totalQty += qty;
        }
        
        TextView tvTotal = view.findViewById(R.id.tvTotalAmount);
        if (tvTotal != null) {
            tvTotal.setText("Total: " + com.posbillingapp.utils.CurrencyUtils.format(total));
        }
        
        enableSwipeButton(totalQty > 0);
    }

    private void fetchActiveProducts() {
        RetrofitClient.getApiService().getMenu(companyId).enqueue(new Callback<List<MenuItemModel>>() {
            @Override
            public void onResponse(Call<List<MenuItemModel>> call, Response<List<MenuItemModel>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    activeProducts.clear();
                    // Filter only available items
                    for (MenuItemModel item : response.body()) {
                        if (item.isAvailable()) {
                            activeProducts.add(item);
                        }
                    }
                    if (productAdapter != null) {
                        productAdapter.updateList(activeProducts);
                    }
                }
            }

            @Override
            public void onFailure(Call<List<MenuItemModel>> call, Throwable t) {
                Toast.makeText(BillingActivity.this, "Failed to load products", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadNextBillNumber() {
        RetrofitClient.getApiService().getNextBillNumber(companyId).enqueue(new Callback<com.posbillingapp.models.AuthModels.AuthResponse>() {
            @Override
            public void onResponse(Call<com.posbillingapp.models.AuthModels.AuthResponse> call, Response<com.posbillingapp.models.AuthModels.AuthResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().success) {
                    String billNum = "#" + response.body().message;
                    binding.tvBillInfo.setText("Bill #: " + billNum);
                } else {
                    binding.tvBillInfo.setText("Bill #: #" + System.currentTimeMillis());
                }
            }

            @Override
            public void onFailure(Call<com.posbillingapp.models.AuthModels.AuthResponse> call, Throwable t) {
                binding.tvBillInfo.setText("Bill #: #" + System.currentTimeMillis());
            }
        });
    }

    private void submitOrder() {
        if (productAdapter == null) return;
        
        java.util.Map<Long, Integer> cart = productAdapter.getCartItems();
        if (cart.isEmpty()) {
            Toast.makeText(this, "Cart is empty", Toast.LENGTH_SHORT).show();
            resetSwipeButton();
            return;
        }

        List<com.posbillingapp.models.BillingModels.OrderItemRequest> orderItems = new ArrayList<>();
        double totalAmount = 0;
        java.util.List<com.posbillingapp.utils.PrinterService.ReceiptItem> printItems = new ArrayList<>();

        for (MenuItemModel product : activeProducts) {
            if (cart.containsKey(product.getId())) {
                int qty = cart.get(product.getId());
                orderItems.add(new com.posbillingapp.models.BillingModels.OrderItemRequest(
                        product.getId(),
                        product.getName(),
                        product.getPrice(),
                        qty
                ));
                totalAmount += (product.getPrice() * qty);
                
                // Prepare print data
                printItems.add(new com.posbillingapp.utils.PrinterService.ReceiptItem(
                    product.getName(),
                    qty,
                    (double) product.getPrice()
                ));
            }
        }
        
        String billNumber = binding.tvBillInfo.getText().toString().replace("Bill #: ", "");
        final double finalTotal = totalAmount;

        // NEW FLOW: Print First -> Then Create Order
        Toast.makeText(this, "Checking Printer...", Toast.LENGTH_SHORT).show();
        
        com.posbillingapp.utils.PrinterService.printReceipt(
            this,
            sessionManager.getCompanyName(),
            sessionManager.getUserRole(),
            billNumber,
            finalTotal,
            printItems,
            new com.posbillingapp.utils.PrinterService.PrintCallback() {
                @Override
                public void onSuccess() {
                    runOnUiThread(() -> {
                        Toast.makeText(BillingActivity.this, "Print Success! Creating Order...", Toast.LENGTH_SHORT).show();
                        createOrderInBackend(billNumber, finalTotal, orderItems, printItems);
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        Toast.makeText(BillingActivity.this, "Print Failed: " + error + "\nOrder NOT Created.", Toast.LENGTH_LONG).show();
                        logPrinterError(error);
                        resetSwipeButton();
                    });
                }
            }
        );
    }

    private void logPrinterError(String errorMsg) {
        String deviceInfo = android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL + " (Android " + android.os.Build.VERSION.RELEASE + ")";
        com.posbillingapp.models.ErrorLogRequest request = new com.posbillingapp.models.ErrorLogRequest(
            "PrinterError",
            "BillingActivity",
            errorMsg,
            "",
            deviceInfo,
            String.valueOf(sessionManager.getUserId())
        );

        RetrofitClient.getApiService().logError(request).enqueue(new Callback<com.posbillingapp.models.AuthModels.AuthResponse>() {
            @Override
            public void onResponse(Call<com.posbillingapp.models.AuthModels.AuthResponse> call, Response<com.posbillingapp.models.AuthModels.AuthResponse> response) {
                // Logged successfully (fail silently if not)
            }

            @Override
            public void onFailure(Call<com.posbillingapp.models.AuthModels.AuthResponse> call, Throwable t) {
                // Failed to log
            }
        });
    }

    private void createOrderInBackend(String billNumber, double totalAmount, 
                                      List<com.posbillingapp.models.BillingModels.OrderItemRequest> items,
                                      java.util.List<com.posbillingapp.utils.PrinterService.ReceiptItem> printItems) {
        
        long userId = sessionManager.getUserId();

        com.posbillingapp.models.BillingModels.OrderRequest request = new com.posbillingapp.models.BillingModels.OrderRequest(
                companyId,
                userId,
                billNumber,
                totalAmount,
                items
        );

        RetrofitClient.getApiService().createOrder(request).enqueue(new Callback<com.posbillingapp.models.BillingModels.OrderResponse>() {
             @Override
             public void onResponse(Call<com.posbillingapp.models.BillingModels.OrderResponse> call, Response<com.posbillingapp.models.BillingModels.OrderResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().success) {
                    Toast.makeText(BillingActivity.this, "Order Created!", Toast.LENGTH_SHORT).show();
                    
                    // Navigate to Receipt Activity (which can act as a Done screen now, or just finish)
                    // Since we already printed, we might pass a flag to ReceiptActivity to NOT auto-print?
                    // User said "print receipt... then count it". We already printed.
                    // Let's go to ReceiptActivity but disable auto-print there?
                    // Or just clear the screen and stay here?
                    // Standard flow is usually showing the receipt screen.
                    showReceiptPreview(billNumber, totalAmount, printItems);
                } else {
                    Toast.makeText(BillingActivity.this, "Order Creation Failed!", Toast.LENGTH_SHORT).show();
                    resetSwipeButton();
                }
            }

             @Override
             public void onFailure(Call<com.posbillingapp.models.BillingModels.OrderResponse> call, Throwable t) {
                 Toast.makeText(BillingActivity.this, "Network Error: Order NOT Created", Toast.LENGTH_SHORT).show();
                 resetSwipeButton();
             }
        });
    }

    private void resetSwipeButton() {
        binding.btnSwipeThumb.setOnTouchListener(null); 
        binding.tvSwipeText.setAlpha(1.0f);
        binding.btnSwipeThumb.animate().x(4).setDuration(200).start();
        binding.tvSwipeText.animate().alpha(1.0f).setDuration(200).start();
        setupSwipeButton(); // Re-attach listener
    }

    private void showReceiptPreview(String billNumber, double total, java.util.List<com.posbillingapp.utils.PrinterService.ReceiptItem> items) {
        Intent intent = new Intent(this, ReceiptActivity.class);
        intent.putExtra("billNumber", billNumber);
        intent.putExtra("total", total);
        intent.putExtra("items", (java.io.Serializable) items);
        intent.putExtra("alreadyPrinted", true); // Pass flag so ReceiptActivity doesn't print again
        startActivity(intent);
        finish();
    }
}
