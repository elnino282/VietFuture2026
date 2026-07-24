import { useMarketplaceFarms, useMarketplaceProducts } from "@/features/marketplace/hooks";
import { ErrorBoundary } from "@/shared/components/ErrorBoundary";
import { MarketHomeHero } from "./components/home/MarketHomeHero";
import { MarketHomeFeatures } from "./components/home/MarketHomeFeatures";
import { MarketHomeProducts } from "./components/home/MarketHomeProducts";
import { MarketHomeFarms } from "./components/home/MarketHomeFarms";

function SectionError({ title }: { title: string }) {
  return (
    <section className="py-16 lg:py-24">
      <div className="mx-auto max-w-enterprise px-6">
        <h2 className="font-heading text-3xl font-bold tracking-tight text-earth-900 mb-8">{title}</h2>
        <div className="rounded-2xl border border-destructive/20 bg-destructive/5 p-8 text-center text-sm text-destructive">
          Không thể tải dữ liệu cho mục này. Vui lòng thử lại sau.
        </div>
      </div>
    </section>
  );
}

export function MarketHomePage() {
  // We still fetch here JUST to get the hero image if we want to pass it down, 
  // or we could just pass null and let it use the default fallback.
  // Let's pass a static/default hero image to MarketHomeHero or extract from queries if available.
  const productsQuery = useMarketplaceProducts({ page: 0, size: 1, sort: "newest" });
  const farmsQuery = useMarketplaceFarms({ page: 0, size: 1 });
  
  const heroImage =
    farmsQuery.data?.items[0]?.coverImageUrl ??
    productsQuery.data?.items[0]?.imageUrl ??
    undefined;

  return (
    <div className="flex min-h-screen flex-col bg-white font-sans">
      <MarketHomeHero heroImageUrl={heroImage} />
      
      <MarketHomeFeatures />
      
      <ErrorBoundary fallback={<SectionError title="Sản phẩm nổi bật" />}>
        <MarketHomeProducts />
      </ErrorBoundary>
      
      <ErrorBoundary fallback={<SectionError title="Nông trại đối tác" />}>
        <MarketHomeFarms />
      </ErrorBoundary>
    </div>
  );
}
