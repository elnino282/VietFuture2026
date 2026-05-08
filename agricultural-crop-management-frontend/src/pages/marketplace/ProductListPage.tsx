import { useState } from "react";
import { PackageOpen, Search, SlidersHorizontal, X } from "lucide-react";
import { Link, useSearchParams } from "react-router-dom";
import { useAuth } from "@/features/auth";
import { cn } from "@/shared/lib";
import {
  Badge,
  Button,
  Input,
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
} from "@/shared/ui";
import { useMarketplaceAddToCart, useMarketplaceCategories, useMarketplaceProducts } from "@/features/marketplace/hooks";
import { getCategoryLabel } from "@/features/marketplace/lib/categoryLabels";
import { formatVnd } from "@/features/marketplace/lib/format";

function toPositiveInt(value: string | null, fallback: number) {
  const parsed = Number(value);
  return Number.isFinite(parsed) && parsed > 0 ? Math.floor(parsed) : fallback;
}

const SORT_LABELS: Record<string, string> = {
  newest: "Mới nhất",
  price_asc: "Giá thấp → cao",
  price_desc: "Giá cao → thấp",
};

function ProductCardSkeleton() {
  return (
    <div className="overflow-hidden rounded-xl border border-border bg-card shadow-sm">
      <div className="aspect-square animate-pulse bg-muted" />
      <div className="space-y-3 p-4">
        <div className="h-3 w-16 animate-pulse rounded bg-muted" />
        <div className="h-4 w-3/4 animate-pulse rounded bg-muted" />
        <div className="h-3 w-1/2 animate-pulse rounded bg-muted" />
        <div className="flex justify-between">
          <div className="h-4 w-24 animate-pulse rounded bg-muted" />
          <div className="h-4 w-16 animate-pulse rounded bg-muted" />
        </div>
      </div>
    </div>
  );
}

function FilterBadge({ label, onRemove }: { label: string; onRemove: () => void }) {
  return (
    <span className="inline-flex items-center gap-1 rounded-full bg-primary/10 px-2.5 py-1 text-xs font-medium text-primary ring-1 ring-primary/20">
      {label}
      <button type="button" onClick={onRemove} aria-label={`Xóa lọc: ${label}`}>
        <X size={11} />
      </button>
    </span>
  );
}

