
import { useState, useEffect, useMemo } from "react";
import { useSelector } from "react-redux";
import { createOrder, getAssignedMenuDetails, getProducts } from "@/lib/api";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Loader2, CheckCircle2, Printer, Receipt, Zap, Delete, X } from "lucide-react";
import {
    Dialog,
    DialogContent,
    DialogHeader,
    DialogTitle,
    DialogFooter
} from "@/components/ui/dialog";

export default function Calculator() {
    const { user } = useSelector((state) => state.auth);
    const [expression, setExpression] = useState("");
    const [submitting, setSubmitting] = useState(false);
    const [orderDone, setOrderDone] = useState(false);
    const [lastOrderDetails, setLastOrderDetails] = useState(null);
    const [assignedItems, setAssignedItems] = useState([]);
    const [error, setError] = useState(null);

    useEffect(() => {
        fetchPlaceholderItem();
    }, []);

    const fetchPlaceholderItem = async () => {
        try {
            const data = await (user?.role === "Admin" ? getProducts(user.companyId) : getAssignedMenuDetails(user.id));
            setAssignedItems(data || []);
        } catch (err) {
            console.error("Failed to fetch assigned items", err);
        }
    };

    const currentAmount = useMemo(() => {
        const val = parseFloat(expression);
        return isNaN(val) ? 0 : val;
    }, [expression]);

    const handleNumberClick = (val) => {
        if (expression.length >= 10) return;
        setExpression(prev => prev + val);
    };

    const handleClear = () => {
        setExpression("");
    };

    const handleDelete = () => {
        setExpression(prev => prev.slice(0, -1));
    };

    const handleDot = () => {
        if (!expression.includes(".")) {
            setExpression(prev => (prev === "" ? "0." : prev + "."));
        }
    };

    const handleCheckout = async () => {
        if (currentAmount <= 0) return;
        if (assignedItems.length === 0) {
            setError("No menu items assigned. Cannot create bill.");
            return;
        }

        setSubmitting(true);
        setError(null);

        try {
            const autoItem = assignedItems[0];
            const orderData = {
                companyId: user.companyId,
                userId: user.id,
                billNumber: `CALC-${Date.now().toString().slice(-6)}`,
                totalAmount: currentAmount,
                items: [{
                    menuItemId: autoItem.id,
                    itemName: autoItem.name,
                    price: currentAmount,
                    quantity: 1
                }]
            };

            const response = await createOrder(orderData);
            if (response.success) {
                const details = {
                    ...orderData,
                    orderId: response.orderId,
                    date: new Date().toLocaleString()
                };
                setLastOrderDetails(details);
                setExpression("");
                executePrint(details);
                setOrderDone(true);
            }
        } catch (err) {
            setError(err.message || "Checkout failed");
        } finally {
            setSubmitting(false);
        }
    };

    const formatCurrency = (amount) => {
        return new Intl.NumberFormat('en-IN', {
            style: 'currency',
            currency: user?.currencySymbol === '₹' ? 'INR' : 'USD',
        }).format(amount).replace('INR', '₹');
    };

    const executePrint = (details) => {
        if (!details) return;
        const printWindow = window.open('', '_blank');
        printWindow.document.write(`
            <html>
                <head>
                    <title>Calculator Receipt</title>
                    <style>
                        body { font-family: 'Courier New', monospace; width: 80mm; padding: 10px; margin: 0; text-align: center; }
                        .item { display: flex; justify-content: space-between; margin: 5px 0; }
                    </style>
                </head>
                <body onload="window.print();window.close()">
                    <h3>${user?.companyName}</h3>
                    <p>Bill #: ${details.orderId}</p>
                    <p>Date: ${details.date}</p>
                    <hr/>
                    <div class="item">
                        <span>${details.items[0].itemName}</span>
                        <span>${formatCurrency(details.totalAmount)}</span>
                    </div>
                    <hr/>
                    <div class="item" style="font-weight:bold">
                        <span>TOTAL</span>
                        <span>${formatCurrency(details.totalAmount)}</span>
                    </div>
                    <p style="margin-top:20px">Thank you!</p>
                </body>
            </html>
        `);
        printWindow.document.close();
    };

    return (
        <div className="flex flex-col items-center justify-center h-[calc(100vh-140px)] max-w-md mx-auto">
            <Card className="w-full shadow-xl border-none">
                <CardHeader className="text-center pb-2">
                    <CardTitle className="text-2xl font-bold flex items-center justify-center gap-2">
                        <Zap className="text-amber-500 fill-amber-500" /> POS Calculator
                    </CardTitle>
                    <p className="text-xs text-muted-foreground uppercase tracking-widest font-semibold">Quick Billing</p>
                </CardHeader>
                <CardContent className="space-y-6">
                    {/* Display Area */}
                    <div className="bg-gray-100/80 rounded-2xl p-6 text-right space-y-1 shadow-inner border border-gray-200">
                        <div className="text-xs text-muted-foreground font-mono h-4">
                            {expression || "0"}
                        </div>
                        <div className="text-4xl font-bold font-mono tracking-tighter text-primary">
                            {formatCurrency(currentAmount)}
                        </div>
                    </div>

                    {error && <p className="text-xs text-red-500 text-center font-medium bg-red-50 py-2 rounded-lg">{error}</p>}

                    {/* Keypad */}
                    <div className="grid grid-cols-3 gap-3">
                        {[1, 2, 3, 4, 5, 6, 7, 8, 9].map(num => (
                            <Button
                                key={num}
                                variant="outline"
                                className="h-16 text-2xl font-semibold rounded-2xl hover:bg-primary/5 active:scale-95 transition-all"
                                onClick={() => handleNumberClick(num.toString())}
                            >
                                {num}
                            </Button>
                        ))}
                        <Button
                            variant="outline"
                            className="h-16 text-2xl font-semibold rounded-2xl hover:bg-primary/5"
                            onClick={handleDot}
                        >
                            .
                        </Button>
                        <Button
                            variant="outline"
                            className="h-16 text-2xl font-semibold rounded-2xl hover:bg-primary/5"
                            onClick={() => handleNumberClick("0")}
                        >
                            0
                        </Button>
                        <Button
                            variant="ghost"
                            className="h-16 text-destructive hover:bg-red-50 rounded-2xl"
                            onClick={handleDelete}
                        >
                            <Delete />
                        </Button>
                    </div>

                    <div className="grid grid-cols-2 gap-3">
                        <Button
                            variant="secondary"
                            className="h-14 font-bold rounded-2xl"
                            onClick={handleClear}
                        >
                            CLEAR
                        </Button>
                        <Button
                            className="h-14 font-bold rounded-2xl bg-primary text-primary-foreground shadow-lg shadow-primary/20 flex gap-2"
                            disabled={currentAmount <= 0 || submitting}
                            onClick={handleCheckout}
                        >
                            {submitting ? <Loader2 className="animate-spin" /> : <><Receipt size={20} /> BILL & PRINT</>}
                        </Button>
                    </div>
                </CardContent>
            </Card>

            <Dialog open={orderDone} onOpenChange={setOrderDone}>
                <DialogContent className="sm:max-w-md text-center py-10">
                    <div className="flex flex-col items-center space-y-4">
                        <div className="bg-green-100 text-green-600 p-4 rounded-full">
                            <CheckCircle2 size={48} />
                        </div>
                        <h2 className="text-2xl font-bold">Bill Generated!</h2>
                        <p className="text-muted-foreground">Order ID: #{lastOrderDetails?.orderId}</p>
                        <div className="text-3xl font-black text-primary py-2">
                            {formatCurrency(lastOrderDetails?.totalAmount)}
                        </div>
                        <Button className="w-full h-12 rounded-xl" onClick={() => setOrderDone(false)}>Next Sale</Button>
                    </div>
                </DialogContent>
            </Dialog>
        </div>
    );
}
