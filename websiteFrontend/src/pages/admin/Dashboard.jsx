import { useState, useEffect } from "react";
import { useSelector } from "react-redux";
import { Link } from "react-router-dom";
import { getDashboardStats, getOrderHistory } from "@/lib/api";
import {
    Users,
    Package,
    TrendingUp,
    DollarSign,
    CreditCard,
    Activity,
    Receipt,
    Filter,
    Calendar,
    ArrowRight,
    Zap,
    ShieldCheck
} from "lucide-react";

import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from "@/components/ui/select";

export default function AdminDashboard() {
    const { user } = useSelector((state) => state.auth);
    const [timeRange, setTimeRange] = useState("today");
    const [stats, setStats] = useState({
        employeeCount: 0,
        totalCollection: 0,
        receiptCount: 0
    });
    const [recentSales, setRecentSales] = useState([]);
    const [loading, setLoading] = useState(true);

    const formatDate = (date, includeTime = true) => {
        if (!date) return null;
        const d = new Date(date);
        const year = d.getFullYear();
        const month = String(d.getMonth() + 1).padStart(2, '0');
        const day = String(d.getDate()).padStart(2, '0');
        if (!includeTime) return `${year}-${month}-${day}`;

        const hour = String(d.getHours()).padStart(2, '0');
        const min = String(d.getMinutes()).padStart(2, '0');
        const sec = String(d.getSeconds()).padStart(2, '0');
        return `${year}-${month}-${day} ${hour}:${min}:${sec}`;
    };

    const getDates = (range) => {
        const now = new Date();

        switch (range) {
            case "today": {
                const startOfToday = new Date(now.getFullYear(), now.getMonth(), now.getDate(), 0, 0, 0);
                const endOfToday = new Date(now.getFullYear(), now.getMonth(), now.getDate(), 23, 59, 59);
                return { from: formatDate(startOfToday), to: formatDate(endOfToday) };
            }
            case "yesterday": {
                const startOfYesterday = new Date(now.getFullYear(), now.getMonth(), now.getDate() - 1, 0, 0, 0);
                const endOfYesterday = new Date(now.getFullYear(), now.getMonth(), now.getDate() - 1, 23, 59, 59);
                return { from: formatDate(startOfYesterday), to: formatDate(endOfYesterday) };
            }
            case "week": {
                const weekAgo = new Date(now.getFullYear(), now.getMonth(), now.getDate() - 7, 0, 0, 0);
                const endOfToday = new Date(now.getFullYear(), now.getMonth(), now.getDate(), 23, 59, 59);
                return { from: formatDate(weekAgo), to: formatDate(endOfToday) };
            }
            case "this_month": {
                const firstDay = new Date(now.getFullYear(), now.getMonth(), 1, 0, 0, 0);
                const lastDay = new Date(now.getFullYear(), now.getMonth() + 1, 0, 23, 59, 59);
                return { from: formatDate(firstDay), to: formatDate(lastDay) };
            }
            case "month": {
                const monthAgo = new Date(now.getFullYear(), now.getMonth(), now.getDate() - 30, 0, 0, 0);
                const endOfToday = new Date(now.getFullYear(), now.getMonth(), now.getDate(), 23, 59, 59);
                return { from: formatDate(monthAgo), to: formatDate(endOfToday) };
            }
            default:
                return { from: null, to: null };
        }
    };

    const fetchData = async () => {
        if (!user?.companyId) return;
        try {
            setLoading(true);
            const { from, to } = getDates(timeRange);

            // Use allSettled so one failure doesn't block the other
            const [statsRes, ordersRes] = await Promise.allSettled([
                getDashboardStats(user.companyId, from, to),
                getOrderHistory(user.companyId, from, to)
            ]);

            if (statsRes.status === 'fulfilled' && statsRes.value.success) {
                const data = statsRes.value;
                setStats({
                    employeeCount: data.employeeCount,
                    totalCollection: data.totalCollection,
                    receiptCount: data.receiptCount
                });
            }

            if (ordersRes.status === 'fulfilled') {
                setRecentSales(ordersRes.value || []);
            }
        } catch (err) {
            console.error("Dashboard fetch error:", err);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        if (user?.companyId) {
            fetchData();
        }
    }, [user?.companyId, timeRange]);

    const formatCurrency = (amount) => {
        return new Intl.NumberFormat('en-IN', {
            style: 'currency',
            currency: user?.currencySymbol === '₹' ? 'INR' : 'USD',
            maximumFractionDigits: 2
        }).format(amount).replace('INR', '₹');
    };

    const getRangeLabel = () => {
        switch (timeRange) {
            case "today": return "Today";
            case "yesterday": return "Yesterday";
            case "week": return "Last 7 Days";
            case "this_month": return "This Month";
            case "month": return "Last 30 Days";
            default: return "Lifetime";
        }
    };

    return (
        <div className="space-y-6">
            <header className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
                <div>
                    <h1 className="text-3xl font-bold tracking-tight">Dashboard Overview</h1>
                    <p className="text-muted-foreground text-sm">Welcome back, {user?.companyName || "Admin"}.</p>
                </div>
                <div className="flex flex-col sm:flex-row items-center gap-3 w-full md:w-auto">
                    <div className="flex items-center gap-2 bg-white px-3 py-1.5 rounded-md border shadow-sm w-full sm:w-auto">
                        <Calendar size={16} className="text-muted-foreground" />
                        <Select value={timeRange} onValueChange={setTimeRange}>
                            <SelectTrigger className="w-full sm:w-[150px] border-none shadow-none focus:ring-0 h-8 p-0">
                                <SelectValue placeholder="Period" />
                            </SelectTrigger>
                            <SelectContent>
                                <SelectItem value="today">Today</SelectItem>
                                <SelectItem value="yesterday">Yesterday</SelectItem>
                                <SelectItem value="this_month">This Month</SelectItem>
                            </SelectContent>
                        </Select>
                    </div>

                </div>
            </header>

            {/* --- Stats Cards --- */}
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
                <StatCard
                    title="Revenue"
                    value={formatCurrency(stats.totalCollection)}
                    trend={getRangeLabel()}
                    icon={<DollarSign className="text-blue-600" />}
                    loading={loading}
                />
                <StatCard
                    title="Receipts"
                    value={stats.receiptCount.toString()}
                    trend={getRangeLabel()}
                    icon={<CreditCard className="text-green-600" />}
                    loading={loading}
                />
                <StatCard
                    title="Staff"
                    value={stats.employeeCount.toString()}
                    trend="Active"
                    icon={<Users className="text-purple-600" />}
                    loading={loading}
                />
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 items-start">
                {/* --- Recent Sales --- */}
                <Card className="lg:col-span-2 shadow-sm border bg-white flex flex-col">
                    <CardHeader className="flex flex-row items-center justify-between">
                        <div>
                            <CardTitle>Recent Sales</CardTitle>
                            <CardDescription>Latest transactions from your POS.</CardDescription>
                        </div>
                        <Button variant="outline" size="sm" asChild>
                            <Link to="/admin/reports">View All</Link>
                        </Button>
                    </CardHeader>
                    <CardContent className="flex-1 overflow-hidden">
                        {loading ? (
                            <div className="flex justify-center p-8">
                                <Loader2 className="animate-spin text-primary" size={24} />
                            </div>
                        ) : recentSales.length === 0 ? (
                            <div className="text-center py-8 text-muted-foreground">
                                No sales yet.
                            </div>
                        ) : (
                            <div className="space-y-6 max-h-[600px] overflow-y-auto pr-2 custom-scrollbar">
                                {recentSales.map((sale) => (
                                    <SaleItem
                                        key={sale.id}
                                        name={sale.userName}
                                        role={sale.userRole}
                                        billNumber={sale.billNumber}
                                        amount={formatCurrency(sale.totalAmount)}
                                        date={`${new Date(sale.createdAt).toLocaleDateString([], { day: '2-digit', month: 'short' })}, ${new Date(sale.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}`}
                                    />
                                ))}
                            </div>
                        )}
                    </CardContent>
                </Card>

                {/* --- Quick Actions --- */}
                <Card className="shadow-sm border bg-white flex flex-col ">
                    <CardHeader>
                        <CardTitle>Quick Actions</CardTitle>
                        <CardDescription>Management shortcuts</CardDescription>
                    </CardHeader>
                    <CardContent className="grid gap-3">
                        <Button className="w-full justify-start" variant="outline" asChild>
                            <Link to="/admin/products">
                                <Package className="mr-2" size={18} /> Manage Products
                            </Link>
                        </Button>
                        <Button className="w-full justify-start" variant="outline" asChild>
                            <Link to="/admin/employees">
                                <Users className="mr-2" size={18} /> Manage Employees
                            </Link>
                        </Button>
                        <Button className="w-full justify-start" variant="outline" asChild>
                            <Link to="/admin/reports">
                                <TrendingUp className="mr-2" size={18} /> View Reports
                            </Link>
                        </Button>
                        <Button className="w-full justify-start" variant="outline" asChild>
                            <Link to="/admin/billing">
                                <Receipt className="mr-2" size={18} /> New Bill
                            </Link>
                        </Button>
                        <Button className="w-full justify-start" variant="outline" asChild>
                            <Link to="/admin/employees">
                                <Filter className="mr-2" size={18} /> Permissions
                            </Link>
                        </Button>
                    </CardContent>
                </Card>
            </div>
        </div>
    );
}

