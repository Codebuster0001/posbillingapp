const getAuthHeaders = () => {
    const token = localStorage.getItem('token');
    return {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
    };
};

const refreshToken = async () => {
    try {
        const rt = localStorage.getItem('refreshToken');
        if (!rt) throw new Error("No refresh token");

        const response = await fetch('/api/Auth/refresh', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ refreshToken: rt }),
        });

        if (!response.ok) throw new Error("Refresh failed");

        const data = await response.json();
        if (data.token) {
            localStorage.setItem('token', data.token);
            return data.token;
        }
        throw new Error("No token returned");
    } catch (error) {
        console.error("Token refresh error:", error);
        localStorage.clear(); // Clear all if refresh fails
        window.location.href = '/login';
        throw error;
    }
};

const authenticatedFetch = async (url, options = {}) => {
    let response = await fetch(url, options);

    if (response.status === 401) {
        try {
            const newToken = await refreshToken();
            const retryHeaders = {
                ...options.headers,
                'Authorization': `Bearer ${newToken}`
            };
            response = await fetch(url, { ...options, headers: retryHeaders });
        } catch (e) {
            throw new Error("Session expired. Please login again.");
        }
    }

    // 403 Forbidden
    if (response.status === 403) {
        // Handle generic forbidden if needed
    }

    return response;
};

export const login = async (email, password) => {
    try {
        const response = await fetch('/api/Auth/login', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({ email, password }),
        });

        if (!response.ok) {
            const errorText = await response.text();
            let errorMessage = "Login failed";
            try {
                const errorJson = JSON.parse(errorText);
                errorMessage = errorJson.message || errorMessage;
            } catch (e) {
                // If text is not json, use it directly if short, or generic
                errorMessage = errorText.length < 100 ? errorText : errorMessage;
            }
            throw new Error(errorMessage);
        }
        return await response.json();
    } catch (error) {
        throw error;
    }
};

export const getEmployees = async (companyId) => {
    try {
        const response = await authenticatedFetch(`/api/Company/employees/${companyId}`, {
            method: 'GET',
            headers: getAuthHeaders(),
        });

        if (!response.ok) throw new Error("Failed to fetch employees");
        return await response.json();
    } catch (error) {
        console.error("Error fetching employees:", error);
        throw error;
    }
};

export const getRoles = async () => {
    try {
        const response = await authenticatedFetch('/api/Metadata/roles', {
            method: 'GET',
            headers: getAuthHeaders(),
        });
        if (!response.ok) throw new Error("Failed to fetch roles");
        return await response.json();
    } catch (error) {
        console.error("Error fetching roles:", error);
        throw error;
    }
};

export const createEmployee = async (employeeData) => {
    try {
        const response = await authenticatedFetch('/api/Company/employees', {
            method: 'POST',
            headers: getAuthHeaders(),
            body: JSON.stringify(employeeData),
        });

        if (!response.ok) {
            const errorData = await response.json();
            throw new Error(errorData.message || "Failed to create employee");
        }
        return await response.json();
    } catch (error) {
        console.error("Error creating employee:", error);
        throw error;
    }
};

export const getCategories = async () => {
    try {
        const response = await authenticatedFetch('/api/Company/categories', {
            method: 'GET',
            headers: getAuthHeaders(),
        });
        if (!response.ok) throw new Error("Failed to fetch categories");
        return await response.json();
    } catch (error) {
        console.error("Error fetching categories:", error);
        throw error;
    }
};

export const getProducts = async (companyId) => {
    try {
        const response = await authenticatedFetch(`/api/Company/menu/${companyId}`, {
            method: 'GET',
            headers: getAuthHeaders(),
        });
        if (!response.ok) throw new Error("Failed to fetch products");
        return await response.json();
    } catch (error) {
        console.error("Error fetching products:", error);
        throw error;
    }
};

