import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import { useSelector } from "react-redux";
import Login from "@/pages/Login";
import ForgotPassword from "@/pages/ForgotPassword";
import AdminDashboard from "@/pages/admin/Dashboard";
import Employees from "@/pages/admin/Employees";
import Products from "@/pages/admin/Products";
import Billing from "@/pages/admin/Billing";
import Reports from "@/pages/admin/Reports";
import Permissions from "@/pages/admin/Permissions";
import Settings from "@/pages/admin/Settings";
import Calculator from "@/pages/admin/Calculator";
import UserDashboard from "@/pages/user/Dashboard";
import DashboardLayout from "@/components/DashboardLayout";
import Register from "@/pages/Register";
import "./App.css";

import PermissionGuard from "@/components/auth/PermissionGuard";

// Protected Route Component
const ProtectedRoute = ({ children, allowedRoles }) => {
  const { user, token } = useSelector((state) => state.auth);

  if (!token) {
    return <Navigate to="/" replace />;
  }

  if (allowedRoles && !allowedRoles.includes(user?.role)) {
    // Redirect based on role if trying to access unauthorized page
    if (user?.role === "Admin") return <Navigate to="/admin" replace />;
    return <Navigate to="/user" replace />;
  }

  return children;
};

// Login Route Wrapper to redirect if already logged in
const LoginRoute = () => {
  const { user, token } = useSelector((state) => state.auth);
  if (token && user) {
    if (user.role === "Admin") return <Navigate to="/admin" replace />;
    return <Navigate to="/user" replace />;
  }
  return <Login />;
}

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<LoginRoute />} />
        <Route path="/register" element={<Register />} />
        <Route path="/forgot-password" element={<ForgotPassword />} />

        {/* Protected Layout Route */}
        <Route element={<ProtectedRoute allowedRoles={["Admin", "User", "Employee", "Full Access", "Limited Access"]}><DashboardLayout /></ProtectedRoute>}>

          {/* Admin and Feature Routes */}
          <Route path="/admin" element={<AdminDashboard />} />
          <Route path="/admin/employees" element={<PermissionGuard permission="VIEW_EMPLOYEES"><Employees /></PermissionGuard>} />
          <Route path="/admin/products" element={<PermissionGuard permission="VIEW_PRODUCTS"><Products /></PermissionGuard>} />
          <Route path="/admin/billing" element={<PermissionGuard permission="VIEW_BILLING"><Billing /></PermissionGuard>} />
          <Route path="/admin/calculator" element={<PermissionGuard permission="POS_CALCULATOR"><Calculator /></PermissionGuard>} />
          <Route path="/admin/reports" element={<PermissionGuard permission="VIEW_REPORTS"><Reports /></PermissionGuard>} />
          <Route path="/admin/permissions" element={<PermissionGuard permission="MANAGE_PERMISSIONS"><Permissions /></PermissionGuard>} />
          <Route path="/admin/settings" element={<PermissionGuard permission="VIEW_SETTINGS"><Settings /></PermissionGuard>} />

          {/* User Routes */}
          <Route path="/user" element={<UserDashboard />} />
          <Route path="/user/billing" element={<PermissionGuard permission="VIEW_BILLING"><Billing /></PermissionGuard>} />

        </Route>

        {/* Catch all redirect */}
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
