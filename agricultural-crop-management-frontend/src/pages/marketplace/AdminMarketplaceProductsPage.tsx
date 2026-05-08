import { useState } from "react";
import { Check, RotateCcw, Search, X } from "lucide-react";
import { toast } from "sonner";
import type { MarketplaceProductStatus } from "@/shared/api";
import { Badge, Button, Input } from "@/shared/ui";
import {
  useMarketplaceAdminProducts,
  useMarketplaceUpdateAdminProductStatusMutation,
} from "@/features/marketplace/hooks";
import { formatVnd } from "@/features/marketplace/lib/format";
import { RejectWithReasonModal, PaginationControls } from "@/features/marketplace/components";

const statusFilters: Array<{ value: "ALL" | MarketplaceProductStatus; label: string }> = [
  { value: "ALL", label: "All" },
  { value: "DRAFT", label: "Draft" },
  { value: "PENDING_REVIEW", label: "Pending review" },
  { value: "PUBLISHED", label: "Published" },
  { value: "HIDDEN", label: "Hidden" },
];

function statusVariant(status: MarketplaceProductStatus) {
  if (status === "PUBLISHED") return "success" as const;
  if (status === "PENDING_REVIEW") return "warning" as const;
  if (status === "HIDDEN") return "destructive" as const;
  return "secondary" as const;
}

function statusLabel(status: MarketplaceProductStatus) {
  switch (status) {
    case "DRAFT":
      return "Draft";
    case "PENDING_REVIEW":
      return "Pending review";
    case "PUBLISHED":
      return "Published";
    case "HIDDEN":
      return "Hidden";
    default:
      return status;
  }
}

function ModerationActions({
  productId,
  currentStatus,
  onOpenRejectModal,
}: {
  productId: number;
  currentStatus: MarketplaceProductStatus;
  onOpenRejectModal: (productId: number, targetStatus: MarketplaceProductStatus) => void;
}) {
  const mutation = useMarketplaceUpdateAdminProductStatusMutation(productId);

  const handleApprove = (status: MarketplaceProductStatus) => {
    mutation.mutate({ status });
  };

  const actions: Array<{
    status: MarketplaceProductStatus;
    label: string;
    icon: typeof Check;
    className: string;
    requiresReason: boolean;
  }> =
    currentStatus === "PENDING_REVIEW"
      ? [
          {
            status: "PUBLISHED",
            label: "Approve",
            icon: Check,
            className: "text-primary hover:bg-emerald-50 hover:text-primary",
            requiresReason: false,
          },
          {
            status: "HIDDEN",
            label: "Hide",
            icon: X,
            className: "text-destructive hover:bg-red-50 hover:text-red-700",
            requiresReason: true,
          },
        ]
      : currentStatus === "PUBLISHED"
        ? [
            {
              status: "HIDDEN",
              label: "Hide",
              icon: X,
              className: "text-destructive hover:bg-red-50 hover:text-red-700",
              requiresReason: true,
            },
          ]
        : currentStatus === "HIDDEN"
          ? [
              {
                status: "PUBLISHED",
                label: "Publish",
                icon: Check,
                className: "text-primary hover:bg-emerald-50 hover:text-primary",
                requiresReason: false,
              },
              {
                status: "PENDING_REVIEW",
                label: "Return to review",
                icon: RotateCcw,
                className: "text-muted-foreground hover:bg-muted hover:text-foreground",
                requiresReason: false,
              },
            ]
          : [
              {
                status: "PENDING_REVIEW",
                label: "Send to review",
                icon: RotateCcw,
                className: "text-muted-foreground hover:bg-muted hover:text-foreground",
                requiresReason: false,
              },
              {
                status: "HIDDEN",
                label: "Hide",
                icon: X,
                className: "text-destructive hover:bg-red-50 hover:text-red-700",
                requiresReason: true,
              },
            ];

  return (
    <div className="flex flex-wrap justify-end gap-1">
      {actions.map((action) => {
        const Icon = action.icon;

        return (
          <Button
            key={action.status}
            type="button"
            variant="ghost"
            size="sm"
            className={action.className}
            disabled={mutation.isPending}
            onClick={() => {
              if (action.requiresReason) {
                onOpenRejectModal(productId, action.status);
              } else {
                handleApprove(action.status);
              }
            }}
          >
            <Icon size={14} />
            <span className="ml-1">{action.label}</span>
          </Button>
        );
      })}
    </div>
  );
}

