package com.posbillingapp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.posbillingapp.R;
import com.posbillingapp.databinding.ActivityReceiptBinding;
import com.posbillingapp.utils.PrinterService;
import com.posbillingapp.utils.SessionManager;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.List;
import java.util.ArrayList;

public class ReceiptActivity extends AppCompatActivity {

    private ActivityReceiptBinding binding;
    private String billNumber;
    private double total;
    private List<PrinterService.ReceiptItem> items;
    private SessionManager sessionManager;
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Toast.makeText(this, "Permission granted! Try printing again.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Bluetooth permission is required for printing.", Toast.LENGTH_LONG).show();
                }
            });

    @SuppressWarnings("unchecked")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityReceiptBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);

        // Get data from intent
        billNumber = getIntent().getStringExtra("billNumber");
        total = getIntent().getDoubleExtra("total", 0.0);
        
        try {
            items = (List<PrinterService.ReceiptItem>) getIntent().getSerializableExtra("items");
        } catch (Exception e) {
            items = new ArrayList<>();
        }

        setupUI();
        setupButtons();

        boolean alreadyPrinted = getIntent().getBooleanExtra("alreadyPrinted", false);
        
        if (alreadyPrinted) {
            // If already printed successfully in BillingActivity, just show UI and enable Done button
            binding.btnDone.setEnabled(true);
            binding.btnDone.setAlpha(1.0f);
        } else {
            // Auto-trigger printing logic (checks Bluetooth/USB first)
            triggerPrint();
        }
    }

    private void setupUI() {
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        // Set business info
        binding.tvCompanyName.setText(sessionManager.getCompanyName().toUpperCase());

        // Set bill details
        binding.tvBillNumber.setText(billNumber);
        
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
        binding.tvDate.setText(dateFormat.format(new Date()));
        
        binding.tvTotal.setText(com.posbillingapp.utils.CurrencyUtils.format(total));

        // Display items
        LayoutInflater inflater = LayoutInflater.from(this);
        if (items != null) {
            for (PrinterService.ReceiptItem item : items) {
                View itemView = inflater.inflate(R.layout.item_receipt_line, binding.layoutItems, false);
                
                TextView tvItemName = itemView.findViewById(R.id.tvItemName);
                TextView tvItemPrice = itemView.findViewById(R.id.tvItemPrice);
                
                if ("Admin".equalsIgnoreCase(sessionManager.getUserRole()) || "Full Access".equalsIgnoreCase(sessionManager.getUserRole())) {
                    tvItemName.setText(item.name + " x " + item.quantity);
                } else {
                    tvItemName.setText(item.name);
                }
                double itemTotal = item.getTotal();
                tvItemPrice.setText(com.posbillingapp.utils.CurrencyUtils.format(itemTotal));
                
                binding.layoutItems.addView(itemView);
            }
        }
    }

    private void setupButtons() {
        binding.btnDone.setEnabled(false); // Disable until printed
        binding.btnDone.setAlpha(0.5f);

        binding.btnPrintAgain.setOnClickListener(v -> {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                if (checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    requestPermissionLauncher.launch(android.Manifest.permission.BLUETOOTH_CONNECT);
                    return;
                }
            }
            triggerPrint();
        });

        binding.btnDone.setOnClickListener(v -> navigateToDashboard());
    }

    private void triggerPrint() {
         PrinterService.printReceipt(this, 
                sessionManager.getCompanyName(),
                sessionManager.getUserRole(),
                billNumber, total, items, getPrintCallback());
    }

    private PrinterService.PrintCallback getPrintCallback() {
        return new PrinterService.PrintCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    Toast.makeText(ReceiptActivity.this, "Print Success! Order Counted.", Toast.LENGTH_SHORT).show();
                    binding.btnDone.setEnabled(true);
                    binding.btnDone.setAlpha(1.0f);
                    // Automatically click done? Or just let user click. User said "then count it".
                    // We simply enable the button to allow them to "finish" the flow.
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(ReceiptActivity.this, "Print Failed: " + error + ". Order NOT Counted.", Toast.LENGTH_LONG).show();
                    // Button remains disabled
                    binding.btnDone.setEnabled(false);
                    binding.btnDone.setAlpha(0.5f);
                });
            }
        };
    }

    private void navigateToDashboard() {
        Intent intent = new Intent(this, DashboardActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("role", sessionManager.getUserRole());
        intent.putExtra("companyId", sessionManager.getCompanyId());
        startActivity(intent);
        finish();
    }
}
