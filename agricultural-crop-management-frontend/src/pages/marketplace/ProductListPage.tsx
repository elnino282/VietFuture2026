import { useEffect, useMemo, useState, type FormEvent, type ReactNode } from "react";
import { Camera, ImageOff, PackageOpen, Search, SlidersHorizontal, X } from "lucide-react";
import { Link, useSearchParams } from "react-router-dom";
import { useAuth } from "@/features/auth";
import { ImageSearchModal } from "@/features/marketplace/image-search";
import { cn } from "@/shared/lib";
import type { MarketplaceImageSearchFilters } from "@/shared/api";
import {
  Badge,
  Button,
  Input,
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
} from "@/shared/ui";
import {
  useMarketplaceAddToCart,
  useMarketplaceCategories,
  useMarketplaceProducts,
} from "@/features/marketplace/hooks";
import { getCategoryLabel } from "@/features/marketplace/lib/categoryLabels";
import { formatVnd } from "@/features/marketplace/lib/format";
import "./ProductListPage.css";

function toPositiveInt(value: string | null, fallback: number) {
  const parsed = Number(value);
  return Number.isFinite(parsed) && parsed > 0 ? Math.floor(parsed) : fallback;
}

const SORT_OPTIONS = [
  { value: "newest", label: "Mới nhất" },
  { value: "price_asc", label: "Giá thấp đến cao" },
  { value: "price_desc", label: "Giá cao đến thấp" },
] as const;

const PRICE_RANGES = [
  { value: "under_100", label: "Dưới 100.000đ", maxPrice: 100000 },
  {
    value: "100_180",
    label: "100.000đ - 180.000đ",
    minPrice: 100000,
    maxPrice: 180000,
  },
  {
    value: "180_250",
    label: "180.000đ - 250.000đ",
    minPrice: 180000,
    maxPrice: 250000,
  },
  { value: "over_250", label: "Trên 250.000đ", minPrice: 250000 },
] as const;

// TODO: Add a rating filter when MarketplaceProductQuery supports rating/minRating.

type SortValue = (typeof SORT_OPTIONS)[number]["value"];
type PriceRangeValue = "" | (typeof PRICE_RANGES)[number]["value"];

type FilterState = {
  q: string;
  farmId: number | null;
  category: string;
  region: string;
  priceRange: PriceRangeValue;
  traceable: boolean;
  sort: SortValue;
};

type FilterChipKey = keyof FilterState;

const DEFAULT_FILTERS: FilterState = {
  q: "",
  farmId: null,
  category: "",
  region: "",
  priceRange: "",
  traceable: false,
  sort: "newest",
};

function isSortValue(value: string | null): value is SortValue {
  return SORT_OPTIONS.some((option) => option.value === value);
}

function getPriceRange(value: PriceRangeValue) {
  return PRICE_RANGES.find((range) => range.value === value);
}

function getPriceBounds(value: PriceRangeValue) {
  const range = getPriceRange(value);

  return {
    minPrice: range && "minPrice" in range ? range.minPrice : undefined,
    maxPrice: range && "maxPrice" in range ? range.maxPrice : undefined,
  };
}

function priceRangeFromParams(searchParams: URLSearchParams): PriceRangeValue {
  const minPrice = searchParams.get("minPrice");
  const maxPrice = searchParams.get("maxPrice");

  if (!minPrice && maxPrice === "100000") return "under_100";
  if (minPrice === "100000" && maxPrice === "180000") return "100_180";
  if (minPrice === "180000" && maxPrice === "250000") return "180_250";
  if (minPrice === "250000" && !maxPrice) return "over_250";

  return "";
}

function parseFilters(searchParams: URLSearchParams): FilterState {
  const sortParam = searchParams.get("sort");
  const farmId = toPositiveInt(searchParams.get("farmId"), 0);

  return {
    q: searchParams.get("q") ?? "",
    farmId: farmId > 0 ? farmId : null,
    category: searchParams.get("category") ?? "",
    region: searchParams.get("region") ?? "",
    priceRange: priceRangeFromParams(searchParams),
    traceable: searchParams.get("traceable") === "true",
    sort: isSortValue(sortParam) ? sortParam : "newest",
  };
}