export function AdminMarketplaceProductsPage() {
  const [status, setStatus] = useState<"ALL" | MarketplaceProductStatus>("ALL");
  const [search, setSearch] = useState("");
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(25);
  const [rejectModalState, setRejectModalState] = useState<{
    isOpen: boolean;
    productId: number | null;
    targetStatus: MarketplaceProductStatus | null;
  }>({ isOpen: false, productId: null, targetStatus: null });

  const productsQuery = useMarketplaceAdminProducts({
    page,
    size: pageSize,
    q: search.trim() || undefined,
    status: status === "ALL" ? undefined : status,
  });

  // Move mutation hook to component level to fix Rules of Hooks violation
  const rejectMutation = useMarketplaceUpdateAdminProductStatusMutation(
    rejectModalState.productId ?? 0
  );

  const openRejectModal = (productId: number, targetStatus: MarketplaceProductStatus) => {
    setRejectModalState({ isOpen: true, productId, targetStatus });
  };

  const closeRejectModal = () => {
    setRejectModalState({ isOpen: false, productId: null, targetStatus: null });
  };

  const handleRejectConfirm = async (reason: string) => {
    if (!rejectModalState.productId || !rejectModalState.targetStatus) return;

    try {
      await rejectMutation.mutateAsync({
        status: rejectModalState.targetStatus,
        statusReason: reason,
      });
      closeRejectModal();
    } catch (error) {
      toast.error("Failed to update product status. Please try again.");
      // Keep modal open on error so user can retry
    }
  };

  return (
    <div className="space-y-6">
      <div>
        <p className="text-sm font-medium text-primary">FarmTrace Admin</p>
        <h1 className="mt-1 text-3xl font-bold text-foreground">Moderate marketplace products</h1>
        <p className="mt-2 max-w-2xl text-sm text-muted-foreground">
          Return to the older review-table layout while keeping current moderation mutations and filters.
        </p>
      </div>

      <div className="rounded-xl border border-border bg-card p-4 shadow-sm">
        <div className="flex flex-col gap-4 lg:flex-row lg:items-center">
          <div className="relative max-w-xl flex-1">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground" size={18} />
            <Input
              value={search}
              onChange={(event) => setSearch(event.target.value)}
              placeholder="Search by product, farm, or seller..."
              className="h-11 rounded-xl border-border pl-10"
            />
          </div>

          <div className="flex flex-wrap gap-2">
            {statusFilters.map((option) => (
              <Button
                key={option.value}
                type="button"
                variant={status === option.value ? "default" : "outline"}
                size="sm"
                onClick={() => {
                  setStatus(option.value);
                  setPage(0); // Reset page to 0 on filter change
                }}
                className="rounded-full"
              >
                {option.label}
              </Button>
            ))}
          </div>
        </div>
      </div>

      <div className="overflow-hidden rounded-xl border border-border bg-card shadow-sm">
        <div className="overflow-x-auto">
          <table className="w-full border-collapse text-left">
            <thead>
              <tr className="border-b border-border bg-muted text-sm text-muted-foreground">
                <th className="p-4 font-medium">Product</th>
                <th className="p-4 font-medium">Seller</th>
                <th className="p-4 font-medium">Traceability</th>
                <th className="p-4 font-medium">Status</th>
                <th className="p-4 font-medium">Price</th>
                <th className="p-4 text-right font-medium">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-border">
              {(productsQuery.data?.items ?? []).map((product) => (
                <tr key={product.id} className="hover:bg-muted/50">
                  <td className="p-4">
                    <div className="flex items-center gap-3">
                      <img
                        src={product.imageUrl}
                        alt={product.name}
                        className="h-11 w-11 rounded-lg bg-muted object-cover"
                        referrerPolicy="no-referrer"
                      />
                      <div className="min-w-0">
                        <p className="truncate font-medium text-foreground">{product.name}</p>
                        <p className="truncate text-xs text-muted-foreground">{product.category || "Uncategorized"}</p>
                      </div>
                    </div>
                  </td>
                  <td className="p-4 text-sm text-muted-foreground">
                    <div>{product.farmerDisplayName}</div>
                    <div className="text-xs text-muted-foreground/60">{product.farmName ?? "Unknown farm"}</div>
                  </td>
                  <td className="p-4">
                    {product.traceable ? <Badge variant="success">Yes</Badge> : <Badge variant="secondary">No</Badge>}
                  </td>
                  <td className="p-4">
                    <Badge variant={statusVariant(product.status)}>{statusLabel(product.status)}</Badge>
                  </td>
                  <td className="p-4 font-medium text-foreground">{formatVnd(product.price)}</td>
                  <td className="p-4 text-right">
                    <ModerationActions
                      productId={product.id}
                      currentStatus={product.status}
                      onOpenRejectModal={openRejectModal}
                    />
                  </td>
                </tr>
              ))}
              {productsQuery.isLoading ? (
                <tr>
                  <td colSpan={6} className="p-8 text-center text-sm text-muted-foreground">
                    Loading products...
                  </td>
                </tr>
              ) : null}
              {!productsQuery.isLoading && !productsQuery.isError && (productsQuery.data?.items ?? []).length === 0 ? (
                <tr>
                  <td colSpan={6} className="p-8 text-center text-sm text-muted-foreground">
                    No products matched the current moderation filters.
                  </td>
                </tr>
              ) : null}
              {productsQuery.isError ? (
                <tr>
                  <td colSpan={6} className="p-8 text-center text-sm text-destructive">
                    Failed to load admin marketplace products.
                  </td>
                </tr>
              ) : null}
            </tbody>
          </table>
        </div>
      </div>

      {productsQuery.data && (
        <PaginationControls
          currentPage={page}
          totalPages={productsQuery.data.totalPages}
          totalElements={productsQuery.data.totalElements}
          pageSize={pageSize}
          onPageChange={setPage}
          onPageSizeChange={(newSize) => {
            setPageSize(newSize);
            setPage(0);
          }}
        />
      )}

      <RejectWithReasonModal
        isOpen={rejectModalState.isOpen}
        onClose={closeRejectModal}
        onConfirm={handleRejectConfirm}
        isLoading={rejectMutation.isPending}
        title={rejectModalState.targetStatus === "HIDDEN" ? "Hide Product" : "Reject Product"}
        description={
          rejectModalState.targetStatus === "HIDDEN"
            ? "This product will be hidden from the marketplace. The farmer will be notified."
            : "This product will be rejected. The farmer will be notified."
        }
        reasonLabel="Reason for action"
        reasonPlaceholder="Explain why this action is being taken..."
      />
    </div>
  );
}
