import { ArrowRight, Box, Store } from "lucide-react";
import { Link } from "react-router-dom";
import { useMarketplaceProducts } from "@/features/marketplace/hooks";
import { MarketplaceProductCard } from "@/features/marketplace/components/MarketplaceProductCard";
import { getGridColsClass } from "@/pages/marketplace/lib/gridLayout";

function ProductCardSkeleton() {
  return (
    <div className="overflow-hidden rounded-2xl border border-earth-100 bg-white shadow-sm transition-shadow">
      <div className="aspect-[4/3] w-full animate-pulse bg-earth-100" />
      <div className="flex flex-col p-5">
        <div className="mb-3 -mt-8 relative z-10">
          <div className="h-10 w-10 animate-pulse rounded-full border-2 border-white bg-earth-200 shadow-sm" />
        </div>
        <div className="h-3 w-16 animate-pulse rounded-md bg-earth-100 mb-2" />
        <div className="h-5 w-3/4 animate-pulse rounded-md bg-earth-200 mb-3" />
        <div className="h-3 w-1/2 animate-pulse rounded-md bg-earth-100 mb-5" />
        <div className="mt-auto flex items-end justify-between">
          <div className="h-6 w-24 animate-pulse rounded-md bg-earth-200" />
          <div className="h-4 w-12 animate-pulse rounded-md bg-earth-100" />
        </div>
      </div>
    </div>
  );
}

export function MarketHomeProducts() {
  const productsQuery = useMarketplaceProducts({ page: 0, size: 4, sort: "newest" });
  const featuredProducts = productsQuery.data?.items ?? [];

  if (productsQuery.isError) {
    throw new Error("Failed to load featured products");
  }

  return (
    <section className="bg-terracotta-50 py-16 lg:py-24 relative z-0 -mt-8 sm:-mt-12 pt-24 sm:pt-28 lg:pt-36">
      <div className="absolute inset-0 -z-10 opacity-[0.04]" 
           style={{ backgroundImage: 'radial-gradient(#574b3f 1px, transparent 1px)', backgroundSize: '16px 16px' }} />
      <div className="mx-auto max-w-enterprise px-6">
        <div className="mb-12 flex flex-col items-start justify-between gap-4 sm:flex-row sm:items-end">
          <div>
            <h2 className="font-heading text-3xl font-bold tracking-tight text-earth-900">Sản phẩm nổi bật</h2>
            <p className="mt-2 text-earth-600">
              Nông sản mới lên kệ từ các lô thu hoạch đã được liên kết
            </p>
          </div>
          <Link
            to="/marketplace/products"
            className="inline-flex items-center gap-1.5 font-semibold text-primary transition-colors hover:text-primary/90"
          >
            Xem tất cả <ArrowRight size={16} />
          </Link>
        </div>

        {productsQuery.isLoading ? (
          <div className={`${getGridColsClass(4, 4)} gap-8`}>
            {Array.from({ length: 4 }, (_, index) => (
              <ProductCardSkeleton key={index} />
            ))}
          </div>
        ) : featuredProducts.length === 0 ? (
          <div className="flex flex-col items-center justify-center rounded-3xl border border-dashed border-earth-300 bg-white px-6 py-16 text-center shadow-sm">
            <div className="mb-6 flex h-20 w-20 items-center justify-center rounded-full bg-orange-50 text-accent ring-8 ring-orange-50/50">
              <Box size={32} />
            </div>
            <h3 className="font-heading text-2xl font-bold text-earth-900 mb-2">Chưa có sản phẩm nào</h3>
            <p className="mb-8 max-w-md text-earth-600">
              Hiện tại các nông trại đang trong quá trình chuẩn bị lô thu hoạch mới. 
              Hãy quay lại sau hoặc trở thành nhà cung cấp đầu tiên của chúng tôi!
            </p>
            <div className="flex flex-col sm:flex-row gap-4">
              <Link 
                to="/marketplace/farms" 
                className="inline-flex items-center justify-center rounded-xl bg-primary px-6 py-3 text-sm font-semibold text-white shadow-sm transition-all hover:bg-primary/90 hover:shadow"
              >
                Khám phá nông trại
              </Link>
              <Link 
                to="/become-seller" 
                className="inline-flex items-center justify-center gap-2 rounded-xl border border-earth-200 bg-white px-6 py-3 text-sm font-semibold text-earth-700 shadow-sm transition-all hover:bg-earth-50 hover:text-earth-900"
              >
                <Store size={18} />
                Đăng ký bán hàng
              </Link>
            </div>
          </div>
        ) : (
          <div className={`${getGridColsClass(featuredProducts.length, 4)} gap-8`}>
            {featuredProducts.map((product) => (
              <MarketplaceProductCard
                key={product.id}
                product={product}
                hideAddToCart={true}
              />
            ))}
          </div>
        )}
      </div>
    </section>
  );
}
