import { useState } from "react";
import { ChevronRight, Package } from "lucide-react";
import { Link, useSearchParams } from "react-router-dom";
import { Button } from "@/shared/ui";
import { type MarketplaceOrder, type MarketplaceOrderItem, type MarketplaceOrderStatus } from "@/shared/api";
import { useMarketplaceOrders } from "@/features/marketplace/hooks";
import { formatDate, formatVnd } from "@/features/marketplace/lib/format";

const ORDER_STATUSES: Array<{ value: MarketplaceOrderStatus }> = [
  { value: "PENDING" },
  { value: "CONFIRMED" },
  { value: "PREPARING" },
  { value: "DELIVERING" },
  { value: "COMPLETED" },
  { value: "CANCELLED" },
];

const STATUS_LABELS: Record<string, string> = {
  PENDING: "Chờ xử lý",
  CONFIRMED: "Đã xác nhận",
  PREPARING: "Đang chuẩn bị",
  DELIVERING: "Đang giao",
  COMPLETED: "Hoàn tất",
  CANCELLED: "Đã hủy",
};

const STATUS_COLORS: Record<string, string> = {
  PENDING: "bg-orange-100 text-orange-700 border border-orange-200",
  CONFIRMED: "bg-blue-100 text-blue-700 border border-blue-200",
  PREPARING: "bg-indigo-100 text-indigo-700 border border-indigo-200",
  DELIVERING: "bg-sky-100 text-sky-700 border border-sky-200",
  COMPLETED: "bg-emerald-100 text-primary border border-emerald-200",
  CANCELLED: "bg-red-100 text-destructive border border-destructive/30",
};

function getStatusLabel(status: string): string {
  return STATUS_LABELS[status] ?? status;
}

function getStatusColor(status: string): string {
  return STATUS_COLORS[status] ?? "bg-muted text-muted-foreground border border-border";
}

function toPositiveInt(value: string | null, fallback: number) {
  const parsed = Number(value);
  return Number.isFinite(parsed) && parsed > 0 ? Math.floor(parsed) : fallback;
}

