import { Package, ShieldAlert, ShoppingBag, Wallet } from "lucide-react";
import { Link } from "react-router-dom";
import type { MarketplaceStatsUnavailableReason } from "@/shared/api";
import { Card, CardContent, CardHeader, CardTitle } from "@/shared/ui";
import { useMarketplaceAdminStats } from "@/features/marketplace/hooks";
import { formatDateTime, formatVnd } from "@/features/marketplace/lib/format";

function MetricCard({
  icon: Icon,
  label,
  value,
  tone,
  helperText,
}: {
  icon: typeof Package;
  label: string;
  value: string | number;
  tone: string;
  helperText?: string;
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
          {helperText ? (
            <p className="mt-1 text-xs text-muted-foreground">{helperText}</p>
          ) : null}
        </div>
      </CardContent>
    </Card>
  );
}

function unavailableReasonLabel(reason: MarketplaceStatsUnavailableReason) {
  switch (reason) {
    case "NO_PRODUCTS":
      return "No marketplace products are available in the system yet.";
    case "NO_ORDERS":
      return "No marketplace orders have been created yet.";
    case "NO_REVENUE_DATA":
      return "Revenue data is unavailable because there are no orders yet.";
    case "NO_COMPLETED_ORDERS":
      return "Revenue data is unavailable because no order has reached completed status.";
    default:
      return "Some marketplace metrics are unavailable.";
  }
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
  const hasProducts = stats.hasProducts;
  const hasOrders = stats.hasOrders;
  const hasRevenueData = stats.hasRevenueData;
  const showSystemEmpty = !hasProducts && !hasOrders;
  const pendingPaymentVerificationOrders = stats.pendingPaymentVerificationOrders;
  const unavailableReasons = stats.unavailableReasons;

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <p className="text-sm font-medium text-primary">FarmTrace Admin</p>
          <h1 className="mt-1 text-3xl font-bold text-foreground">Marketplace dashboard</h1>
          <p className="mt-2 max-w-2xl text-sm text-muted-foreground">
            Live marketplace metrics only. Empty states are shown when product, order, or revenue data is not available yet.
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

      {showSystemEmpty ? (
        <Card className="border-dashed border-border">
          <CardContent className="space-y-3 p-8 text-center">
            <h2 className="text-xl font-semibold text-foreground">Marketplace has no product or order yet</h2>
            <p className="mx-auto max-w-2xl text-sm text-muted-foreground">
              Admin stats are live and currently empty because sellers have not published products and buyers have not placed orders.
            </p>
            {unavailableReasons.length > 0 ? (
              <p className="text-xs text-muted-foreground">
                {unavailableReasons.map(unavailableReasonLabel).join(" ")}
              </p>
            ) : null}
          </CardContent>
        </Card>
      ) : null}

      <div className="grid grid-cols-1 gap-6 md:grid-cols-2 xl:grid-cols-5">
        <MetricCard
          icon={Package}
          label="Total products"
          value={stats.totalProducts}
          tone="bg-emerald-100 text-primary"
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
        <MetricCard
          icon={ShieldAlert}
          label="Payment proofs pending"
          value={pendingPaymentVerificationOrders}
          tone={pendingPaymentVerificationOrders > 0 ? "bg-orange-100 text-orange-600" : "bg-muted text-muted-foreground"}
          helperText={pendingPaymentVerificationOrders > 0 ? "Requires admin verification." : "No pending verification."}
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
            <div className="flex items-center justify-between rounded-xl border border-border p-4">
              <span className="text-muted-foreground">Last order activity</span>
              <span className="font-semibold text-foreground">
                {stats.lastOrderAt ? formatDateTime(stats.lastOrderAt) : "No orders yet"}
              </span>
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
                  <p className="mt-1 text-3xl font-bold text-primary">
                    {hasRevenueData && stats.totalRevenue != null ? formatVnd(stats.totalRevenue) : "--"}
                  </p>
                  {!hasRevenueData ? (
                    <p className="mt-1 text-xs text-muted-foreground">
                      Revenue metric is empty until completed orders are available.
                    </p>
                  ) : null}
                </div>
              </div>
            </div>

            {pendingPaymentVerificationOrders > 0 ? (
              <div className="rounded-2xl border border-orange-200 bg-orange-50 p-5">
                <p className="text-sm font-medium text-orange-800">Pending payment verifications</p>
                <p className="mt-2 text-2xl font-bold text-foreground">{pendingPaymentVerificationOrders}</p>
                <p className="mt-2 text-sm text-muted-foreground">
                  Review bank transfer proof submissions in the order moderation screen.
                </p>
              </div>
            ) : null}

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
