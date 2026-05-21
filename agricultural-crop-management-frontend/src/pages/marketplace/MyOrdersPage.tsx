import { useState } from "react";
import { ChevronRight, Package } from "lucide-react";
import { Link, useSearchParams } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { Button } from "@/shared/ui";
import {
  type MarketplaceOrder,
  type MarketplaceOrderItem,
  type MarketplaceOrderStatus,
  type MarketplacePaymentMethod,
  type MarketplacePaymentVerificationStatus,
} from "@/shared/api";
import { useMarketplaceOrders } from "@/features/marketplace/hooks";
import { formatDate, formatVnd } from "@/features/marketplace/lib/format";
import {
  BUYER_ORDER_FILTER_STATUSES,
  getMarketplaceOrderStatusBadgeClass,
  getMarketplaceOrderStatusGroup,
  getMarketplaceOrderStatusLabel,
  normalizeMarketplaceOrderStatus,
  isMarketplaceBuyerOrderCancellable,
} from "@/features/marketplace/lib/orderStatus";

function toPositiveInt(value: string | null, fallback: number) {
  const parsed = Number(value);
  return Number.isFinite(parsed) && parsed > 0 ? Math.floor(parsed) : fallback;
}

function getPaymentMethodLabel(method: MarketplacePaymentMethod, t: (key: string) => string): string {
  return t(`marketplaceBuyer.myOrders.paymentMethod.${method}`);
}

function getPaymentVerificationStatusLabel(
  verificationStatus: MarketplacePaymentVerificationStatus,
  t: (key: string, options?: Record<string, unknown>) => string,
): string {
  const knownStatuses: MarketplacePaymentVerificationStatus[] = [
    "NOT_REQUIRED",
    "AWAITING_PROOF",
    "SUBMITTED",
    "VERIFIED",
    "REJECTED",
  ];
  if (knownStatuses.includes(verificationStatus)) {
    return t(`marketplaceBuyer.myOrders.paymentVerificationStatus.${verificationStatus}`);
  }
  return t("marketplaceBuyer.myOrders.paymentVerificationStatus.unknown", {
    status: verificationStatus,
  });
}