export const createProduct = async (formData) => {
    try {
        const token = localStorage.getItem('token');
        const response = await authenticatedFetch('/api/Company/menu', {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${token}`
                // Note: Don't set Content-Type for FormData, browser will do it with boundary
            },
            body: formData,
        });

        if (!response.ok) {
            const errorData = await response.json();
            throw new Error(errorData.message || "Failed to create product");
        }
        return await response.json();
    } catch (error) {
        console.error("Error creating product:", error);
        throw error;
    }
};

export const updateProduct = async (id, formData) => {
    try {
        const token = localStorage.getItem('token');
        const response = await authenticatedFetch(`/api/Company/menu/${id}`, {
            method: 'PUT',
            headers: {
                'Authorization': `Bearer ${token}`
            },
            body: formData,
        });

        if (!response.ok) {
            const errorData = await response.json();
            throw new Error(errorData.message || "Failed to update product");
        }
        return await response.json();
    } catch (error) {
        console.error("Error updating product:", error);
        throw error;
    }
};

export const deleteProduct = async (id) => {
    try {
        const response = await authenticatedFetch(`/api/Company/menu/${id}`, {
            method: 'DELETE',
            headers: getAuthHeaders(),
        });

        if (!response.ok) {
            const errorData = await response.json();
            throw new Error(errorData.message || "Failed to delete product");
        }
        return await response.json();
    } catch (error) {
        console.error("Error deleting product:", error);
        throw error;
    }
};

export const updateEmployee = async (id, employeeData) => {
    try {
        const response = await authenticatedFetch(`/api/Company/employees/${id}`, {
            method: 'PUT',
            headers: getAuthHeaders(),
            body: JSON.stringify(employeeData),
        });

        if (!response.ok) {
            const errorData = await response.json();
            throw new Error(errorData.message || "Failed to update employee");
        }
        return await response.json();
    } catch (error) {
        console.error("Error updating employee:", error);
        throw error;
    }
};

export const deleteEmployee = async (id) => {
    try {
        const response = await authenticatedFetch(`/api/Company/employees/${id}`, {
            method: 'DELETE',
            headers: getAuthHeaders(),
        });

        if (!response.ok) {
            const errorData = await response.json();
            throw new Error(errorData.message || "Failed to delete employee");
        }
        return await response.json();
    } catch (error) {
        console.error("Error deleting employee:", error);
        throw error;
    }
};

export const getDashboardStats = async (companyId, from = null, to = null) => {
    try {
        let url = `/api/Company/stats/${companyId}`;
        const params = new URLSearchParams();
        if (from) params.append('from', from);
        if (to) params.append('to', to);
        if (params.toString()) url += `?${params.toString()}`;

        const response = await authenticatedFetch(url, {
            method: 'GET',
            headers: getAuthHeaders(),
        });
        if (!response.ok) throw new Error("Failed to fetch dashboard stats");
        return await response.json();
    } catch (error) {
        console.error("Error fetching dashboard stats:", error);
        throw error;
    }
};

export const getOrderHistory = async (companyId, from = null, to = null) => {
    try {
        let url = `/api/Billing/history/${companyId}`;
        const params = new URLSearchParams();
        if (from) params.append('from', from);
        if (to) params.append('to', to);
        if (params.toString()) url += `?${params.toString()}`;

        const response = await authenticatedFetch(url, {
            method: 'GET',
            headers: getAuthHeaders(),
        });
        if (!response.ok) throw new Error("Failed to fetch order history");
        return await response.json();
    } catch (error) {
        console.error("Error fetching order history:", error);
        throw error;
    }
};

export const getAssignedMenuItems = async (userId) => {
    try {
        const response = await authenticatedFetch(`/api/Company/employees/${userId}/menu-access`, {
            method: 'GET',
            headers: getAuthHeaders(),
        });
        if (!response.ok) throw new Error("Failed to fetch assigned menu items");
        return await response.json();
    } catch (error) {
        console.error("Error fetching assigned menu items:", error);
        throw error;
    }
};

export const updateMenuAssignments = async (userId, menuItemIds) => {
    try {
        const response = await authenticatedFetch('/api/Company/employees/menu-access', {
            method: 'POST',
            headers: getAuthHeaders(),
            body: JSON.stringify({ userId, menuItemIds }),
        });
        if (!response.ok) throw new Error("Failed to update menu assignments");
        return await response.json();
    } catch (error) {
        console.error("Error updating menu assignments:", error);
        throw error;
    }
};

export const getAssignedMenuDetails = async (userId) => {
    try {
        const response = await authenticatedFetch(`/api/Company/employees/${userId}/assigned-menu`, {
            method: 'GET',
            headers: getAuthHeaders(),
        });
        if (!response.ok) throw new Error("Failed to fetch assigned menu details");
        return await response.json();
    } catch (error) {
        console.error("Error fetching assigned menu details:", error);
        throw error;
    }
};

export const createOrder = async (orderData) => {
    try {
        const response = await authenticatedFetch('/api/Billing/order', {
            method: 'POST',
            headers: getAuthHeaders(),
            body: JSON.stringify(orderData),
        });
        if (!response.ok) {
            const errorData = await response.json();
            throw new Error(errorData.message || "Failed to create order");
        }
        return await response.json();
    } catch (error) {
        console.error("Error creating order:", error);
        throw error;
    }
};
export const forgotPassword = async (email) => {
    try {
        const response = await fetch('/api/Auth/forgot-password', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email }),
        });
        const data = await response.json();
        if (!response.ok) throw new Error(data.message || "Failed to send OTP");
        return data;
    } catch (error) {
        throw error;
    }
};

export const resetPassword = async (email, otpCode, newPassword) => {
    try {
        const response = await fetch('/api/Auth/reset-password', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email, otpCode, newPassword }),
        });
        const data = await response.json();
        if (!response.ok) throw new Error(data.message || "Failed to reset password");
        return data;
    } catch (error) {
        throw error;
    }
};

export const getAllPermissions = async () => {
    try {
        const response = await authenticatedFetch('/api/Permissions/all', {
            method: 'GET',
            headers: getAuthHeaders(),
        });
        if (!response.ok) throw new Error("Failed to fetch all permissions");
        return await response.json();
    } catch (error) {
        console.error("Error fetching all permissions:", error);
        throw error;
    }
};

export const getRolePermissions = async (roleId, companyId) => {
    try {
        const response = await authenticatedFetch(`/api/Permissions/role/${roleId}/${companyId}`, {
            method: 'GET',
            headers: getAuthHeaders(),
        });
        if (!response.ok) throw new Error("Failed to fetch role permissions");
        return await response.json();
    } catch (error) {
        console.error("Error fetching role permissions:", error);
        throw error;
    }
};

export const updateRolePermissions = async (roleId, companyId, permissionKeys) => {
    try {
        const response = await authenticatedFetch('/api/Permissions/role', {
            method: 'POST',
            headers: getAuthHeaders(),
            body: JSON.stringify({ roleId, companyId, permissionKeys }),
        });

        if (!response.ok) {
            const errorData = await response.json();
            throw new Error(errorData.message || "Failed to update role permissions");
        }
        return await response.json();
    } catch (error) {
        console.error("Error updating role permissions:", error);
        throw error;
    }
};

// Settings & Payment Modes
export const getCompanyProfile = async (companyId) => {
    const response = await authenticatedFetch(`/api/Settings/profile/${companyId}`, {
        method: 'GET',
        headers: getAuthHeaders(),
    });
    if (!response.ok) throw new Error("Failed to fetch profile");
    return await response.json();
};

export const updateCompanyProfile = async (companyId, profileData) => {
    const response = await authenticatedFetch(`/api/Settings/profile/${companyId}`, {
        method: 'PUT',
        headers: getAuthHeaders(),
        body: JSON.stringify(profileData),
    });
    if (!response.ok) throw new Error("Failed to update profile");
    return await response.json();
};

export const uploadLogo = async (companyId, formData) => {
    const token = localStorage.getItem('token');
    const response = await authenticatedFetch(`/api/Settings/logo/${companyId}`, {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${token}` },
        body: formData,
    });
    if (!response.ok) throw new Error("Failed to upload logo");
    return await response.json();
};

