import { useState } from "react";
import { Eye, Inbox, ShoppingBag } from "lucide-react";
import { Link } from "react-router-dom";
import type { MarketplaceOrderStatus } from "@/shared/api";
import { useI18n } from "@/hooks/useI18n";
import { cn } from "@/shared/lib";
import {
  Badge,
  Button,
  Card,
  CardContent,
  PageContainer,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/shared/ui";
import { useMarketplaceFarmerOrders } from "@/features/marketplace/hooks";
import { SellerMarketplaceTabs } from "@/features/marketplace/layout";
import { formatDate, formatVnd } from "@/features/marketplace/lib/format";
import {
  getMarketplaceOrderStatusLabel,
  getMarketplaceOrderStatusTone,
  type CanonicalMarketplaceOrderStatus,
} from "@/features/marketplace/lib/orderStatus";

type Translator = (key: string, optionsOrDefault?: Record<string, unknown> | string) => string;

const SELLER_ORDER_FILTER_STATUSES: CanonicalMarketplaceOrderStatus[] = [
  "PENDING_PAYMENT",
  "PAYMENT_SUBMITTED",
  "PAYMENT_VERIFIED",
  "CONFIRMED",
  "PREPARING",
  "SHIPPED",
  "DELIVERED",
  "COMPLETED",
  "REJECTED",
  "CANCELLED",
];

function statusBadgeVariant(status: MarketplaceOrderStatus) {
  switch (getMarketplaceOrderStatusTone(status)) {
    case "warning":
      return "warning" as const;
    case "info":
      return "info" as const;
    case "success":
      return "success" as const;
    case "destructive":
      return "destructive" as const;
    case "neutral":
      return "outline" as const;
    default:
      return "secondary" as const;
  }
}

function orderStatusLabel(status: MarketplaceOrderStatus, t: Translator) {
  return getMarketplaceOrderStatusLabel(status, t, "marketplaceSeller.status.order");
}

function statusBadge(status: MarketplaceOrderStatus, t: Translator) {
  return <Badge variant={statusBadgeVariant(status)}>{orderStatusLabel(status, t)}</Badge>;
}

export function SellerOrdersPage() {
  const { t, locale } = useI18n();
  const [status, setStatus] = useState<"ALL" | MarketplaceOrderStatus>("ALL");

  const statusOptions: Array<{ value: "ALL" | MarketplaceOrderStatus; label: string }> = [
    { value: "ALL", label: t("marketplaceSeller.orders.filters.all", "All") },
    ...SELLER_ORDER_FILTER_STATUSES.map((value) => ({
      value,
      label: orderStatusLabel(value, t),
    })),
  ];
  const ordersQuery = useMarketplaceFarmerOrders({
    page: 0,
    size: 50,
    status: status === "ALL" ? undefined : status,
  });
  const orders = ordersQuery.data?.items ?? [];

  return (
    <PageContainer variant="wide" className="space-y-6">
      <SellerMarketplaceTabs />

      <Card variant="page-header">
        <CardContent className="px-6 py-4">
          <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
            <div className="flex-shrink-0">
              <h1 className="flex items-center gap-2 text-2xl font-bold leading-tight text-foreground">
                <ShoppingBag className="h-6 w-6 text-primary" />
                {t("marketplaceSeller.orders.title", "Manage orders")}
              </h1>
              <p className="mt-1 max-w-2xl text-sm text-muted-foreground">
                {t(
                  "marketplaceSeller.orders.subtitle",
                  "Track buyer orders, monitor status transitions, and open order details for fulfillment.",
                )}
              </p>
            </div>
          </div>
        </CardContent>
      </Card>

      <Card variant="filter">
        <CardContent className="px-4 py-4 sm:px-6">
          <div className="-mx-1 overflow-x-auto pb-1">
            <div
              className="flex min-w-max items-center gap-2 px-1 md:min-w-0 md:flex-wrap"
              role="group"
              aria-label={t("marketplaceSeller.orders.filters.ariaLabel", "Order status filters")}
            >
              {statusOptions.map((option) => {
                const isActive = status === option.value;

                return (
                  <Button
                    key={option.value}
                    type="button"
                    variant={isActive ? "default" : "outline"}
                    size="sm"
                    aria-pressed={isActive}
                    onClick={() => setStatus(option.value)}
                    className={cn(
                      "rounded-full",
                      isActive
                        ? "bg-primary text-primary-foreground hover:bg-primary/90"
                        : "bg-card text-muted-foreground hover:border-primary/40 hover:bg-primary/5 hover:text-foreground",
                    )}
                  >
                    {option.label}
                  </Button>
                );
              })}
            </div>
          </div>
        </CardContent>
      </Card>

      <Card variant="content" className="overflow-hidden rounded-xl">
        <Table>
          <TableHeader>
            <TableRow className="bg-muted/50">
              <TableHead className="pl-4">{t("marketplaceSeller.orders.table.orderCode", "Order code")}</TableHead>
              <TableHead>{t("marketplaceSeller.orders.table.orderDate", "Order date")}</TableHead>
              <TableHead>{t("marketplaceSeller.orders.table.customer", "Customer")}</TableHead>
              <TableHead>{t("marketplaceSeller.orders.table.total", "Total")}</TableHead>
              <TableHead>{t("marketplaceSeller.orders.table.status", "Status")}</TableHead>
              <TableHead className="pr-4 text-right">
                {t("marketplaceSeller.orders.table.actions", "Actions")}
              </TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {ordersQuery.isLoading
              ? Array.from({ length: 5 }, (_, index) => (
                  <TableRow key={`skeleton-${index}`}>
                    <TableCell className="pl-4">
                      <div className="h-4 w-28 animate-pulse rounded bg-muted" />
                    </TableCell>
                    <TableCell>
                      <div className="h-4 w-24 animate-pulse rounded bg-muted" />
                    </TableCell>
                    <TableCell>
                      <div className="h-4 w-24 animate-pulse rounded bg-muted" />
                    </TableCell>
                    <TableCell>
                      <div className="h-4 w-20 animate-pulse rounded bg-muted" />
                    </TableCell>
                    <TableCell>
                      <div className="h-6 w-24 animate-pulse rounded-full bg-muted" />
                    </TableCell>
                    <TableCell className="pr-4">
                      <div className="ml-auto h-4 w-16 animate-pulse rounded bg-muted" />
                    </TableCell>
                  </TableRow>
                ))
              : null}

            {!ordersQuery.isLoading && !ordersQuery.isError
              ? orders.map((order) => (
                  <TableRow key={order.id}>
                    <TableCell className="pl-4 font-medium text-foreground">{order.orderCode}</TableCell>
                    <TableCell className="text-muted-foreground">{formatDate(order.createdAt, locale)}</TableCell>
                    <TableCell className="text-muted-foreground">
                      {order.shippingRecipientName ||
                        t("marketplaceSeller.orders.table.buyerFallback", {
                          id: order.buyerUserId,
                          defaultValue: "Buyer #{{id}}",
                        })}
                    </TableCell>
                    <TableCell className="font-medium text-primary">
                      {formatVnd(order.totalAmount, locale)}
                    </TableCell>
                    <TableCell>{statusBadge(order.status, t)}</TableCell>
                    <TableCell className="pr-4 text-right">
                      <Link to={`/farmer/marketplace-orders/${order.id}`}>
                        <Button variant="ghost" size="sm" className="text-primary hover:bg-primary/10 hover:text-primary">
                          <Eye size={16} className="mr-1" />
                          {t("marketplaceSeller.orders.table.detail", "Detail")}
                        </Button>
                      </Link>
                    </TableCell>
                  </TableRow>
                ))
              : null}

            {!ordersQuery.isLoading && !ordersQuery.isError && orders.length === 0 ? (
              <TableRow>
                <TableCell colSpan={6} className="py-10 text-center">
                  <div className="flex flex-col items-center justify-center gap-3">
                    <Inbox className="text-muted-foreground/40" size={44} />
                    <p className="text-sm text-muted-foreground">
                      {t("marketplaceSeller.orders.empty", "No orders matched the current filter.")}
                    </p>
                  </div>
                </TableCell>
              </TableRow>
            ) : null}

            {ordersQuery.isError ? (
              <TableRow>
                <TableCell colSpan={6} className="p-8 text-center text-sm text-destructive">
                  {t("marketplaceSeller.orders.error", "Failed to load seller orders.")}
                </TableCell>
              </TableRow>
            ) : null}
          </TableBody>
        </Table>
      </Card>
    </PageContainer>
  );
}
