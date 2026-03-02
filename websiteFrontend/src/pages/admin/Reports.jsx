import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";

export default function Reports() {
    return (
        <div className="space-y-6">
            <header>
                <h1 className="text-3xl font-bold tracking-tight">Sales Reports</h1>
                <p className="text-muted-foreground text-sm">Analyze your business performance.</p>
            </header>
            <Card className="bg-white">
                <CardHeader>
                    <CardTitle>Coming Soon</CardTitle>
                    <CardDescription>We are working on detailed analytics for your company.</CardDescription>
                </CardHeader>
                <CardContent className="h-64 flex items-center justify-center text-muted-foreground">
                    Detailed graphs and exports will be available here.
                </CardContent>
            </Card>
        </div>
    );
}
