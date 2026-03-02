package com.posbillingapp.utils;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.RequiresPermission;

import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class PrinterService {
    private static final String TAG = "PrinterService";
    private static final UUID PRINTER_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    public interface PrintCallback {
        void onSuccess();
        void onError(String error);
    }

    public static class ReceiptItem implements java.io.Serializable {
        public String name;
        public int quantity;
        public double price;

        public ReceiptItem(String name, int quantity, double price) {
            this.name = name;
            this.quantity = quantity;
            this.price = price;
        }

        public double getTotal() {
            return quantity * price;
        }
    }

    // Main method - only checks hardware (Bluetooth/USB)
    public static void printReceipt(Context context, String companyName, String userRole, String billNumber, double total, 
                                     java.util.List<ReceiptItem> items, PrintCallback callback) {
        if (tryBluetoothPrint(context, companyName, userRole, billNumber, total, items, callback)) {
            if (!tryUsbPrint(context, companyName, userRole, billNumber, total, items, callback)) {
                callback.onError("Please connect printer");
            }
        }
    }

    public static void printViaBluetooth(Context context, String companyName, String userRole, String billNumber, double total, 
                                         java.util.List<ReceiptItem> items, PrintCallback callback) {
        if (tryBluetoothPrint(context, companyName, userRole, billNumber, total, items, callback)) {
            callback.onError("No paired Bluetooth printer found. Make sure your printer (like KP307) is paired in settings.");
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public static void printToDevice(Context context, BluetoothDevice device, String companyName, String userRole, String billNumber, double total,
                                     List<ReceiptItem> items, PrintCallback callback) {
        new Thread(() -> {
            try {
                BluetoothSocket socket = device.createRfcommSocketToServiceRecord(PRINTER_UUID);
                socket.connect();
                
                OutputStream outputStream = socket.getOutputStream();
                String receiptContent = buildReceiptContent(context, companyName, userRole, billNumber, total, items);
                
                outputStream.write(receiptContent.getBytes());
                outputStream.write(new byte[]{0x1D, 0x56, 0x00}); // Cut
                
                outputStream.flush();
                outputStream.close();
                socket.close();
                callback.onSuccess();
            } catch (Exception e) {
                callback.onError("Failed to connect to " + device.getName() + ": " + e.getMessage());
            }
        }).start();
    }

    public static java.util.List<BluetoothDevice> getPairedDevices(Context context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                return new java.util.ArrayList<>();
            }
        }

        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager == null) return new java.util.ArrayList<>();
        
        android.bluetooth.BluetoothAdapter adapter = bluetoothManager.getAdapter();
        if (adapter != null && adapter.isEnabled()) {
            return new java.util.ArrayList<>(adapter.getBondedDevices());
        }
        return new java.util.ArrayList<>();
    }

    public static void printViaUsb(Context context, String companyName, String userRole, String billNumber, double total, 
                                  java.util.List<ReceiptItem> items, PrintCallback callback) {
        if (!tryUsbPrint(context, companyName, userRole, billNumber, total, items, callback)) {
            callback.onError("USB printer not found. Please connect printer.");
        }
    }

    public static void printViaSystem(Context context, String companyName, String userRole, String billNumber, double total, 
                                     java.util.List<ReceiptItem> items, PrintCallback callback) {
        printViaAndroidPrintFramework(context, companyName, userRole, billNumber, total, items, callback);
    }

    // Bluetooth Thermal Printer
    private static boolean tryBluetoothPrint(Context context, String companyName, String userRole, String billNumber, double total,
                                             java.util.List<ReceiptItem> items, PrintCallback callback) {
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "Missing BLUETOOTH_CONNECT permission");
                return true;
            }
        }

        try {
            BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
            if (bluetoothManager == null) return true;
            
            android.bluetooth.BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
            
            if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
                Log.e(TAG, "Bluetooth not available or disabled.");
                return true;
            }

            // Android 12+ check - if we lack permission, we might fail, but we'll try to find bonded devices anyway.
            // On some devices, getBondedDevices() might still work if the device is already paired.
            java.util.Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
            if (pairedDevices == null || pairedDevices.isEmpty()) {
                Log.e(TAG, "No paired devices found.");
                return true;
            }

            BluetoothDevice printer = null;
            // 1. Pass: Look for anything that sounds like a printer
            for (BluetoothDevice device : pairedDevices) {
                String name = device.getName();
                if (name == null) continue;
                String lowerName = name.toLowerCase();
                if (lowerName.contains("print") || lowerName.contains("thermal") || 
                    lowerName.contains("pos") || lowerName.contains("receipt") || 
                    lowerName.contains("mpt") || lowerName.contains("rpp") || 
                    lowerName.contains("kp")) {
                    printer = device;
                    break;
                }
            }

            // 2. Pass Fallback: Just take the first paired device if nothing obvious found
            if (printer == null && !pairedDevices.isEmpty()) {
                printer = pairedDevices.iterator().next();
            }

            if (printer == null) {
                return true;
            }

            // Print in background thread
            final BluetoothDevice finalPrinter = printer;
            new Thread(() -> {
                BluetoothSocket socket = null;
                try {
                    // 1. Try secure connection 
                    try {
                        socket = finalPrinter.createRfcommSocketToServiceRecord(PRINTER_UUID);
                        socket.connect();
                    } catch (Exception e1) {
                        Log.w(TAG, "Secure connection failed, trying insecure...");
                        // 2. Try insecure connection
                        try {
                            socket = finalPrinter.createInsecureRfcommSocketToServiceRecord(PRINTER_UUID);
                            socket.connect();
                        } catch (Exception e2) {
                            Log.w(TAG, "Insecure connection failed, trying reflection fallback...");
                            // 3. Reflection Fallback (Port 1 is standard for SPP)
                            socket = (BluetoothSocket) finalPrinter.getClass()
                                .getMethod("createRfcommSocket", new Class[]{int.class})
                                .invoke(finalPrinter, 1);
                            socket.connect();
                        }
                    }
                    
                    // Small delay to stabilize connection
                    Thread.sleep(500);

                    OutputStream outputStream = socket.getOutputStream();
                    
                    // Initialize printer (ESC @)
                    outputStream.write(new byte[]{0x1B, 0x40});
                    
                    String receiptContent = buildReceiptContent(context, companyName, userRole, billNumber, total, items);
                    
                    // Send to printer using UTF-8
                    outputStream.write(receiptContent.getBytes("UTF-8"));
                    
                    // ESC/POS Cut
                    outputStream.write(new byte[]{0x1D, 0x56, 0x00});
                    
                    outputStream.flush();
                    outputStream.close();
                    socket.close();

                    callback.onSuccess();
                    
                } catch (Exception e) {
                    Log.e(TAG, "Bluetooth print error", e);
                    if (socket != null) {
                        try { socket.close(); } catch (Exception ignored) {}
                    }
                    callback.onError("Bluetooth error: " + e.getMessage());
                }
            }).start();
            
            return false;
            
        } catch (Exception e) {
            Log.e(TAG, "Bluetooth setup error", e);
            return true;
        }
    }

    // USB Printer
    private static boolean tryUsbPrint(Context context, String companyName, String userRole, String billNumber, double total,
                                      java.util.List<ReceiptItem> items, PrintCallback callback) {
        try {
            UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
            if (usbManager == null) {
                return false;
            }

            Map<String, UsbDevice> deviceList = usbManager.getDeviceList();
            if (deviceList.isEmpty()) {
                return false;
            }

            // Find printer device (typically Class 7 = Printer)
            UsbDevice printer = null;
            for (UsbDevice device : deviceList.values()) {
                if (device.getDeviceClass() == 7 || // Printer class
                    device.getDeviceName().toLowerCase().contains("printer")) {
                    printer = device;
                    break;
                }
            }

            if (printer == null) {
                return false;
            }

            final UsbDevice finalPrinter = printer;
            new Thread(() -> {
                try {
                    // Note: USB printing requires UsbConnection and proper endpoint setup
                    // This is a simplified version - full implementation would need USB permissions
                    callback.onError("USB printer detected but requires manual permission. Please use Android Print Framework.");
                } catch (Exception e) {
                    Log.e(TAG, "USB print error", e);
                    callback.onError("USB print failed: " + e.getMessage());
                }
            }).start();
            
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "USB setup error", e);
            return false;
        }
    }

    // Android Print Framework (WiFi, Network, Cloud printers)
    private static void printViaAndroidPrintFramework(Context context, String companyName, String userRole, String billNumber, 
                                                     double total, java.util.List<ReceiptItem> items, 
                                                     PrintCallback callback) {
        if (!(context instanceof Activity)) {
            callback.onError("Print Framework requires Activity context");
            return;
        }

        Activity activity = (Activity) context;
        
        // Create web view for printing
        WebView webView = new WebView(context);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                createPrintJob(activity, view, billNumber, callback);
            }
        });

        // Generate HTML receipt
        String htmlReceipt = buildHtmlReceipt(companyName, userRole, billNumber, total, items);
        webView.loadDataWithBaseURL(null, htmlReceipt, "text/html", "UTF-8", null);
    }

    private static void createPrintJob(Activity activity, WebView webView, 
                                      String billNumber, PrintCallback callback) {
        PrintManager printManager = (PrintManager) activity.getSystemService(Context.PRINT_SERVICE);
        
        String jobName = "Receipt - " + billNumber;
        PrintDocumentAdapter printAdapter = webView.createPrintDocumentAdapter(jobName);
        
        PrintAttributes attributes = new PrintAttributes.Builder()
                .setMediaSize(PrintAttributes.MediaSize.NA_INDEX_3X5) // Receipt size
                .setResolution(new PrintAttributes.Resolution("id", "Receipt", 203, 203))
                .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                .build();

        printManager.print(jobName, printAdapter, attributes);
        
        callback.onSuccess();
    }

    // Build plain text receipt for thermal printers
    private static String buildReceiptContent(Context context, String companyName, String userRole, String billNumber, double total, java.util.List<ReceiptItem> items) {
        StringBuilder receipt = new StringBuilder();
        
        // Get Safe Currency (ASCII only)
        String currency = getSafeCurrency(context);
        
        // Center Align Command
        receipt.append((char)27).append((char)97).append((char)1);
        
        // Header
        receipt.append("================================\n");
        receipt.append(companyName.toUpperCase()).append("\n");
        receipt.append("================================\n\n");
        
        // Bill info (Split Left/Right)
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
        String dateStr = dateFormat.format(new Date());
        
        // Helper to center or split text would be nice, but we'll do manual formatting for 32 chars
        // Receipt ID
        String labelId = "Receipt ID";
        String valId = billNumber;
        int padId = 32 - labelId.length() - valId.length();
        if (padId > 0) receipt.append(labelId).append(" ".repeat(padId)).append(valId).append("\n");
        else receipt.append(labelId).append(" ").append(valId).append("\n");

        // Date
        String labelDate = "Date/Time";
        int padDate = 32 - labelDate.length() - dateStr.length();
        if (padDate > 0) receipt.append(labelDate).append(" ".repeat(padDate)).append(dateStr).append("\n");
        else receipt.append(labelDate).append(" ").append(dateStr).append("\n");
        
        receipt.append("--------------------------------\n");
        
        // Items Header (Simplified matches preview)
        receipt.append("ORDER SUMMARY\n");

        for (ReceiptItem item : items) {
            String itemStr;
            String priceStr = String.format("%s%.0f", currency, item.getTotal());
            if (item.getTotal() != (long)item.getTotal()) {
                priceStr = String.format("%s%.2f", currency, item.getTotal());
            }

            // Logic matching ReceiptPreview: Hide Qty for Limited Access
            if ("Admin".equalsIgnoreCase(userRole) || "Full Access".equalsIgnoreCase(userRole)) {
                itemStr = item.name + " x " + item.quantity;
            } else {
                itemStr = item.name;
            }

            if (itemStr.length() > 22) itemStr = itemStr.substring(0, 19) + "...";
            
            int padding = 32 - itemStr.length() - priceStr.length();
            if (padding < 1) padding = 1;
            
            receipt.append(itemStr).append(" ".repeat(padding)).append(priceStr).append("\n");
        }
        receipt.append("--------------------------------\n");
        
        // Total (Split Left/Right)
        String labelTotal = "TOTAL AMOUNT";
        String valTotal;
        if (total == (long)total) valTotal = String.format("%s%d", currency, (long)total);
        else valTotal = String.format("%s%.2f", currency, total);
        
        int padTotal = 32 - labelTotal.length() - valTotal.length();
        if (padTotal > 0) receipt.append(labelTotal).append(" ".repeat(padTotal)).append(valTotal).append("\n");
        else receipt.append(labelTotal).append(" ").append(valTotal).append("\n");
        
        // Footer (Center)
        receipt.append("================================\n");
        receipt.append("     Thank You Visit Again!     \n");
        receipt.append("================================\n\n\n");
        
        // Reset to Left Align (Standard practice)
        receipt.append((char)27).append((char)97).append((char)0);
        
        return receipt.toString();
    }

    // Build HTML receipt for Android Print Framework
    private static String buildHtmlReceipt(String companyName, String userRole, String billNumber, double total, java.util.List<ReceiptItem> items) {
        StringBuilder html = new StringBuilder();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
        
        String currency = CurrencyUtils.getCurrencySymbol();
        if (currency == null) currency = ""; // Fallback to empty string
        
        html.append("<!DOCTYPE html><html><head>");
        html.append("<style>");
        html.append("body { font-family: 'Courier New', Courier, monospace; width: 300px; margin: 0; padding: 10px; color: #000; }");
        html.append(".header { text-align: center; border-bottom: 2px solid #000; padding-bottom: 10px; margin-bottom: 10px; }");
        html.append(".company-name { font-size: 20px; font-weight: bold; }");
        html.append(".bill-info { font-size: 14px; margin-bottom: 15px; }");
        html.append("table { width: 100%; border-collapse: collapse; }");
        html.append("th { border-bottom: 1px solid #000; text-align: left; padding: 5px 0; }");
        html.append("td { padding: 5px 0; }");
        html.append(".text-right { text-align: right; }");
        html.append(".total-section { border-top: 2px solid #000; margin-top: 10px; padding-top: 10px; }");
        html.append(".footer { text-align: center; margin-top: 30px; border-top: 1px dashed #000; padding-top: 10px; font-style: italic; }");
        html.append("</style></head><body>");
        
        html.append("<div class='header'>");
        html.append("<div class='company-name'>").append(companyName.toUpperCase()).append("</div>");
        html.append("<div>Official Receipt</div>");
        html.append("</div>");
        
        html.append("<div class='bill-info'>");
        html.append("<div>Bill #: ").append(billNumber).append("</div>");
        html.append("<div>Date: ").append(dateFormat.format(new Date())).append("</div>");
        html.append("</div>");
        
        html.append("<table><thead><tr>");
        html.append("<th>ITEM</th>");
        if ("Admin".equalsIgnoreCase(userRole) || "Full Access".equalsIgnoreCase(userRole)) {
            html.append("<th class='text-right'>QTY</th>");
        }
        html.append("<th class='text-right'>PRICE</th>");
        html.append("</tr></thead><tbody>");
        
        for (ReceiptItem item : items) {
            html.append("<tr>");
            html.append("<td>").append(item.name).append("</td>");
            if ("Admin".equalsIgnoreCase(userRole) || "Full Access".equalsIgnoreCase(userRole)) {
                html.append("<td class='text-right'>").append(item.quantity).append("</td>");
            }
            html.append("<td class='text-right'>").append(currency).append(String.format("%.2f", item.getTotal())).append("</td>");
            html.append("</tr>");
        }
        
        html.append("</tbody></table>");
        
        html.append("<div class='total-section'>");
        html.append("<div class='text-right' style='font-size: 18px; font-weight: bold;'>");
        html.append("TOTAL: ").append(currency).append(String.format("%.2f", total)).append("</div>");
        html.append("</div>");
        
        html.append("<div class='footer'>Thank you for your business!</div>");
        html.append("</body></html>");
        
        return html.toString();
    }

    private static String getSafeCurrency(Context context) {
        SessionManager sm = new SessionManager(context);
        String symbol = sm.getCurrencySymbol();
        String code = sm.getCurrencyCode();

        // If it's the Rupee symbol, use "Rs." because most thermal printers can't print ₹
        if ("₹".equals(symbol)) return "Rs.";

        // Use symbol if it exists
        if (symbol != null && !symbol.isEmpty()) return symbol;
        // Fallback to code
        if (code != null && !code.isEmpty()) return code;
        return "";
    }

    private static boolean isSafeAscii(String s) {
        if (s == null || s.isEmpty()) return false;
        for (char c : s.toCharArray()) {
            if (c > 127) return false;
        }
        return true;
    }
}
