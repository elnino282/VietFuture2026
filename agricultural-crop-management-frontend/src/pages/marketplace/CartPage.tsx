import { Minus, Package, Plus, ShoppingCart, Store, Trash2 } from "lucide-react";
import { Link, useNavigate } from "react-router-dom";
import { BackButton, Button, Card, CardContent } from "@/shared/ui";
import {
  useMarketplaceCart,
  useMarketplaceRemoveCartItemMutation,
  useMarketplaceUpdateCartItemMutation,
} from "@/features/marketplace/hooks";
import { formatVnd } from "@/features/marketplace/lib/format";

const SHIPPING_FEE = 20_000;

/* ------------------------------------------------------------------ */
/*  Sub-components                                                     */
/* ------------------------------------------------------------------ */

function CartEmpty() {
  const navigate = useNavigate();
  return (
    <div className="flex flex-col items-center justify-center py-20">
      <div className="relative mb-8">
        <div className="flex h-28 w-28 items-center justify-center rounded-full bg-gradient-to-br from-emerald-50 to-emerald-100">
          <ShoppingCart size={52} className="text-emerald-300" />
        </div>
        <div className="absolute -bottom-1 -right-1 flex h-10 w-10 items-center justify-center rounded-full border-4 border-white bg-muted">
          <Package size={18} className="text-muted-foreground" />
        </div>
      </div>
      <h2 className="mb-2 text-2xl font-bold text-foreground">Giỏ hàng trống</h2>
      <p className="mb-8 max-w-sm text-center text-muted-foreground">
        Bạn chưa có sản phẩm nào trong giỏ hàng. Hãy khám phá các sản phẩm nông sản tươi ngon!
      </p>
      <Button
        className="bg-primary hover:bg-primary/90 text-white acm-rounded-sm acm-button-shadow px-8"
        size="lg"
        onClick={() => navigate("/marketplace/products")}
      >
        Khám phá sản phẩm
      </Button>
    </div>
  );
}

function QuantityStepper({
  quantity,
  maxQuantity,
  disabled,
  onDecrease,
  onIncrease,
}: {
  quantity: number;
  maxQuantity: number;
  disabled: boolean;
  onDecrease: () => void;
  onIncrease: () => void;
}) {
  return (
    <div className="flex items-center rounded-lg border border-border bg-card">
      <button
        type="button"
        aria-label="Giảm số lượng"
        className="flex h-9 w-9 items-center justify-center rounded-l-lg text-muted-foreground transition-colors hover:bg-emerald-50 hover:text-primary active:bg-emerald-100 disabled:cursor-not-allowed disabled:opacity-40"
        disabled={disabled || quantity <= 1}
        onClick={onDecrease}
      >
        <Minus size={15} />
      </button>
      <span className="flex min-w-[3rem] items-center justify-center border-x border-border text-sm font-semibold tabular-nums">
        {quantity}
      </span>
      <button
        type="button"
        aria-label="Tăng số lượng"
        className="flex h-9 w-9 items-center justify-center rounded-r-lg text-muted-foreground transition-colors hover:bg-emerald-50 hover:text-primary active:bg-emerald-100 disabled:cursor-not-allowed disabled:opacity-40"
        disabled={disabled || quantity >= maxQuantity}
        onClick={onIncrease}
      >
        <Plus size={15} />
      </button>
    </div>
  );
}

