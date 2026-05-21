import {
  useCreateWarehouse,
  useDeleteWarehouse,
  useLocations,
  useMovements,
  useMyWarehouses,
  useOnHandList,
  useRecordStockMovement,
  useUpdateWarehouse,
  type MovementsParams,
  type OnHandParams,
  type OnHandRow,
  type StockMovementRequest,
  type StockMovement,
  type Warehouse as WarehouseEntity,
} from "@/entities/inventory";
import { useFarms } from "@/entities/farm";
import type { WeightUnit } from "@/entities/preferences";
import { useI18n } from "@/hooks/useI18n";
import { usePreferences } from "@/shared/contexts";
import {
  convertWeight,
  convertWeightToKg,
  getWeightUnitLabel,
  normalizeWeightUnit,
} from "@/shared/lib";
import {
  Button,
  Card,
  CardContent,
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
  PageContainer,
  PageHeader,
} from "@/shared/ui";
import {
  MoreVertical,
  Pencil,
  Plus,
  Trash2,
  Warehouse as WarehouseIcon,
} from "lucide-react";
import { useMemo, useState } from "react";
import { toast } from "sonner";
import "./InventoryPage.css";

// ═══════════════════════════════════════════════════════════════
// INVENTORY PAGE COMPONENT
// ═══════════════════════════════════════════════════════════════

type TabType = "on-hand" | "movements";
type WarehouseDialogMode = "create" | "edit";
type TranslateFn = ReturnType<typeof useI18n>["t"];

const formatNumber = (
  value: number,
  locale: string,
  maximumFractionDigits?: number,
) => new Intl.NumberFormat(locale, { maximumFractionDigits }).format(value);

const getDisplayQuantity = (
  quantity: number,
  unit: string | null | undefined,
  displayUnit: WeightUnit,
  locale: string,
) => {
  const normalizedUnit = normalizeWeightUnit(unit);
  if (!normalizedUnit) {
    return {
      formatted: formatNumber(quantity, locale),
      displayValue: quantity,
      unitLabel: unit ?? "",
      normalizedUnit: null,
    };
  }

  const valueKg = convertWeightToKg(quantity, normalizedUnit);
  const displayValue = convertWeight(valueKg, displayUnit);
  const formatted = formatNumber(
    displayValue,
    locale,
    displayUnit === "G" ? 0 : 2,
  );

  return {
    formatted,
    displayValue,
    unitLabel: getWeightUnitLabel(displayUnit),
    normalizedUnit,
  };
};

const toRowUnitQuantity = (
  quantity: number,
  rowUnit: WeightUnit,
  displayUnit: WeightUnit,
) => convertWeight(convertWeightToKg(quantity, displayUnit), rowUnit);

const getLotStatusMeta = (
  status: string | null | undefined,
  t: TranslateFn,
): { label: string; className: string } => {
  const normalized = status?.trim().toUpperCase();
  switch (normalized) {
    case "IN_STOCK":
      return { label: t("inventory.status.inStock"), className: "status-in-stock" };
    case "LOW_STOCK":
      return { label: t("inventory.status.lowStock"), className: "status-low-stock" };
    case "EXPIRED":
      return { label: t("inventory.status.expired"), className: "status-expired" };
    case "OUT_OF_STOCK":
    case "DEPLETED":
      return { label: t("inventory.status.outOfStock"), className: "status-out-of-stock" };
    case "HOLD":
    case "RESERVED":
    case "PENDING":
      return { label: t("inventory.status.hold"), className: "status-hold" };
    default:
      return {
        label: status?.trim() || "-",
        className: "status-neutral",
      };
  }
};

const getMovementTypeMeta = (
  movementType: string | null | undefined,
  t: TranslateFn,
): { label: string; className: string } => {
  switch (movementType) {
    case "IN":
      return { label: t("inventory.types.in"), className: "movement-in" };
    case "OUT":
      return { label: t("inventory.types.out"), className: "movement-out" };
    case "ADJUST":
      return { label: t("inventory.types.adjust"), className: "movement-adjust" };
    default:
      return { label: movementType || "-", className: "" };
  }
};

const getLocationDisplayLabel = (
  locationLabel: string | null | undefined,
  t: TranslateFn,
): string => {
  const normalized = locationLabel?.trim().toLowerCase();
  if (!normalized || normalized === "any location") {
    return t("inventory.unassignedLocation");
  }
  return locationLabel ?? t("inventory.unassignedLocation");
};