function StatusBadge({
  status,
  orderId,
  t,
}: {
  status: MarketplaceOrderStatus;
  orderId: number;
  t: (key: string, options?: Record<string, unknown>) => string;
}) {
  return (
    <span
      data-testid={`order-status-${orderId}`}
      className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-semibold ${getMarketplaceOrderStatusBadgeClass(status)}`}
    >
      {getMarketplaceOrderStatusLabel(status, t)}
    </span>
  );
}

function OrderItemPreview({
  item,
  t,
}: {
  item: MarketplaceOrderItem;
  t: (key: string, options?: Record<string, unknown>) => string;
}) {
  const [imgError, setImgError] = useState(false);
  return (
    <div className="flex items-center gap-3 px-4 py-3">
      {item.imageUrl && !imgError ? (
        <img
          src={item.imageUrl}
          alt={item.productName}
          className="h-16 w-16 rounded-lg object-cover shrink-0 bg-slate-100"
          referrerPolicy="no-referrer"
          onError={() => setImgError(true)}
        />
      ) : (
        <div className="h-16 w-16 rounded-lg bg-muted/50 border border-border flex items-center justify-center shrink-0">
          <Package size={18} className="text-muted-foreground/40" />
        </div>
      )}
      <div className="flex-1 min-w-0">
        <p className="text-sm font-medium text-foreground line-clamp-1">{item.productName}</p>
        <p className="text-xs text-muted-foreground mt-0.5">
          {t("marketplaceBuyer.myOrders.quantity")}: {item.quantity} x {formatVnd(item.unitPriceSnapshot)}
        </p>
      </div>
      <span className="text-sm font-semibold text-foreground shrink-0">
        {formatVnd(item.lineTotal)}
      </span>
    </div>
  );
}

function OrderSummaryCard({
  order,
  t,
}: {
  order: MarketplaceOrder;
  t: (key: string, options?: Record<string, unknown>) => string;
}) {
  const previewItems = order.items.slice(0, 2);
  const extraCount = order.items.length - previewItems.length;
  const cancellable = isMarketplaceBuyerOrderCancellable(order);
  const paymentInfo =
    order.payment != null
      ? `${getPaymentMethodLabel(order.payment.method, t)} · ${getPaymentVerificationStatusLabel(order.payment.verificationStatus, t)}`
      : null;

  return (
    <div className="rounded-xl border border-border bg-card shadow-sm hover:shadow-md transition overflow-hidden">
      <div className="flex items-start justify-between px-4 py-3 border-b border-border">
        <div>
          <p className="text-sm font-medium text-muted-foreground">
            {t("marketplaceBuyer.myOrders.orderCode")}:{" "}
            <span className="font-semibold text-foreground">#{order.orderCode}</span>
          </p>
          <p className="text-xs text-muted-foreground/70 mt-0.5">
            {t("marketplaceBuyer.myOrders.orderDate")}: {formatDate(order.createdAt)}
          </p>
          <p className="mt-2 flex flex-wrap gap-x-3 gap-y-1 text-xs text-muted-foreground/80">
            <span>
              {t("marketplaceBuyer.myOrders.group")}:{" "}
              {getMarketplaceOrderStatusGroup(order.status, t)}
            </span>
            <span>{cancellable ? t("marketplaceBuyer.myOrders.eligibleForCancellation") : t("marketplaceBuyer.myOrders.cannotCancel")}</span>
            {paymentInfo ? (
              <span>
                {t("marketplaceBuyer.myOrders.payment")}: {paymentInfo}
              </span>
            ) : null}
          </p>
        </div>
        <StatusBadge status={order.status} orderId={order.id} t={t} />
      </div>
      <div className="divide-y divide-border">
        {previewItems.map((item) => (
          <OrderItemPreview key={item.id} item={item} t={t} />
        ))}
        {extraCount > 0 && (
          <p className="px-4 py-2 text-xs text-muted-foreground/60 italic">
            {t("marketplaceBuyer.myOrders.otherItems", { count: extraCount })}
          </p>
        )}
      </div>
      <div className="border-t border-border px-4 py-3 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
        <p className="text-sm text-muted-foreground flex-1">
          {t("marketplaceBuyer.myOrders.totalWithCount", { count: order.items.length })}:{" "}
          <span className="font-bold text-primary">{formatVnd(order.totalAmount)}</span>
        </p>
        <Link to={`/marketplace/orders/${order.id}`} className="shrink-0">
          <Button
            variant="outline"
            size="sm"
            className="w-full sm:w-auto flex items-center justify-center gap-1 acm-rounded-sm border-border hover:bg-muted transition-colors duration-200"
          >
            {t("marketplaceBuyer.myOrders.orderDetail")} <ChevronRight size={16} />
          </Button>
        </Link>
      </div>
    </div>
  );
}

export function MyOrdersPage() {
  const { t } = useTranslation();
  const [searchParams, setSearchParams] = useSearchParams();
  const page = toPositiveInt(searchParams.get("page"), 1);
  const rawStatusParam = searchParams.get("status");
  const normalizedStatusParam = rawStatusParam ? normalizeMarketplaceOrderStatus(rawStatusParam) : "UNKNOWN";
  const selectedStatus =
    normalizedStatusParam !== "UNKNOWN" &&
    BUYER_ORDER_FILTER_STATUSES.includes(normalizedStatusParam)
      ? normalizedStatusParam
      : undefined;

  const ordersQuery = useMarketplaceOrders({
    status: selectedStatus as MarketplaceOrderStatus | undefined,
    page: page - 1,
    size: 10,
  });

  function updateParams(patch: Record<string, string | null>) {
    const next = new URLSearchParams(searchParams);
    Object.entries(patch).forEach(([key, value]) => {
      if (!value) {
        next.delete(key);
      } else {
        next.set(key, value);
      }
    });
    if (!("page" in patch)) {
      next.set("page", "1");
    }
    setSearchParams(next);
  }

  if (ordersQuery.isLoading) {
    return (
      <div className="max-w-[1800px] mx-auto px-6 pt-6">
        <div className="rounded-xl border border-dashed border-border bg-card p-8 text-center text-sm text-muted-foreground">
          {t("marketplaceBuyer.myOrders.loadingOrders")}
        </div>
      </div>
    );
  }

  if (ordersQuery.isError) {
    return (
      <div className="max-w-[1800px] mx-auto px-6 pt-6">
        <div className="rounded-xl border border-dashed border-destructive/30 bg-card p-8 text-center text-sm text-destructive">
          {t("marketplaceBuyer.myOrders.errorOrders")}
        </div>
      </div>
    );
  }

  const orderPage = ordersQuery.data;
  const orders = orderPage?.items ?? [];
  const totalPages = Math.max(orderPage?.totalPages ?? 1, 1);

  return (
    <div className="max-w-[1800px] mx-auto px-6 pt-6">
      <h1 className="text-2xl font-bold text-foreground">{t("marketplaceBuyer.myOrders.title")}</h1>
      <p className="mt-1 text-sm text-muted-foreground">{t("marketplaceBuyer.myOrders.subtitle")}</p>

      <div className="mt-4 flex flex-wrap gap-2">
        <Button
          type="button"
          variant={!selectedStatus ? "default" : "outline"}
          size="sm"
          onClick={() => updateParams({ status: null })}
        >
          {t("marketplaceBuyer.myOrders.filterAll")}
        </Button>
        {BUYER_ORDER_FILTER_STATUSES.map((value) => (
          <Button
            key={value}
            type="button"
            variant={selectedStatus === value ? "default" : "outline"}
            size="sm"
            onClick={() => updateParams({ status: value })}
          >
            {getMarketplaceOrderStatusLabel(value, t)}
          </Button>
        ))}
      </div>

      <div className="mt-6 space-y-4">
        {orders.map((order) => (
          <OrderSummaryCard key={order.id} order={order} t={t} />
        ))}

        {orders.length === 0 && (
          <div className="rounded-xl border border-border bg-card py-20 text-center">
            <Package size={48} className="mx-auto mb-4 text-muted-foreground/40" />
            <h3 className="mb-2 text-lg font-medium text-foreground">{t("marketplaceBuyer.myOrders.emptyTitle")}</h3>
            <p className="mb-6 text-sm text-muted-foreground">{t("marketplaceBuyer.myOrders.emptyDesc")}</p>
            <Link to="/marketplace/products">
              <Button className="bg-primary hover:bg-primary/90 text-white acm-rounded-sm acm-button-shadow">
                {t("marketplaceBuyer.myOrders.startShopping")}
              </Button>
            </Link>
          </div>
        )}

        {orders.length > 0 && (
          <div className="flex items-center justify-between rounded-xl border border-border bg-card px-4 py-3">
            <p className="text-sm text-muted-foreground">
              {t("marketplaceBuyer.myOrders.page")} {page} {t("marketplaceBuyer.myOrders.of")} {totalPages}
            </p>
            <div className="flex gap-2">
              <Button
                variant="outline"
                size="sm"
                disabled={page <= 1}
                onClick={() => updateParams({ page: String(page - 1) })}
              >
                {t("marketplaceBuyer.myOrders.previous")}
              </Button>
              <Button
                variant="outline"
                size="sm"
                disabled={page >= totalPages}
                onClick={() => updateParams({ page: String(page + 1) })}
              >
                {t("marketplaceBuyer.myOrders.next")}
              </Button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
