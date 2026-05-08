import { useState } from "react";
import { useSearchParams } from "react-router-dom";
import { toast } from "sonner";
import type { MarketplaceOrderStatus } from "@/shared/api";
import { Badge, Button, Card, CardContent, CardHeader, CardTitle } from "@/shared/ui";
import {
  useMarketplaceAdminOrderAuditLogs,
  useMarketplaceAdminOrderDetail,
  useMarketplaceAdminOrders,
  useMarketplaceUpdateAdminOrderPaymentVerificationMutation,
  useMarketplaceUpdateAdminOrderStatusMutation,
} from "@/features/marketplace/hooks";
import { formatDateTime, formatVnd } from "@/features/marketplace/lib/format";
import { RejectWithReasonModal, PaginationControls } from "@/features/marketplace/components";

const statusFilters: Array<{ value: "ALL" | MarketplaceOrderStatus; label: string }> = [
  { value: "ALL", label: "All" },
  { value: "PENDING", label: "Pending" },
  { value: "CONFIRMED", label: "Confirmed" },
  { value: "PREPARING", label: "Preparing" },
  { value: "DELIVERING", label: "Delivering" },
  { value: "COMPLETED", label: "Completed" },
  { value: "CANCELLED", label: "Cancelled" },
];

function statusVariant(status: MarketplaceOrderStatus) {
  if (status === "COMPLETED") return "success" as const;
  if (status === "CANCELLED") return "destructive" as const;
  if (status === "PENDING") return "warning" as const;
  return "secondary" as const;
}

function paymentStatusLabel(status: string) {
  switch (status) {
    case "PENDING":
      return "Pending";
    case "SUBMITTED":
      return "Submitted";
    case "VERIFIED":
      return "Verified";
    case "REJECTED":
      return "Rejected";
    default:
      return status;
  }
}

