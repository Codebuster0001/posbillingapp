package com.posbillingapp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.posbillingapp.R;
import com.posbillingapp.adapter.EmployeeAdapter;
import com.posbillingapp.databinding.ActivityEmployeeBinding;
import com.posbillingapp.models.EmployeeModel;
import com.posbillingapp.models.EmployeeModel.AddEmployeeRequest;
import com.posbillingapp.network.RetrofitClient;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EmployeeActivity extends AppCompatActivity {

    private ActivityEmployeeBinding binding;
    private EmployeeAdapter adapter;
    private long companyId;
    private com.posbillingapp.utils.SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEmployeeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new com.posbillingapp.utils.SessionManager(this);
        companyId = sessionManager.getCompanyId();

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            binding.toolbar.setNavigationOnClickListener(v -> onBackPressed());
        }

        if (companyId == -1) {
            Toast.makeText(this, "Session Error: Invalid Company ID. Please log in again.", Toast.LENGTH_LONG).show();
        }

        setupRecyclerView();
        fetchEmployees();

        binding.fabAddEmployee.setOnClickListener(v -> showAddEmployeeDialog());
    }

    private void setupRecyclerView() {
        adapter = new EmployeeAdapter(new ArrayList<>(), new EmployeeAdapter.OnEmployeeActionListener() {
            @Override
            public void onEdit(EmployeeModel employee) {
                showEditEmployeeDialog(employee);
            }

            @Override
            public void onDelete(long id) {
                new com.google.android.material.dialog.MaterialAlertDialogBuilder(EmployeeActivity.this)
                    .setTitle("Delete Employee")
                    .setMessage("Are you sure you want to delete this employee?")
                    .setPositiveButton("Delete", (dialog, which) -> deleteEmployee(id))
                    .setNegativeButton("Cancel", null)
                    .show();
            }

            @Override
            public void onAssignItems(EmployeeModel employee) {
                openAssignMenuActivity(employee);
            }
        });
        binding.rvEmployees.setLayoutManager(new LinearLayoutManager(this));
        binding.rvEmployees.setAdapter(adapter);
    }

    private void fetchEmployees() {
        android.util.Log.d("EMP_LIST", "Fetching employees for company: " + companyId);
        RetrofitClient.getApiService().getEmployees(companyId).enqueue(new Callback<List<EmployeeModel>>() {
            @Override
            public void onResponse(Call<List<EmployeeModel>> call, Response<List<EmployeeModel>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    android.util.Log.d("EMP_LIST", "Loaded " + response.body().size() + " employees");
                    adapter.updateList(response.body());
                } else {
                    String error = "Error " + response.code();
                    try {
                        if (response.errorBody() != null) error += ": " + response.errorBody().string();
                    } catch (Exception e) {}
                    android.util.Log.e("EMP_LIST", error);
                    Toast.makeText(EmployeeActivity.this, "Failed to load: " + error, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<List<EmployeeModel>> call, Throwable t) {
                android.util.Log.e("EMP_LIST", "Network Fail: " + t.getMessage());
                Toast.makeText(EmployeeActivity.this, "Network error loading employees", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAddEmployeeDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_employee, null);
        EditText etName = dialogView.findViewById(R.id.etEmployeeName);
        EditText etEmail = dialogView.findViewById(R.id.etEmployeeEmail);
        EditText etPhone = dialogView.findViewById(R.id.etEmployeePhone);
        RadioGroup rgRole = dialogView.findViewById(R.id.rgRole);

        new MaterialAlertDialogBuilder(this)
            .setTitle("Add New Employee")
            .setView(dialogView)
            .setPositiveButton("Add", (dialog, which) -> {
                String name = etName.getText().toString().trim();
                String email = etEmail.getText().toString().trim();
                String phone = etPhone.getText().toString().trim();
                String role = rgRole.getCheckedRadioButtonId() == R.id.rbFull ? "Full Access" : "Limited Access";

                if (name.isEmpty() || email.isEmpty()) {
                    Toast.makeText(this, "Name and Email are required", Toast.LENGTH_SHORT).show();
                    return;
                }

                addEmployee(new AddEmployeeRequest(companyId, name, email, phone, role));
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void showEditEmployeeDialog(EmployeeModel employee) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_employee, null);
        EditText etName = dialogView.findViewById(R.id.etEmployeeName);
        EditText etEmail = dialogView.findViewById(R.id.etEmployeeEmail);
        EditText etPhone = dialogView.findViewById(R.id.etEmployeePhone);
        RadioGroup rgRole = dialogView.findViewById(R.id.rgRole);

        etName.setText(employee.getName());
        etEmail.setText(employee.getEmail());
        etEmail.setEnabled(false); // Typically email shouldn't be changed
        etPhone.setText(employee.getPhoneNumber());
        
        // Set existing role
        if ("Full Access".equals(employee.getRole())) {
            rgRole.check(R.id.rbFull);
        } else {
            rgRole.check(R.id.rbLimited);
        }

        new MaterialAlertDialogBuilder(this)
            .setTitle("Edit Employee")
            .setView(dialogView)
            .setPositiveButton("Update", (dialog, which) -> {
                employee.setName(etName.getText().toString().trim());
                employee.setPhoneNumber(etPhone.getText().toString().trim());
                employee.setRole(rgRole.getCheckedRadioButtonId() == R.id.rbFull ? "Full Access" : "Limited Access");
                updateEmployee(employee);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void addEmployee(AddEmployeeRequest request) {
        android.util.Log.d("EMP_ADD", "Request: " + new com.google.gson.Gson().toJson(request));
        RetrofitClient.getApiService().addEmployee(request).enqueue(new Callback<com.posbillingapp.models.AuthModels.AuthResponse>() {
            @Override
            public void onResponse(Call<com.posbillingapp.models.AuthModels.AuthResponse> call, Response<com.posbillingapp.models.AuthModels.AuthResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    com.posbillingapp.models.AuthModels.AuthResponse res = response.body();
                    android.util.Log.d("EMP_ADD", "Success: " + res.message);
                    Toast.makeText(EmployeeActivity.this, res.message != null ? res.message : "Employee Added Successfully", Toast.LENGTH_LONG).show();
                    fetchEmployees();
                } else {
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "Unknown error";
                        android.util.Log.e("EMP_ADD", "Error: " + response.code() + " - " + errorBody);
                        Toast.makeText(EmployeeActivity.this, "Error: " + errorBody, Toast.LENGTH_LONG).show();
                    } catch (java.io.IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(Call<com.posbillingapp.models.AuthModels.AuthResponse> call, Throwable t) {
                android.util.Log.e("EMP_ADD", "Failure: " + t.getMessage());
                Toast.makeText(EmployeeActivity.this, "Network Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateEmployee(EmployeeModel employee) {
        RetrofitClient.getApiService().updateEmployee(employee.getId(), employee).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(EmployeeActivity.this, "Employee Updated", Toast.LENGTH_SHORT).show();
                    fetchEmployees();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(EmployeeActivity.this, "Update Failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteEmployee(long id) {
        RetrofitClient.getApiService().deleteEmployee(id).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(EmployeeActivity.this, "Employee Deleted", Toast.LENGTH_SHORT).show();
                    fetchEmployees();
                } else {
                    Toast.makeText(EmployeeActivity.this, "Delete Failed", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(EmployeeActivity.this, "Network Error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openAssignMenuActivity(EmployeeModel employee) {
        Intent intent = new Intent(this, AssignMenuActivity.class);
        intent.putExtra("userId", employee.getId());
        intent.putExtra("companyId", companyId);
        intent.putExtra("employeeName", employee.getName());
        intent.putExtra("employeeRole", employee.getRole());
        startActivity(intent);
    }
}
