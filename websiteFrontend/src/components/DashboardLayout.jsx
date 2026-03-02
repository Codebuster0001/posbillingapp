import { Outlet, useLocation } from "react-router-dom";
import Sidebar from "./Sidebar";
import { useSelector } from "react-redux";
import { Sheet, SheetContent, SheetTrigger } from "@/components/ui/sheet";
import { Menu } from "lucide-react";
import { Button } from "@/components/ui/button";
import { useState, useEffect } from "react";

export default function DashboardLayout() {
    const { user } = useSelector((state) => state.auth);
    const { pathname } = useLocation();
    const isMobileApp = new URLSearchParams(window.location.search).get("mobile") === "true";
    const [isMobileOpen, setIsMobileOpen] = useState(false);

    useEffect(() => {
        const handleResize = () => {
            if (window.innerWidth >= 768) setIsMobileOpen(false);
        };
        window.addEventListener("resize", handleResize);
        return () => window.removeEventListener("resize", handleResize);
    }, []);

    return (
        <div className="flex h-screen overflow-hidden">
            {/* Desktop Sidebar */}
            {!isMobileApp && <Sidebar className="hidden md:flex flex-col border-r w-64 bg-gray-50 flex-shrink-0" />}

            <div className="flex-1 flex flex-col h-full overflow-hidden">
                {/* Mobile Header / Trigger */}
                {!isMobileApp && (
                    <div className="md:hidden border-b bg-white p-4 flex items-center justify-between flex-shrink-0">
                        <div className="flex items-center gap-4">
                            <Sheet open={isMobileOpen} onOpenChange={setIsMobileOpen}>
                                <SheetTrigger asChild>
                                    <Button variant="ghost" size="icon">
                                        <Menu className="h-6 w-6" />
                                    </Button>
                                </SheetTrigger>
                                <SheetContent side="left" className="p-0 w-64">
                                    <Sidebar className="flex border-none h-full" onClick={() => setIsMobileOpen(false)} />
                                </SheetContent>
                            </Sheet>
                            <span className="font-bold text-lg truncate">{user?.companyName}</span>
                        </div>
                    </div>
                )}

                {/* Main Content Area */}
                <main className={`flex-1 overflow-y-auto bg-gray-100 ${isMobileApp ? 'p-0' : 'p-4'}`}>
                    <Outlet />
                </main>
            </div>
        </div>
    );
}
