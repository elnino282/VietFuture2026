import { useEffect, useState } from "react";
import {
  Dialog,
  DialogContent,
  DialogTitle,
  Input,
  Badge,
} from "@/shared/ui";
import {
  Search,
  Loader2,
  Package,
  CheckSquare,
  FileText,
  Calendar,
  MapPin,
  ArrowRight,
  User,
  DollarSign,
  CornerDownLeft,
} from "lucide-react";
import { searchApi } from "@/entities/search";
import { marketplaceApi } from "@/shared/api";

export interface SearchResultItemData {
  id: string | number;
  title: string;
  subtitle?: string | null;
  type: string;
  route: string;
}

interface SearchWindowProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onSelect: (result: SearchResultItemData) => void;
}

// Aggregated search across searchApi (seasons, tasks, docs, plots) & marketplaceApi (products)
async function searchAll(query: string): Promise<SearchResultItemData[]> {
  try {
    const globalPromise = searchApi.search({ q: query, limit: 6 }).catch((err) => {
      console.error("Global search error:", err);
      return { results: [] };
    });

    const productsPromise = marketplaceApi.listProducts({ q: query, size: 6 }).catch((err) => {
      console.error("Marketplace products search error:", err);
      return { result: { items: [] } };
    });

    const [globalRes, productsRes] = await Promise.all([
      globalPromise,
      productsPromise,
    ]);

    const results: SearchResultItemData[] = [];

    // Add global search results (tasks, seasons, docs, plots, farms, users, expenses)
    if (globalRes && Array.isArray(globalRes.results)) {
      globalRes.results.forEach((item) => {
        results.push({
          id: `global-${item.type}-${item.id}`,
          title: item.title,
          subtitle: item.subtitle,
          type: item.type,
          route: item.route || "",
        });
      });
    }

    // Add marketplace products
    if (productsRes && productsRes.result && Array.isArray(productsRes.result.items)) {
      productsRes.result.items.forEach((item) => {
        results.push({
          id: `product-${item.id}`,
          title: item.name,
          subtitle: item.farmName || "Marketplace Product",
          type: "PRODUCT",
          route: `/marketplace/products/${item.slug}`,
        });
      });
    }

    return results;
  } catch (error) {
    console.error("Failed to run unified searchAll query", error);
    return [];
  }
}

function getResultIcon(type: string) {
  switch (type) {
    case "PRODUCT":
      return <Package className="w-4 h-4 text-emerald-600 dark:text-emerald-400" />;
    case "TASK":
      return <CheckSquare className="w-4 h-4 text-blue-600 dark:text-blue-400" />;
    case "DOCUMENT":
      return <FileText className="w-4 h-4 text-amber-600 dark:text-amber-400" />;
    case "SEASON":
      return <Calendar className="w-4 h-4 text-purple-600 dark:text-purple-400" />;
    case "PLOT":
      return <MapPin className="w-4 h-4 text-red-600 dark:text-red-400" />;
    case "FARM":
      return <MapPin className="w-4 h-4 text-teal-600 dark:text-teal-400" />;
    case "USER":
      return <User className="w-4 h-4 text-indigo-600 dark:text-indigo-400" />;
    case "EXPENSE":
      return <DollarSign className="w-4 h-4 text-orange-600 dark:text-orange-400" />;
    default:
      return <Search className="w-4 h-4 text-muted-foreground" />;
  }
}

function getResultBadge(type: string) {
  switch (type) {
    case "PRODUCT":
      return <Badge variant="success" className="scale-90 text-[10px]">Sản phẩm</Badge>;
    case "TASK":
      return <Badge variant="info" className="scale-90 text-[10px]">Công việc</Badge>;
    case "DOCUMENT":
      return <Badge variant="warning" className="scale-90 text-[10px]">Tài liệu</Badge>;
    case "SEASON":
      return <Badge variant="default" className="scale-90 text-[10px]">Mùa vụ</Badge>;
    case "PLOT":
      return <Badge variant="destructive" className="scale-90 text-[10px]">Thửa đất</Badge>;
    case "FARM":
      return <Badge variant="secondary" className="scale-90 text-[10px]">Nông trại</Badge>;
    case "USER":
      return <Badge variant="outline" className="scale-90 text-[10px]">Thành viên</Badge>;
    case "EXPENSE":
      return <Badge variant="warning" className="scale-90 text-[10px]">Chi phí</Badge>;
    default:
      return <Badge variant="outline" className="scale-90 text-[10px]">{type}</Badge>;
  }
}