export function ProductListPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const { isAuthenticated } = useAuth();
  const { addToCart, isAdding } = useMarketplaceAddToCart();
  const [mobileFilterOpen, setMobileFilterOpen] = useState(false);

  const q = searchParams.get("q") ?? "";
  const category = searchParams.get("category") ?? "";
  const region = searchParams.get("region") ?? "";
  const traceable = searchParams.get("traceable") === "true";
  const sort = (searchParams.get("sort") as "newest" | "price_asc" | "price_desc" | null) ?? "newest";
  const page = toPositiveInt(searchParams.get("page"), 1);

  const productsQuery = useMarketplaceProducts({
    q: q || undefined,
    category: category || undefined,
    region: region || undefined,
    traceable: traceable || undefined,
    sort,
    page: page - 1,
    size: 18,
  });

  const categoriesQuery = useMarketplaceCategories(mobileFilterOpen);
  const mobileCategories = categoriesQuery.data ?? [];

  const products = productsQuery.data?.items ?? [];
  const totalPages = Math.max(productsQuery.data?.totalPages ?? 1, 1);

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

  const hasActiveFilters = q || category || region || traceable || sort !== "newest";

  // Mobile filter local state (applied via updateParams on submit)
  const [mobileCategory, setMobileCategory] = useState(category);
  const [mobileRegion, setMobileRegion] = useState(region);
  const [mobileTraceable, setMobileTraceable] = useState(traceable);
  const [mobileSort, setMobileSort] = useState(sort);

  function openMobileFilter() {
    setMobileCategory(category);
    setMobileRegion(region);
    setMobileTraceable(traceable);
    setMobileSort(sort);
    setMobileFilterOpen(true);
  }

  function applyMobileFilter() {
    const patch: Record<string, string | null> = {
      category: mobileCategory || null,
      region: mobileRegion.trim() || null,
      traceable: mobileTraceable ? "true" : null,
      sort: mobileSort !== "newest" ? mobileSort : null,
      page: "1",
    };
    const next = new URLSearchParams();
    if (q) next.set("q", q);
    Object.entries(patch).forEach(([key, value]) => {
      if (value) next.set(key, value);
    });
    setSearchParams(next);
    setMobileFilterOpen(false);
  }

  return (
    <div className="max-w-[1800px] mx-auto px-6 pt-6">
      {/* Title row */}
      <div className="mb-4 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold text-foreground">Sản phẩm nông sản</h1>
          <p className="mt-1 text-sm text-muted-foreground">Danh sách sản phẩm đang được công khai trên marketplace</p>
        </div>
        <div className="flex items-center gap-2">
          <Button
            variant="outline"
            size="sm"
            className="md:hidden"
            onClick={openMobileFilter}
          >
            <SlidersHorizontal size={14} />
            Bộ lọc
            {hasActiveFilters && (
              <span className="ml-1 inline-flex h-4 w-4 items-center justify-center rounded-full bg-emerald-600 text-[10px] font-bold text-white">
                !
              </span>
            )}
          </Button>
          <div className="relative w-full sm:w-72">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
            <Input
              value={q}
              onChange={(event) => updateParams({ q: event.target.value || null })}
              placeholder="Tìm kiếm sản phẩm..."
              className="pl-10 rounded-xl border-border focus:border-primary"
            />
          </div>
        </div>
      </div>

      {/* Active filter badges */}
      {hasActiveFilters && (
        <div className="mb-4 flex flex-wrap items-center gap-2">
          <span className="text-xs font-medium text-muted-foreground">Bộ lọc:</span>
          {category && (
            <FilterBadge
              label={getCategoryLabel(category)}
              onRemove={() => updateParams({ category: null })}
            />
          )}
          {region && (
            <FilterBadge
              label={`Khu vực: ${region}`}
              onRemove={() => updateParams({ region: null })}
            />
          )}
          {traceable && (
            <FilterBadge
              label="Có truy xuất"
              onRemove={() => updateParams({ traceable: null })}
            />
          )}
          {sort !== "newest" && (
            <FilterBadge
              label={SORT_LABELS[sort] ?? sort}
              onRemove={() => updateParams({ sort: null })}
            />
          )}
          <button
            type="button"
            onClick={() => setSearchParams(new URLSearchParams())}
            className="text-xs font-medium text-muted-foreground hover:text-foreground hover:underline"
          >
            Xóa tất cả
          </button>
        </div>
      )}

      {/* Product grid */}
      {productsQuery.isLoading ? (
        <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
          {Array.from({ length: 8 }, (_, index) => <ProductCardSkeleton key={index} />)}
        </div>
      ) : productsQuery.isError ? (
        <div className="rounded-xl border border-dashed border-destructive/30 bg-card p-8 text-center text-sm text-destructive">
          Không thể tải danh sách sản phẩm.
        </div>
      ) : products.length > 0 ? (
        <>
          <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
            {products.map((product) => (
              <div key={product.id} className="group overflow-hidden rounded-xl border border-border bg-card shadow-sm transition-shadow hover:shadow-md">
                <div className="relative aspect-square overflow-hidden bg-muted">
                  <img
                    src={product.imageUrl}
                    alt={product.name}
                    className="h-full w-full object-cover transition-transform duration-300 group-hover:scale-105"
                    referrerPolicy="no-referrer"
                  />
                  {product.traceable ? (
                    <Badge className="absolute left-2 top-2 bg-emerald-500 text-white">Có truy xuất</Badge>
                  ) : null}
                </div>
                <div className="p-4">
                  <div className="mb-1 text-xs text-muted-foreground">{getCategoryLabel(product.category)}</div>
                  <Link
                    to={`/marketplace/products/${product.slug}`}
                    className="mb-2 line-clamp-2 block h-10 font-semibold text-foreground transition-colors hover:text-primary"
                  >
                    {product.name}
                  </Link>
                  <p className="mb-4 text-sm text-muted-foreground">
                    {product.farmName ?? "Nông trại đang cập nhật"}
                    {product.region ? ` · ${product.region}` : ""}
                  </p>
                  <div className="flex items-center justify-between gap-3">
                    <span className="text-lg font-bold text-primary">
                      {formatVnd(product.price)}
                      <span className="ml-1 text-sm font-normal text-muted-foreground">/{product.unit}</span>
                    </span>
                    <span className="text-sm text-muted-foreground">Tồn: {product.availableQuantity}</span>
                  </div>
                  <div className="mt-4 flex gap-2">
                    <Link
                      to={`/marketplace/products/${product.slug}`}
                      className="inline-flex flex-1 items-center justify-center rounded-xl border border-border px-3 py-2 text-sm font-medium text-foreground transition-colors hover:bg-muted"
                    >
                      Xem chi tiết
                    </Link>
                    {isAuthenticated ? (
                      <Button
                        size="sm"
                        className="flex-1"
                        disabled={isAdding || product.availableQuantity <= 0}
                        onClick={async () => {
                          await addToCart(product.id, 1);
                        }}
                      >
                        Thêm giỏ
                      </Button>
                    ) : (
                      <Button asChild size="sm" variant="outline" className="flex-1">
                        <Link to="/sign-up">Tạo tài khoản</Link>
                      </Button>
                    )}
                  </div>
                </div>
              </div>
            ))}
          </div>

          <div className="mt-6 flex items-center justify-between rounded-xl border border-border bg-card px-4 py-3">
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
        </>
      ) : (
        <div className="rounded-xl border border-border bg-card py-16 text-center">
          <PackageOpen className="mx-auto mb-3 text-muted-foreground/40" size={48} />
          <p className="text-base font-semibold text-foreground">Không tìm thấy sản phẩm phù hợp</p>
          <p className="mt-1 text-sm text-muted-foreground">Hãy thử thay đổi bộ lọc hoặc từ khóa tìm kiếm.</p>
          {hasActiveFilters ? (
            <button
              type="button"
              onClick={() => setSearchParams(new URLSearchParams())}
              className="mt-4 text-sm font-medium text-primary hover:underline"
            >
              Xóa toàn bộ bộ lọc
            </button>
          ) : null}
        </div>
      )}

      {/* Mobile filter sheet */}
      <Sheet open={mobileFilterOpen} onOpenChange={setMobileFilterOpen}>
        <SheetContent side="bottom" className="max-h-[90dvh] overflow-y-auto rounded-t-2xl">
          <SheetHeader className="pb-2">
            <SheetTitle>Bộ lọc sản phẩm</SheetTitle>
          </SheetHeader>
          <div className="space-y-5 px-4 pb-2">
            {/* Category chips */}
            <div>
              <p className="mb-2 text-xs font-semibold uppercase tracking-wide text-muted-foreground">Danh mục</p>
              {categoriesQuery.isLoading ? (
                <div className="flex flex-wrap gap-2">
                  {Array.from({ length: 4 }, (_, i) => (
                    <div key={i} className="h-7 w-20 animate-pulse rounded-full bg-muted" />
                  ))}
                </div>
              ) : (
                <div className="flex flex-wrap gap-2">
                  <button
                    type="button"
                    onClick={() => setMobileCategory("")}
                    className={cn(
                      "rounded-full px-3 py-1 text-sm font-medium transition-colors",
                      !mobileCategory
                        ? "bg-emerald-50 text-primary ring-1 ring-emerald-300"
                        : "bg-muted text-foreground",
                    )}
                  >
                    Tất cả
                  </button>
                  {mobileCategories.map((cat) => (
                    <button
                      key={cat}
                      type="button"
                      onClick={() => setMobileCategory(mobileCategory === cat ? "" : cat)}
                      className={cn(
                        "rounded-full px-3 py-1 text-sm font-medium transition-colors",
                        mobileCategory === cat
                          ? "bg-emerald-50 text-primary ring-1 ring-emerald-300"
                          : "bg-muted text-foreground",
                      )}
                    >
                      {getCategoryLabel(cat)}
                    </button>
                  ))}
                </div>
              )}
            </div>

            {/* Region */}
            <div>
              <p className="mb-2 text-xs font-semibold uppercase tracking-wide text-muted-foreground">Khu vực</p>
              <input
                type="text"
                value={mobileRegion}
                onChange={(e) => setMobileRegion(e.target.value)}
                placeholder="Ví dụ: Lâm Đồng, An Giang..."
                className="h-9 w-full rounded-lg border border-border bg-card px-3 text-sm text-foreground outline-none transition focus:border-emerald-500 focus:ring-2 focus:ring-emerald-500/20"
              />
            </div>

            {/* Traceability */}
            <div>
              <label className={cn(
                "flex cursor-pointer items-center gap-3 rounded-xl border-2 px-4 py-3 text-sm transition-colors",
                mobileTraceable
                  ? "border-emerald-500 bg-emerald-50"
                  : "border-border hover:border-border",
              )}>
                <input
                  type="checkbox"
                  className="h-4 w-4 rounded border-border accent-emerald-600"
                  checked={mobileTraceable}
                  onChange={(e) => setMobileTraceable(e.target.checked)}
                />
                <div>
                  <p className={cn("font-medium", mobileTraceable ? "text-primary" : "text-foreground")}>
                    Chỉ sản phẩm có truy xuất
                  </p>
                  <p className="text-xs text-muted-foreground">Ưu tiên sản phẩm có mùa vụ và lô thu hoạch rõ ràng.</p>
                </div>
              </label>
            </div>

            {/* Sort */}
            <div>
              <p className="mb-2 text-xs font-semibold uppercase tracking-wide text-muted-foreground">Sắp xếp</p>
              <select
                value={mobileSort}
                onChange={(e) => setMobileSort(e.target.value as typeof mobileSort)}
                className="h-9 w-full rounded-lg border border-border bg-card px-3 text-sm text-foreground outline-none focus:border-emerald-500 focus:ring-2 focus:ring-emerald-500/20"
              >
                <option value="newest">Mới nhất</option>
                <option value="price_asc">Giá thấp → cao</option>
                <option value="price_desc">Giá cao → thấp</option>
              </select>
            </div>
          </div>

          {/* Sheet footer */}
          <div className="flex gap-3 border-t border-border/50 px-4 pt-4 pb-6">
            <Button
              variant="outline"
              className="flex-1"
              onClick={() => {
                setMobileCategory("");
                setMobileRegion("");
                setMobileTraceable(false);
                setMobileSort("newest");
              }}
            >
              Xóa lọc
            </Button>
            <Button className="flex-1" onClick={applyMobileFilter}>
              Áp dụng
            </Button>
          </div>
        </SheetContent>
      </Sheet>
    </div>
  );
}