// --- Reusable Components ---

function Loader2({ className, size }) {
    return <Activity className={`${className} animate-pulse`} size={size} />;
}

function StatCard({ title, value, trend, icon, loading }) {
    return (
        <Card className="shadow-sm border-none">
            <CardHeader className="flex flex-row items-center justify-between pb-2">
                <CardTitle className="text-xs font-medium text-muted-foreground uppercase tracking-wider">{title}</CardTitle>
                <div className="p-2 bg-gray-50 rounded-md">{icon}</div>
            </CardHeader>
            <CardContent>
                {loading ? (
                    <div className="h-8 w-24 bg-gray-100 animate-pulse rounded"></div>
                ) : (
                    <div className="text-2xl font-bold">{value}</div>
                )}
                <p className="text-xs text-muted-foreground mt-1">
                    {trend}
                </p>
            </CardContent>
        </Card>
    );
}

function SaleItem({ name, role, billNumber, amount, date }) {
    return (
        <div className="flex items-center justify-between group">
            <div className="flex items-center gap-4">
                <Avatar className="h-9 w-9">
                    <AvatarFallback className="bg-blue-50 text-blue-600 font-bold">{name?.[0] || 'A'}</AvatarFallback>
                </Avatar>
                <div className="space-y-0.5">
                    <div className="flex items-center gap-2">
                        <p className="text-sm font-semibold">{name}</p>
                        <span className="text-[10px] px-1.5 py-0.5 rounded-full bg-gray-100 text-gray-500 font-medium whitespace-nowrap">
                            {role}
                        </span>
                    </div>
                    <p className="text-xs text-muted-foreground">Bill: {billNumber} • {date}</p>
                </div>
            </div>
            <div className="font-bold text-sm">{amount}</div>
        </div>
    );
}
