import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import { login as loginApi } from '../../lib/api';

const initialState = {
    user: JSON.parse(localStorage.getItem('user')) || null,
    token: localStorage.getItem('token') || null,
    refreshToken: localStorage.getItem('refreshToken') || null,
    role: localStorage.getItem('role') || null,
    roleId: localStorage.getItem('roleId') || null,
    permissions: JSON.parse(localStorage.getItem('permissions')) || [],
    loading: false,
    error: null,
};

// Async thunk for login
export const loginUser = createAsyncThunk(
    'auth/loginUser',
    async ({ email, password }, { rejectWithValue }) => {
        try {
            const response = await loginApi(email, password);
            if (response.success) {
                if (response.roleId === 2) {
                    return rejectWithValue("Access Denied: Your account does not have permission to access the website.");
                }
                localStorage.setItem('token', response.token);
                localStorage.setItem('refreshToken', response.refreshToken);
                localStorage.setItem('role', response.role);
                localStorage.setItem('roleId', response.roleId);
                localStorage.setItem('permissions', JSON.stringify(response.permissions || []));
                localStorage.setItem('user', JSON.stringify(response));
                return response;
            } else {
                return rejectWithValue(response.message || 'Login failed');
            }
        } catch (error) {
            return rejectWithValue(error.message || 'An error occurred during login');
        }
    }
);

const authSlice = createSlice({
    name: 'auth',
    initialState,
    reducers: {
        logout: (state) => {
            state.user = null;
            state.token = null;
            state.refreshToken = null;
            state.role = null;
            state.roleId = null;
            state.permissions = [];
            state.error = null;
            localStorage.removeItem('user');
            localStorage.removeItem('token');
            localStorage.removeItem('refreshToken');
            localStorage.removeItem('role');
            localStorage.removeItem('roleId');
            localStorage.removeItem('permissions');
        },
        clearError: (state) => {
            state.error = null;
        },
        updateUser: (state, action) => {
            state.user = { ...state.user, ...action.payload };
            localStorage.setItem('user', JSON.stringify(state.user));
        }
    },
    extraReducers: (builder) => {
        builder
            .addCase(loginUser.pending, (state) => {
                state.loading = true;
                state.error = null;
            })
            .addCase(loginUser.fulfilled, (state, action) => {
                state.loading = false;
                state.token = action.payload.token;
                state.refreshToken = action.payload.refreshToken;
                state.role = action.payload.role;
                state.roleId = action.payload.roleId;
                state.permissions = action.payload.permissions || [];
                state.user = action.payload;
                state.error = null;
            })
            .addCase(loginUser.rejected, (state, action) => {
                state.loading = false;
                state.error = action.payload;
            });
    },
});

export const { logout, clearError, updateUser } = authSlice.actions;
export default authSlice.reducer;
