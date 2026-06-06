import { useState, type ReactNode } from "react";
import { Ban, ChevronRight, CreditCard, Package, ShieldCheck, Truck } from "lucide-react";
import { Link, useSearchParams } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { BackButton, Button } from "@/shared/ui";
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

type Translator = (key: string, options?: Record<string, unknown>) => string;

function toPositiveInt(value: string | null, fallback: number) {
  const parsed = Number(value);
  return Number.isFinite(parsed) && parsed > 0 ? Math.floor(parsed) : fallback;
}

function getPaymentMethodLabel(method: MarketplacePaymentMethod, t: Translator): string {
  return t(`marketplaceBuyer.myOrders.paymentMethod.${method}`);
}

function getPaymentVerificationStatusLabel(
  verificationStatus: MarketplacePaymentVerificationStatus,
  t: Translator,
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

function getPaymentChipClass(verificationStatus: MarketplacePaymentVerificationStatus): string {
  switch (verificationStatus) {
    case "VERIFIED":
      return "border-emerald-200 bg-emerald-50 text-emerald-700";
    case "REJECTED":
      return "border-red-200 bg-red-50 text-red-700";
    case "AWAITING_PROOF":
    case "SUBMITTED":
      return "border-amber-200 bg-amber-50 text-amber-700";
    default:
      return "border-gray-200 bg-gray-50 text-gray-600";
  }
}

function getItemCountLabel(count: number, t: Translator): string {
  const totalLabel = t("marketplaceBuyer.myOrders.totalWithCount", { count });
  const match = totalLabel.match(/\(([^)]+)\)/);
  return match?.[1] ?? `${count}`;
}

function getTotalLabel(count: number, t: Translator): string {
  return t("marketplaceBuyer.myOrders.totalWithCount", { count }).replace(/\s*\([^)]+\)/, "");
}

function StatusBadge({
  status,
  orderId,
  t,
}: {
  status: MarketplaceOrderStatus;
  orderId: number;
  t: Translator;
}) {
  return (
    <span
      data-testid={`order-status-${orderId}`}
      className={`inline-flex shrink-0 items-center rounded-full px-3 py-1 text-xs font-semibold ${getMarketplaceOrderStatusBadgeClass(status)}`}
    >
      {getMarketplaceOrderStatusLabel(status, t)}
    </span>
  );
}

function MetadataChip({
  children,
  icon,
  className = "border-gray-200 bg-gray-50 text-gray-600",
}: {
  children: ReactNode;
  icon?: ReactNode;
  className?: string;
}) {
  return (
    <span
      className={`inline-flex max-w-full items-center gap-1.5 rounded-lg border px-2.5 py-1 text-xs font-medium leading-none ${className}`}
    >
      {icon}
      <span className="truncate">{children}</span>
    </span>
  );
}

function PaymentStatusChip({
  payment,
  t,
}: {
  payment: MarketplaceOrder["payment"];
  t: Translator;
}) {
  if (!payment) return null;

  const methodLabel = getPaymentMethodLabel(payment.method, t);
  const statusLabel = getPaymentVerificationStatusLabel(payment.verificationStatus, t);

  return (
    <MetadataChip
      className={getPaymentChipClass(payment.verificationStatus)}
      icon={<CreditCard size={14} className="shrink-0" />}
    >
      {t("marketplaceBuyer.myOrders.payment")}: {methodLabel} - {statusLabel}
    </MetadataChip>
  );
}

function OrderFilterTabs({
  selectedStatus,
  onSelectStatus,
  t,
}: {
  selectedStatus: MarketplaceOrderStatus | undefined;
  onSelectStatus: (status: MarketplaceOrderStatus | null) => void;
  t: Translator;
}) {
  return (
    <div className="-mx-4 mt-5 overflow-x-auto px-4 pb-1 sm:mx-0 sm:px-0">
      <div className="flex min-w-max items-center gap-2">
        <Button
          type="button"
          variant="outline"
          size="sm"
          className={`rounded-full px-4 font-semibold shadow-sm ${
            !selectedStatus
              ? "border-emerald-600 bg-emerald-600 text-white hover:bg-emerald-700 hover:text-white"
              : "border-gray-200 bg-white text-gray-700 hover:bg-gray-50"
          }`}
          onClick={() => onSelectStatus(null)}
        >
          {t("marketplaceBuyer.myOrders.filterAll")}
        </Button>
        {BUYER_ORDER_FILTER_STATUSES.map((value) => (
          <Button
            key={value}
            type="button"
            variant="outline"
            size="sm"
            className={`rounded-full px-4 font-semibold shadow-sm ${
              selectedStatus === value
                ? "border-emerald-600 bg-emerald-600 text-white hover:bg-emerald-700 hover:text-white"
                : "border-gray-200 bg-white text-gray-700 hover:bg-gray-50"
            }`}
            onClick={() => onSelectStatus(value)}
          >
            {getMarketplaceOrderStatusLabel(value, t)}
          </Button>
        ))}
      </div>
    </div>
  );
}

