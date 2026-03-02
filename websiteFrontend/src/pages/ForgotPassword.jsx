
import { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import { forgotPassword, resetPassword } from "@/lib/api";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
    Card,
    CardContent,
    CardDescription,
    CardHeader,
    CardTitle,
    CardFooter,
} from "@/components/ui/card";
import { Loader2, ArrowLeft, Mail, KeyRound, CheckCircle2 } from "lucide-react";
import { Alert, AlertDescription } from "@/components/ui/alert";

export default function ForgotPassword() {
    const [step, setStep] = useState(1); // 1: Email, 2: Reset
    const [email, setEmail] = useState("");
    const [otp, setOtp] = useState("");
    const [newPassword, setNewPassword] = useState("");
    const [confirmPassword, setConfirmPassword] = useState("");
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");
    const [success, setSuccess] = useState("");
    const navigate = useNavigate();

    const handleSendOtp = async (e) => {
        e.preventDefault();
        setError("");
        setLoading(true);
        try {
            const res = await forgotPassword(email);
            setStep(2);
            // Use message from API (it might contain the OTP if email fails)
            let msg = res.message || "OTP sent successfully!";
            if (res.token) msg += ` Code: ${res.token}`;
            setSuccess(msg);
        } catch (err) {
            setError(err.message || "Failed to send OTP. Please try again.");
        } finally {
            setLoading(false);
        }
    };

    const handleResetPassword = async (e) => {
        e.preventDefault();
        setError("");
        if (newPassword !== confirmPassword) {
            setError("Passwords do not match.");
            return;
        }
        setLoading(true);
        try {
            const res = await resetPassword(email, otp, newPassword);
            setStep(3); // Success step
            setSuccess(res.message || "Password reset successful!");
        } catch (err) {
            setError(err.message || "Failed to reset password. Check your OTP.");
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="flex items-center justify-center min-h-screen bg-gray-50/50 p-4">
            <Card className="w-full max-w-[400px] shadow-lg border-none">
                <CardHeader className="space-y-1">
                    <div className="flex items-center gap-2 mb-2">
                        <Link to="/login" className="text-muted-foreground hover:text-primary transition-colors">
                            <ArrowLeft size={18} />
                        </Link>
                    </div>
                    <CardTitle className="text-2xl font-bold">
                        {step === 1 ? "Forgot Password" : step === 2 ? "Reset Password" : "All Set!"}
                    </CardTitle>
                    <CardDescription>
                        {step === 1
                            ? "Enter your email to receive a recovery OTP."
                            : step === 2
                                ? "Enter the 6-digit code and your new password."
                                : "Your password has been successfully updated."}
                    </CardDescription>
                </CardHeader>
                <CardContent>
                    {error && (
                        <Alert variant="destructive" className="mb-4">
                            <AlertDescription>{error}</AlertDescription>
                        </Alert>
                    )}
                    {success && step !== 3 && (
                        <Alert className="mb-4 bg-green-50 border-green-200 text-green-700">
                            <AlertDescription>{success}</AlertDescription>
                        </Alert>
                    )}

                    {step === 1 && (
                        <form onSubmit={handleSendOtp} className="space-y-4">
                            <div className="space-y-2">
                                <Label htmlFor="email">Email Address</Label>
                                <div className="relative">
                                    <Mail className="absolute left-3 top-3 h-4 w-4 text-muted-foreground" />
                                    <Input
                                        id="email"
                                        type="email"
                                        placeholder="name@company.com"
                                        className="pl-10"
                                        value={email}
                                        onChange={(e) => setEmail(e.target.value)}
                                        required
                                    />
                                </div>
                            </div>
                            <Button className="w-full h-11" type="submit" disabled={loading}>
                                {loading ? <Loader2 className="animate-spin mr-2" /> : null}
                                Send Reset Code
                            </Button>
                        </form>
                    )}

                    {step === 2 && (
                        <form onSubmit={handleResetPassword} className="space-y-4">
                            <div className="space-y-2">
                                <Label htmlFor="otp">Verification Code (OTP)</Label>
                                <Input
                                    id="otp"
                                    type="text"
                                    placeholder="Enter 6-digit code"
                                    className="text-center text-lg tracking-widest font-bold"
                                    maxLength={6}
                                    value={otp}
                                    onChange={(e) => setOtp(e.target.value)}
                                    required
                                />
                            </div>
                            <div className="space-y-2">
                                <Label htmlFor="newPassword">New Password</Label>
                                <div className="relative">
                                    <KeyRound className="absolute left-3 top-3 h-4 w-4 text-muted-foreground" />
                                    <Input
                                        id="newPassword"
                                        type="password"
                                        placeholder="Min. 8 characters"
                                        className="pl-10"
                                        value={newPassword}
                                        onChange={(e) => setNewPassword(e.target.value)}
                                        required
                                    />
                                </div>
                            </div>
                            <div className="space-y-2">
                                <Label htmlFor="confirmPassword">Confirm Password</Label>
                                <div className="relative">
                                    <KeyRound className="absolute left-3 top-3 h-4 w-4 text-muted-foreground" />
                                    <Input
                                        id="confirmPassword"
                                        type="password"
                                        placeholder="Repeat new password"
                                        className="pl-10"
                                        value={confirmPassword}
                                        onChange={(e) => setConfirmPassword(e.target.value)}
                                        required
                                    />
                                </div>
                            </div>
                            <Button className="w-full h-11" type="submit" disabled={loading}>
                                {loading ? <Loader2 className="animate-spin mr-2" /> : null}
                                Reset Password
                            </Button>
                        </form>
                    )}

                    {step === 3 && (
                        <div className="flex flex-col items-center py-6 space-y-4">
                            <div className="bg-green-100 text-green-600 p-3 rounded-full">
                                <CheckCircle2 size={40} />
                            </div>
                            <p className="text-center font-medium">You can now sign in with your new password.</p>
                            <Button className="w-full" onClick={() => navigate("/login")}>
                                Go to Login
                            </Button>
                        </div>
                    )}
                </CardContent>
                <CardFooter className="flex justify-center border-t py-4">
                    <p className="text-sm text-muted-foreground">
                        Remember your password?{" "}
                        <Link to="/login" className="text-primary font-semibold hover:underline">
                            Login
                        </Link>
                    </p>
                </CardFooter>
            </Card>
        </div>
    );
}
