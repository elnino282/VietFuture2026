import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { Search, ShoppingCart, Loader2, ArrowRight } from "lucide-react";
import {
  Dialog,
  DialogContent,
  DialogTitle,
  Input,
  Button,
} from "@/shared/ui";
import { useMarketplaceProducts, useMarketplaceAddToCart } from "../hooks";
import { formatVnd } from "../lib/format";

interface SearchWindowProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export function SearchWindow({ open, onOpenChange }: SearchWindowProps) {
  const navigate = useNavigate();
  const [query, setQuery] = useState("");
  const [debouncedQuery, setDebouncedQuery] = useState("");
  const { addToCart, isAdding } = useMarketplaceAddToCart();
  const [activeIndex, setActiveIndex] = useState(-1);

  // Debounce query
  useEffect(() => {
    const handler = setTimeout(() => {
      setDebouncedQuery(query);
    }, 300);
    return () => clearTimeout(handler);
  }, [query]);

  // Reset active index when query changes
  useEffect(() => {
    setActiveIndex(-1);
  }, [debouncedQuery]);

  // Fetch results based on debounced query
  const { data: searchResult, isLoading: isSearching } = useMarketplaceProducts(
    debouncedQuery ? { q: debouncedQuery, size: 6 } : undefined
  );

  // Fetch recommendations when query is empty
  const { data: recommendedResult, isLoading: isRecommendedLoading } = useMarketplaceProducts(
    !debouncedQuery ? { size: 4 } : undefined
  );

  const products = debouncedQuery ? (searchResult?.items ?? []) : (recommendedResult?.items ?? []);
  const isLoading = debouncedQuery ? isSearching : isRecommendedLoading;

  // Handle Cmd+K / Ctrl+K global keyboard shortcut
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if ((e.metaKey || e.ctrlKey) && e.key === "k") {
        e.preventDefault();
        onOpenChange(true);
      }
    };
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [onOpenChange]);

  // Handle arrow keys and enter in results list
  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (products.length === 0) return;

    if (e.key === "ArrowDown") {
      e.preventDefault();
      setActiveIndex((prev) => (prev < products.length - 1 ? prev + 1 : prev));
    } else if (e.key === "ArrowUp") {
      e.preventDefault();
      setActiveIndex((prev) => (prev > 0 ? prev - 1 : -1));
    } else if (e.key === "Enter" && activeIndex >= 0) {
      e.preventDefault();
      const activeProduct = products[activeIndex];
      navigate(`/marketplace/products/${activeProduct.slug}`);
      onOpenChange(false);
    }
  };

  const handleProductClick = (slug: string) => {
    navigate(`/marketplace/products/${slug}`);
    onOpenChange(false);
  };

  const handleAddToCart = async (e: React.MouseEvent, productId: number) => {
    e.stopPropagation(); // Prevent navigation
    await addToCart(productId, 1);
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-[650px] p-0 overflow-hidden border border-border shadow-2xl rounded-2xl bg-card">
        {/* Search Input Bar */}
        <div className="flex items-center gap-3 px-4 py-3.5 border-b border-border/80 bg-muted/20">
          <Search className="w-5 h-5 text-muted-foreground shrink-0" />
          <DialogTitle className="sr-only">Tìm kiếm sản phẩm</DialogTitle>
          <Input
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            onKeyDown={handleKeyDown}
            placeholder="Tìm kiếm nông sản, nông trại... (Mũi tên để chọn, Enter để xem)"
            className="border-0 bg-transparent focus-visible:ring-0 focus-visible:ring-offset-0 px-0 h-9 text-base text-foreground placeholder:text-muted-foreground w-full"
            autoFocus
          />
          <kbd className="hidden sm:inline-flex h-5 select-none items-center gap-0.5 rounded border border-border bg-muted px-1.5 font-mono text-[10px] font-medium text-muted-foreground opacity-100 shrink-0">
            <span className="text-xs">ESC</span>
          </kbd>
        </div>

        {/* Results Panel */}
        <div className="max-h-[380px] overflow-y-auto p-3 space-y-2">
          {isLoading ? (
            <div className="flex flex-col items-center justify-center py-12 gap-3 text-muted-foreground">
              <Loader2 className="w-8 h-8 animate-spin text-primary" />
              <p className="text-sm">Đang tìm kiếm thông tin...</p>
            </div>
          ) : products.length > 0 ? (
            <div className="space-y-1">
              <p className="text-xs font-semibold text-muted-foreground px-2 py-1.5 uppercase tracking-wider">
                {debouncedQuery ? "Kết quả tìm kiếm" : "Gợi ý sản phẩm cho bạn"}
              </p>
              {products.map((product, index) => {
                const isActive = index === activeIndex;
                return (
                  <div
                    key={product.id}
                    onClick={() => handleProductClick(product.slug)}
                    className={`flex items-center justify-between p-2 rounded-xl border border-transparent cursor-pointer transition-all duration-200 ${
                      isActive
                        ? "bg-primary/5 border-primary/20 shadow-sm"
                        : "hover:bg-muted/50 hover:border-border/50"
                    }`}
                  >
                    <div className="flex items-center gap-3 min-w-0">
                      <div className="h-12 w-12 rounded-lg bg-muted overflow-hidden shrink-0 border border-border/40">
                        {product.imageUrl ? (
                          <img
                            src={product.imageUrl}
                            alt={product.name}
                            className="w-full h-full object-cover"
                          />
                        ) : (
                          <div className="w-full h-full flex items-center justify-center text-muted-foreground/40 font-bold text-xs bg-muted">
                            ACM
                          </div>
                        )}
                      </div>
                      <div className="min-w-0">
                        <div className="flex items-center gap-2">
                          <p className="font-semibold text-sm text-foreground truncate">
                            {product.name}
                          </p>
                          <span className="text-[10px] bg-muted border border-border px-1.5 py-0.5 rounded-full text-muted-foreground font-medium shrink-0">
                            {product.category}
                          </span>
                        </div>
                        <p className="text-xs text-muted-foreground truncate">
                          {product.farmName ?? "Hợp tác xã VietFuture"}
                        </p>
                        <p className="text-sm font-bold text-primary mt-0.5">
                          {formatVnd(product.price)}
                        </p>
                      </div>
                    </div>

                    <div className="flex items-center gap-2 shrink-0">
                      <Button
                        size="sm"
                        variant="ghost"
                        onClick={(e) => handleAddToCart(e, product.id)}
                        disabled={isAdding}
                        className="h-8 px-2.5 hover:bg-primary/10 hover:text-primary rounded-lg"
                        title="Thêm vào giỏ hàng"
                      >
                        <ShoppingCart className="w-4 h-4" />
                      </Button>
                      <ArrowRight className={`w-4 h-4 text-muted-foreground/60 transition-transform ${isActive ? "translate-x-1 text-primary" : ""}`} />
                    </div>
                  </div>
                );
              })}
            </div>
          ) : (
            <div className="text-center py-10 text-muted-foreground">
              <p className="text-sm">Không tìm thấy sản phẩm nào khớp với "{query}"</p>
              <p className="text-xs mt-1 text-muted-foreground/80">Thử tìm kiếm với từ khóa khác xem sao nhé!</p>
            </div>
          )}
        </div>
      </DialogContent>
    </Dialog>
  );
}
