import type { ReactNode } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { ArrowLeft, Eye, EyeOff, Pencil } from "lucide-react";
import { Badge, Button, Card, CardContent, CardHeader, CardTitle } from "@/shared/ui";
import type { MarketplaceProductStatus } from "@/shared/api";
import {
  useMarketplaceFarmerProductDetail,
  useMarketplaceUpdateFarmerProductStatusMutation,
} from "@/features/marketplace/hooks";
import { SellerMarketplaceTabs } from "@/features/marketplace/layout";
import { formatDate, formatVnd } from "@/features/marketplace/lib/format";
import {
  getNextSellerProductStatusAction,
  getNextSellerProductStatusLabel,
} from "@/features/marketplace/lib/sellerProductStatus";

function statusVariant(status: MarketplaceProductStatus) {
  switch (status) {
    case "PUBLISHED":
      return "success" as const;
    case "PENDING_REVIEW":
      return "warning" as const;
    case "HIDDEN":
      return "destructive" as const;
    default:
      return "secondary" as const;
  }
}

function statusLabel(status: MarketplaceProductStatus): string {
  switch (status) {
    case "PUBLISHED":
      return "Published";
    case "PENDING_REVIEW":
      return "Pending review";
    case "HIDDEN":
      return "Hidden";
    case "DRAFT":
      return "Draft";
    default:
      return status;
  }
}

function DetailRow({ label, value }: { label: string; value: ReactNode }) {
  return (
    <div className="flex items-start justify-between gap-4 py-3">
      <span className="text-sm text-muted-foreground">{label}</span>
      <span className="text-right text-sm font-medium text-foreground">{value}</span>
    </div>
  );
}

