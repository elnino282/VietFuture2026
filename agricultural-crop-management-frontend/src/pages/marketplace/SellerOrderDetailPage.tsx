import { useMemo } from "react";
import { ArrowLeft, CreditCard, MapPin, Phone, Truck } from "lucide-react";
import { Link, useParams } from "react-router-dom";
import type { MarketplaceOrderStatus } from "@/shared/api";
import { Badge, Button, Card, CardContent, CardHeader, CardTitle } from "@/shared/ui";
import {
  useMarketplaceFarmerOrderDetail,
  useMarketplaceUpdateFarmerOrderStatusMutation,
} from "@/features/marketplace/hooks";
import { SellerMarketplaceTabs } from "@/features/marketplace/layout";
import { formatDateTime, formatVnd } from "@/features/marketplace/lib/format";

function nextStatusOptions(status: MarketplaceOrderStatus): MarketplaceOrderStatus[] {
  switch (status) {
    case "PENDING":
      return ["CONFIRMED", "CANCELLED"];
    case "CONFIRMED":
      return ["PREPARING", "CANCELLED"];
    case "PREPARING":
      return ["DELIVERING"];
    case "DELIVERING":
      return ["COMPLETED"];
    default:
      return [];
  }
}

function sellerOrderStatusLabel(status: MarketplaceOrderStatus) {
  switch (status) {
    case "PENDING":
      return "Pending";
    case "CONFIRMED":
      return "Confirmed";
    case "PREPARING":
      return "Preparing";
    case "DELIVERING":
      return "Shipped";
    case "COMPLETED":
      return "Delivered";
    case "CANCELLED":
      return "Cancelled";
    default:
      return status;
  }
}

function statusBadge(status: MarketplaceOrderStatus) {
  switch (status) {
    case "PENDING":
      return <Badge variant="warning">{sellerOrderStatusLabel(status)}</Badge>;
    case "CONFIRMED":
      return <Badge variant="secondary">{sellerOrderStatusLabel(status)}</Badge>;
    case "PREPARING":
      return <Badge variant="secondary">{sellerOrderStatusLabel(status)}</Badge>;
    case "DELIVERING":
      return <Badge variant="default">{sellerOrderStatusLabel(status)}</Badge>;
    case "COMPLETED":
      return <Badge variant="success">{sellerOrderStatusLabel(status)}</Badge>;
    case "CANCELLED":
      return <Badge variant="destructive">{sellerOrderStatusLabel(status)}</Badge>;
    default:
      return <Badge variant="secondary">{status}</Badge>;
  }
}

