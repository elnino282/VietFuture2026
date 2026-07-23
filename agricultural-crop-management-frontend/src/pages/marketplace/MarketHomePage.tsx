import { useEffect, useState } from "react";
import { ArrowRight, Leaf, ShieldCheck, Star, Truck } from "lucide-react";
import { Link } from "react-router-dom";
import { cn } from "@/shared/lib";
import { Card, CardContent } from "@/shared/ui";
import { useInView, useMarketplaceFarms, useMarketplaceProducts } from "@/features/marketplace/hooks";
import { formatVnd } from "@/features/marketplace/lib/format";
import { MarketplaceProductCard } from "@/features/marketplace/components/MarketplaceProductCard";

function ProductCardSkeleton() {
  return (
    <div className="overflow-hidden rounded-xl border border-border bg-card shadow-sm">
      <div className="aspect-square animate-pulse bg-muted" />
      <div className="space-y-2 p-4">
        <div className="h-3 w-16 animate-pulse rounded bg-muted" />
        <div className="h-5 w-3/4 animate-pulse rounded bg-muted" />
        <div className="h-4 w-1/2 animate-pulse rounded bg-muted" />
      </div>
    </div>
  );
}

function FarmCardSkeleton() {
  return (
    <Card className="overflow-hidden sm:flex">
      <div className="aspect-video animate-pulse bg-muted sm:w-1/3 sm:aspect-square" />
      <CardContent className="flex-1 p-6">
        <div className="space-y-2">
          <div className="h-5 w-40 animate-pulse rounded bg-muted" />
          <div className="h-4 w-full animate-pulse rounded bg-muted" />
          <div className="h-4 w-28 animate-pulse rounded bg-muted" />
        </div>
      </CardContent>
    </Card>
  );
}

