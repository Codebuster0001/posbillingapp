package com.posbillingapp.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.posbillingapp.R;
import com.posbillingapp.adapter.MenuAdapter;
import com.posbillingapp.databinding.ActivityMenuBinding;
import com.posbillingapp.models.MenuItemModel;
import com.posbillingapp.models.MenuItemModel.AddMenuItemRequest;
import com.posbillingapp.network.RetrofitClient;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MenuActivity extends AppCompatActivity {

    private ActivityMenuBinding binding;
    private MenuAdapter adapter;
    private long companyId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMenuBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        companyId = getIntent().getLongExtra("companyId", -1);

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            binding.toolbar.setNavigationOnClickListener(v -> onBackPressed());
        }

        setupRecyclerView();
        fetchMenu();

        binding.fabAddMenuItem.setOnClickListener(v -> showAddMenuItemDialog());
    }

    private void setupRecyclerView() {
        adapter = new MenuAdapter(new ArrayList<>(), new MenuAdapter.OnMenuItemActionListener() {
            @Override
            public void onEdit(MenuItemModel item) {
                showEditMenuItemDialog(item);
            }

            @Override
            public void onDelete(long id) {
                new MaterialAlertDialogBuilder(MenuActivity.this)
                    .setTitle("Delete Item")
                    .setMessage("Are you sure you want to delete this item?")
                    .setPositiveButton("Delete", (dialog, which) -> deleteMenuItem(id))
                    .setNegativeButton("Cancel", null)
                    .show();
            }
        });
        binding.rvMenu.setLayoutManager(new LinearLayoutManager(this));
        binding.rvMenu.setAdapter(adapter);
    }

    private void fetchMenu() {
        RetrofitClient.getApiService().getMenu(companyId).enqueue(new Callback<List<MenuItemModel>>() {
            @Override
            public void onResponse(Call<List<MenuItemModel>> call, Response<List<MenuItemModel>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    adapter.updateList(response.body());
                }
            }

            @Override
            public void onFailure(Call<List<MenuItemModel>> call, Throwable t) {
                Toast.makeText(MenuActivity.this, "Failed to load menu", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private android.net.Uri selectedImageUri;
    private android.widget.ImageView imgPreviewRef; // Temporary reference for dialog

    private final androidx.activity.result.ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(
            new androidx.activity.result.contract.ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    if (imgPreviewRef != null) {
                        imgPreviewRef.setImageURI(uri);
                    }
                }
            });

    private void showAddMenuItemDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_menu_item, null);
        EditText etName = dialogView.findViewById(R.id.etItemName);
        AutoCompleteTextView etCategory = dialogView.findViewById(R.id.etItemCategory);
        EditText etPrice = dialogView.findViewById(R.id.etItemPrice);
        EditText etDescription = dialogView.findViewById(R.id.etItemDescription);
        SwitchMaterial switchAvailable = dialogView.findViewById(R.id.switchAvailable);
        
        // Image Picker Setup
        com.google.android.material.button.MaterialButton btnPick = dialogView.findViewById(R.id.btnPickImage);
        android.widget.ImageView imgPreview = dialogView.findViewById(R.id.imgPreview);
        imgPreviewRef = imgPreview;
        selectedImageUri = null; // Reset
        
        btnPick.setOnClickListener(v -> pickImageLauncher.launch("image/*"));

        fetchCategories(etCategory);

        com.google.android.material.textfield.TextInputLayout layoutPrice = dialogView.findViewById(R.id.layoutPrice);
        if (layoutPrice != null) {
            layoutPrice.setHint("Price (" + com.posbillingapp.utils.CurrencyUtils.getCurrencySymbol() + ")");
        }

        new MaterialAlertDialogBuilder(this)
            .setTitle("Add Menu Item")
            .setView(dialogView)
            .setPositiveButton("Add", (dialog, which) -> {
                String name = etName.getText().toString().trim();
                String category = etCategory.getText().toString().trim();
                String priceStr = etPrice.getText().toString().trim();
                String description = etDescription.getText().toString().trim();
                boolean isAvailable = switchAvailable.isChecked();

                if (name.isEmpty() || category.isEmpty() || priceStr.isEmpty()) {
                    Toast.makeText(this, "Name, Category and Price are required", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Prepare Data
                java.util.Map<String, okhttp3.RequestBody> textFields = new java.util.HashMap<>();
                textFields.put("Name", createPartFromString(name));
                textFields.put("Category", createPartFromString(category));
                textFields.put("Price", createPartFromString(priceStr));
                textFields.put("Description", createPartFromString(description));
                textFields.put("IsAvailable", createPartFromString(String.valueOf(isAvailable)));
                textFields.put("CompanyId", createPartFromString(String.valueOf(companyId)));

                okhttp3.MultipartBody.Part imagePart = null;
                if (selectedImageUri != null) {
                    imagePart = prepareFilePart("imageFile", selectedImageUri);
                }

                addMenuItem(textFields, imagePart);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void showEditMenuItemDialog(MenuItemModel item) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_menu_item, null);
        EditText etName = dialogView.findViewById(R.id.etItemName);
        AutoCompleteTextView etCategory = dialogView.findViewById(R.id.etItemCategory);
        EditText etPrice = dialogView.findViewById(R.id.etItemPrice);
        EditText etDescription = dialogView.findViewById(R.id.etItemDescription);
        SwitchMaterial switchAvailable = dialogView.findViewById(R.id.switchAvailable);
        
        // Image Picker Setup
        com.google.android.material.button.MaterialButton btnPick = dialogView.findViewById(R.id.btnPickImage);
        android.widget.ImageView imgPreview = dialogView.findViewById(R.id.imgPreview);
        imgPreviewRef = imgPreview;
        selectedImageUri = null; // Reset

        etName.setText(item.getName());
        etCategory.setText(item.getCategory());
        fetchCategories(etCategory);
        
        com.google.android.material.textfield.TextInputLayout layoutPrice = dialogView.findViewById(R.id.layoutPrice);
        if (layoutPrice != null) {
            layoutPrice.setHint("Price (" + com.posbillingapp.utils.CurrencyUtils.getCurrencySymbol() + ")");
        }

        etPrice.setText(String.valueOf(item.getPrice()));
        etDescription.setText(item.getDescription());
        
        // Load existing image if available using Glide
        if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
            String fullUrl = item.getImageUrl().startsWith("http") ? item.getImageUrl() 
                             : com.posbillingapp.network.RetrofitClient.getBaseUrl() + item.getImageUrl().replaceAll("^/", "");
            com.bumptech.glide.Glide.with(this).load(fullUrl).into(imgPreview);
        }
        
        btnPick.setOnClickListener(v -> pickImageLauncher.launch("image/*"));
        
        switchAvailable.setChecked(item.isAvailable());

        new MaterialAlertDialogBuilder(this)
            .setTitle("Edit Menu Item")
            .setView(dialogView)
            .setPositiveButton("Update", (dialog, which) -> {
                String name = etName.getText().toString().trim();
                String category = etCategory.getText().toString().trim();
                String priceStr = etPrice.getText().toString().trim();
                String description = etDescription.getText().toString().trim();
                boolean isAvailable = switchAvailable.isChecked();

                 // Prepare Data
                java.util.Map<String, okhttp3.RequestBody> textFields = new java.util.HashMap<>();
                textFields.put("Name", createPartFromString(name));
                textFields.put("Category", createPartFromString(category));
                textFields.put("Price", createPartFromString(priceStr));
                textFields.put("Description", createPartFromString(description));
                textFields.put("IsAvailable", createPartFromString(String.valueOf(isAvailable)));
                textFields.put("CompanyId", createPartFromString(String.valueOf(companyId)));
                // We also send ImageUrl (old one) if no new one is selected, 
                // but our backend logic only updates if new file is sent or we explicitly send new URL.
                // We are sending file if selected. If not selected, backend keeps old.
                // However, to be safe, if we want to KEEP the old one, we don't need to send anything.
                // But the backend uses [FromForm], so `imageUrl` field will be null if not sent.
                // Backend logic: `if (!string.IsNullOrEmpty(request.ImageUrl))` -> update.
                // So if we don't send ImageUrl part, it will be null.
                if (item.getImageUrl() != null) {
                     textFields.put("ImageUrl", createPartFromString(item.getImageUrl()));
                }

                okhttp3.MultipartBody.Part imagePart = null;
                if (selectedImageUri != null) {
                    imagePart = prepareFilePart("imageFile", selectedImageUri);
                    // If sending new file, ImageUrl in request body is ignored/overwritten by backend logic usually
                    // Backend: `if (imageFile != null) request.ImageUrl = path;`
                }

                updateMenuItem(item.getId(), textFields, imagePart);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void addMenuItem(java.util.Map<String, okhttp3.RequestBody> textFields, okhttp3.MultipartBody.Part imagePart) {
        Call<Void> call;
        if (imagePart != null) {
            call = RetrofitClient.getApiService().addMenuItem(textFields, imagePart);
        } else {
            // Remove ImageUrl from map if it was added for safety (it shouldn't be for add, but just in case)
            textFields.remove("ImageUrl");
            call = RetrofitClient.getApiService().addMenuItemNoImage(textFields);
        }
        
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(MenuActivity.this, "Item Added", Toast.LENGTH_SHORT).show();
                    fetchMenu();
                } else {
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "Unknown Error";
                        Toast.makeText(MenuActivity.this, "Error adding item: " + response.code() + " " + errorBody, Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        Toast.makeText(MenuActivity.this, "Error adding item: " + response.code(), Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(MenuActivity.this, "Network Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
                t.printStackTrace();
            }
        });
    }

    private void updateMenuItem(long id, java.util.Map<String, okhttp3.RequestBody> textFields, okhttp3.MultipartBody.Part imagePart) {
        Call<Void> call;
        if (imagePart != null) {
            call = RetrofitClient.getApiService().updateMenuItem(id, textFields, imagePart);
        } else {
            // Remove ImageUrl from map if user didn't select new image, so backend keeps old one?
            // Wait, if no new image, we send ImageUrl (old one) in textFields.
            // Backend update logic: if Request.ImageUrl is not empty, update it.
            // So if we send old ImageUrl, it updates with same value. Safe.
            call = RetrofitClient.getApiService().updateMenuItemNoImage(id, textFields);
        }

        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(MenuActivity.this, "Item Updated", Toast.LENGTH_SHORT).show();
                    fetchMenu();
                } else {
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "Unknown Error";
                        Toast.makeText(MenuActivity.this, "Update Failed: " + response.code() + " " + errorBody, Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        Toast.makeText(MenuActivity.this, "Update Failed: " + response.code(), Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(MenuActivity.this, "Network Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
                t.printStackTrace();
            }
        });
    }

    // Helper methods for Multipart
    private okhttp3.RequestBody createPartFromString(String descriptionString) {
        if (descriptionString == null) descriptionString = "";
        return okhttp3.RequestBody.create(okhttp3.MultipartBody.FORM, descriptionString);
    }

    private okhttp3.MultipartBody.Part prepareFilePart(String partName, android.net.Uri fileUri) {
        try {
            java.io.InputStream inputStream = getContentResolver().openInputStream(fileUri);
            byte[] fileBytes = new byte[inputStream.available()];
            inputStream.read(fileBytes);
            inputStream.close();
            
            // Get file extension/type
            String type = getContentResolver().getType(fileUri);
            okhttp3.RequestBody requestFile = okhttp3.RequestBody.create(okhttp3.MediaType.parse(type), fileBytes);
            
            return okhttp3.MultipartBody.Part.createFormData(partName, "upload.jpg", requestFile);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void deleteMenuItem(long id) {
        RetrofitClient.getApiService().deleteMenuItem(id).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(MenuActivity.this, "Item Deleted", Toast.LENGTH_SHORT).show();
                    fetchMenu();
                } else {
                    Toast.makeText(MenuActivity.this, "Delete Failed", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(MenuActivity.this, "Network Error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchCategories(AutoCompleteTextView autoCompleteTextView) {
        RetrofitClient.getApiService().getCategories().enqueue(new Callback<List<String>>() {
            @Override
            public void onResponse(Call<List<String>> call, Response<List<String>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                        MenuActivity.this, 
                        android.R.layout.simple_dropdown_item_1line, 
                        response.body()
                    );
                    autoCompleteTextView.setAdapter(adapter);
                }
            }

            @Override
            public void onFailure(Call<List<String>> call, Throwable t) {
                // Fail silently, user can still type
            }
        });
    }
}
