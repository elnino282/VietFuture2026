import type { MarketplaceOrder, MarketplaceOrderStatus } from "@/shared/api";

export type CanonicalMarketplaceOrderStatus = Exclude<
  MarketplaceOrderStatus,
  "PENDING" | "DELIVERING"
>;

export type MarketplaceOrderStatusTone =
  | "warning"
  | "info"
  | "secondary"
  | "primary"
  | "success"
  | "destructive"
  | "neutral";

export type MarketplaceOrderLifecycleGroup =
  | "payment"
  | "review"
  | "processing"
  | "shipping"
  | "completed"
  | "cancelled"
  | "unknown";

type Translator = (key: string, options?: Record<string, unknown>) => string;
type OrderStatusLabelPrefix =
  | "marketplaceBuyer.myOrders.status"
  | "marketplaceSeller.status.order";

const CANONICAL_STATUS_SET = new Set<CanonicalMarketplaceOrderStatus>([
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
]);

const LEGACY_STATUS_MAP = {
  // Legacy compatibility: kept for old fixtures/data snapshots.
  PENDING: "PENDING_PAYMENT",
  DELIVERING: "SHIPPED",
} as const;

const BUYER_FALLBACK_CANCELLABLE_STATUSES = new Set<CanonicalMarketplaceOrderStatus>([
  "PENDING_PAYMENT",
  "PAYMENT_SUBMITTED",
  "CONFIRMED",
]);

export const BUYER_ORDER_FILTER_STATUSES: CanonicalMarketplaceOrderStatus[] = [
  "PENDING_PAYMENT",
  "CONFIRMED",
  "PREPARING",
  "SHIPPED",
  "DELIVERED",
  "COMPLETED",
  "CANCELLED",
];

export function normalizeMarketplaceOrderStatus(
  status: MarketplaceOrderStatus | string,
): CanonicalMarketplaceOrderStatus | "UNKNOWN" {
  const maybeMapped =
    status in LEGACY_STATUS_MAP
      ? LEGACY_STATUS_MAP[status as keyof typeof LEGACY_STATUS_MAP]
      : status;
  return CANONICAL_STATUS_SET.has(maybeMapped as CanonicalMarketplaceOrderStatus)
    ? (maybeMapped as CanonicalMarketplaceOrderStatus)
    : "UNKNOWN";
}

export function getMarketplaceOrderStatusLabel(
  status: MarketplaceOrderStatus | string,
  t: Translator,
  labelPrefix: OrderStatusLabelPrefix = "marketplaceBuyer.myOrders.status",
): string {
  if (labelPrefix === "marketplaceBuyer.myOrders.status" && (status === "PENDING" || status === "DELIVERING")) {
    return t(`${labelPrefix}.${status}`);
  }
  const normalizedStatus = normalizeMarketplaceOrderStatus(status);
  if (normalizedStatus === "UNKNOWN") {
    return t(`${labelPrefix}.unknown`, { status });
  }
  return t(`${labelPrefix}.${normalizedStatus}`);
}

export function getMarketplaceOrderStatusTone(
  status: MarketplaceOrderStatus | string,
): MarketplaceOrderStatusTone {
  const normalizedStatus = normalizeMarketplaceOrderStatus(status);
  switch (normalizedStatus) {
    case "PENDING_PAYMENT":
      return "warning";
    case "PAYMENT_SUBMITTED":
      return "info";
    case "PAYMENT_VERIFIED":
      return "secondary";
    case "CONFIRMED":
    case "PREPARING":
      return "primary";
    case "SHIPPED":
    case "DELIVERED":
      return "secondary";
    case "COMPLETED":
      return "success";
    case "REJECTED":
    case "CANCELLED":
      return "destructive";
    default:
      return "neutral";
  }
}

export function getMarketplaceOrderStatusGroup(
  status: MarketplaceOrderStatus | string,
  t: Translator,
): string {
  const normalizedStatus = normalizeMarketplaceOrderStatus(status);
  let group: MarketplaceOrderLifecycleGroup;
  switch (normalizedStatus) {
    case "PENDING_PAYMENT":
    case "PAYMENT_SUBMITTED":
      group = "payment";
      break;
    case "PAYMENT_VERIFIED":
      group = "review";
      break;
    case "CONFIRMED":
    case "PREPARING":
      group = "processing";
      break;
    case "SHIPPED":
    case "DELIVERED":
      group = "shipping";
      break;
    case "COMPLETED":
      group = "completed";
      break;
    case "REJECTED":
    case "CANCELLED":
      group = "cancelled";
      break;
    default:
      group = "unknown";
      break;
  }
  return t(`marketplaceBuyer.myOrders.statusGroup.${group}`);
}

export function getMarketplaceOrderStatusBadgeClass(
  status: MarketplaceOrderStatus | string,
): string {
  const tone = getMarketplaceOrderStatusTone(status);
  switch (tone) {
    case "warning":
      return "bg-orange-100 text-orange-700 border border-orange-200";
    case "info":
      return "bg-amber-100 text-amber-700 border border-amber-200";
    case "primary":
      return "bg-blue-100 text-blue-700 border border-blue-200";
    case "secondary":
      return "bg-sky-100 text-sky-700 border border-sky-200";
    case "success":
      return "bg-emerald-100 text-primary border border-emerald-200";
    case "destructive":
      return "bg-red-100 text-destructive border border-destructive/30";
    default:
      return "bg-muted text-muted-foreground border border-border";
  }
}

export function isMarketplaceBuyerOrderCancellable(
  order: Pick<MarketplaceOrder, "status" | "canCancel">,
): boolean {
  if (typeof order.canCancel === "boolean") {
    return order.canCancel;
  }
  const normalizedStatus = normalizeMarketplaceOrderStatus(order.status);
  return (
    normalizedStatus !== "UNKNOWN" &&
    BUYER_FALLBACK_CANCELLABLE_STATUSES.has(normalizedStatus)
  );
}
