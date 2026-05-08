import { useMemo, useState } from "react";
import { ArrowLeft, Calendar, MapPin, Minus, Package, Plus, ShieldCheck, ShoppingCart, Star } from "lucide-react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { useAuth } from "@/features/auth";
import { Badge, Button } from "@/shared/ui";
import {
  useMarketplaceAddToCart,
  useMarketplaceProductDetail,
  useMarketplaceProductReviews,
  useMarketplaceTraceability,
} from "@/features/marketplace/hooks";
import { formatDateTime, formatVnd } from "@/features/marketplace/lib/format";

function StarRating({ rating }: { rating?: number }) {
  const score = Math.min(Math.max(Math.round(rating ?? 5), 1), 5);
  return (
    <div className="flex items-center gap-0.5 text-base">
      {Array.from({ length: 5 }, (_, index) => (
        <span key={index} className={index < score ? "text-amber-400" : "text-slate-200"}>
          ★
        </span>
      ))}
    </div>
  );
}

export function ProductDetailPage() {
  const { slug } = useParams<{ slug: string }>();
  const navigate = useNavigate();
  const { isAuthenticated } = useAuth();
  const [quantity, setQuantity] = useState(1);
  const [imageFailed, setImageFailed] = useState(false);
  const { addToCart, isAdding } = useMarketplaceAddToCart();

  const productQuery = useMarketplaceProductDetail(slug);
  const product = productQuery.data;
  const primaryImage = product ? (product.imageUrls[0] ?? product.imageUrl) : null;

  const reviewsQuery = useMarketplaceProductReviews(product?.id, { page: 0, size: 5 });
  const traceabilityQuery = useMarketplaceTraceability(product?.traceable ? product.id : null);

  const canIncrease = useMemo(() => {
    if (!product) {
      return false;
    }
    return quantity < product.availableQuantity;
  }, [product, quantity]);

  const quantityValue = product
    ? Math.min(Math.max(quantity, 1), Math.max(product.availableQuantity, 1))
    : quantity;

  if (productQuery.isLoading) {
    return (
      <div className="max-w-[1800px] mx-auto px-6 pt-6">
        <div className="rounded-xl border border-dashed border-border bg-card p-8 text-center text-sm text-muted-foreground">
          Đang tải chi tiết sản phẩm...
        </div>
      </div>
    );
  }

  if (productQuery.isError || !product) {
    return (
      <div className="max-w-[1800px] mx-auto px-6 pt-6">
        <div className="space-y-3 rounded-xl border border-dashed border-destructive/30 bg-card p-8 text-center text-sm text-destructive">
          <p>Sản phẩm không tồn tại hoặc chưa được công khai.</p>
          <Link to="/marketplace/products" className="text-primary hover:underline">
            Quay lại danh sách sản phẩm
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-[1800px] mx-auto px-6 pt-6">
      <div className="max-w-[1800px] mx-auto">
        <Link
          to="/marketplace/products"
          className="mb-4 inline-flex items-center gap-1 text-sm text-primary hover:underline"
        >
          <ArrowLeft size={15} /> Quay lại danh sách sản phẩm
        </Link>

        <div className="mb-6 grid grid-cols-1 gap-6 lg:grid-cols-2">
          <div className="overflow-hidden rounded-xl bg-card border border-border shadow-sm">
            <div className="aspect-[4/3] overflow-hidden bg-gradient-to-br from-gray-50 to-gray-100">
              {primaryImage && !imageFailed ? (
                <img
                  src={primaryImage}
                  alt={product.name}
                  className="h-full w-full object-cover"
                  referrerPolicy="no-referrer"
                  onError={() => setImageFailed(true)}
                />
              ) : (
                <div className="flex h-full w-full items-center justify-center">
                  <div className="text-center">
                    <Package className="mx-auto h-24 w-24 text-muted-foreground/40" strokeWidth={1.5} />
                    <p className="mt-2 text-sm text-muted-foreground/60">Không có hình ảnh</p>
                  </div>
                </div>
              )}
            </div>
          </div>

          <div className="flex flex-col space-y-6">
            <div>
              <div className="mb-3 flex items-center gap-2 flex-wrap">
                <span className="text-xs font-medium uppercase tracking-wider text-muted-foreground">
                  {product.category}
                </span>
                {product.traceable && (
                  <Badge variant="success" className="gap-1 text-xs">
                    <ShieldCheck size={12} /> Có truy xuất
                  </Badge>
                )}
              </div>

              <h1 className="mb-4 text-3xl font-bold text-foreground leading-tight">{product.name}</h1>

              <div className="mb-4 flex items-center gap-3 text-sm">
                <div className="flex items-center gap-1">
                  <Star className="fill-yellow-400 text-yellow-400" size={16} />
                  <span className="font-semibold text-foreground">
                    {product.ratingAverage ? product.ratingAverage.toFixed(1) : "5.0"}
                  </span>
                </div>
                <span className="text-muted-foreground/40">|</span>
                <span className="text-muted-foreground">{product.ratingCount} đánh giá</span>
                <span className="text-muted-foreground/40">|</span>
                <span className="text-muted-foreground">Đã bán {product.ratingCount > 100 ? `${(product.ratingCount / 100).toFixed(1)}k` : product.ratingCount}</span>
              </div>

              <div className="mb-6">
                <div className="text-4xl font-bold text-primary">
                  {formatVnd(product.price)}
                  <span className="ml-2 text-lg font-normal text-muted-foreground">/ {product.unit}</span>
                </div>
              </div>

              {product.shortDescription && (
                <p className="mb-6 text-sm leading-relaxed text-muted-foreground">{product.shortDescription}</p>
              )}
            </div>

            <div>
              <label className="mb-3 block text-sm font-medium text-foreground">Số lượng</label>
              {isAuthenticated ? (
                <div className="flex items-center gap-4">
                  <div className="flex items-center rounded-xl border border-border bg-card">
                    <button
                      className="h-9 w-9 flex items-center justify-center text-muted-foreground transition-colors hover:bg-muted/50"
                      onClick={() => setQuantity((prev) => Math.max(1, prev - 1))}
                      type="button"
                    >
                      <Minus size={16} />
                    </button>
                    <span className="min-w-12 text-center text-sm font-medium text-foreground">{quantityValue}</span>
                    <button
                      className="h-9 w-9 flex items-center justify-center text-muted-foreground transition-colors hover:bg-muted/50 disabled:cursor-not-allowed disabled:opacity-40"
                      onClick={() => setQuantity((prev) => prev + 1)}
                      disabled={!canIncrease}
                      type="button"
                    >
                      <Plus size={16} />
                    </button>
                  </div>
                  <span className="text-sm text-muted-foreground">{product.availableQuantity} sản phẩm có sẵn</span>
                </div>
              ) : (
                <div className="rounded-lg bg-amber-50 border border-amber-200 px-4 py-3 text-sm text-amber-800">
                  Hãy đăng nhập để thêm sản phẩm vào giỏ hàng hoặc đặt mua ngay.
                </div>
              )}
            </div>

            <div className="flex gap-3">
              {isAuthenticated ? (
                <>
                  <Button
                    variant="outline"
                    size="lg"
                    className="flex-1 border border-border text-foreground hover:bg-muted acm-rounded-sm"
                    disabled={isAdding || product.availableQuantity <= 0}
                    onClick={async () => {
                      await addToCart(product.id, quantity);
                    }}
                  >
                    <ShoppingCart size={18} />
                    Thêm vào giỏ
                  </Button>
                  <Button
                    size="lg"
                    className="flex-1 bg-primary hover:bg-primary/90 text-white acm-rounded-sm acm-button-shadow"
                    disabled={isAdding || product.availableQuantity <= 0}
                    onClick={async () => {
                      const mode = await addToCart(product.id, quantity);
                      if (mode === "server") {
                        navigate("/marketplace/cart");
                      }
                    }}
                  >
                    {isAdding ? "Đang xử lý..." : "Mua ngay"}
                  </Button>
                </>
              ) : (
                <>
                  <Button asChild variant="outline" size="lg" className="flex-1 border border-border acm-rounded-sm">
                    <Link to="/sign-in">Đăng nhập</Link>
                  </Button>
                  <Button asChild size="lg" className="flex-1">
                    <Link to="/sign-up">Tạo tài khoản</Link>
                  </Button>
                </>
              )}
            </div>
          </div>
        </div>

      {product.traceable ? (
        <div className="mb-6">
          <div className="mb-4 flex items-center gap-2 text-lg font-bold text-foreground">
            <ShieldCheck className="text-primary" size={24} />
            Thông tin truy xuất nguồn gốc
          </div>
          <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
            <div className="rounded-xl border border-border bg-card p-6 shadow-sm">
              <h3 className="mb-4 text-base font-bold text-primary">Thông tin Nông trại</h3>
              <div className="space-y-3 text-sm">
                <div className="flex justify-between">
                  <span className="text-muted-foreground">Tên nông trại:</span>
                  <span className="font-semibold text-foreground text-right">
                    {traceabilityQuery.data?.farm?.name ?? product.farmName ?? product.farmerDisplayName}
                  </span>
                </div>
                <div className="flex justify-between">
                  <span className="text-muted-foreground">Khu vực:</span>
                  <span className="flex items-center gap-1 font-semibold text-foreground">
                    <MapPin size={14} className="text-primary" />
                    {traceabilityQuery.data?.farm?.region ?? product.region ?? "Đang cập nhật"}
                  </span>
                </div>
                <div className="flex justify-between">
                  <span className="text-muted-foreground">Địa chỉ:</span>
                  <span className="max-w-[250px] text-right font-semibold text-foreground">
                    {traceabilityQuery.data?.farm?.address ?? "Đang cập nhật"}
                  </span>
                </div>
              </div>
            </div>

            <div className="rounded-xl border border-border bg-card p-6 shadow-sm">
              <h3 className="mb-4 text-base font-bold text-primary">Thông tin Lô hàng</h3>
              <div className="space-y-3 text-sm">
                <div className="flex justify-between">
                  <span className="text-muted-foreground">Mã lô:</span>
                  <span className="font-semibold text-foreground">
                    {traceabilityQuery.data?.lot?.lotCode ?? product.traceabilityCode ?? "Đang cập nhật"}
                  </span>
                </div>
                <div className="flex justify-between">
                  <span className="text-muted-foreground">Mùa vụ:</span>
                  <span className="font-semibold text-foreground">
                    {traceabilityQuery.data?.season?.name ?? product.seasonName ?? "Đang cập nhật"}
                  </span>
                </div>
                <div className="flex justify-between">
                  <span className="text-muted-foreground">Ngày thu hoạch:</span>
                  <span className="flex items-center gap-1 font-semibold text-foreground">
                    <Calendar size={14} className="text-primary" />
                    {traceabilityQuery.data?.lot?.harvestedAt ?? "Đang cập nhật"}
                  </span>
                </div>
              </div>
            </div>
          </div>
        </div>
      ) : null}

      {product.description && (
        <div className="mb-6">
          <div className="rounded-xl border border-border bg-card p-6 shadow-sm">
            <h2 className="mb-4 text-lg font-bold text-foreground">Mô tả sản phẩm</h2>
            <div className="text-sm leading-relaxed text-foreground">
              <p>{product.description}</p>
              <p className="mt-4 text-xs text-muted-foreground/60">Cập nhật lần cuối: {formatDateTime(product.updatedAt)}</p>
            </div>
          </div>
        </div>
      )}

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        <div className="rounded-xl border border-border bg-card p-6 shadow-sm">
          <h2 className="mb-4 text-base font-bold text-foreground">Thông tin bổ sung</h2>
          <div className="grid grid-cols-2 gap-3">
            <div className="rounded-xl border border-border bg-muted/50 p-3">
              <p className="text-xs text-muted-foreground mb-1">Người bán</p>
              <p className="text-sm font-semibold text-foreground truncate">{product.farmerDisplayName}</p>
            </div>
            <div className="rounded-xl border border-border bg-muted/50 p-3">
              <p className="text-xs text-muted-foreground mb-1">Tồn kho</p>
              <p className="text-sm font-semibold text-foreground">
                {product.availableQuantity} {product.unit}
              </p>
            </div>
            {product.region && (
              <div className="col-span-2 rounded-xl border border-border bg-muted/50 p-3">
                <p className="text-xs text-muted-foreground mb-1">Khu vực</p>
                <p className="text-sm font-semibold text-foreground flex items-center gap-1">
                  <MapPin size={14} className="text-primary" /> {product.region}
                </p>
              </div>
            )}
          </div>
        </div>

        <div className="rounded-xl border border-border bg-card p-6 shadow-sm">
          <h2 className="mb-4 text-base font-bold text-foreground">Đánh giá gần đây</h2>
          {reviewsQuery.isLoading ? (
            <p className="text-sm text-muted-foreground">Đang tải đánh giá...</p>
          ) : reviewsQuery.isError ? (
            <p className="text-sm text-destructive">Không thể tải đánh giá.</p>
          ) : reviewsQuery.data && reviewsQuery.data.items.length > 0 ? (
            <div className="space-y-3">
              {reviewsQuery.data.items.map((review) => (
                <div key={review.id} className="rounded-xl border border-border bg-muted/50 p-3">
                  <div className="mb-1 flex items-center justify-between gap-2">
                    <p className="text-sm font-semibold text-foreground">{review.buyerDisplayName}</p>
                    <StarRating rating={(review as { rating?: number }).rating} />
                  </div>
                  <p className="text-xs text-muted-foreground/60 mb-1">{formatDateTime(review.createdAt)}</p>
                  <p className="text-sm text-muted-foreground">{review.comment}</p>
                </div>
              ))}
            </div>
          ) : (
            <p className="text-sm text-muted-foreground">Chưa có đánh giá nào cho sản phẩm này.</p>
          )}
        </div>
      </div>
    </div>
    </div>
  );
}