function OrderItemRow({
  item,
  t,
}: {
  item: MarketplaceOrderItem;
  t: Translator;
}) {
  const [imgError, setImgError] = useState(false);

  return (
    <div className="flex items-center gap-3 py-3 sm:gap-4">
      {/* Thumbnail */}
      <div className="shrink-0">
        {item.imageUrl && !imgError ? (
          <img
            src={item.imageUrl}
            alt={item.productName}
            className="h-16 w-16 rounded-xl bg-gray-100 object-cover"
            referrerPolicy="no-referrer"
            onError={() => setImgError(true)}
          />
        ) : (
          <div className="flex h-16 w-16 items-center justify-center rounded-xl border border-gray-200 bg-gray-50">
            <Package size={18} className="text-gray-400" />
          </div>
        )}
      </div>

      {/* Product info */}
      <div className="min-w-0 flex-1">
        <p className="truncate text-sm font-semibold text-gray-900">{item.productName}</p>
        <p className="mt-1 truncate text-sm text-gray-500">
          {t("marketplaceBuyer.myOrders.quantity")}: {item.quantity} x {formatVnd(item.unitPriceSnapshot)}
        </p>
      </div>

      {/* Line total — always right-aligned */}
      <p className="shrink-0 text-sm font-semibold text-gray-900">
        {formatVnd(item.lineTotal)}
      </p>
    </div>
  );
}

function OrderActionButtons({ orderId, t }: { orderId: number; t: Translator }) {
  return (
    <Link to={`/marketplace/orders/${orderId}`} className="shrink-0">
      <Button
        variant="outline"
        size="sm"
        className="flex items-center gap-1 rounded-full border-emerald-500 bg-white px-4 text-emerald-600 hover:bg-emerald-50 hover:border-emerald-600"
      >
        {t("marketplaceBuyer.myOrders.orderDetail")}
        <ChevronRight size={15} />
      </Button>
    </Link>
  );
}

function OrderCard({ order, t }: { order: MarketplaceOrder; t: Translator }) {
  const cancellable = isMarketplaceBuyerOrderCancellable(order);
  const itemCount = order.items.length;

  return (
    <article className="rounded-2xl border border-gray-200 bg-white p-4 shadow-sm transition-shadow hover:shadow-md sm:p-5">
      <header className="flex items-start justify-between gap-4">
        <div className="min-w-0">
          <h2 className="truncate text-lg font-bold leading-tight text-gray-900">#{order.orderCode}</h2>
          <p className="mt-1 text-sm text-gray-500">
            {t("marketplaceBuyer.myOrders.orderDate")}: {formatDate(order.createdAt)}
          </p>
        </div>
        <StatusBadge status={order.status} orderId={order.id} t={t} />
      </header>

      <div className="mt-3 flex flex-wrap gap-2">
        <MetadataChip
          className="border-blue-200 bg-blue-50 text-blue-700"
          icon={<Truck size={14} className="shrink-0" />}
        >
          {t("marketplaceBuyer.myOrders.group")}: {getMarketplaceOrderStatusGroup(order.status, t)}
        </MetadataChip>
        <MetadataChip
          className={
            cancellable
              ? "border-emerald-200 bg-emerald-50 text-emerald-700"
              : "border-gray-200 bg-gray-50 text-gray-600"
          }
          icon={
            cancellable ? (
              <ShieldCheck size={14} className="shrink-0" />
            ) : (
              <Ban size={14} className="shrink-0" />
            )
          }
        >
          {cancellable
            ? t("marketplaceBuyer.myOrders.eligibleForCancellation")
            : t("marketplaceBuyer.myOrders.cannotCancel")}
        </MetadataChip>
        <PaymentStatusChip payment={order.payment} t={t} />
      </div>

      <div className="mt-4 divide-y divide-gray-100 border-t border-gray-200">
        {order.items.map((item) => (
          <OrderItemRow key={item.id} item={item} t={t} />
        ))}
      </div>

      <footer className="flex flex-wrap items-center gap-x-4 gap-y-2 border-t border-gray-200 pt-3">
        {/* Left: item count */}
        <div className="inline-flex shrink-0 items-center gap-1.5 text-sm text-gray-500">
          <Package size={15} className="text-gray-400" />
          <span>{getItemCountLabel(itemCount, t)}</span>
        </div>

        {/* Right: total + detail button — pushed to the far right */}
        <div className="ml-auto flex items-center gap-3">
          <p className="whitespace-nowrap text-sm text-gray-600">
            {getTotalLabel(itemCount, t)}:{" "}
            <span className="text-base font-bold text-emerald-600">{formatVnd(order.totalAmount)}</span>
          </p>
          <div className="h-4 w-px shrink-0 bg-gray-200" />
          <OrderActionButtons orderId={order.id} t={t} />
        </div>
      </footer>
    </article>
  );
}

