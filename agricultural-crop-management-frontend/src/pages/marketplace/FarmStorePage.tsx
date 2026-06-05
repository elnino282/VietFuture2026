import { useEffect, useMemo, useState } from "react";
import { Link, useParams } from "react-router-dom";
import {
  BadgeCheck,
  Calendar,
  CheckCircle,
  ChevronRight,
  Heart,
  ImageOff,
  MessageCircle,
  PackageOpen,
  Search,
  ShoppingCart,
  SlidersHorizontal,
  Star,
  Tractor,
  Truck,
  UserPlus,
  Users,
  Leaf,
  Shield,
  Headphones,
  BarChart3,
} from "lucide-react";
import { useAuth } from "@/features/auth";
import { Button } from "@/shared/ui";
import {
  useMarketplaceAddToCart,
  useMarketplaceFarmDetail,
  useMarketplaceProducts,
} from "@/features/marketplace/hooks";
import { formatVnd } from "@/features/marketplace/lib/format";
import {
  getFarmStoreEnrichedData,
  getFarmStoreMockProducts,
} from "./farmStoreMockData";
import type { FarmStoreEnrichedData, FarmStoreMockProduct } from "./farmStoreMockData";
import "./FarmStorePage.css";

// ═══════════════════════════════════════════════════════════════
// CONSTANTS
// ═══════════════════════════════════════════════════════════════

const SORT_OPTIONS = [
  { value: "newest", label: "Mới nhất" },
  { value: "price_asc", label: "Giá thấp → cao" },
  { value: "price_desc", label: "Giá cao → thấp" },
] as const;

type SortValue = (typeof SORT_OPTIONS)[number]["value"];

/** Fixed category tabs shown on the store page */
const CATEGORY_TABS = [
  { value: "", label: "Tất cả sản phẩm" },
  { value: "Vegetable", label: "Rau củ" },
  { value: "RICE", label: "Gạo" },
  { value: "SOYBEAN", label: "Đậu nành" },
  { value: "__other", label: "Khác" },
] as const;

// ═══════════════════════════════════════════════════════════════
// TYPES
// ═══════════════════════════════════════════════════════════════

type SimpleProduct = {
  id: number;
  slug: string;
  name: string;
  imageUrl: string;
  category: string;
  farmName: string | null;
  region: string | null;
  price: number;
  unit: string;
  availableQuantity: number;
  traceable: boolean;
};

// ═══════════════════════════════════════════════════════════════
// SMALL HELPER COMPONENTS
// ═══════════════════════════════════════════════════════════════

function Breadcrumb({ farmName }: { farmName: string }) {
  return (
    <nav className="farm-store-breadcrumb" aria-label="Breadcrumb">
      <Link to="/marketplace">Trang chủ</Link>
      <span className="farm-store-breadcrumb__separator" aria-hidden="true">
        <ChevronRight style={{ display: "inline", width: 12, height: 12 }} />
      </span>
      <Link to="/marketplace/products">Sản phẩm</Link>
      <span className="farm-store-breadcrumb__separator" aria-hidden="true">
        <ChevronRight style={{ display: "inline", width: 12, height: 12 }} />
      </span>
      <span aria-current="page">{farmName}</span>
    </nav>
  );
}

function ProductImage({ src, alt }: { src?: string | null; alt: string }) {
  const [hasError, setHasError] = useState(false);

  useEffect(() => {
    setHasError(false);
  }, [src]);

  if (!src || hasError) {
    return (
      <div className="fs-product-card__image-fallback">
        <div>
          <ImageOff className="mx-auto mb-1 h-6 w-6 opacity-60" aria-hidden="true" />
          <span>Đang cập nhật</span>
        </div>
      </div>
    );
  }

  return (
    <img
      src={src}
      alt={alt}
      className="fs-product-card__image"
      loading="lazy"
      referrerPolicy="no-referrer"
      onError={() => setHasError(true)}
    />
  );
}

// ═══════════════════════════════════════════════════════════════
// HERO: BANNER + PROFILE OVERLAY + STATS
// ═══════════════════════════════════════════════════════════════

type HeroProps = {
  farmName: string;
  coverImageUrl: string | null;
  enriched: FarmStoreEnrichedData;
  productCount: number;
};