function CartItemRow({
  item,
  isMutating,
  onUpdate,
  onRemove,
}: {
  item: {
    productId: number;
    slug: string;
    name: string;
    imageUrl: string;
    unitPrice: number;
    quantity: number;
    maxQuantity: number;
  };
  isMutating: boolean;
  onUpdate: (productId: number, quantity: number) => void;
  onRemove: (productId: number) => void;
}) {
  return (
    <div className="group flex gap-4 rounded-xl border border-border bg-card p-4 transition-all hover:border-primary/20 hover:shadow-md sm:p-5">
      {/* Thumbnail */}
      <Link
        to={`/marketplace/products/${item.slug}`}
        className="shrink-0 overflow-hidden rounded-lg"
      >
        <img
          src={item.imageUrl}
          alt={item.name}
          className="h-20 w-20 rounded-lg bg-muted object-cover transition-transform group-hover:scale-105 sm:h-24 sm:w-24"
          referrerPolicy="no-referrer"
        />
      </Link>

      {/* Info + actions */}
      <div className="flex min-w-0 flex-1 flex-col justify-between gap-3">
        {/* Top row: name & delete */}
        <div className="flex items-start justify-between gap-2">
          <div className="min-w-0 flex-1">
            <Link
              to={`/marketplace/products/${item.slug}`}
              className="line-clamp-2 text-sm font-semibold leading-snug text-foreground transition-colors hover:text-primary sm:text-base"
            >
              {item.name}
            </Link>
            <p className="mt-1 text-xs text-muted-foreground sm:text-sm">
              {formatVnd(item.unitPrice)} / đơn vị
            </p>
          </div>

          <button
            type="button"
            aria-label="Xoá sản phẩm"
            className="shrink-0 rounded-lg p-2 text-muted-foreground transition-colors hover:bg-red-50 hover:text-destructive disabled:cursor-not-allowed disabled:opacity-40"
            disabled={isMutating}
            onClick={() => onRemove(item.productId)}
          >
            <Trash2 size={17} />
          </button>
        </div>

        {/* Bottom row: stepper & line total */}
        <div className="flex flex-wrap items-center justify-between gap-3">
          <QuantityStepper
            quantity={item.quantity}
            maxQuantity={item.maxQuantity}
            disabled={isMutating}
            onDecrease={() => onUpdate(item.productId, Math.max(1, item.quantity - 1))}
            onIncrease={() => onUpdate(item.productId, Math.min(item.maxQuantity, item.quantity + 1))}
          />

          <p className="text-base font-bold tabular-nums text-primary">
            {formatVnd(item.unitPrice * item.quantity)}
          </p>
        </div>
      </div>
    </div>
  );
}

/* ------------------------------------------------------------------ */
/*  Main page                                                          */
/* ------------------------------------------------------------------ */