export function SellerProductDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const productId = Number(id ?? 0);

  const productQuery = useMarketplaceFarmerProductDetail(productId);
  const product = productQuery.data;
  const statusMutation = useMarketplaceUpdateFarmerProductStatusMutation(productId);

  async function handleStatusTransition() {
    if (!product) {
      return;
    }

    const nextAction = getNextSellerProductStatusAction(product.status);
    if (!nextAction) {
      return;
    }

    try {
      await statusMutation.mutateAsync(nextAction);
    } catch {
      // Query layer exposes the last known state on refetch.
    }
  }

  if (productQuery.isLoading) {
    return (
      <div className="max-w-[1800px] mx-auto px-6 pt-6 space-y-6">
        <SellerMarketplaceTabs />
        <Card className="border-dashed">
          <CardContent className="p-8 text-sm text-muted-foreground">Loading product detail...</CardContent>
        </Card>
      </div>
    );
  }

  if (productQuery.isError || !product) {
    return (
      <div className="max-w-[1800px] mx-auto px-6 pt-6 space-y-6">
        <SellerMarketplaceTabs />
        <Card>
          <CardContent className="space-y-4 p-8 text-center">
            <h2 className="text-xl font-semibold text-foreground">Product not found</h2>
            <p className="text-sm text-muted-foreground">
              This product does not exist or is not available in your seller account.
            </p>
            <div className="flex justify-center">
              <Button type="button" variant="outline" onClick={() => navigate("/farmer/marketplace-products")}>
                Back to products
              </Button>
            </div>
          </CardContent>
        </Card>
      </div>
    );
  }

  const gallery = product.imageUrls.length > 0 ? product.imageUrls : [product.imageUrl];

  return (
    <div className="min-h-full space-y-6 px-4 py-6 sm:px-6 lg:px-8 lg:py-8">
      <SellerMarketplaceTabs />

      <div className="flex flex-wrap items-center justify-between gap-4">
        <div className="flex items-center gap-3">
          <button
            type="button"
            onClick={() => navigate("/farmer/marketplace-products")}
            className="rounded-full border border-border p-2 text-muted-foreground transition-colors hover:bg-muted hover:text-foreground"
          >
            <ArrowLeft size={18} />
          </button>
          <div>
            <p className="text-sm font-medium text-primary">FarmTrace Seller Portal</p>
            <h1 className="mt-1 text-3xl font-bold text-foreground">Product detail</h1>
          </div>
        </div>

        <div className="flex flex-wrap gap-2">
          {getNextSellerProductStatusAction(product.status) ? (
            <Button
              variant="outline"
              size="sm"
              onClick={handleStatusTransition}
              disabled={statusMutation.isPending}
              className="gap-2"
            >
              {product.status === "PUBLISHED" ? <EyeOff size={14} /> : <Eye size={14} />}
              {statusMutation.isPending ? "Updating..." : getNextSellerProductStatusLabel(product.status)}
            </Button>
          ) : null}
          <Link to={`/farmer/marketplace-products/${product.id}/edit`}>
            <Button size="sm" className="gap-2">
              <Pencil size={14} /> Edit listing
            </Button>
          </Link>
        </div>
      </div>

      <Card className="overflow-hidden border-border shadow-sm">
        <CardHeader className="border-b border-border/50">
          <div className="flex flex-wrap items-start justify-between gap-4">
            <div>
              <CardTitle className="text-2xl">{product.name}</CardTitle>
              <p className="mt-2 text-sm text-muted-foreground">
                Listing #{product.id} • Updated {formatDate(product.updatedAt)}
              </p>
            </div>
            <Badge variant={statusVariant(product.status)}>{statusLabel(product.status)}</Badge>
          </div>
        </CardHeader>
        <CardContent className="space-y-8 p-6">
          <div className="grid gap-8 lg:grid-cols-[minmax(0,1.05fr)_minmax(0,0.95fr)]">
            <div className="space-y-4">
              <div className="overflow-hidden rounded-xl border border-border bg-muted/50">
                {gallery[0] ? (
                  <img
                    src={gallery[0]}
                    alt={product.name}
                    className="h-[360px] w-full object-cover"
                    referrerPolicy="no-referrer"
                  />
                ) : (
                  <div className="flex h-[360px] items-center justify-center text-sm text-muted-foreground">
                    No image available
                  </div>
                )}
              </div>

              {gallery.length > 1 ? (
                <div className="grid grid-cols-3 gap-3">
                  {gallery.slice(0, 3).map((imageUrl, index) => (
                    <div key={`${imageUrl}-${index}`} className="overflow-hidden rounded-xl border border-border bg-muted/50">
                      <img
                        src={imageUrl}
                        alt={`${product.name} ${index + 1}`}
                        className="h-24 w-full object-cover"
                        referrerPolicy="no-referrer"
                      />
                    </div>
                  ))}
                </div>
              ) : null}
            </div>

            <div className="space-y-6">
              <div>
                <h3 className="text-sm font-semibold uppercase tracking-[0.18em] text-muted-foreground">
                  Listing information
                </h3>
                <div className="mt-3 divide-y divide-border/50 rounded-xl bg-muted/50 px-5">
                  <DetailRow label="Category" value={product.category || "-"} />
                  <DetailRow
                    label="Price"
                    value={
                      <span className="font-semibold text-primary">
                        {formatVnd(product.price)} / {product.unit}
                      </span>
                    }
                  />
                  <DetailRow label="Listed quantity" value={`${product.stockQuantity} ${product.unit}`} />
                  <DetailRow label="Available quantity" value={`${product.availableQuantity} ${product.unit}`} />
                  <DetailRow label="Farmer" value={product.farmerDisplayName} />
                  <DetailRow label="Region" value={product.region || "-"} />
                </div>
              </div>

              <div>
                <h3 className="text-sm font-semibold uppercase tracking-[0.18em] text-muted-foreground">
                  Harvest traceability
                </h3>
                <div className="mt-3 divide-y divide-emerald-100 rounded-2xl border border-emerald-100 bg-emerald-50 px-5">
                  <DetailRow label="Farm" value={product.farmName || "Not linked"} />
                  <DetailRow label="Season" value={product.seasonName || "Not linked"} />
                  <DetailRow label="Lot" value={product.traceabilityCode || "Not linked"} />
                  <DetailRow label="Traceable" value={product.traceable ? "Yes" : "No"} />
                </div>
              </div>
            </div>
          </div>

          <div className="border-t border-border/50 pt-6">
            <h3 className="text-sm font-semibold uppercase tracking-[0.18em] text-muted-foreground">
              Description
            </h3>
            <p className="mt-3 whitespace-pre-wrap text-sm leading-7 text-muted-foreground">
              {product.description || product.shortDescription || "No product description provided yet."}
            </p>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
