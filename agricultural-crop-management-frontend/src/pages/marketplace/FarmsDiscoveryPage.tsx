import { useMemo, useState, type FormEvent } from "react";
import {
  ChevronDown,
  ImageOff,
  Leaf,
  MapPin,
  MessageCircle,
  PackageOpen,
  Search,
  ShieldCheck,
  Sprout,
  Star,
  Store,
} from "lucide-react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "@/features/auth/context/AuthContext";
import { useMarketplaceFarms } from "@/features/marketplace/hooks";
import type { MarketplaceFarmSummary } from "@/shared/api";
import { getFarmDiscoveryMetadata, type FarmDiscoveryCategory } from "./farmDiscoveryMetadata";
import "./FarmsDiscoveryPage.css";

const CATEGORY_OPTIONS: Array<{ value: FarmDiscoveryCategory; label: string }> = [
  { value: "all", label: "Tất cả" },
  { value: "vegetables", label: "Rau củ" },
  { value: "fruits", label: "Trái cây" },
  { value: "rice", label: "Gạo" },
  { value: "specialties", label: "Đặc sản" },
];

const PRODUCT_TYPE_OPTIONS = [
  { value: "", label: "Tất cả loại" },
  { value: "vegetables", label: "Rau củ" },
  { value: "fruits", label: "Trái cây" },
  { value: "rice", label: "Gạo" },
  { value: "specialties", label: "Đặc sản" },
] as const;

const SORT_OPTIONS = [
  { value: "popular", label: "Phổ biến" },
  { value: "rating", label: "Đánh giá cao" },
  { value: "products", label: "Nhiều sản phẩm" },
  { value: "name", label: "Tên A-Z" },
] as const;

type ProductTypeValue = (typeof PRODUCT_TYPE_OPTIONS)[number]["value"];
type SortValue = (typeof SORT_OPTIONS)[number]["value"];

function normalizeText(value: string | null | undefined) {
  return (value ?? "").trim().toLocaleLowerCase("vi");
}

function getInitials(name: string, fallback?: string) {
  if (fallback) return fallback;

  const words = name
    .trim()
    .split(/\s+/)
    .filter(Boolean);

  if (words.length === 0) return "FA";
  if (words.length === 1) return words[0].slice(0, 2).toUpperCase();

  return `${words[0][0]}${words[words.length - 1][0]}`.toUpperCase();
}

function getFarmOwnerId(farm: MarketplaceFarmSummary) {
  const ownerFields = farm as MarketplaceFarmSummary & {
    ownerUserId?: unknown;
    ownerId?: unknown;
    farmerUserId?: unknown;
    userId?: unknown;
  };

  for (const value of [
    ownerFields.ownerUserId,
    ownerFields.ownerId,
    ownerFields.farmerUserId,
    ownerFields.userId,
  ]) {
    const parsed = Number(value);
    if (Number.isFinite(parsed) && parsed > 0) return parsed;
  }

  return null;
}

function matchesCategory(farmId: number, category: FarmDiscoveryCategory | ProductTypeValue) {
  if (!category || category === "all") return true;
  return getFarmDiscoveryMetadata(farmId).categories.includes(category);
}

function FarmImage({ src, alt }: { src?: string | null; alt: string }) {
  const [hasError, setHasError] = useState(false);

  if (!src || hasError) {
    return (
      <div className="farms-discovery-card__image-fallback">
        <ImageOff aria-hidden="true" />
        <span>Ảnh nông trại đang cập nhật</span>
      </div>
    );
  }

  return (
    <img
      src={src}
      alt={alt}
      className="farms-discovery-card__image"
      loading="lazy"
      referrerPolicy="no-referrer"
      onError={() => setHasError(true)}
    />
  );
}

type FarmCardProps = {
  farm: MarketplaceFarmSummary;
  onMessage: (farm: MarketplaceFarmSummary) => void;
};

