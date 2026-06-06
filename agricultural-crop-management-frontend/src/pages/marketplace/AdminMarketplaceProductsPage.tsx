import { useState } from "react";
import { Check, MoreVertical, RotateCcw, Search, X } from "lucide-react";
import { toast } from "sonner";
import type { MarketplaceProductStatus } from "@/shared/api";
import {
  Badge,
  Button,
  CardContent,
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
  Input,
} from "@/shared/ui";
import {
  AdminContentCard,
  AdminFilterCard,
  AdminHeaderCard,
  AdminPageContainer,
} from "@/features/admin/shared/ui";
import {
  useMarketplaceAdminProducts,
  useMarketplaceUpdateAdminProductStatusMutation,
} from "@/features/marketplace/hooks";
import { formatVnd } from "@/features/marketplace/lib/format";
import { RejectWithReasonModal, PaginationControls } from "@/features/marketplace/components";
import { useI18n } from "@/shared/lib/hooks/useI18n";

const statusFilters: Array<{ value: "ALL" | MarketplaceProductStatus; labelKey: string }> = [
  { value: "ALL", labelKey: "admin.marketplace.products.filters.all" },
  { value: "DRAFT", labelKey: "admin.marketplace.products.status.draft" },
  { value: "PENDING_REVIEW", labelKey: "admin.marketplace.products.status.pendingReview" },
  { value: "ACTIVE", labelKey: "admin.marketplace.products.status.active" },
  { value: "INACTIVE", labelKey: "admin.marketplace.products.status.inactive" },
  { value: "REJECTED", labelKey: "admin.marketplace.products.status.rejected" },
  { value: "SOLD_OUT", labelKey: "admin.marketplace.products.status.soldOut" },
];

function statusVariant(status: MarketplaceProductStatus) {
  if (status === "ACTIVE" || status === "PUBLISHED") return "success" as const;
  if (status === "PENDING_REVIEW") return "warning" as const;
  if (status === "INACTIVE" || status === "REJECTED" || status === "HIDDEN") return "destructive" as const;
  return "secondary" as const;
}

function statusLabel(status: MarketplaceProductStatus, t: ReturnType<typeof useI18n>["t"]) {
  switch (status) {
    case "DRAFT":
      return t("admin.marketplace.products.status.draft");
    case "PENDING_REVIEW":
      return t("admin.marketplace.products.status.pendingReview");
    case "ACTIVE":
      return t("admin.marketplace.products.status.active");
    case "INACTIVE":
      return t("admin.marketplace.products.status.inactive");
    case "REJECTED":
      return t("admin.marketplace.products.status.rejected");
    case "SOLD_OUT":
      return t("admin.marketplace.products.status.soldOut");
    case "PUBLISHED":
      return t("admin.marketplace.products.status.published");
    case "HIDDEN":
      return t("admin.marketplace.products.status.hidden");
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
  const { t } = useI18n();
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
            status: "ACTIVE",
            label: t("admin.marketplace.products.actions.approve"),
            icon: Check,
            className: "text-primary hover:bg-emerald-50 hover:text-primary",
            requiresReason: false,
          },
          {
            status: "REJECTED",
            label: t("admin.marketplace.products.actions.reject"),
            icon: X,
            className: "text-destructive hover:bg-red-50 hover:text-red-700",
            requiresReason: true,
          },
        ]
      : currentStatus === "ACTIVE" || currentStatus === "PUBLISHED"
        ? [
            {
              status: "INACTIVE",
              label: t("admin.marketplace.products.actions.hide"),
              icon: X,
              className: "text-destructive hover:bg-red-50 hover:text-red-700",
              requiresReason: true,
            },
          ]
        : currentStatus === "INACTIVE" || currentStatus === "HIDDEN"
          ? [
              {
                status: "ACTIVE",
                label: t("admin.marketplace.products.actions.publish"),
                icon: Check,
                className: "text-primary hover:bg-emerald-50 hover:text-primary",
                requiresReason: false,
              },
              {
                status: "PENDING_REVIEW",
                label: t("admin.marketplace.products.actions.returnToReview"),
                icon: RotateCcw,
                className: "text-muted-foreground hover:bg-muted hover:text-foreground",
                requiresReason: false,
              },
            ]
          : currentStatus === "REJECTED"
            ? [
                {
                  status: "PENDING_REVIEW",
                  label: t("admin.marketplace.products.actions.returnToReview"),
                  icon: RotateCcw,
                  className: "text-muted-foreground hover:bg-muted hover:text-foreground",
                  requiresReason: false,
                },
                {
                  status: "ACTIVE",
                  label: t("admin.marketplace.products.actions.approve"),
                  icon: Check,
                  className: "text-primary hover:bg-emerald-50 hover:text-primary",
                  requiresReason: false,
                },
              ]
          : [
              {
                status: "PENDING_REVIEW",
                label: t("admin.marketplace.products.actions.sendToReview"),
                icon: RotateCcw,
                className: "text-muted-foreground hover:bg-muted hover:text-foreground",
                requiresReason: false,
              },
              {
                status: "INACTIVE",
                label: t("admin.marketplace.products.actions.hide"),
                icon: X,
                className: "text-destructive hover:bg-red-50 hover:text-red-700",
                requiresReason: true,
              },
            ];

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button
          type="button"
          variant="ghost"
          size="icon"
          className="h-8 w-8 rounded-[14px]"
          disabled={mutation.isPending}
          aria-label={t("admin.marketplace.products.actions.productActions")}
        >
          <MoreVertical className="h-4 w-4" />
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" className="w-48">
        {actions.map((action) => {
          const Icon = action.icon;

          return (
            <DropdownMenuItem
              key={action.status}
              className={action.className}
              disabled={mutation.isPending}
              onSelect={() => {
                if (action.requiresReason) {
                  onOpenRejectModal(productId, action.status);
                } else {
                  handleApprove(action.status);
                }
              }}
            >
              <Icon size={14} />
              <span>{action.label}</span>
            </DropdownMenuItem>
          );
        })}
      </DropdownMenuContent>
    </DropdownMenu>
  );
}

