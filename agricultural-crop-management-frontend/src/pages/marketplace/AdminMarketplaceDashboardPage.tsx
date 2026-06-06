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
import { useI18n } from "@/shared/lib/hooks/useI18n";

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

function unavailableReasonLabel(reason: MarketplaceStatsUnavailableReason, t: ReturnType<typeof useI18n>["t"]) {
  switch (reason) {
    case "NO_PRODUCTS":
      return t("admin.marketplace.dashboard.unavailableReasons.noProducts");
    case "NO_ORDERS":
      return t("admin.marketplace.dashboard.unavailableReasons.noOrders");
    case "NO_REVENUE_DATA":
      return t("admin.marketplace.dashboard.unavailableReasons.noRevenueData");
    case "NO_COMPLETED_ORDERS":
      return t("admin.marketplace.dashboard.unavailableReasons.noCompletedOrders");
    default:
      return t("admin.marketplace.dashboard.unavailableReasons.default");
  }
}

export function AdminMarketplaceDashboardPage() {
  const { t } = useI18n();
  const statsQuery = useMarketplaceAdminStats();

  if (statsQuery.isLoading) {
    return (
      <AdminPageContainer>
      <AdminContentCard className="border-dashed">
        <CardContent className="p-8 text-sm text-muted-foreground">
          {t("admin.marketplace.dashboard.loading")}
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
          {t("admin.marketplace.dashboard.loadError")}
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
        title={t("admin.marketplace.dashboard.title")}
        description={t("admin.marketplace.dashboard.description")}
        metadata={<span className="rounded-full bg-primary/10 px-2 py-0.5 text-xs font-medium text-primary">{t("admin.marketplace.common.adminBadge")}</span>}
        actions={
          <div className="flex flex-wrap gap-3 text-sm">
          <Link
            to="/admin/marketplace-products"
            className="rounded-[14px] border border-primary/20 bg-primary/5 px-4 py-2 font-medium text-primary transition-colors hover:bg-primary/10"
          >
            {t("admin.marketplace.dashboard.actions.reviewProducts")}
          </Link>
          <Link
            to="/admin/marketplace-orders"
            className="rounded-[14px] border border-border px-4 py-2 font-medium text-foreground transition-colors hover:bg-muted"
          >
            {t("admin.marketplace.dashboard.actions.reviewOrders")}
          </Link>
          </div>
        }
      />

      {showSystemEmpty ? (
        <AdminContentCard className="border-dashed">
          <CardContent className="space-y-3 p-8 text-center">
            <h2 className="text-xl font-semibold text-foreground">{t("admin.marketplace.dashboard.empty.title")}</h2>
            <p className="mx-auto max-w-2xl text-sm text-muted-foreground">
              {t("admin.marketplace.dashboard.empty.description")}
            </p>
            {unavailableReasons.length > 0 ? (
              <p className="text-xs text-muted-foreground">
                {unavailableReasons.map((reason) => unavailableReasonLabel(reason, t)).join(" ")}
              </p>
            ) : null}
          </CardContent>
        </AdminContentCard>
      ) : null}

      <div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-5">
        <MetricCard
          icon={Package}
          label={t("admin.marketplace.dashboard.metrics.totalProducts")}
          value={stats.totalProducts}
          tone="bg-emerald-100 text-primary"
        />
        <MetricCard
          icon={Package}
          label={t("admin.marketplace.dashboard.metrics.published")}
          value={stats.publishedProducts}
          tone="bg-emerald-100 text-primary"
        />
        <MetricCard
          icon={ShieldAlert}
          label={t("admin.marketplace.dashboard.metrics.pendingReview")}
          value={stats.pendingReviewProducts}
          tone="bg-yellow-100 text-yellow-600"
        />
        <MetricCard
          icon={ShoppingBag}
          label={t("admin.marketplace.dashboard.metrics.totalOrders")}
          value={stats.totalOrders}
          tone="bg-purple-100 text-purple-600"
        />
        <MetricCard
          icon={ShieldAlert}
          label={t("admin.marketplace.dashboard.metrics.paymentProofsPending")}
          value={pendingPaymentVerificationOrders}
          tone={pendingPaymentVerificationOrders > 0 ? "bg-orange-100 text-orange-600" : "bg-muted text-muted-foreground"}
          helperText={pendingPaymentVerificationOrders > 0 ? t("admin.marketplace.dashboard.helpers.requiresVerification") : t("admin.marketplace.dashboard.helpers.noPendingVerification")}
        />
      </div>

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        <AdminContentCard>
          <CardHeader className="border-b border-border/50">
            <CardTitle>{t("admin.marketplace.dashboard.operations.title")}</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4 p-6">
            <div className="flex items-center justify-between rounded-xl border border-border p-4">
              <span className="text-muted-foreground">{t("admin.marketplace.dashboard.operations.activeOrders")}</span>
              <span className="font-semibold text-foreground">{stats.activeOrders}</span>
            </div>
            <div className="flex items-center justify-between rounded-xl border border-border p-4">
              <span className="text-muted-foreground">{t("admin.marketplace.dashboard.operations.completedOrders")}</span>
              <span className="font-semibold text-foreground">{stats.completedOrders}</span>
            </div>
            <div className="flex items-center justify-between rounded-xl border border-border p-4">
              <span className="text-muted-foreground">{t("admin.marketplace.dashboard.operations.cancelledOrders")}</span>
              <span className="font-semibold text-foreground">{stats.cancelledOrders}</span>
            </div>
            <div className="flex items-center justify-between rounded-xl border border-border p-4">
              <span className="text-muted-foreground">{t("admin.marketplace.dashboard.operations.hiddenProducts")}</span>
              <span className="font-semibold text-foreground">{stats.hiddenProducts}</span>
            </div>
            <div className="flex items-center justify-between rounded-xl border border-border p-4">
              <span className="text-muted-foreground">{t("admin.marketplace.dashboard.operations.lastOrderActivity")}</span>
              <span className="font-semibold text-foreground">
                {stats.lastOrderAt ? formatDateTime(stats.lastOrderAt) : t("admin.marketplace.dashboard.operations.noOrdersYet")}
              </span>
            </div>
          </CardContent>
        </AdminContentCard>

        <AdminContentCard>
          <CardHeader className="border-b border-border/50">
            <CardTitle>{t("admin.marketplace.dashboard.revenue.title")}</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4 p-6">
            <div className="rounded-xl border border-border p-5">
              <div className="flex items-center gap-3">
                <div className="rounded-xl bg-emerald-100 p-3 text-primary">
                  <Wallet size={20} />
                </div>
                <div>
                  <p className="text-sm text-muted-foreground">{t("admin.marketplace.dashboard.revenue.marketplaceRevenue")}</p>
                  <p className="mt-1 text-3xl font-bold text-primary">
                    {hasRevenueData && stats.totalRevenue != null ? formatVnd(stats.totalRevenue) : "--"}
                  </p>
                  {!hasRevenueData ? (
                    <p className="mt-1 text-xs text-muted-foreground">
                      {t("admin.marketplace.dashboard.revenue.emptyRevenue")}
                    </p>
                  ) : null}
                </div>
              </div>
            </div>

            {pendingPaymentVerificationOrders > 0 ? (
              <div className="rounded-2xl border border-orange-200 bg-orange-50 p-5">
                <p className="text-sm font-medium text-orange-800">{t("admin.marketplace.dashboard.revenue.pendingPaymentVerifications")}</p>
                <p className="mt-2 text-2xl font-bold text-foreground">{pendingPaymentVerificationOrders}</p>
                <p className="mt-2 text-sm text-muted-foreground">
                  {t("admin.marketplace.dashboard.revenue.pendingPaymentDescription")}
                </p>
              </div>
            ) : null}

            <div className="rounded-2xl border border-yellow-200 bg-yellow-50 p-5">
              <p className="text-sm font-medium text-yellow-800">{t("admin.marketplace.dashboard.revenue.pendingProductApprovals")}</p>
              <p className="mt-2 text-2xl font-bold text-foreground">{stats.pendingReviewProducts}</p>
              <p className="mt-2 text-sm text-muted-foreground">
                {t("admin.marketplace.dashboard.revenue.pendingProductDescription")}
              </p>
            </div>
          </CardContent>
        </AdminContentCard>
      </div>
    </AdminPageContainer>
  );
}
