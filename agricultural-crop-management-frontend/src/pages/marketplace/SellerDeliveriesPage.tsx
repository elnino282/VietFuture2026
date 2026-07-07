import { useState } from "react";
import { Eye, Inbox, Truck, RefreshCw } from "lucide-react";
import { Link } from "react-router-dom";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { useI18n } from "@/hooks/useI18n";
import { cn } from "@/shared/lib";
import {
  Badge,
  Button,
  Card,
  CardContent,
  Label,
  PageContainer,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/shared/ui";
import { deliveryApi, type DeliveryOrder } from "@/shared/api";
import { SellerMarketplaceTabs } from "@/features/marketplace/layout";
import { formatDate, formatVnd } from "@/features/marketplace/lib/format";

const DELIVERY_STATUSES: DeliveryOrder["status"][] = [
  "PENDING",
  "PICKUP_SCHEDULED",
  "IN_TRANSIT",
  "OUT_FOR_DELIVERY",
  "DELIVERED",
  "RETURNED",
  "CANCELLED",
];

function statusBadgeVariant(status: DeliveryOrder["status"]) {
  switch (status) {
    case "PENDING":
      return "warning" as const;
    case "PICKUP_SCHEDULED":
    case "IN_TRANSIT":
    case "OUT_FOR_DELIVERY":
      return "info" as const;
    case "DELIVERED":
      return "success" as const;
    case "RETURNED":
    case "CANCELLED":
      return "destructive" as const;
    default:
      return "secondary" as const;
  }
}

function getProviderName(providerId: number): string {
  switch (providerId) {
    case 1:
      return "Giao hàng tiết kiệm (GHTK)";
    case 2:
      return "Giao hàng nhanh (GHN)";
    case 3:
      return "Ninja Van";
    case 4:
      return "J&T Express";
    default:
      return "Đối tác vận chuyển";
  }
}

export function SellerDeliveriesPage() {
  const { t, locale } = useI18n();
  const queryClient = useQueryClient();
  const [statusFilter, setStatusFilter] = useState<"ALL" | DeliveryOrder["status"]>("ALL");

  const deliveriesQuery = useQuery({
    queryKey: ["allDeliveries"],
    queryFn: () => deliveryApi.getAllDeliveryOrders(),
  });

  const updateStatusMutation = useMutation({
    mutationFn: ({ id, status }: { id: number; status: DeliveryOrder["status"] }) =>
      deliveryApi.updateDeliveryStatus(id, status),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["allDeliveries"] });
      toast.success("Cập nhật trạng thái vận đơn thành công.");
    },
    onError: (error) => {
      toast.error(error instanceof Error ? error.message : "Không thể cập nhật trạng thái.");
    },
  });

  const deliveries = deliveriesQuery.data ?? [];
  const filteredDeliveries = deliveries.filter(
    (d) => statusFilter === "ALL" || d.status === statusFilter
  );
  const hasDeliveries = filteredDeliveries.length > 0;

  return (
    <PageContainer variant="wide">
      <div className="space-y-4 md:space-y-5">
        <SellerMarketplaceTabs />

        <Card variant="page-header" className="rounded-lg">
          <CardContent className="px-4 py-4 sm:px-5">
            <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
              <div className="min-w-0">
                <h1 className="flex items-center gap-2 text-xl font-bold leading-tight text-foreground md:text-2xl">
                  <Truck className="h-5 w-5 text-primary md:h-6 md:w-6" />
                  Quản lý Vận đơn (Delivery Orders)
                </h1>
                <p className="mt-1 max-w-2xl text-sm text-muted-foreground">
                  Theo dõi trạng thái các lô hàng nông sản được gửi qua các đối tác giao hàng, bao gồm chuỗi lạnh và giao trong ngày.
                </p>
              </div>
            </div>
          </CardContent>
        </Card>

        {/* Filter Section */}
        <Card variant="filter" className="rounded-lg">
          <CardContent className="px-4 py-4 sm:px-5">
            <Label className="mb-2 block text-xs font-semibold text-muted-foreground">
              Lọc trạng thái vận đơn
            </Label>
            <div className="-mx-1 overflow-x-auto pb-1">
              <div className="flex min-w-max items-center gap-2 px-1 md:min-w-0 md:flex-wrap">
                <Button
                  variant={statusFilter === "ALL" ? "default" : "outline"}
                  size="sm"
                  onClick={() => setStatusFilter("ALL")}
                  className={cn("rounded-full", statusFilter === "ALL" ? "bg-primary text-primary-foreground" : "")}
                >
                  Tất cả
                </Button>
                {DELIVERY_STATUSES.map((status) => (
                  <Button
                    key={status}
                    variant={statusFilter === status ? "default" : "outline"}
                    size="sm"
                    onClick={() => setStatusFilter(status)}
                    className={cn("rounded-full", statusFilter === status ? "bg-primary text-primary-foreground" : "")}
                  >
                    {status}
                  </Button>
                ))}
              </div>
            </div>
          </CardContent>
        </Card>

        {/* Table View for Desktop */}
        <Card variant="content" className="hidden overflow-hidden rounded-lg md:block">
          <Table>
            <TableHeader>
              <TableRow className="bg-muted/40">
                <TableHead className="pl-4">Mã vận đơn</TableHead>
                <TableHead>Đơn hàng liên kết</TableHead>
                <TableHead>Đối tác vận chuyển</TableHead>
                <TableHead>Người nhận</TableHead>
                <TableHead>Phí vận chuyển</TableHead>
                <TableHead>Trạng thái</TableHead>
                <TableHead className="pr-4 text-right">Hành động</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {deliveriesQuery.isLoading ? (
                Array.from({ length: 5 }, (_, index) => (
                  <TableRow key={`skeleton-${index}`}>
                    <TableCell className="pl-4">
                      <div className="h-4 w-28 animate-pulse rounded bg-muted" />
                    </TableCell>
                    <TableCell>
                      <div className="h-4 w-24 animate-pulse rounded bg-muted" />
                    </TableCell>
                    <TableCell>
                      <div className="h-4 w-32 animate-pulse rounded bg-muted" />
                    </TableCell>
                    <TableCell>
                      <div className="h-4 w-28 animate-pulse rounded bg-muted" />
                    </TableCell>
                    <TableCell>
                      <div className="h-4 w-20 animate-pulse rounded bg-muted" />
                    </TableCell>
                    <TableCell>
                      <div className="h-6 w-24 animate-pulse rounded-full bg-muted" />
                    </TableCell>
                    <TableCell className="pr-4">
                      <div className="ml-auto h-8 w-20 animate-pulse rounded bg-muted" />
                    </TableCell>
                  </TableRow>
                ))
              ) : null}

              {!deliveriesQuery.isLoading && !deliveriesQuery.isError && hasDeliveries
                ? filteredDeliveries.map((delivery) => (
                    <TableRow key={delivery.id}>
                      <TableCell className="pl-4 font-semibold text-foreground">
                        <div className="flex flex-col">
                          <span>{delivery.trackingNumber || "Chưa tạo vận đơn"}</span>
                          <span className="text-xs text-muted-foreground">{formatDate(delivery.createdAt, locale)}</span>
                        </div>
                      </TableCell>
                      <TableCell className="font-medium">
                        <Link
                          to={`/farmer/marketplace-orders/${delivery.marketplaceOrderId}`}
                          className="text-primary hover:underline"
                        >
                          Đơn #{delivery.marketplaceOrderId}
                        </Link>
                      </TableCell>
                      <TableCell className="text-muted-foreground">
                        <div className="flex flex-col">
                          <span>{getProviderName(delivery.providerId)}</span>
                          <div className="flex items-center gap-1.5 mt-0.5">
                            {delivery.requiresColdChain && (
                              <Badge className="bg-blue-100 text-blue-800 text-[10px] hover:bg-blue-100">Chuỗi lạnh</Badge>
                            )}
                          </div>
                        </div>
                      </TableCell>
                      <TableCell className="text-muted-foreground">
                        <div className="flex flex-col text-xs">
                          <span className="font-medium text-foreground">{delivery.recipientName}</span>
                          <span>{delivery.recipientPhone}</span>
                          <span className="truncate max-w-[150px]">{delivery.recipientAddress}</span>
                        </div>
                      </TableCell>
                      <TableCell className="font-medium text-primary">
                        {formatVnd(delivery.shippingFeeVnd, locale)}
                      </TableCell>
                      <TableCell>
                        <Badge variant={statusBadgeVariant(delivery.status)}>{delivery.status}</Badge>
                      </TableCell>
                      <TableCell className="pr-4 text-right">
                        <div className="flex justify-end gap-2">
                          <Select
                            value={delivery.status}
                            onValueChange={(val) =>
                              updateStatusMutation.mutate({ id: delivery.id, status: val as DeliveryOrder["status"] })
                            }
                          >
                            <SelectTrigger className="w-[140px] text-xs h-8">
                              <SelectValue placeholder="Cập nhật trạng thái" />
                            </SelectTrigger>
                            <SelectContent>
                              {DELIVERY_STATUSES.map((status) => (
                                <SelectItem key={status} value={status}>
                                  {status}
                                </SelectItem>
                              ))}
                            </SelectContent>
                          </Select>
                        </div>
                      </TableCell>
                    </TableRow>
                  ))
                : null}

              {!deliveriesQuery.isLoading && !deliveriesQuery.isError && !hasDeliveries ? (
                <TableRow>
                  <TableCell colSpan={7} className="p-0">
                    <div className="flex flex-col items-center justify-center gap-3 px-4 py-10 text-center">
                      <span className="flex h-12 w-12 items-center justify-center rounded-full bg-muted text-muted-foreground">
                        <Inbox size={24} />
                      </span>
                      <p className="max-w-md text-sm text-muted-foreground">
                        Không tìm thấy vận đơn nào phù hợp với bộ lọc hiện tại.
                      </p>
                    </div>
                  </TableCell>
                </TableRow>
              ) : null}
            </TableBody>
          </Table>
        </Card>

        {/* Mobile View */}
        <div className="space-y-3 md:hidden">
          {!deliveriesQuery.isLoading && !deliveriesQuery.isError && hasDeliveries
            ? filteredDeliveries.map((delivery) => (
                <Card key={delivery.id} className="rounded-lg border border-border bg-card p-4 shadow-sm">
                  <div className="flex justify-between items-start">
                    <div>
                      <p className="font-semibold text-foreground">Vận đơn: {delivery.trackingNumber || "N/A"}</p>
                      <p className="text-xs text-muted-foreground mt-0.5">{formatDate(delivery.createdAt, locale)}</p>
                    </div>
                    <Badge variant={statusBadgeVariant(delivery.status)}>{delivery.status}</Badge>
                  </div>
                  <div className="mt-3 space-y-1 text-sm border-t border-border pt-2.5">
                    <p className="text-muted-foreground">
                      Đơn hàng:{" "}
                      <Link to={`/farmer/marketplace-orders/${delivery.marketplaceOrderId}`} className="text-primary hover:underline">
                        #{delivery.marketplaceOrderId}
                      </Link>
                    </p>
                    <p className="text-muted-foreground">Đối tác: {getProviderName(delivery.providerId)}</p>
                    <p className="text-muted-foreground">Người nhận: {delivery.recipientName} ({delivery.recipientPhone})</p>
                    <p className="text-muted-foreground">Phí ship: <span className="font-semibold text-primary">{formatVnd(delivery.shippingFeeVnd, locale)}</span></p>
                  </div>
                </Card>
              ))
            : null}
        </div>
      </div>
    </PageContainer>
  );
}
