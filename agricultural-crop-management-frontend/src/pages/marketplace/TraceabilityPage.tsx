import { useMemo, useState } from "react";
import { Search, ShieldCheck } from "lucide-react";
import { useNavigate, useParams } from "react-router-dom";
import { Button, Card, CardContent, Input } from "@/shared/ui";
import { useMarketplaceTraceability } from "@/features/marketplace/hooks";
import { formatDateTime } from "@/features/marketplace/lib/format";

export function TraceabilityPage() {
  const { productId } = useParams<{ productId?: string }>();
  const navigate = useNavigate();

  const parsedProductId = useMemo(() => {
    if (!productId) {
      return null;
    }
    const value = Number(productId);
    if (!Number.isFinite(value) || value <= 0) {
      return null;
    }
    return value;
  }, [productId]);

  const [inputProductId, setInputProductId] = useState(productId ?? "");
  const traceabilityQuery = useMarketplaceTraceability(parsedProductId);

  return (
    <div className="max-w-[1800px] mx-auto px-6 pt-6 space-y-4">
      <Card className="border border-border rounded-xl shadow-sm">
        <CardContent className="px-6 py-4">
          <div className="flex-shrink-0 mb-4">
            <h1 className="text-2xl font-bold text-foreground flex items-center gap-2 leading-tight">
              <ShieldCheck className="w-6 h-6 text-emerald-600" />
              Traceability lookup
            </h1>
            <p className="text-sm text-muted-foreground mt-1">
              Enter product ID to inspect farm-season-lot chain from backend.
            </p>
          </div>
          <div className="flex flex-col gap-3 sm:flex-row">
            <div className="relative flex-1">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" />
              <Input
                placeholder="Example: 201"
                value={inputProductId}
                onChange={(event) => setInputProductId(event.target.value)}
                className="pl-10 rounded-xl border-border focus:border-primary"
              />
            </div>
            <Button
              className="bg-primary hover:bg-primary/90 text-white acm-rounded-sm acm-button-shadow"
              onClick={() => {
                const next = Number(inputProductId);
                if (!Number.isFinite(next) || next <= 0) {
                  return;
                }
                navigate(`/marketplace/traceability/${next}`);
              }}
            >
              Lookup
            </Button>
          </div>
        </CardContent>
      </Card>

      {parsedProductId == null ? (
        <div className="rounded-xl border border-dashed border-border bg-card p-8 text-center text-sm text-muted-foreground">
          Product ID is not selected.
        </div>
      ) : traceabilityQuery.isLoading ? (
        <div className="rounded-xl border border-dashed border-border bg-card p-8 text-center text-sm text-muted-foreground">
          Loading traceability chain...
        </div>
      ) : traceabilityQuery.isError ? (
        <div className="rounded-xl border border-dashed border-destructive/30 bg-card p-8 text-center text-sm text-destructive">
          Failed to load traceability chain.
        </div>
      ) : traceabilityQuery.data ? (
        <Card className="border border-border rounded-xl shadow-sm">
          <CardContent className="space-y-4 p-6">
            <h2 className="text-lg font-semibold text-foreground">
              Result for product #{traceabilityQuery.data.productId}
            </h2>

            {!traceabilityQuery.data.traceable ? (
              <p className="text-sm text-muted-foreground">This product is not traceable.</p>
            ) : (
              <div className="grid gap-3 sm:grid-cols-3">
                <div className="rounded-xl border border-border bg-muted/50 p-3">
                  <p className="text-xs text-muted-foreground">Farm</p>
                  <p className="text-sm font-semibold text-foreground">
                    {traceabilityQuery.data.farm?.name}
                  </p>
                  <p className="text-xs text-muted-foreground">{traceabilityQuery.data.farm?.region}</p>
                </div>

                <div className="rounded-xl border border-border bg-muted/50 p-3">
                  <p className="text-xs text-muted-foreground">Season</p>
                  <p className="text-sm font-semibold text-foreground">
                    {traceabilityQuery.data.season?.name}
                  </p>
                  <p className="text-xs text-muted-foreground">
                    Harvest plan: {traceabilityQuery.data.season?.plannedHarvestDate ?? "N/A"}
                  </p>
                </div>

                <div className="rounded-xl border border-border bg-muted/50 p-3">
                  <p className="text-xs text-muted-foreground">Lot</p>
                  <p className="text-sm font-semibold text-foreground">
                    {traceabilityQuery.data.lot?.lotCode}
                  </p>
                  <p className="text-xs text-muted-foreground">
                    Harvested: {traceabilityQuery.data.lot?.harvestedAt ?? "N/A"}
                  </p>
                </div>
              </div>
            )}

            <p className="text-xs text-muted-foreground">
              Validated at: {formatDateTime(traceabilityQuery.data.validatedAt)}
            </p>
          </CardContent>
        </Card>
      ) : (
        <div className="rounded-xl border border-dashed border-border bg-card p-8 text-center text-sm text-muted-foreground">
          No traceability data found.
        </div>
      )}
    </div>
  );
}
