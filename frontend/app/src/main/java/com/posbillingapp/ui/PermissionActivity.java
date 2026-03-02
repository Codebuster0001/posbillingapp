package com.posbillingapp.ui;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.posbillingapp.databinding.ActivityPermissionBinding;
import com.posbillingapp.utils.SessionManager;

public class PermissionActivity extends AppCompatActivity {

    private ActivityPermissionBinding binding;
    private SessionManager session;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPermissionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        session = new SessionManager(this);

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Access Control");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        binding.toolbar.setNavigationOnClickListener(v -> finish());

        setupWebView();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        WebSettings settings = binding.webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        
        // Construct URL
        String ip = session.getServerIp();
        String url = "http://" + ip + ":5173/admin/permissions?mobile=true";

        binding.webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                binding.progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                binding.progressBar.setVisibility(View.GONE);
                
                // Inject JWT into LocalStorage
                String token = session.getToken();
                String userJson = String.format("{\"role\":\"%s\",\"companyId\":%d,\"userId\":%d,\"name\":\"Admin\",\"companyName\":\"%s\",\"success\":true}", 
                    session.getUserRole(), session.getCompanyId(), session.getUserId(), session.getCompanyName());
                
                // Only reload if we haven't injected yet (detect by checking localStorage)
                String script = "if (!localStorage.getItem('token')) {" +
                               "localStorage.setItem('token', '" + token + "');" +
                               "localStorage.setItem('user', '" + userJson + "');" +
                               "localStorage.setItem('role', 'Admin');" +
                               "location.reload();" +
                               "}";
                
                view.evaluateJavascript(script, null);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(PermissionActivity.this, "Failed to connect to web dashboard. Check Server IP.", Toast.LENGTH_LONG).show();
            }
        });

        binding.webView.loadUrl(url);
    }

    @Override
    public void onBackPressed() {
        if (binding.webView.canGoBack()) {
            binding.webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
