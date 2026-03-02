import { useState, useEffect } from "react";
import { useSelector } from "react-redux";
import { getAllPermissions, getRoles, getRolePermissions, updateRolePermissions } from "@/lib/api";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Checkbox } from "@/components/ui/checkbox";
import { Loader2, ShieldCheck, AlertCircle, Check } from "lucide-react";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { ScrollArea } from "@/components/ui/scroll-area";

import { useNavigate } from "react-router-dom";

export default function Permissions() {
    const { user } = useSelector((state) => state.auth);
    const navigate = useNavigate();
    const [allPermissions, setAllPermissions] = useState([]);
    const [roles, setRoles] = useState([]);
    const [selectedRole, setSelectedRole] = useState("");
    const [rolePermissions, setRolePermissions] = useState([]);
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState(null);
    const [success, setSuccess] = useState(false);

    useEffect(() => {
        if (user?.role !== "Admin") {
            navigate("/admin");
            return;
        }
        fetchInitialData();
    }, [user, navigate]);

    useEffect(() => {
        if (selectedRole) {
            fetchRolePermissions();
        }
    }, [selectedRole]);

    const fetchInitialData = async () => {
        try {
            setLoading(true);
            const [permsData, rolesData] = await Promise.all([
                getAllPermissions(),
                getRoles()
            ]);
            setAllPermissions(permsData);
            // Filter out Admin from roles list for management (Admin always has everything)
            const manageableRoles = rolesData.filter(r => r.roleName !== "Admin");
            setRoles(manageableRoles);
            if (manageableRoles.length > 0) {
                setSelectedRole(manageableRoles[0].id.toString());
            }
        } catch (err) {
            setError("Failed to load permission data.");
        } finally {
            setLoading(false);
        }
    };

    const fetchRolePermissions = async () => {
        try {
            setSuccess(false);
            const data = await getRolePermissions(selectedRole, user.companyId);
            setRolePermissions(data);
        } catch (err) {
            setError("Failed to load permissions for this role.");
        }
    };

    const handleTogglePermission = (key) => {
        setRolePermissions(prev =>
            prev.includes(key)
                ? prev.filter(k => k !== key)
                : [...prev, key]
        );
        setSuccess(false);
    };

    const handleSelectAll = () => {
        const allKeys = allPermissions.map(p => p.key);
        const allSelected = allKeys.every(k => rolePermissions.includes(k));
        setRolePermissions(allSelected ? [] : allKeys);
        setSuccess(false);
    };

    const handleSave = async () => {
        try {
            setSaving(true);
            setError(null);
            await updateRolePermissions(selectedRole, user.companyId, rolePermissions);
            setSuccess(true);
        } catch (err) {
            setError("Failed to save permissions.");
        } finally {
            setSaving(false);
        }
    };

    if (loading) {
        return (
            <div className="flex h-[400px] items-center justify-center">
                <Loader2 className="h-8 w-8 animate-spin text-primary" />
            </div>
        );
    }

    return (
        <div className="space-y-6">
            <header className="flex justify-between items-center">
                <div>
                    <h1 className="text-3xl font-bold tracking-tight">Access Control</h1>
                    <p className="text-muted-foreground text-sm">Configure granular permissions for your staff roles.</p>
                </div>
                <Button onClick={handleSave} disabled={saving}>
                    {saving ? <Loader2 className="mr-2 h-4 w-4 animate-spin" /> : <ShieldCheck className="mr-2 h-4 w-4" />}
                    Save Changes
                </Button>
            </header>

            {error && (
                <Alert variant="destructive">
                    <AlertCircle className="h-4 w-4" />
                    <AlertTitle>Error</AlertTitle>
                    <AlertDescription>{error}</AlertDescription>
                </Alert>
            )}

            {success && (
                <Alert className="bg-green-50 text-green-700 border-green-200">
                    <Check className="h-4 w-4 text-green-700" />
                    <AlertTitle>Success</AlertTitle>
                    <AlertDescription>Permissions updated successfully. Changes will take effect on next user login.</AlertDescription>
                </Alert>
            )}

            <div className="grid gap-6 md:grid-cols-[1fr_2fr]">
                <Card className="h-fit">
                    <CardHeader>
                        <CardTitle>Select Role</CardTitle>
                        <CardDescription>Choose a role to configure permissions.</CardDescription>
                    </CardHeader>
                    <CardContent className="space-y-4">
                        <div className="space-y-2">
                            <Label htmlFor="role-select">Staff Role</Label>
                            <Select value={selectedRole} onValueChange={setSelectedRole}>
                                <SelectTrigger id="role-select">
                                    <SelectValue placeholder="Select a role" />
                                </SelectTrigger>
                                <SelectContent>
                                    {roles.map((role) => (
                                        <SelectItem key={role.id} value={role.id.toString()}>
                                            {role.roleName}
                                        </SelectItem>
                                    ))}
                                </SelectContent>
                            </Select>
                        </div>
                    </CardContent>
                </Card>

                <Card>
                    <CardHeader>
                        <div className="flex items-center justify-between">
                            <div>
                                <CardTitle>Permissions List</CardTitle>
                                <CardDescription>
                                    Managed permissions for <strong>{roles.find(r => r.id.toString() === selectedRole)?.roleName}</strong>
                                </CardDescription>
                            </div>
                            <div className="flex items-center gap-2">
                                <Checkbox
                                    id="select-all-perms"
                                    checked={allPermissions.length > 0 && allPermissions.every(p => rolePermissions.includes(p.key))}
                                    onCheckedChange={handleSelectAll}
                                />
                                <Label htmlFor="select-all-perms" className="text-sm font-medium cursor-pointer select-none">
                                    Select All
                                </Label>
                            </div>
                        </div>
                    </CardHeader>
                    <CardContent>
                        <ScrollArea className="h-[400px] pr-4">
                            <div className="space-y-4">
                                {allPermissions.map((perm) => (
                                    <div key={perm.id} className="flex items-start space-x-3 space-y-0 rounded-md border p-4 hover:bg-muted/50 transition-colors">
                                        <Checkbox
                                            id={`perm-${perm.id}`}
                                            checked={rolePermissions.includes(perm.key)}
                                            onCheckedChange={() => handleTogglePermission(perm.key)}
                                        />
                                        <div className="grid gap-1.5 leading-none">
                                            <Label
                                                htmlFor={`perm-${perm.id}`}
                                                className="text-sm font-medium leading-none peer-disabled:cursor-not-allowed peer-disabled:opacity-70 cursor-pointer"
                                            >
                                                {perm.key.replace(/_/g, ' ')}
                                            </Label>
                                            <p className="text-sm text-muted-foreground">
                                                {perm.description}
                                            </p>
                                        </div>
                                    </div>
                                ))}
                            </div>
                        </ScrollArea>
                    </CardContent>
                </Card>
            </div>
        </div>
    );
}