function buildSearchParams(filters: FilterState, page = "1") {
  const params = new URLSearchParams();
  const q = filters.q.trim();
  const region = filters.region.trim();
  const priceBounds = getPriceBounds(filters.priceRange);

  if (q) params.set("q", q);
  if (filters.farmId) params.set("farmId", String(filters.farmId));
  if (filters.category) params.set("category", filters.category);
  if (region) params.set("region", region);
  if (filters.traceable) params.set("traceable", "true");
  if (filters.sort !== "newest") params.set("sort", filters.sort);
  if (priceBounds.minPrice) params.set("minPrice", String(priceBounds.minPrice));
  if (priceBounds.maxPrice) params.set("maxPrice", String(priceBounds.maxPrice));

  params.set("page", page);
  return params;
}

function hasFilters(filters: FilterState) {
  return Boolean(
    filters.q.trim() ||
      filters.farmId ||
      filters.category ||
      filters.region.trim() ||
      filters.priceRange ||
      filters.traceable ||
      filters.sort !== "newest",
  );
}

function countByValue(values: Array<string | null | undefined>) {
  return values.reduce<Map<string, number>>((counts, rawValue) => {
    const value = rawValue?.trim();
    if (!value) return counts;
    counts.set(value, (counts.get(value) ?? 0) + 1);
    return counts;
  }, new Map<string, number>());
}

function activeFilterChips(filters: FilterState) {
  const chips: Array<{ key: FilterChipKey; label: string }> = [];
  const priceRange = getPriceRange(filters.priceRange);
  const sortLabel = SORT_OPTIONS.find((option) => option.value === filters.sort)?.label;

  if (filters.q.trim()) chips.push({ key: "q", label: `Từ khóa: ${filters.q.trim()}` });
  if (filters.farmId) chips.push({ key: "farmId", label: `Nông trại #${filters.farmId}` });
  if (filters.category) {
    chips.push({ key: "category", label: `Danh mục: ${getCategoryLabel(filters.category)}` });
  }
  if (filters.region.trim()) chips.push({ key: "region", label: `Khu vực: ${filters.region.trim()}` });
  if (priceRange) chips.push({ key: "priceRange", label: `Khoảng giá: ${priceRange.label}` });
  if (filters.traceable) chips.push({ key: "traceable", label: "Có truy xuất" });
  if (filters.sort !== "newest" && sortLabel) chips.push({ key: "sort", label: sortLabel });

  return chips;
}

function ProductCardSkeleton() {
  return (
    <div className="marketplace-product-card">
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
    <span className="inline-flex max-w-full items-center gap-1.5 rounded-full border border-emerald-700/20 bg-emerald-50 px-3 py-1.5 text-xs font-semibold text-emerald-950">
      <span className="truncate">{label}</span>
      <button
        type="button"
        onClick={onRemove}
        className="shrink-0 rounded-full p-0.5 text-emerald-900 transition hover:bg-emerald-100"
        aria-label={`Xóa lọc: ${label}`}
      >
        <X size={12} />
      </button>
    </span>
  );
}

type ProductFilterPanelProps = {
  draft: FilterState;
  onDraftChange: (patch: Partial<FilterState>) => void;
  onApply: () => void;
  onClear: () => void;
  categories: string[];
  categoryCounts: Map<string, number>;
  categoriesLoading: boolean;
  categoriesError: boolean;
  regions: string[];
  regionCounts: Map<string, number>;
  regionsLoading: boolean;
  compact?: boolean;
};