export function CartPage() {
  const navigate = useNavigate();

  const cartQuery = useMarketplaceCart();
  const updateItemMutation = useMarketplaceUpdateCartItemMutation();
  const removeItemMutation = useMarketplaceRemoveCartItemMutation();

  /* ---------- Loading ---------- */
  if (cartQuery.isLoading) {
    return (
      <div className="max-w-[1800px] mx-auto px-6 pt-6">
        <BackButton to="/marketplace/products" className="mb-4 w-fit" />
        <div className="mb-8 h-9 w-56 animate-pulse rounded-lg bg-muted" />
        <div className="flex flex-col gap-8 lg:flex-row">
          <div className="flex-1 space-y-4">
            {[1, 2].map((i) => (
              <div key={i} className="h-28 animate-pulse rounded-xl bg-muted" />
            ))}
          </div>
          <div className="w-full shrink-0 lg:w-80">
            <div className="h-56 animate-pulse rounded-xl bg-muted" />
          </div>
        </div>
      </div>
    );
  }

  /* ---------- Error ---------- */
  if (cartQuery.isError) {
    return (
      <div className="max-w-[1800px] mx-auto px-6 pt-6">
        <BackButton to="/marketplace/products" className="mb-4 w-fit" />
        <div className="rounded-xl border border-dashed border-destructive/30 bg-card p-8 text-center text-sm text-destructive">
          Không thể tải giỏ hàng từ máy chủ. Vui lòng thử lại sau.
        </div>
      </div>
    );
  }

  /* ---------- Empty ---------- */
  const cart = cartQuery.data;
  if (!cart || cart.items.length === 0) {
    return (
      <div className="max-w-[1800px] mx-auto px-6 pt-6">
        <BackButton to="/marketplace/products" className="mb-4 w-fit" />
        <CartEmpty />
      </div>
    );
  }

  /* ---------- With items ---------- */
  const isMutating = updateItemMutation.isPending || removeItemMutation.isPending;
  const total = (cart.subtotal ?? 0) + SHIPPING_FEE;

  /* Use seller groups from API (contains farmerName, farmName).
     Fall back to manual grouping for backward compatibility with
     older API responses that may not include sellerGroups. */
  const sellerGroups =
    cart.sellerGroups && cart.sellerGroups.length > 0
      ? cart.sellerGroups
      : cart.items.reduce<
          Array<{ farmerUserId: number; farmerName: string | null; farmName: string | null; items: typeof cart.items }>
        >((groups, item) => {
          const existing = groups.find((group) => group.farmerUserId === item.farmerUserId);
          if (existing) existing.items.push(item);
          else groups.push({ farmerUserId: item.farmerUserId, farmerName: null, farmName: null, items: [item] });
          return groups;
        }, []);

  const handleUpdate = async (productId: number, quantity: number) => {
    await updateItemMutation.mutateAsync({ productId, request: { quantity } });
  };

  const handleRemove = async (productId: number) => {
    await removeItemMutation.mutateAsync(productId);
  };

  return (
    <div className="max-w-[1800px] mx-auto px-6 pt-6 pb-12">
      {/* Header */}
      <div className="mb-6 flex flex-col gap-3 sm:mb-8 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <BackButton to="/marketplace/products" className="mb-2 w-fit" />
          <h1 className="text-2xl font-bold text-foreground sm:text-3xl">
            Giỏ hàng
            <span className="ml-2 text-lg font-medium text-muted-foreground">
              ({cart.itemCount} sản phẩm)
            </span>
          </h1>
        </div>
      </div>

      {/* Body */}
      <div className="flex flex-col gap-6 lg:flex-row lg:gap-8">
        {/* Left: items */}
        <div className="flex-1 space-y-6">
          {sellerGroups.map((group) => (
            <div key={group.farmerUserId} className="space-y-3">
              {/* Seller header */}
              <div className="flex items-center gap-2 rounded-lg bg-muted/70 px-4 py-2.5">
                <Store size={16} className="text-muted-foreground" />
                <h2 className="text-sm font-semibold text-foreground">
                  {group.farmerName ?? `Người bán #${group.farmerUserId}`}
                </h2>
                {group.farmName && (
                  <span className="text-xs text-muted-foreground">
                    — {group.farmName}
                  </span>
                )}
                <span className="ml-auto text-xs font-medium tabular-nums text-muted-foreground">
                  {formatVnd(group.items.reduce((sum, item) => sum + item.unitPrice * item.quantity, 0))}
                </span>
              </div>

              {/* Items */}
              <div className="space-y-3">
                {group.items.map((item) => (
                  <CartItemRow
                    key={item.productId}
                    item={item}
                    isMutating={isMutating}
                    onUpdate={handleUpdate}
                    onRemove={handleRemove}
                  />
                ))}
              </div>
            </div>
          ))}
        </div>

        {/* Right: order summary */}
        <div className="w-full shrink-0 lg:w-80">
          <Card className="sticky top-24 overflow-hidden border border-border bg-card shadow-lg rounded-xl">
            <CardContent className="p-0">
              {/* Summary header */}
              <div className="border-b border-border bg-muted/40 px-5 py-4">
                <h3 className="text-base font-bold text-foreground">Tổng đơn hàng</h3>
              </div>

              {/* Summary body */}
              <div className="space-y-4 px-5 py-5">
                <div className="space-y-2.5 text-sm">
                  <div className="flex justify-between">
                    <span className="text-muted-foreground">Tạm tính ({cart.itemCount} sản phẩm)</span>
                    <span className="font-medium tabular-nums">{formatVnd(cart.subtotal)}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-muted-foreground">Phí giao hàng</span>
                    <span className="font-medium tabular-nums">{formatVnd(SHIPPING_FEE)}</span>
                  </div>
                </div>

                <div className="border-t border-border pt-4">
                  <div className="flex items-baseline justify-between">
                    <span className="text-sm font-bold text-foreground">Tổng cộng</span>
                    <span className="text-xl font-bold tabular-nums text-primary">{formatVnd(total)}</span>
                  </div>
                </div>

                <Button
                  className="w-full bg-primary hover:bg-primary/90 text-white acm-rounded-sm acm-button-shadow mt-1"
                  size="lg"
                  onClick={() => navigate("/marketplace/checkout")}
                >
                  Tiến hành thanh toán
                </Button>

                <p className="text-center text-xs text-muted-foreground">
                  Phí giao hàng có thể thay đổi khi chọn địa chỉ
                </p>
              </div>
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
}
