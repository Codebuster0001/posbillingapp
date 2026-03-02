import { useState, useEffect, useMemo, useRef } from "react";
import { useSelector } from "react-redux";
import { getProducts, createOrder, getAssignedMenuDetails, getCategories } from "@/lib/api";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Search, ShoppingCart, Trash2, Plus, Minus, Loader2, CheckCircle2, Receipt, Printer, Wifi, WifiOff, AlertTriangle, Filter, AlertCircle, Zap } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import {
    Dialog,
    DialogContent,
    DialogHeader,
    DialogTitle,
    DialogDescription,
    DialogFooter
} from "@/components/ui/dialog";
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from "@/components/ui/select";

export default function Billing() {
    const { user } = useSelector((state) => state.auth);
    const [products, setProducts] = useState([]);
    const [categories, setCategories] = useState([]);
    const [cart, setCart] = useState([]);
    const [searchTerm, setSearchTerm] = useState("");
    const [selectedCategory, setSelectedCategory] = useState("all");
    const [loading, setLoading] = useState(true);
    const [submitting, setSubmitting] = useState(false);
    const [orderDone, setOrderDone] = useState(false);
    const [lastOrderId, setLastOrderId] = useState(null);
    const [lastOrderDetails, setLastOrderDetails] = useState(null);
    const [error, setError] = useState(null);
    const [printerStatus, setPrinterStatus] = useState("checking");
    const [printerErrorMessage, setPrinterErrorMessage] = useState("");

    useEffect(() => {
        if (user?.companyId) {
            fetchInitialData();
        }
        checkPrinterConnection();
    }, [user?.companyId]);

    const checkPrinterConnection = async () => {
        try {
            let connected = false;
            if (navigator.bluetooth && navigator.bluetooth.getDevices) {
                const devices = await navigator.bluetooth.getDevices();
                if (devices.length > 0) connected = true;
            }
            if (!connected && navigator.usb && navigator.usb.getDevices) {
                const devices = await navigator.usb.getDevices();
                if (devices.length > 0) connected = true;
            }
            setPrinterStatus(connected ? "connected" : "disconnected");
        } catch (e) {
            setPrinterStatus("disconnected");
        }
    };

    const fetchInitialData = async () => {
        try {
            setLoading(true);
            const [productsData, categoriesData] = await Promise.all([
                user?.role === "Limited Access" ? getAssignedMenuDetails(user.id) : getProducts(user.companyId),
                getCategories()
            ]);
            setProducts(productsData.filter(p => p.isAvailable));
            setCategories(categoriesData);
        } catch (err) {
            console.error("Failed to load data", err);
            setError("Could not load menu items. Please refresh.");
        } finally {
            setLoading(false);
        }
    };

    const filteredProducts = useMemo(() => {
        return products.filter(p => {
            const matchesSearch = p.name.toLowerCase().includes(searchTerm.toLowerCase());
            const matchesCategory = selectedCategory === "all" || p.category === selectedCategory;
            return matchesSearch && matchesCategory;
        });
    }, [products, searchTerm, selectedCategory]);

    const addToCart = (product) => {
        setCart(prev => {
            const existing = prev.find(item => item.id === product.id);
            if (existing) {
                return prev.map(item =>
                    item.id === product.id ? { ...item, quantity: item.quantity + 1 } : item
                );
            }
            return [...prev, { ...product, quantity: 1 }];
        });
    };

    const updateQuantity = (id, delta) => {
        setCart(prev => prev.map(item => {
            if (item.id === id) {
                const newQty = Math.max(0, item.quantity + delta);
                return { ...item, quantity: newQty };
            }
            return item;
        }).filter(item => item.quantity > 0));
    };

    const removeFromCart = (id) => {
        setCart(prev => prev.filter(item => item.id !== id));
    };

    const total = useMemo(() => {
        return cart.reduce((sum, item) => sum + (item.price * item.quantity), 0);
    }, [cart]);

    const handleCheckout = async () => {
        if (cart.length === 0) return;

        // Stricter Printer Check BEFORE proceeding
        setPrinterErrorMessage("");
        await checkPrinterConnection();

        if (printerStatus !== "connected") {
            setPrinterErrorMessage("Please connect the printer before generating the bill.");
            return;
        }

        setSubmitting(true);
        setError(null);

        try {
            const orderData = {
                companyId: user.companyId,
                userId: user.id,
                billNumber: `BILL-${Date.now().toString().slice(-6)}`,
                totalAmount: total,
                items: cart.map(item => ({
                    menuItemId: item.id,
                    itemName: item.name,
                    price: item.price,
                    quantity: item.quantity
                }))
            };

            const response = await createOrder(orderData);
            if (response.success) {
                const details = {
                    ...orderData,
                    orderId: response.orderId,
                    date: new Date().toLocaleString()
                };
                setLastOrderId(response.orderId);
                setLastOrderDetails(details);
                setCart([]);

                // Direct Printing as requested
                executePrint(details);
                setOrderDone(true);
            }
        } catch (err) {
            setError(err.message || "Failed to process order.");
        } finally {
            setSubmitting(false);
        }
    };

    const handlePrint = () => {
        if (printerStatus !== "connected") {
            setPrinterErrorMessage("Please connect/pair the printer (Bluetooth or USB) to print receipt.");
            return;
        }
        executePrint(lastOrderDetails);
    };

    const getReceiptHtml = (details) => {
        return `
            <div class="text-center">
                <h3 class="font-bold">${user?.companyName}</h3>
                <p class="text-sm">Bill #: ${details.orderId}</p>
                <p class="text-sm">Date: ${details.date}</p>
                <p class="text-sm">Served by: ${user?.name}</p>
            </div>
            <div class="border-t" style="border-top: 1px dashed #000; margin: 10px 0;"></div>
            <div>
                ${details.items.map(item => `
                    <div style="display: flex; justify-content: space-between;">
                        <span>${item.itemName} x${item.quantity}</span>
                        <span>${formatCurrency(item.price * item.quantity)}</span>
                    </div>
                `).join('')}
            </div>
            <div class="border-t" style="border-top: 1px dashed #000; margin: 10px 0;"></div>
            <div style="display: flex; justify-content: space-between; font-weight: bold; margin-top: 5px;">
                <span>TOTAL</span>
                <span>${formatCurrency(details.totalAmount || 0)}</span>
            </div>
            <div class="border-t" style="border-top: 1px dashed #000; margin: 10px 0;"></div>
            <div class="text-center text-sm" style="text-align: center; margin-top: 15px;">
                <p>Thank you for your visit!</p>
                <p style="font-size: 10px;">Powered by POS Billing App</p>
            </div>
        `;
    };

    const executePrint = (details) => {
        if (!details) return;
        const content = getReceiptHtml(details);
        const printWindow = window.open('', '_blank');
        printWindow.document.write(`
            <html>
                <head>
                    <title>Order Receipt</title>
                    <style>
                        body { font-family: 'Courier New', Courier, monospace; width: 80mm; padding: 10px; margin: 0; }
                        .text-center { text-align: center; }
                        .font-bold { font-weight: bold; }
                        @page { margin: 0; }
                    </style>
                </head>
                <body onload="window.print();window.close()">
                    ${content}
                </body>
            </html>
        `);
        printWindow.document.close();
    };

    const formatCurrency = (amount) => {
        return new Intl.NumberFormat('en-IN', {
            style: 'currency',
            currency: user?.currencySymbol === '₹' ? 'INR' : 'USD',
        }).format(amount).replace('INR', '₹');
    };

    return (
        <div className="flex flex-col h-[calc(100vh-120px)] lg:flex-row gap-6">
            <div className="flex-1 flex flex-col space-y-4 min-w-0">
                <Card className="shadow-sm border-none bg-white">
                    <CardHeader className="pb-3 border-b mb-4">
                        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
                            <div className="flex items-center gap-4">
                                <div>
                                    <h2 className="text-xl font-bold flex items-center gap-2">
                                        <Receipt className="text-primary" /> POS Billing
                                    </h2>

                                </div>
                            </div>

                            <div className="flex flex-wrap items-center gap-3">
                                <div className="flex items-center gap-2">
                                    <Filter size={16} className="text-muted-foreground" />
                                    <Select value={selectedCategory} onValueChange={setSelectedCategory}>
                                        <SelectTrigger className="w-[130px] h-9">
                                            <SelectValue placeholder="Category" />
                                        </SelectTrigger>
                                        <SelectContent>
                                            <SelectItem value="all">Categories</SelectItem>
                                            {categories.map((cat, idx) => (
                                                <SelectItem key={idx} value={cat}>
                                                    {cat}
                                                </SelectItem>
                                            ))}
                                        </SelectContent>
                                    </Select>
                                </div>

                                <div className="relative w-full md:w-52">
                                    <Search className="absolute left-2.5 top-2.5 h-4 w-4 text-muted-foreground" />
                                    <Input
                                        placeholder="Search..."
                                        className="pl-8 h-9"
                                        value={searchTerm}
                                        onChange={(e) => setSearchTerm(e.target.value)}
                                    />
                                </div>
                            </div>
                        </div>
                    </CardHeader>
                    <CardContent>
                        {loading ? (
                            <div className="flex flex-col items-center justify-center p-12 space-y-4">
                                <Loader2 className="animate-spin text-primary" size={40} />
                                <p className="text-muted-foreground">Syncing products...</p>
                            </div>
                        ) : (
                            <ScrollArea className="h-[calc(100vh-280px)] pr-4">
                                <div className="grid grid-cols-2 md:grid-cols-3 xl:grid-cols-4 gap-4">
                                    {filteredProducts.map((product) => (
                                        <div
                                            key={product.id}
                                            onClick={() => addToCart(product)}
                                            className="group relative bg-white border rounded-xl p-3 cursor-pointer hover:border-primary hover:shadow-md transition-all active:scale-95"
                                        >
                                            <div className="aspect-square bg-gray-100 rounded-lg overflow-hidden mb-3">
                                                {product.imageUrl ? (
                                                    <img src={product.imageUrl} alt={product.name} className="w-full h-full object-cover" />
                                                ) : (
                                                    <div className="w-full h-full flex items-center justify-center text-gray-300">
                                                        <ShoppingCart size={40} />
                                                    </div>
                                                )}
                                            </div>
                                            <div className="space-y-1">
                                                <Badge variant="secondary" className="text-[10px] uppercase font-bold tracking-wider opacity-60">
                                                    {product.category}
                                                </Badge>
                                                <h3 className="font-semibold text-sm line-clamp-1">{product.name}</h3>
                                                <p className="text-primary font-bold">{formatCurrency(product.price)}</p>
                                            </div>
                                            <div className="absolute top-2 right-2 opacity-0 group-hover:opacity-100 transition-opacity">
                                                <div className="bg-primary text-white p-1 rounded-full shadow-lg">
                                                    <Plus size={16} />
                                                </div>
                                            </div>
                                        </div>
                                    ))}
                                </div>
                                {filteredProducts.length === 0 && (
                                    <div className="text-center p-12 text-muted-foreground">
                                        <AlertTriangle className="mx-auto mb-2 opacity-20" size={48} />
                                        <p>No products available.</p>
                                    </div>
                                )}
                            </ScrollArea>
                        )}
                    </CardContent>
                </Card>
            </div>

            <div className="w-full lg:w-[380px] flex flex-col flex-shrink-0">
                <Card className="flex-1 flex flex-col shadow-lg border-primary/10 bg-white overflow-hidden">
                    <CardHeader className="bg-primary/5 border-b py-4">
                        <div className="flex items-center justify-between">
                            <div className="flex items-center gap-2">
                                <ShoppingCart className="text-primary" size={20} />
                                <CardTitle className="text-xl">Checkout</CardTitle>
                            </div>
                        </div>
                    </CardHeader>

                    <CardContent className="flex-1 flex flex-col p-0">
                        <ScrollArea className="flex-1 px-6 py-4">
                            {cart.length === 0 ? (
                                <div className="flex flex-col items-center justify-center h-[250px] text-muted-foreground space-y-4">
                                    <div className="p-4 bg-gray-50 rounded-full">
                                        <ShoppingCart size={48} className="opacity-10" />
                                    </div>
                                    <p className="font-medium text-sm">Cart is empty</p>
                                </div>
                            ) : (
                                <div className="space-y-4">
                                    {cart.map((item) => (
                                        <div key={item.id} className="flex flex-col space-y-2 border-b border-dashed pb-3">
                                            <div className="flex justify-between items-start">
                                                <h4 className="text-sm font-semibold pr-4">{item.name}</h4>
                                                <p className="text-sm font-bold whitespace-nowrap">{formatCurrency(item.price * item.quantity)}</p>
                                            </div>
                                            <div className="flex justify-between items-center">
                                                <div className="flex items-center gap-1">
                                                    <Button variant="outline" size="icon" className="h-7 w-7 rounded-full" onClick={() => updateQuantity(item.id, -1)}>
                                                        <Minus size={12} />
                                                    </Button>
                                                    <span className="w-8 text-center text-sm font-medium">{item.quantity}</span>
                                                    <Button variant="outline" size="icon" className="h-7 w-7 rounded-full" onClick={() => updateQuantity(item.id, 1)}>
                                                        <Plus size={12} />
                                                    </Button>
                                                </div>
                                                <Button variant="ghost" size="icon" className="h-7 w-7 text-destructive hover:bg-destructive/10" onClick={() => removeFromCart(item.id)}>
                                                    <Trash2 size={14} />
                                                </Button>
                                            </div>
                                        </div>
                                    ))}
                                </div>
                            )}
                        </ScrollArea>

                        <div className="p-6 bg-gray-50 border-t space-y-4 shadow-[0_-4px_6px_-1px_rgba(0,0,0,0.05)]">
                            {error && (
                                <Alert variant="destructive" className="py-2">
                                    <AlertDescription className="text-xs">{error}</AlertDescription>
                                </Alert>
                            )}

                            {printerErrorMessage && (
                                <Alert variant="destructive" className="py-2 border-2 animate-pulse">
                                    <AlertCircle size={14} className="mr-2" />
                                    <AlertDescription className="text-xs font-bold">{printerErrorMessage}</AlertDescription>
                                </Alert>
                            )}

                            <div className="space-y-2">
                                <div className="flex justify-between text-xs text-muted-foreground">
                                    <span>Subtotal</span>
                                    <span>{formatCurrency(total)}</span>
                                </div>
                                <div className="flex justify-between text-xl font-bold pt-3 border-t">
                                    <span>Total Payable</span>
                                    <span className="text-primary">{formatCurrency(total)}</span>
                                </div>
                            </div>

                            <Button
                                className="w-full h-14 text-xl font-bold rounded-xl shadow-lg active:scale-95 transition-all bg-primary"
                                size="lg"
                                disabled={cart.length === 0 || submitting}
                                onClick={handleCheckout}
                            >
                                {submitting ? <Loader2 className="animate-spin mr-2" /> : (
                                    <><Zap className="mr-2" /> Complete & Print</>
                                )}
                            </Button>
                        </div>
                    </CardContent>
                </Card>
            </div>

            {/* Success Dialog */}
            <Dialog open={orderDone} onOpenChange={setOrderDone}>
                <DialogContent className="sm:max-w-md text-center p-0 overflow-hidden">
                    <div className="bg-primary/10 py-8 flex flex-col items-center">
                        <div className="bg-green-100 text-green-600 p-4 rounded-full mb-3">
                            <CheckCircle2 size={48} />
                        </div>
                        <DialogTitle className="text-2xl font-bold">Transaction Successful!</DialogTitle>
                        <p className="text-sm text-muted-foreground">Order ID: #{lastOrderId}</p>
                    </div>

                    <div className="p-6 space-y-6">
                        <div className="bg-gray-50 p-4 rounded-xl border-2 border-dashed">
                            <div className="flex justify-between items-center text-sm">
                                <span className="text-muted-foreground font-medium">Payment Received</span>
                                <span className="font-bold text-xl text-primary">{formatCurrency(lastOrderDetails?.totalAmount)}</span>
                            </div>
                        </div>

                        <div className="grid grid-cols-2 gap-3">
                            <Button variant="outline" className="w-full h-12" onClick={() => setOrderDone(false)}>Next Sale</Button>
                            <Button className="w-full h-12 gap-2" onClick={handlePrint}>
                                <Printer size={18} /> Re-print
                            </Button>
                        </div>
                    </div>
                </DialogContent>
            </Dialog>
        </div>
    );
}
