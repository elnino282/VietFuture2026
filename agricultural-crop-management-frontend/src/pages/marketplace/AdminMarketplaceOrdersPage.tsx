import { useState } from "react";
import { useSearchParams } from "react-router-dom";
import { toast } from "sonner";
import type { MarketplaceOrderStatus } from "@/shared/api";
import { Badge, Button, CardContent, CardHeader, CardTitle } from "@/shared/ui";
import { BackButton } from "@/shared/ui/back-button";
import {
  AdminContentCard,
  AdminFilterCard,
  AdminHeaderCard,
  AdminPageContainer,
} from "@/features/admin/shared/ui";
import {
  useMarketplaceAdminOrderAuditLogs,
  useMarketplaceAdminOrderDetail,
  useMarketplaceAdminOrders,
  useMarketplaceUpdateAdminOrderPaymentVerificationMutation,
  useMarketplaceUpdateAdminOrderStatusMutation,
} from "@/features/marketplace/hooks";
import { formatDateTime, formatVnd } from "@/features/marketplace/lib/format";
import { RejectWithReasonModal, PaginationControls } from "@/features/marketplace/components";
import { useI18n } from "@/shared/lib/hooks/useI18n";

const statusFilters: Array<{ value: "ALL" | MarketplaceOrderStatus; labelKey: string }> = [
  { value: "ALL", labelKey: "admin.marketplace.orders.filters.all" },
  { value: "PENDING", labelKey: "admin.marketplace.orders.status.pending" },
  { value: "CONFIRMED", labelKey: "admin.marketplace.orders.status.confirmed" },
  { value: "PREPARING", labelKey: "admin.marketplace.orders.status.preparing" },
  { value: "DELIVERING", labelKey: "admin.marketplace.orders.status.delivering" },
  { value: "COMPLETED", labelKey: "admin.marketplace.orders.status.completed" },
  { value: "CANCELLED", labelKey: "admin.marketplace.orders.status.cancelled" },
];

function statusVariant(status: MarketplaceOrderStatus) {
  if (status === "COMPLETED") return "success" as const;
  if (status === "CANCELLED") return "destructive" as const;
  if (status === "PENDING") return "warning" as const;
  return "secondary" as const;
}

function orderStatusLabel(status: MarketplaceOrderStatus, t: ReturnType<typeof useI18n>["t"]) {
  switch (status) {
    case "PENDING":
      return t("admin.marketplace.orders.status.pending");
    case "CONFIRMED":
      return t("admin.marketplace.orders.status.confirmed");
    case "PREPARING":
      return t("admin.marketplace.orders.status.preparing");
    case "DELIVERING":
      return t("admin.marketplace.orders.status.delivering");
    case "COMPLETED":
      return t("admin.marketplace.orders.status.completed");
    case "CANCELLED":
      return t("admin.marketplace.orders.status.cancelled");
    default:
      return status;
  }
}

function paymentStatusLabel(status: string, t: ReturnType<typeof useI18n>["t"]) {
  switch (status) {
    case "PENDING":
      return t("admin.marketplace.orders.paymentStatus.pending");
    case "SUBMITTED":
      return t("admin.marketplace.orders.paymentStatus.submitted");
    case "VERIFIED":
      return t("admin.marketplace.orders.paymentStatus.verified");
    case "REJECTED":
      return t("admin.marketplace.orders.paymentStatus.rejected");
    default:
      return status;
  }
}

