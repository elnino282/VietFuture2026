import {
  DollarSign,
  Package,
  Plus,
  ShoppingBag,
  Store,
} from "lucide-react";
import { Link, useNavigate } from "react-router-dom";
import { AsyncState, Button, Card, CardContent, CardHeader, CardTitle, PageContainer } from "@/shared/ui";
import { useMarketplaceFarmerDashboard, useMarketplaceFarmerProducts } from "@/features/marketplace/hooks";
import { SellerMarketplaceTabs } from "@/features/marketplace/layout";
import { formatDateTime, formatVnd } from "@/features/marketplace/lib/format";

function MetricCard({
  icon: Icon,
  label,
  value,
  tone,
}: {
  icon: typeof DollarSign;
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

function orderStatusLabel(status: string) {
  switch (status) {
    case "PENDING":
      return "Pending";
    case "CONFIRMED":
      return "Confirmed";
    case "PREPARING":
      return "Preparing";
    case "DELIVERING":
      return "Delivering";
    case "COMPLETED":
      return "Completed";
    case "CANCELLED":
      return "Cancelled";
    default:
      return status;
  }
}

export function SellerDashboardPage() {
  const navigate = useNavigate();
  const dashboardQuery = useMarketplaceFarmerDashboard();
  const productsQuery = useMarketplaceFarmerProducts({ page: 0, size: 20, status: "PUBLISHED" });

  const dashboard = dashboardQuery.data;

  const topProducts = (productsQuery.data?.items ?? [])
    .slice()
    .sort((left, right) => right.availableQuantity - left.availableQuantity)
    .slice(0, 5);

  return (
    <PageContainer>
      <div className="space-y-6">
        <SellerMarketplaceTabs />

        <AsyncState
          isLoading={dashboardQuery.isLoading}
          isEmpty={false}
          error={dashboardQuery.isError ? (dashboardQuery.error as Error) : null}
          onRetry={() => dashboardQuery.refetch()}
          loadingText="Loading marketplace dashboard..."
        >
          <Card className="border border-border rounded-xl shadow-sm">
            <CardContent className="px-6 py-4">
              {/* Header Row: Title + Actions */}
              <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
                {/* Title Section */}
                <div className="flex-shrink-0">
                  <h1 className="text-2xl font-bold text-foreground flex items-center gap-2 leading-tight">
                    <Store className="w-6 h-6 text-emerald-600" />
                    Manage products
                  </h1>
                  <p className="text-sm text-muted-foreground mt-1">
                    List, moderate, and manage stock for your marketplace products.
                  </p>
                </div>

                {/* Primary Action */}
                <div className="flex items-center gap-3 flex-shrink-0">
                  <Button
                    className="bg-primary hover:bg-primary/90 text-white acm-rounded-sm acm-button-shadow"
                    onClick={() => navigate("/farmer/marketplace-products/new")}
                  >
                    <Plus className="w-4 h-4 mr-2" />
                    Add product
                  </Button>
                </div>
              </div>
            </CardContent>
          </Card>

          <div className="grid grid-cols-1 gap-6 md:grid-cols-2 xl:grid-cols-4">
            <MetricCard
              icon={DollarSign}
              label="Revenue"
              value={formatVnd(dashboard?.totalRevenue ?? 0)}
              tone="bg-primary/10 text-primary"
            />
            <MetricCard
              icon={ShoppingBag}
              label="Pending orders"
              value={dashboard?.pendingOrders ?? 0}
              tone="bg-blue-100 text-blue-600"
            />
            <MetricCard
              icon={Package}
              label="Published products"
              value={dashboard?.publishedProducts ?? 0}
              tone="bg-purple-100 text-purple-600"
            />
            <MetricCard
              icon={Store}
              label="Pending review"
              value={dashboard?.pendingReviewProducts ?? 0}
              tone="bg-orange-100 text-orange-600"
            />
          </div>

          <div className="grid grid-cols-1 gap-6 xl:grid-cols-2">
            <Card className="overflow-hidden border-border shadow-sm">
              <CardHeader className="border-b border-border/50">
                <div className="flex items-center justify-between gap-3">
                  <CardTitle>Recent orders</CardTitle>
                  <Link to="/farmer/marketplace-orders" className="text-sm font-medium text-primary hover:underline">
                    See all
                  </Link>
                </div>
              </CardHeader>
              <CardContent className="space-y-4 p-6">
                {(dashboard?.recentOrders?.length ?? 0) > 0 ? (
                  dashboard!.recentOrders.map((order) => (
                    <Link
                      key={order.id}
                      to={`/farmer/marketplace-orders/${order.id}`}
                      className="flex items-center justify-between rounded-xl border border-border p-4 transition-colors hover:bg-muted/50"
                    >
                      <div>
                        <p className="font-semibold text-foreground">{order.orderCode}</p>
                        <p className="text-sm text-muted-foreground">
                          {order.items.length} items • {formatDateTime(order.createdAt)}
                        </p>
                      </div>
                      <div className="text-right">
                        <p className="font-semibold text-primary">{formatVnd(order.totalAmount)}</p>
                        <p className="text-sm text-muted-foreground">{orderStatusLabel(order.status)}</p>
                      </div>
                    </Link>
                  ))
                ) : (
                  <div className="rounded-xl border border-dashed border-border px-4 py-8 text-center text-sm text-muted-foreground">
                    No buyer orders yet.
                  </div>
                )}
              </CardContent>
            </Card>

            <Card className="overflow-hidden border-border shadow-sm">
              <CardHeader className="border-b border-border/50">
                <div className="flex items-center justify-between gap-3">
                  <CardTitle>Top products</CardTitle>
                  <Link to="/farmer/marketplace-products" className="text-sm font-medium text-primary hover:underline">
                    Manage products
                  </Link>
                </div>
              </CardHeader>
              <CardContent className="space-y-4 p-6">
                {topProducts.length > 0 ? (
                  topProducts.map((product) => (
                    <div key={product.id} className="flex items-center gap-4 rounded-xl border border-border p-4">
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
                        <p className="text-sm text-muted-foreground">{formatVnd(product.price)} / {product.unit}</p>
                      </div>
                      <div className="text-right">
                        <p className="text-xs uppercase tracking-wide text-muted-foreground">Available</p>
                        <p className="font-semibold text-primary">
                          {product.availableQuantity} {product.unit}
                        </p>
                      </div>
                    </div>
                  ))
                ) : (
                  <div className="rounded-xl border border-dashed border-border px-4 py-8 text-center text-sm text-muted-foreground">
                    No published products yet.
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