function ProductFilterPanel({
  draft,
  onDraftChange,
  onApply,
  onClear,
  categories,
  categoryCounts,
  categoriesLoading,
  categoriesError,
  regions,
  regionCounts,
  regionsLoading,
  compact = false,
}: ProductFilterPanelProps) {
  const panelId = compact ? "mobile" : "desktop";

  return (
    <div
      className={cn(
        "marketplace-filter-panel",
        compact ? "space-y-5" : "marketplace-filter-panel--desktop",
      )}
    >
      {!compact ? (
        <div className="shrink-0 border-b border-border px-5 py-4">
          <div className="flex items-center gap-2">
            <SlidersHorizontal size={17} className="text-muted-foreground" aria-hidden="true" />
            <h2 className="text-lg font-semibold text-foreground">Bộ lọc sản phẩm</h2>
          </div>
          <p className="mt-1 text-xs text-muted-foreground">Tìm nông sản phù hợp</p>
        </div>
      ) : null}

      <div className={cn("space-y-5", compact ? "" : "marketplace-filter-panel__body px-5 py-5")}>
        <FilterSection title="Danh mục">
          {categoriesLoading ? (
            <FilterSkeletonRows />
          ) : categoriesError ? (
            <p className="rounded-lg bg-destructive/10 px-3 py-2 text-xs leading-5 text-destructive">
              Không thể tải danh mục từ sản phẩm công khai.
            </p>
          ) : (
            <div className="marketplace-filter-options space-y-1">
              <RadioFilterOption
                checked={!draft.category}
                label="Tất cả"
                name={`${panelId}-category`}
                onChange={() => onDraftChange({ category: "" })}
              />
              {categories.map((category) => (
                <RadioFilterOption
                  key={category}
                  checked={draft.category === category}
                  count={categoryCounts.get(category)}
                  label={getCategoryLabel(category)}
                  name={`${panelId}-category`}
                  onChange={() => onDraftChange({ category })}
                />
              ))}
              {categories.length === 0 ? (
                <p className="px-2 py-1 text-xs leading-5 text-muted-foreground">
                  Chưa có danh mục từ sản phẩm công khai.
                </p>
              ) : null}
            </div>
          )}
        </FilterSection>

        <FilterSection title="Khu vực">
          {regionsLoading ? (
            <FilterSkeletonRows />
          ) : (
            <div className="marketplace-filter-options space-y-1">
              <RadioFilterOption
                checked={!draft.region}
                label="Tất cả khu vực"
                name={`${panelId}-region`}
                onChange={() => onDraftChange({ region: "" })}
              />
              {regions.map((region) => (
                <RadioFilterOption
                  key={region}
                  checked={draft.region === region}
                  count={regionCounts.get(region)}
                  label={region}
                  name={`${panelId}-region`}
                  onChange={() => onDraftChange({ region })}
                />
              ))}
              {regions.length === 0 ? (
                <p className="px-2 py-1 text-xs leading-5 text-muted-foreground">
                  Chưa có khu vực từ dữ liệu sản phẩm.
                </p>
              ) : null}
            </div>
          )}
        </FilterSection>

        <FilterSection title="Khoảng giá">
          <div className="space-y-1">
            <RadioFilterOption
              checked={!draft.priceRange}
              label="Tất cả mức giá"
              name={`${panelId}-priceRange`}
              onChange={() => onDraftChange({ priceRange: "" })}
            />
            {PRICE_RANGES.map((range) => (
              <RadioFilterOption
                key={range.value}
                checked={draft.priceRange === range.value}
                label={range.label}
                name={`${panelId}-priceRange`}
                onChange={() => onDraftChange({ priceRange: range.value })}
              />
            ))}
          </div>
        </FilterSection>

        <FilterSection title="Tiêu chuẩn / truy xuất">
          <label
            className={cn(
              "flex cursor-pointer items-start gap-3 rounded-xl border px-3 py-3 text-sm transition",
              draft.traceable
                ? "border-emerald-500 bg-emerald-50"
                : "border-border hover:border-emerald-200 hover:bg-emerald-50/40",
            )}
          >
            <input
              type="checkbox"
              className="mt-0.5 h-4 w-4 shrink-0 rounded border-border accent-emerald-600"
              checked={draft.traceable}
              onChange={(event) => onDraftChange({ traceable: event.target.checked })}
            />
            <span className="min-w-0">
              <span className="block font-medium text-foreground">Có truy xuất nguồn gốc</span>
              <span className="mt-1 block text-xs leading-5 text-muted-foreground">
                Ưu tiên sản phẩm có mùa vụ và lô thu hoạch rõ ràng.
              </span>
            </span>
          </label>
        </FilterSection>
      </div>

      <div
        className={cn(
          "grid grid-cols-2 gap-3",
          compact ? "pt-1" : "marketplace-filter-panel__footer border-t border-border bg-card p-5",
        )}
      >
        <Button type="button" variant="outline" onClick={onClear}>
          Xóa lọc
        </Button>
        <Button type="button" onClick={onApply} className="bg-emerald-600 hover:bg-emerald-700">
          Áp dụng
        </Button>
      </div>
    </div>
  );
}

