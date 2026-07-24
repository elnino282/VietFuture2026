import React, { useState } from "react";
import { Link } from "react-router-dom";
import { Star, ShieldCheck, ShoppingCart, UserCircle2 } from "lucide-react";
import { getCategoryLabel } from "@/features/marketplace/lib/categoryLabels";
import { formatVnd } from "@/features/marketplace/lib/format";
import { Badge, Button, Card, CardContent, ImagePlaceholder } from "@/shared/ui";
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

  if (hasError || !src) {
    return <ImagePlaceholder />;
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
  const farmerName = product.farmerName ?? product.farmName ?? "Nông dân vô danh";
  const avatarLetter = farmerName.charAt(0).toUpperCase();

  return (
    <Card className="group flex h-full flex-col overflow-hidden border-earth-100 bg-white shadow-sm transition-all duration-500 ease-out hover:-translate-y-1 hover:border-terracotta-200 hover:shadow-[0_8px_30px_rgb(0,0,0,0.04)]">
      <Link to={`/marketplace/products/${product.slug}`} className="relative aspect-[4/3] overflow-hidden bg-muted sm:aspect-square block">
        <ProductImage src={product.imageUrl} alt={product.name} />
        
        {product.traceable && (
          <div className="absolute left-2 top-2 right-2 flex flex-col gap-1 rounded-lg bg-primary/90 p-2 backdrop-blur-md">
             <div className="flex items-center gap-1.5 text-white">
                <ShieldCheck className="h-4 w-4 text-white" />
                <span className="text-xs font-bold leading-none">Có truy xuất nguồn gốc</span>
             </div>
             {product.updatesCount !== undefined && product.updatesCount > 0 && (
               <span className="text-[10px] font-medium text-white/90">Đã cập nhật {product.updatesCount} nhật ký</span>
             )}
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
             <span className="text-[10px] font-bold text-accent uppercase tracking-wider bg-white/90 backdrop-blur rounded px-1.5 py-0.5 leading-tight inline-block mb-1 shadow-sm">Trồng bởi</span>
             <span className="text-xs font-bold text-neutral-800 leading-none truncate max-w-[120px]">{farmerName}</span>
           </div>
        </div>

        <div className="mb-1 text-[11px] font-medium text-muted-foreground">
          {getCategoryLabel(product.category)}
        </div>
        
        <Link
          to={`/marketplace/products/${product.slug}`}
          className="mb-1 line-clamp-2 h-10 text-sm font-bold leading-5 text-earth-900 transition-colors group-hover:text-primary"
        >
          {product.name}
        </Link>
        
        <p className="mb-3 line-clamp-1 text-xs text-muted-foreground flex items-center gap-1">
           {product.region ? product.region : "Khu vực đang cập nhật"}
        </p>

        <div className="mt-auto flex flex-col gap-3">
          <div className="flex items-end justify-between">
            <div>
              <span className="font-heading text-lg font-bold text-price-highlight">
                {formatVnd(product.price)}
              </span>
              <span className="ml-1 text-xs font-normal text-muted-foreground">/{product.unit}</span>
            </div>
            {(product.ratingAverage && product.ratingAverage > 0) ? (
              <span className="flex items-center gap-1 text-xs font-medium text-muted-foreground">
                <Star className="h-3 w-3 fill-rating-star text-rating-star" />
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
                    "flex-1 h-9 rounded-xl text-xs font-semibold shadow-sm transition-all duration-300", 
                    isSoldOut ? "bg-earth-100 text-earth-400" : "bg-primary hover:bg-primary/90 text-white hover:shadow-md hover:-translate-y-0.5"
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
                <Button asChild size="sm" variant="outline" className="flex-1 h-9 rounded-xl text-xs font-semibold text-primary border-primary/20 bg-primary/5 hover:bg-primary/10 transition-all duration-300 hover:shadow-sm">
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
