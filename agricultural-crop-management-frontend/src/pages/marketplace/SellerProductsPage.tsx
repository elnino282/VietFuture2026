import { useMemo, useState } from "react";
import { Edit, Eye, EyeOff, Package, Plus, Search } from "lucide-react";
import { Link } from "react-router-dom";
import type { MarketplaceProductStatus, MarketplaceProductSummary } from "@/shared/api";
import {
  Badge,
  Button,
  Card,
  CardContent,
  Input,
  PageContainer,
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/shared/ui";
import { useI18n } from "@/hooks/useI18n";
import {
  useMarketplaceFarmerProducts,
  useMarketplaceUpdateFarmerProductStatusMutation,
} from "@/features/marketplace/hooks";
import { SellerMarketplaceTabs } from "@/features/marketplace/layout";
import { formatDateTime, formatVnd } from "@/features/marketplace/lib/format";
import {
  getNextSellerProductStatusAction,
  getNextSellerProductStatusLabel,
} from "@/features/marketplace/lib/sellerProductStatus";

function statusVariant(status: MarketplaceProductStatus) {
  switch (status) {
    case "PUBLISHED":
      return "default" as const;
    case "PENDING_REVIEW":
      return "secondary" as const;
    case "HIDDEN":
      return "destructive" as const;
    default:
      return "outline" as const;
  }
}

function useStatusLabel() {
  const { t } = useI18n();
  return (status: MarketplaceProductStatus) => {
    switch (status) {
      case "DRAFT":
        return t("marketplaceSeller.status.draft");
      case "PENDING_REVIEW":
        return t("marketplaceSeller.status.pendingReview");
      case "PUBLISHED":
        return t("marketplaceSeller.status.published");
      case "HIDDEN":
        return t("marketplaceSeller.status.hidden");
      default:
        return status;
    }
  };
}

function moderationReason(product: MarketplaceProductSummary) {
  return product.rejectionReason ?? product.statusReason ?? null;
}

function ProductActions({ product }: { product: MarketplaceProductSummary }) {
  const mutation = useMarketplaceUpdateFarmerProductStatusMutation(product.id);
  const nextAction = getNextSellerProductStatusAction(product.status);

  return (
    <div className="flex items-center justify-end gap-2">
      <Button
        variant="ghost"
        size="sm"
        className="text-muted-foreground hover:bg-muted hover:text-foreground"
        disabled={mutation.isPending || !nextAction}
        title={getNextSellerProductStatusLabel(product.status)}
        onClick={() => {
          if (nextAction) {
            mutation.mutate(nextAction);
          }
        }}
      >
        {product.status === "PUBLISHED" ? <EyeOff size={16} /> : <Eye size={16} />}
      </Button>
      <Link to={`/farmer/marketplace-products/${product.id}/edit`}>
        <Button variant="ghost" size="sm" className="text-primary hover:bg-primary/10 hover:text-primary">
          <Edit size={16} />
        </Button>
      </Link>
    </div>
  );
}

export function SellerProductsPage() {
  const { t } = useI18n();
  const statusLabel = useStatusLabel();
  const [search, setSearch] = useState("");
  const [status, setStatus] = useState<"ALL" | MarketplaceProductStatus>("ALL");

  const query = useMemo(
    () => ({
      page: 0,
      size: 100,
      q: search.trim() || undefined,
      status: status === "ALL" ? undefined : status,
    }),
    [search, status],
  );

  const productsQuery = useMarketplaceFarmerProducts(query);
  const products = productsQuery.data?.items ?? [];

  return (
    <PageContainer variant="wide">
      <div className="space-y-6">
      <SellerMarketplaceTabs />

      <Card variant="page-header">
        <CardContent className="px-6 py-4">
          {/* Header Row: Title + Actions */}
          <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
            {/* Title Section */}
            <div className="flex-shrink-0">
              <h1 className="text-2xl font-bold text-foreground flex items-center gap-2 leading-tight">
                <Package className="w-6 h-6 text-primary" />
                {t("marketplaceSeller.products.title")}
              </h1>
              <p className="text-sm text-muted-foreground mt-1">
                {t("marketplaceSeller.products.subtitle")}
              </p>
            </div>

            {/* Primary Action */}
            <div className="flex items-center gap-3 flex-shrink-0">
              <Link to="/farmer/marketplace-products/new">
                <Button className="acm-rounded-sm acm-button-shadow">
                  <Plus className="w-4 h-4 mr-2" />
                  {t("marketplaceSeller.products.addProduct")}
                </Button>
              </Link>
            </div>
          </div>
        </CardContent>
      </Card>

      <Card variant="filter" className="rounded-2xl">
        <CardContent className="p-4">
        <div className="flex flex-col gap-4 md:flex-row">
          <div className="relative flex-1">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground" size={18} />
            <Input
              type="text"
              placeholder={t("marketplaceSeller.products.searchPlaceholder")}
              className="h-11 rounded-xl border-border pl-10"
              value={search}
              onChange={(event) => setSearch(event.target.value)}
            />
          </div>
          <Select value={status} onValueChange={(value) => setStatus(value as "ALL" | MarketplaceProductStatus)}>
            <SelectTrigger className="h-11 min-w-[220px] rounded-xl border-border">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="ALL">{t("marketplaceSeller.filters.allStatuses")}</SelectItem>
              <SelectItem value="DRAFT">{t("marketplaceSeller.status.draft")}</SelectItem>
              <SelectItem value="PENDING_REVIEW">{t("marketplaceSeller.status.pendingReview")}</SelectItem>
              <SelectItem value="PUBLISHED">{t("marketplaceSeller.status.published")}</SelectItem>
              <SelectItem value="HIDDEN">{t("marketplaceSeller.status.hidden")}</SelectItem>
            </SelectContent>
          </Select>
        </div>
        </CardContent>
      </Card>

      <Card variant="content" className="overflow-hidden rounded-2xl">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead className="pl-4">{t("marketplaceSeller.table.product")}</TableHead>
              <TableHead>{t("marketplaceSeller.table.category")}</TableHead>
              <TableHead>{t("marketplaceSeller.table.price")}</TableHead>
              <TableHead>{t("marketplaceSeller.table.stock")}</TableHead>
              <TableHead>{t("marketplaceSeller.table.status")}</TableHead>
              <TableHead>{t("marketplaceSeller.table.updated")}</TableHead>
              <TableHead className="text-right pr-4">{t("marketplaceSeller.table.actions")}</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {products.map((product) => (
              <TableRow key={product.id}>
                <TableCell className="pl-4">
                  <div className="flex items-center gap-3">
                    <img
                      src={product.imageUrl}
                      alt={product.name}
                      className="h-11 w-11 rounded-lg bg-muted object-cover"
                      referrerPolicy="no-referrer"
                    />
                    <div className="min-w-0">
                      <Link
                        to={`/farmer/marketplace-products/${product.id}`}
                        className="block truncate font-medium text-foreground hover:text-primary"
                      >
                        {product.name}
                      </Link>
                      <p className="truncate text-xs text-muted-foreground">{product.shortDescription}</p>
                      {moderationReason(product) ? (
                        <p className="mt-1 text-xs font-medium text-destructive">
                          {t("marketplaceSeller.table.adminReason")}: {moderationReason(product)}
                        </p>
                      ) : null}
                    </div>
                  </div>
                </TableCell>
                <TableCell className="text-muted-foreground">{product.category || "-"}</TableCell>
                <TableCell className="font-medium text-foreground">
                  {formatVnd(product.price)}/{product.unit}
                </TableCell>
                <TableCell className="text-sm text-muted-foreground">
                  <div>{product.stockQuantity} {product.unit}</div>
                  <div className="text-xs text-muted-foreground/70">
                    {t("marketplaceSeller.table.available")}: {product.availableQuantity} {product.unit}
                  </div>
                </TableCell>
                <TableCell>
                  <Badge variant={statusVariant(product.status)}>{statusLabel(product.status)}</Badge>
                </TableCell>
                <TableCell className="text-sm text-muted-foreground">{formatDateTime(product.updatedAt)}</TableCell>
                <TableCell className="text-right pr-4">
                  <ProductActions product={product} />
                </TableCell>
              </TableRow>
            ))}
            {productsQuery.isLoading ? (
              <TableRow>
                <TableCell colSpan={7} className="p-8 text-center text-sm text-muted-foreground">
                  {t("marketplaceSeller.products.loading")}
                </TableCell>
              </TableRow>
            ) : null}
            {!productsQuery.isLoading && !productsQuery.isError && products.length === 0 ? (
              <TableRow>
                <TableCell colSpan={7} className="p-8 text-center text-sm text-muted-foreground">
                  {t("marketplaceSeller.products.emptyFiltered")}
                </TableCell>
              </TableRow>
            ) : null}
            {productsQuery.isError ? (
              <TableRow>
                <TableCell colSpan={7} className="p-8 text-center text-sm text-destructive">
                  {t("marketplaceSeller.products.loadError")}
                </TableCell>
              </TableRow>
            ) : null}
          </TableBody>
        </Table>
      </Card>
      </div>
    </PageContainer>
  );
}
