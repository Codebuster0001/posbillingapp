package com.posbillingapp.ui;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.snackbar.Snackbar;
import com.posbillingapp.databinding.ActivityRegisterBinding;
import com.posbillingapp.models.AuthModels.*;
import com.posbillingapp.models.LocationModels;
import java.util.List;

public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;
    private RegisterViewModel viewModel;
    
    private ArrayAdapter<LocationModels.Country> countryAdapter;
    private ArrayAdapter<LocationModels.State> stateAdapter;
    private ArrayAdapter<LocationModels.City> cityAdapter;

    private int selectedCountryId = 1; // Default
    private int selectedStateId = 0;
    private int selectedCityId = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(RegisterViewModel.class);

        setupObservers();
        setupLocationDropdowns();

        binding.btnRegister.setOnClickListener(v -> handleRegistration());
        
        // Initial load
        viewModel.fetchCountries();
    }

    private void setupObservers() {
        // Observe Countries
        viewModel.getCountries().observe(this, countries -> {
            countryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, countries);
            binding.etCountry.setAdapter(countryAdapter);
        });

        // Observe States
        viewModel.getStates().observe(this, states -> {
            stateAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, states);
            binding.etState.setAdapter(stateAdapter);
        });

        // Observe Cities
        viewModel.getCities().observe(this, cities -> {
            cityAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, cities);
            binding.etCity.setAdapter(cityAdapter);
        });

        // Observe Registration Result
        viewModel.getRegistrationResult().observe(this, res -> {
            if (res.success) {
                Toast.makeText(this, "Registered! Please Login.", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Snackbar.make(binding.getRoot(), res.message, Snackbar.LENGTH_LONG).show();
            }
        });

        // Observe Errors
        viewModel.getErrorMessage().observe(this, msg -> {
            Snackbar.make(binding.getRoot(), msg, Snackbar.LENGTH_LONG).show();
        });

        // Observe Loading State
        viewModel.getIsLoading().observe(this, isLoading -> {
            binding.btnRegister.setEnabled(!isLoading);
            binding.btnRegister.setText(isLoading ? "REGISTERING..." : "CREATE ACCOUNT");
        });
    }

    private void setupLocationDropdowns() {
        binding.etCountry.setOnItemClickListener((parent, view, position, id) -> {
            if (countryAdapter != null) {
                LocationModels.Country selected = countryAdapter.getItem(position);
                if (selected != null) {
                    selectedCountryId = selected.id;
                    resetLocationFields(true, true);
                    viewModel.fetchStates(selected.id);
                }
            }
        });

        binding.etState.setOnItemClickListener((parent, view, position, id) -> {
            if (stateAdapter != null) {
                LocationModels.State selected = stateAdapter.getItem(position);
                if (selected != null) {
                    selectedStateId = selected.id;
                    resetLocationFields(false, true);
                    viewModel.fetchCities(selected.id);
                }
            }
        });

        binding.etCity.setOnItemClickListener((parent, view, position, id) -> {
            if (cityAdapter != null) {
                LocationModels.City selected = cityAdapter.getItem(position);
                if (selected != null) {
                    selectedCityId = selected.id;
                }
            }
        });
    }

    private void resetLocationFields(boolean resetState, boolean resetCity) {
        if (resetState) {
            binding.etState.setText("");
            selectedStateId = 0;
            if (stateAdapter != null) stateAdapter.clear();
        }
        if (resetCity) {
            binding.etCity.setText("");
            selectedCityId = 0;
            if (cityAdapter != null) cityAdapter.clear();
        }
    }

    private void handleRegistration() {
        String company = binding.etCompanyName.getText() != null ? binding.etCompanyName.getText().toString().trim() : "";
        String phone = binding.etPhone.getText() != null ? binding.etPhone.getText().toString().trim() : "";
        String email = binding.etEmail.getText() != null ? binding.etEmail.getText().toString().trim() : "";
        String pass = binding.etPassword.getText() != null ? binding.etPassword.getText().toString().trim() : "";
        String confirm = binding.etConfirmPassword.getText() != null ? binding.etConfirmPassword.getText().toString().trim() : "";

        if (company.isEmpty() || phone.isEmpty() || email.isEmpty() || pass.isEmpty()) {
            Snackbar.make(binding.getRoot(), "Please fill all fields", Snackbar.LENGTH_SHORT).show();
            return;
        }

        if (!pass.equals(confirm)) {
            Snackbar.make(binding.getRoot(), "Passwords do not match", Snackbar.LENGTH_SHORT).show();
            return;
        }

        if (selectedCountryId <= 0) {
            Snackbar.make(binding.getRoot(), "Please select a country", Snackbar.LENGTH_SHORT).show();
            return;
        }

        if (selectedStateId <= 0) {
            Snackbar.make(binding.getRoot(), "Please select a state", Snackbar.LENGTH_SHORT).show();
            return;
        }

        if (selectedCityId <= 0) {
            Snackbar.make(binding.getRoot(), "Please select a city", Snackbar.LENGTH_SHORT).show();
            return;
        }

        RegisterRequest request = new RegisterRequest(company, phone, email, pass, selectedCountryId, selectedStateId, selectedCityId);
        viewModel.register(request);
    }
}