function FilterSection({
  title,
  children,
}: {
  title: string;
  children: ReactNode;
}) {
  return (
    <section className="border-t border-border/70 pt-4 first:border-t-0 first:pt-0">
      <h3 className="mb-3 text-sm font-semibold text-foreground">{title}</h3>
      {children}
    </section>
  );
}

function FilterSkeletonRows() {
  return (
    <div className="space-y-2">
      {Array.from({ length: 4 }, (_, index) => (
        <div key={index} className="h-6 animate-pulse rounded-lg bg-muted" />
      ))}
    </div>
  );
}

function RadioFilterOption({
  checked,
  count,
  label,
  name,
  onChange,
}: {
  checked: boolean;
  count?: number;
  label: string;
  name: string;
  onChange: () => void;
}) {
  return (
    <label
      className={cn(
        "flex cursor-pointer items-center justify-between gap-3 rounded-lg px-2 py-2 text-sm transition",
        checked ? "bg-emerald-50 text-emerald-950" : "text-slate-700 hover:bg-muted",
      )}
    >
      <span className="flex min-w-0 items-center gap-2">
        <input
          type="radio"
          name={name}
          checked={checked}
          onChange={onChange}
          className="h-4 w-4 shrink-0 border-border accent-emerald-600"
        />
        <span className="truncate font-medium">{label}</span>
      </span>
      {typeof count === "number" ? (
        <span className="shrink-0 text-xs font-medium text-muted-foreground">{count}</span>
      ) : null}
    </label>
  );
}

import { MarketplaceProductCard } from "@/features/marketplace/components/MarketplaceProductCard";

