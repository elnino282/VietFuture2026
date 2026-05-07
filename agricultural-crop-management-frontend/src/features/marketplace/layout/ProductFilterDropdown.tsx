import { useMemo, useState, type ReactNode } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import {
  CheckCircle2,
  ChevronDown,
  Filter,
  MapPin,
  RotateCcw,
} from "lucide-react";
import { cn } from "@/shared/lib";
import { useMarketplaceCategories, useMarketplaceProducts } from "../hooks";
import { getCategoryLabel } from "../lib/categoryLabels";

interface ProductFilterDropdownProps {
  onMouseEnter: () => void;
  onMouseLeave: () => void;
  onClose: () => void;
}

const SORT_OPTIONS = [
  { value: "newest", label: "Mới nhất" },
  { value: "price_asc", label: "Giá thấp → cao" },
  { value: "price_desc", label: "Giá cao → thấp" },
] as const;

const REGION_QUERY = { page: 0, size: 100 };

type SortValue = (typeof SORT_OPTIONS)[number]["value"];

export function ProductFilterDropdown({
  onMouseEnter,
  onMouseLeave,
  onClose,
}: ProductFilterDropdownProps) {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  const [pendingCategory, setPendingCategory] = useState(
    searchParams.get("category") ?? "",
  );
  const [pendingRegion, setPendingRegion] = useState(
    searchParams.get("region") ?? "",
  );
  const [pendingSort, setPendingSort] = useState<SortValue>(
    (searchParams.get("sort") as SortValue | null) ?? "newest",
  );

  const categoriesQuery = useMarketplaceCategories(true);
  const productsQuery = useMarketplaceProducts(REGION_QUERY);

  const categories = categoriesQuery.data ?? [];
  const regionSuggestions = useMemo(
    () =>
      Array.from(
        new Set(
          (productsQuery.data?.items ?? [])
            .map((product) => product.region?.trim())
            .filter((region): region is string => Boolean(region)),
        ),
      ).sort((left, right) => left.localeCompare(right)),
    [productsQuery.data?.items],
  );

  const selectedCategoryLabel = pendingCategory
    ? getCategoryLabel(pendingCategory)
    : "Tất cả";
  const selectedSortLabel =
    SORT_OPTIONS.find((option) => option.value === pendingSort)?.label ??
    "Mới nhất";
  const selectedSummary = [
    selectedCategoryLabel,
    pendingRegion.trim() ? pendingRegion.trim() : null,
    selectedSortLabel,
  ].filter(Boolean);

  function handleReset() {
    setPendingCategory("");
    setPendingRegion("");
    setPendingSort("newest");
  }

  function handleApply() {
    const params = new URLSearchParams();

    if (pendingCategory) params.set("category", pendingCategory);
    if (pendingRegion.trim()) params.set("region", pendingRegion.trim());
    if (pendingSort !== "newest") params.set("sort", pendingSort);

    params.set("page", "1");

    navigate(`/marketplace/products?${params.toString()}`);
    onClose();
  }

  return (
    <div
      className="marketplace-filter-panel"
      onMouseEnter={onMouseEnter}
      onMouseLeave={onMouseLeave}
    >
      <div className="marketplace-filter-header">
        <div className="marketplace-filter-icon">
          <Filter size={22} aria-hidden="true" />
        </div>
        <div className="marketplace-filter-heading">
          <h3>Bộ lọc sản phẩm</h3>
          <p>Tìm sản phẩm theo danh mục, khu vực và cách sắp xếp.</p>
        </div>
      </div>

      <div className="marketplace-filter-body">
        <section className="marketplace-filter-section">
          <SectionLabel>DANH MỤC</SectionLabel>

          {categoriesQuery.isLoading ? (
            <div className="marketplace-filter-chip-list" aria-label="Đang tải danh mục">
              {Array.from({ length: 5 }, (_, index) => (
                <div key={index} className="marketplace-filter-chip-skeleton" />
              ))}
            </div>
          ) : categoriesQuery.isError ? (
            <div className="marketplace-filter-stack">
              <CategoryButton
                active={!pendingCategory}
                onClick={() => setPendingCategory("")}
              >
                Tất cả
              </CategoryButton>
              <p className="marketplace-filter-error">
                Không thể tải danh mục từ sản phẩm công khai.
              </p>
            </div>
          ) : (
            <div className="marketplace-filter-stack">
              <div className="marketplace-filter-chip-list">
                <CategoryButton
                  active={!pendingCategory}
                  onClick={() => setPendingCategory("")}
                >
                  Tất cả
                </CategoryButton>

                {categories.map((category) => (
                  <CategoryButton
                    key={category}
                    active={pendingCategory === category}
                    onClick={() =>
                      setPendingCategory(
                        pendingCategory === category ? "" : category,
                      )
                    }
                  >
                    {getCategoryLabel(category)}
                  </CategoryButton>
                ))}
              </div>

              {categories.length === 0 ? (
                <p className="marketplace-filter-muted">
                  Chưa có danh mục từ sản phẩm công khai.
                </p>
              ) : null}
            </div>
          )}
        </section>

        <section className="marketplace-filter-section">
          <SectionLabel>KHU VỰC</SectionLabel>

          <label className="marketplace-filter-field">
            <span className="sr-only">Khu vực</span>
            <MapPin className="marketplace-filter-field-icon" size={20} aria-hidden="true" />
            <input
              type="text"
              value={pendingRegion}
              onChange={(event) => setPendingRegion(event.target.value)}
              placeholder="Nhập hoặc chọn khu vực"
            />
          </label>

          {regionSuggestions.length > 0 ? (
            <div className="marketplace-filter-suggestion-list">
              {regionSuggestions.map((region) => (
                <SuggestionButton
                  key={region}
                  active={pendingRegion.trim() === region}
                  onClick={() => setPendingRegion(region)}
                >
                  <MapPin size={14} aria-hidden="true" />
                  <span>{region}</span>
                </SuggestionButton>
              ))}
            </div>
          ) : null}
        </section>

        <section className="marketplace-filter-section">
          <SectionLabel>SẮP XẾP</SectionLabel>

          <label className="marketplace-filter-field">
            <span className="sr-only">Sắp xếp</span>
            <CheckCircle2
              className="marketplace-filter-field-icon marketplace-filter-field-icon--green"
              size={20}
              aria-hidden="true"
            />
            <select
              value={pendingSort}
              onChange={(event) => setPendingSort(event.target.value as SortValue)}
            >
              {SORT_OPTIONS.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
            <ChevronDown className="marketplace-filter-select-icon" size={20} aria-hidden="true" />
          </label>
        </section>

        <div className="marketplace-filter-summary" aria-live="polite">
          <CheckCircle2 size={18} aria-hidden="true" />
          <span>Đã chọn:</span>
          <strong>{selectedSummary.join(" · ")}</strong>
        </div>
      </div>

      <div className="marketplace-filter-footer">
        <button
          type="button"
          onClick={handleReset}
          className="marketplace-filter-reset"
        >
          <RotateCcw size={17} aria-hidden="true" />
          <span>Đặt lại</span>
        </button>

        <button
          type="button"
          onClick={handleApply}
          className="marketplace-filter-apply"
        >
          <CheckCircle2 size={18} aria-hidden="true" />
          <span>Áp dụng bộ lọc</span>
        </button>
      </div>
    </div>
  );
}

function SectionLabel({ children }: { children: ReactNode }) {
  return <p className="marketplace-filter-label">{children}</p>;
}

interface CategoryButtonProps {
  active: boolean;
  onClick: () => void;
  children: ReactNode;
}

function CategoryButton({ active, onClick, children }: CategoryButtonProps) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={cn(
        "marketplace-filter-chip",
        active && "marketplace-filter-chip--active",
      )}
    >
      <span>{children}</span>
    </button>
  );
}

interface SuggestionButtonProps {
  active: boolean;
  onClick: () => void;
  children: ReactNode;
}

function SuggestionButton({
  active,
  onClick,
  children,
}: SuggestionButtonProps) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={cn(
        "marketplace-filter-suggestion",
        active && "marketplace-filter-suggestion--active",
      )}
    >
      {children}
    </button>
  );
}