function FarmHero({ farmName, coverImageUrl, enriched, productCount }: HeroProps) {
  const initial = farmName.slice(0, 1).toUpperCase();

  return (
    <section className="farm-store-banner-section">
      {/* ── Left: Banner with profile overlay ── */}
      <div className="farm-store-banner-area">
        {coverImageUrl ? (
          <img
            src={coverImageUrl}
            alt={`Ảnh bìa ${farmName}`}
            className="farm-store-banner-area__image"
            referrerPolicy="no-referrer"
          />
        ) : (
          <div className="farm-store-banner-area__placeholder" aria-hidden="true">
            <Tractor />
          </div>
        )}

        <div className="farm-store-profile-overlay">
          <div className="farm-store-profile-overlay__header">
            <div className="farm-store-profile-overlay__avatar" aria-hidden="true">
              {initial}
            </div>
            <div className="farm-store-profile-overlay__info">
              <div className="farm-store-profile-overlay__name-row">
                <h1 className="farm-store-profile-overlay__name">{farmName}</h1>
                {enriched.certifications.length > 0 && (
                  <span className="farm-store-profile-overlay__verified">
                    <BadgeCheck style={{ width: 11, height: 11 }} />
                    Đã xác minh
                  </span>
                )}
              </div>
              <div className="farm-store-profile-overlay__status">
                <span className="farm-store-profile-overlay__status-dot" />
                Đang hoạt động • Online 11 phút trước
              </div>
              <p className="farm-store-profile-overlay__desc">
                {enriched.introduction}
              </p>
            </div>
          </div>

          <div className="farm-store-profile-overlay__actions">
            <button
              type="button"
              className="farm-store-profile-overlay__btn farm-store-profile-overlay__btn--primary"
            >
              <MessageCircle /> Nhắn tin
            </button>
            <button
              type="button"
              className="farm-store-profile-overlay__btn farm-store-profile-overlay__btn--outline"
            >
              <UserPlus /> Theo dõi
            </button>
          </div>
        </div>
      </div>

      {/* ── Right: Stats card ── */}
      <div className="farm-store-stats-card">
        <div className="farm-store-stat-item">
          <BarChart3 className="farm-store-stat-item__icon" />
          <div className="farm-store-stat-item__content">
            <span className="farm-store-stat-item__value">{productCount}</span>
            <span className="farm-store-stat-item__label">Sản phẩm</span>
          </div>
        </div>
        <div className="farm-store-stat-item">
          <Users className="farm-store-stat-item__icon" />
          <div className="farm-store-stat-item__content">
            <span className="farm-store-stat-item__value">
              {enriched.followers.toLocaleString("vi-VN")}
            </span>
            <span className="farm-store-stat-item__label">Người theo dõi</span>
          </div>
        </div>
        <div className="farm-store-stat-item">
          <Star className="farm-store-stat-item__icon" />
          <div className="farm-store-stat-item__content">
            <span className="farm-store-stat-item__value">
              {enriched.rating.toFixed(1)}/5
            </span>
            <span className="farm-store-stat-item__label">
              ({enriched.ratingCount} đánh giá)
            </span>
          </div>
        </div>
        <div className="farm-store-stat-item">
          <Calendar className="farm-store-stat-item__icon" />
          <div className="farm-store-stat-item__content">
            <span className="farm-store-stat-item__value">
              {enriched.yearsActive} năm
            </span>
            <span className="farm-store-stat-item__label">Năm hoạt động</span>
          </div>
        </div>
      </div>
    </section>
  );
}

// ═══════════════════════════════════════════════════════════════
// PRODUCT CARD
// ═══════════════════════════════════════════════════════════════

/** Derive certification tags from farm-level certs + product traceability */
function getProductTags(
  product: SimpleProduct,
  enriched: FarmStoreEnrichedData,
): { label: string; type: "vietgap" | "organic" | "trace" }[] {
  const tags: { label: string; type: "vietgap" | "organic" | "trace" }[] = [];
  const certIds = enriched.certifications.map((c) => c.id);

  if (certIds.includes("vietgap")) {
    tags.push({ label: "VietGAP", type: "vietgap" });
  }
  if (certIds.includes("organic")) {
    tags.push({ label: "Hữu cơ", type: "organic" });
  }
  if (product.traceable) {
    tags.push({ label: "Truy xuất nguồn gốc", type: "trace" });
  }

  return tags;
}

