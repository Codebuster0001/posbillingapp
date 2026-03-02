
import { useSelector, useDispatch } from "react-redux";
import { Link, useNavigate } from "react-router-dom";
import { logout } from "@/features/auth/authSlice";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import { LogOut, Home, Users, ShoppingCart, Settings, Package, TrendingUp, ShieldCheck } from "lucide-react";

export default function Sidebar({ className, onClick }) {
    const { user } = useSelector((state) => state.auth);
    const dispatch = useDispatch();
    const navigate = useNavigate();

    const handleLogout = () => {
        dispatch(logout());
        navigate("/");
    };

    const navConfig = [
        { title: "Dashboard", href: "/admin", icon: Home, adminOnly: true },
        { title: "Dashboard", href: "/user", icon: Home, userOnly: true },
        { title: "Billing", href: "/admin/billing", icon: ShoppingCart, permission: "VIEW_BILLING", adminOnly: true },
        { title: "Billing", href: "/user/billing", icon: ShoppingCart, permission: "VIEW_BILLING", userOnly: true },
        { title: "Employees", href: "/admin/employees", icon: Users, permission: "VIEW_EMPLOYEES", adminOnly: true },
        { title: "Products", href: "/admin/products", icon: Package, permission: "VIEW_PRODUCTS", adminOnly: true },
        { title: "Reports", href: "/admin/reports", icon: TrendingUp, permission: "VIEW_REPORTS", adminOnly: true },
        { title: "Access Control", href: "/admin/permissions", icon: ShieldCheck, adminOnly: true },
        { title: "Settings", href: "/admin/settings", icon: Settings, adminOnly: true },
    ];

    const permissions = useSelector((state) => state.auth.permissions) || [];
    const isAdmin = user?.role === "Admin";
    const isFullAccess = user?.role === "Full Access";
    const hasManagementAccess = isAdmin || isFullAccess;

    const filteredNavItems = navConfig.filter(item => {
        // Special case for Dashboard & Billing: Managers use User pages
        if (item.title === "Dashboard" || item.title === "Billing") {
            if (isAdmin) return item.adminOnly === true;
            return item.userOnly === true;
        }

        // 1. Role/Path Check
        if (item.adminOnly && !hasManagementAccess) return false;
        if (item.userOnly && hasManagementAccess) return false;

        // 2. Permission Check
        if (isAdmin) return true;
        if (item.title === "Dashboard") return true; // Everyone sees their home dashboard
        if (item.title === "Access Control") return false; // Strict Admin only
        if (item.permission && !permissions.includes(item.permission)) return false;

        return true;
    });

    return (
        <div className={cn("pb-12 border-r bg-gray-100/40 relative flex flex-col h-full", className)}>
            <div className="space-y-4 py-4 flex-1">
                <div className="px-4 py-2">
                    <div className="flex items-center gap-2 mb-2">
                        {user?.companyLogo ? (
                            <img src={user.companyLogo} alt="Logo" className="w-10 h-10 rounded-full object-cover" />
                        ) : (
                            <div className="w-10 h-10 rounded-full bg-primary flex items-center justify-center text-primary-foreground font-bold text-lg">
                                {user?.companyName?.charAt(0) || "C"}
                            </div>
                        )}
                        <div className="flex flex-col">
                            <h2 className="text-lg font-semibold tracking-tight">
                                {user?.companyName || "Company Name"}
                            </h2>
                            <span className="text-xs text-muted-foreground capitalize">{user?.role}</span>
                        </div>
                    </div>
                </div>
                <div className="px-3 py-2">
                    <div className="space-y-1">
                        {filteredNavItems.map((item) => (
                            <Link key={item.href} to={item.href} onClick={onClick}>
                                <Button variant="ghost" className="w-full justify-start">
                                    <item.icon className="mr-2 h-4 w-4" />
                                    {item.title}
                                </Button>
                            </Link>
                        ))}
                    </div>
                </div>
            </div>

            <div className="p-4 border-t">
                <Button variant="outline" className="w-full justify-start text-red-500 hover:text-red-600 hover:bg-red-50" onClick={handleLogout}>
                    <LogOut className="mr-2 h-4 w-4" />
                    Logout
                </Button>
            </div>
        </div>
    );
}