function FarmCard({ farm, onMessage }: FarmCardProps) {
  const metadata = getFarmDiscoveryMetadata(farm.id);
  const farmName = farm.name || "Nông trại";
  const region = farm.region || farm.address || "Đang cập nhật khu vực";
  const coverImageUrl = farm.coverImageUrl || metadata.coverImageUrl;

  return (
    <article className="farms-discovery-card" aria-label={farmName}>
      <Link to={`/marketplace/farms/${farm.id}`} className="farms-discovery-card__media">
        <FarmImage src={coverImageUrl} alt={`Ảnh bìa ${farmName}`} />
        <span className="farms-discovery-card__favorite" aria-hidden="true">
          <Leaf />
        </span>
      </Link>

      <div className="farms-discovery-card__body">
        <div className="farms-discovery-card__identity">
          <div className="farms-discovery-card__avatar" aria-hidden="true">
            {getInitials(farmName, metadata.avatarLabel)}
          </div>
          <div className="farms-discovery-card__title">
            <Link to={`/marketplace/farms/${farm.id}`}>{farmName}</Link>
            <span>
              <MapPin aria-hidden="true" />
              {region}
            </span>
          </div>
        </div>

        <p className="farms-discovery-card__description">
          {metadata.specialty || metadata.description}
        </p>

        <div className="farms-discovery-card__stats" aria-label="Thống kê nông trại">
          <span>
            <PackageOpen aria-hidden="true" />
            {farm.productCount ?? 0} sản phẩm
          </span>
          <span>
            <Star aria-hidden="true" />
            {(farm.ratingAverage ?? 0).toFixed(1)} ({farm.ratingCount ?? 0})
          </span>
          {farm.hasTraceableProducts ? (
            <span>
              <ShieldCheck aria-hidden="true" />
              Có truy xuất
            </span>
          ) : null}
        </div>

        <div className="farms-discovery-card__badges flex flex-wrap gap-2">
          {farm.hasTraceableProducts ? (
            <span className="inline-flex items-center gap-1 bg-emerald-50 text-emerald-700 px-2 py-1 rounded-md text-xs font-semibold border border-emerald-100">
              <ShieldCheck className="w-3 h-3" />
              Cam kết minh bạch
            </span>
          ) : null}
          <span className="inline-flex items-center gap-1 bg-blue-50 text-blue-700 px-2 py-1 rounded-md text-xs font-semibold border border-blue-100">
            Cập nhật liên tục
          </span>
          {metadata.openForSale && farm.active ? (
            <span className="inline-flex items-center gap-1 bg-neutral-100 text-neutral-700 px-2 py-1 rounded-md text-xs font-semibold">
              Đang mở bán
            </span>
          ) : null}
        </div>

        <div className="farms-discovery-card__actions">
          <Link
            to={`/marketplace/farms/${farm.id}`}
            className="farms-discovery-card__primary"
            aria-label={`Xem nông trại ${farmName}`}
          >
            Xem nông trại
          </Link>
          <button
            type="button"
            className="farms-discovery-card__secondary"
            onClick={() => onMessage(farm)}
          >
            <MessageCircle aria-hidden="true" />
            Nhắn tin
          </button>
        </div>
      </div>
    </article>
  );
}

function FarmCardSkeleton() {
  return (
    <div className="farms-discovery-card farms-discovery-card--loading">
      <div className="farms-discovery-card__skeleton-media" />
      <div className="farms-discovery-card__skeleton-body">
        <span />
        <span />
        <span />
        <span />
      </div>
    </div>
  );
}

function EmptyState() {
  return (
    <div className="farms-discovery-state">
      <Store aria-hidden="true" />
      <h2>Chưa tìm thấy nông trại phù hợp</h2>
      <p>Hãy thử thay đổi từ khóa, khu vực hoặc bộ lọc nông sản.</p>
    </div>
  );
}

function ErrorState() {
  return (
    <div className="farms-discovery-state">
      <PackageOpen aria-hidden="true" />
      <h2>Không thể tải danh sách nông trại</h2>
      <p>Vui lòng thử lại sau ít phút.</p>
    </div>
  );
}

