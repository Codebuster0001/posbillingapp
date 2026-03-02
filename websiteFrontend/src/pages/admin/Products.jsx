import { useState, useEffect } from "react";
import { useSelector } from "react-redux";
import { getProducts, createProduct, updateProduct, deleteProduct, getCategories } from "@/lib/api";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Plus, Loader2, AlertCircle, Package, Edit, Trash2, Image as ImageIcon } from "lucide-react";
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
import { Textarea } from "@/components/ui/textarea";
import { Checkbox } from "@/components/ui/checkbox";
import { Badge } from "@/components/ui/badge";

export default function Products() {
    const { user, permissions } = useSelector((state) => state.auth);
    const [products, setProducts] = useState([]);
    const [categories, setCategories] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [createLoading, setCreateLoading] = useState(false);

    const [isDialogOpen, setIsDialogOpen] = useState(false);
    const [isEditDialogOpen, setIsEditDialogOpen] = useState(false);
    const [isDeleteDialogOpen, setIsDeleteDialogOpen] = useState(false);

    const [editingProduct, setEditingProduct] = useState(null);
    const [productToDelete, setProductToDelete] = useState(null);

    // Form State
    const [name, setName] = useState("");
    const [price, setPrice] = useState("");
    const [category, setCategory] = useState("");
    const [description, setDescription] = useState("");
    const [imageFile, setImageFile] = useState(null);
    const [isAvailable, setIsAvailable] = useState(true);

    const fetchProducts = async () => {
        if (!user?.companyId) return;
        try {
            setLoading(true);
            const data = await getProducts(user.companyId);
            setProducts(data);
        } catch (err) {
            setError(err.message);
        } finally {
            setLoading(false);
        }
    };

    const fetchCategories = async () => {
        try {
            const data = await getCategories();
            setCategories(data);
            if (data.length > 0) setCategory(data[0].categoryName || data[0]);
        } catch (err) {
            console.error("Failed to load categories", err);
        }
    };

    useEffect(() => {
        if (user?.companyId) {
            fetchProducts();
            fetchCategories();
        }
    }, [user?.companyId]);

    const handleCreate = async (e) => {
        e.preventDefault();
        setError(null);
        setCreateLoading(true);

        const formData = new FormData();
        formData.append("Name", name);
        formData.append("Price", price);
        formData.append("Category", category);
        formData.append("Description", description);
        formData.append("IsAvailable", isAvailable);
        formData.append("CompanyId", user.companyId);
        if (imageFile) {
            formData.append("imageFile", imageFile);
        }

        try {
            await createProduct(formData);
            setIsDialogOpen(false);
            await fetchProducts();
            resetForm();
        } catch (err) {
            setError(err.message);
        } finally {
            setCreateLoading(false);
        }
    };

    const handleUpdate = async (e) => {
        e.preventDefault();
        if (!editingProduct) return;
        setError(null);
        setCreateLoading(true);

        const formData = new FormData();
        formData.append("Name", name);
        formData.append("Price", price);
        formData.append("Category", category);
        formData.append("Description", description);
        formData.append("IsAvailable", isAvailable);
        formData.append("CompanyId", user.companyId);
        if (imageFile) {
            formData.append("imageFile", imageFile);
        }

        try {
            await updateProduct(editingProduct.id, formData);
            setIsEditDialogOpen(false);
            setEditingProduct(null);
            await fetchProducts();
            resetForm();
        } catch (err) {
            setError(err.message);
        } finally {
            setCreateLoading(false);
        }
    };

    const handleDelete = async () => {
        if (!productToDelete) return;
        try {
            await deleteProduct(productToDelete.id);
            setProductToDelete(null);
            setIsDeleteDialogOpen(false);
            await fetchProducts();
        } catch (err) {
            setError(err.message);
        }
    };

    const confirmDelete = (product) => {
        setProductToDelete(product);
        setIsDeleteDialogOpen(true);
    };

    const resetForm = () => {
        setName("");
        setPrice("");
        setCategory(categories.length > 0 ? (categories[0].categoryName || categories[0]) : "");
        setDescription("");
        setImageFile(null);
        setIsAvailable(true);
    };

    const openEditDialog = (product) => {
        setEditingProduct(product);
        setName(product.name);
        setPrice(product.price.toString());
        setCategory(product.category);
        setDescription(product.description || "");
        setIsAvailable(product.isAvailable);
        setImageFile(null);
        setIsEditDialogOpen(true);
    };

    const openCreateDialog = () => {
        resetForm();
        setIsDialogOpen(true);
    };

    const renderProductFormFields = () => (
        <div className="space-y-6">
            <div className="grid grid-cols-4 items-center gap-4">
                <Label htmlFor="name" className="text-right">Name</Label>
                <Input id="name" value={name} onChange={(e) => setName(e.target.value)} className="col-span-3" placeholder="e.g. Burger" required />
            </div>
            <div className="grid grid-cols-4 items-center gap-4">
                <Label htmlFor="price" className="text-right">Price</Label>
                <Input id="price" type="number" step="0.01" value={price} onChange={(e) => setPrice(e.target.value)} className="col-span-3" placeholder="0.00" required />
            </div>
            <div className="grid grid-cols-4 items-center gap-4">
                <Label htmlFor="category" className="text-right">Category</Label>
                <Select value={category} onValueChange={setCategory}>
                    <SelectTrigger className="col-span-3">
                        <SelectValue placeholder="Select Category" />
                    </SelectTrigger>
                    <SelectContent>
                        {categories.map((cat, idx) => (
                            <SelectItem key={idx} value={cat.categoryName || cat}>
                                {cat.categoryName || cat}
                            </SelectItem>
                        ))}
                    </SelectContent>
                </Select>
            </div>
            <div className="grid grid-cols-4 items-center gap-4">
                <Label htmlFor="description" className="text-right">Description</Label>
                <Textarea id="description" value={description} onChange={(e) => setDescription(e.target.value)} className="col-span-3" placeholder="Optional description..." />
            </div>
            <div className="grid grid-cols-4 items-center gap-4">
                <Label htmlFor="image" className="text-right">Image</Label>
                <Input id="image" type="file" accept="image/*" onChange={(e) => setImageFile(e.target.files[0])} className="col-span-3" />
            </div>
            <div className="grid grid-cols-4 items-center gap-4">
                <Label htmlFor="available" className="text-right">Available</Label>
                <div className="col-span-3 flex items-center">
                    <Checkbox id="available" checked={isAvailable} onCheckedChange={(val) => setIsAvailable(val === true)} />
                    <Label htmlFor="available" className="ml-2 text-xs text-muted-foreground">Product will be visible in menu</Label>
                </div>
            </div>
        </div>
    );

    const canManage = permissions?.includes("MANAGE_PRODUCTS") || user?.role === "Admin";

    return (
        <div className="space-y-6 w-full">
            <header className="flex flex-col sm:flex-row sm:justify-between sm:items-center gap-4">
                <div>
                    <h1 className="text-2xl sm:text-3xl font-bold tracking-tight">Product Management</h1>
                    <p className="text-muted-foreground text-sm">Manage your menu items and prices.</p>
                </div>

                {canManage && (
                    <Button onClick={openCreateDialog} className="w-full sm:w-auto">
                        <Plus className="h-4 w-4" />Add Product
                    </Button>
                )}
            </header>

            {error && (
                <Alert variant="destructive">
                    <AlertCircle className="h-4 w-4" />
                    <AlertTitle>Error</AlertTitle>
                    <AlertDescription>{error}</AlertDescription>
                </Alert>
            )}

            <Card className="shadow-sm bg-white w-full">
                <CardHeader>
                    <CardTitle>Product List</CardTitle>
                    <CardDescription>View and manage your current offerings.</CardDescription>
                </CardHeader>
                <CardContent>
                    {loading ? (
                        <div className="flex justify-center p-8">
                            <Loader2 className="animate-spin text-primary" size={32} />
                        </div>
                    ) : products.length === 0 ? (
                        <div className="text-center p-8 text-muted-foreground">No products found.</div>
                    ) : (
                        <div className="overflow-x-auto">
                            <Table>
                                <TableHeader>
                                    <TableRow>
                                        <TableHead className="w-[10%]">Image</TableHead>
                                        <TableHead>Name</TableHead>
                                        <TableHead className="text-center">Category</TableHead>
                                        <TableHead className="text-center">Price</TableHead>
                                        <TableHead className="text-center">Status</TableHead>
                                        {canManage && <TableHead className="text-center w-[120px]">Actions</TableHead>}
                                    </TableRow>
                                </TableHeader>
                                <TableBody>
                                    {products.map((product) => (
                                        <TableRow key={product.id}>
                                            <TableCell>
                                                <div className="w-10 h-10 rounded-md overflow-hidden bg-gray-50 border flex items-center justify-center">
                                                    {product.imageUrl ? (
                                                        <img src={product.imageUrl} alt={product.name} className="w-full h-full object-cover" />
                                                    ) : (
                                                        <Package className="text-gray-300" size={18} />
                                                    )}
                                                </div>
                                            </TableCell>
                                            <TableCell className="font-medium">{product.name}</TableCell>
                                            <TableCell className="text-center">{product.category}</TableCell>
                                            <TableCell className="text-center font-medium">{user?.currencySymbol || "₹"} {product.price}</TableCell>
                                            <TableCell className="text-center">
                                                <Badge variant={product.isAvailable ? "success" : "secondary"}>
                                                    {product.isAvailable ? "Available" : "Hidden"}
                                                </Badge>
                                            </TableCell>
                                            {canManage && (
                                                <TableCell className="text-right">
                                                    <div className="flex justify-end gap-2">
                                                        <Button variant="ghost" size="icon" onClick={() => openEditDialog(product)}>
                                                            <Edit className="h-4 w-4" />
                                                        </Button>
                                                        <Button variant="ghost" size="icon" className="text-destructive hover:bg-destructive/10" onClick={() => confirmDelete(product)}>
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

            {/* ADD DIALOG */}
            <Dialog open={isDialogOpen} onOpenChange={setIsDialogOpen}>
                <DialogContent>
                    <DialogHeader>
                        <DialogTitle>Add New Product</DialogTitle>
                        <DialogDescription>Create a new item for your menu.</DialogDescription>
                    </DialogHeader>
                    <form onSubmit={handleCreate}>
                        {renderProductFormFields()}
                        <DialogFooter>
                            <Button type="button" variant="outline" onClick={() => setIsDialogOpen(false)}>Cancel</Button>
                            <Button type="submit" disabled={createLoading}>
                                {createLoading ? <Loader2 className="animate-spin mr-2" /> : "Save Product"}
                            </Button>
                        </DialogFooter>
                    </form>
                </DialogContent>
            </Dialog>

            {/* EDIT DIALOG */}
            <Dialog open={isEditDialogOpen} onOpenChange={setIsEditDialogOpen}>
                <DialogContent>
                    <DialogHeader>
                        <DialogTitle>Edit Product</DialogTitle>
                        <DialogDescription>Update product details and availability.</DialogDescription>
                    </DialogHeader>
                    <form onSubmit={handleUpdate}>
                        {renderProductFormFields()}
                        <DialogFooter>
                            <Button type="button" variant="outline" onClick={() => setIsEditDialogOpen(false)}>Cancel</Button>
                            <Button type="submit" disabled={createLoading}>
                                {createLoading ? <Loader2 className="animate-spin mr-2" /> : "Update Product"}
                            </Button>
                        </DialogFooter>
                    </form>
                </DialogContent>
            </Dialog>

            {/* DELETE DIALOG */}
            <Dialog open={isDeleteDialogOpen} onOpenChange={setIsDeleteDialogOpen}>
                <DialogContent>
                    <DialogHeader>
                        <DialogTitle>Delete Product</DialogTitle>
                        <DialogDescription>Are you sure you want to delete {productToDelete?.name}?</DialogDescription>
                    </DialogHeader>
                    <DialogFooter>
                        <Button variant="outline" onClick={() => setIsDeleteDialogOpen(false)}>Cancel</Button>
                        <Button variant="destructive" onClick={handleDelete}>Confirm Delete</Button>
                    </DialogFooter>
                </DialogContent>
            </Dialog>
        </div>
    );
}
