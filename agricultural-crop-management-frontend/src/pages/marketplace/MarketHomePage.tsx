import { useEffect, useState } from "react";
import { ArrowRight, Leaf, ShieldCheck, Star, Truck } from "lucide-react";
import { Link } from "react-router-dom";
import { cn } from "@/shared/lib";
import { Card, CardContent } from "@/shared/ui";
import { useInView, useMarketplaceFarms, useMarketplaceProducts } from "@/features/marketplace/hooks";
import { formatVnd } from "@/features/marketplace/lib/format";

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
    <div>
      <section className="fb-marketplace fb-hero">
        <svg
          className="fb-blob"
          viewBox="0 0 480 480"
          aria-hidden="true"
          focusable="false"
        >
          <path
            fill="#E1F5EE"
            d="M398,98c45,57,57,154,18,224s-138,114-219,99S52,357,42,277S82,108,162,68s191-27,236,30Z"
          />
        </svg>

        <div className="max-w-[1800px] mx-auto px-6">
          <div className="fb-hero-grid">
            <div className={cn("fb-hero-left fb-stagger", mounted && "fb-mounted")}>
              <span className="fb-chip">
                <Leaf size={14} aria-hidden="true" />
                Nông sản minh bạch
              </span>

              <h1 className="fb-h1">
                Nông Sản Sạch,
                <br />
                <span className="fb-h1-gradient">Rõ Nguồn Gốc</span>
              </h1>

              <p className="fb-subtitle">
                Khám phá nông sản tươi ngon được kết nối trực tiếp với nông trại, mùa vụ và lô thu hoạch thật trong hệ thống hiện tại.
              </p>

              <div className="flex flex-col gap-3 sm:flex-row">
                <Link to="/marketplace/products" className="fb-cta-primary">
                  Mua sắm ngay
                  <ArrowRight size={18} aria-hidden="true" />
                </Link>
                <Link to="/marketplace/traceability" className="fb-cta-ghost">
                  Tìm hiểu truy xuất
                </Link>
              </div>

              <ul className="fb-trust-strip" aria-label="Cam kết">
                <li>✓ 500+ nông trại</li>
                <li>✓ Truy xuất minh bạch</li>
                <li>✓ Giao toàn quốc</li>
              </ul>
            </div>

            <div className="fb-hero-right">
              <div className="fb-hero-image-wrap">
                {heroImage ? (
                  <>
                    <img
                      src={heroImage}
                      alt="Nông trại xanh tươi"
                      referrerPolicy="no-referrer"
                    />
                    <div className="fb-hero-overlay" aria-hidden="true" />
                    <div className="absolute bottom-6 left-3 right-6 z-10 text-white">
                      <div className="mb-2 flex flex-wrap items-center gap-2">
                        <span className="fb-frosted-pill">Có truy xuất</span>
                        <span className="fb-frosted-pill">Kết nối dữ liệu thật</span>
                      </div>
                      <p className="text-lg font-medium text-white/90">
                        Thu hoạch minh bạch, giao dịch trực tiếp từ nông trại địa phương
                      </p>
                    </div>
                  </>
                ) : (
                  <div className="flex h-full min-h-[320px] items-center justify-center bg-emerald-50 text-sm font-medium text-primary">
                    Dữ liệu hình ảnh sẽ hiển thị khi có sản phẩm hoặc nông trại công khai.
                  </div>
                )}
              </div>
            </div>
          </div>
        </div>
      </section>

      <section className="fb-marketplace fb-features-section">
        <div className="max-w-[1800px] mx-auto px-6">
          <div className="grid grid-cols-1 gap-6 md:grid-cols-3">
            <div
              ref={card1Ref}
              className={cn("fb-feature-card fb-reveal", card1InView && "fb-visible")}
            >
              <div className="fb-icon-circle">
                <ShieldCheck size={28} aria-hidden="true" />
              </div>
              <h3 className="fb-card-title">Truy xuất rõ ràng</h3>
              <p className="fb-card-body">
                Xem nông trại, mùa vụ và lô thu hoạch ngay trên từng sản phẩm.
              </p>
            </div>
            <div
              ref={card2Ref}
              className={cn(
                "fb-feature-card fb-reveal fb-reveal--delay-1",
                card2InView && "fb-visible",
              )}
            >
              <div className="fb-icon-circle">
                <Leaf size={28} aria-hidden="true" />
              </div>
              <h3 className="fb-card-title">Sản phẩm minh bạch</h3>
              <p className="fb-card-body">
                Người bán chỉ đăng được sản phẩm gắn với tồn kho thu hoạch hiện có.
              </p>
            </div>
            <div
              ref={card3Ref}
              className={cn(
                "fb-feature-card fb-reveal fb-reveal--delay-2",
                card3InView && "fb-visible",
              )}
            >
              <div className="fb-icon-circle">
                <Truck size={28} aria-hidden="true" />
              </div>
              <h3 className="fb-card-title">Đặt mua trực tiếp</h3>
              <p className="fb-card-body">
                Giữ trải nghiệm thương mại điện tử quen thuộc với giỏ hàng, checkout và đơn hàng thật.
              </p>
            </div>
          </div>
        </div>
      </section>

      <section className="bg-muted/50 py-16">
        <div className="max-w-[1800px] mx-auto px-6">
          <div className="mb-8 flex items-end justify-between">
            <div>
              <h2 className="text-3xl font-bold text-foreground">Sản phẩm nổi bật</h2>
              <p className="mt-2 text-muted-foreground">Nông sản mới lên kệ từ các lô thu hoạch đã được liên kết</p>
            </div>
            <Link to="/marketplace/products" className="flex items-center gap-1 font-medium text-primary hover:underline">
              Xem tất cả <ArrowRight size={16} />
            </Link>
          </div>

          {productsQuery.isLoading ? (
            <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4">
              {Array.from({ length: 4 }, (_, index) => <ProductCardSkeleton key={index} />)}
            </div>
          ) : productsQuery.isError ? (
            <div className="rounded-xl border border-red-200 bg-card p-8 text-center text-sm text-destructive">
              Không thể tải danh sách sản phẩm nổi bật.
            </div>
          ) : featuredProducts.length === 0 ? (
            <div className="rounded-xl border border-dashed border-border bg-card p-8 text-center text-sm text-muted-foreground">
              Chưa có sản phẩm công khai. Vui lòng quay lại sau.
            </div>
          ) : (
            <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4">
              {featuredProducts.map((product) => (
                <Link key={product.id} to={`/marketplace/products/${product.slug}`}>
                  <Card className="group h-full overflow-hidden transition-shadow hover:shadow-md">
                    <div className="relative aspect-square overflow-hidden bg-muted">
                      <img
                        src={product.imageUrl}
                        alt={product.name}
                        className="h-full w-full object-cover transition-transform duration-300 group-hover:scale-105"
                        referrerPolicy="no-referrer"
                      />
                      {product.traceable ? (
                        <span className="absolute left-2 top-2 rounded-full bg-emerald-500 px-3 py-1 text-xs font-semibold text-white">
                          Có truy xuất
                        </span>
                      ) : null}
                    </div>
                    <CardContent className="p-4">
                      <div className="mb-1 text-xs text-muted-foreground">{product.category}</div>
                      <h3 className="mb-2 line-clamp-2 h-10 font-semibold text-foreground">{product.name}</h3>
                      <div className="mt-4 flex items-center justify-between">
                        <span className="text-lg font-bold text-primary">
                          {formatVnd(product.price)}
                          <span className="ml-1 text-sm font-normal text-muted-foreground">/{product.unit}</span>
                        </span>
                        <span className="flex items-center gap-1 text-sm text-muted-foreground">
                          <Star size={14} className="fill-current text-yellow-500" />
                          {product.ratingAverage ? product.ratingAverage.toFixed(1) : "Mới"}
                        </span>
                      </div>
                    </CardContent>
                  </Card>
                </Link>
              ))}
            </div>
          )}
        </div>
      </section>

      <section className="bg-card py-16">
        <div className="max-w-[1800px] mx-auto px-6">
          <h2 className="mb-8 text-center text-3xl font-bold text-foreground">Nông trại đối tác</h2>
          {farmsQuery.isLoading ? (
            <div className="grid grid-cols-1 gap-8 md:grid-cols-2">
              {Array.from({ length: 2 }, (_, index) => <FarmCardSkeleton key={index} />)}
            </div>
          ) : farmsQuery.isError ? (
            <div className="rounded-xl border border-red-200 bg-card p-8 text-center text-sm text-destructive">
              Không thể tải danh sách nông trại.
            </div>
          ) : featuredFarms.length === 0 ? (
            <div className="rounded-xl border border-dashed border-border bg-card p-8 text-center text-sm text-muted-foreground">
              Chưa có nông trại công khai trên marketplace.
            </div>
          ) : (
            <div className="grid grid-cols-1 gap-8 md:grid-cols-2">
              {featuredFarms.map((farm) => (
                <Card key={farm.id} className="overflow-hidden sm:flex">
                  <div className="aspect-video w-full sm:w-1/3 sm:aspect-square">
                    {farm.coverImageUrl ? (
                      <img
                        src={farm.coverImageUrl}
                        alt={farm.name}
                        className="h-full w-full object-cover"
                        referrerPolicy="no-referrer"
                      />
                    ) : (
                      <div className="flex h-full items-center justify-center bg-muted text-xs text-muted-foreground/60">
                        Chưa có ảnh
                      </div>
                    )}
                  </div>
                  <CardContent className="flex flex-1 flex-col justify-center p-6">
                    <h3 className="mb-2 text-xl font-bold">{farm.name}</h3>
                    <p className="mb-4 line-clamp-2 text-sm text-muted-foreground">
                      Nông trại tham gia hệ thống với {farm.productCount} sản phẩm đang được công khai trên sàn.
                    </p>
                    <div className="mb-4 text-sm text-muted-foreground">
                      <span className="font-medium text-foreground">Khu vực:</span> {farm.region ?? "Đang cập nhật"}
                    </div>
                    <Link to={`/marketplace/farms/${farm.id}`} className="mt-auto inline-block font-medium text-primary hover:underline">
                      Xem nông trại →
                    </Link>
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