export function AdminMarketplaceProductsPage() {
  const { t } = useI18n();
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
    } catch {
      toast.error(t("admin.marketplace.products.toast.updateStatusFailed"));
      // Keep modal open on error so user can retry
    }
  };

  return (
    <AdminPageContainer>
      <AdminHeaderCard
        title={t("admin.marketplace.products.title")}
        description={t("admin.marketplace.products.description")}
        metadata={<span className="rounded-full bg-primary/10 px-2 py-0.5 text-xs font-medium text-primary">{t("admin.marketplace.common.adminBadge")}</span>}
      />

      <AdminFilterCard>
        <CardContent className="p-4">
        <div className="flex flex-col gap-4 lg:flex-row lg:items-center">
          <div className="relative max-w-xl flex-1">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground" size={18} />
            <Input
              value={search}
              onChange={(event) => setSearch(event.target.value)}
              placeholder={t("admin.marketplace.products.searchPlaceholder")}
              className="h-11 rounded-[14px] border-border pl-10"
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
                className="rounded-[14px]"
              >
                {t(option.labelKey)}
              </Button>
            ))}
          </div>
        </div>
        </CardContent>
      </AdminFilterCard>

      <AdminContentCard>
        <div className="overflow-x-auto">
          <table className="w-full border-collapse text-left">
            <thead>
              <tr className="border-b border-border bg-muted text-sm text-muted-foreground">
                <th className="p-4 font-medium">{t("admin.marketplace.products.table.product")}</th>
                <th className="p-4 font-medium">{t("admin.marketplace.products.table.seller")}</th>
                <th className="p-4 font-medium">{t("admin.marketplace.products.table.traceability")}</th>
                <th className="p-4 font-medium">{t("admin.marketplace.products.table.status")}</th>
                <th className="p-4 font-medium">{t("admin.marketplace.products.table.price")}</th>
                <th className="p-4 text-right font-medium">{t("admin.marketplace.products.table.actions")}</th>
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
                        <p className="truncate text-xs text-muted-foreground">{product.category || t("admin.marketplace.products.fallback.uncategorized")}</p>
                      </div>
                    </div>
                  </td>
                  <td className="p-4 text-sm text-muted-foreground">
                    <div>{product.farmerDisplayName}</div>
                    <div className="text-xs text-muted-foreground/60">{product.farmName ?? t("admin.marketplace.products.fallback.unknownFarm")}</div>
                  </td>
                  <td className="p-4">
                    {product.traceable ? (
                      <Badge variant="success">{t("common.yes")}</Badge>
                    ) : (
                      <Badge variant="secondary">{t("common.no")}</Badge>
                    )}
                  </td>
                  <td className="p-4">
                    <Badge variant={statusVariant(product.status)}>{statusLabel(product.status, t)}</Badge>
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
                    {t("admin.marketplace.products.table.loading")}
                  </td>
                </tr>
              ) : null}
              {!productsQuery.isLoading && !productsQuery.isError && (productsQuery.data?.items ?? []).length === 0 ? (
                <tr>
                  <td colSpan={6} className="p-8 text-center text-sm text-muted-foreground">
                    {t("admin.marketplace.products.table.empty")}
                  </td>
                </tr>
              ) : null}
              {productsQuery.isError ? (
                <tr>
                  <td colSpan={6} className="p-8 text-center text-sm text-destructive">
                    {t("admin.marketplace.products.table.loadError")}
                  </td>
                </tr>
              ) : null}
            </tbody>
          </table>
        </div>
      </AdminContentCard>

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
        title={
          rejectModalState.targetStatus === "INACTIVE" || rejectModalState.targetStatus === "HIDDEN"
            ? t("admin.marketplace.products.modal.hideTitle")
            : t("admin.marketplace.products.modal.rejectTitle")
        }
        description={
          rejectModalState.targetStatus === "INACTIVE" || rejectModalState.targetStatus === "HIDDEN"
            ? t("admin.marketplace.products.modal.hideDescription")
            : t("admin.marketplace.products.modal.rejectDescription")
        }
        reasonLabel={t("admin.marketplace.products.modal.reasonLabel")}
        reasonPlaceholder={t("admin.marketplace.products.modal.reasonPlaceholder")}
      />
    </AdminPageContainer>
  );
}