export function AdminMarketplaceOrdersPage() {
  const { t } = useI18n();
  const [searchParams, setSearchParams] = useSearchParams();
  const [status, setStatus] = useState<"ALL" | MarketplaceOrderStatus>("ALL");
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(25);
  const [rejectPaymentModalState, setRejectPaymentModalState] = useState<{
    isOpen: boolean;
    orderId: number | null;
  }>({ isOpen: false, orderId: null });
  const [cancelOrderModalState, setCancelOrderModalState] = useState<{
    isOpen: boolean;
    orderId: number | null;
  }>({ isOpen: false, orderId: null });

  const ordersQuery = useMarketplaceAdminOrders({
    page,
    size: pageSize,
    status: status === "ALL" ? undefined : status,
  });

  const selectedOrderId = Number(searchParams.get("orderId") ?? 0);
  const selectedOrderQuery = useMarketplaceAdminOrderDetail(selectedOrderId);
  const selectedOrder = selectedOrderQuery.data;
  const auditLogsQuery = useMarketplaceAdminOrderAuditLogs(selectedOrderId);
  const verifyMutation = useMarketplaceUpdateAdminOrderPaymentVerificationMutation(selectedOrderId || 0);
  const cancelMutation = useMarketplaceUpdateAdminOrderStatusMutation(selectedOrderId || 0);
  const closeSelectedOrder = () => {
    const next = new URLSearchParams(searchParams);
    next.delete("orderId");
    setSearchParams(next, { replace: true });
  };

  const openRejectPaymentModal = (orderId: number) => {
    setRejectPaymentModalState({ isOpen: true, orderId });
  };

  const closeRejectPaymentModal = () => {
    setRejectPaymentModalState({ isOpen: false, orderId: null });
  };

  const handleRejectPaymentConfirm = async (reason: string) => {
    if (!rejectPaymentModalState.orderId) return;

    try {
      await verifyMutation.mutateAsync({
        verificationStatus: "REJECTED",
        verificationNote: reason,
      });
      await Promise.all([selectedOrderQuery.refetch(), auditLogsQuery.refetch()]);
      closeRejectPaymentModal();
    } catch {
      toast.error(t("admin.marketplace.orders.toast.rejectPaymentFailed"));
      // Keep modal open on error so user can retry
    }
  };

  const openCancelOrderModal = (orderId: number) => {
    setCancelOrderModalState({ isOpen: true, orderId });
  };

  const closeCancelOrderModal = () => {
    setCancelOrderModalState({ isOpen: false, orderId: null });
  };

  const handleCancelOrderConfirm = async (reason: string) => {
    if (!cancelOrderModalState.orderId) return;

    try {
      await cancelMutation.mutateAsync({ status: "CANCELLED", reason });
      await Promise.all([selectedOrderQuery.refetch(), auditLogsQuery.refetch()]);
      closeCancelOrderModal();
    } catch {
      toast.error(t("admin.marketplace.orders.toast.cancelOrderFailed"));
      // Keep modal open on error so user can retry
    }
  };

  return (
    <AdminPageContainer>
      <AdminHeaderCard
        title={t("admin.marketplace.orders.title")}
        description={t("admin.marketplace.orders.description")}
        metadata={<span className="rounded-full bg-primary/10 px-2 py-0.5 text-xs font-medium text-primary">{t("admin.marketplace.common.adminBadge")}</span>}
      />

      <AdminFilterCard>
        <CardContent className="p-4">
      <div className="flex flex-wrap gap-2">
        {statusFilters.map((option) => (
          <Button
            key={option.value}
            type="button"
            variant={status === option.value ? "default" : "outline"}
            size="sm"
            onClick={() => {
              setStatus(option.value);
              setPage(0);
            }}
            className="rounded-[14px]"
          >
            {t(option.labelKey)}
          </Button>
        ))}
      </div>
        </CardContent>
      </AdminFilterCard>

      <div className="grid grid-cols-1 gap-6 xl:grid-cols-[380px_minmax(0,1fr)]">
        <AdminContentCard>
          <CardHeader className="border-b border-border/50">
            <CardTitle>{t("admin.marketplace.orders.list.title")}</CardTitle>
          </CardHeader>
          <CardContent className="space-y-3 p-4">
            {(ordersQuery.data?.items ?? []).map((order) => (
              <button
                key={order.id}
                type="button"
                onClick={() => setSearchParams({ orderId: String(order.id) })}
                className="w-full rounded-xl border border-border p-4 text-left transition-colors hover:bg-muted/50"
              >
                <div className="flex items-start justify-between gap-3">
                  <div className="min-w-0">
                    <p className="font-semibold text-foreground">{order.orderCode}</p>
                    <p className="text-xs text-muted-foreground">
                      {t("admin.marketplace.orders.list.participants", {
                        buyerId: order.buyerUserId,
                        farmerId: order.farmerUserId,
                      })}
                    </p>
                    <p className="text-xs text-muted-foreground">
                      {t("admin.marketplace.orders.list.paymentSummary", {
                        method: order.payment.method,
                        status: paymentStatusLabel(order.payment.verificationStatus, t),
                      })}
                    </p>
                    <p className="text-xs text-muted-foreground/60">{formatDateTime(order.createdAt)}</p>
                  </div>
                  <div className="text-right">
                    <Badge variant={statusVariant(order.status)}>{orderStatusLabel(order.status, t)}</Badge>
                    <p className="mt-2 font-semibold text-primary">{formatVnd(order.totalAmount)}</p>
                  </div>
                </div>
              </button>
            ))}
            {ordersQuery.isLoading ? <p className="p-4 text-sm text-muted-foreground">{t("admin.marketplace.orders.list.loading")}</p> : null}
            {ordersQuery.isError ? <p className="p-4 text-sm text-destructive">{t("admin.marketplace.orders.list.loadError")}</p> : null}
            {!ordersQuery.isLoading && !ordersQuery.isError && (ordersQuery.data?.items ?? []).length === 0 ? (
              <p className="p-4 text-sm text-muted-foreground">{t("admin.marketplace.orders.list.empty")}</p>
            ) : null}
          </CardContent>
        </AdminContentCard>

        {selectedOrder ? (
          <div className="space-y-6">
            <AdminContentCard>
              <CardHeader className="border-b border-border/50">
                <BackButton onClick={closeSelectedOrder} className="mb-3 w-fit" />
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <div>
                    <CardTitle>{t("admin.marketplace.orders.detail.title")}</CardTitle>
                    <p className="mt-2 text-sm text-muted-foreground">{selectedOrder.orderCode}</p>
                  </div>
                  <Badge variant={statusVariant(selectedOrder.status)}>{orderStatusLabel(selectedOrder.status, t)}</Badge>
                </div>
              </CardHeader>
              <CardContent className="space-y-6 p-6">
                <div className="grid gap-4 md:grid-cols-2">
                  <div className="rounded-xl border border-border p-4 text-sm text-muted-foreground">
                    <p className="text-xs uppercase tracking-[0.18em] text-muted-foreground/60">{t("admin.marketplace.orders.detail.shipping")}</p>
                    <p className="mt-3 font-medium text-foreground">{selectedOrder.shippingRecipientName}</p>
                    <p>{selectedOrder.shippingPhone}</p>
                    <p>{selectedOrder.shippingAddressLine}</p>
                    {selectedOrder.note ? (
                      <p className="mt-3 text-muted-foreground">
                        {t("admin.marketplace.orders.detail.note", { note: selectedOrder.note })}
                      </p>
                    ) : null}
                  </div>

                  <div className="rounded-xl border border-border p-4 text-sm text-muted-foreground">
                    <p className="text-xs uppercase tracking-[0.18em] text-muted-foreground/60">{t("admin.marketplace.orders.detail.payment")}</p>
                    <p className="mt-3 font-medium text-foreground">{selectedOrder.payment.method}</p>
                    <p>{paymentStatusLabel(selectedOrder.payment.verificationStatus, t)}</p>
                    <p className="mt-3 text-lg font-semibold text-primary">{formatVnd(selectedOrder.totalAmount)}</p>
                  </div>
                </div>

                {selectedOrder.payment.proofFileName ? (
                  <div className="rounded-xl border border-border bg-muted/50 p-4 text-sm text-muted-foreground">
                    <p className="font-medium text-foreground">{t("admin.marketplace.orders.detail.paymentProof")}</p>
                    <p className="mt-2">{selectedOrder.payment.proofFileName}</p>
                    {selectedOrder.payment.proofUploadedAt ? (
                      <p className="mt-1 text-xs text-muted-foreground/60">
                        {t("admin.marketplace.orders.detail.uploadedAt", {
                          date: formatDateTime(selectedOrder.payment.proofUploadedAt),
                        })}
                      </p>
                    ) : null}
                  </div>
                ) : null}

                <div className="rounded-xl border border-border p-4">
                  <p className="mb-3 text-sm font-medium text-foreground">{t("admin.marketplace.orders.detail.paymentVerification")}</p>
                  <div className="flex flex-wrap gap-2">
                    <Button
                      disabled={verifyMutation.isPending}
                      onClick={async () => {
                        try {
                          await verifyMutation.mutateAsync({
                            verificationStatus: "VERIFIED",
                            verificationNote: "",
                          });
                          await Promise.all([selectedOrderQuery.refetch(), auditLogsQuery.refetch()]);
                        } catch {
                          toast.error(t("admin.marketplace.orders.toast.verifyPaymentFailed"));
                        }
                      }}
                    >
                      {t("admin.marketplace.orders.actions.markVerified")}
                    </Button>
                    <Button
                      variant="outline"
                      disabled={verifyMutation.isPending}
                      onClick={() => openRejectPaymentModal(selectedOrder.id)}
                    >
                      {t("admin.marketplace.orders.actions.rejectProof")}
                    </Button>
                    {selectedOrder.status !== "CANCELLED" ? (
                      <Button
                        variant="destructive"
                        disabled={cancelMutation.isPending}
                        onClick={() => openCancelOrderModal(selectedOrder.id)}
                      >
                        {t("admin.marketplace.orders.actions.cancelOrder")}
                      </Button>
                    ) : null}
                  </div>
                </div>

                <div>
                  <p className="mb-3 text-sm font-semibold uppercase tracking-[0.18em] text-muted-foreground">
                    {t("admin.marketplace.orders.detail.orderItems")}
                  </p>
                  <div className="space-y-3">
                    {selectedOrder.items.map((item) => (
                      <div key={item.id} className="flex items-center justify-between rounded-xl border border-border p-4">
                        <div>
                          <p className="font-medium text-foreground">{item.productName}</p>
                          <p className="text-sm text-muted-foreground">
                            {t("admin.marketplace.orders.detail.itemQuantityPrice", {
                              quantity: item.quantity,
                              price: formatVnd(item.unitPriceSnapshot),
                            })}
                          </p>
                        </div>
                        <p className="font-semibold text-foreground">{formatVnd(item.lineTotal)}</p>
                      </div>
                    ))}
                  </div>
                </div>
              </CardContent>
            </AdminContentCard>

            <AdminContentCard>
              <CardHeader className="border-b border-border/50">
                <CardTitle>{t("admin.marketplace.orders.audit.title")}</CardTitle>
              </CardHeader>
              <CardContent className="space-y-3 p-6">
                {auditLogsQuery.isLoading ? <p className="text-sm text-muted-foreground">{t("admin.marketplace.orders.audit.loading")}</p> : null}
                {auditLogsQuery.isError ? <p className="text-sm text-destructive">{t("admin.marketplace.orders.audit.loadError")}</p> : null}
                {(auditLogsQuery.data ?? []).map((log) => (
                  <div key={log.id} className="rounded-xl border border-border p-4">
                    <p className="font-medium text-foreground">{log.operation}</p>
                    <p className="mt-1 text-xs text-muted-foreground">
                      {t("admin.marketplace.orders.audit.performedBy", {
                        user: log.performedBy,
                        date: formatDateTime(log.performedAt),
                      })}
                    </p>
                    {log.reason ? <p className="mt-2 text-sm text-muted-foreground">{log.reason}</p> : null}
                  </div>
                ))}
                {!auditLogsQuery.isLoading && !auditLogsQuery.isError && (auditLogsQuery.data ?? []).length === 0 ? (
                  <p className="text-sm text-muted-foreground">{t("admin.marketplace.orders.audit.empty")}</p>
                ) : null}
              </CardContent>
            </AdminContentCard>
          </div>
        ) : (
          <AdminContentCard>
            <CardContent className="p-8 text-sm text-muted-foreground">
              {t("admin.marketplace.orders.detail.emptySelection")}
            </CardContent>
          </AdminContentCard>
        )}
      </div>

      {ordersQuery.data && (
        <PaginationControls
          currentPage={page}
          totalPages={ordersQuery.data.totalPages}
          totalElements={ordersQuery.data.totalElements}
          pageSize={pageSize}
          onPageChange={setPage}
          onPageSizeChange={(newSize) => {
            setPageSize(newSize);
            setPage(0);
          }}
        />
      )}

      <RejectWithReasonModal
        isOpen={rejectPaymentModalState.isOpen}
        onClose={closeRejectPaymentModal}
        onConfirm={handleRejectPaymentConfirm}
        title={t("admin.marketplace.orders.rejectPayment.title")}
        description={t("admin.marketplace.orders.rejectPayment.description")}
        reasonLabel={t("admin.marketplace.orders.rejectPayment.reasonLabel")}
        reasonPlaceholder={t("admin.marketplace.orders.rejectPayment.reasonPlaceholder")}
        isLoading={verifyMutation.isPending}
      />

      <RejectWithReasonModal
        isOpen={cancelOrderModalState.isOpen}
        onClose={closeCancelOrderModal}
        onConfirm={handleCancelOrderConfirm}
        title={t("admin.marketplace.orders.cancel.title")}
        description={t("admin.marketplace.orders.cancel.description")}
        reasonLabel={t("admin.marketplace.orders.cancel.reasonLabel")}
        reasonPlaceholder={t("admin.marketplace.orders.cancel.reasonPlaceholder")}
        isLoading={cancelMutation.isPending}
      />
    </AdminPageContainer>
  );
}
