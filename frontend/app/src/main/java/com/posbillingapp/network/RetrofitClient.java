package com.posbillingapp.network;

import android.content.Context;
import com.posbillingapp.utils.SessionManager;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static Retrofit retrofit = null;
    private static SessionManager sessionManager = null;

    public static void init(Context context) {
        if (sessionManager == null) {
            sessionManager = new SessionManager(context.getApplicationContext());
        }
    }

    public static void resetClient() {
        retrofit = null;
    }

    public static String getBaseUrl() {
        return "https://posbillingapp-production.up.railway.app/";
    }

    public static ApiService getApiService() {
        if (retrofit == null) {
            // Add logging for debugging API calls as requested by user
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .addInterceptor(chain -> {
                        Request original = chain.request();
                        Request.Builder requestBuilder = original.newBuilder();
                        
                        // Add JWT Token if available
                        if (sessionManager != null) {
                            String token = sessionManager.getToken();
                            if (token != null && !token.isEmpty()) {
                                requestBuilder.addHeader("Authorization", "Bearer " + token);
                            }
                        }
                        
                        return chain.proceed(requestBuilder.build());
                    })
                    .build();

            String baseUrl = getBaseUrl();

            retrofit = new Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit.create(ApiService.class);
    }
}
