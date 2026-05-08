import { useState } from "react";
import { MapPin, Search, Wheat } from "lucide-react";
import { Link } from "react-router-dom";
import { Badge, Card, CardContent, Input } from "@/shared/ui";
import { useMarketplaceFarms } from "@/features/marketplace/hooks";

function FarmCardSkeleton() {
  return (
    <div className="rounded-xl border border-border bg-card p-5">
      <div className="mb-3 flex items-center gap-3">
        <div className="h-10 w-10 animate-pulse rounded-lg bg-muted" />
        <div className="flex-1 space-y-1.5">
          <div className="h-4 w-3/4 animate-pulse rounded bg-muted" />
          <div className="h-3 w-1/2 animate-pulse rounded bg-muted" />
        </div>
      </div>
      <div className="flex items-center justify-between">
        <div className="h-5 w-24 animate-pulse rounded-full bg-muted" />
        <div className="h-3 w-20 animate-pulse rounded bg-muted" />
      </div>
    </div>
  );
}

export function FarmListPage() {
  const [search, setSearch] = useState("");

  const farmsQuery = useMarketplaceFarms({
    q: search || undefined,
    page: 0,
    size: 24,
  });
  const farms = farmsQuery.data?.items ?? [];

  return (
    <div className="max-w-[1800px] mx-auto px-6 pt-6 space-y-4">
      <Card className="border border-border rounded-xl shadow-sm">
        <CardContent className="px-6 py-4">
          <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            <div className="flex-shrink-0">
              <h1 className="text-2xl font-bold text-foreground flex items-center gap-2 leading-tight">
                <Wheat className="w-6 h-6 text-emerald-600" />
                Farm list
              </h1>
              <p className="text-sm text-muted-foreground mt-1">
                Browse available farms on the marketplace
              </p>
            </div>
            <div className="relative w-full sm:w-72">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" />
              <Input
                placeholder="Search farms..."
                value={search}
                onChange={(event) => setSearch(event.target.value)}
                className="pl-10 rounded-xl border-border focus:border-primary"
              />
            </div>
          </div>
        </CardContent>
      </Card>

      {farmsQuery.isLoading ? (
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
          {Array.from({ length: 6 }, (_, i) => <FarmCardSkeleton key={i} />)}
        </div>
      ) : farmsQuery.isError ? (
        <div className="rounded-xl border border-dashed border-destructive/30 bg-card p-8 text-center text-sm text-destructive">
          Failed to load farms.
        </div>
      ) : farms.length > 0 ? (
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
          {farms.map((farm) => (
            <Card
              key={farm.id}
              className="overflow-hidden border border-border rounded-xl shadow-sm transition-shadow hover:shadow-md"
            >
              <CardContent className="p-5">
                <div className="mb-4 flex items-center gap-3">
                  <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-emerald-100 text-emerald-700">
                    <Wheat size={20} />
                  </div>
                  <div className="min-w-0 flex-1">
                    <h2 className="truncate text-base font-semibold text-foreground">{farm.name}</h2>
                    {farm.region ? (
                      <div className="mt-0.5 flex items-center gap-1 text-xs text-muted-foreground">
                        <MapPin size={11} className="shrink-0" />
                        <span className="truncate">{farm.region}</span>
                      </div>
                    ) : (
                      <p className="mt-0.5 text-xs text-muted-foreground/60">Unknown region</p>
                    )}
                  </div>
                </div>

                <div className="flex items-center justify-between">
                  <Badge variant="outline" className="text-xs acm-rounded-sm">
                    {farm.productCount} published products
                  </Badge>
                  <Link
                    to={`/marketplace/farms/${farm.id}`}
                    className="text-xs font-medium text-primary hover:underline"
                  >
                    View farm →
                  </Link>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      ) : (
        <div className="rounded-xl border border-dashed border-border bg-card p-8 text-center text-sm text-muted-foreground">
          No farm found.
        </div>
      )}
    </div>
  );
}