export function FarmsDiscoveryPage() {
  const navigate = useNavigate();
  const { isAuthenticated } = useAuth();
  const [searchDraft, setSearchDraft] = useState("");
  const [query, setQuery] = useState("");
  const [region, setRegion] = useState("");
  const [productType, setProductType] = useState<ProductTypeValue>("");
  const [traceableOnly, setTraceableOnly] = useState(false);
  const [openOnly, setOpenOnly] = useState(false);
  const [sort, setSort] = useState<SortValue>("popular");
  const [category, setCategory] = useState<FarmDiscoveryCategory>("all");

  const farmsQuery = useMarketplaceFarms({
    q: query.trim() || undefined,
    region: region.trim() || undefined,
    page: 0,
    size: 24,
  });

  const farms = farmsQuery.data?.items ?? [];
  const regionOptions = useMemo(() => {
    return Array.from(
      new Set(
        farms
          .map((farm) => farm.region?.trim())
          .filter((value): value is string => Boolean(value)),
      ),
    ).sort((left, right) => left.localeCompare(right, "vi"));
  }, [farms]);

  const filteredFarms = useMemo(() => {
    const selectedCategory = productType || category;

    return [...farms]
      .filter((farm) => {
        const metadata = getFarmDiscoveryMetadata(farm.id);
        const searchValue = normalizeText(searchDraft);
        const searchable = normalizeText(
          `${farm.name} ${farm.region ?? ""} ${farm.address ?? ""} ${metadata.specialty} ${metadata.description}`,
        );

        if (searchValue && !searchable.includes(searchValue)) return false;
        if (!matchesCategory(farm.id, selectedCategory)) return false;
        if (traceableOnly && !farm.hasTraceableProducts) return false;
        if (openOnly && (!farm.active || !metadata.openForSale || farm.productCount <= 0)) return false;
        return true;
      })
      .sort((left, right) => {
        if (sort === "rating") {
          return (right.ratingAverage ?? 0) - (left.ratingAverage ?? 0);
        }
        if (sort === "products") {
          return (right.productCount ?? 0) - (left.productCount ?? 0);
        }
        if (sort === "name") {
          return left.name.localeCompare(right.name, "vi");
        }
        return (
          (right.ratingAverage ?? 0) * 20 +
          (right.ratingCount ?? 0) +
          (right.productCount ?? 0) -
          ((left.ratingAverage ?? 0) * 20 + (left.ratingCount ?? 0) + (left.productCount ?? 0))
        );
      });
  }, [category, farms, openOnly, productType, searchDraft, sort, traceableOnly]);

  function handleSearchSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setQuery(searchDraft.trim());
  }

  function handleMessage(farm: MarketplaceFarmSummary) {
    if (!isAuthenticated) {
      navigate("/sign-in");
      return;
    }

    const ownerId = getFarmOwnerId(farm);

    window.dispatchEvent(
      new CustomEvent("open-chat-widget", {
        detail: ownerId ? { peerUserId: ownerId } : undefined,
      }),
    );
  }

  return (
    <div className="farms-discovery-page">
      <section className="farms-discovery-hero">
        <div className="farms-discovery-hero__content">
          <h1>Khám phá nông trại</h1>
          <p>
            Tìm kiếm nông trại uy tín theo khu vực và nông sản để kết nối nguồn gốc, an tâm chất lượng.
          </p>
          <form onSubmit={handleSearchSubmit} className="farms-discovery-hero__search">
            <Search aria-hidden="true" />
            <input
              type="search"
              value={searchDraft}
              onChange={(event) => setSearchDraft(event.target.value)}
              placeholder="Tìm kiếm nông trại, khu vực hoặc nông sản..."
            />
          </form>
        </div>
      </section>

      <main className="farms-discovery-content">
        <section className="farms-discovery-toolbar" aria-label="Bộ lọc nông trại">
          <label className="farms-discovery-field">
            <span>Khu vực</span>
            <select value={region} onChange={(event) => setRegion(event.target.value)} aria-label="Khu vực">
              <option value="">Tất cả khu vực</option>
              {regionOptions.map((option) => (
                <option key={option} value={option}>
                  {option}
                </option>
              ))}
            </select>
            <ChevronDown aria-hidden="true" />
          </label>

          <label className="farms-discovery-field">
            <span>Loại nông sản</span>
            <select
              value={productType}
              onChange={(event) => setProductType(event.target.value as ProductTypeValue)}
              aria-label="Loại nông sản"
            >
              {PRODUCT_TYPE_OPTIONS.map((option) => (
                <option key={option.value || "all"} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
            <ChevronDown aria-hidden="true" />
          </label>

          <button
            type="button"
            className={`farms-discovery-toggle${traceableOnly ? " is-active" : ""}`}
            aria-pressed={traceableOnly}
            onClick={() => setTraceableOnly((current) => !current)}
          >
            <ShieldCheck aria-hidden="true" />
            Có truy xuất
          </button>

          <button
            type="button"
            className={`farms-discovery-toggle${openOnly ? " is-active" : ""}`}
            aria-pressed={openOnly}
            onClick={() => setOpenOnly((current) => !current)}
          >
            <Store aria-hidden="true" />
            Đang mở bán
          </button>

          <label className="farms-discovery-field farms-discovery-field--sort">
            <span>Sắp xếp</span>
            <select value={sort} onChange={(event) => setSort(event.target.value as SortValue)}>
              {SORT_OPTIONS.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
            <ChevronDown aria-hidden="true" />
          </label>
        </section>

        <section className="farms-discovery-category-row" aria-label="Danh mục nông trại">
          <div className="farms-discovery-category-row__title">
            <Sprout aria-hidden="true" />
            <span>Nông trại nổi bật</span>
          </div>
          <div className="farms-discovery-chips">
            {CATEGORY_OPTIONS.map((option) => (
              <button
                key={option.value}
                type="button"
                className={category === option.value ? "is-active" : ""}
                onClick={() => setCategory(option.value)}
              >
                {option.label}
              </button>
            ))}
          </div>
        </section>

        {farmsQuery.isLoading ? (
          <div className="farms-discovery-grid">
            {Array.from({ length: 6 }, (_, index) => (
              <FarmCardSkeleton key={index} />
            ))}
          </div>
        ) : farmsQuery.isError ? (
          <ErrorState />
        ) : filteredFarms.length > 0 ? (
          <div className="farms-discovery-grid">
            {filteredFarms.map((farm) => (
              <FarmCard
                key={farm.id}
                farm={farm}
                onMessage={handleMessage}
              />
            ))}
          </div>
        ) : (
          <EmptyState />
        )}
      </main>
    </div>
  );
}
