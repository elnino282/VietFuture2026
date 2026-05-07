import { useState, type ReactNode } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { CheckSquare, Square } from "lucide-react";
import { cn } from "@/shared/lib";
import { useMarketplaceCategories } from "../hooks";
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
  const [pendingTraceable, setPendingTraceable] = useState(
    searchParams.get("traceable") === "true",
  );
  const [pendingSort, setPendingSort] = useState<SortValue>(
    (searchParams.get("sort") as SortValue | null) ?? "newest",
  );

  const categoriesQuery = useMarketplaceCategories(true);
  const categories = categoriesQuery.data ?? [];

  const hasChanges =
    pendingCategory ||
    pendingRegion.trim() ||
    pendingTraceable ||
    pendingSort !== "newest";

  function handleReset() {
    setPendingCategory("");
    setPendingRegion("");
    setPendingTraceable(false);
    setPendingSort("newest");
  }

  function handleApply() {
    const params = new URLSearchParams();

    if (pendingCategory) params.set("category", pendingCategory);
    if (pendingRegion.trim()) params.set("region", pendingRegion.trim());
    if (pendingTraceable) params.set("traceable", "true");
    if (pendingSort !== "newest") params.set("sort", pendingSort);

    params.set("page", "1");

    navigate(`/marketplace/products?${params.toString()}`);
    onClose();
  }

  return (
    <div
      className={cn(
        "z-50",
        "flex w-full min-w-0 flex-col",
        "max-h-[min(560px,calc(100vh-92px))]",
        "overflow-hidden rounded-2xl border border-gray-200 bg-white shadow-2xl",
      )}
      onMouseEnter={onMouseEnter}
      onMouseLeave={onMouseLeave}
    >
      {/* Header */}
      <div className="shrink-0 border-b border-gray-100 px-4 py-3 sm:px-5">
        <div className="flex items-start justify-between gap-4">
          <div className="min-w-0">
            <h3 className="text-base font-semibold text-gray-900">
              Bộ lọc sản phẩm
            </h3>
            <p className="mt-1 text-sm leading-5 text-gray-500">
              Lọc nhanh theo danh mục, khu vực và truy xuất nguồn gốc.
            </p>
          </div>

          {hasChanges ? (
            <button
              type="button"
              onClick={handleReset}
              className="shrink-0 rounded-full px-3 py-1 text-sm font-medium text-gray-500 transition hover:bg-gray-100 hover:text-gray-700"
            >
              Xóa lọc
            </button>
          ) : null}
        </div>
      </div>

      {/* Body */}
      <div className="min-h-0 flex-1 space-y-4 overflow-y-auto overflow-x-hidden px-4 py-4 [scrollbar-gutter:stable] sm:px-5">
        {/* Category chips */}
        <section className="space-y-3">
          <p className="text-xs font-semibold uppercase tracking-wide text-gray-500">
            Danh mục
          </p>

          {categoriesQuery.isLoading ? (
            <div className="flex flex-wrap gap-2.5">
              {Array.from({ length: 5 }, (_, i) => (
                <div
                  key={i}
                  className="h-8 w-20 animate-pulse rounded-full bg-gray-200"
                />
              ))}
            </div>
          ) : categoriesQuery.isError ? (
            <div className="space-y-2">
              <CategoryButton
                active={!pendingCategory}
                onClick={() => setPendingCategory("")}
              >
                Tất cả
              </CategoryButton>

              <p className="text-sm text-red-600">
                Không thể tải danh mục từ sản phẩm công khai.
              </p>
            </div>
          ) : (
            <div className="space-y-2">
              <div className="flex flex-wrap gap-2.5">
                <CategoryButton
                  active={!pendingCategory}
                  onClick={() => setPendingCategory("")}
                >
                  Tất cả
                </CategoryButton>

                {categories.map((cat) => (
                  <CategoryButton
                    key={cat}
                    active={pendingCategory === cat}
                    onClick={() =>
                      setPendingCategory(pendingCategory === cat ? "" : cat)
                    }
                  >
                    {getCategoryLabel(cat)}
                  </CategoryButton>
                ))}
              </div>

              {categories.length === 0 ? (
                <p className="text-sm text-gray-500">
                  Chưa có danh mục từ sản phẩm công khai.
                </p>
              ) : null}
            </div>
          )}
        </section>

        <div className="grid min-w-0 grid-cols-1 gap-4 sm:grid-cols-2">
          {/* Region input */}
          <section className="min-w-0 space-y-3">
            <p className="text-xs font-semibold uppercase tracking-wide text-gray-500">
              Khu vực
            </p>

            <input
              type="text"
              value={pendingRegion}
              onChange={(e) => setPendingRegion(e.target.value)}
              placeholder="Ví dụ: Lâm Đồng, An Giang, Đồng Tháp..."
              className="h-10 w-full min-w-0 rounded-xl border border-gray-300 bg-white px-3 text-sm text-gray-700 outline-none transition placeholder:text-gray-400 focus:border-emerald-500 focus:ring-2 focus:ring-emerald-500/20"
            />
          </section>

          {/* Sort */}
          <section className="min-w-0 space-y-3">
            <p className="text-xs font-semibold uppercase tracking-wide text-gray-500">
              Sắp xếp
            </p>

            <select
              value={pendingSort}
              onChange={(e) => setPendingSort(e.target.value as SortValue)}
              className="h-10 w-full min-w-0 rounded-xl border border-gray-300 bg-white px-3 text-sm text-gray-700 outline-none transition hover:border-gray-400 focus:border-emerald-500 focus:ring-2 focus:ring-emerald-500/20"
            >
              {SORT_OPTIONS.map((opt) => (
                <option key={opt.value} value={opt.value}>
                  {opt.label}
                </option>
              ))}
            </select>
          </section>
        </div>

        {/* Traceability */}
        <section className="space-y-3">
          <p className="text-xs font-semibold uppercase tracking-wide text-gray-500">
            Truy xuất nguồn gốc
          </p>

          <label
            className={cn(
              "flex cursor-pointer items-start gap-3 rounded-2xl border px-4 py-4 text-sm transition-colors",
              pendingTraceable
                ? "border-emerald-500 bg-emerald-50"
                : "border-gray-200 hover:border-gray-300 hover:bg-gray-50",
            )}
          >
            <input
              type="checkbox"
              className="sr-only"
              checked={pendingTraceable}
              onChange={(e) => setPendingTraceable(e.target.checked)}
            />

            {pendingTraceable ? (
              <CheckSquare
                size={18}
                className="mt-0.5 shrink-0 text-emerald-600"
              />
            ) : (
              <Square size={18} className="mt-0.5 shrink-0 text-gray-400" />
            )}

            <div className="min-w-0">
              <p
                className={cn(
                  "font-medium",
                  pendingTraceable ? "text-emerald-700" : "text-gray-700",
                )}
              >
                Chỉ sản phẩm có truy xuất
              </p>
              <p className="mt-1 text-xs leading-5 text-gray-500">
                Ưu tiên sản phẩm có mùa vụ và lô thu hoạch rõ ràng.
              </p>
            </div>
          </label>
        </section>
      </div>

      {/* Footer */}
      <div className="shrink-0 border-t border-gray-100 bg-white px-4 py-3 sm:px-5">
        <div className="grid grid-cols-1 gap-3 sm:grid-cols-[minmax(0,1fr)_auto] sm:items-center">
          <Link
            to="/marketplace/products"
            onClick={onClose}
            className="min-w-0 whitespace-nowrap text-sm font-semibold text-gray-600 hover:text-gray-800 hover:underline"
          >
            Xem tất cả sản phẩm
          </Link>

          <button
            type="button"
            onClick={handleApply}
            className="inline-flex h-11 w-full items-center justify-center whitespace-nowrap rounded-xl bg-emerald-600 px-6 text-sm font-semibold text-white shadow-sm transition hover:bg-emerald-700 focus:outline-none focus:ring-2 focus:ring-emerald-500 focus:ring-offset-2 sm:w-auto sm:min-w-40"
          >
            Áp dụng bộ lọc
          </button>
        </div>
      </div>
    </div>
  );
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
        "max-w-[11rem] truncate rounded-full px-4 py-1.5 text-sm font-medium transition-colors sm:max-w-[13rem]",
        active
          ? "bg-emerald-50 text-emerald-700 ring-1 ring-emerald-300"
          : "bg-gray-100 text-gray-700 hover:bg-gray-200",
      )}
    >
      {children}
    </button>
  );
}