function FarmProductCard({
  product,
  enriched,
  isAuthenticated,
  isAdding,
  onAddToCart,
}: {
  product: SimpleProduct;
  enriched: FarmStoreEnrichedData;
  isAuthenticated: boolean;
  isAdding: boolean;
  onAddToCart: (productId: number) => Promise<void>;
}) {
  const tags = getProductTags(product, enriched);
  const isMock = product.id < 0;

  return (
    <article className="fs-product-card">
      <div className="fs-product-card__media">
        <ProductImage src={product.imageUrl} alt={product.name} />
        {product.traceable && (
          <span className="fs-product-card__badge-trace">Có truy xuất</span>
        )}
        <button type="button" className="fs-product-card__fav-btn" aria-label="Yêu thích">
          <Heart />
        </button>
      </div>

      <div className="fs-product-card__body">
        <Link
          to={isMock ? "#" : `/marketplace/products/${product.slug}`}
          className="fs-product-card__name"
        >
          {product.name}
        </Link>

        {tags.length > 0 && (
          <div className="fs-product-card__tags">
            {tags.map((tag) => (
              <span
                key={tag.label}
                className={`fs-product-card__tag fs-product-card__tag--${tag.type}`}
              >
                {tag.type === "vietgap" && <Shield />}
                {tag.type === "organic" && <Leaf />}
                {tag.type === "trace" && <Shield />}
                {tag.label}
              </span>
            ))}
          </div>
        )}

        <div className="fs-product-card__price">
          {formatVnd(product.price)}
          <span className="fs-product-card__price-unit">/{product.unit}</span>
        </div>

        {isAuthenticated ? (
          <button
            type="button"
            className="fs-product-card__add-btn"
            disabled={isMock || isAdding || product.availableQuantity <= 0}
            onClick={() => {
              if (!isMock) onAddToCart(product.id);
            }}
          >
            <ShoppingCart /> Thêm vào giỏ
          </button>
        ) : (
          <Link to="/sign-up" className="fs-product-card__signup-btn">
            Tạo tài khoản để mua
          </Link>
        )}
      </div>
    </article>
  );
}

// ═══════════════════════════════════════════════════════════════
// BOTTOM TRUST BAR
// ═══════════════════════════════════════════════════════════════

function BottomTrustBar() {
  const items = [
    {
      icon: <CheckCircle />,
      title: "Nguồn gốc minh bạch",
      desc: "Truy xuất rõ ràng bằng mã QR",
    },
    {
      icon: <Leaf />,
      title: "Canh tác bền vững",
      desc: "An toàn cho sức khỏe và môi trường",
    },
    {
      icon: <Truck />,
      title: "Giao hàng nhanh",
      desc: "2–4 ngày trên toàn quốc",
    },
    {
      icon: <Headphones />,
      title: "Hỗ trợ tận tâm",
      desc: "Chat với nông trại nhanh chóng",
    },
  ];

  return (
    <div className="farm-store-bottom-trust">
      {items.map((item) => (
        <div key={item.title} className="farm-store-bottom-trust__item">
          <div className="farm-store-bottom-trust__icon">{item.icon}</div>
          <div>
            <div className="farm-store-bottom-trust__title">{item.title}</div>
            <div className="farm-store-bottom-trust__desc">{item.desc}</div>
          </div>
        </div>
      ))}
    </div>
  );
}

// ═══════════════════════════════════════════════════════════════
// LOADING / ERROR / NOT FOUND
// ═══════════════════════════════════════════════════════════════

