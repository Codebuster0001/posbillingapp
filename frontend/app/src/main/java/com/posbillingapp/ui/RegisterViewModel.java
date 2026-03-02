package com.posbillingapp.ui;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.posbillingapp.models.AuthModels.*;
import com.posbillingapp.models.LocationModels;
import com.posbillingapp.network.RetrofitClient;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterViewModel extends ViewModel {
    private final MutableLiveData<List<LocationModels.Country>> countries = new MutableLiveData<>();
    private final MutableLiveData<List<LocationModels.State>> states = new MutableLiveData<>();
    private final MutableLiveData<List<LocationModels.City>> cities = new MutableLiveData<>();
    private final MutableLiveData<AuthResponse> registrationResult = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    public LiveData<List<LocationModels.Country>> getCountries() { return countries; }
    public LiveData<List<LocationModels.State>> getStates() { return states; }
    public LiveData<List<LocationModels.City>> getCities() { return cities; }
    public LiveData<AuthResponse> getRegistrationResult() { return registrationResult; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }

    public void fetchCountries() {
        RetrofitClient.getApiService().getCountries().enqueue(new Callback<List<LocationModels.Country>>() {
            @Override
            public void onResponse(Call<List<LocationModels.Country>> call, Response<List<LocationModels.Country>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    countries.setValue(response.body());
                } else {
                    errorMessage.setValue("Failed to load countries.");
                }
            }

            @Override
            public void onFailure(Call<List<LocationModels.Country>> call, Throwable t) {
                errorMessage.setValue("Network error: " + t.getLocalizedMessage());
            }
        });
    }

    public void fetchStates(int countryId) {
        RetrofitClient.getApiService().getStates(countryId).enqueue(new Callback<List<LocationModels.State>>() {
            @Override
            public void onResponse(Call<List<LocationModels.State>> call, Response<List<LocationModels.State>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    states.setValue(response.body());
                } else {
                    errorMessage.setValue("Failed to load states.");
                }
            }

            @Override
            public void onFailure(Call<List<LocationModels.State>> call, Throwable t) {
                errorMessage.setValue("Network error: " + t.getLocalizedMessage());
            }
        });
    }

    public void fetchCities(int stateId) {
        RetrofitClient.getApiService().getCities(stateId).enqueue(new Callback<List<LocationModels.City>>() {
            @Override
            public void onResponse(Call<List<LocationModels.City>> call, Response<List<LocationModels.City>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    cities.setValue(response.body());
                } else {
                    errorMessage.setValue("Failed to load cities.");
                }
            }

            @Override
            public void onFailure(Call<List<LocationModels.City>> call, Throwable t) {
                errorMessage.setValue("Network error: " + t.getLocalizedMessage());
            }
        });
    }

    public void register(RegisterRequest request) {
        isLoading.setValue(true);
        RetrofitClient.getApiService().register(request).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                isLoading.setValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    registrationResult.setValue(response.body());
                } else {
                    String error = "Registration failed";
                    try {
                        if (response.errorBody() != null) {
                            String errorStr = response.errorBody().string();
                            if (errorStr.contains("message")) {
                                AuthResponse errRes = new com.google.gson.Gson().fromJson(errorStr, AuthResponse.class);
                                error = errRes.message;
                            } else {
                                error = errorStr;
                            }
                        }
                    } catch (Exception e) {}
                    errorMessage.setValue(error);
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                isLoading.setValue(false);
                errorMessage.setValue("Network error: " + t.getLocalizedMessage());
            }
        });
    }
}
