import {
  DollarSign,
  Package,
  Plus,
  ShoppingBag,
  Store,
} from "lucide-react";
import { Link, useNavigate } from "react-router-dom";
// eslint-disable-next-line no-restricted-imports
import { useI18n } from "@/hooks/useI18n";
import type { MarketplaceStatsUnavailableReason } from "@/shared/api";
import { AsyncState, Button, Card, CardContent, CardHeader, CardTitle, PageContainer } from "@/shared/ui";
import { useMarketplaceFarmerDashboard, useMarketplaceFarmerProducts } from "@/features/marketplace/hooks";
import { SellerMarketplaceTabs } from "@/features/marketplace/layout";
import { formatDateTime, formatVnd } from "@/features/marketplace/lib/format";
import { getMarketplaceOrderStatusLabel } from "@/features/marketplace/lib/orderStatus";

type Translator = (key: string, optionsOrDefault?: Record<string, unknown> | string) => string;

function MetricCard({
  icon: Icon,
  label,
  value,
  tone,
  helperText,
}: {
  icon: typeof DollarSign;
  label: string;
  value: string | number;
  tone: string;
  helperText?: string;
}) {
  return (
    <Card className="rounded-lg border-border shadow-sm">
      <CardContent className="flex items-center gap-4 p-5">
        <div className={`rounded-lg p-3 ${tone}`}>
          <Icon size={21} />
        </div>
        <div>
          <p className="text-sm font-medium text-muted-foreground">{label}</p>
          <p className="mt-1 text-xl font-bold text-foreground md:text-2xl">{value}</p>
          {helperText ? (
            <p className="mt-1 text-xs text-muted-foreground">{helperText}</p>
          ) : null}
        </div>
      </CardContent>
    </Card>
  );
}

function orderStatusLabel(status: string, t: Translator) {
  return getMarketplaceOrderStatusLabel(status, t, "marketplaceSeller.status.order");
}

function unavailableReasonLabel(reason: MarketplaceStatsUnavailableReason, t: Translator) {
  switch (reason) {
    case "NO_PRODUCTS":
      return t(
        "marketplaceSeller.dashboard.unavailableReasons.noProducts",
        "No products in your marketplace catalog yet.",
      );
    case "NO_ORDERS":
      return t(
        "marketplaceSeller.dashboard.unavailableReasons.noOrders",
        "No buyer orders have been created yet.",
      );
    case "NO_REVENUE_DATA":
      return t(
        "marketplaceSeller.dashboard.unavailableReasons.noRevenueData",
        "Revenue data is unavailable because there are no orders yet.",
      );
    case "NO_COMPLETED_ORDERS":
      return t(
        "marketplaceSeller.dashboard.unavailableReasons.noCompletedOrders",
        "Revenue remains unavailable until at least one order is completed.",
      );
    default:
      return t(
        "marketplaceSeller.dashboard.unavailableReasons.fallback",
        "Some dashboard metrics are unavailable.",
      );
  }
}