function LoadingSkeleton() {
  return (
    <div className="container mx-auto px-4 py-6 farm-store-page">
      <div className="h-4 w-48 animate-pulse rounded bg-muted mb-4" />
      <div className="farm-store-skeleton-hero animate-pulse bg-muted" />
      <div className="farm-store-skeleton-bar animate-pulse bg-muted" />
      <div className="farm-store-products-grid">
        {Array.from({ length: 4 }, (_, i) => (
          <div key={i} className="fs-product-card">
            <div style={{ height: 190 }} className="animate-pulse bg-muted" />
            <div className="space-y-2 p-3">
              <div className="h-3 w-16 animate-pulse rounded bg-muted" />
              <div className="h-4 w-3/4 animate-pulse rounded bg-muted" />
              <div className="h-3 w-1/2 animate-pulse rounded bg-muted" />
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

function NotFoundState() {
  return (
    <div className="container mx-auto px-4 py-6">
      <div className="farm-store-error">
        <Tractor className="mx-auto mb-4 h-16 w-16 text-muted-foreground" />
        <h2>Không tìm thấy nông trại</h2>
        <p>Nông trại bạn tìm kiếm không tồn tại hoặc đã bị gỡ.</p>
        <Button asChild variant="outline" className="mt-2">
          <Link to="/marketplace">Quay về Marketplace</Link>
        </Button>
      </div>
    </div>
  );
}

function ErrorState({ message }: { message?: string }) {
  return (
    <div className="container mx-auto px-4 py-6">
      <div className="farm-store-error">
        <h2>Đã xảy ra lỗi</h2>
        <p>{message ?? "Không thể tải thông tin nông trại. Vui lòng thử lại."}</p>
        <Button asChild variant="outline" className="mt-2">
          <Link to="/marketplace">Quay về Marketplace</Link>
        </Button>
      </div>
    </div>
  );
}

// ═══════════════════════════════════════════════════════════════
// MAIN PAGE
// ═══════════════════════════════════════════════════════════════

export function FarmStorePage() {
  const { farmId: farmIdParam } = useParams<{ farmId: string }>();
  const farmId = Number(farmIdParam);
  const isValidFarmId = Number.isFinite(farmId) && farmId > 0;

  const { isAuthenticated } = useAuth();
  const { addToCart, isAdding } = useMarketplaceAddToCart();

  // ── Fetch data ──
  const farmQuery = useMarketplaceFarmDetail(isValidFarmId ? farmId : undefined);
  const productsQuery = useMarketplaceProducts(
    isValidFarmId ? { page: 0, size: 50 } : undefined,
  );

  // ── Local state ──
  const [searchQuery, setSearchQuery] = useState("");
  const [sortValue, setSortValue] = useState<SortValue>("newest");
  const [selectedCategory, setSelectedCategory] = useState("");

  // ── Derived ──
  const farm = farmQuery.data;
  const allProducts = useMemo(() => productsQuery.data?.items ?? [], [productsQuery.data]);

  // Filter products belonging to this farm
  const farmProducts = useMemo(
    () => allProducts.filter((p) => p.farmId === farmId),
    [allProducts, farmId],
  );

  // Supplement with mock products when real product count < 4
  const mockProducts = useMemo(
    () => (isValidFarmId ? getFarmStoreMockProducts(farmId) : []),
    [farmId, isValidFarmId],
  );

  const displayProducts: SimpleProduct[] = useMemo(() => {
    if (farmProducts.length >= 4) return farmProducts;

    // Merge real products with mock supplements, avoiding ID collisions
    const realIds = new Set(farmProducts.map((p) => p.id));
    const supplements = mockProducts
      .filter((mp: FarmStoreMockProduct) => !realIds.has(mp.id))
      .slice(0, 4 - farmProducts.length)
      .map((mp: FarmStoreMockProduct): SimpleProduct => ({
        id: mp.id,
        slug: mp.slug,
        name: mp.name,
        imageUrl: mp.imageUrl,
        category: mp.category,
        farmName: mp.farmName,
        region: mp.region,
        price: mp.price,
        unit: mp.unit,
        availableQuantity: mp.availableQuantity,
        traceable: mp.traceable,
      }));

    return [...farmProducts, ...supplements];
  }, [farmProducts, mockProducts]);

  // Apply local filters
  const filteredProducts = useMemo(() => {
    let items = displayProducts;

    if (selectedCategory === "__other") {
      const knownCats: string[] = CATEGORY_TABS.filter(
        (t) => t.value && t.value !== "__other",
      ).map((t) => t.value);
      items = items.filter((p) => !knownCats.includes(p.category));
    } else if (selectedCategory) {
      items = items.filter((p) => p.category === selectedCategory);
    }

    const trimmed = searchQuery.trim().toLowerCase();
    if (trimmed) {
      items = items.filter(
        (p) =>
          p.name.toLowerCase().includes(trimmed) ||
          p.category.toLowerCase().includes(trimmed),
      );
    }

    if (sortValue === "price_asc") {
      items = [...items].sort((a, b) => a.price - b.price);
    } else if (sortValue === "price_desc") {
      items = [...items].sort((a, b) => b.price - a.price);
    }

    return items;
  }, [displayProducts, selectedCategory, searchQuery, sortValue]);

  const enriched = useMemo(
    () => (isValidFarmId ? getFarmStoreEnrichedData(farmId) : null),
    [farmId, isValidFarmId],
  );

  // ── Guards ──
  if (!isValidFarmId) return <NotFoundState />;
  if (farmQuery.isLoading) return <LoadingSkeleton />;
  if (farmQuery.isError) return <ErrorState />;
  if (!farm) return <NotFoundState />;

  const enrichedData = enriched!;

  async function handleAddToCart(productId: number) {
    await addToCart(productId, 1);
  }

  return (
    <div className="container mx-auto px-4 py-2 farm-store-page">
      {/* 1. Breadcrumb */}
      <Breadcrumb farmName={farm.name} />

      {/* 2. Hero: Banner + Profile Overlay + Stats */}
      <FarmHero
        farmName={farm.name}
        coverImageUrl={farm.coverImageUrl}
        enriched={enrichedData}
        productCount={farm.productCount}
      />

      {/* 3. Category Tabs */}
      <div className="farm-store-category-tabs" role="tablist">
        {CATEGORY_TABS.map((tab) => (
          <button
            key={tab.value}
            type="button"
            role="tab"
            aria-selected={selectedCategory === tab.value}
            className={`farm-store-category-tab ${selectedCategory === tab.value ? "farm-store-category-tab--active" : ""
              }`}
            onClick={() => setSelectedCategory(tab.value)}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {/* 4. Product heading + toolbar */}
      <div className="farm-store-products-header">
        <h2>Sản phẩm ({displayProducts.length})</h2>
        <div className="farm-store-products-toolbar">
          <div className="farm-store-products-search">
            <Search />
            <input
              type="search"
              placeholder="Tìm trong cửa hàng"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
            />
          </div>
          <div className="farm-store-products-sort">
            <select
              value={sortValue}
              onChange={(e) => setSortValue(e.target.value as SortValue)}
            >
              {SORT_OPTIONS.map((opt) => (
                <option key={opt.value} value={opt.value}>
                  {opt.label}
                </option>
              ))}
            </select>
          </div>
          <button type="button" className="farm-store-filter-btn">
            <SlidersHorizontal /> Bộ lọc
          </button>
        </div>
      </div>

      {/* 5. Product grid */}
      {productsQuery.isLoading ? (
        <div className="farm-store-products-grid">
          {Array.from({ length: 4 }, (_, i) => (
            <div key={i} className="fs-product-card">
              <div style={{ height: 190 }} className="animate-pulse bg-muted" />
              <div className="space-y-2 p-3">
                <div className="h-3 w-16 animate-pulse rounded bg-muted" />
                <div className="h-4 w-3/4 animate-pulse rounded bg-muted" />
                <div className="h-3 w-1/2 animate-pulse rounded bg-muted" />
              </div>
            </div>
          ))}
        </div>
      ) : filteredProducts.length > 0 ? (
        <div className="farm-store-products-grid">
          {filteredProducts.map((product) => (
            <FarmProductCard
              key={product.id}
              product={product}
              enriched={enrichedData}
              isAuthenticated={isAuthenticated}
              isAdding={isAdding}
              onAddToCart={handleAddToCart}
            />
          ))}
        </div>
      ) : displayProducts.length > 0 ? (
        <div className="farm-store-empty">
          <Search />
          <h3>Không tìm thấy sản phẩm</h3>
          <p>Thử thay đổi từ khóa hoặc bộ lọc.</p>
        </div>
      ) : (
        <div className="farm-store-empty">
          <PackageOpen />
          <h3>Chưa có sản phẩm</h3>
          <p>Nông trại chưa đăng bán sản phẩm nào trên marketplace.</p>
        </div>
      )}

      {/* 6. Bottom trust bar */}
      <BottomTrustBar />
    </div>
  );
}