export function MarketHomePage() {
  const productsQuery = useMarketplaceProducts({ page: 0, size: 4, sort: "newest" });
  const farmsQuery = useMarketplaceFarms({ page: 0, size: 2 });

  const featuredProducts = productsQuery.data?.items ?? [];
  const featuredFarms = farmsQuery.data?.items ?? [];
  const heroImage =
    featuredFarms[0]?.coverImageUrl ??
    featuredProducts[0]?.imageUrl ??
    null;

  const [mounted, setMounted] = useState(false);
  useEffect(() => {
    setMounted(true);
  }, []);

  const [card1Ref, card1InView] = useInView<HTMLDivElement>();
  const [card2Ref, card2InView] = useInView<HTMLDivElement>();
  const [card3Ref, card3InView] = useInView<HTMLDivElement>();

  return (
    <div className="flex min-h-screen flex-col bg-background">
      {/* Hero Section */}
      <section className="relative overflow-hidden bg-emerald-950 px-6 py-24 sm:py-32 lg:px-8">
        <div className="absolute inset-0 -z-10 h-full w-full">
          <img
            src="/background.png"
            alt=""
            className="h-full w-full object-cover opacity-[0.15]"
          />
          <div className="absolute inset-0 bg-gradient-to-r from-emerald-950 via-emerald-950/90 to-emerald-900/40" />
        </div>

        <div className="mx-auto max-w-[1800px]">
          <div className="grid grid-cols-1 gap-x-12 gap-y-16 lg:grid-cols-2 lg:items-center">
            <div
              className={cn(
                "max-w-2xl transition-all duration-1000 ease-out",
                mounted ? "translate-y-0 opacity-100" : "translate-y-8 opacity-0",
              )}
            >
              <div className="mb-6 inline-flex items-center gap-2 rounded-full border border-emerald-500/30 bg-emerald-500/10 px-3 py-1 text-sm font-medium text-emerald-300">
                <Leaf size={16} aria-hidden="true" />
                Nông sản minh bạch
              </div>

              <h1 className="text-4xl font-bold tracking-tight text-white sm:text-6xl">
                Nông Sản Sạch,
                <br />
                Rõ Nguồn Gốc
              </h1>

              <p className="mt-6 text-lg leading-8 text-emerald-100/90">
                Khám phá nông sản tươi ngon được kết nối trực tiếp với nông trại, mùa vụ và lô thu hoạch thật trong hệ thống hiện tại.
              </p>

              <div className="mt-10 flex items-center gap-x-6">
                <Link
                  to="/marketplace/products"
                  className="inline-flex items-center justify-center rounded-xl bg-emerald-500 px-6 py-3.5 text-sm font-semibold text-white shadow-sm transition hover:bg-emerald-400 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-emerald-400"
                >
                  Mua sắm ngay
                  <ArrowRight size={18} className="ml-2" aria-hidden="true" />
                </Link>
              </div>

              <ul className="mt-10 flex flex-wrap gap-6 text-sm font-medium text-emerald-200/80">
                <li className="flex items-center gap-2">
                  <ShieldCheck size={16} className="text-emerald-400" aria-hidden="true" />
                  500+ nông trại
                </li>
                <li className="flex items-center gap-2">
                  <ShieldCheck size={16} className="text-emerald-400" aria-hidden="true" />
                  Truy xuất minh bạch
                </li>
                <li className="flex items-center gap-2">
                  <ShieldCheck size={16} className="text-emerald-400" aria-hidden="true" />
                  Giao toàn quốc
                </li>
              </ul>
            </div>

            <div className="relative lg:ml-auto lg:w-full lg:max-w-xl">
              {heroImage ? (
                <div className="relative aspect-[4/3] w-full overflow-hidden rounded-2xl shadow-2xl ring-1 ring-white/10">
                  <img
                    src={heroImage}
                    alt="Nông trại xanh tươi"
                    className="h-full w-full object-cover"
                    referrerPolicy="no-referrer"
                  />
                  <div className="absolute inset-0 bg-gradient-to-t from-black/80 via-black/20 to-transparent" />
                  <div className="absolute bottom-0 left-0 right-0 p-6 sm:p-8">
                    <div className="mb-4 flex flex-wrap gap-2">
                      <span className="rounded-full bg-white/20 px-3 py-1 text-xs font-medium text-white backdrop-blur-md">
                        Có truy xuất
                      </span>
                      <span className="rounded-full bg-white/20 px-3 py-1 text-xs font-medium text-white backdrop-blur-md">
                        Kết nối dữ liệu thật
                      </span>
                    </div>
                    <p className="text-base font-medium text-white/95 sm:text-lg">
                      Thu hoạch minh bạch, giao dịch trực tiếp từ nông trại địa phương
                    </p>
                  </div>
                </div>
              ) : (
                <div className="flex aspect-[4/3] w-full items-center justify-center rounded-2xl bg-emerald-900/50 p-8 text-center text-sm font-medium text-emerald-200/60 ring-1 ring-white/10">
                  Dữ liệu hình ảnh sẽ hiển thị khi có sản phẩm hoặc nông trại công khai.
                </div>
              )}
            </div>
          </div>
        </div>
      </section>

      {/* Features Section */}
      <section className="bg-background py-24 sm:py-32">
        <div className="mx-auto max-w-[1800px] px-6">
          <div className="grid grid-cols-1 gap-12 sm:grid-cols-2 lg:grid-cols-3">
            <div
              ref={card1Ref}
              className={cn(
                "transition-all duration-700 ease-out",
                card1InView ? "translate-y-0 opacity-100" : "translate-y-8 opacity-0",
              )}
            >
              <div className="mb-6 flex h-12 w-12 items-center justify-center rounded-xl bg-emerald-50 text-emerald-600 ring-1 ring-emerald-500/20">
                <ShieldCheck size={24} aria-hidden="true" />
              </div>
              <h3 className="mb-3 text-lg font-semibold text-foreground">Truy xuất rõ ràng</h3>
              <p className="text-muted-foreground">
                Xem nông trại, mùa vụ và lô thu hoạch ngay trên từng sản phẩm.
              </p>
            </div>
            
            <div
              ref={card2Ref}
              className={cn(
                "transition-all duration-700 ease-out delay-150",
                card2InView ? "translate-y-0 opacity-100" : "translate-y-8 opacity-0",
              )}
            >
              <div className="mb-6 flex h-12 w-12 items-center justify-center rounded-xl bg-emerald-50 text-emerald-600 ring-1 ring-emerald-500/20">
                <Leaf size={24} aria-hidden="true" />
              </div>
              <h3 className="mb-3 text-lg font-semibold text-foreground">Sản phẩm minh bạch</h3>
              <p className="text-muted-foreground">
                Người bán chỉ đăng được sản phẩm gắn với tồn kho thu hoạch hiện có.
              </p>
            </div>

            <div
              ref={card3Ref}
              className={cn(
                "transition-all duration-700 ease-out delay-300",
                card3InView ? "translate-y-0 opacity-100" : "translate-y-8 opacity-0",
              )}
            >
              <div className="mb-6 flex h-12 w-12 items-center justify-center rounded-xl bg-emerald-50 text-emerald-600 ring-1 ring-emerald-500/20">
                <Truck size={24} aria-hidden="true" />
              </div>
              <h3 className="mb-3 text-lg font-semibold text-foreground">Đặt mua trực tiếp</h3>
              <p className="text-muted-foreground">
                Giữ trải nghiệm thương mại điện tử quen thuộc với giỏ hàng, checkout và đơn hàng thật.
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* Featured Products */}
      <section className="bg-muted/30 py-24 sm:py-32">
        <div className="mx-auto max-w-[1800px] px-6">
          <div className="mb-12 flex flex-col items-start justify-between gap-4 sm:flex-row sm:items-end">
            <div>
              <h2 className="text-3xl font-bold tracking-tight text-foreground">Sản phẩm nổi bật</h2>
              <p className="mt-2 text-muted-foreground">
                Nông sản mới lên kệ từ các lô thu hoạch đã được liên kết
              </p>
            </div>
            <Link
              to="/marketplace/products"
              className="inline-flex items-center gap-1.5 font-medium text-emerald-600 hover:text-emerald-700 hover:underline"
            >
              Xem tất cả <ArrowRight size={16} />
            </Link>
          </div>

          {productsQuery.isLoading ? (
            <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-4">
              {Array.from({ length: 4 }, (_, index) => <ProductCardSkeleton key={index} />)}
            </div>
          ) : productsQuery.isError ? (
            <div className="rounded-2xl border border-destructive/20 bg-destructive/5 p-8 text-center text-sm text-destructive">
              Không thể tải danh sách sản phẩm nổi bật.
            </div>
          ) : featuredProducts.length === 0 ? (
            <div className="rounded-2xl border border-dashed border-border bg-card p-12 text-center">
              <p className="text-sm text-muted-foreground">Chưa có sản phẩm công khai. Vui lòng quay lại sau.</p>
            </div>
          ) : (
            <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-4">
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

      {/* Partner Farms */}
      <section className="bg-background py-24 sm:py-32">
        <div className="mx-auto max-w-[1800px] px-6">
          <div className="mb-12 text-center">
            <h2 className="text-3xl font-bold tracking-tight text-foreground">Nông trại đối tác</h2>
            <p className="mt-4 text-muted-foreground">Các hợp tác xã và nông trại tham gia nền tảng</p>
          </div>

          {farmsQuery.isLoading ? (
            <div className="grid grid-cols-1 gap-8 lg:grid-cols-2">
              {Array.from({ length: 2 }, (_, index) => <FarmCardSkeleton key={index} />)}
            </div>
          ) : farmsQuery.isError ? (
            <div className="mx-auto max-w-2xl rounded-2xl border border-destructive/20 bg-destructive/5 p-8 text-center text-sm text-destructive">
              Không thể tải danh sách nông trại.
            </div>
          ) : featuredFarms.length === 0 ? (
            <div className="mx-auto max-w-2xl rounded-2xl border border-dashed border-border bg-card p-12 text-center">
              <p className="text-sm text-muted-foreground">Chưa có nông trại công khai trên marketplace.</p>
            </div>
          ) : (
            <div className="grid grid-cols-1 gap-8 lg:grid-cols-2">
              {featuredFarms.map((farm) => (
                <Card key={farm.id} className="overflow-hidden border-border/50 shadow-sm transition-shadow hover:shadow-md sm:flex">
                  <div className="aspect-video w-full shrink-0 sm:w-2/5 sm:aspect-auto">
                    {farm.coverImageUrl ? (
                      <img
                        src={farm.coverImageUrl}
                        alt={farm.name}
                        className="h-full w-full object-cover"
                        referrerPolicy="no-referrer"
                      />
                    ) : (
                      <div className="flex h-full items-center justify-center bg-muted">
                        <span className="text-sm text-muted-foreground">Chưa có ảnh</span>
                      </div>
                    )}
                  </div>
                  <CardContent className="flex flex-1 flex-col justify-center p-6 sm:p-8">
                    <div className="flex items-center gap-2 mb-2">
                      <h3 className="text-xl font-bold tracking-tight text-foreground">{farm.name}</h3>
                      {farm.hasTraceableProducts && (
                        <span className="inline-flex items-center gap-1 bg-emerald-50 text-emerald-700 px-2 py-0.5 rounded text-xs font-semibold border border-emerald-100">
                          <ShieldCheck className="w-3 h-3" />
                          Cam kết minh bạch
                        </span>
                      )}
                    </div>
                    <p className="mb-6 line-clamp-2 text-sm leading-relaxed text-muted-foreground">
                      Nông trại tham gia hệ thống với {farm.productCount} sản phẩm đang được công khai trên sàn.
                    </p>
                    <div className="mt-auto flex items-center justify-between text-sm text-muted-foreground">
                      <div>
                        <span className="mr-2 font-medium text-foreground">Khu vực:</span> 
                        {farm.region ?? "Đang cập nhật"}
                      </div>
                      <span className="inline-flex items-center gap-1 text-blue-600 bg-blue-50 px-2 py-1 rounded text-xs font-medium">
                        Cập nhật liên tục
                      </span>
                    </div>
                  </CardContent>
                </Card>
              ))}
            </div>
          )}
        </div>
      </section>
    </div>
  );
}
