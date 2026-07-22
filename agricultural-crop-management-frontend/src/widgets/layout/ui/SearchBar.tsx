import {
  useGlobalSearch,
  type SearchEntityType,
  type SearchResultItem,
} from "@/entities/search";
import { useTranslation } from "react-i18next";
import { useDebounce } from "@/shared/lib";
import { Badge, Input, Dialog, DialogContent, DialogTitle } from "@/shared/ui";
import { Loader2, Search, ArrowRight, CornerDownLeft, Sparkles } from "lucide-react";
import {
  useEffect,
  useMemo,
  useState,
} from "react";
import { useNavigate } from "react-router-dom";
import type { SearchBarProps } from "../model/types";

type PortalKind = "admin" | "farmer";

const DEFAULT_LIMIT = 6;

const TYPE_LABEL_KEYS: Record<SearchEntityType, string> = {
  FARM: "search.types.farms",
  PLOT: "search.types.plots",
  SEASON: "search.types.seasons",
  TASK: "search.types.tasks",
  EXPENSE: "search.types.expenses",
  DOCUMENT: "search.types.documents",
  USER: "search.types.users",
};

const TYPE_ORDER: Record<PortalKind, SearchEntityType[]> = {
  admin: ["FARM", "PLOT", "SEASON", "DOCUMENT", "USER"],
  farmer: ["TASK", "SEASON", "DOCUMENT", "PLOT", "EXPENSE"],
};

const QUICK_LINKS: Record<
  PortalKind,
  Array<{ labelKey: string; fallback: string; route: string }>
> = {
  admin: [
    {
      labelKey: "search.global.quickLinks.admin.inventoryRisks",
      fallback: "Cảnh báo Kho hàng",
      route: "/admin/inventory?status=RISK",
    },
    {
      labelKey: "search.global.quickLinks.admin.incidents",
      fallback: "Nhật ký Sự cố",
      route: "/admin/incidents",
    },
    {
      labelKey: "search.global.quickLinks.admin.alertsCenter",
      fallback: "Trung tâm Cảnh báo",
      route: "/admin/alerts",
    },
    {
      labelKey: "search.global.quickLinks.admin.farmsPlots",
      fallback: "Nông trại & Thửa đất",
      route: "/admin/farms-plots",
    },
  ],
  farmer: [
    {
      labelKey: "search.global.quickLinks.farmer.tasksWorkspace",
      fallback: "Không gian Công việc",
      route: "/farmer/tasks",
    },
    {
      labelKey: "search.global.quickLinks.farmer.expenses",
      fallback: "Quản lý Chi phí",
      route: "/farmer/expenses",
    },
    {
      labelKey: "search.global.quickLinks.farmer.documents",
      fallback: "Tài liệu Nông nghiệp",
      route: "/farmer/documents",
    },
    {
      labelKey: "search.global.quickLinks.farmer.inventory",
      fallback: "Vật tư & Kho hàng",
      route: "/farmer/inventory",
    },
  ],
};

type NavItem = {
  route: string;
  title: string;
  subtitle?: string | null;
  type?: SearchEntityType;
};