export function SellerOrderDetailPage() {
  const { id } = useParams();
  const orderId = Number(id ?? 0);
  const orderQuery = useMarketplaceFarmerOrderDetail(orderId);
  const statusMutation = useMarketplaceUpdateFarmerOrderStatusMutation(orderId);

  const actions = useMemo(
    () => (orderQuery.data ? nextStatusOptions(orderQuery.data.status) : []),
    [orderQuery.data],
  );
  const mutationError =
    statusMutation.isError && statusMutation.error instanceof Error
      ? statusMutation.error.message
      : null;

  if (orderQuery.isLoading) {
    return (
      <div className="max-w-[1800px] mx-auto px-6 pt-6 space-y-6">
        <SellerMarketplaceTabs />
        <Card className="border-dashed">
          <CardContent className="p-8 text-sm text-muted-foreground">Loading order detail...</CardContent>
        </Card>
      </div>
    );
  }

  if (orderQuery.isError || !orderQuery.data) {
    return (
      <div className="max-w-[1800px] mx-auto px-6 pt-6 space-y-6">
        <SellerMarketplaceTabs />
        <Card className="border-destructive/30">
          <CardContent className="p-8 text-sm text-destructive">Failed to load seller order detail.</CardContent>
        </Card>
      </div>
    );
  }

  const order = orderQuery.data;

  return (
    <div className="max-w-[1800px] mx-auto px-6 pt-6 space-y-6">
      <SellerMarketplaceTabs />

      <div className="flex flex-wrap items-center justify-between gap-4">
        <div className="flex items-center gap-4">
          <Link
            to="/farmer/marketplace-orders"
            className="rounded-full border border-border p-2 text-muted-foreground transition-colors hover:bg-muted hover:text-foreground"
            aria-label="Back to orders"
          >
            <ArrowLeft size={18} />
          </Link>
          <div>
            <p className="text-sm font-medium text-primary">FarmTrace Seller Portal</p>
            <h1 className="mt-1 text-3xl font-bold text-foreground">{order.orderCode}</h1>
          </div>
        </div>

        <div className="flex flex-wrap gap-2">
          {actions.map((action) => (
            <Button
              key={action}
              type="button"
              variant={action === "CANCELLED" ? "destructive" : "default"}
              onClick={() => statusMutation.mutate({ status: action })}
              disabled={statusMutation.isPending}
            >
              {sellerOrderStatusLabel(action)}
            </Button>
          ))}
        </div>
      </div>

      {mutationError ? <p className="text-sm text-destructive">{mutationError}</p> : null}

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-[minmax(0,1.45fr)_minmax(0,0.85fr)]">
        <Card className="border-border shadow-sm">
          <CardHeader className="border-b border-border/50">
            <div className="flex flex-wrap items-start justify-between gap-3">
              <div>
                <CardTitle>Order items</CardTitle>
                <p className="mt-2 text-sm text-muted-foreground">{formatDateTime(order.createdAt)}</p>
              </div>
              {statusBadge(order.status)}
            </div>
          </CardHeader>
          <CardContent className="p-0">
            <div className="divide-y divide-border">
              {order.items.map((item) => (
                <div key={item.id} className="flex items-center gap-4 p-4">
                  <img
                    src={item.imageUrl}
                    alt={item.productName}
                    className="h-16 w-16 rounded-lg bg-muted object-cover"
                    referrerPolicy="no-referrer"
                  />
                  <div className="min-w-0 flex-1">
                    <p className="font-medium text-foreground">{item.productName}</p>
                    <p className="text-sm text-muted-foreground">
                      {formatVnd(item.unitPriceSnapshot)} x {item.quantity}
                    </p>
                  </div>
                  <p className="font-semibold text-foreground">{formatVnd(item.lineTotal)}</p>
                </div>
              ))}
            </div>

            <div className="space-y-2 border-t border-border bg-muted/50 p-4 text-sm">
              <div className="flex justify-between text-muted-foreground">
                <span>Subtotal</span>
                <span>{formatVnd(order.subtotal)}</span>
              </div>
              <div className="flex justify-between text-muted-foreground">
                <span>Shipping fee</span>
                <span>{formatVnd(order.shippingFee)}</span>
              </div>
              <div className="flex justify-between border-t border-border pt-2 text-lg font-bold text-primary">
                <span>Total</span>
                <span>{formatVnd(order.totalAmount)}</span>
              </div>
            </div>
          </CardContent>
        </Card>

        <div className="space-y-6">
          <Card className="border-border shadow-sm">
            <CardHeader className="pb-3">
              <CardTitle className="text-sm uppercase tracking-[0.18em] text-muted-foreground">
                Customer information
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-3">
              <div className="flex items-start gap-3">
                <MapPin size={16} className="mt-1 shrink-0 text-muted-foreground/60" />
                <div>
                  <p className="font-medium text-foreground">{order.shippingRecipientName}</p>
                  <p className="text-sm text-muted-foreground">{order.shippingAddressLine}</p>
                </div>
              </div>
              <div className="flex items-center gap-3">
                <Phone size={16} className="shrink-0 text-muted-foreground/60" />
                <p className="text-sm text-muted-foreground">{order.shippingPhone}</p>
              </div>
            </CardContent>
          </Card>

          <Card className="border-border shadow-sm">
            <CardHeader className="pb-3">
              <CardTitle className="text-sm uppercase tracking-[0.18em] text-muted-foreground">
                Payment and shipping
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="flex items-center gap-3">
                <CreditCard size={16} className="shrink-0 text-muted-foreground/60" />
                <div>
                  <p className="text-sm font-medium text-foreground">{order.payment.method}</p>
                  <p className="text-xs text-muted-foreground">
                    Verification: {order.payment.verificationStatus}
                  </p>
                </div>
              </div>
              <div className="flex items-center gap-3">
                <Truck size={16} className="shrink-0 text-muted-foreground/60" />
                <div>
                  <p className="text-sm font-medium text-foreground">Shipping status</p>
                  <p className="text-xs text-muted-foreground">{sellerOrderStatusLabel(order.status)}</p>
                </div>
              </div>
              {order.note ? (
                <div className="rounded-xl border border-border bg-muted/50 p-3 text-sm text-muted-foreground">
                  Note: {order.note}
                </div>
              ) : null}
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
}
