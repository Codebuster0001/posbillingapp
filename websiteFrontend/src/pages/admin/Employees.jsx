import { useState, useEffect } from "react";
import { useSelector } from "react-redux";
import {
    getEmployees, createEmployee, updateEmployee, deleteEmployee,
    getRoles, getProducts, getAssignedMenuItems, updateMenuAssignments,
    getCategories
} from "@/lib/api";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Loader2, AlertCircle, Plus, Edit, Trash2, ShieldCheck, Check } from "lucide-react";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
    DialogTrigger,
} from "@/components/ui/dialog";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Checkbox } from "@/components/ui/checkbox";
import { ScrollArea } from "@/components/ui/scroll-area";

export default function Employees() {
    const { user, permissions } = useSelector((state) => state.auth);
    const [employees, setEmployees] = useState([]);
    const [loading, setLoading] = useState(true);
    const [createLoading, setCreateLoading] = useState(false);
    const [error, setError] = useState(null);

    // Dialog states
    const [isDialogOpen, setIsDialogOpen] = useState(false);
    const [isEditDialogOpen, setIsEditDialogOpen] = useState(false);
    const [isDeleteDialogOpen, setIsDeleteDialogOpen] = useState(false);

    // Form states
    const [name, setName] = useState("");
    const [email, setEmail] = useState("");
    const [phoneNumber, setPhoneNumber] = useState("");
    const [role, setRole] = useState("");
    const [isActive, setIsActive] = useState(true);
    const [editingEmployee, setEditingEmployee] = useState(null);
    const [employeeToDelete, setEmployeeToDelete] = useState(null);

    // Assignment States
    const [isAssignOpen, setIsAssignOpen] = useState(false);
    const [assigningEmployee, setAssigningEmployee] = useState(null);
    const [assignedItems, setAssignedItems] = useState([]);
    const [assignLoading, setAssignLoading] = useState(false);

    // Roles state
    const [roles, setRoles] = useState([]);
    const [categories, setCategories] = useState([]);
    const [products, setProducts] = useState([]);


    useEffect(() => {
        if (user?.companyId) {
            fetchEmployees();
            fetchRoles();
            fetchCategories();
            fetchProducts();
        }
    }, [user?.companyId]); // Use stable primitive ID

    const fetchRoles = async () => {
        try {
            const data = await getRoles();
            setRoles(data);
            // Only set default role if current role is empty (don't override user typing)
            if (data.length > 0 && !role) setRole(data[0].roleName);
        } catch (err) {
            console.error("Failed to fetch roles", err);
        }
    };

    const fetchCategories = async () => {
        try {
            const data = await getCategories();
            setCategories(data);
        } catch (err) {
            console.error("Failed to fetch categories", err);
        }
    };

    const fetchProducts = async () => {
        try {
            if (user?.companyId) {
                const data = await getProducts(user.companyId);
                setProducts(data);
            }
        } catch (err) {
            console.error("Failed to fetch products", err);
        }
    };

    const fetchEmployees = async () => {
        try {
            setLoading(true);
            const data = await getEmployees(user.companyId);
            setEmployees(data);
            setError(null);
        } catch (err) {
            setError(err.message);
        } finally {
            setLoading(false);
        }
    };

    const handleCreate = async (e) => {
        e.preventDefault();
        setCreateLoading(true);
        try {
            await createEmployee({
                companyId: user.companyId,
                name,
                email,
                phoneNumber,
                role,
                photoUrl: ""
            });
            setIsDialogOpen(false);
            resetForm();
            await fetchEmployees();
        } catch (err) {
            setError(err.message);
        } finally {
            setCreateLoading(false);
        }
    };

    const handleUpdate = async (e) => {
        e.preventDefault();
        if (!editingEmployee) return;

        try {
            await updateEmployee(editingEmployee.id, {
                name,
                email,
                phoneNumber,
                role,
                isActive,
                photoUrl: editingEmployee.photoUrl
            });
            setIsEditDialogOpen(false);
            setEditingEmployee(null);
            resetForm();
            await fetchEmployees();
        } catch (err) {
            setError(err.message);
        }
    };

    const handleDelete = async () => {
        if (!employeeToDelete) return;
        try {
            await deleteEmployee(employeeToDelete.id);
            setIsDeleteDialogOpen(false);
            setEmployeeToDelete(null);
            await fetchEmployees();
        } catch (err) {
            setError(err.message);
        }
    };

    const openAssignDialog = async (emp) => {
        setAssigningEmployee(emp);
        setAssignLoading(true);
        setIsAssignOpen(true);
        try {
            const items = await getAssignedMenuItems(emp.id);
            setAssignedItems(items);
        } catch (err) {
            console.error("Failed to fetch assignments", err);
        } finally {
            setAssignLoading(false);
        }
    };

    const handleAssignSubmit = async () => {
        if (!assigningEmployee) return;
        setAssignLoading(true);
        try {
            await updateMenuAssignments(assigningEmployee.id, assignedItems);
            setIsAssignOpen(false);
        } catch (err) {
            setError("Failed to update assignments");
        } finally {
            setAssignLoading(false);
        }
    };

    const toggleItemAssignment = (productId) => {
        if (assigningEmployee?.role === "Limited Access") {
            // Limited Access users can only have ONE item assigned
            setAssignedItems([productId]);
            return;
        }
        setAssignedItems(prev =>
            prev.includes(productId)
                ? prev.filter(id => id !== productId)
                : [...prev, productId]
        );
    };

    const resetForm = () => {
        setName("");
        setEmail("");
        setPhoneNumber("");
        setRole(roles.length > 0 ? roles[0].roleName : "");
        setIsActive(true);
    };

    const openEditDialog = (emp) => {
        setEditingEmployee(emp);
        setName(emp.name);
        setEmail(emp.email);
        setPhoneNumber(emp.phoneNumber || "");
        setRole(emp.role);
        setIsActive(emp.isActive);
        setIsEditDialogOpen(true);
    };

    const openCreateDialog = () => {
        resetForm();
        setIsDialogOpen(true);
    };

    const confirmDelete = (emp) => {
        setEmployeeToDelete(emp);
        setIsDeleteDialogOpen(true);
    };

    const canManage = permissions?.includes("MANAGE_EMPLOYEES") || user?.role === "Admin";

    return (
        <div className="space-y-6">
            <header className="flex justify-between items-center">
                <div>
                    <h1 className="text-3xl font-bold tracking-tight">Employee Management</h1>
                    <p className="text-muted-foreground text-sm">Manage your staff and their access levels.</p>
                </div>
                {canManage && (
                    <Dialog open={isDialogOpen} onOpenChange={setIsDialogOpen}>
                        <Button onClick={openCreateDialog}>
                            <Plus className="h-4 w-4" />Add Employee
                        </Button>
                        <DialogContent className="sm:max-w-[425px]">
                            <DialogHeader>
                                <DialogTitle>Add New Employee</DialogTitle>
                                <DialogDescription>
                                    Create a new account for your staff member.
                                </DialogDescription>
                            </DialogHeader>
                            <form onSubmit={handleCreate} className="space-y-4 py-4">
                                <div className="grid gap-2">
                                    <Label htmlFor="name">Full Name</Label>
                                    <Input id="name" placeholder="John Doe" value={name} onChange={(e) => setName(e.target.value)} required />
                                </div>
                                <div className="grid gap-2">
                                    <Label htmlFor="email">Email</Label>
                                    <Input id="email" type="email" placeholder="john@example.com" value={email} onChange={(e) => setEmail(e.target.value)} required />
                                </div>
                                <div className="grid gap-2">
                                    <Label htmlFor="phone">Phone (Optional)</Label>
                                    <Input id="phone" placeholder="+1234567890" value={phoneNumber} onChange={(e) => setPhoneNumber(e.target.value)} />
                                </div>
                                <div className="grid gap-2">
                                    <Label htmlFor="role">Role</Label>
                                    <Select value={role} onValueChange={setRole}>
                                        <SelectTrigger>
                                            <SelectValue placeholder="Select a role" />
                                        </SelectTrigger>
                                        <SelectContent>
                                            {roles.map((r) => (
                                                <SelectItem key={r.id} value={r.roleName}>{r.roleName}</SelectItem>
                                            ))}
                                        </SelectContent>
                                    </Select>
                                </div>
                                <DialogFooter>
                                    <Button type="submit" disabled={createLoading}>
                                        {createLoading ? <><Loader2 className="animate-spin mr-2" size={16} /> Adding...</> : "Create Account"}
                                    </Button>
                                </DialogFooter>
                            </form>
                        </DialogContent>
                    </Dialog>
                )}
            </header>

            {error && (
                <Alert variant="destructive">
                    <AlertCircle className="h-4 w-4" />
                    <AlertTitle>Error</AlertTitle>
                    <AlertDescription>{error}</AlertDescription>
                </Alert>
            )}

            <Card className="shadow-sm bg-white">
                <CardHeader>
                    <CardTitle>Staff List</CardTitle>
                    <CardDescription>View and manage existing employees.</CardDescription>
                </CardHeader>
                <CardContent>
                    {loading ? (
                        <div className="flex justify-center p-8">
                            <Loader2 className="animate-spin text-primary" size={32} />
                        </div>
                    ) : (
                        <div className="w-full overflow-x-auto">
                            <Table className="w-full table-auto">
                                <TableHeader>
                                    <TableRow>
                                        <TableHead className="text-left">Name</TableHead>
                                        <TableHead className="text-left">Email</TableHead>
                                        <TableHead className="text-left">Role</TableHead>
                                        {canManage && <TableHead className="text-center">Actions</TableHead>}
                                    </TableRow>
                                </TableHeader>

                                <TableBody>
                                    {employees.map((emp) => (
                                        <TableRow key={emp.id} className="hover:bg-muted/40 transition-colors">

                                            <TableCell className="font-medium break-words">
                                                {emp.name}
                                            </TableCell>

                                            <TableCell className="break-words">
                                                {emp.email}
                                            </TableCell>

                                            <TableCell>
                                                <span
                                                    className={`px-2 py-1 rounded-full text-xs font-semibold ${emp.role === "Admin"
                                                        ? "bg-purple-100 text-purple-700"
                                                        : emp.role === "Full Access"
                                                            ? "bg-blue-100 text-blue-700"
                                                            : "bg-orange-100 text-orange-700"
                                                        }`}
                                                >
                                                    {emp.role}
                                                </span>
                                            </TableCell>

                                            {canManage && (
                                                <TableCell className="text-center">
                                                    <div className="flex items-center justify-center gap-2">
                                                        {emp.role === "Limited Access" && (
                                                            <Button
                                                                variant="ghost"
                                                                size="icon"
                                                                className="text-primary"
                                                                onClick={() => openAssignDialog(emp)}
                                                                title="Assign Menu Items"
                                                            >
                                                                <ShieldCheck className="h-4 w-4" />
                                                            </Button>
                                                        )}

                                                        <Button
                                                            variant="ghost"
                                                            size="icon"
                                                            onClick={() => openEditDialog(emp)}
                                                        >
                                                            <Edit className="h-4 w-4" />
                                                        </Button>

                                                        <Button
                                                            variant="ghost"
                                                            size="icon"
                                                            className="text-destructive"
                                                            onClick={() => confirmDelete(emp)}
                                                        >
                                                            <Trash2 className="h-4 w-4" />
                                                        </Button>
                                                    </div>
                                                </TableCell>
                                            )}

                                        </TableRow>
                                    ))}
                                </TableBody>
                            </Table>
                        </div>

                    )}
                </CardContent>
            </Card>

            {/* Edit Dialog */}
            <Dialog open={isEditDialogOpen} onOpenChange={setIsEditDialogOpen}>
                <DialogContent className="sm:max-w-[425px]">
                    <DialogHeader>
                        <DialogTitle>Edit Employee</DialogTitle>
                        <DialogDescription>
                            Modify the details and permissions for this employee.
                        </DialogDescription>
                    </DialogHeader>
                    <form onSubmit={handleUpdate} className="space-y-4 py-4">
                        <div className="grid gap-2">
                            <Label htmlFor="edit-name">Full Name</Label>
                            <Input id="edit-name" value={name} onChange={(e) => setName(e.target.value)} required />
                        </div>
                        <div className="grid gap-2">
                            <Label htmlFor="edit-email">Email</Label>
                            <Input id="edit-email" type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
                        </div>
                        <div className="grid gap-2">
                            <Label htmlFor="edit-phone">Phone Number</Label>
                            <Input id="edit-phone" value={phoneNumber} onChange={(e) => setPhoneNumber(e.target.value)} />
                        </div>
                        <div className="grid gap-2">
                            <Label htmlFor="edit-role">Role</Label>
                            <Select value={role} onValueChange={setRole}>
                                <SelectTrigger><SelectValue /></SelectTrigger>
                                <SelectContent>
                                    {roles.map((r) => (<SelectItem key={r.id} value={r.roleName}>{r.roleName}</SelectItem>))}
                                </SelectContent>
                            </Select>
                        </div>
                        <div className="flex items-center space-x-2">
                            <Checkbox id="edit-active" checked={isActive} onCheckedChange={setIsActive} />
                            <Label htmlFor="edit-active">Account Active</Label>
                        </div>
                        <DialogFooter>
                            <Button type="submit" disabled={createLoading}>Save Changes</Button>
                        </DialogFooter>
                    </form>
                </DialogContent>
            </Dialog>

            {/* Delete Dialog */}
            <Dialog open={isDeleteDialogOpen} onOpenChange={setIsDeleteDialogOpen}>
                <DialogContent className="sm:max-w-[425px]">
                    <DialogHeader>
                        <DialogTitle className="text-destructive">Confirm Deletion</DialogTitle>
                        <DialogDescription>
                            Delete <strong>{employeeToDelete?.name}</strong>? This cannot be undone.
                        </DialogDescription>
                    </DialogHeader>
                    <DialogFooter>
                        <Button variant="outline" onClick={() => setIsDeleteDialogOpen(false)}>Cancel</Button>
                        <Button variant="destructive" onClick={handleDelete}>Delete</Button>
                    </DialogFooter>
                </DialogContent>
            </Dialog>

            {/* Assign Menu Dialog */}
            <Dialog open={isAssignOpen} onOpenChange={setIsAssignOpen}>
                <DialogContent className="sm:max-w-[500px]">
                    <DialogHeader>
                        <DialogTitle>Assign Menu Items</DialogTitle>
                        <DialogDescription>
                            Select products that <strong>{assigningEmployee?.name}</strong> can access in their POS.
                        </DialogDescription>
                    </DialogHeader>

                    {assignLoading ? (
                        <div className="py-12 flex justify-center"><Loader2 className="animate-spin" /></div>
                    ) : (
                        <ScrollArea className="h-[300px] border rounded-md p-4 bg-gray-50/50">
                            <div className="grid grid-cols-1 gap-3">
                                {products.map((product) => (
                                    <div key={product.id} className="flex items-center justify-between p-2 bg-white rounded border shadow-sm">
                                        <div className="flex flex-col">
                                            <span className="font-medium text-sm">{product.name}</span>
                                            <span className="text-[10px] text-muted-foreground uppercase">{product.category}</span>
                                        </div>
                                        <Checkbox
                                            checked={assignedItems.includes(product.id)}
                                            onCheckedChange={() => toggleItemAssignment(product.id)}
                                        />
                                    </div>
                                ))}
                            </div>
                        </ScrollArea>
                    )}

                    <DialogFooter className="flex items-center justify-between sm:justify-between w-full">
                        <div className="text-sm text-muted-foreground">
                            {assignedItems.length} items selected
                        </div>
                        <div className="flex gap-2">
                            <Button variant="outline" onClick={() => setIsAssignOpen(false)}>Cancel</Button>
                            <Button onClick={handleAssignSubmit} disabled={assignLoading}>
                                {assignLoading ? <Loader2 className="animate-spin mr-2" /> : <Check className="mr-2" size={16} />} Save Permissions
                            </Button>
                        </div>
                    </DialogFooter>
                </DialogContent>
            </Dialog>
        </div>
    );
}