export function ProductListPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const searchParamString = searchParams.toString();
  const appliedFilters = useMemo(
    () => parseFilters(new URLSearchParams(searchParamString)),
    [searchParamString],
  );
  const { isAuthenticated } = useAuth();
  const { addToCart, isAdding } = useMarketplaceAddToCart();
  const [mobileFilterOpen, setMobileFilterOpen] = useState(false);
  const [imageSearchOpen, setImageSearchOpen] = useState(false);
  const [draftFilters, setDraftFilters] = useState<FilterState>(appliedFilters);

  useEffect(() => {
    setDraftFilters(appliedFilters);
  }, [appliedFilters]);

  const page = toPositiveInt(searchParams.get("page"), 1);
  const appliedPriceBounds = getPriceBounds(appliedFilters.priceRange);
  const imageSearchFilters = useMemo<MarketplaceImageSearchFilters>(
    () => ({
      region: appliedFilters.region.trim() || undefined,
      minPrice: appliedPriceBounds.minPrice,
      maxPrice: appliedPriceBounds.maxPrice,
      traceable: appliedFilters.traceable || undefined,
      sort: appliedFilters.sort,
      page: 0,
      size: 8,
    }),
    [
      appliedFilters.region,
      appliedFilters.traceable,
      appliedFilters.sort,
      appliedPriceBounds.minPrice,
      appliedPriceBounds.maxPrice,
    ],
  );

  const productsQuery = useMarketplaceProducts({
    q: appliedFilters.q.trim() || undefined,
    farmId: appliedFilters.farmId ?? undefined,
    category: appliedFilters.category || undefined,
    region: appliedFilters.region.trim() || undefined,
    minPrice: appliedPriceBounds.minPrice,
    maxPrice: appliedPriceBounds.maxPrice,
    traceable: appliedFilters.traceable || undefined,
    sort: appliedFilters.sort,
    page: page - 1,
    size: 18,
  });

  const categoriesQuery = useMarketplaceCategories(true);
  const filterOptionsQuery = useMarketplaceProducts({ page: 0, size: 100 });

  const products = productsQuery.data?.items ?? [];
  const optionProducts = filterOptionsQuery.data?.items ?? [];
  const categories = categoriesQuery.data ?? [];
  const categoryCounts = useMemo(
    () => countByValue(optionProducts.map((product) => product.category)),
    [optionProducts],
  );
  const regions = useMemo(
    () =>
      Array.from(
        new Set(
          optionProducts
            .map((product) => product.region?.trim())
            .filter((region): region is string => Boolean(region)),
        ),
      ).sort((left, right) => left.localeCompare(right)),
    [optionProducts],
  );
  const regionCounts = useMemo(
    () => countByValue(optionProducts.map((product) => product.region)),
    [optionProducts],
  );
  const totalPages = Math.max(productsQuery.data?.totalPages ?? 1, 1);
  const hasActiveFilters = hasFilters(appliedFilters);
  const activeChips = activeFilterChips(appliedFilters);

  function updateDraft(patch: Partial<FilterState>) {
    setDraftFilters((current) => ({ ...current, ...patch }));
  }

  function applyFilters(filters = draftFilters) {
    const nextFilters = {
      ...filters,
      q: filters.q.trim(),
      region: filters.region.trim(),
    };
    setDraftFilters(nextFilters);
    setSearchParams(buildSearchParams(nextFilters, "1"));
    setMobileFilterOpen(false);
  }

  function clearFilters() {
    setDraftFilters(DEFAULT_FILTERS);
    setSearchParams(buildSearchParams(DEFAULT_FILTERS, "1"));
    setMobileFilterOpen(false);
  }

  function removeFilter(key: FilterChipKey) {
    const nextFilters: FilterState = {
      ...appliedFilters,
      [key]: DEFAULT_FILTERS[key],
    };
    setDraftFilters(nextFilters);
    setSearchParams(buildSearchParams(nextFilters, "1"));
  }

  function setPage(nextPage: number) {
    const next = new URLSearchParams(searchParams);
    next.set("page", String(nextPage));
    setSearchParams(next);
  }

  function handleSearchSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    applyFilters(draftFilters);
  }

  function handleImageKeywordSearch(keyword: string) {
    const nextFilters = {
      ...appliedFilters,
      q: keyword.trim(),
    };
    setDraftFilters(nextFilters);
    setSearchParams(buildSearchParams(nextFilters, "1"));
  }

  function openMobileFilter() {
    setDraftFilters(appliedFilters);
    setMobileFilterOpen(true);
  }

  async function handleAddToCart(productId: number) {
    await addToCart(productId, 1);
  }

  const filterPanelProps = {
    draft: draftFilters,
    onDraftChange: updateDraft,
    onApply: () => applyFilters(),
    onClear: clearFilters,
    categories,
    categoryCounts,
    categoriesLoading: categoriesQuery.isLoading,
    categoriesError: categoriesQuery.isError,
    regions,
    regionCounts,
    regionsLoading: filterOptionsQuery.isLoading,
  };

  return (
    <div className="marketplace-products-page">
      <div className="marketplace-products-layout">
        <aside className="marketplace-products-sidebar" aria-label="Bộ lọc sản phẩm">
          <ProductFilterPanel {...filterPanelProps} />
        </aside>

        <main className="marketplace-products-content">
          <div className="marketplace-products-heading">
            <div className="min-w-0">
              <h1 className="text-2xl font-bold text-foreground">Sản phẩm nông sản</h1>
              <p className="mt-1 text-sm text-muted-foreground">
                Danh sách sản phẩm đang được công khai trên marketplace
              </p>
            </div>
            <Button
              variant="outline"
              size="sm"
              className="marketplace-products-mobile-filter-button"
              onClick={openMobileFilter}
            >
              <SlidersHorizontal size={14} />
              Bộ lọc
              {hasActiveFilters ? (
                <span className="ml-1 inline-flex h-4 min-w-4 items-center justify-center rounded-full bg-emerald-600 px-1 text-[10px] font-bold text-white">
                  {activeChips.length}
                </span>
              ) : null}
            </Button>
          </div>

          <section className="marketplace-products-controls rounded-2xl border border-border bg-card p-4 shadow-sm">
            <div className="marketplace-products-control-row">
              <form onSubmit={handleSearchSubmit} className="marketplace-products-search-form">
                <div className="relative min-w-0">
                  <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
                  <Input
                    value={draftFilters.q}
                    onChange={(event) => updateDraft({ q: event.target.value })}
                    placeholder="Tìm kiếm sản phẩm, nông trại..."
                    className="h-11 rounded-xl border-border pl-10 focus:border-primary"
                  />
                </div>
                <Button
                  type="button"
                  variant="outline"
                  className="marketplace-products-image-search-button h-11"
                  onClick={() => setImageSearchOpen(true)}
                  aria-label="Tìm bằng ảnh"
                  title="Tìm bằng ảnh"
                >
                  <Camera size={17} aria-hidden="true" />
                </Button>
                <Button
                  type="submit"
                  className="marketplace-products-search-submit h-11 bg-emerald-600 px-5 hover:bg-emerald-700"
                >
                  Tìm kiếm
                </Button>
              </form>

              <label className="marketplace-products-sort text-sm text-muted-foreground">
                <span className="shrink-0">Sắp xếp:</span>
                <select
                  value={draftFilters.sort}
                  onChange={(event) => updateDraft({ sort: event.target.value as SortValue })}
                  className="h-11 rounded-xl border border-border bg-card px-3 text-sm font-medium text-foreground outline-none transition focus:border-emerald-500 focus:ring-2 focus:ring-emerald-500/20"
                >
                  {SORT_OPTIONS.map((option) => (
                    <option key={option.value} value={option.value}>
                      {option.label}
                    </option>
                  ))}
                </select>
              </label>
            </div>

            {hasActiveFilters ? (
              <div className="mt-3 flex flex-wrap items-center gap-2">
                {activeChips.map((chip) => (
                  <FilterBadge
                    key={chip.key}
                    label={chip.label}
                    onRemove={() => removeFilter(chip.key)}
                  />
                ))}
                <button
                  type="button"
                  onClick={clearFilters}
                  className="px-1 text-xs font-medium text-destructive transition hover:underline"
                >
                  Xóa tất cả
                </button>
              </div>
            ) : null}
          </section>

          {productsQuery.isLoading ? (
            <div className="marketplace-products-grid">
              {Array.from({ length: 8 }, (_, index) => (
                <ProductCardSkeleton key={index} />
              ))}
            </div>
          ) : productsQuery.isError ? (
            <div className="rounded-xl border border-dashed border-destructive/30 bg-card p-8 text-center text-sm text-destructive">
              Không thể tải danh sách sản phẩm.
            </div>
          ) : products.length > 0 ? (
            <>
              <div className="marketplace-products-grid">
                {products.map((product) => (
                  <MarketplaceProductCard
                    key={product.id}
                    product={product}
                    isAuthenticated={isAuthenticated}
                    isAdding={isAdding}
                    onAddToCart={handleAddToCart}
                  />
                ))}
              </div>

              <div className="marketplace-products-pagination flex flex-col gap-3 rounded-xl border border-border bg-card px-4 py-3 sm:flex-row sm:items-center sm:justify-between">
                <p className="text-sm text-muted-foreground">
                  Trang {page} / {totalPages}
                </p>
                <div className="flex gap-2">
                  <Button
                    variant="outline"
                    size="sm"
                    disabled={page <= 1}
                    onClick={() => setPage(page - 1)}
                  >
                    Trước
                  </Button>
                  <Button
                    variant="outline"
                    size="sm"
                    disabled={page >= totalPages}
                    onClick={() => setPage(page + 1)}
                  >
                    Sau
                  </Button>
                </div>
              </div>
            </>
          ) : (
            <div className="rounded-xl border border-border bg-card py-16 text-center">
              <PackageOpen className="mx-auto mb-3 text-muted-foreground/40" size={48} />
              <p className="text-base font-semibold text-foreground">
                Không tìm thấy sản phẩm phù hợp
              </p>
              <p className="mt-1 text-sm text-muted-foreground">
                Hãy thử thay đổi bộ lọc hoặc từ khóa tìm kiếm.
              </p>
              {hasActiveFilters ? (
                <button
                  type="button"
                  onClick={clearFilters}
                  className="mt-4 text-sm font-medium text-primary hover:underline"
                >
                  Xóa toàn bộ bộ lọc
                </button>
              ) : null}
            </div>
          )}
        </main>
      </div>

      <ImageSearchModal
        open={imageSearchOpen}
        onOpenChange={setImageSearchOpen}
        filters={imageSearchFilters}
        onKeywordSearch={handleImageKeywordSearch}
      />

      <Sheet open={mobileFilterOpen} onOpenChange={setMobileFilterOpen}>
        <SheetContent side="bottom" className="max-h-[90dvh] overflow-y-auto rounded-t-2xl">
          <SheetHeader className="pb-2">
            <SheetTitle>Bộ lọc sản phẩm</SheetTitle>
          </SheetHeader>
          <div className="px-4 pb-6">
            <ProductFilterPanel {...filterPanelProps} compact />
          </div>
        </SheetContent>
      </Sheet>
    </div>
  );
}