export function InventoryPage() {
  const { t } = useI18n();
  const [activeTab, setActiveTab] = useState<TabType>("on-hand");
  const [selectedWarehouseId, setSelectedWarehouseId] = useState<
    number | undefined
  >(undefined);
  const [selectedLocationId, setSelectedLocationId] = useState<
    number | undefined
  >(undefined);
  const [searchQuery, setSearchQuery] = useState("");
  const [movementTypeFilter, setMovementTypeFilter] = useState<string>("");
  const [dateFrom, setDateFrom] = useState("");
  const [dateTo, setDateTo] = useState("");
  const [page, setPage] = useState(0);
  const [warehouseDialogMode, setWarehouseDialogMode] =
    useState<WarehouseDialogMode | null>(null);
  const [warehouseNameInput, setWarehouseNameInput] = useState("");
  const [warehouseFarmIdInput, setWarehouseFarmIdInput] = useState<
    number | undefined
  >(undefined);
  const [warehouseFormError, setWarehouseFormError] = useState("");
  const [showDeleteWarehouseModal, setShowDeleteWarehouseModal] =
    useState(false);

  // Stock Out Modal State
  const [showStockOutModal, setShowStockOutModal] = useState(false);
  const [stockOutRow, setStockOutRow] = useState<OnHandRow | null>(null);

  // Adjust Modal State
  const [showAdjustModal, setShowAdjustModal] = useState(false);
  const [adjustRow, setAdjustRow] = useState<OnHandRow | null>(null);

  const { preferences } = usePreferences();

  // ===== QUERIES =====
  const { data: warehouses, isLoading: loadingWarehouses } =
    useMyWarehouses("INPUT");
  const { data: farmsData } = useFarms({ active: true, page: 0, size: 200 });
  const supplyWarehouses = useMemo(
    () => warehouses ?? [],
    [warehouses],
  );
  const farmOptions = useMemo(
    () => farmsData?.content ?? [],
    [farmsData?.content],
  );
  const selectedWarehouse = useMemo(
    () =>
      supplyWarehouses.find((warehouse) => warehouse.id === selectedWarehouseId) ??
      null,
    [selectedWarehouseId, supplyWarehouses],
  );
  const { data: locations } = useLocations(selectedWarehouseId);

  const onHandParams: OnHandParams | undefined = selectedWarehouseId
    ? {
        warehouseId: selectedWarehouseId,
        locationId: selectedLocationId,
        q: searchQuery || undefined,
        page,
        size: 20,
      }
    : undefined;

  const movementsParams: MovementsParams | undefined = selectedWarehouseId
    ? {
        warehouseId: selectedWarehouseId,
        type: movementTypeFilter || undefined,
        from: dateFrom || undefined,
        to: dateTo || undefined,
        page,
        size: 20,
      }
    : undefined;

  const { data: onHandData, isLoading: loadingOnHand } =
    useOnHandList(onHandParams);
  const { data: movementsData, isLoading: loadingMovements } =
    useMovements(movementsParams);
  const recordMovementMutation = useRecordStockMovement();
  const createWarehouseMutation = useCreateWarehouse();
  const updateWarehouseMutation = useUpdateWarehouse();
  const deleteWarehouseMutation = useDeleteWarehouse();

  // Auto-select first warehouse when warehouses load
  if (
    supplyWarehouses.length > 0 &&
    (!selectedWarehouseId ||
      !supplyWarehouses.some((warehouse) => warehouse.id === selectedWarehouseId))
  ) {
    setSelectedWarehouseId(supplyWarehouses[0].id);
  }
  if (supplyWarehouses.length === 0 && selectedWarehouseId) {
    setSelectedWarehouseId(undefined);
  }

  // ===== HANDLERS =====
  const handleWarehouseChange = (warehouseId: number) => {
    setSelectedWarehouseId(warehouseId);
    setSelectedLocationId(undefined);
    setPage(0);
  };

  const openCreateWarehouseModal = () => {
    setWarehouseDialogMode("create");
    setWarehouseNameInput("");
    setWarehouseFarmIdInput(selectedWarehouse?.farmId ?? farmOptions[0]?.id);
    setWarehouseFormError("");
  };

  const openEditWarehouseModal = () => {
    if (!selectedWarehouse) return;
    setWarehouseDialogMode("edit");
    setWarehouseNameInput(selectedWarehouse.name ?? "");
    setWarehouseFarmIdInput(selectedWarehouse.farmId ?? undefined);
    setWarehouseFormError("");
  };

  const closeWarehouseFormModal = () => {
    if (createWarehouseMutation.isPending || updateWarehouseMutation.isPending) {
      return;
    }
    setWarehouseDialogMode(null);
    setWarehouseNameInput("");
    setWarehouseFarmIdInput(undefined);
    setWarehouseFormError("");
  };

  const handleSubmitWarehouseForm = async () => {
    const trimmedName = warehouseNameInput.trim();
    if (!trimmedName) {
      setWarehouseFormError(t("inventory.validation.warehouseNameRequired"));
      return;
    }

    const farmId =
      warehouseDialogMode === "create"
        ? warehouseFarmIdInput
        : selectedWarehouse?.farmId;
    if (!farmId) {
      setWarehouseFormError(t("inventory.validation.farmRequired"));
      return;
    }

    setWarehouseFormError("");
    try {
      if (warehouseDialogMode === "create") {
        await createWarehouseMutation.mutateAsync({
          name: trimmedName,
          farmId,
          type: "INPUT",
        });
        toast.success(t("inventory.toast.warehouseCreateSuccess"));
      } else if (warehouseDialogMode === "edit" && selectedWarehouse) {
        await updateWarehouseMutation.mutateAsync({
          id: selectedWarehouse.id,
          data: {
            name: trimmedName,
            farmId,
          },
        });
        toast.success(t("inventory.toast.warehouseUpdateSuccess"));
      }
      closeWarehouseFormModal();
    } catch (error) {
      setWarehouseFormError(
        error instanceof Error
          ? error.message
          : t("inventory.toast.warehouseSaveError"),
      );
    }
  };

  const confirmDeleteWarehouse = async () => {
    if (!selectedWarehouseId) return;
    try {
      await deleteWarehouseMutation.mutateAsync(selectedWarehouseId);
      toast.success(t("inventory.toast.warehouseDeleteSuccess"));
      setShowDeleteWarehouseModal(false);
      setSelectedWarehouseId(undefined);
      setSelectedLocationId(undefined);
      setPage(0);
    } catch (error) {
      toast.error(
        error instanceof Error
          ? error.message
          : t("inventory.toast.warehouseDeleteError"),
      );
    }
  };

  const handleStockOut = (row: OnHandRow) => {
    setStockOutRow(row);
    setShowStockOutModal(true);
  };

  const handleAdjust = (row: OnHandRow) => {
    setAdjustRow(row);
    setShowAdjustModal(true);
  };

  const formatDate = (dateStr: string | null | undefined): string => {
    if (!dateStr) return "-";
    try {
      const date = new Date(dateStr);
      return date.toLocaleDateString(preferences.locale);
    } catch {
      return dateStr;
    }
  };

  const formatDateTime = (dateStr: string | null | undefined): string => {
    if (!dateStr) return "-";
    try {
      const date = new Date(dateStr);
      return date.toLocaleString(preferences.locale);
    } catch {
      return dateStr;
    }
  };

  // ===== RENDER =====
  return (
    <PageContainer variant="wide">
      <div className="farmer-inventory-page">
        <Card variant="page-header" className="mb-6">
          <CardContent className="px-6 py-4">
            <PageHeader
              className="mb-0"
              icon={<WarehouseIcon className="w-8 h-8" />}
              title={t("inventory.title")}
              subtitle={t("inventory.subtitle")}
            />
          </CardContent>
        </Card>

        {/* ===== CONTROLS ===== */}
        <Card variant="filter" className="mb-6">
          <CardContent className="px-6 py-4">
            <div className="inventory-controls flex flex-wrap items-center justify-start gap-4">
              <div className="control-group">
                <label htmlFor="warehouse-select">
                  {t("inventory.warehouse")}
                </label>
                <select
                  id="warehouse-select"
                  value={selectedWarehouseId || ""}
                  onChange={(e) =>
                    handleWarehouseChange(Number(e.target.value))
                  }
                  disabled={loadingWarehouses}
                >
                  {loadingWarehouses && (
                    <option>{t("inventory.loading")}</option>
                  )}
                  {!loadingWarehouses &&
                    supplyWarehouses.length === 0 && (
                      <option value="">{t("inventory.noWarehouses")}</option>
                    )}
                  {supplyWarehouses.map((w: WarehouseEntity) => (
                    <option key={w.id} value={w.id}>
                      {w.name} {w.farmName ? `(${w.farmName})` : ""}
                    </option>
                  ))}
                </select>
              </div>

              <div className="inventory-warehouse-toolbar">
                <Button
                  type="button"
                  variant="default"
                  size="sm"
                  onClick={openCreateWarehouseModal}
                >
                  <Plus className="w-4 h-4" />
                  {t("inventory.actions.addWarehouse")}
                </Button>
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  onClick={openEditWarehouseModal}
                  disabled={!selectedWarehouse}
                >
                  <Pencil className="w-4 h-4" />
                  {t("inventory.actions.editWarehouse")}
                </Button>
                <Button
                  type="button"
                  variant="destructive"
                  size="sm"
                  onClick={() => setShowDeleteWarehouseModal(true)}
                  disabled={!selectedWarehouse}
                >
                  <Trash2 className="w-4 h-4" />
                  {t("inventory.actions.deleteWarehouse")}
                </Button>
              </div>

              <div className="control-group">
                <label htmlFor="location-select">
                  {t("inventory.location")}
                </label>
                <select
                  id="location-select"
                  value={selectedLocationId || ""}
                  onChange={(e) =>
                    setSelectedLocationId(
                      e.target.value ? Number(e.target.value) : undefined,
                    )
                  }
                >
                  <option value="">{t("inventory.allLocations")}</option>
                  {locations?.map((loc) => (
                    <option key={loc.id} value={loc.id}>
                      {loc.label || t("inventory.locationFallback", { id: loc.id })}
                    </option>
                  ))}
                </select>
              </div>

              {activeTab === "on-hand" && (
                <div className="control-group control-group--search">
                  <label htmlFor="search-input">{t("common.search")}</label>
                  <input
                    id="search-input"
                    type="text"
                    placeholder={t("inventory.searchPlaceholder")}
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                  />
                </div>
              )}

              {activeTab === "movements" && (
                <>
                  <div className="control-group">
                    <label htmlFor="type-filter">
                      {t("inventory.movementType")}
                    </label>
                    <select
                      id="type-filter"
                      value={movementTypeFilter}
                      onChange={(e) => setMovementTypeFilter(e.target.value)}
                    >
                      <option value="">{t("inventory.allTypes")}</option>
                      <option value="IN">{t("inventory.types.in")}</option>
                      <option value="OUT">{t("inventory.types.out")}</option>
                      <option value="ADJUST">
                        {t("inventory.types.adjust")}
                      </option>
                    </select>
                  </div>
                  <div className="control-group">
                    <label htmlFor="date-from">{t("common.from")}</label>
                    <input
                      id="date-from"
                      type="date"
                      value={dateFrom}
                      onChange={(e) => setDateFrom(e.target.value)}
                    />
                  </div>
                  <div className="control-group">
                    <label htmlFor="date-to">{t("common.to")}</label>
                    <input
                      id="date-to"
                      type="date"
                      value={dateTo}
                      onChange={(e) => setDateTo(e.target.value)}
                    />
                  </div>
                </>
              )}
            </div>
          </CardContent>
        </Card>

        {/* ===== TABS ===== */}
        <div
          className="inventory-tabs"
          role="tablist"
          aria-label={t("inventory.tabsAriaLabel")}
        >
          <button
            type="button"
            id="inventory-tab-on-hand"
            role="tab"
            aria-controls="inventory-tabpanel-on-hand"
            aria-selected={activeTab === "on-hand"}
            className={`tab ${activeTab === "on-hand" ? "active" : ""}`}
            onClick={() => {
              setActiveTab("on-hand");
              setPage(0);
            }}
          >
            {t("inventory.tabs.onHand")}
          </button>
          <button
            type="button"
            id="inventory-tab-movements"
            role="tab"
            aria-controls="inventory-tabpanel-movements"
            aria-selected={activeTab === "movements"}
            className={`tab ${activeTab === "movements" ? "active" : ""}`}
            onClick={() => {
              setActiveTab("movements");
              setPage(0);
            }}
          >
            {t("inventory.tabs.movements")}
          </button>
        </div>

        {/* ===== CONTENT ===== */}
        <div className="inventory-content">
          {!selectedWarehouseId && (
            <div className="empty-state">
              <p>{t("inventory.selectWarehouse")}</p>
            </div>
          )}

          {selectedWarehouseId && activeTab === "on-hand" && (
            <div
              id="inventory-tabpanel-on-hand"
              role="tabpanel"
              aria-labelledby="inventory-tab-on-hand"
            >
              <OnHandTable
                data={onHandData?.items || []}
                loading={loadingOnHand}
                onStockOut={handleStockOut}
                onAdjust={handleAdjust}
                formatDate={formatDate}
              />
            </div>
          )}

          {selectedWarehouseId && activeTab === "movements" && (
            <div
              id="inventory-tabpanel-movements"
              role="tabpanel"
              aria-labelledby="inventory-tab-movements"
            >
              <MovementsTable
                data={movementsData?.items || []}
                loading={loadingMovements}
                formatDateTime={formatDateTime}
              />
            </div>
          )}

          {/* Pagination */}
          {selectedWarehouseId && (
            <div className="pagination">
              <button
                disabled={page === 0}
                onClick={() => setPage((p) => p - 1)}
              >
                {t("common.previous")}
              </button>
              <span>
                {t("pagination.page")} {page + 1}
              </span>
              <button
                disabled={
                  activeTab === "on-hand"
                    ? !onHandData || page >= onHandData.totalPages - 1
                    : !movementsData || page >= movementsData.totalPages - 1
                }
                onClick={() => setPage((p) => p + 1)}
              >
                {t("common.next")}
              </button>
            </div>
          )}
        </div>

        {/* ===== MODALS ===== */}
        {warehouseDialogMode && (
          <WarehouseFormModal
            mode={warehouseDialogMode}
            name={warehouseNameInput}
            farmId={warehouseFarmIdInput}
            farms={farmOptions.map((farm) => ({ id: farm.id, name: farm.name }))}
            onNameChange={setWarehouseNameInput}
            onFarmIdChange={setWarehouseFarmIdInput}
            onClose={closeWarehouseFormModal}
            onSubmit={handleSubmitWarehouseForm}
            isPending={
              createWarehouseMutation.isPending || updateWarehouseMutation.isPending
            }
            error={warehouseFormError}
          />
        )}

        {showDeleteWarehouseModal && selectedWarehouse && (
          <DeleteWarehouseModal
            warehouseName={selectedWarehouse.name}
            onClose={() => {
              if (!deleteWarehouseMutation.isPending) {
                setShowDeleteWarehouseModal(false);
              }
            }}
            onConfirm={confirmDeleteWarehouse}
            isPending={deleteWarehouseMutation.isPending}
          />
        )}

        {showStockOutModal && stockOutRow && (
          <StockOutModal
            row={stockOutRow}
            onClose={() => {
              setShowStockOutModal(false);
              setStockOutRow(null);
            }}
            onSubmit={async (data) => {
              await recordMovementMutation.mutateAsync(data);
              setShowStockOutModal(false);
              setStockOutRow(null);
            }}
            isPending={recordMovementMutation.isPending}
          />
        )}

        {showAdjustModal && adjustRow && (
          <AdjustModal
            row={adjustRow}
            onClose={() => {
              setShowAdjustModal(false);
              setAdjustRow(null);
            }}
            onSubmit={async (data) => {
              await recordMovementMutation.mutateAsync(data);
              setShowAdjustModal(false);
              setAdjustRow(null);
            }}
            isPending={recordMovementMutation.isPending}
          />
        )}
      </div>
    </PageContainer>
  );
}

