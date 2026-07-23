import React, { useState } from "react";
import { Link } from "react-router-dom";
import { ImageOff, Star, ShieldCheck, ShoppingCart, UserCircle2 } from "lucide-react";
import { getCategoryLabel } from "@/features/marketplace/lib/categoryLabels";
import { formatVnd } from "@/features/marketplace/lib/format";
import { Badge, Button, Card, CardContent } from "@/shared/ui";
import { cn } from "@/shared/lib";

export type MarketplaceProductCardProps = {
  product: {
    id: number;
    slug: string;
    name: string;
    imageUrl?: string | null;
    traceable?: boolean;
    category: string;
    farmName?: string | null;
    region?: string | null;
    price: number;
    unit: string;
    availableQuantity: number;
    ratingAverage?: number | null;
    ratingCount?: number;
    // mock properties for the new design
    updatesCount?: number;
    farmerAvatarUrl?: string | null;
    farmerName?: string | null;
  };
  isAuthenticated?: boolean;
  isAdding?: boolean;
  onAddToCart?: (productId: number) => Promise<void>;
  hideAddToCart?: boolean;
};

function ProductImage({ src, alt }: { src?: string | null; alt: string }) {
  const [hasError, setHasError] = useState(false);

  if (!src || hasError) {
    return (
      <div className="flex h-full w-full flex-col items-center justify-center bg-neutral-100 text-neutral-400">
        <ImageOff className="mb-2 h-8 w-8 opacity-50" aria-hidden="true" />
        <span className="text-xs">Chưa có ảnh</span>
      </div>
    );
  }

  return (
    <img
      src={src}
      alt={alt}
      className="h-full w-full object-cover transition-transform duration-500 group-hover:scale-105"
      loading="lazy"
      referrerPolicy="no-referrer"
      onError={() => setHasError(true)}
    />
  );
}

export function MarketplaceProductCard({
  product,
  isAuthenticated = false,
  isAdding = false,
  onAddToCart,
  hideAddToCart = false,
}: MarketplaceProductCardProps) {
  const isSoldOut = product.availableQuantity <= 0;
  
  // Fake some data if missing to demonstrate the new design
  const updatesCount = product.updatesCount ?? Math.floor(Math.random() * 20) + 5;
  const farmerName = product.farmerName ?? product.farmName ?? "Nông dân vô danh";
  const avatarLetter = farmerName.charAt(0).toUpperCase();

  return (
    <Card className="group flex h-full flex-col overflow-hidden border-border/50 bg-card shadow-sm transition-all hover:border-emerald-500/30 hover:shadow-md">
      <Link to={`/marketplace/products/${product.slug}`} className="relative aspect-[4/3] overflow-hidden bg-muted sm:aspect-square block">
        <ProductImage src={product.imageUrl} alt={product.name} />
        
        {/* NEW: Trust Score / Progress Bar instead of simple badge */}
        {product.traceable && (
          <div className="absolute left-2 top-2 right-2 flex flex-col gap-1 rounded-lg bg-black/40 p-2 backdrop-blur-md">
             <div className="flex items-center gap-1.5 text-white">
                <ShieldCheck className="h-4 w-4 text-emerald-400" />
                <span className="text-xs font-bold leading-none">Minh Bạch 100%</span>
             </div>
             <div className="h-1.5 w-full overflow-hidden rounded-full bg-white/20">
                <div className="h-full bg-emerald-400" style={{ width: '80%' }}></div>
             </div>
             <span className="text-[10px] font-medium text-white/90">Đã cập nhật {updatesCount} nhật ký</span>
          </div>
        )}
      </Link>

      <CardContent className="flex flex-1 flex-col p-4">
        {/* NEW: Farmer Info Overlay Feel */}
        <div className="mb-3 -mt-8 relative z-10 flex items-end gap-2">
           {product.farmerAvatarUrl ? (
             <img src={product.farmerAvatarUrl} alt={farmerName} className="h-10 w-10 rounded-full border-2 border-white bg-white object-cover shadow-sm" />
           ) : (
             <div className="flex h-10 w-10 items-center justify-center rounded-full border-2 border-white bg-emerald-100 text-sm font-bold text-emerald-700 shadow-sm">
                {avatarLetter}
             </div>
           )}
           <div className="flex flex-col drop-shadow-sm">
             <span className="text-[10px] font-medium text-emerald-700 uppercase tracking-wider bg-white/80 backdrop-blur rounded px-1 leading-tight inline-block mb-0.5">Trồng bởi</span>
             <span className="text-xs font-bold text-neutral-800 leading-none truncate max-w-[120px]">{farmerName}</span>
           </div>
        </div>

        <div className="mb-1 text-[11px] font-medium text-muted-foreground">
          {getCategoryLabel(product.category)}
        </div>
        
        <Link
          to={`/marketplace/products/${product.slug}`}
          className="mb-1 line-clamp-2 h-10 text-sm font-semibold leading-5 text-foreground transition-colors hover:text-emerald-600"
        >
          {product.name}
        </Link>
        
        <p className="mb-3 line-clamp-1 text-xs text-muted-foreground flex items-center gap-1">
           {product.region ? product.region : "Khu vực đang cập nhật"}
        </p>

        <div className="mt-auto flex flex-col gap-3">
          <div className="flex items-end justify-between">
            <div>
              <span className="text-lg font-bold text-emerald-600">
                {formatVnd(product.price)}
              </span>
              <span className="ml-1 text-xs font-normal text-muted-foreground">/{product.unit}</span>
            </div>
            {(product.ratingAverage && product.ratingAverage > 0) ? (
              <span className="flex items-center gap-1 text-xs font-medium text-muted-foreground">
                <Star className="h-3 w-3 fill-yellow-400 text-yellow-400" />
                {product.ratingAverage.toFixed(1)}
              </span>
            ) : (
              <span className="text-xs text-muted-foreground">Mới</span>
            )}
          </div>

          {!hideAddToCart && (
            <div className="flex gap-2">
              {isAuthenticated ? (
                <Button
                  size="sm"
                  className={cn(
                    "flex-1 h-9 rounded-lg text-xs font-semibold shadow-sm transition-all", 
                    isSoldOut ? "bg-neutral-100 text-neutral-400" : "bg-emerald-600 hover:bg-emerald-700 text-white hover:shadow"
                  )}
                  disabled={isAdding || isSoldOut}
                  onClick={(e) => {
                    e.preventDefault();
                    onAddToCart?.(product.id);
                  }}
                >
                  <ShoppingCart className="h-4 w-4 mr-1.5" />
                  {isSoldOut ? "Hết hàng" : "Thêm giỏ"}
                </Button>
              ) : (
                <Button asChild size="sm" variant="outline" className="flex-1 h-9 rounded-lg text-xs font-semibold text-emerald-700 border-emerald-200 bg-emerald-50 hover:bg-emerald-100">
                  <Link to="/sign-up">Tạo tài khoản</Link>
                </Button>
              )}
            </div>
          )}
        </div>
      </CardContent>
    </Card>
  );
}
