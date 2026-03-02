import { useState, useEffect } from "react";
import { useSelector } from "react-redux";
import { Link } from "react-router-dom";
import { getDashboardStats, getOrderHistory } from "@/lib/api";
import {
    Users,
    DollarSign,
    CreditCard,
    Activity,
    Receipt,
    Zap,
    Calendar,
    ArrowRight,
    ShoppingBag
} from "lucide-react";

import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from "@/components/ui/select";
import { Badge } from "@/components/ui/badge";

export default function UserDashboard() {
    const { user } = useSelector((state) => state.auth);
    const [timeRange, setTimeRange] = useState("today");
    const [stats, setStats] = useState({
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
            case "this_month": {
                const firstDay = new Date(now.getFullYear(), now.getMonth(), 1, 0, 0, 0);
                const lastDay = new Date(now.getFullYear(), now.getMonth() + 1, 0, 23, 59, 59);
                return { from: formatDate(firstDay), to: formatDate(lastDay) };
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

            // Parallel fetch stats and history
            const [statsData, historyData] = await Promise.all([
                getDashboardStats(user.companyId, from, to),
                getOrderHistory(user.companyId, from, to)
            ]);

            if (statsData.success) {
                setStats({
                    totalCollection: statsData.totalCollection,
                    receiptCount: statsData.receiptCount
                });
            }
            setRecentSales(historyData || []);
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
            case "this_month": return "This Month";
            default: return "Lifetime";
        }
    };

    return (
        <div className="space-y-6 pb-12">
            <header className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
                <div>
                    <h1 className="text-3xl font-bold tracking-tight">Personal Dashboard</h1>
                    <p className="text-muted-foreground text-sm">Track your sales activity and performance.</p>
                </div>
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
            </header>

            {/* Stats Overview */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
                <StatCard
                    title="My Collection"
                    value={formatCurrency(stats.totalCollection)}
                    trend={getRangeLabel()}
                    icon={<DollarSign className="text-blue-600" />}
                    loading={loading}
                />
                <StatCard
                    title="Receipts Issued"
                    value={stats.receiptCount.toString()}
                    trend={getRangeLabel()}
                    icon={<Receipt className="text-green-600" />}
                    loading={loading}
                />
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 items-start">
                {/* Recent Sales Table */}
                <Card className="lg:col-span-2 shadow-sm border bg-white overflow-hidden">
                    <CardHeader className="flex flex-row items-center justify-between">
                        <div>
                            <CardTitle>Recent Sales</CardTitle>
                            <CardDescription>Your latest transactions for {getRangeLabel().toLowerCase()}</CardDescription>
                        </div>
                        <Button variant="ghost" size="sm" asChild>
                            <Link to="/user/billing" className="text-primary flex items-center gap-1">
                                View All <ArrowRight size={14} />
                            </Link>
                        </Button>
                    </CardHeader>
                    <CardContent className="p-0">
                        <div className="overflow-x-auto">
                            <table className="w-full text-sm text-left">
                                <thead className="bg-gray-50 text-gray-500 uppercase text-xs font-semibold">
                                    <tr>
                                        <th className="px-6 py-3">Bill #</th>
                                        <th className="px-6 py-3">Date & Time</th>
                                        <th className="px-6 py-3 text-right">Amount</th>
                                    </tr>
                                </thead>
                                <tbody className="divide-y divide-gray-100">
                                    {loading ? (
                                        [...Array(5)].map((_, i) => (
                                            <tr key={i}>
                                                <td className="px-6 py-4"><div className="h-4 w-12 bg-gray-100 animate-pulse rounded"></div></td>
                                                <td className="px-6 py-4"><div className="h-4 w-24 bg-gray-100 animate-pulse rounded"></div></td>
                                                <td className="px-6 py-4"><div className="h-4 w-16 bg-gray-100 animate-pulse rounded ml-auto"></div></td>
                                            </tr>
                                        ))
                                    ) : recentSales.length === 0 ? (
                                        <tr>
                                            <td colSpan="3" className="px-6 py-10 text-center text-muted-foreground">
                                                No receipts found for this period.
                                            </td>
                                        </tr>
                                    ) : (
                                        recentSales.map((sale) => (
                                            <tr key={sale.id} className="hover:bg-gray-50/50 transition-colors">
                                                <td className="px-6 py-4 font-medium">#{sale.billNumber}</td>
                                                <td className="px-6 py-4 text-gray-500">
                                                    {new Date(sale.createdAt).toLocaleDateString([], { day: '2-digit', month: 'short' })}, {new Date(sale.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                                                </td>
                                                <td className="px-6 py-4 text-right font-semibold text-primary">
                                                    {formatCurrency(sale.totalAmount)}
                                                </td>
                                            </tr>
                                        ))
                                    )}
                                </tbody>
                            </table>
                        </div>
                    </CardContent>
                </Card>


            </div>
        </div>
    );
}

function StatCard({ title, value, trend, icon, loading }) {
    return (
        <Card className="shadow-sm border-none overflow-hidden group">
            <div className="absolute top-0 left-0 w-1 h-full bg-primary/20 group-hover:bg-primary transition-colors"></div>
            <CardHeader className="flex flex-row items-center justify-between pb-2">
                <CardTitle className="text-xs font-medium text-muted-foreground uppercase tracking-wider">{title}</CardTitle>
                <div className="p-2 bg-gray-50 rounded-md group-hover:scale-110 transition-transform">{icon}</div>
            </CardHeader>
            <CardContent>
                {loading ? (
                    <div className="h-8 w-24 bg-gray-100 animate-pulse rounded"></div>
                ) : (
                    <div className="text-2xl font-bold">{value}</div>
                )}
                <p className="text-xs text-muted-foreground mt-1 flex items-center gap-1">
                    <Activity size={12} /> {trend}
                </p>
            </CardContent>
        </Card>
    );
}
