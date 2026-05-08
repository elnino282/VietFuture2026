import { useMemo } from "react";
import { ArrowLeft, MapPin, User, Wheat } from "lucide-react";
import { Link, useParams } from "react-router-dom";
import { Badge, Card, CardContent } from "@/shared/ui";
import { useMarketplaceFarmDetail, useMarketplaceProducts } from "@/features/marketplace/hooks";
import { formatVnd } from "@/features/marketplace/lib/format";

export function FarmDetailPage() {
  const { id } = useParams<{ id: string }>();
  const farmId = Number(id);

  const farmQuery = useMarketplaceFarmDetail(farmId);
  const productsQuery = useMarketplaceProducts({ page: 0, size: 100 });

  const farmProducts = useMemo(
    () => (productsQuery.data?.items ?? []).filter((product) => product.farmId === farmId),
    [productsQuery.data, farmId],
  );

  if (farmQuery.isLoading) {
    return (
      <div className="max-w-[1800px] mx-auto px-6 pt-6">
        <div className="rounded-xl border border-dashed border-border bg-card p-8 text-center text-sm text-muted-foreground">
          Loading farm detail...
        </div>
      </div>
    );
  }

  if (farmQuery.isError || !farmQuery.data) {
    return (
      <div className="max-w-[1800px] mx-auto px-6 pt-6">
        <div className="space-y-3 rounded-xl border border-dashed border-destructive/30 bg-card p-8 text-center text-sm text-destructive">
          <p>Farm not found.</p>
          <Link to="/marketplace/farms" className="text-primary hover:underline">
            Back to farm list
          </Link>
        </div>
      </div>
    );
  }

  const farm = farmQuery.data;

  return (
    <div className="max-w-[1800px] mx-auto px-6 pt-6 space-y-4">
      <Link
        to="/marketplace/farms"
        className="inline-flex items-center gap-1 text-sm text-primary hover:underline"
      >
        <ArrowLeft size={15} /> Back to farm list
      </Link>

      {/* Farm header banner */}
      <div className="overflow-hidden rounded-xl bg-gradient-to-r from-emerald-600 to-lime-500 p-8 text-white shadow-sm">
        <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
          <div className="flex items-center gap-4">
            <div className="flex h-16 w-16 shrink-0 items-center justify-center rounded-2xl bg-white/20 backdrop-blur">
              <Wheat size={32} className="text-white" />
            </div>
            <div>
              <h1 className="text-2xl font-bold tracking-tight">{farm.name}</h1>
              {farm.region ? (
                <div className="mt-1 flex items-center gap-1.5 text-emerald-100">
                  <MapPin size={14} />
                  <span className="text-sm">{farm.region}</span>
                </div>
              ) : null}
            </div>
          </div>
          <div className="flex flex-wrap gap-2">
            {farm.region ? (
              <span className="inline-flex items-center rounded-full bg-white/20 px-3 py-1 text-sm font-medium text-white backdrop-blur">
                {farm.region}
              </span>
            ) : null}
            <span className="inline-flex items-center rounded-full bg-white/20 px-3 py-1 text-sm font-medium text-white backdrop-blur">
              {farmProducts.length} products
            </span>
          </div>
        </div>
      </div>

      {/* Farm info */}
      <Card className="border border-border rounded-xl shadow-sm">
        <CardContent className="p-6">
          <div className="grid gap-6 sm:grid-cols-3">
            <div className="sm:col-span-2">
              <h2 className="mb-2 text-sm font-semibold text-foreground">About this farm</h2>
              <p className="text-sm leading-relaxed text-muted-foreground">
                {farm.description ?? "No description provided."}
              </p>
            </div>
            <div className="space-y-3">
              {farm.ownerDisplayName ? (
                <div className="flex items-center gap-2 text-sm text-muted-foreground">
                  <User size={15} className="shrink-0 text-muted-foreground/60" />
                  <span>{farm.ownerDisplayName}</span>
                </div>
              ) : null}
              {farm.address ? (
                <div className="flex items-start gap-2 text-sm text-muted-foreground">
                  <MapPin size={15} className="mt-0.5 shrink-0 text-muted-foreground/60" />
                  <span>{farm.address}</span>
                </div>
              ) : null}
              {farm.region ? (
                <Badge variant="outline" className="w-fit acm-rounded-sm">
                  {farm.region}
                </Badge>
              ) : null}
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Products */}
      <section className="space-y-3">
        <h2 className="text-xl font-semibold text-foreground">Published products</h2>

        {productsQuery.isLoading ? (
          <p className="text-sm text-muted-foreground">Loading products...</p>
        ) : productsQuery.isError ? (
          <p className="text-sm text-destructive">Failed to load products.</p>
        ) : farmProducts.length > 0 ? (
          <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
            {farmProducts.map((product) => (
              <Card key={product.id} className="group overflow-hidden border border-border rounded-xl shadow-sm transition-shadow hover:shadow-md">
                <div className="overflow-hidden">
                  <img
                    src={product.imageUrl}
                    alt={product.name}
                    className="h-44 w-full object-cover transition-transform duration-300 group-hover:scale-105"
                    referrerPolicy="no-referrer"
                  />
                </div>
                <CardContent className="space-y-2 p-4">
                  <h3 className="line-clamp-2 min-h-10 text-sm font-semibold text-foreground">
                    {product.name}
                  </h3>
                  <p className="text-sm font-medium text-primary">{formatVnd(product.price)}</p>
                  <Link
                    to={`/marketplace/products/${product.slug}`}
                    className="text-xs text-primary hover:underline"
                  >
                    View product →
                  </Link>
                </CardContent>
              </Card>
            ))}
          </div>
        ) : (
          <div className="rounded-xl border border-dashed border-border bg-card p-8 text-center text-sm text-muted-foreground">
            This farm has no published product.
          </div>
        )}
      </section>
    </div>
  );
}
