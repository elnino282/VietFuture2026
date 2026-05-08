import { Package, ShieldAlert, ShoppingBag, Wallet } from "lucide-react";
import { Link } from "react-router-dom";
import { Card, CardContent, CardHeader, CardTitle } from "@/shared/ui";
import { useMarketplaceAdminStats } from "@/features/marketplace/hooks";
import { formatVnd } from "@/features/marketplace/lib/format";

function MetricCard({
  icon: Icon,
  label,
  value,
  tone,
}: {
  icon: typeof Package;
  label: string;
  value: string | number;
  tone: string;
}) {
  return (
    <Card className="border-border shadow-sm">
      <CardContent className="flex items-center gap-4 p-6">
        <div className={`rounded-xl p-3 ${tone}`}>
          <Icon size={22} />
        </div>
        <div>
          <p className="text-sm font-medium text-muted-foreground">{label}</p>
          <p className="mt-1 text-2xl font-bold text-foreground">{value}</p>
        </div>
      </CardContent>
    </Card>
  );
}

export function AdminMarketplaceDashboardPage() {
  const statsQuery = useMarketplaceAdminStats();

  if (statsQuery.isLoading) {
    return (
      <Card className="border-dashed">
        <CardContent className="p-8 text-sm text-muted-foreground">
          Loading marketplace admin dashboard...
        </CardContent>
      </Card>
    );
  }

  if (statsQuery.isError || !statsQuery.data) {
    return (
      <Card className="border-destructive/30">
        <CardContent className="p-8 text-sm text-destructive">
          Failed to load marketplace admin stats.
        </CardContent>
      </Card>
    );
  }

  const stats = statsQuery.data;

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <p className="text-sm font-medium text-primary">FarmTrace Admin</p>
          <h1 className="mt-1 text-3xl font-bold text-foreground">Marketplace dashboard</h1>
          <p className="mt-2 max-w-2xl text-sm text-muted-foreground">
            Restore the older moderation dashboard feel while keeping the current live statistics and admin workflows.
          </p>
        </div>
        <div className="flex flex-wrap gap-3 text-sm">
          <Link
            to="/admin/marketplace-products"
            className="rounded-full border border-primary/20 bg-primary/5 px-4 py-2 font-medium text-primary transition-colors hover:bg-primary/10"
          >
            Review products
          </Link>
          <Link
            to="/admin/marketplace-orders"
            className="rounded-full border border-border px-4 py-2 font-medium text-foreground transition-colors hover:bg-muted"
          >
            Review orders
          </Link>
        </div>
      </div>

      <div className="grid grid-cols-1 gap-6 md:grid-cols-2 xl:grid-cols-4">
        <MetricCard
          icon={Package}
          label="Total products"
          value={stats.totalProducts}
          tone="bg-blue-100 text-blue-600"
        />
        <MetricCard
          icon={Package}
          label="Published"
          value={stats.publishedProducts}
          tone="bg-emerald-100 text-primary"
        />
        <MetricCard
          icon={ShieldAlert}
          label="Pending review"
          value={stats.pendingReviewProducts}
          tone="bg-yellow-100 text-yellow-600"
        />
        <MetricCard
          icon={ShoppingBag}
          label="Total orders"
          value={stats.totalOrders}
          tone="bg-purple-100 text-purple-600"
        />
      </div>

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        <Card className="border-border shadow-sm">
          <CardHeader className="border-b border-border/50">
            <CardTitle>Operations summary</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4 p-6">
            <div className="flex items-center justify-between rounded-xl border border-border p-4">
              <span className="text-muted-foreground">Active orders</span>
              <span className="font-semibold text-foreground">{stats.activeOrders}</span>
            </div>
            <div className="flex items-center justify-between rounded-xl border border-border p-4">
              <span className="text-muted-foreground">Completed orders</span>
              <span className="font-semibold text-foreground">{stats.completedOrders}</span>
            </div>
            <div className="flex items-center justify-between rounded-xl border border-border p-4">
              <span className="text-muted-foreground">Cancelled orders</span>
              <span className="font-semibold text-foreground">{stats.cancelledOrders}</span>
            </div>
            <div className="flex items-center justify-between rounded-xl border border-border p-4">
              <span className="text-muted-foreground">Hidden products</span>
              <span className="font-semibold text-foreground">{stats.hiddenProducts}</span>
            </div>
          </CardContent>
        </Card>

        <Card className="border-border shadow-sm">
          <CardHeader className="border-b border-border/50">
            <CardTitle>Revenue and moderation</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4 p-6">
            <div className="rounded-xl border border-border p-5">
              <div className="flex items-center gap-3">
                <div className="rounded-xl bg-emerald-100 p-3 text-primary">
                  <Wallet size={20} />
                </div>
                <div>
                  <p className="text-sm text-muted-foreground">Marketplace revenue</p>
                  <p className="mt-1 text-3xl font-bold text-primary">{formatVnd(stats.totalRevenue)}</p>
                </div>
              </div>
            </div>

            <div className="rounded-2xl border border-yellow-200 bg-yellow-50 p-5">
              <p className="text-sm font-medium text-yellow-800">Pending product approvals</p>
              <p className="mt-2 text-2xl font-bold text-foreground">{stats.pendingReviewProducts}</p>
              <p className="mt-2 text-sm text-muted-foreground">
                Use the moderation screen to publish, hide, or return listings to review.
              </p>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
