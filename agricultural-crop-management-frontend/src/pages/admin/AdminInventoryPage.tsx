import {
  Alert,
  AlertDescription,
  AlertTitle,
  BackButton,
  Badge,
  Button,
  CardContent,
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
  Input,
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
  Skeleton,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/shared/ui";
import { adminInventoryApi } from "@/features/admin/shared/api";
import {
  AdminContentCard,
  AdminHeaderCard,
  AdminPageContainer,
} from "@/features/admin/shared/ui";
import { usePreferences } from "@/shared/contexts";
import { useDebounce } from "@/shared/lib";
import { useI18n } from "@/shared/lib/hooks/useI18n";
import { useQuery } from "@tanstack/react-query";
import { AlertCircle, MoreVertical, Search } from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import { useSearchParams } from "react-router-dom";

const WINDOW_OPTIONS = [7, 14, 30, 60, 90];
const SORT_OPTIONS = [
  { value: "EXPIRY_ASC", labelKey: "admin.inventoryRisks.sort.expiryAsc" },
  { value: "EXPIRY_DESC", labelKey: "admin.inventoryRisks.sort.expiryDesc" },
  { value: "ONHAND_DESC", labelKey: "admin.inventoryRisks.sort.onHandDesc" },
];
const STATUS_OPTIONS = [
  { value: "RISK", labelKey: "admin.inventoryRisks.status.risk" },
  { value: "EXPIRED", labelKey: "admin.inventoryRisks.status.expired" },
  { value: "EXPIRING", labelKey: "admin.inventoryRisks.status.expiring" },
  { value: "LOW_STOCK", labelKey: "admin.inventoryRisks.status.lowStock" },
  { value: "ABNORMAL_MOVEMENT", labelKey: "admin.inventoryRisks.status.abnormalMovement" },
  { value: "UNKNOWN_EXPIRY", labelKey: "admin.inventoryRisks.status.unknownExpiry" },
  { value: "ALL", labelKey: "admin.inventoryRisks.status.allLots" },
];
const SEVERITY_OPTIONS = [
  { value: "ALL", labelKey: "admin.inventoryRisks.severity.all" },
  { value: "CRITICAL", labelKey: "admin.inventoryRisks.severity.critical" },
  { value: "HIGH", labelKey: "admin.inventoryRisks.severity.high" },
  { value: "MEDIUM", labelKey: "admin.inventoryRisks.severity.medium" },
  { value: "LOW", labelKey: "admin.inventoryRisks.severity.low" },
  { value: "NONE", labelKey: "common.none" },
];

const parseNumber = (value: string | null) => {
  if (!value) return undefined;
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : undefined;
};

const formatDate = (value: string | null | undefined, locale: string) => {
  if (!value) return "-";
  try {
    return new Date(value).toLocaleDateString(locale);
  } catch {
    return value;
  }
};

export function AdminInventoryPage() {
  const { t } = useI18n();
  const { preferences } = usePreferences();
  const [searchParams, setSearchParams] = useSearchParams();
  const [page, setPage] = useState(0);
  const [detailOpen, setDetailOpen] = useState(false);
  const [selectedLotId, setSelectedLotId] = useState<number | null>(null);
  const [searchInput, setSearchInput] = useState(searchParams.get("q") ?? "");

  const farmId = parseNumber(searchParams.get("farmId"));
  const itemId = parseNumber(searchParams.get("itemId"));
  const status = (searchParams.get("status") || "RISK").toUpperCase();
  const severity = (searchParams.get("severity") || "ALL").toUpperCase();
  const windowDays = parseNumber(searchParams.get("windowDays")) ?? 30;
  const sort = (searchParams.get("sort") || "EXPIRY_ASC").toUpperCase();
  const lowStockThreshold =
    parseNumber(searchParams.get("lowStockThreshold")) ?? 5;
  const debouncedSearch = useDebounce(searchInput, 400);

  useEffect(() => {
    setPage(0);
  }, [
    farmId,
    itemId,
    status,
    severity,
    windowDays,
    sort,
    lowStockThreshold,
    debouncedSearch,
  ]);

  useEffect(() => {
    setSearchParams((prev) => {
      const next = new URLSearchParams(prev);
      if (debouncedSearch) {
        next.set("q", debouncedSearch);
      } else {
        next.delete("q");
      }
      return next;
    });
  }, [debouncedSearch, setSearchParams]);

  const optionsQuery = useQuery({
    queryKey: ["adminInventory", "options"],
    queryFn: () => adminInventoryApi.getOptions(),
  });

  const lotsQuery = useQuery({
    queryKey: [
      "adminInventory",
      "lots",
      farmId,
      itemId,
      status,
      severity,
      windowDays,
      sort,
      debouncedSearch,
      lowStockThreshold,
      page,
    ],
    queryFn: () =>
      adminInventoryApi.listRiskLots({
        farmId: farmId ?? undefined,
        itemId: itemId ?? undefined,
        status,
        severity,
        windowDays,
        q: debouncedSearch || undefined,
        sort,
        lowStockThreshold,
        page,
        limit: 20,
      }),
  });

  const detailQuery = useQuery({
    queryKey: ["adminInventory", "lotDetail", selectedLotId],
    queryFn: () => adminInventoryApi.getLotDetail(selectedLotId as number),
    enabled: selectedLotId != null,
  });

  const movementsQuery = useQuery({
    queryKey: ["adminInventory", "lotMovements", selectedLotId],
    queryFn: () => adminInventoryApi.getLotMovements(selectedLotId as number),
    enabled: selectedLotId != null,
  });

  const formatNumber = useMemo(
    () => (value: number) =>
      new Intl.NumberFormat(preferences.locale).format(value),
    [preferences.locale],
  );

  const updateParams = (
    updates: Record<string, string | number | undefined>,
  ) => {
    setSearchParams((prev) => {
      const next = new URLSearchParams(prev);
      Object.entries(updates).forEach(([key, value]) => {
        if (value === undefined || value === "") {
          next.delete(key);
        } else {
          next.set(key, String(value));
        }
      });
      return next;
    });
  };

  const showWindowDays = status === "EXPIRING" || status === "RISK";
  const showLowStockThreshold = status === "LOW_STOCK" || status === "RISK";

  const handleViewDetail = (lotId: number) => {
    setSelectedLotId(lotId);
    setDetailOpen(true);
  };
  const closeDetail = () => {
    setDetailOpen(false);
    setSelectedLotId(null);
  };

  const renderStatusBadge = (value: string) => {
    switch (value) {
      case "EXPIRED":
        return <Badge variant="destructive">{t("admin.inventoryRisks.status.expired")}</Badge>;
      case "EXPIRING":
        return (
          <Badge variant="outline" className="text-amber-600 border-amber-300">
            {t("admin.inventoryRisks.status.expiring")}
          </Badge>
        );
      case "LOW_STOCK":
        return (
          <Badge
            variant="outline"
            className="text-orange-600 border-orange-300"
          >
            {t("admin.inventoryRisks.status.lowStock")}
          </Badge>
        );
      case "ABNORMAL_MOVEMENT":
        return (
          <Badge variant="outline" className="text-red-600 border-red-300">
            {t("admin.inventoryRisks.status.abnormalMovement")}
          </Badge>
        );
      case "UNKNOWN_EXPIRY":
        return <Badge variant="secondary">{t("admin.inventoryRisks.status.unknownExpiry")}</Badge>;
      case "HEALTHY":
        return <Badge variant="secondary">{t("admin.inventoryRisks.status.healthy")}</Badge>;
      default:
        return <Badge variant="outline">{value}</Badge>;
    }
  };

  const renderSeverityBadge = (value: string | null | undefined) => {
    switch (value) {
      case "CRITICAL":
        return <Badge variant="destructive">{t("admin.inventoryRisks.severity.critical")}</Badge>;
      case "HIGH":
        return (
          <Badge variant="outline" className="text-red-600 border-red-300">
            {t("admin.inventoryRisks.severity.high")}
          </Badge>
        );
      case "MEDIUM":
        return (
          <Badge variant="outline" className="text-amber-600 border-amber-300">
            {t("admin.inventoryRisks.severity.medium")}
          </Badge>
        );
      case "LOW":
        return <Badge variant="secondary">{t("admin.inventoryRisks.severity.low")}</Badge>;
      default:
        return <Badge variant="outline">{value || t("common.none")}</Badge>;
    }
  };

  return (
    <AdminPageContainer>
      <AdminHeaderCard
        title={t("admin.inventoryRisks.title")}
        description={t("admin.inventoryRisks.subtitle")}
      />

      <AdminContentCard>
        <CardContent className="space-y-4 p-4">
          <div className="flex flex-col items-stretch gap-3 sm:flex-row sm:flex-wrap sm:items-center">
            <Select
              value={farmId?.toString() ?? "all"}
              onValueChange={(value) =>
                updateParams({ farmId: value === "all" ? undefined : value })
              }
            >
              <SelectTrigger className="h-9 w-full rounded-[14px] sm:w-[220px]">
                <SelectValue placeholder={t("admin.alerts.farms.all")} />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">{t("admin.alerts.farms.all")}</SelectItem>
                {(optionsQuery.data?.farms ?? []).map((farm) => (
                  <SelectItem key={farm.id} value={String(farm.id)}>
                    {farm.name}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>

            <Select
              value={itemId?.toString() ?? "all"}
              onValueChange={(value) =>
                updateParams({ itemId: value === "all" ? undefined : value })
              }
            >
              <SelectTrigger className="h-9 w-full rounded-[14px] sm:w-[220px]">
                <SelectValue placeholder={t("admin.inventoryRisks.filters.allItems")} />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">{t("admin.inventoryRisks.filters.allItems")}</SelectItem>
                {(optionsQuery.data?.items ?? []).map((item) => (
                  <SelectItem key={item.id} value={String(item.id)}>
                    {item.name}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>

            <Select
              value={status}
              onValueChange={(value) => updateParams({ status: value })}
            >
              <SelectTrigger className="h-9 w-full rounded-[14px] sm:w-[170px]">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {STATUS_OPTIONS.map((option) => (
                  <SelectItem key={option.value} value={option.value}>
                    {t(option.labelKey)}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>

            <Select
              value={severity}
              onValueChange={(value) => updateParams({ severity: value })}
            >
              <SelectTrigger className="h-9 w-full rounded-[14px] sm:w-[170px]">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {SEVERITY_OPTIONS.map((option) => (
                  <SelectItem key={option.value} value={option.value}>
                    {t(option.labelKey)}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>

            {showWindowDays && (
              <Select
                value={String(windowDays)}
                onValueChange={(value) => updateParams({ windowDays: value })}
              >
                <SelectTrigger className="h-9 w-full rounded-[14px] sm:w-[120px]">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {WINDOW_OPTIONS.map((option) => (
                    <SelectItem key={option} value={String(option)}>
                      {t("admin.inventoryRisks.filters.days", { count: option })}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            )}

            {showLowStockThreshold && (
              <div className="flex items-center gap-2 w-full sm:w-auto">
                <Input
                  type="number"
                  min={1}
                  value={lowStockThreshold}
                  onChange={(event) =>
                    updateParams({
                      lowStockThreshold: event.target.value || undefined,
                    })
                  }
                  className="h-9 w-full rounded-[14px] sm:w-[120px]"
                />
                <span className="text-xs text-muted-foreground">
                  {t("admin.inventoryRisks.filters.lowStockThreshold")}
                </span>
              </div>
            )}

            <Select
              value={sort}
              onValueChange={(value) => updateParams({ sort: value })}
            >
              <SelectTrigger className="h-9 w-full rounded-[14px] sm:w-[190px]">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {SORT_OPTIONS.map((option) => (
                  <SelectItem key={option.value} value={option.value}>
                    {t(option.labelKey)}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>

            <div className="relative w-full sm:w-[260px]">
              <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
              <Input
                placeholder={t("admin.inventoryRisks.searchPlaceholder")}
                className="h-9 rounded-[14px] pl-9"
                value={searchInput}
                onChange={(event) => setSearchInput(event.target.value)}
              />
            </div>
          </div>

          {lotsQuery.isLoading && (
            <div className="space-y-3">
              {[...Array(5)].map((_, i) => (
                <Skeleton key={i} className="h-10 w-full" />
              ))}
            </div>
          )}

          {lotsQuery.isError && (
            <Alert variant="destructive">
              <AlertCircle className="h-4 w-4" />
              <AlertTitle>{t("admin.inventoryRisks.error.loadLots")}</AlertTitle>
              <AlertDescription className="mt-2 flex items-center justify-between gap-3">
                <span>{lotsQuery.error?.message || t("common.error.description")}</span>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => lotsQuery.refetch()}
                >
                  {t("common.retry")}
                </Button>
              </AlertDescription>
            </Alert>
          )}

          {!lotsQuery.isLoading && !lotsQuery.isError && (
            <>
              {lotsQuery.data?.items?.length ? (
                <>
                  <Table>
                    <TableHeader>
                      <TableRow>
                        <TableHead>{t("admin.inventoryRisks.table.itemLot")}</TableHead>
                        <TableHead>{t("admin.farmsPlots.table.farm")}</TableHead>
                        <TableHead>{t("admin.inventoryRisks.table.expiry")}</TableHead>
                        <TableHead className="text-right">{t("admin.inventoryRisks.table.onHand")}</TableHead>
                        <TableHead>{t("admin.inventoryRisks.table.daysToExpiry")}</TableHead>
                        <TableHead>{t("common.status")}</TableHead>
                        <TableHead>{t("admin.inventoryRisks.table.severity")}</TableHead>
                        <TableHead className="text-right">{t("common.actions")}</TableHead>
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                      {lotsQuery.data.items.map((lot) => (
                        <TableRow key={lot.lotId}>
                          <TableCell>
                            <div className="font-medium">{lot.itemName}</div>
                            <div className="text-xs text-muted-foreground">
                              {lot.lotCode || t("admin.inventoryRisks.noLotCode")}
                            </div>
                          </TableCell>
                          <TableCell>{lot.farmName || "-"}</TableCell>
                          <TableCell>
                            {formatDate(lot.expiryDate, preferences.locale)}
                          </TableCell>
                          <TableCell className="text-right font-mono">
                            {formatNumber(Number(lot.onHand))} {lot.unit || ""}
                          </TableCell>
                          <TableCell>{lot.daysToExpiry ?? "-"}</TableCell>
                          <TableCell>{renderStatusBadge(lot.status)}</TableCell>
                          <TableCell>
                            {renderSeverityBadge(lot.severity)}
                          </TableCell>
                          <TableCell className="text-right">
                            <DropdownMenu>
                              <DropdownMenuTrigger asChild>
                                <Button
                                  type="button"
                                  variant="ghost"
                                  size="icon"
                                  className="h-8 w-8 rounded-[14px]"
                                  aria-label={t("admin.farmsPlots.actionsFor", { name: lot.itemName })}
                                >
                                  <MoreVertical className="h-4 w-4" />
                                </Button>
                              </DropdownMenuTrigger>
                              <DropdownMenuContent align="end" className="w-40">
                                <DropdownMenuItem
                                  onSelect={() => handleViewDetail(lot.lotId)}
                                >
                                  {t("admin.farmsPlots.actions.viewDetails")}
                                </DropdownMenuItem>
                              </DropdownMenuContent>
                            </DropdownMenu>
                          </TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                  <div className="flex flex-col-reverse sm:flex-row sm:items-center sm:justify-end gap-2 mt-4">
                    <Button
                      variant="outline"
                      size="sm"
                      disabled={page === 0}
                      onClick={() => setPage((prev) => Math.max(prev - 1, 0))}
                    >
                      {t("pagination.previousPage")}
                    </Button>
                    <Button
                      variant="outline"
                      size="sm"
                      disabled={page >= (lotsQuery.data?.totalPages ?? 1) - 1}
                      onClick={() => setPage((prev) => prev + 1)}
                    >
                      {t("pagination.nextPage")}
                    </Button>
                  </div>
                </>
              ) : (
                <div className="py-10 text-center text-sm text-muted-foreground">
                  {t("admin.inventoryRisks.empty")}
                </div>
              )}
            </>
          )}
        </CardContent>
      </AdminContentCard>

      {detailOpen && (
        <div
          className="fixed inset-0 z-50 bg-background/80 backdrop-blur-sm cursor-pointer"
          onClick={closeDetail}
        >
          <div
            className="fixed right-0 top-0 h-full w-full max-w-xl bg-card border-l border-border shadow-lg overflow-auto cursor-default"
            onClick={(event) => event.stopPropagation()}
          >
            <div className="p-6 space-y-6">
              <div className="flex items-center justify-between">
                <div>
                  <h2 className="text-lg font-semibold">{t("admin.inventoryRisks.detail.title")}</h2>
                  <p className="text-sm text-muted-foreground">
                    {detailQuery.data?.itemName || t("admin.inventoryRisks.detail.fallback")}
                  </p>
                </div>
                <BackButton onClick={closeDetail} />
              </div>

              {detailQuery.isLoading && <Skeleton className="h-32 w-full" />}
              {detailQuery.isError && (
                <Alert variant="destructive">
                  <AlertCircle className="h-4 w-4" />
                  <AlertTitle>{t("admin.inventoryRisks.error.loadDetail")}</AlertTitle>
                  <AlertDescription>
                    {detailQuery.error?.message}
                  </AlertDescription>
                </Alert>
              )}

              {detailQuery.data && (
                <div className="space-y-4">
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                    <div>
                      <p className="text-xs text-muted-foreground">{t("admin.inventoryRisks.detail.lotCode")}</p>
                      <p className="text-sm font-medium">
                        {detailQuery.data.lotCode || "-"}
                      </p>
                    </div>
                    <div>
                      <p className="text-xs text-muted-foreground">{t("admin.inventoryRisks.detail.supplier")}</p>
                      <p className="text-sm font-medium">
                        {detailQuery.data.supplierName || "-"}
                      </p>
                    </div>
                    <div>
                      <p className="text-xs text-muted-foreground">{t("admin.inventoryRisks.table.expiry")}</p>
                      <p className="text-sm font-medium">
                        {formatDate(
                          detailQuery.data.expiryDate,
                          preferences.locale,
                        )}
                      </p>
                    </div>
                    <div>
                      <p className="text-xs text-muted-foreground">{t("admin.inventoryRisks.table.onHand")}</p>
                      <p className="text-sm font-medium">
                        {formatNumber(
                          Number(detailQuery.data.onHandTotal || 0),
                        )}{" "}
                        {detailQuery.data.unit || ""}
                      </p>
                    </div>
                  </div>

                  <div>
                    <p className="text-sm font-semibold mb-2">{t("admin.inventoryRisks.detail.balances")}</p>
                    {detailQuery.data.balances.length ? (
                      <Table>
                        <TableHeader>
                          <TableRow>
                            <TableHead>{t("admin.inventoryRisks.detail.warehouse")}</TableHead>
                            <TableHead>{t("admin.farmsPlots.table.location")}</TableHead>
                            <TableHead className="text-right">
                              {t("admin.inventoryRisks.detail.quantity")}
                            </TableHead>
                          </TableRow>
                        </TableHeader>
                        <TableBody>
                          {detailQuery.data.balances.map((balance, index) => (
                            <TableRow
                              key={`${balance.warehouseId}-${balance.locationId}-${index}`}
                            >
                              <TableCell>
                                {balance.warehouseName || "-"}
                              </TableCell>
                              <TableCell>
                                {balance.locationLabel || "-"}
                              </TableCell>
                              <TableCell className="text-right font-mono">
                                {formatNumber(Number(balance.quantity))}
                              </TableCell>
                            </TableRow>
                          ))}
                        </TableBody>
                      </Table>
                    ) : (
                      <p className="text-sm text-muted-foreground">
                        {t("admin.inventoryRisks.detail.noBalances")}
                      </p>
                    )}
                  </div>
                </div>
              )}

              <div>
                <p className="text-sm font-semibold mb-2">{t("admin.inventoryRisks.detail.movements")}</p>
                {movementsQuery.isLoading && (
                  <Skeleton className="h-28 w-full" />
                )}
                {movementsQuery.isError && (
                  <Alert variant="destructive">
                    <AlertCircle className="h-4 w-4" />
                    <AlertTitle>{t("admin.inventoryRisks.error.loadMovements")}</AlertTitle>
                  </Alert>
                )}
                {movementsQuery.data &&
                  (movementsQuery.data.length ? (
                    <Table>
                      <TableHeader>
                        <TableRow>
                          <TableHead>{t("common.date")}</TableHead>
                          <TableHead>{t("common.type")}</TableHead>
                          <TableHead className="text-right">{t("admin.inventoryRisks.detail.qty")}</TableHead>
                          <TableHead>{t("admin.inventoryRisks.detail.reference")}</TableHead>
                        </TableRow>
                      </TableHeader>
                      <TableBody>
                        {movementsQuery.data.map((movement) => (
                          <TableRow key={movement.movementId}>
                            <TableCell>
                              {formatDate(
                                movement.movementDate,
                                preferences.locale,
                              )}
                            </TableCell>
                            <TableCell>
                              {movement.movementType || "-"}
                            </TableCell>
                            <TableCell className="text-right font-mono">
                              {formatNumber(Number(movement.quantity))}
                            </TableCell>
                            <TableCell>
                              {movement.reference || movement.note || "-"}
                            </TableCell>
                          </TableRow>
                        ))}
                      </TableBody>
                    </Table>
                  ) : (
                    <p className="text-sm text-muted-foreground">
                      {t("admin.inventoryRisks.detail.noMovements")}
                    </p>
                  ))}
              </div>
            </div>
          </div>
        </div>
      )}
    </AdminPageContainer>
  );
}
