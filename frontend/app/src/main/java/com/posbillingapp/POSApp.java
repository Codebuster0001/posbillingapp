package com.posbillingapp;

import android.app.Application;
import com.posbillingapp.network.RetrofitClient;

public class POSApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Initialize Retrofit with token interceptor
        RetrofitClient.init(this);
    }
}