export const getBankDetails = async (companyId) => {
    const response = await authenticatedFetch(`/api/Settings/bank/${companyId}`, {
        method: 'GET',
        headers: getAuthHeaders(),
    });
    if (!response.ok) throw new Error("Failed to fetch bank details");
    return await response.json();
};

export const updateBankDetails = async (companyId, bankData) => {
    const response = await authenticatedFetch(`/api/Settings/bank/${companyId}`, {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify(bankData),
    });
    if (!response.ok) throw new Error("Failed to update bank details");
    return await response.json();
};

export const getPaymentModes = async () => {
    const response = await authenticatedFetch('/api/Settings/payment-modes', {
        method: 'GET',
        headers: getAuthHeaders(),
    });
    if (!response.ok) throw new Error("Failed to fetch payment modes");
    return await response.json();
};

// Export a default object for object-style usage: api.get(), api.post(), etc.
const api = {
    get: async (url) => {
        console.log(`[API GET] Requesting: ${url}`);
        const res = await authenticatedFetch(url, { method: 'GET', headers: getAuthHeaders() });
        let data = {};
        try {
            const text = await res.text();
            console.log(`[API GET] Response from ${url}: Status ${res.status}, Body length: ${text.length}`);
            data = text ? JSON.parse(text) : {};
        } catch (e) {
            console.error(`API Get Parse Error for URL: ${url}`, e);
        }
        return { data, status: res.status, ok: res.ok };
    },
    post: async (url, body) => {
        const isFormData = body instanceof FormData;
        const headers = isFormData ? { 'Authorization': `Bearer ${localStorage.getItem('token')}` } : getAuthHeaders();
        const res = await authenticatedFetch(url, {
            method: 'POST',
            headers,
            body: isFormData ? body : JSON.stringify(body)
        });
        let data = {};
        try {
            const text = await res.text();
            data = text ? JSON.parse(text) : {};
        } catch (e) {
            console.error("API Post Parse Error for URL:", url, e);
        }
        return { data, status: res.status, ok: res.ok };
    },
    put: async (url, body) => {
        const res = await authenticatedFetch(url, {
            method: 'PUT',
            headers: getAuthHeaders(),
            body: JSON.stringify(body)
        });
        let data = {};
        try {
            const text = await res.text();
            data = text ? JSON.parse(text) : {};
        } catch (e) {
            console.error("API Put Parse Error for URL:", url, e);
        }
        return { data, status: res.status, ok: res.ok };
    },
    delete: async (url) => {
        const res = await authenticatedFetch(url, { method: 'DELETE', headers: getAuthHeaders() });
        let data = {};
        try {
            const text = await res.text();
            data = text ? JSON.parse(text) : {};
        } catch (e) {
            console.error("API Delete Parse Error for URL:", url, e);
        }
        return { data, status: res.status, ok: res.ok };
    },
    defaults: {
        baseURL: window.location.origin
    }
};

export default api;


