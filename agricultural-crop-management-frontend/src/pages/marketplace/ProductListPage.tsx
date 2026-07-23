import { useEffect, useMemo, useState, type FormEvent, type ReactNode } from "react";
import { Camera, ImageOff, PackageOpen, Search, X, ChevronDown, ShieldCheck } from "lucide-react";
import { Link, useSearchParams } from "react-router-dom";
import { useAuth } from "@/features/auth";
import { ImageSearchModal } from "@/features/marketplace/image-search";
import { cn } from "@/shared/lib";
import type { MarketplaceImageSearchFilters } from "@/shared/api";
import {
  Badge,
  Button,
  Input,
} from "@/shared/ui";
import {
  useMarketplaceAddToCart,
  useMarketplaceCategories,
  useMarketplaceProducts,
} from "@/features/marketplace/hooks";
import { getCategoryLabel } from "@/features/marketplace/lib/categoryLabels";
import { formatVnd } from "@/features/marketplace/lib/format";
import "./FarmsDiscoveryPage.css";
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
    <div className="farms-discovery-page">
      <section className="farms-discovery-hero">
        <div className="farms-discovery-hero__content">
          <h1>Khám phá sản phẩm</h1>
          <p>
            Tìm kiếm nông sản sạch, an toàn, có chứng nhận và minh bạch nguồn gốc.
          </p>
          <form onSubmit={handleSearchSubmit} className="farms-discovery-hero__search relative">
            <Search aria-hidden="true" />
            <input
              type="search"
              value={draftFilters.q}
              onChange={(event) => updateDraft({ q: event.target.value })}
              placeholder="Tìm kiếm sản phẩm, nông trại..."
            />
            <Button
              type="button"
              variant="ghost"
              className="absolute right-2 top-1/2 -translate-y-1/2 rounded-full p-2 text-emerald-700 hover:bg-emerald-50"
              onClick={() => setImageSearchOpen(true)}
              aria-label="Tìm bằng ảnh"
              title="Tìm bằng ảnh"
            >
              <Camera size={20} aria-hidden="true" />
            </Button>
          </form>
        </div>
      </section>

      <main className="farms-discovery-content">
        <section className="farms-discovery-toolbar" aria-label="Bộ lọc sản phẩm">
          <label className="farms-discovery-field">
            <span>Khu vực</span>
            <select
              value={draftFilters.region}
              onChange={(event) => updateDraft({ region: event.target.value })}
              aria-label="Khu vực"
            >
              <option value="">Tất cả khu vực</option>
              {regions.map((option) => (
                <option key={option} value={option}>
                  {option}
                </option>
              ))}
            </select>
            <ChevronDown aria-hidden="true" />
          </label>

          <label className="farms-discovery-field">
            <span>Khoảng giá</span>
            <select
              value={draftFilters.priceRange}
              onChange={(event) => updateDraft({ priceRange: event.target.value as PriceRangeValue })}
              aria-label="Khoảng giá"
            >
              <option value="">Tất cả mức giá</option>
              <option value="under_100">Dưới 100.000đ</option>
              <option value="100_180">100.000đ - 180.000đ</option>
              <option value="180_250">180.000đ - 250.000đ</option>
              <option value="over_250">Trên 250.000đ</option>
            </select>
            <ChevronDown aria-hidden="true" />
          </label>

          <button
            type="button"
            className={`farms-discovery-toggle${draftFilters.traceable ? " is-active" : ""}`}
            aria-pressed={draftFilters.traceable}
            onClick={() => updateDraft({ traceable: !draftFilters.traceable })}
          >
            <ShieldCheck aria-hidden="true" />
            Có truy xuất
          </button>

          <button
            type="button"
            className={`farms-discovery-toggle px-3 py-1.5 text-sm font-semibold rounded-full border border-emerald-600 bg-emerald-600 text-white shadow-sm hover:bg-emerald-700 transition`}
            onClick={() => applyFilters()}
          >
            Lọc
          </button>

          <label className="farms-discovery-field farms-discovery-field--sort">
            <span>Sắp xếp</span>
            <select
              value={draftFilters.sort}
              onChange={(event) => {
                updateDraft({ sort: event.target.value as SortValue });
                applyFilters({ ...draftFilters, sort: event.target.value as SortValue });
              }}
            >
              {SORT_OPTIONS.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
            <ChevronDown aria-hidden="true" />
          </label>
        </section>

        <section className="farms-discovery-category-row" aria-label="Danh mục nông sản">
          <div className="farms-discovery-category-row__title">
            <PackageOpen aria-hidden="true" />
            <span>Danh mục sản phẩm</span>
          </div>
          <div className="farms-discovery-chips">
            <button
              type="button"
              className={!draftFilters.category ? "is-active" : ""}
              onClick={() => {
                updateDraft({ category: "" });
                applyFilters({ ...draftFilters, category: "" });
              }}
            >
              Tất cả
            </button>
            {categories.map((cat) => (
              <button
                key={cat}
                type="button"
                className={draftFilters.category === cat ? "is-active" : ""}
                onClick={() => {
                  updateDraft({ category: cat });
                  applyFilters({ ...draftFilters, category: cat });
                }}
              >
                {getCategoryLabel(cat)}
              </button>
            ))}
          </div>
        </section>

        {hasActiveFilters ? (
          <div className="mb-4 flex flex-wrap items-center gap-2">
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

            <div className="mt-8 flex flex-col gap-3 rounded-2xl border border-border bg-white px-5 py-4 shadow-sm sm:flex-row sm:items-center sm:justify-between">
              <p className="text-sm font-medium text-emerald-800">
                Trang {page} / {totalPages}
              </p>
              <div className="flex gap-2">
                <Button
                  variant="outline"
                  size="sm"
                  className="rounded-xl border-emerald-200 bg-white font-medium text-emerald-700 hover:bg-emerald-50"
                  disabled={page <= 1}
                  onClick={() => setPage(page - 1)}
                >
                  Trước
                </Button>
                <Button
                  variant="outline"
                  size="sm"
                  className="rounded-xl border-emerald-200 bg-white font-medium text-emerald-700 hover:bg-emerald-50"
                  disabled={page >= totalPages}
                  onClick={() => setPage(page + 1)}
                >
                  Sau
                </Button>
              </div>
            </div>
          </>
        ) : (
          <div className="rounded-3xl border border-emerald-100 bg-white py-16 text-center shadow-sm">
            <PackageOpen className="mx-auto mb-4 text-emerald-200" size={48} />
            <p className="text-lg font-bold text-emerald-950">
              Không tìm thấy sản phẩm phù hợp
            </p>
            <p className="mt-1 text-sm text-emerald-700/70">
              Hãy thử thay đổi bộ lọc hoặc từ khóa tìm kiếm.
            </p>
            {hasActiveFilters ? (
              <button
                type="button"
                onClick={clearFilters}
                className="mt-6 text-sm font-semibold text-emerald-600 hover:underline"
              >
                Xóa toàn bộ bộ lọc
              </button>
            ) : null}
          </div>
        )}
      </main>

      <ImageSearchModal
        open={imageSearchOpen}
        onOpenChange={setImageSearchOpen}
        filters={imageSearchFilters}
        onKeywordSearch={handleImageKeywordSearch}
      />
    </div>
  );
}