export function GlobalSearchBar({
  portal,
  placeholder,
}: SearchBarProps) {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [query, setQuery] = useState("");
  const [open, setOpen] = useState(false);
  const [highlightedIndex, setHighlightedIndex] = useState(-1);

  const debouncedQuery = useDebounce(query, 300);
  const normalizedQuery = debouncedQuery.trim();
  const canSearch = normalizedQuery.length >= 2;
  const resolvedPlaceholder =
    placeholder ??
    t(
      "search.global.placeholder",
      "Tìm kiếm nhanh công việc, mùa vụ, tài liệu... (Ctrl+K)"
    );

  // Global hotkey Ctrl+K / Cmd+K listener
  useEffect(() => {
    const handleGlobalKeyDown = (e: globalThis.KeyboardEvent) => {
      if ((e.metaKey || e.ctrlKey) && e.key === "k") {
        e.preventDefault();
        setOpen(true);
      }
    };
    window.addEventListener("keydown", handleGlobalKeyDown);
    return () => window.removeEventListener("keydown", handleGlobalKeyDown);
  }, []);

  const { data, isFetching, isError } = useGlobalSearch(
    { q: normalizedQuery, limit: DEFAULT_LIMIT },
    { enabled: canSearch, staleTime: 5000 }
  );

  const resultGroups = useMemo(() => {
    const groups: Record<string, SearchResultItem[]> = {};
    for (const item of data?.results ?? []) {
      const type = item.type;
      if (!groups[type]) {
        groups[type] = [];
      }
      if (groups[type].length < DEFAULT_LIMIT) {
        groups[type].push(item);
      }
    }
    return groups;
  }, [data?.results]);

  const orderedGroups = useMemo(() => {
    const order = TYPE_ORDER[portal];
    const known = new Set(order);
    const extras = Object.keys(resultGroups).filter(
      (type) => !known.has(type as SearchEntityType)
    );
    return [...order, ...(extras as SearchEntityType[])];
  }, [portal, resultGroups]);

  const flatResults = useMemo<NavItem[]>(() => {
    const items: NavItem[] = [];
    for (const type of orderedGroups) {
      const groupItems = resultGroups[type] ?? [];
      for (const item of groupItems) {
        items.push({
          route: item.route || "",
          title: item.title,
          subtitle: item.subtitle,
          type,
        });
      }
    }
    return items;
  }, [orderedGroups, resultGroups]);

  const quickLinks = QUICK_LINKS[portal];
  const quickLinkItems = useMemo<NavItem[]>(
    () =>
      quickLinks.map((link) => ({
        route: link.route,
        title: t(link.labelKey, link.fallback),
      })),
    [quickLinks, t]
  );

  const navigationItems = canSearch
    ? flatResults
    : normalizedQuery.length === 0
      ? quickLinkItems
      : [];

  useEffect(() => {
    setHighlightedIndex(-1);
  }, [normalizedQuery, open, flatResults.length]);

  const navigateTo = (route: string) => {
    if (!route) return;
    navigate(route);
    setOpen(false);
    setQuery("");
    setHighlightedIndex(-1);
  };

  const handleKeyDown = (event: React.KeyboardEvent<HTMLInputElement>) => {
    if (event.key === "Escape") {
      setOpen(false);
      setHighlightedIndex(-1);
      return;
    }

    if (event.key === "ArrowDown") {
      event.preventDefault();
      setHighlightedIndex((prev) =>
        navigationItems.length === 0
          ? -1
          : Math.min(prev + 1, navigationItems.length - 1)
      );
    }

    if (event.key === "ArrowUp") {
      event.preventDefault();
      setHighlightedIndex((prev) =>
        navigationItems.length === 0 ? -1 : Math.max(prev - 1, 0)
      );
    }

    if (event.key === "Enter") {
      event.preventDefault();
      if (highlightedIndex >= 0 && navigationItems[highlightedIndex]) {
        navigateTo(navigationItems[highlightedIndex].route);
        return;
      }
      if (canSearch) {
        navigate(`/${portal}/search?q=${encodeURIComponent(normalizedQuery)}`);
        setOpen(false);
        setQuery("");
      }
    }
  };

  return (
    <>
      {/* Clickable Header Button */}
      <div 
        onClick={() => setOpen(true)}
        className="relative cursor-pointer hidden md:flex items-center w-64 lg:w-96 h-10 border border-white/30 hover:border-white/60 bg-white/10 hover:bg-white/15 px-3 rounded-full text-white/70 select-none transition-colors duration-200"
      >
        <Search className="w-4 h-4 mr-2.5 text-white/80" />
        <span className="text-sm truncate">
          {resolvedPlaceholder}
        </span>
        <kbd className="absolute right-3 top-1/2 -translate-y-1/2 hidden sm:inline-flex h-5 select-none items-center gap-0.5 rounded border border-white/20 bg-white/10 px-1.5 font-mono text-[9px] font-medium text-white/95 opacity-80 shrink-0">
          Ctrl+K
        </kbd>
      </div>

      {/* Dialog Command Palette */}
      <Dialog open={open} onOpenChange={setOpen}>
        <DialogContent className="max-w-[620px] p-0 overflow-hidden border border-border shadow-2xl rounded-2xl bg-card">
          {/* Top Search Input */}
          <div className="flex items-center gap-3 px-4 py-3.5 border-b border-border/80 bg-muted/20">
            <Search className="w-5 h-5 text-muted-foreground shrink-0" />
            <DialogTitle className="sr-only">Tìm kiếm hệ thống</DialogTitle>
            <Input
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              onKeyDown={handleKeyDown}
              placeholder="Tìm kiếm công việc, tài liệu, vật tư... (Mũi tên để chọn, Enter để mở)"
              className="border-0 bg-transparent focus-visible:ring-0 focus-visible:ring-offset-0 px-0 h-9 text-base text-foreground placeholder:text-muted-foreground w-full"
              autoFocus
            />
            <kbd className="hidden sm:inline-flex h-5 select-none items-center gap-0.5 rounded border border-border bg-muted px-1.5 font-mono text-[10px] font-medium text-muted-foreground opacity-100 shrink-0">
              <span className="text-xs">ESC</span>
            </kbd>
          </div>

          {/* Results List */}
          <div className="max-h-[380px] overflow-y-auto p-3 space-y-2">
            {isFetching && (
              <div className="flex flex-col items-center justify-center py-10 gap-3 text-muted-foreground">
                <Loader2 className="w-7 h-7 animate-spin text-primary" />
                <span className="text-xs">Đang tìm kiếm thông tin...</span>
              </div>
            )}

            {isError && (
              <div className="px-3 py-4 text-sm text-destructive text-center">
                {t("search.global.error", "Tìm kiếm thất bại. Vui lòng thử lại sau.")}
              </div>
            )}

            {!isFetching && !isError && (
              <>
                {/* Empty State / Min Chars warning */}
                {normalizedQuery.length === 1 && (
                  <div className="px-3 py-4 text-sm text-muted-foreground text-center">
                    {t("search.global.minChars", "Nhập ít nhất 2 ký tự để bắt đầu tìm kiếm.")}
                  </div>
                )}

                {canSearch && flatResults.length === 0 && (
                  <div className="px-3 py-6 text-sm text-muted-foreground text-center">
                    {t("search.global.noResults", "Không tìm thấy kết quả phù hợp.")}
                  </div>
                )}

                {/* Quick Links (when input is empty) */}
                {normalizedQuery.length === 0 && (
                  <div className="space-y-1">
                    <p className="text-xs font-semibold text-muted-foreground px-2 py-1.5 uppercase tracking-wider flex items-center gap-1.5">
                      <Sparkles className="w-3.5 h-3.5 text-primary" />
                      {t("search.global.quickLinks.title", "Truy cập nhanh")}
                    </p>
                    {quickLinkItems.map((link, index) => {
                      const isActive = index === highlightedIndex;
                      return (
                        <div
                          key={link.route}
                          onClick={() => navigateTo(link.route)}
                          className={`flex items-center justify-between p-2.5 rounded-xl cursor-pointer transition-colors duration-200 ${
                            isActive
                              ? "bg-primary/5 text-foreground font-medium"
                              : "text-muted-foreground hover:bg-muted/50 hover:text-foreground"
                          }`}
                          onMouseEnter={() => setHighlightedIndex(index)}
                        >
                          <span className="text-sm">{link.title}</span>
                          <ArrowRight className={`w-4 h-4 opacity-60 transition-transform ${isActive ? "translate-x-1 opacity-100 text-primary" : ""}`} />
                        </div>
                      );
                    })}
                  </div>
                )}

                {/* Grouped Search Results */}
                {canSearch && flatResults.length > 0 && (
                  <div className="space-y-3">
                    {orderedGroups.map((type) => {
                      const items = resultGroups[type] ?? [];
                      if (!items.length) return null;

                      const groupStartIndex = flatResults.findIndex(
                        (item) => item.type === type
                      );

                      return (
                        <div key={type} className="space-y-1">
                          <div className="flex items-center justify-between px-2 py-1">
                            <span className="text-xs font-bold text-muted-foreground/80 uppercase tracking-wider">
                              {t(TYPE_LABEL_KEYS[type], type)}
                            </span>
                            <Badge variant="secondary" className="text-[10px] scale-90">
                              {items.length}
                            </Badge>
                          </div>
                          <div className="space-y-0.5">
                            {items.map((item, index) => {
                              const flatIndex = groupStartIndex + index;
                              const isActive = flatIndex === highlightedIndex;
                              return (
                                <div
                                  key={`${type}-${item.id}-${index}`}
                                  onClick={() => navigateTo(item.route || "")}
                                  className={`flex items-center justify-between p-2 rounded-xl cursor-pointer border border-transparent transition-colors duration-150 ${
                                    isActive
                                      ? "bg-primary/5 border-primary/25 shadow-sm"
                                      : "hover:bg-muted/40"
                                  }`}
                                  onMouseEnter={() => setHighlightedIndex(flatIndex)}
                                >
                                  <div className="min-w-0">
                                    <div className="text-sm font-semibold text-foreground truncate">
                                      {item.title}
                                    </div>
                                    {item.subtitle && (
                                      <div className="text-xs text-muted-foreground truncate">
                                        {item.subtitle}
                                      </div>
                                    )}
                                  </div>
                                  <CornerDownLeft className={`w-3.5 h-3.5 text-muted-foreground/60 transition-opacity ${isActive ? "opacity-100 text-primary" : "opacity-0"}`} />
                                </div>
                              );
                            })}
                          </div>
                        </div>
                      );
                    })}
                  </div>
                )}
              </>
            )}
          </div>
        </DialogContent>
      </Dialog>
    </>
  );
}

export const SearchBar = GlobalSearchBar;