export function AdminMarketplaceOrdersPage() {
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
    } catch (error) {
      toast.error("Failed to reject payment proof. Please try again.");
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
    } catch (error) {
      toast.error("Failed to cancel order. Please try again.");
      // Keep modal open on error so user can retry
    }
  };

  return (
    <div className="space-y-6">
      <div>
        <p className="text-sm font-medium text-primary">FarmTrace Admin</p>
        <h1 className="mt-1 text-3xl font-bold text-foreground">Manage marketplace orders</h1>
        <p className="mt-2 max-w-2xl text-sm text-muted-foreground">
          Review and manage marketplace orders, verify payment proofs, and track order status changes.
        </p>
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
              setPage(0);
            }}
            className="rounded-full"
          >
            {option.label}
          </Button>
        ))}
      </div>

      <div className="grid grid-cols-1 gap-6 xl:grid-cols-[380px_minmax(0,1fr)]">
        <Card className="border-border shadow-sm">
          <CardHeader className="border-b border-border/50">
            <CardTitle>Order list</CardTitle>
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
                    <p className="text-xs text-muted-foreground">Buyer #{order.buyerUserId} • Farmer #{order.farmerUserId}</p>
                    <p className="text-xs text-muted-foreground">
                      {order.payment.method} • {paymentStatusLabel(order.payment.verificationStatus)}
                    </p>
                    <p className="text-xs text-muted-foreground/60">{formatDateTime(order.createdAt)}</p>
                  </div>
                  <div className="text-right">
                    <Badge variant={statusVariant(order.status)}>{order.status}</Badge>
                    <p className="mt-2 font-semibold text-primary">{formatVnd(order.totalAmount)}</p>
                  </div>
                </div>
              </button>
            ))}
            {ordersQuery.isLoading ? <p className="p-4 text-sm text-muted-foreground">Loading orders...</p> : null}
            {ordersQuery.isError ? <p className="p-4 text-sm text-destructive">Failed to load admin orders.</p> : null}
            {!ordersQuery.isLoading && !ordersQuery.isError && (ordersQuery.data?.items ?? []).length === 0 ? (
              <p className="p-4 text-sm text-muted-foreground">No orders matched the current status filter.</p>
            ) : null}
          </CardContent>
        </Card>

        {selectedOrder ? (
          <div className="space-y-6">
            <Card className="border-border shadow-sm">
              <CardHeader className="border-b border-border/50">
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <div>
                    <CardTitle>Order detail</CardTitle>
                    <p className="mt-2 text-sm text-muted-foreground">{selectedOrder.orderCode}</p>
                  </div>
                  <Badge variant={statusVariant(selectedOrder.status)}>{selectedOrder.status}</Badge>
                </div>
              </CardHeader>
              <CardContent className="space-y-6 p-6">
                <div className="grid gap-4 md:grid-cols-2">
                  <div className="rounded-xl border border-border p-4 text-sm text-muted-foreground">
                    <p className="text-xs uppercase tracking-[0.18em] text-muted-foreground/60">Shipping</p>
                    <p className="mt-3 font-medium text-foreground">{selectedOrder.shippingRecipientName}</p>
                    <p>{selectedOrder.shippingPhone}</p>
                    <p>{selectedOrder.shippingAddressLine}</p>
                    {selectedOrder.note ? <p className="mt-3 text-muted-foreground">Note: {selectedOrder.note}</p> : null}
                  </div>

                  <div className="rounded-xl border border-border p-4 text-sm text-muted-foreground">
                    <p className="text-xs uppercase tracking-[0.18em] text-muted-foreground/60">Payment</p>
                    <p className="mt-3 font-medium text-foreground">{selectedOrder.payment.method}</p>
                    <p>{paymentStatusLabel(selectedOrder.payment.verificationStatus)}</p>
                    <p className="mt-3 text-lg font-semibold text-primary">{formatVnd(selectedOrder.totalAmount)}</p>
                  </div>
                </div>

                {selectedOrder.payment.proofFileName ? (
                  <div className="rounded-xl border border-border bg-muted/50 p-4 text-sm text-muted-foreground">
                    <p className="font-medium text-foreground">Payment proof</p>
                    <p className="mt-2">{selectedOrder.payment.proofFileName}</p>
                    {selectedOrder.payment.proofUploadedAt ? (
                      <p className="mt-1 text-xs text-muted-foreground/60">
                        Uploaded {formatDateTime(selectedOrder.payment.proofUploadedAt)}
                      </p>
                    ) : null}
                  </div>
                ) : null}

                <div className="rounded-xl border border-border p-4">
                  <p className="mb-3 text-sm font-medium text-foreground">Payment verification</p>
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
                        } catch (error) {
                          toast.error("Failed to verify payment. Please try again.");
                        }
                      }}
                    >
                      Mark verified
                    </Button>
                    <Button
                      variant="outline"
                      disabled={verifyMutation.isPending}
                      onClick={() => openRejectPaymentModal(selectedOrder.id)}
                    >
                      Reject proof
                    </Button>
                    {selectedOrder.status !== "CANCELLED" ? (
                      <Button
                        variant="destructive"
                        disabled={cancelMutation.isPending}
                        onClick={() => openCancelOrderModal(selectedOrder.id)}
                      >
                        Cancel order
                      </Button>
                    ) : null}
                  </div>
                </div>

                <div>
                  <p className="mb-3 text-sm font-semibold uppercase tracking-[0.18em] text-muted-foreground">
                    Order items
                  </p>
                  <div className="space-y-3">
                    {selectedOrder.items.map((item) => (
                      <div key={item.id} className="flex items-center justify-between rounded-xl border border-border p-4">
                        <div>
                          <p className="font-medium text-foreground">{item.productName}</p>
                          <p className="text-sm text-muted-foreground">
                            {item.quantity} x {formatVnd(item.unitPriceSnapshot)}
                          </p>
                        </div>
                        <p className="font-semibold text-foreground">{formatVnd(item.lineTotal)}</p>
                      </div>
                    ))}
                  </div>
                </div>
              </CardContent>
            </Card>

            <Card className="border-border shadow-sm">
              <CardHeader className="border-b border-border/50">
                <CardTitle>Audit log</CardTitle>
              </CardHeader>
              <CardContent className="space-y-3 p-6">
                {auditLogsQuery.isLoading ? <p className="text-sm text-muted-foreground">Loading audit log...</p> : null}
                {auditLogsQuery.isError ? <p className="text-sm text-destructive">Failed to load audit log.</p> : null}
                {(auditLogsQuery.data ?? []).map((log) => (
                  <div key={log.id} className="rounded-xl border border-border p-4">
                    <p className="font-medium text-foreground">{log.operation}</p>
                    <p className="mt-1 text-xs text-muted-foreground">
                      {log.performedBy} • {formatDateTime(log.performedAt)}
                    </p>
                    {log.reason ? <p className="mt-2 text-sm text-muted-foreground">{log.reason}</p> : null}
                  </div>
                ))}
                {!auditLogsQuery.isLoading && !auditLogsQuery.isError && (auditLogsQuery.data ?? []).length === 0 ? (
                  <p className="text-sm text-muted-foreground">No audit log entries for this order yet.</p>
                ) : null}
              </CardContent>
            </Card>
          </div>
        ) : (
          <Card className="border-border shadow-sm">
            <CardContent className="p-8 text-sm text-muted-foreground">
              Select an order from the list to review shipping, payment proof, and audit history.
            </CardContent>
          </Card>
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
        title="Reject Payment Proof"
        description="The buyer will be notified and may need to resubmit proof."
        reasonLabel="Reason for rejection"
        reasonPlaceholder="Explain why the payment proof is being rejected..."
        isLoading={verifyMutation.isPending}
      />

      <RejectWithReasonModal
        isOpen={cancelOrderModalState.isOpen}
        onClose={closeCancelOrderModal}
        onConfirm={handleCancelOrderConfirm}
        title="Cancel Order"
        description="This order will be cancelled. The buyer and farmer will be notified."
        reasonLabel="Reason for cancellation"
        reasonPlaceholder="Explain why this order is being cancelled..."
        isLoading={cancelMutation.isPending}
      />
    </div>
  );
}