export function SearchWindow({ open, onOpenChange, onSelect }: SearchWindowProps) {
  const [query, setQuery] = useState("");
  const [loading, setLoading] = useState(false);
  const [results, setResults] = useState<SearchResultItemData[]>([]);
  const [activeIndex, setActiveIndex] = useState(-1);

  // Debounce search input
  useEffect(() => {
    if (query.trim().length < 2) {
      setResults([]);
      return;
    }

    setLoading(true);
    const timer = setTimeout(async () => {
      const data = await searchAll(query);
      setResults(data);
      setLoading(false);
    }, 300);

    return () => clearTimeout(timer);
  }, [query]);

  // Reset indices on query or open state changes
  useEffect(() => {
    setActiveIndex(-1);
  }, [query, open]);

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (results.length === 0) return;

    if (e.key === "ArrowDown") {
      e.preventDefault();
      setActiveIndex((prev) => (prev < results.length - 1 ? prev + 1 : prev));
    } else if (e.key === "ArrowUp") {
      e.preventDefault();
      setActiveIndex((prev) => (prev > 0 ? prev - 1 : -1));
    } else if (e.key === "Enter" && activeIndex >= 0) {
      e.preventDefault();
      const selected = results[activeIndex];
      onSelect(selected);
      onOpenChange(false);
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-[620px] p-0 overflow-hidden border border-border shadow-2xl rounded-2xl bg-card">
        {/* Search input bar */}
        <div className="flex items-center gap-3 px-4 py-3.5 border-b border-border/80 bg-muted/20">
          <Search className="w-5 h-5 text-muted-foreground shrink-0" />
          <DialogTitle className="sr-only">Tìm kiếm tổng hợp</DialogTitle>
          <Input
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            onKeyDown={handleKeyDown}
            placeholder="Tìm kiếm nhanh công việc, nông sản, tài liệu..."
            className="border-0 bg-transparent focus-visible:ring-0 focus-visible:ring-offset-0 px-0 h-9 text-base text-foreground placeholder:text-muted-foreground w-full"
            autoFocus
          />
          <kbd className="hidden sm:inline-flex h-5 select-none items-center gap-0.5 rounded border border-border bg-muted px-1.5 font-mono text-[10px] font-medium text-muted-foreground opacity-100 shrink-0">
            <span className="text-xs">ESC</span>
          </kbd>
        </div>

        {/* Results section */}
        <div className="max-h-[360px] overflow-y-auto p-3 space-y-1">
          {loading && (
            <div className="flex flex-col items-center justify-center py-10 gap-2 text-muted-foreground">
              <Loader2 className="w-7 h-7 animate-spin text-primary" />
              <p className="text-xs">Đang tìm kiếm thông tin...</p>
            </div>
          )}

          {!loading && results.length > 0 && (
            <div className="space-y-0.5">
              {results.map((r, index) => {
                const isActive = index === activeIndex;
                return (
                  <div
                    key={r.id}
                    onClick={() => {
                      onSelect(r);
                      onOpenChange(false);
                    }}
                    className={`flex items-center justify-between p-2.5 rounded-xl cursor-pointer border border-transparent transition-all duration-150 ${
                      isActive
                        ? "bg-primary/5 border-primary/20 shadow-sm"
                        : "hover:bg-muted/50"
                    }`}
                  >
                    <div className="flex items-center gap-3 min-w-0">
                      <div className="w-8 h-8 rounded-lg bg-muted flex items-center justify-center shrink-0 border border-border/40">
                        {getResultIcon(r.type)}
                      </div>
                      <div className="min-w-0">
                        <div className="flex items-center gap-2">
                          <p className="font-semibold text-sm text-foreground truncate">
                            {r.title}
                          </p>
                          {getResultBadge(r.type)}
                        </div>
                        {r.subtitle && (
                          <p className="text-xs text-muted-foreground truncate">
                            {r.subtitle}
                          </p>
                        )}
                      </div>
                    </div>
                    <div className="flex items-center gap-2 shrink-0">
                      <CornerDownLeft className={`w-3.5 h-3.5 text-muted-foreground/60 transition-opacity ${isActive ? "opacity-100 text-primary" : "opacity-0"}`} />
                      <ArrowRight className={`w-4 h-4 text-muted-foreground/60 transition-transform ${isActive ? "translate-x-1 text-primary" : ""}`} />
                    </div>
                  </div>
                );
              })}
            </div>
          )}

          {!loading && query.trim().length >= 2 && results.length === 0 && (
            <div className="text-center py-8 text-muted-foreground text-sm">
              Không tìm thấy kết quả nào phù hợp.
            </div>
          )}

          {!loading && query.trim().length < 2 && (
            <div className="text-center py-8 text-muted-foreground text-xs">
              Nhập tối thiểu 2 ký tự để tìm kiếm nhanh.
            </div>
          )}
        </div>
      </DialogContent>
    </Dialog>
  );
}