// ═══════════════════════════════════════════════════════════════
// ON-HAND TABLE COMPONENT
// ═══════════════════════════════════════════════════════════════

interface OnHandTableProps {
  data: OnHandRow[];
  loading: boolean;
  onStockOut: (row: OnHandRow) => void;
  onAdjust: (row: OnHandRow) => void;
  formatDate: (date: string | null | undefined) => string;
}

function OnHandTable({
  data,
  loading,
  onStockOut,
  onAdjust,
  formatDate,
}: OnHandTableProps) {
  const { preferences } = usePreferences();
  const { t } = useI18n();

  if (loading) {
    return <div className="loading-state">{t("inventory.loading")}</div>;
  }

  if (data.length === 0) {
    return <div className="empty-state">{t("inventory.noStock")}</div>;
  }

  return (
    <div className="table-container">
      <table className="inventory-table">
        <thead>
          <tr>
            <th>{t("inventory.table.lotCode")}</th>
            <th>{t("inventory.table.item")}</th>
            <th>{t("inventory.table.unit")}</th>
            <th>{t("inventory.table.expiryDate")}</th>
            <th>{t("inventory.table.location")}</th>
            <th>{t("inventory.table.onHand")}</th>
            <th>{t("inventory.table.status")}</th>
            <th>{t("inventory.table.actions")}</th>
          </tr>
        </thead>
        <tbody>
          {data.map((row) => {
            const display = getDisplayQuantity(
              row.onHandQuantity,
              row.unit,
              preferences.weightUnit,
              preferences.locale,
            );

            return (
              <tr key={`${row.supplyLotId}-${row.locationId || "any"}`}>
                <td>{row.batchCode || "-"}</td>
                <td>{row.supplyItemName || "-"}</td>
                <td>{display.unitLabel || "-"}</td>
                <td>{formatDate(row.expiryDate)}</td>
                <td>{getLocationDisplayLabel(row.locationLabel, t)}</td>
                <td className="quantity">{display.formatted}</td>
                <td>
                  {(() => {
                    const statusMeta = getLotStatusMeta(row.lotStatus, t);
                    return (
                      <span className={`status-badge ${statusMeta.className}`}>
                        {statusMeta.label}
                      </span>
                    );
                  })()}
                </td>
                <td className="actions-cell">
                  <DropdownMenu>
                    <DropdownMenuTrigger asChild>
                      <button
                        type="button"
                        className="action-btn"
                        aria-label={t("inventory.actions.menu")}
                        title={t("inventory.actions.menu")}
                      >
                        <MoreVertical className="w-4 h-4" />
                      </button>
                    </DropdownMenuTrigger>
                    <DropdownMenuContent align="end">
                      <DropdownMenuItem
                        variant="destructive"
                        onClick={() => onStockOut(row)}
                      >
                        {t("inventory.actions.stockOut")}
                      </DropdownMenuItem>
                      <DropdownMenuItem onClick={() => onAdjust(row)}>
                        {t("inventory.actions.adjust")}
                      </DropdownMenuItem>
                    </DropdownMenuContent>
                  </DropdownMenu>
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}

// ═══════════════════════════════════════════════════════════════
// MOVEMENTS TABLE COMPONENT
// ═══════════════════════════════════════════════════════════════

interface MovementsTableProps {
  data: StockMovement[];
  loading: boolean;
  formatDateTime: (date: string | null | undefined) => string;
}

function MovementsTable({
  data,
  loading,
  formatDateTime,
}: MovementsTableProps) {
  const { preferences } = usePreferences();
  const { t } = useI18n();

  if (loading) {
    return (
      <div className="loading-state">{t("inventory.loadingMovements")}</div>
    );
  }

  if (data.length === 0) {
    return <div className="empty-state">{t("inventory.noMovements")}</div>;
  }

  return (
    <div className="table-container">
      <table className="inventory-table">
        <thead>
          <tr>
            <th>{t("inventory.movementsTable.date")}</th>
            <th>{t("inventory.movementsTable.type")}</th>
            <th>{t("inventory.movementsTable.quantity")}</th>
            <th>{t("inventory.movementsTable.lotCode")}</th>
            <th>{t("inventory.movementsTable.item")}</th>
            <th>{t("inventory.movementsTable.location")}</th>
            <th>{t("inventory.movementsTable.season")}</th>
            <th>{t("inventory.movementsTable.task")}</th>
            <th>{t("inventory.movementsTable.note")}</th>
          </tr>
        </thead>
        <tbody>
          {data.map((mv) => {
            const display = getDisplayQuantity(
              mv.quantity,
              mv.unit,
              preferences.weightUnit,
              preferences.locale,
            );
            const movementMeta = getMovementTypeMeta(mv.movementType, t);
            const unitSuffix = display.unitLabel ? ` ${display.unitLabel}` : "";

            return (
              <tr key={mv.id}>
                <td>{formatDateTime(mv.movementDate)}</td>
                <td>
                  <span
                    className={`type-badge ${movementMeta.className}`}
                  >
                    {movementMeta.label}
                  </span>
                </td>
                <td className={`quantity ${movementMeta.className}`}>
                  {movementMeta.className === "movement-out" ? "-" : ""}
                  {display.formatted}
                  {unitSuffix}
                </td>
                <td>{mv.batchCode || "-"}</td>
                <td>{mv.supplyItemName || "-"}</td>
                <td>{getLocationDisplayLabel(mv.locationLabel, t)}</td>
                <td>{mv.seasonName || "-"}</td>
                <td>{mv.taskTitle || "-"}</td>
                <td className="note">{mv.note || "-"}</td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}

// ═══════════════════════════════════════════════════════════════
// STOCK OUT MODAL
// ═══════════════════════════════════════════════════════════════

interface StockOutModalProps {
  row: OnHandRow;
  onClose: () => void;
  onSubmit: (data: StockMovementRequest) => Promise<void>;
  isPending: boolean;
}

function StockOutModal({
  row,
  onClose,
  onSubmit,
  isPending,
}: StockOutModalProps) {
  const { preferences } = usePreferences();
  const { t } = useI18n();
  const [quantity, setQuantity] = useState<number>(0);
  const [seasonId, setSeasonId] = useState<number | null>(null);
  const [note, setNote] = useState("");
  const [error, setError] = useState("");
  const display = getDisplayQuantity(
    row.onHandQuantity,
    row.unit,
    preferences.weightUnit,
    preferences.locale,
  );
  const maxDisplayQuantity = display.displayValue;
  const unitSuffix = display.unitLabel ? ` ${display.unitLabel}` : "";
  const rowWeightUnit = normalizeWeightUnit(row.unit);

  const handleSubmit = async () => {
    if (!seasonId) {
      setError(t("inventory.validation.seasonRequired"));
      return;
    }
    if (quantity <= 0) {
      setError(t("inventory.validation.quantityPositive"));
      return;
    }
    if (quantity > maxDisplayQuantity) {
      setError(t("inventory.validation.quantityExceeds"));
      return;
    }
    setError("");

    try {
      const quantityToSend = rowWeightUnit
        ? toRowUnitQuantity(quantity, rowWeightUnit, preferences.weightUnit)
        : quantity;
      await onSubmit({
        movementType: "OUT",
        supplyLotId: row.supplyLotId,
        warehouseId: row.warehouseId,
        locationId: row.locationId,
        quantity: quantityToSend,
        seasonId,
        note: note || undefined,
      });
    } catch (e) {
      setError(
        e instanceof Error
          ? e.message
          : t("inventory.validation.movementFailed"),
      );
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content" onClick={(e) => e.stopPropagation()}>
        <h2>{t("inventory.stockOut")}</h2>
        <p className="modal-subtitle">
          {row.supplyItemName} ({row.batchCode})
        </p>
        <p className="on-hand-info">
          {t("inventory.currentOnHand")}:{" "}
          <strong>
            {display.formatted}
            {unitSuffix}
          </strong>
        </p>

        {error && <div className="error-message">{error}</div>}

        <div className="form-group">
          <label>{t("inventory.seasonRequired")}</label>
          <input
            type="number"
            placeholder={t("inventory.enterSeasonId")}
            value={seasonId || ""}
            onChange={(e) =>
              setSeasonId(e.target.value ? Number(e.target.value) : null)
            }
          />
        </div>

        <div className="form-group">
          <label>
            {t("inventory.quantity")}
            {display.unitLabel ? ` (${display.unitLabel})` : ""}
          </label>
          <input
            type="number"
            min={0}
            max={maxDisplayQuantity}
            value={quantity}
            onChange={(e) => setQuantity(Number(e.target.value))}
          />
        </div>

        <div className="form-group">
          <label>{t("inventory.noteOptional")}</label>
          <textarea
            value={note}
            onChange={(e) => setNote(e.target.value)}
            placeholder={t("inventory.addNote")}
          />
        </div>

        <div className="modal-actions">
          <button type="button" className="btn-cancel" onClick={onClose} disabled={isPending}>
            {t("common.cancel")}
          </button>
          <button
            type="button"
            className="btn-submit"
            onClick={handleSubmit}
            disabled={isPending}
          >
            {isPending
              ? t("common.processing")
              : t("inventory.confirmStockOut")}
          </button>
        </div>
      </div>
    </div>
  );
}

// ═══════════════════════════════════════════════════════════════
// ADJUST MODAL
// ═══════════════════════════════════════════════════════════════

interface AdjustModalProps {
  row: OnHandRow;
  onClose: () => void;
  onSubmit: (data: StockMovementRequest) => Promise<void>;
  isPending: boolean;
}

function AdjustModal({ row, onClose, onSubmit, isPending }: AdjustModalProps) {
  const { preferences } = usePreferences();
  const { t } = useI18n();
  const [adjustQuantity, setAdjustQuantity] = useState<number>(0);
  const [note, setNote] = useState("");
  const [error, setError] = useState("");
  const display = getDisplayQuantity(
    row.onHandQuantity,
    row.unit,
    preferences.weightUnit,
    preferences.locale,
  );
  const baseDisplayQuantity = display.displayValue;
  const unitSuffix = display.unitLabel ? ` ${display.unitLabel}` : "";
  const rowWeightUnit = normalizeWeightUnit(row.unit);

  const newOnHand = baseDisplayQuantity + adjustQuantity;

  const handleSubmit = async () => {
    if (!note.trim()) {
      setError(t("inventory.validation.noteRequired"));
      return;
    }
    if (adjustQuantity === 0) {
      setError(t("inventory.validation.adjustNotZero"));
      return;
    }
    if (newOnHand < 0) {
      setError(t("inventory.validation.negativeOnHand"));
      return;
    }
    setError("");

    try {
      const quantityToSend = rowWeightUnit
        ? toRowUnitQuantity(
            adjustQuantity,
            rowWeightUnit,
            preferences.weightUnit,
          )
        : adjustQuantity;
      await onSubmit({
        movementType: "ADJUST",
        supplyLotId: row.supplyLotId,
        warehouseId: row.warehouseId,
        locationId: row.locationId,
        quantity: quantityToSend,
        note,
      });
    } catch (e) {
      setError(
        e instanceof Error ? e.message : t("inventory.validation.adjustFailed"),
      );
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content" onClick={(e) => e.stopPropagation()}>
        <h2>{t("inventory.adjustStock")}</h2>
        <p className="modal-subtitle">
          {row.supplyItemName} ({row.batchCode})
        </p>
        <p className="on-hand-info">
          {t("inventory.currentOnHand")}:{" "}
          <strong>
            {display.formatted}
            {unitSuffix}
          </strong>
        </p>

        {error && <div className="error-message">{error}</div>}

        <div className="form-group">
          <label>
            {t("inventory.adjustmentAmount")}
            {display.unitLabel ? ` (${display.unitLabel})` : ""}
          </label>
          <input
            type="number"
            value={adjustQuantity}
            onChange={(e) => setAdjustQuantity(Number(e.target.value))}
            placeholder={t("inventory.adjustPlaceholder")}
          />
          <p className="preview">
            {t("inventory.newOnHand")}:{" "}
            <strong className={newOnHand < 0 ? "negative" : ""}>
              {formatNumber(
                newOnHand,
                preferences.locale,
                preferences.weightUnit === "G" ? 0 : 2,
              )}
              {unitSuffix}
            </strong>
          </p>
        </div>

        <div className="form-group">
          <label>{t("inventory.noteRequired")}</label>
          <textarea
            value={note}
            onChange={(e) => setNote(e.target.value)}
            placeholder={t("inventory.adjustReason")}
            required
          />
        </div>

        <div className="modal-actions">
          <button type="button" className="btn-cancel" onClick={onClose} disabled={isPending}>
            {t("common.cancel")}
          </button>
          <button
            type="button"
            className="btn-submit"
            onClick={handleSubmit}
            disabled={isPending || !note.trim()}
          >
            {isPending ? t("common.processing") : t("inventory.confirmAdjust")}
          </button>
        </div>
      </div>
    </div>
  );
}

interface WarehouseFormModalProps {
  mode: WarehouseDialogMode;
  name: string;
  farmId: number | undefined;
  farms: Array<{ id: number; name: string }>;
  onNameChange: (value: string) => void;
  onFarmIdChange: (value: number | undefined) => void;
  onClose: () => void;
  onSubmit: () => void;
  isPending: boolean;
  error: string;
}

function WarehouseFormModal({
  mode,
  name,
  farmId,
  farms,
  onNameChange,
  onFarmIdChange,
  onClose,
  onSubmit,
  isPending,
  error,
}: WarehouseFormModalProps) {
  const { t } = useI18n();

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content" onClick={(event) => event.stopPropagation()}>
        <h2>{mode === "create" ? t("inventory.dialog.addWarehouseTitle") : t("inventory.dialog.editWarehouseTitle")}</h2>
        <p className="modal-subtitle">
          {t("inventory.dialog.warehouseHint")}
        </p>

        {error && <div className="error-message">{error}</div>}

        <div className="form-group">
          <label>{t("inventory.form.warehouseNameLabel")}</label>
          <input
            type="text"
            value={name}
            onChange={(event) => onNameChange(event.target.value)}
            placeholder={t("inventory.form.warehouseNamePlaceholder")}
            maxLength={150}
          />
        </div>

        <div className="form-group">
          <label>{t("inventory.form.farmLabel")}</label>
          <select
            value={farmId ?? ""}
            onChange={(event) =>
              onFarmIdChange(
                event.target.value ? Number(event.target.value) : undefined,
              )
            }
            disabled={mode === "edit"}
          >
            <option value="">{t("inventory.form.selectFarm")}</option>
            {farms.map((farm) => (
              <option key={farm.id} value={farm.id}>
                {farm.name}
              </option>
            ))}
          </select>
        </div>

        <div className="modal-actions">
          <button type="button" className="btn-cancel" onClick={onClose} disabled={isPending}>
            {t("common.cancel")}
          </button>
          <button type="button" className="btn-submit" onClick={onSubmit} disabled={isPending}>
            {isPending ? t("common.processing") : t("common.save")}
          </button>
        </div>
      </div>
    </div>
  );
}

interface DeleteWarehouseModalProps {
  warehouseName: string;
  onClose: () => void;
  onConfirm: () => void;
  isPending: boolean;
}

function DeleteWarehouseModal({
  warehouseName,
  onClose,
  onConfirm,
  isPending,
}: DeleteWarehouseModalProps) {
  const { t } = useI18n();

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content" onClick={(event) => event.stopPropagation()}>
        <h2>{t("inventory.dialog.deleteWarehouseTitle")}</h2>
        <p className="modal-subtitle">
          {t("inventory.dialog.deleteWarehouseDescription", { warehouseName })}
        </p>
        <p className="on-hand-info">
          {t("inventory.dialog.deleteWarehouseHint")}
        </p>
        <div className="modal-actions">
          <button type="button" className="btn-cancel" onClick={onClose} disabled={isPending}>
            {t("common.cancel")}
          </button>
          <button
            type="button"
            className="btn-submit btn-submit-danger"
            onClick={onConfirm}
            disabled={isPending}
          >
            {isPending ? t("common.processing") : t("inventory.actions.deleteWarehouse")}
          </button>
        </div>
      </div>
    </div>
  );
}

export default InventoryPage;


