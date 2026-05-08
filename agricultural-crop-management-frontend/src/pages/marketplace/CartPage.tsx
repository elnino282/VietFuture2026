import { Minus, Plus, ShoppingCart, Trash2 } from "lucide-react";
import { Link, useNavigate } from "react-router-dom";
import { Button, Card, CardContent } from "@/shared/ui";
import {
  useMarketplaceCart,
  useMarketplaceRemoveCartItemMutation,
  useMarketplaceUpdateCartItemMutation,
} from "@/features/marketplace/hooks";
import { formatVnd } from "@/features/marketplace/lib/format";

const SHIPPING_FEE = 20_000;

export function CartPage() {
  const navigate = useNavigate();

  const cartQuery = useMarketplaceCart();
  const updateItemMutation = useMarketplaceUpdateCartItemMutation();
  const removeItemMutation = useMarketplaceRemoveCartItemMutation();

  if (cartQuery.isLoading) {
    return (
      <div className="max-w-[1800px] mx-auto px-6 pt-6">
        <div className="rounded-xl border border-dashed border-border bg-card p-8 text-center text-sm text-muted-foreground">
          Đang tải giỏ hàng...
        </div>
      </div>
    );
  }

  if (cartQuery.isError) {
    return (
      <div className="max-w-[1800px] mx-auto px-6 pt-6">
        <div className="rounded-xl border border-dashed border-destructive/30 bg-card p-8 text-center text-sm text-destructive">
          Không thể tải giỏ hàng từ máy chủ.
        </div>
      </div>
    );
  }

  const cart = cartQuery.data;

  if (!cart || cart.items.length === 0) {
    return (
      <div className="max-w-[1800px] mx-auto px-6 pt-6 text-center py-12">
        <div className="mx-auto mb-6 flex h-24 w-24 items-center justify-center rounded-full bg-muted">
          <ShoppingCart size={48} className="text-muted-foreground/50" />
        </div>
        <h2 className="mb-4 text-2xl font-bold text-foreground">Giỏ hàng trống</h2>
        <p className="mb-8 text-muted-foreground">Bạn chưa có sản phẩm nào trong giỏ hàng.</p>
        <Button className="bg-primary hover:bg-primary/90 text-white acm-rounded-sm acm-button-shadow" onClick={() => navigate("/marketplace/products")}>Tiếp tục mua sắm</Button>
      </div>
    );
  }

  const isMutating = updateItemMutation.isPending || removeItemMutation.isPending;
  const total = (cart.subtotal ?? 0) + SHIPPING_FEE;

  const sellerGroups = cart.items.reduce<Array<{ farmerUserId: number; items: typeof cart.items }>>((groups, item) => {
    const existing = groups.find((group) => group.farmerUserId === item.farmerUserId);
    if (existing) existing.items.push(item);
    else groups.push({ farmerUserId: item.farmerUserId, items: [item] });
    return groups;
  }, []);

  return (
    <div className="max-w-[1800px] mx-auto px-6 pt-6">
      <h1 className="mb-8 text-3xl font-bold text-foreground">Giỏ hàng của bạn</h1>

      <div className="flex flex-col gap-8 lg:flex-row">
        <div className="flex-1 space-y-6">
          {sellerGroups.map((group) => (
            <div key={group.farmerUserId} className="space-y-4">
              <div className="flex items-center justify-between rounded-xl bg-muted px-4 py-3">
                <h2 className="text-sm font-semibold text-foreground">Người bán #{group.farmerUserId}</h2>
                <span className="text-sm font-medium text-muted-foreground">
                  {formatVnd(group.items.reduce((sum, item) => sum + item.unitPrice * item.quantity, 0))}
                </span>
              </div>
              {group.items.map((item) => (
                <Card key={item.productId} className="overflow-hidden border border-border bg-card rounded-xl shadow-sm transition-all hover:border-primary/30 hover:shadow-md">
                  <CardContent className="flex items-start gap-4 p-6">
                    <img
                      src={item.imageUrl}
                      alt={item.name}
                      className="h-20 w-20 rounded-lg bg-muted object-cover"
                      referrerPolicy="no-referrer"
                    />
                    <div className="flex-1 space-y-1">
                      <Link
                        to={`/marketplace/products/${item.slug}`}
                        className="line-clamp-2 text-base font-semibold text-foreground transition-colors hover:text-primary"
                      >
                        {item.name}
                      </Link>
                      <div className="text-sm text-muted-foreground">
                        {formatVnd(item.unitPrice)}
                      </div>
                    </div>

                    <div className="flex flex-col items-end space-y-2">
                      <div className="flex items-center gap-2">
                        <div className="flex items-center rounded-xl border border-border">
                          <button
                            type="button"
                            className="h-8 w-8 text-muted-foreground transition-colors hover:bg-emerald-50 hover:text-primary active:bg-emerald-100 disabled:cursor-not-allowed disabled:opacity-50"
                            disabled={isMutating}
                            onClick={async () => {
                              const next = Math.max(1, item.quantity - 1);
                              await updateItemMutation.mutateAsync({
                                productId: item.productId,
                                request: { quantity: next },
                              });
                            }}
                          >
                            <Minus size={16} />
                          </button>
                          <span className="min-w-12 text-center text-sm font-medium">{item.quantity}</span>
                          <button
                            type="button"
                            className="h-8 w-8 text-muted-foreground transition-colors hover:bg-emerald-50 hover:text-primary active:bg-emerald-100 disabled:cursor-not-allowed disabled:opacity-50"
                            disabled={isMutating}
                            onClick={async () => {
                              const next = Math.min(item.maxQuantity, item.quantity + 1);
                              await updateItemMutation.mutateAsync({
                                productId: item.productId,
                                request: { quantity: next },
                              });
                            }}
                          >
                            <Plus size={16} />
                          </button>
                        </div>

                        <button
                          type="button"
                          className="h-8 w-8 rounded-md text-destructive transition-colors hover:bg-red-50 hover:text-red-700 disabled:cursor-not-allowed disabled:opacity-50"
                          disabled={isMutating}
                          onClick={async () => {
                            await removeItemMutation.mutateAsync(item.productId);
                          }}
                        >
                          <Trash2 size={18} />
                        </button>
                      </div>

                      <p className="text-base font-bold text-primary">
                        {formatVnd(item.unitPrice * item.quantity)}
                      </p>
                    </div>
                  </CardContent>
                </Card>
              ))}
            </div>
          ))}
        </div>

        <div className="w-full shrink-0 lg:w-96">
          <Card className="sticky top-24 border border-border bg-muted/50 shadow-lg rounded-xl">
            <CardContent className="p-6">
              <h3 className="mb-4 text-xl font-bold">Tổng đơn hàng</h3>
              <div className="mb-6 space-y-3 text-sm">
                <div className="flex justify-between">
                  <span className="text-muted-foreground">Tạm tính:</span>
                  <span className="font-medium">{formatVnd(cart.subtotal)}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-muted-foreground">Phí giao hàng:</span>
                  <span className="font-medium">{formatVnd(SHIPPING_FEE)}</span>
                </div>
                <div className="flex items-center justify-between border-t border-border pt-3">
                  <span className="font-bold text-foreground">Tổng cộng:</span>
                  <span className="text-2xl font-bold text-primary">{formatVnd(total)}</span>
                </div>
              </div>
              <Button className="w-full bg-primary hover:bg-primary/90 text-white acm-rounded-sm acm-button-shadow" size="lg" onClick={() => navigate("/marketplace/checkout")}>
                Tiến hành thanh toán
              </Button>
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
}