export function SellerDashboardPage() {
  const navigate = useNavigate();
  const { t, locale } = useI18n();
  const dashboardQuery = useMarketplaceFarmerDashboard();
  const productsQuery = useMarketplaceFarmerProducts({ page: 0, size: 20, status: "ACTIVE" });

  const dashboard = dashboardQuery.data;
  const hasProducts = dashboard?.hasProducts ?? false;
  const hasOrders = dashboard?.hasOrders ?? false;
  const hasRevenueData = dashboard?.hasRevenueData ?? false;
  const unavailableReasons = dashboard?.unavailableReasons ?? [];

  const topProducts = (productsQuery.data?.items ?? [])
    .slice()
    .sort((left, right) => right.availableQuantity - left.availableQuantity)
    .slice(0, 5);

  return (
    <PageContainer variant="wide">
      <div className="space-y-4 md:space-y-5">
        <SellerMarketplaceTabs />

        <AsyncState
          isLoading={dashboardQuery.isLoading}
          isEmpty={false}
          error={dashboardQuery.isError ? (dashboardQuery.error as Error) : null}
          onRetry={() => dashboardQuery.refetch()}
          loadingText={t("marketplaceSeller.dashboard.loading", "Loading marketplace dashboard...")}
        >


          {!hasProducts ? (
            <Card className="rounded-lg border-dashed border-border">
              <CardContent className="space-y-4 p-8 text-center">
                <h2 className="text-xl font-semibold text-foreground">
                  {t("marketplaceSeller.dashboard.emptyProducts.title", "No marketplace product yet")}
                </h2>
                <p className="mx-auto max-w-2xl text-sm text-muted-foreground">
                  {t(
                    "marketplaceSeller.dashboard.emptyProducts.description",
                    "Add your first product listing to unlock order and revenue metrics on this dashboard.",
                  )}
                </p>
                <div className="flex flex-wrap justify-center gap-3">
                  <Button onClick={() => navigate("/farmer/marketplace-products/new")}>
                    <Plus className="mr-2 h-4 w-4" />
                    {t("marketplaceSeller.dashboard.emptyProducts.actions.createFirst", "Create first product")}
                  </Button>
                  <Button asChild variant="outline">
                    <Link to="/farmer/marketplace-products">
                      {t("marketplaceSeller.dashboard.emptyProducts.actions.goToListings", "Go to listings")}
                    </Link>
                  </Button>
                </div>
                {unavailableReasons.length > 0 ? (
                  <p className="text-xs text-muted-foreground">
                    {unavailableReasons.map((reason) => unavailableReasonLabel(reason, t)).join(" ")}
                  </p>
                ) : null}
              </CardContent>
            </Card>
          ) : null}

          {hasProducts && !hasOrders ? (
            <Card className="rounded-lg border-dashed border-border">
              <CardContent className="space-y-3 p-8 text-center">
                <h2 className="text-xl font-semibold text-foreground">
                  {t("marketplaceSeller.dashboard.noOrders.title", "Products are live, but no orders yet")}
                </h2>
                <p className="mx-auto max-w-2xl text-sm text-muted-foreground">
                  {t(
                    "marketplaceSeller.dashboard.noOrders.description",
                    "Keep listings updated and in stock. Buyer orders will appear here as soon as they are placed.",
                  )}
                </p>
                {unavailableReasons.length > 0 ? (
                  <p className="text-xs text-muted-foreground">
                    {unavailableReasons.map((reason) => unavailableReasonLabel(reason, t)).join(" ")}
                  </p>
                ) : null}
              </CardContent>
            </Card>
          ) : null}

          <div className="grid grid-cols-1 gap-6 md:grid-cols-2 xl:grid-cols-4">
            <MetricCard
              icon={DollarSign}
              label={t("marketplaceSeller.dashboard.metrics.revenue", "Revenue")}
              value={
                hasRevenueData && dashboard?.totalRevenue != null
                  ? formatVnd(dashboard.totalRevenue, locale)
                  : "--"
              }
              helperText={
                hasRevenueData
                  ? undefined
                  : t("marketplaceSeller.dashboard.metrics.revenueEmpty", "No completed orders yet.")
              }
              tone="bg-primary/10 text-primary"
            />
            <MetricCard
              icon={ShoppingBag}
              label={t("marketplaceSeller.dashboard.metrics.pendingOrders", "Pending orders")}
              value={dashboard?.pendingOrders ?? "--"}
              tone="bg-secondary/15 text-secondary"
            />
            <MetricCard
              icon={Package}
              label={t("marketplaceSeller.dashboard.metrics.publishedProducts", "Published products")}
              value={dashboard?.publishedProducts ?? "--"}
              tone="bg-accent/20 text-accent"
            />
            <MetricCard
              icon={Store}
              label={t("marketplaceSeller.dashboard.metrics.pendingReview", "Pending review")}
              value={dashboard?.pendingReviewProducts ?? "--"}
              tone="bg-muted text-foreground"
            />
          </div>

          <div className="grid grid-cols-1 gap-6 xl:grid-cols-2">
            <Card className="overflow-hidden rounded-lg border-border shadow-sm">
              <CardHeader className="border-b border-border/50 px-5 pt-5">
                <div className="flex items-center justify-between gap-3">
                  <CardTitle>{t("marketplaceSeller.dashboard.recentOrders.title", "Recent orders")}</CardTitle>
                  <Link to="/farmer/marketplace-orders" className="text-sm font-medium text-primary hover:underline">
                    {t("marketplaceSeller.dashboard.recentOrders.seeAll", "See all")}
                  </Link>
                </div>
              </CardHeader>
              <CardContent className="space-y-3 p-5">
                {(dashboard?.recentOrders?.length ?? 0) > 0 ? (
                  dashboard!.recentOrders.map((order) => (
                    <Link
                      key={order.id}
                      to={`/farmer/marketplace-orders/${order.id}`}
                      className="flex items-center justify-between rounded-lg border border-border p-4 transition-colors hover:bg-muted/50"
                    >
                      <div>
                        <p className="font-semibold text-foreground">{order.orderCode}</p>
                        <p className="text-sm text-muted-foreground">
                          {t("marketplaceSeller.dashboard.recentOrders.itemCount", {
                            count: order.items.length,
                            defaultValue: "{{count}} items",
                          })}{" "}
                          - {formatDateTime(order.createdAt, locale)}
                        </p>
                      </div>
                      <div className="text-right">
                        <p className="font-semibold text-primary">{formatVnd(order.totalAmount, locale)}</p>
                        <p className="text-sm text-muted-foreground">{orderStatusLabel(order.status, t)}</p>
                      </div>
                    </Link>
                  ))
                ) : (
                  <div className="rounded-lg border border-dashed border-border px-4 py-8 text-center text-sm text-muted-foreground">
                    {hasProducts
                      ? t("marketplaceSeller.dashboard.recentOrders.emptyWithProducts", "No buyer orders yet.")
                      : t(
                          "marketplaceSeller.dashboard.recentOrders.emptyNoProducts",
                          "Buyer orders will appear after your first product is listed.",
                        )}
                  </div>
                )}
              </CardContent>
            </Card>

            <Card className="overflow-hidden rounded-lg border-border shadow-sm">
              <CardHeader className="border-b border-border/50 px-5 pt-5">
                <div className="flex items-center justify-between gap-3">
                  <CardTitle>{t("marketplaceSeller.dashboard.topProducts.title", "Top products")}</CardTitle>
                  <Link to="/farmer/marketplace-products" className="text-sm font-medium text-primary hover:underline">
                    {t("marketplaceSeller.dashboard.topProducts.manageProducts", "Manage products")}
                  </Link>
                </div>
              </CardHeader>
              <CardContent className="space-y-3 p-5">
                {topProducts.length > 0 ? (
                  topProducts.map((product) => (
                    <div key={product.id} className="flex items-center gap-4 rounded-lg border border-border p-4">
                      <div className="h-14 w-14 overflow-hidden rounded-lg bg-muted">
                        <img
                          src={product.imageUrl}
                          alt={product.name}
                          className="h-full w-full object-cover"
                          referrerPolicy="no-referrer"
                        />
                      </div>
                      <div className="min-w-0 flex-1">
                        <p className="truncate font-medium text-foreground">{product.name}</p>
                        <p className="text-sm text-muted-foreground">
                          {formatVnd(product.price, locale)} / {product.unit}
                        </p>
                      </div>
                      <div className="text-right">
                        <p className="text-xs uppercase tracking-wide text-muted-foreground">
                          {t("marketplaceSeller.table.available", "Available")}
                        </p>
                        <p className="font-semibold text-primary">
                          {product.availableQuantity} {product.unit}
                        </p>
                      </div>
                    </div>
                  ))
                ) : (
                  <div className="rounded-lg border border-dashed border-border px-4 py-8 text-center text-sm text-muted-foreground">
                    {hasProducts
                      ? t("marketplaceSeller.dashboard.topProducts.emptyWithProducts", "No published products yet.")
                      : t("marketplaceSeller.dashboard.topProducts.emptyNoProducts", "No products available yet.")}
                  </div>
                )}
              </CardContent>
            </Card>
          </div>
        </AsyncState>
      </div>
    </PageContainer>
  );
}
