import { useState, useEffect } from "react";
import { useNavigate, Link } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
    Card,
    CardContent,
    CardDescription,
    CardHeader,
    CardTitle,
} from "@/components/ui/card";
import { Loader2, Building2, CheckCircle2 } from "lucide-react";

// ─── API helpers (no auth needed for registration) ───────────────────────────
const api = {
    get: (url) => fetch(url).then(r => r.ok ? r.json() : Promise.reject(r)),
    post: (url, body) => fetch(url, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(body),
    }).then(async r => {
        const data = await r.json().catch(() => ({}));
        if (!r.ok) throw new Error(data.message || "Registration failed");
        return data;
    }),
};

export default function Register() {
    const navigate = useNavigate();

    // Form state
    const [form, setForm] = useState({
        companyName: "",
        phoneNumber: "",
        email: "",
        password: "",
        confirmPassword: "",
        countryId: "",
        stateId: "",
        cityId: "",
    });

    // Location data
    const [countries, setCountries] = useState([]);
    const [states, setStates] = useState([]);
    const [cities, setCities] = useState([]);
    const [loadingStates, setLoadingStates] = useState(false);
    const [loadingCities, setLoadingCities] = useState(false);

    // Submit state
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");
    const [success, setSuccess] = useState(false);

    // Load countries on mount
    useEffect(() => {
        api.get("/api/Metadata/countries")
            .then(setCountries)
            .catch(() => setError("Failed to load countries."));
    }, []);

    // Load states when country changes
    useEffect(() => {
        if (!form.countryId) { setStates([]); return; }
        setLoadingStates(true);
        setForm(f => ({ ...f, stateId: "", cityId: "" }));
        setCities([]);
        api.get(`/api/Metadata/states/${form.countryId}`)
            .then(setStates)
            .catch(() => setError("Failed to load states."))
            .finally(() => setLoadingStates(false));
    }, [form.countryId]);

    // Load cities when state changes
    useEffect(() => {
        if (!form.stateId) { setCities([]); return; }
        setLoadingCities(true);
        setForm(f => ({ ...f, cityId: "" }));
        api.get(`/api/Metadata/cities/${form.stateId}`)
            .then(setCities)
            .catch(() => setError("Failed to load cities."))
            .finally(() => setLoadingCities(false));
    }, [form.stateId]);

    const set = (key) => (e) => {
        setError("");
        setForm(f => ({ ...f, [key]: e.target.value }));
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError("");

        if (form.password !== form.confirmPassword) {
            setError("Passwords do not match.");
            return;
        }
        if (!form.countryId || !form.stateId || !form.cityId) {
            setError("Please select your Country, State, and City.");
            return;
        }

        setLoading(true);
        try {
            await api.post("/api/Auth/register", {
                companyName: form.companyName,
                phoneNumber: form.phoneNumber,
                email: form.email,
                password: form.password,
                countryId: Number(form.countryId),
                stateId: Number(form.stateId),
                cityId: Number(form.cityId),
            });
            setSuccess(true);
            setTimeout(() => navigate("/"), 2500);
        } catch (err) {
            setError(err.message || "Registration failed. Please try again.");
        } finally {
            setLoading(false);
        }
    };

    if (success) {
        return (
            <div className="flex items-center justify-center min-h-screen bg-gray-100">
                <Card className="w-[380px] text-center">
                    <CardContent className="pt-10 pb-8 space-y-4">
                        <CheckCircle2 className="h-14 w-14 text-green-500 mx-auto" />
                        <h2 className="text-xl font-bold">Account Created!</h2>
                        <p className="text-muted-foreground text-sm">
                            Your company has been registered with a <span className="font-semibold text-indigo-600">20-day free trial</span>. Redirecting to login…
                        </p>
                        <Loader2 className="h-5 w-5 animate-spin mx-auto text-muted-foreground" />
                    </CardContent>
                </Card>
            </div>
        );
    }

    return (
        <div className="flex items-center justify-center min-h-screen bg-gray-100 py-8 px-4">
            <Card className="w-full max-w-md shadow-lg">
                <CardHeader className="text-center pb-2">
                    <div className="flex items-center justify-center gap-2 mb-1">
                        <Building2 className="h-7 w-7 text-primary" />
                        <span className="text-lg font-black tracking-tight">POS Billing</span>
                    </div>
                    <CardTitle className="text-2xl">Create Account</CardTitle>
                    <CardDescription>
                        Start your <span className="font-semibold text-indigo-600">20-day free trial</span> — no credit card required.
                    </CardDescription>
                </CardHeader>

                <CardContent>
                    <form onSubmit={handleSubmit} className="grid gap-4 mt-2">

                        {/* Company Name */}
                        <div className="flex flex-col gap-1.5">
                            <Label htmlFor="companyName">Company Name</Label>
                            <Input
                                id="companyName"
                                placeholder="Acme Retail Store"
                                value={form.companyName}
                                onChange={set("companyName")}
                                required
                            />
                        </div>

                        {/* Phone */}
                        <div className="flex flex-col gap-1.5">
                            <Label htmlFor="phone">Phone Number</Label>
                            <Input
                                id="phone"
                                type="tel"
                                placeholder="+91 9876543210"
                                value={form.phoneNumber}
                                onChange={set("phoneNumber")}
                                required
                            />
                        </div>

                        {/* Email */}
                        <div className="flex flex-col gap-1.5">
                            <Label htmlFor="email">Email</Label>
                            <Input
                                id="email"
                                type="email"
                                placeholder="owner@company.com"
                                value={form.email}
                                onChange={set("email")}
                                required
                            />
                        </div>

                        {/* Password */}
                        <div className="grid grid-cols-2 gap-3">
                            <div className="flex flex-col gap-1.5">
                                <Label htmlFor="password">Password</Label>
                                <Input
                                    id="password"
                                    type="password"
                                    placeholder="Min 6 chars"
                                    value={form.password}
                                    onChange={set("password")}
                                    minLength={6}
                                    required
                                />
                            </div>
                            <div className="flex flex-col gap-1.5">
                                <Label htmlFor="confirmPassword">Confirm Password</Label>
                                <Input
                                    id="confirmPassword"
                                    type="password"
                                    placeholder="Repeat password"
                                    value={form.confirmPassword}
                                    onChange={set("confirmPassword")}
                                    minLength={6}
                                    required
                                />
                            </div>
                        </div>

                        {/* Divider */}
                        <div className="flex items-center gap-2 my-1">
                            <div className="flex-1 border-t" />
                            <span className="text-xs text-muted-foreground">Location</span>
                            <div className="flex-1 border-t" />
                        </div>

                        {/* Country */}
                        <div className="flex flex-col gap-1.5">
                            <Label htmlFor="country">Country</Label>
                            <select
                                id="country"
                                className="flex h-9 w-full rounded-md border border-input bg-background px-3 py-1 text-sm shadow-sm transition-colors focus:outline-none focus:ring-1 focus:ring-ring disabled:opacity-50"
                                value={form.countryId}
                                onChange={set("countryId")}
                                required
                            >
                                <option key="country-placeholder" value="">— Select Country —</option>
                                {countries.map(c => (
                                    <option key={`country-${c.id}`} value={c.id}>{c.countryName}</option>
                                ))}
                            </select>
                        </div>

                        {/* State */}
                        <div className="flex flex-col gap-1.5">
                            <Label htmlFor="state">State</Label>
                            <select
                                id="state"
                                className="flex h-9 w-full rounded-md border border-input bg-background px-3 py-1 text-sm shadow-sm transition-colors focus:outline-none focus:ring-1 focus:ring-ring disabled:opacity-50"
                                value={form.stateId}
                                onChange={set("stateId")}
                                disabled={!form.countryId || loadingStates}
                                required
                            >
                                <option key="state-placeholder" value="">
                                    {loadingStates ? "Loading states…" : "— Select State —"}
                                </option>
                                {states.map(s => (
                                    <option key={`state-${s.id}`} value={s.id}>{s.stateName}</option>
                                ))}
                            </select>
                        </div>

                        {/* City */}
                        <div className="flex flex-col gap-1.5">
                            <Label htmlFor="city">City</Label>
                            <select
                                id="city"
                                className="flex h-9 w-full rounded-md border border-input bg-background px-3 py-1 text-sm shadow-sm transition-colors focus:outline-none focus:ring-1 focus:ring-ring disabled:opacity-50"
                                value={form.cityId}
                                onChange={set("cityId")}
                                disabled={!form.stateId || loadingCities}
                                required
                            >
                                <option key="city-placeholder" value="">
                                    {loadingCities ? "Loading cities…" : "— Select City —"}
                                </option>
                                {cities.map(c => (
                                    <option key={`city-${c.id}`} value={c.id}>{c.cityName}</option>
                                ))}
                            </select>
                        </div>

                        {/* Error */}
                        {error && (
                            <p className="text-sm text-red-500 font-medium">{error}</p>
                        )}

                        {/* Submit */}
                        <Button
                            type="submit"
                            className="w-full mt-1"
                            disabled={loading}
                        >
                            {loading
                                ? <><Loader2 className="mr-2 h-4 w-4 animate-spin" /> Creating Account…</>
                                : "Create Account"
                            }
                        </Button>

                        {/* Login link */}
                        <p className="text-center text-sm text-muted-foreground">
                            Already have an account?{" "}
                            <Link to="/" className="text-primary font-medium hover:underline">
                                Login
                            </Link>
                        </p>
                    </form>
                </CardContent>
            </Card>
        </div>
    );
}