function OrdersPageShell({ children }: { children: ReactNode }) {
  return (
    <div className="min-h-[calc(100vh-5rem)] bg-gray-50">
      <div className="mx-auto w-full max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
        <BackButton to="/marketplace" className="mb-4 w-fit" />
        {children}
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
      <OrdersPageShell>
        <div className="rounded-2xl border border-dashed border-gray-200 bg-white p-8 text-center text-sm text-gray-500">
          {t("marketplaceBuyer.myOrders.loadingOrders")}
        </div>
      </OrdersPageShell>
    );
  }

  if (ordersQuery.isError) {
    return (
      <OrdersPageShell>
        <div className="rounded-2xl border border-dashed border-red-200 bg-white p-8 text-center text-sm text-red-600">
          {t("marketplaceBuyer.myOrders.errorOrders")}
        </div>
      </OrdersPageShell>
    );
  }

  const orderPage = ordersQuery.data;
  const orders = orderPage?.items ?? [];
  const totalPages = Math.max(orderPage?.totalPages ?? 1, 1);

  return (
    <OrdersPageShell>
      <div className="space-y-1.5">
        <h1 className="text-2xl font-bold leading-tight text-gray-900">{t("marketplaceBuyer.myOrders.title")}</h1>
        <p className="text-sm text-gray-600">{t("marketplaceBuyer.myOrders.subtitle")}</p>
      </div>

      <OrderFilterTabs
        selectedStatus={selectedStatus}
        onSelectStatus={(status) => updateParams({ status })}
        t={t}
      />

      <div className="mt-4 space-y-3 sm:space-y-4">
        {orders.map((order) => (
          <OrderCard key={order.id} order={order} t={t} />
        ))}

        {orders.length === 0 && (
          <div className="rounded-2xl border border-gray-200 bg-white py-16 text-center shadow-sm">
            <Package size={44} className="mx-auto mb-4 text-gray-300" />
            <h3 className="mb-2 text-lg font-semibold text-gray-900">{t("marketplaceBuyer.myOrders.emptyTitle")}</h3>
            <p className="mb-6 text-sm text-gray-500">{t("marketplaceBuyer.myOrders.emptyDesc")}</p>
            <Link to="/marketplace/products">
              <Button className="rounded-full bg-emerald-600 px-5 text-white shadow-sm hover:bg-emerald-700">
                {t("marketplaceBuyer.myOrders.startShopping")}
              </Button>
            </Link>
          </div>
        )}

        {orders.length > 0 && (
          <div className="flex items-center justify-between rounded-2xl border border-gray-200 bg-white px-4 py-3 shadow-sm">
            <p className="text-sm text-gray-500">
              {t("marketplaceBuyer.myOrders.page")} {page} {t("marketplaceBuyer.myOrders.of")} {totalPages}
            </p>
            <div className="flex gap-2">
              <Button
                variant="outline"
                size="sm"
                className="rounded-full border-gray-300 bg-white"
                disabled={page <= 1}
                onClick={() => updateParams({ page: String(page - 1) })}
              >
                {t("marketplaceBuyer.myOrders.previous")}
              </Button>
              <Button
                variant="outline"
                size="sm"
                className="rounded-full border-gray-300 bg-white"
                disabled={page >= totalPages}
                onClick={() => updateParams({ page: String(page + 1) })}
              >
                {t("marketplaceBuyer.myOrders.next")}
              </Button>
            </div>
          </div>
        )}
      </div>
    </OrdersPageShell>
  );
}