function StatusBadge({ status }: { status: string }) {
  return (
    <span
      className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-semibold ${getStatusColor(status)}`}
    >
      {getStatusLabel(status)}
    </span>
  );
}

function OrderItemPreview({ item }: { item: MarketplaceOrderItem }) {
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
          Số lượng: {item.quantity} x {formatVnd(item.unitPriceSnapshot)}
        </p>
      </div>
      <span className="text-sm font-semibold text-foreground shrink-0">
        {formatVnd(item.lineTotal)}
      </span>
    </div>
  );
}

function OrderSummaryCard({ order }: { order: MarketplaceOrder }) {
  const previewItems = order.items.slice(0, 2);
  const extraCount = order.items.length - previewItems.length;

  return (
    <div className="rounded-xl border border-border bg-card shadow-sm hover:shadow-md transition overflow-hidden">
      <div className="flex items-start justify-between px-4 py-3 border-b border-border">
        <div>
          <p className="text-sm font-medium text-muted-foreground">
            Mã đơn: <span className="font-semibold text-foreground">#{order.orderCode}</span>
          </p>
          <p className="text-xs text-muted-foreground/70 mt-0.5">Đặt ngày: {formatDate(order.createdAt)}</p>
        </div>
        <StatusBadge status={order.status} />
      </div>
      <div className="divide-y divide-border">
        {previewItems.map((item) => (
          <OrderItemPreview key={item.id} item={item} />
        ))}
        {extraCount > 0 && (
          <p className="px-4 py-2 text-xs text-muted-foreground/60 italic">+{extraCount} sản phẩm khác</p>
        )}
      </div>
      <div className="border-t border-border px-4 py-3 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
        <p className="text-sm text-muted-foreground flex-1">
          Tổng tiền ({order.items.length} sản phẩm):{" "}
          <span className="font-bold text-primary">{formatVnd(order.totalAmount)}</span>
        </p>
        <Link to={`/marketplace/orders/${order.id}`} className="shrink-0">
          <Button
            variant="outline"
            size="sm"
            className="w-full sm:w-auto flex items-center justify-center gap-1 acm-rounded-sm border-border hover:bg-muted transition-colors duration-200"
          >
            Xem chi tiết <ChevronRight size={16} />
          </Button>
        </Link>
      </div>
    </div>
  );
}

export function MyOrdersPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const page = toPositiveInt(searchParams.get("page"), 1);
  const statusParam = searchParams.get("status");
  const selectedStatus = ORDER_STATUSES.some((item) => item.value === statusParam)
    ? (statusParam as MarketplaceOrderStatus)
    : undefined;

  const ordersQuery = useMarketplaceOrders({ status: selectedStatus, page: page - 1, size: 10 });

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
          Đang tải đơn hàng...
        </div>
      </div>
    );
  }

  if (ordersQuery.isError) {
    return (
      <div className="max-w-[1800px] mx-auto px-6 pt-6">
        <div className="rounded-xl border border-dashed border-destructive/30 bg-card p-8 text-center text-sm text-destructive">
          Không thể tải đơn hàng. Vui lòng thử lại.
        </div>
      </div>
    );
  }

  const orderPage = ordersQuery.data;
  const orders = orderPage?.items ?? [];
  const totalPages = Math.max(orderPage?.totalPages ?? 1, 1);

  return (
    <div className="max-w-[1800px] mx-auto px-6 pt-6">
      <h1 className="text-2xl font-bold text-foreground">Đơn hàng của tôi</h1>

      <div className="mt-4 flex flex-wrap gap-2">
        <Button
          type="button"
          variant="outline"
          size="sm"
          style={!selectedStatus ? { backgroundColor: "var(--primary)", color: "#fff", borderColor: "var(--primary)" } : undefined}
          onClick={() => updateParams({ status: null })}
        >
          Tất cả
        </Button>
        {ORDER_STATUSES.map(({ value }) => (
          <Button
            key={value}
            type="button"
            variant="outline"
            size="sm"
            style={selectedStatus === value ? { backgroundColor: "var(--primary)", color: "#fff", borderColor: "var(--primary)" } : undefined}
            onClick={() => updateParams({ status: value === selectedStatus ? null : value })}
          >
            {getStatusLabel(value)}
          </Button>
        ))}
      </div>

      <div className="mt-6 space-y-4">
        {orders.map((order) => (
          <OrderSummaryCard key={order.id} order={order} />
        ))}

        {orders.length === 0 && (
          <div className="rounded-xl border border-border bg-card py-20 text-center">
            <Package size={48} className="mx-auto mb-4 text-muted-foreground/40" />
            <h3 className="mb-2 text-lg font-medium text-foreground">Bạn chưa có đơn hàng nào.</h3>
            <p className="mb-6 text-sm text-muted-foreground">
              Hãy khám phá sản phẩm nông sản và đặt đơn đầu tiên của bạn.
            </p>
            <Link to="/marketplace/products">
              <Button className="bg-primary hover:bg-primary/90 text-white acm-rounded-sm acm-button-shadow">Mua sắm ngay</Button>
            </Link>
          </div>
        )}

        {orders.length > 0 && (
          <div className="flex items-center justify-between rounded-xl border border-border bg-card px-4 py-3">
            <p className="text-sm text-muted-foreground">
              Trang {page} / {totalPages}
            </p>
            <div className="flex gap-2">
              <Button
                variant="outline"
                size="sm"
                disabled={page <= 1}
                onClick={() => updateParams({ page: String(page - 1) })}
              >
                Trước
              </Button>
              <Button
                variant="outline"
                size="sm"
                disabled={page >= totalPages}
                onClick={() => updateParams({ page: String(page + 1) })}
              >
                Sau
              </Button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
