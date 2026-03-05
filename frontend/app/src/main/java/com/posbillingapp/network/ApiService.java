package com.posbillingapp.network;

import com.posbillingapp.models.AuthModels.*;
import com.posbillingapp.models.EmployeeModel;
import com.posbillingapp.models.EmployeeModel.AddEmployeeRequest;
import com.posbillingapp.models.MenuItemModel;
import com.posbillingapp.models.MenuItemModel.AddMenuItemRequest;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.*;

public interface ApiService {
    @POST("api/auth/register")
    Call<AuthResponse> register(@Body RegisterRequest request);

    @POST("api/auth/login")
    Call<AuthResponse> login(@Body LoginRequest request);

    @POST("api/auth/forgot-password")
    Call<AuthResponse> forgotPassword(@Body ForgotPasswordRequest request);

    @POST("api/auth/reset-password")
    Call<AuthResponse> resetPassword(@Body ResetPasswordRequest request);

    @POST("api/auth/refresh")
    Call<AuthResponse> refresh(@Body RefreshRequest request);

    // Metadata
    @GET("api/metadata/countries")
    Call<List<com.posbillingapp.models.LocationModels.Country>> getCountries();

    @GET("api/metadata/states/{countryId}")
    Call<List<com.posbillingapp.models.LocationModels.State>> getStates(@Path("countryId") int countryId);

    @GET("api/metadata/cities/{stateId}")
    Call<List<com.posbillingapp.models.LocationModels.City>> getCities(@Path("stateId") int stateId);

    // Employee Management
    @GET("api/company/employees/{companyId}")
    Call<List<EmployeeModel>> getEmployees(@Path("companyId") long companyId);

    @POST("api/company/employees")
    Call<com.posbillingapp.models.AuthModels.AuthResponse> addEmployee(@Body AddEmployeeRequest request);

    @PUT("api/company/employees/{id}")
    Call<Void> updateEmployee(@Path("id") long id, @Body EmployeeModel employee);

    @DELETE("api/company/employees/{id}")
    Call<Void> deleteEmployee(@Path("id") long id);

    // Menu Management
    @GET("api/company/categories")
    Call<List<String>> getCategories();

    @GET("api/company/menu/{companyId}")
    Call<List<MenuItemModel>> getMenu(@Path("companyId") long companyId);

    @Multipart
    @POST("api/company/menu")
    Call<Void> addMenuItem(@PartMap java.util.Map<String, okhttp3.RequestBody> textFields, @Part okhttp3.MultipartBody.Part image);

    @Multipart
    @POST("api/company/menu")
    Call<Void> addMenuItemNoImage(@PartMap java.util.Map<String, okhttp3.RequestBody> textFields);

    @Multipart
    @PUT("api/company/menu/{id}")
    Call<Void> updateMenuItem(@Path("id") long id, @PartMap java.util.Map<String, okhttp3.RequestBody> textFields, @Part okhttp3.MultipartBody.Part image);

    @Multipart
    @PUT("api/company/menu/{id}")
    Call<Void> updateMenuItemNoImage(@Path("id") long id, @PartMap java.util.Map<String, okhttp3.RequestBody> textFields);

    @DELETE("api/company/menu/{id}")
    Call<Void> deleteMenuItem(@Path("id") long id);

    @GET("api/company/stats/{companyId}")
    Call<com.posbillingapp.models.DashboardStats> getDashboardStats(@Path("companyId") long companyId);

    // Billing
    @GET("api/billing/next-number/{companyId}")
    Call<com.posbillingapp.models.AuthModels.AuthResponse> getNextBillNumber(@Path("companyId") long companyId);

    @POST("api/billing/order")
    Call<com.posbillingapp.models.BillingModels.OrderResponse> createOrder(@Body com.posbillingapp.models.BillingModels.OrderRequest request);

    // Employee Stats
    @GET("api/company/employees/{userId}/stats")
    Call<com.posbillingapp.models.EmployeeStats> getEmployeeStats(@Path("userId") long userId);

    // Menu Assignment
    @GET("api/company/employees/{userId}/menu-access")
    Call<List<Long>> getAssignedMenuItems(@Path("userId") long userId);

    @GET("api/company/employees/{userId}/assigned-menu")
    Call<List<MenuItemModel>> getAssignedMenuDetails(@Path("userId") long userId);

    @POST("api/company/employees/menu-access")
    Call<Void> updateMenuAssignments(@Body com.posbillingapp.ui.AssignMenuActivity.MenuAssignmentRequest request);

    // Permission Management
    @GET("api/metadata/roles")
    Call<List<com.posbillingapp.models.PermissionModels.Role>> getRoles();

    @GET("api/permissions/all")
    Call<List<com.posbillingapp.models.PermissionModels.Permission>> getAllPermissions();

    @GET("api/permissions/role/{roleId}/{companyId}")
    Call<List<String>> getRolePermissions(@Path("roleId") int roleId, @Path("companyId") long companyId);

    @POST("api/permissions/role")
    Call<com.posbillingapp.models.AuthModels.AuthResponse> updateRolePermissions(@Body com.posbillingapp.models.PermissionModels.UpdateRolePermissionsRequest request);

    // Error Logging
    @POST("api/errorlogs")
    Call<com.posbillingapp.models.AuthModels.AuthResponse> logError(@Body com.posbillingapp.models.ErrorLogRequest request);

    // Settlements - Bank
    @GET("api/Settings/bank/{companyId}")
    Call<List<com.posbillingapp.models.SettlementModels.BankDetails>> getBankDetails(@Path("companyId") long companyId);

    @POST("api/Settings/bank/{companyId}")
    Call<com.posbillingapp.models.SettlementModels.ValidationResult> addBankDetails(@Path("companyId") long companyId, @Body com.posbillingapp.models.SettlementModels.BankDetails bank);

    @POST("api/Settings/bank/primary/{accountId}")
    Call<Void> setPrimaryBank(@Path("accountId") long accountId);

    @DELETE("api/Settings/bank/{accountId}")
    Call<Void> deleteBank(@Path("accountId") long accountId);

    // Settlements - Card
    @GET("api/Settings/card/{companyId}")
    Call<List<com.posbillingapp.models.SettlementModels.CardDetails>> getCardDetails(@Path("companyId") long companyId);

    @POST("api/Settings/card/{companyId}")
    Call<com.posbillingapp.models.SettlementModels.CardDetails> addCardDetails(@Path("companyId") long companyId, @Body com.posbillingapp.models.SettlementModels.CardDetails card);

    @POST("api/Settings/card/primary/{cardId}")
    Call<Void> setPrimaryCard(@Path("cardId") long cardId);

    @DELETE("api/Settings/card/{cardId}")
    Call<Void> deleteCard(@Path("cardId") long cardId);

    // Billing History
    @GET("api/billing/history/{companyId}")
    Call<List<com.posbillingapp.models.BillingModels.OrderHistoryResponse>> getOrderHistory(@Path("companyId") long companyId);
}
