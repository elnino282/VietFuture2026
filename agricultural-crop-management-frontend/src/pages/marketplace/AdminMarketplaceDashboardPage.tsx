import { Package, ShieldAlert, ShoppingBag, Wallet } from "lucide-react";
import { Link } from "react-router-dom";
import type { MarketplaceStatsUnavailableReason } from "@/shared/api";
import { CardContent, CardHeader, CardTitle } from "@/shared/ui";
import { useMarketplaceAdminStats } from "@/features/marketplace/hooks";
import { formatDateTime, formatVnd } from "@/features/marketplace/lib/format";
import {
  AdminContentCard,
  AdminHeaderCard,
  AdminMetricCard,
  AdminPageContainer,
} from "@/features/admin/shared/ui";

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
    <AdminMetricCard
      label={label}
      value={value}
      helper={helperText}
      icon={Icon}
      iconWrapClassName={tone}
    />
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
      <AdminPageContainer>
      <AdminContentCard className="border-dashed">
        <CardContent className="p-8 text-sm text-muted-foreground">
          Loading marketplace admin dashboard...
        </CardContent>
      </AdminContentCard>
      </AdminPageContainer>
    );
  }

  if (statsQuery.isError || !statsQuery.data) {
    return (
      <AdminPageContainer>
      <AdminContentCard className="border-destructive/30">
        <CardContent className="p-8 text-sm text-destructive">
          Failed to load marketplace admin stats.
        </CardContent>
      </AdminContentCard>
      </AdminPageContainer>
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
    <AdminPageContainer>
      <AdminHeaderCard
        title="Marketplace dashboard"
        description="Live marketplace metrics only. Empty states are shown when product, order, or revenue data is not available yet."
        metadata={<span className="rounded-full bg-primary/10 px-2 py-0.5 text-xs font-medium text-primary">FarmTrace Admin</span>}
        actions={
          <div className="flex flex-wrap gap-3 text-sm">
          <Link
            to="/admin/marketplace-products"
            className="rounded-[14px] border border-primary/20 bg-primary/5 px-4 py-2 font-medium text-primary transition-colors hover:bg-primary/10"
          >
            Review products
          </Link>
          <Link
            to="/admin/marketplace-orders"
            className="rounded-[14px] border border-border px-4 py-2 font-medium text-foreground transition-colors hover:bg-muted"
          >
            Review orders
          </Link>
          </div>
        }
      />

      {showSystemEmpty ? (
        <AdminContentCard className="border-dashed">
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
        </AdminContentCard>
      ) : null}

      <div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-5">
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
        <AdminContentCard>
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
        </AdminContentCard>

        <AdminContentCard>
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
        </AdminContentCard>
      </div>
    </AdminPageContainer>
  );
}
