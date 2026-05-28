import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/shared/ui/dropdown-menu";
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
  type StockLocation,
} from "@/entities/inventory";
import {
  useAllSuppliers,
  useAllSupplyItems,
  useCreateSupplier,
  useDeleteSupplier,
  useStockIn,
  useSuppliers,
  useSupplyItems,
  useSupplyLots,
  useUpdateSupplier,
  type CreateSupplierRequest,
  type StockInRequest,
  type Supplier,
  type SupplyItem,
  type SupplyLot,
} from "@/entities/supplies";
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
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  PageContainer,
  PageHeader,
} from "@/shared/ui";
import {
  MoreVertical,
  Pencil,
  Plus,
  Trash2,
  UserPlus,
  Package,
} from "lucide-react";
import { useMemo, useState } from "react";
import { toast } from "sonner";
import "./SupplyManagementPage.css";

// ═══════════════════════════════════════════════════════════════
// HELPERS (from InventoryPage)
// ═══════════════════════════════════════════════════════════════

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

// ═══════════════════════════════════════════════════════════════
// MAIN PAGE COMPONENT
// ═══════════════════════════════════════════════════════════════

type TabType = "suppliers" | "items" | "lots" | "on-hand" | "movements";
type WarehouseDialogMode = "create" | "edit";

export function SupplyManagementPage() {
  const { t } = useI18n();
  const { preferences } = usePreferences();
  const [activeTab, setActiveTab] = useState<TabType>("suppliers");

  // ===== Supplier CRUD state =====
  const [showSupplierFormDialog, setShowSupplierFormDialog] = useState(false);
  const [supplierToEdit, setSupplierToEdit] = useState<Supplier | null>(null);
  const [showDeleteSupplierDialog, setShowDeleteSupplierDialog] = useState(false);
  const [supplierToDelete, setSupplierToDelete] = useState<Supplier | null>(null);

  // ===== Stock In state =====
  const [showStockInModal, setShowStockInModal] = useState(false);

  // ===== Suppliers tab state =====
  const [suppliersSearch, setSuppliersSearch] = useState("");
  const [itemsSearch, setItemsSearch] = useState("");
  const [itemsRestrictedFilter, setItemsRestrictedFilter] = useState<boolean | undefined>(undefined);
  const [lotsSearch, setLotsSearch] = useState("");
  const [lotsItemFilter, setLotsItemFilter] = useState<number | undefined>(undefined);
  const [lotsSupplierFilter, setLotsSupplierFilter] = useState<number | undefined>(undefined);

  // ===== Inventory tab state =====
  const [selectedWarehouseId, setSelectedWarehouseId] = useState<number | undefined>(undefined);
  const [selectedLocationId, setSelectedLocationId] = useState<number | undefined>(undefined);
  const [searchQuery, setSearchQuery] = useState("");
  const [movementTypeFilter, setMovementTypeFilter] = useState<string>("");
  const [dateFrom, setDateFrom] = useState("");
  const [dateTo, setDateTo] = useState("");
  const [warehouseDialogMode, setWarehouseDialogMode] = useState<WarehouseDialogMode | null>(null);
  const [warehouseNameInput, setWarehouseNameInput] = useState("");
  const [warehouseFarmIdInput, setWarehouseFarmIdInput] = useState<number | undefined>(undefined);
  const [warehouseFormError, setWarehouseFormError] = useState("");
  const [showDeleteWarehouseModal, setShowDeleteWarehouseModal] = useState(false);
  const [showStockOutModal, setShowStockOutModal] = useState(false);
  const [stockOutRow, setStockOutRow] = useState<OnHandRow | null>(null);
  const [showAdjustModal, setShowAdjustModal] = useState(false);
  const [adjustRow, setAdjustRow] = useState<OnHandRow | null>(null);

  // ===== Pagination (shared) =====
  const [page, setPage] = useState(0);

  // ===== QUERIES (Suppliers) =====
  const { data: suppliersData, isLoading: loadingSuppliers } = useSuppliers({
    q: suppliersSearch || undefined,
    page,
    size: 20,
  });
  const { data: itemsData, isLoading: loadingItems } = useSupplyItems({
    q: itemsSearch || undefined,
    restricted: itemsRestrictedFilter,
    page,
    size: 20,
  });
  const { data: lotsData, isLoading: loadingLots } = useSupplyLots({
    itemId: lotsItemFilter,
    supplierId: lotsSupplierFilter,
    q: lotsSearch || undefined,
    page,
    size: 20,
  });
  const { data: allSuppliers } = useAllSuppliers();
  const { data: allItems } = useAllSupplyItems();

  // ===== QUERIES (Inventory) =====
  const { data: warehouses, isLoading: loadingWarehouses } = useMyWarehouses("INPUT");
  const { data: farmsData } = useFarms({ active: true, page: 0, size: 200 });
  const supplyWarehouses = useMemo(() => warehouses ?? [], [warehouses]);
  const farmOptions = useMemo(() => farmsData?.content ?? [], [farmsData?.content]);
  const selectedWarehouse = useMemo(
    () => supplyWarehouses.find((w) => w.id === selectedWarehouseId) ?? null,
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

  const { data: onHandData, isLoading: loadingOnHand } = useOnHandList(onHandParams);
  const { data: movementsData, isLoading: loadingMovements } = useMovements(movementsParams);

  // ===== MUTATIONS =====
  const stockInMutation = useStockIn();
  const createSupplierMutation = useCreateSupplier();
  const updateSupplierMutation = useUpdateSupplier();
  const deleteSupplierMutation = useDeleteSupplier();
  const recordMovementMutation = useRecordStockMovement();
  const createWarehouseMutation = useCreateWarehouse();
  const updateWarehouseMutation = useUpdateWarehouse();
  const deleteWarehouseMutation = useDeleteWarehouse();

  // Auto-select first warehouse
  if (
    supplyWarehouses.length > 0 &&
    (!selectedWarehouseId || !supplyWarehouses.some((w) => w.id === selectedWarehouseId))
  ) {
    setSelectedWarehouseId(supplyWarehouses[0].id);
  }
  if (supplyWarehouses.length === 0 && selectedWarehouseId) {
    setSelectedWarehouseId(undefined);
  }

  // ===== TAB CHANGE =====
  const handleTabChange = (tab: TabType) => {
    setActiveTab(tab);
    setPage(0);
  };

  // ===== SUPPLIER HANDLERS =====
  const handleAddSupplier = () => {
    setSupplierToEdit(null);
    setShowSupplierFormDialog(true);
  };

  const handleEditSupplier = (supplier: Supplier) => {
    setSupplierToEdit(supplier);
    setShowSupplierFormDialog(true);
  };

  const handleDeleteSupplier = (supplier: Supplier) => {
    setSupplierToDelete(supplier);
    setShowDeleteSupplierDialog(true);
  };

  const handleSupplierFormSubmit = async (data: CreateSupplierRequest) => {
    if (supplierToEdit) {
      await updateSupplierMutation.mutateAsync({ id: supplierToEdit.id, data });
    } else {
      await createSupplierMutation.mutateAsync(data);
    }
    setShowSupplierFormDialog(false);
    setSupplierToEdit(null);
  };

  const handleConfirmDeleteSupplier = async () => {
    if (supplierToDelete) {
      await deleteSupplierMutation.mutateAsync(supplierToDelete.id);
      setShowDeleteSupplierDialog(false);
      setSupplierToDelete(null);
    }
  };

  // ===== WAREHOUSE HANDLERS =====
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
    if (createWarehouseMutation.isPending || updateWarehouseMutation.isPending) return;
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
    const farmId = warehouseDialogMode === "create" ? warehouseFarmIdInput : selectedWarehouse?.farmId;
    if (!farmId) {
      setWarehouseFormError(t("inventory.validation.farmRequired"));
      return;
    }
    setWarehouseFormError("");
    try {
      if (warehouseDialogMode === "create") {
        await createWarehouseMutation.mutateAsync({ name: trimmedName, farmId, type: "INPUT" });
        toast.success(t("inventory.toast.warehouseCreateSuccess"));
      } else if (warehouseDialogMode === "edit" && selectedWarehouse) {
        await updateWarehouseMutation.mutateAsync({ id: selectedWarehouse.id, data: { name: trimmedName, farmId } });
        toast.success(t("inventory.toast.warehouseUpdateSuccess"));
      }
      closeWarehouseFormModal();
    } catch (error) {
      setWarehouseFormError(error instanceof Error ? error.message : t("inventory.toast.warehouseSaveError"));
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
      toast.error(error instanceof Error ? error.message : t("inventory.toast.warehouseDeleteError"));
    }
  };

  // ===== STOCK OUT / ADJUST HANDLERS =====
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

  // ===== PAGINATION DATA =====
  const isInventoryTab = activeTab === "on-hand" || activeTab === "movements";
  const currentSupplierData =
    activeTab === "suppliers" ? suppliersData
    : activeTab === "items" ? itemsData
    : activeTab === "lots" ? lotsData
    : null;

  // ===== RENDER =====
  return (
    <PageContainer variant="wide">
      <div className="farmer-supply-management-page">
        <div className="supplies-page">
        <Card variant="page-header" className="mb-6">
          <CardContent className="px-6 py-4">
            <PageHeader
              className="mb-0"
              icon={<Package className="w-8 h-8" />}
              title={t("suppliers.title")}
              subtitle={t("suppliers.subtitle")}
              actions={
                <Button onClick={() => setShowStockInModal(true)} variant="default">
                  <Plus className="w-4 h-4 mr-2" />
                  {t("common.stockIn")}
                </Button>
              }
            />
          </CardContent>
        </Card>

        {/* ===== WAREHOUSE CONTROLS (only for inventory tabs) ===== */}
        {isInventoryTab && (
          <Card variant="filter" className="mb-6">
            <CardContent className="px-6 py-4">
              <div className="inventory-controls flex flex-wrap items-center justify-start gap-4">
                <div className="control-group">
                  <label htmlFor="warehouse-select">{t("inventory.warehouse")}</label>
                  <select
                    id="warehouse-select"
                    value={selectedWarehouseId || ""}
                    onChange={(e) => handleWarehouseChange(Number(e.target.value))}
                    disabled={loadingWarehouses}
                  >
                    {loadingWarehouses && <option>{t("inventory.loading")}</option>}
                    {!loadingWarehouses && supplyWarehouses.length === 0 && (
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
                  <Button type="button" variant="default" size="sm" onClick={openCreateWarehouseModal}>
                    <Plus className="w-4 h-4" />
                    {t("inventory.actions.addWarehouse")}
                  </Button>
                  <Button type="button" variant="outline" size="sm" onClick={openEditWarehouseModal} disabled={!selectedWarehouse}>
                    <Pencil className="w-4 h-4" />
                    {t("inventory.actions.editWarehouse")}
                  </Button>
                  <Button type="button" variant="destructive" size="sm" onClick={() => setShowDeleteWarehouseModal(true)} disabled={!selectedWarehouse}>
                    <Trash2 className="w-4 h-4" />
                    {t("inventory.actions.deleteWarehouse")}
                  </Button>
                </div>

                <div className="control-group">
                  <label htmlFor="location-select">{t("inventory.location")}</label>
                  <select
                    id="location-select"
                    value={selectedLocationId || ""}
                    onChange={(e) => setSelectedLocationId(e.target.value ? Number(e.target.value) : undefined)}
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
                      <label htmlFor="type-filter">{t("inventory.movementType")}</label>
                      <select id="type-filter" value={movementTypeFilter} onChange={(e) => setMovementTypeFilter(e.target.value)}>
                        <option value="">{t("inventory.allTypes")}</option>
                        <option value="IN">{t("inventory.types.in")}</option>
                        <option value="OUT">{t("inventory.types.out")}</option>
                        <option value="ADJUST">{t("inventory.types.adjust")}</option>
                      </select>
                    </div>
                    <div className="control-group">
                      <label htmlFor="date-from">{t("common.from")}</label>
                      <input id="date-from" type="date" value={dateFrom} onChange={(e) => setDateFrom(e.target.value)} />
                    </div>
                    <div className="control-group">
                      <label htmlFor="date-to">{t("common.to")}</label>
                      <input id="date-to" type="date" value={dateTo} onChange={(e) => setDateTo(e.target.value)} />
                    </div>
                  </>
                )}
              </div>
            </CardContent>
          </Card>
        )}

        {/* ===== SUPPLIER FILTERS (only for supplier tabs) ===== */}
        {!isInventoryTab && (
          <div className="supplies-toolbar">
            <div className="supplies-filters flex flex-wrap items-center justify-start gap-4">
              {activeTab === "suppliers" && (
                <>
                  <input
                    type="text"
                    placeholder={t("suppliers.searchSuppliers")}
                    value={suppliersSearch}
                    onChange={(e) => { setSuppliersSearch(e.target.value); setPage(0); }}
                    className="search-input"
                  />
                  <Button onClick={handleAddSupplier} variant="outline" size="sm">
                    <UserPlus className="w-4 h-4 mr-2" />
                    {t("suppliers.addSupplier")}
                  </Button>
                </>
              )}

              {activeTab === "items" && (
                <>
                  <input
                    type="text"
                    placeholder={t("suppliers.searchSupplies")}
                    value={itemsSearch}
                    onChange={(e) => { setItemsSearch(e.target.value); setPage(0); }}
                    className="search-input"
                  />
                  <label className="filter-checkbox">
                    <input
                      type="checkbox"
                      checked={itemsRestrictedFilter === true}
                      onChange={(e) => { setItemsRestrictedFilter(e.target.checked ? true : undefined); setPage(0); }}
                    />
                    {t("suppliers.filters.restrictedOnly")}
                  </label>
                </>
              )}

              {activeTab === "lots" && (
                <>
                  <input
                    type="text"
                    placeholder={t("suppliers.lots.searchPlaceholder")}
                    value={lotsSearch}
                    onChange={(e) => { setLotsSearch(e.target.value); setPage(0); }}
                    className="search-input"
                  />
                  <select
                    value={lotsItemFilter || ""}
                    onChange={(e) => { setLotsItemFilter(e.target.value ? Number(e.target.value) : undefined); setPage(0); }}
                    className="filter-select"
                  >
                    <option value="">{t("suppliers.filters.all")} {t("suppliers.tabs.items")}</option>
                    {allItems?.map((item) => (
                      <option key={item.id} value={item.id}>{item.name}</option>
                    ))}
                  </select>
                  <select
                    value={lotsSupplierFilter || ""}
                    onChange={(e) => { setLotsSupplierFilter(e.target.value ? Number(e.target.value) : undefined); setPage(0); }}
                    className="filter-select"
                  >
                    <option value="">{t("suppliers.filters.all")} {t("suppliers.tabs.suppliers")}</option>
                    {allSuppliers?.map((s) => (
                      <option key={s.id} value={s.id}>{s.name}</option>
                    ))}
                  </select>
                </>
              )}
            </div>
          </div>
        )}

        {/* ===== TABS ===== */}
        <div className="supply-management-tabs">
          <button className={`tab ${activeTab === "suppliers" ? "active" : ""}`} onClick={() => handleTabChange("suppliers")}>
            {t("suppliers.tabs.suppliers")}
          </button>
          <button className={`tab ${activeTab === "items" ? "active" : ""}`} onClick={() => handleTabChange("items")}>
            {t("suppliers.tabs.items")}
          </button>
          <button className={`tab ${activeTab === "lots" ? "active" : ""}`} onClick={() => handleTabChange("lots")}>
            {t("suppliers.tabs.lots")}
          </button>
          <button className={`tab ${activeTab === "on-hand" ? "active" : ""}`} onClick={() => handleTabChange("on-hand")}>
            {t("inventory.tabs.onHand")}
          </button>
          <button className={`tab ${activeTab === "movements" ? "active" : ""}`} onClick={() => handleTabChange("movements")}>
            {t("inventory.tabs.movements")}
          </button>
        </div>

        {/* ===== CONTENT ===== */}
        <div className="supply-management-content">
          {activeTab === "suppliers" && (
            <SuppliersTable
              data={suppliersData?.items || []}
              loading={loadingSuppliers}
              onEdit={handleEditSupplier}
              onDelete={handleDeleteSupplier}
            />
          )}
          {activeTab === "items" && (
            <SupplyItemsTable data={itemsData?.items || []} loading={loadingItems} />
          )}
          {activeTab === "lots" && (
            <SupplyLotsTable data={lotsData?.items || []} loading={loadingLots} />
          )}

          {activeTab === "on-hand" && (
            <>
              {!selectedWarehouseId && (
                <div className="empty-state"><p>{t("inventory.selectWarehouse")}</p></div>
              )}
              {selectedWarehouseId && (
                <OnHandTable
                  data={onHandData?.items || []}
                  loading={loadingOnHand}
                  onStockOut={handleStockOut}
                  onAdjust={handleAdjust}
                  formatDate={formatDate}
                />
              )}
            </>
          )}

          {activeTab === "movements" && (
            <>
              {!selectedWarehouseId && (
                <div className="empty-state"><p>{t("inventory.selectWarehouse")}</p></div>
              )}
              {selectedWarehouseId && (
                <MovementsTable
                  data={movementsData?.items || []}
                  loading={loadingMovements}
                  formatDateTime={formatDateTime}
                />
              )}
            </>
          )}

          {/* Pagination */}
          {!isInventoryTab && currentSupplierData && (
            <div className="pagination">
              <button disabled={page === 0} onClick={() => setPage((p) => p - 1)}>
                {t("common.previous")}
              </button>
              <span>
                {t("suppliers.pagination.summary", {
                  page: page + 1,
                  totalPages: currentSupplierData.totalPages || 1,
                })}
              </span>
              <button disabled={page >= currentSupplierData.totalPages - 1} onClick={() => setPage((p) => p + 1)}>
                {t("common.next")}
              </button>
            </div>
          )}

          {isInventoryTab && selectedWarehouseId && (
            <div className="pagination">
              <button disabled={page === 0} onClick={() => setPage((p) => p - 1)}>
                {t("common.previous")}
              </button>
              <span>{t("pagination.page")} {page + 1}</span>
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
        {showStockInModal && (
          <StockInModal
            onClose={() => setShowStockInModal(false)}
            onSuccess={() => setShowStockInModal(false)}
            onSubmit={stockInMutation.mutateAsync}
            isPending={stockInMutation.isPending}
          />
        )}

        <SupplierFormDialog
          open={showSupplierFormDialog}
          onOpenChange={setShowSupplierFormDialog}
          supplier={supplierToEdit}
          onSubmit={handleSupplierFormSubmit}
          isPending={createSupplierMutation.isPending || updateSupplierMutation.isPending}
        />

        <DeleteSupplierDialog
          open={showDeleteSupplierDialog}
          onOpenChange={setShowDeleteSupplierDialog}
          supplier={supplierToDelete}
          onConfirm={handleConfirmDeleteSupplier}
          isPending={deleteSupplierMutation.isPending}
        />

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
            isPending={createWarehouseMutation.isPending || updateWarehouseMutation.isPending}
            error={warehouseFormError}
          />
        )}

        {showDeleteWarehouseModal && selectedWarehouse && (
          <DeleteWarehouseModal
            warehouseName={selectedWarehouse.name}
            onClose={() => { if (!deleteWarehouseMutation.isPending) setShowDeleteWarehouseModal(false); }}
            onConfirm={confirmDeleteWarehouse}
            isPending={deleteWarehouseMutation.isPending}
          />
        )}

        {showStockOutModal && stockOutRow && (
          <StockOutModal
            row={stockOutRow}
            onClose={() => { setShowStockOutModal(false); setStockOutRow(null); }}
            onSubmit={async (data) => { await recordMovementMutation.mutateAsync(data); setShowStockOutModal(false); setStockOutRow(null); }}
            isPending={recordMovementMutation.isPending}
          />
        )}

        {showAdjustModal && adjustRow && (
          <AdjustModal
            row={adjustRow}
            onClose={() => { setShowAdjustModal(false); setAdjustRow(null); }}
            onSubmit={async (data) => { await recordMovementMutation.mutateAsync(data); setShowAdjustModal(false); setAdjustRow(null); }}
            isPending={recordMovementMutation.isPending}
          />
        )}
        </div>
      </div>
    </PageContainer>
  );
}

// ═══════════════════════════════════════════════════════════════
// SUPPLIERS TABLE
// ═══════════════════════════════════════════════════════════════

interface SuppliersTableProps {
  data: Supplier[];
  loading: boolean;
  onEdit: (supplier: Supplier) => void;
  onDelete: (supplier: Supplier) => void;
}

function SuppliersTable({ data, loading, onEdit, onDelete }: SuppliersTableProps) {
  const { t } = useI18n();
  if (loading) return <div className="loading-state">{t("suppliers.loading")}</div>;
  if (data.length === 0) return <div className="empty-state">{t("suppliers.empty")}</div>;

  return (
    <div className="table-container">
      <table className="supplies-table">
        <thead>
          <tr>
            <th>{t("suppliers.table.name")}</th>
            <th className="numeric-cell">{t("suppliers.table.licenseNo")}</th>
            <th className="numeric-cell">{t("suppliers.table.phone")}</th>
            <th>{t("suppliers.table.email")}</th>
            <th className="actions-cell">{t("suppliers.table.actions")}</th>
          </tr>
        </thead>
        <tbody>
          {data.map((supplier) => (
            <tr key={supplier.id}>
              <td className="name-cell">{supplier.name}</td>
              <td className="numeric-cell">{supplier.licenseNo || "-"}</td>
              <td className="numeric-cell">{supplier.contactPhone || "-"}</td>
              <td>{supplier.contactEmail || "-"}</td>
              <td className="actions-cell">
                <DropdownMenu>
                  <DropdownMenuTrigger asChild>
                    <button className="action-btn" title={t("suppliers.actions.menu")}>
                      <MoreVertical className="w-4 h-4" />
                    </button>
                  </DropdownMenuTrigger>
                  <DropdownMenuContent align="end">
                    <DropdownMenuItem onClick={() => onEdit(supplier)}>
                      <Pencil className="w-4 h-4 mr-2" />
                      {t("suppliers.actions.edit")}
                    </DropdownMenuItem>
                    <DropdownMenuItem onClick={() => onDelete(supplier)} className="text-destructive focus:text-destructive">
                      <Trash2 className="w-4 h-4 mr-2" />
                      {t("suppliers.actions.delete")}
                    </DropdownMenuItem>
                  </DropdownMenuContent>
                </DropdownMenu>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

// ═══════════════════════════════════════════════════════════════
// SUPPLY ITEMS TABLE
// ═══════════════════════════════════════════════════════════════

function SupplyItemsTable({ data, loading }: { data: SupplyItem[]; loading: boolean }) {
  const { t } = useI18n();
  if (loading) return <div className="loading-state">{t("suppliers.loadingItems")}</div>;
  if (data.length === 0) return <div className="empty-state">{t("suppliers.noItems")}</div>;

  return (
    <div className="table-container">
      <table className="supplies-table">
        <thead>
          <tr>
            <th>{t("suppliers.itemsTable.name")}</th>
            <th>{t("suppliers.itemsTable.activeIngredient")}</th>
            <th>{t("suppliers.itemsTable.unit")}</th>
            <th>{t("suppliers.itemsTable.restricted")}</th>
          </tr>
        </thead>
        <tbody>
          {data.map((item) => (
            <tr key={item.id}>
              <td className="name-cell">{item.name}</td>
              <td>{item.activeIngredient || "-"}</td>
              <td>{item.unit || "-"}</td>
              <td>
                {item.restrictedFlag ? (
                  <span className="badge badge-restricted">{t("suppliers.itemsTable.restrictedBadge")}</span>
                ) : (
                  <span className="badge badge-normal">{t("suppliers.itemsTable.normalBadge")}</span>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

// ═══════════════════════════════════════════════════════════════
// SUPPLY LOTS TABLE
// ═══════════════════════════════════════════════════════════════

function SupplyLotsTable({ data, loading }: { data: SupplyLot[]; loading: boolean }) {
  const { t, locale } = useI18n();
  if (loading) return <div className="loading-state">{t("suppliers.loadingLots")}</div>;
  if (data.length === 0) return <div className="empty-state">{t("suppliers.noLots")}</div>;

  const fmtDate = (dateStr: string | null | undefined): string => {
    if (!dateStr) return "-";
    try { return new Date(dateStr).toLocaleDateString(locale); } catch { return dateStr; }
  };

  const isExpiringSoon = (dateStr: string | null | undefined): boolean => {
    if (!dateStr) return false;
    try {
      const diffDays = Math.ceil((new Date(dateStr).getTime() - Date.now()) / (1000 * 60 * 60 * 24));
      return diffDays > 0 && diffDays <= 30;
    } catch { return false; }
  };

  return (
    <div className="table-container">
      <table className="supplies-table">
        <thead>
          <tr>
            <th>{t("suppliers.lotsTable.batchCode")}</th>
            <th>{t("suppliers.lotsTable.item")}</th>
            <th>{t("suppliers.lotsTable.supplier")}</th>
            <th className="numeric-cell">{t("suppliers.lotsTable.expiryDate")}</th>
            <th>{t("suppliers.lotsTable.status")}</th>
          </tr>
        </thead>
        <tbody>
          {data.map((lot) => (
            <tr key={lot.id}>
              <td className="batch-cell">{lot.batchCode || "-"}</td>
              <td>
                {lot.supplyItemName || "-"}
                {lot.restrictedFlag && <span className="badge badge-restricted ml-2">R</span>}
              </td>
              <td>{lot.supplierName || "-"}</td>
              <td className={`numeric-cell${isExpiringSoon(lot.expiryDate) ? " expiring-soon" : ""}`}>
                {fmtDate(lot.expiryDate)}
                {isExpiringSoon(lot.expiryDate) && (
                  <span className="expiry-warning">{t("suppliers.lotsTable.expiringSoon")}</span>
                )}
              </td>
              <td>
                <span className={`status-badge ${lot.status?.toLowerCase() || ""}`}>
                  {lot.status || "-"}
                </span>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

// ═══════════════════════════════════════════════════════════════
// ON-HAND TABLE (from InventoryPage)
// ═══════════════════════════════════════════════════════════════

interface OnHandTableProps {
  data: OnHandRow[];
  loading: boolean;
  onStockOut: (row: OnHandRow) => void;
  onAdjust: (row: OnHandRow) => void;
  formatDate: (date: string | null | undefined) => string;
}

function OnHandTable({ data, loading, onStockOut, onAdjust, formatDate }: OnHandTableProps) {
  const { preferences } = usePreferences();
  const { t } = useI18n();

  if (loading) return <div className="loading-state">{t("inventory.loading")}</div>;
  if (data.length === 0) return <div className="empty-state">{t("inventory.noStock")}</div>;

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
            const display = getDisplayQuantity(row.onHandQuantity, row.unit, preferences.weightUnit, preferences.locale);
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
                    return <span className={`status-badge ${statusMeta.className}`}>{statusMeta.label}</span>;
                  })()}
                </td>
                <td className="actions-cell">
                  <DropdownMenu>
                    <DropdownMenuTrigger asChild>
                      <button type="button" className="action-btn" aria-label={t("inventory.actions.menu")} title={t("inventory.actions.menu")}>
                        <MoreVertical className="w-4 h-4" />
                      </button>
                    </DropdownMenuTrigger>
                    <DropdownMenuContent align="end">
                      <DropdownMenuItem variant="destructive" onClick={() => onStockOut(row)}>
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
// MOVEMENTS TABLE (from InventoryPage)
// ═══════════════════════════════════════════════════════════════

function MovementsTable({ data, loading, formatDateTime }: { data: StockMovement[]; loading: boolean; formatDateTime: (date: string | null | undefined) => string }) {
  const { preferences } = usePreferences();
  const { t } = useI18n();

  if (loading) return <div className="loading-state">{t("inventory.loadingMovements")}</div>;
  if (data.length === 0) return <div className="empty-state">{t("inventory.noMovements")}</div>;

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
            const display = getDisplayQuantity(mv.quantity, mv.unit, preferences.weightUnit, preferences.locale);
            const movementMeta = getMovementTypeMeta(mv.movementType, t);
            const unitSuffix = display.unitLabel ? ` ${display.unitLabel}` : "";
            return (
              <tr key={mv.id}>
                <td>{formatDateTime(mv.movementDate)}</td>
                <td><span className={`type-badge ${movementMeta.className}`}>{movementMeta.label}</span></td>
                <td className={`quantity ${movementMeta.className}`}>
                  {movementMeta.className === "movement-out" ? "-" : ""}{display.formatted}{unitSuffix}
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
// STOCK IN MODAL (from SuppliersSuppliesPage)
// ═══════════════════════════════════════════════════════════════

interface StockInModalProps {
  onClose: () => void;
  onSuccess: () => void;
  onSubmit: (data: StockInRequest) => Promise<unknown>;
  isPending: boolean;
}

function StockInModal({ onClose, onSuccess, onSubmit, isPending }: StockInModalProps) {
  const { t } = useI18n();
  const [step, setStep] = useState(1);
  const [error, setError] = useState("");
  const [warehouseId, setWarehouseId] = useState<number | null>(null);
  const [locationId, setLocationId] = useState<number | null>(null);
  const [supplierId, setSupplierId] = useState<number | null>(null);
  const [supplyItemId, setSupplyItemId] = useState<number | null>(null);
  const [confirmRestricted, setConfirmRestricted] = useState(false);
  const [batchCode, setBatchCode] = useState("");
  const [expiryDate, setExpiryDate] = useState("");
  const [quantity, setQuantity] = useState<number>(0);
  const [note, setNote] = useState("");
  const [confirmExpiry, setConfirmExpiry] = useState(false);

  const { data: siWarehouses } = useMyWarehouses();
  const { data: siLocations } = useLocations(warehouseId ?? undefined);
  const { data: siSuppliers } = useAllSuppliers();
  const { data: siItems } = useAllSupplyItems();

  const selectedItem = siItems?.find((i) => i.id === supplyItemId);
  const isRestricted = selectedItem?.restrictedFlag === true;
  const isExpiryPast = expiryDate && new Date(expiryDate) <= new Date();
  const canGoToStep2 = warehouseId !== null;
  const canGoToStep3 = supplierId !== null && supplyItemId !== null && (!isRestricted || confirmRestricted);
  const canSubmit = quantity > 0 && (!isExpiryPast || confirmExpiry);

  const handleNext = () => { setError(""); if (step === 1 && canGoToStep2) setStep(2); else if (step === 2 && canGoToStep3) setStep(3); };
  const handleBack = () => { setError(""); if (step > 1) setStep(step - 1); };

  const handleSubmit = async () => {
    if (!warehouseId || !supplierId || !supplyItemId || quantity <= 0) { setError(t("suppliers.validation.required")); return; }
    if (isRestricted && !confirmRestricted) { setError("Please confirm handling of restricted supplies"); return; }
    if (isExpiryPast && !confirmExpiry) { setError("Please confirm the past expiry date"); return; }
    try {
      await onSubmit({ warehouseId, locationId: locationId || undefined, supplierId, supplyItemId, batchCode: batchCode || undefined, expiryDate: expiryDate || undefined, quantity, confirmRestricted: confirmRestricted || undefined, note: note || undefined });
      onSuccess();
    } catch (e) { setError(e instanceof Error ? e.message : t("suppliers.stockIn.errors.recordFailed")); }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content stock-in-modal" onClick={(e) => e.stopPropagation()}>
        <h2>{t("suppliers.stockIn.title")}</h2>
        <div className="stepper">
          <div className={`step ${step >= 1 ? "active" : ""} ${step > 1 ? "completed" : ""}`}>
            <span className="step-number">1</span>
            <span className="step-label">{t("suppliers.stockIn.steps.warehouse")}</span>
          </div>
          <div className="step-line" />
          <div className={`step ${step >= 2 ? "active" : ""} ${step > 2 ? "completed" : ""}`}>
            <span className="step-number">2</span>
            <span className="step-label">{t("suppliers.stockIn.steps.supplierItem")}</span>
          </div>
          <div className="step-line" />
          <div className={`step ${step >= 3 ? "active" : ""}`}>
            <span className="step-number">3</span>
            <span className="step-label">{t("suppliers.stockIn.steps.batchInfo")}</span>
          </div>
        </div>

        {error && <div className="error-message">{error}</div>}

        {step === 1 && (
          <div className="step-content">
            <div className="form-group">
              <label>{t("suppliers.stockIn.form.warehouse")} *</label>
              <select value={warehouseId || ""} onChange={(e) => { setWarehouseId(e.target.value ? Number(e.target.value) : null); setLocationId(null); }}>
                <option value="">{t("suppliers.stockIn.form.selectWarehouse")}</option>
                {siWarehouses?.map((w: WarehouseEntity) => (<option key={w.id} value={w.id}>{w.name} {w.farmName ? `(${w.farmName})` : ""}</option>))}
              </select>
            </div>
            <div className="form-group">
              <label>{t("suppliers.stockIn.form.locationOptional")}</label>
              <select value={locationId || ""} onChange={(e) => setLocationId(e.target.value ? Number(e.target.value) : null)} disabled={!warehouseId}>
                <option value="">{t("suppliers.stockIn.form.anyLocation")}</option>
                {siLocations?.map((loc: StockLocation) => (<option key={loc.id} value={loc.id}>{loc.label || t("suppliers.stockIn.form.locationFallback", { id: loc.id })}</option>))}
              </select>
            </div>
          </div>
        )}

        {step === 2 && (
          <div className="step-content">
            <div className="form-group">
              <label>{t("suppliers.stockIn.form.supplier")} *</label>
              <select value={supplierId || ""} onChange={(e) => setSupplierId(e.target.value ? Number(e.target.value) : null)}>
                <option value="">{t("suppliers.stockIn.form.selectSupplier")}</option>
                {siSuppliers?.map((s) => (<option key={s.id} value={s.id}>{s.name}</option>))}
              </select>
            </div>
            <div className="form-group">
              <label>{t("suppliers.stockIn.form.supplyItem")} *</label>
              <select value={supplyItemId || ""} onChange={(e) => { setSupplyItemId(e.target.value ? Number(e.target.value) : null); setConfirmRestricted(false); }}>
                <option value="">{t("suppliers.stockIn.form.selectItem")}</option>
                {siItems?.map((i) => (<option key={i.id} value={i.id}>{i.name} {i.unit ? `(${i.unit})` : ""} {i.restrictedFlag ? t("suppliers.stockIn.form.restrictedTag") : ""}</option>))}
              </select>
            </div>
            {isRestricted && (
              <div className="warning-banner">
                <strong>{t("suppliers.stockIn.form.restrictedTitle")}</strong>
                <p>{t("suppliers.stockIn.form.restrictedDescription")}</p>
                <label className="confirm-checkbox">
                  <input type="checkbox" checked={confirmRestricted} onChange={(e) => setConfirmRestricted(e.target.checked)} />
                  {t("suppliers.stockIn.form.restrictedConfirm")}
                </label>
              </div>
            )}
          </div>
        )}

        {step === 3 && (
          <div className="step-content">
            <div className="form-group">
              <label>{t("suppliers.stockIn.form.batchCode")}</label>
              <input type="text" value={batchCode} onChange={(e) => setBatchCode(e.target.value)} placeholder={t("suppliers.stockIn.form.batchCodePlaceholder")} />
            </div>
            <div className="form-group">
              <label>{t("suppliers.stockIn.form.expiryDate")}</label>
              <input type="date" value={expiryDate} onChange={(e) => { setExpiryDate(e.target.value); setConfirmExpiry(false); }} />
              {isExpiryPast && (
                <div className="warning-banner small">
                  <strong>{t("suppliers.stockIn.form.pastExpiryTitle")}</strong>
                  <label className="confirm-checkbox">
                    <input type="checkbox" checked={confirmExpiry} onChange={(e) => setConfirmExpiry(e.target.checked)} />
                    {t("suppliers.stockIn.form.pastExpiryConfirm")}
                  </label>
                </div>
              )}
            </div>
            <div className="form-group">
              <label>{t("suppliers.stockIn.form.quantity")} * {selectedItem?.unit ? `(${selectedItem.unit})` : ""}</label>
              <input type="number" min={0} step="0.001" value={quantity || ""} onChange={(e) => setQuantity(Number(e.target.value))} placeholder={t("suppliers.stockIn.form.quantityPlaceholder")} />
            </div>
            <div className="form-group">
              <label>{t("suppliers.stockIn.form.noteOptional")}</label>
              <textarea value={note} onChange={(e) => setNote(e.target.value)} placeholder={t("suppliers.stockIn.form.notePlaceholder")} />
            </div>
          </div>
        )}

        <div className="modal-actions">
          <button className="btn-cancel" onClick={onClose} disabled={isPending}>{t("common.cancel")}</button>
          <div className="action-group">
            {step > 1 && (<button className="btn-secondary" onClick={handleBack} disabled={isPending}>{t("suppliers.stockIn.actions.back")}</button>)}
            {step < 3 ? (
              <button className="btn-primary" onClick={handleNext} disabled={step === 1 ? !canGoToStep2 : !canGoToStep3}>{t("suppliers.stockIn.actions.next")}</button>
            ) : (
              <button className="btn-primary" onClick={handleSubmit} disabled={isPending || !canSubmit}>{isPending ? t("common.processing") : t("suppliers.stockIn.actions.confirm")}</button>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

// ═══════════════════════════════════════════════════════════════
// STOCK OUT MODAL (from InventoryPage)
// ═══════════════════════════════════════════════════════════════

function StockOutModal({ row, onClose, onSubmit, isPending }: { row: OnHandRow; onClose: () => void; onSubmit: (data: StockMovementRequest) => Promise<void>; isPending: boolean }) {
  const { preferences } = usePreferences();
  const { t } = useI18n();
  const [quantity, setQuantity] = useState<number>(0);
  const [seasonId, setSeasonId] = useState<number | null>(null);
  const [note, setNote] = useState("");
  const [error, setError] = useState("");
  const display = getDisplayQuantity(row.onHandQuantity, row.unit, preferences.weightUnit, preferences.locale);
  const maxDisplayQuantity = display.displayValue;
  const unitSuffix = display.unitLabel ? ` ${display.unitLabel}` : "";
  const rowWeightUnit = normalizeWeightUnit(row.unit);

  const handleSubmit = async () => {
    if (!seasonId) { setError(t("inventory.validation.seasonRequired")); return; }
    if (quantity <= 0) { setError(t("inventory.validation.quantityPositive")); return; }
    if (quantity > maxDisplayQuantity) { setError(t("inventory.validation.quantityExceeds")); return; }
    setError("");
    try {
      const quantityToSend = rowWeightUnit ? toRowUnitQuantity(quantity, rowWeightUnit, preferences.weightUnit) : quantity;
      await onSubmit({ movementType: "OUT", supplyLotId: row.supplyLotId, warehouseId: row.warehouseId, locationId: row.locationId, quantity: quantityToSend, seasonId, note: note || undefined });
    } catch (e) { setError(e instanceof Error ? e.message : t("inventory.validation.movementFailed")); }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content" onClick={(e) => e.stopPropagation()}>
        <h2>{t("inventory.stockOut")}</h2>
        <p className="modal-subtitle">{row.supplyItemName} ({row.batchCode})</p>
        <p className="on-hand-info">{t("inventory.currentOnHand")}: <strong>{display.formatted}{unitSuffix}</strong></p>
        {error && <div className="error-message">{error}</div>}
        <div className="form-group">
          <label>{t("inventory.seasonRequired")}</label>
          <input type="number" placeholder={t("inventory.enterSeasonId")} value={seasonId || ""} onChange={(e) => setSeasonId(e.target.value ? Number(e.target.value) : null)} />
        </div>
        <div className="form-group">
          <label>{t("inventory.quantity")}{display.unitLabel ? ` (${display.unitLabel})` : ""}</label>
          <input type="number" min={0} max={maxDisplayQuantity} value={quantity} onChange={(e) => setQuantity(Number(e.target.value))} />
        </div>
        <div className="form-group">
          <label>{t("inventory.noteOptional")}</label>
          <textarea value={note} onChange={(e) => setNote(e.target.value)} placeholder={t("inventory.addNote")} />
        </div>
        <div className="modal-actions">
          <button type="button" className="btn-cancel" onClick={onClose} disabled={isPending}>{t("common.cancel")}</button>
          <button type="button" className="btn-submit" onClick={handleSubmit} disabled={isPending}>{isPending ? t("common.processing") : t("inventory.confirmStockOut")}</button>
        </div>
      </div>
    </div>
  );
}

// ═══════════════════════════════════════════════════════════════
// ADJUST MODAL (from InventoryPage)
// ═══════════════════════════════════════════════════════════════

function AdjustModal({ row, onClose, onSubmit, isPending }: { row: OnHandRow; onClose: () => void; onSubmit: (data: StockMovementRequest) => Promise<void>; isPending: boolean }) {
  const { preferences } = usePreferences();
  const { t } = useI18n();
  const [adjustQuantity, setAdjustQuantity] = useState<number>(0);
  const [note, setNote] = useState("");
  const [error, setError] = useState("");
  const display = getDisplayQuantity(row.onHandQuantity, row.unit, preferences.weightUnit, preferences.locale);
  const baseDisplayQuantity = display.displayValue;
  const unitSuffix = display.unitLabel ? ` ${display.unitLabel}` : "";
  const rowWeightUnit = normalizeWeightUnit(row.unit);
  const newOnHand = baseDisplayQuantity + adjustQuantity;

  const handleSubmit = async () => {
    if (!note.trim()) { setError(t("inventory.validation.noteRequired")); return; }
    if (adjustQuantity === 0) { setError(t("inventory.validation.adjustNotZero")); return; }
    if (newOnHand < 0) { setError(t("inventory.validation.negativeOnHand")); return; }
    setError("");
    try {
      const quantityToSend = rowWeightUnit ? toRowUnitQuantity(adjustQuantity, rowWeightUnit, preferences.weightUnit) : adjustQuantity;
      await onSubmit({ movementType: "ADJUST", supplyLotId: row.supplyLotId, warehouseId: row.warehouseId, locationId: row.locationId, quantity: quantityToSend, note });
    } catch (e) { setError(e instanceof Error ? e.message : t("inventory.validation.adjustFailed")); }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content" onClick={(e) => e.stopPropagation()}>
        <h2>{t("inventory.adjustStock")}</h2>
        <p className="modal-subtitle">{row.supplyItemName} ({row.batchCode})</p>
        <p className="on-hand-info">{t("inventory.currentOnHand")}: <strong>{display.formatted}{unitSuffix}</strong></p>
        {error && <div className="error-message">{error}</div>}
        <div className="form-group">
          <label>{t("inventory.adjustmentAmount")}{display.unitLabel ? ` (${display.unitLabel})` : ""}</label>
          <input type="number" value={adjustQuantity} onChange={(e) => setAdjustQuantity(Number(e.target.value))} placeholder={t("inventory.adjustPlaceholder")} />
          <p className="preview">
            {t("inventory.newOnHand")}:{" "}
            <strong className={newOnHand < 0 ? "negative" : ""}>
              {formatNumber(newOnHand, preferences.locale, preferences.weightUnit === "G" ? 0 : 2)}{unitSuffix}
            </strong>
          </p>
        </div>
        <div className="form-group">
          <label>{t("inventory.noteRequired")}</label>
          <textarea value={note} onChange={(e) => setNote(e.target.value)} placeholder={t("inventory.adjustReason")} required />
        </div>
        <div className="modal-actions">
          <button type="button" className="btn-cancel" onClick={onClose} disabled={isPending}>{t("common.cancel")}</button>
          <button type="button" className="btn-submit" onClick={handleSubmit} disabled={isPending || !note.trim()}>{isPending ? t("common.processing") : t("inventory.confirmAdjust")}</button>
        </div>
      </div>
    </div>
  );
}

// ═══════════════════════════════════════════════════════════════
// WAREHOUSE FORM MODAL (from InventoryPage)
// ═══════════════════════════════════════════════════════════════

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

function WarehouseFormModal({ mode, name, farmId, farms, onNameChange, onFarmIdChange, onClose, onSubmit, isPending, error }: WarehouseFormModalProps) {
  const { t } = useI18n();
  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content" onClick={(event) => event.stopPropagation()}>
        <h2>{mode === "create" ? t("inventory.dialog.addWarehouseTitle") : t("inventory.dialog.editWarehouseTitle")}</h2>
        <p className="modal-subtitle">{t("inventory.dialog.warehouseHint")}</p>
        {error && <div className="error-message">{error}</div>}
        <div className="form-group">
          <label>{t("inventory.form.warehouseNameLabel")}</label>
          <input type="text" value={name} onChange={(event) => onNameChange(event.target.value)} placeholder={t("inventory.form.warehouseNamePlaceholder")} maxLength={150} />
        </div>
        <div className="form-group">
          <label>{t("inventory.form.farmLabel")}</label>
          <select value={farmId ?? ""} onChange={(event) => onFarmIdChange(event.target.value ? Number(event.target.value) : undefined)} disabled={mode === "edit"}>
            <option value="">{t("inventory.form.selectFarm")}</option>
            {farms.map((farm) => (<option key={farm.id} value={farm.id}>{farm.name}</option>))}
          </select>
        </div>
        <div className="modal-actions">
          <button type="button" className="btn-cancel" onClick={onClose} disabled={isPending}>{t("common.cancel")}</button>
          <button type="button" className="btn-submit" onClick={onSubmit} disabled={isPending}>{isPending ? t("common.processing") : t("common.save")}</button>
        </div>
      </div>
    </div>
  );
}

// ═══════════════════════════════════════════════════════════════
// DELETE WAREHOUSE MODAL (from InventoryPage)
// ═══════════════════════════════════════════════════════════════

function DeleteWarehouseModal({ warehouseName, onClose, onConfirm, isPending }: { warehouseName: string; onClose: () => void; onConfirm: () => void; isPending: boolean }) {
  const { t } = useI18n();
  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content" onClick={(event) => event.stopPropagation()}>
        <h2>{t("inventory.dialog.deleteWarehouseTitle")}</h2>
        <p className="modal-subtitle">{t("inventory.dialog.deleteWarehouseDescription", { warehouseName })}</p>
        <p className="on-hand-info">{t("inventory.dialog.deleteWarehouseHint")}</p>
        <div className="modal-actions">
          <button type="button" className="btn-cancel" onClick={onClose} disabled={isPending}>{t("common.cancel")}</button>
          <button type="button" className="btn-submit btn-submit-danger" onClick={onConfirm} disabled={isPending}>{isPending ? t("common.processing") : t("inventory.actions.deleteWarehouse")}</button>
        </div>
      </div>
    </div>
  );
}

// ═══════════════════════════════════════════════════════════════
// SUPPLIER FORM DIALOG (from SuppliersSuppliesPage)
// ═══════════════════════════════════════════════════════════════

interface SupplierFormDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  supplier: Supplier | null;
  onSubmit: (data: CreateSupplierRequest) => Promise<void>;
  isPending: boolean;
}

function SupplierFormDialog({ open, onOpenChange, supplier, onSubmit, isPending }: SupplierFormDialogProps) {
  const { t } = useI18n();
  const [name, setName] = useState("");
  const [licenseNo, setLicenseNo] = useState("");
  const [contactEmail, setContactEmail] = useState("");
  const [contactPhone, setContactPhone] = useState("");
  const [error, setError] = useState("");

  const resetForm = () => {
    if (supplier) { setName(supplier.name || ""); setLicenseNo(supplier.licenseNo || ""); setContactEmail(supplier.contactEmail || ""); setContactPhone(supplier.contactPhone || ""); }
    else { setName(""); setLicenseNo(""); setContactEmail(""); setContactPhone(""); }
    setError("");
  };

  if (open && name === "" && supplier) resetForm();

  const handleOpenChange = (newOpen: boolean) => {
    if (!newOpen) { setName(""); setLicenseNo(""); setContactEmail(""); setContactPhone(""); setError(""); }
    else if (supplier) resetForm();
    onOpenChange(newOpen);
  };

  const handleSubmit = async () => {
    if (!name.trim()) { setError(t("suppliers.errors.nameRequired")); return; }
    try {
      await onSubmit({ name: name.trim(), licenseNo: licenseNo.trim() || null, contactEmail: contactEmail.trim() || null, contactPhone: contactPhone.trim() || null });
      handleOpenChange(false);
    } catch (e) { setError(e instanceof Error ? e.message : t("suppliers.errors.saveFailed")); }
  };

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogContent className="w-[92vw] max-w-[420px]">
        <DialogHeader>
          <DialogTitle>{supplier ? t("suppliers.form.editTitle") : t("suppliers.form.addTitle")}</DialogTitle>
          <DialogDescription>{supplier ? t("suppliers.form.editDescription") : t("suppliers.form.addDescription")}</DialogDescription>
        </DialogHeader>
        {error && <div className="error-message text-destructive text-sm mb-4">{error}</div>}
        <div className="space-y-4">
          <div className="form-group">
            <label className="text-sm font-medium">{t("suppliers.form.name")} *</label>
            <input type="text" value={name} onChange={(e) => setName(e.target.value)} placeholder={t("suppliers.form.namePlaceholder")} className="w-full px-3 py-2 border border-border rounded-md" />
          </div>
          <div className="form-group">
            <label className="text-sm font-medium">{t("suppliers.form.licenseNo")}</label>
            <input type="text" value={licenseNo} onChange={(e) => setLicenseNo(e.target.value)} placeholder={t("suppliers.form.licenseNoPlaceholder")} className="w-full px-3 py-2 border border-border rounded-md" />
          </div>
          <div className="form-group">
            <label className="text-sm font-medium">{t("suppliers.form.phone")}</label>
            <input type="text" value={contactPhone} onChange={(e) => setContactPhone(e.target.value)} placeholder={t("suppliers.form.phonePlaceholder")} className="w-full px-3 py-2 border border-border rounded-md" />
          </div>
          <div className="form-group">
            <label className="text-sm font-medium">{t("suppliers.form.email")}</label>
            <input type="email" value={contactEmail} onChange={(e) => setContactEmail(e.target.value)} placeholder={t("suppliers.form.emailPlaceholder")} className="w-full px-3 py-2 border border-border rounded-md" />
          </div>
        </div>
        <DialogFooter className="mt-6">
          <Button variant="ghost" onClick={() => handleOpenChange(false)} disabled={isPending}>{t("common.cancel")}</Button>
          <Button onClick={handleSubmit} disabled={isPending}>{isPending ? t("common.saving") : supplier ? t("common.saveChanges") : t("suppliers.form.addButton")}</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

// ═══════════════════════════════════════════════════════════════
// DELETE SUPPLIER DIALOG (from SuppliersSuppliesPage)
// ═══════════════════════════════════════════════════════════════

function DeleteSupplierDialog({ open, onOpenChange, supplier, onConfirm, isPending }: { open: boolean; onOpenChange: (open: boolean) => void; supplier: Supplier | null; onConfirm: () => Promise<void>; isPending: boolean }) {
  const { t } = useI18n();
  const [error, setError] = useState("");

  const handleConfirm = async () => { try { await onConfirm(); } catch (e) { setError(e instanceof Error ? e.message : t("suppliers.errors.deleteFailed")); } };
  const handleOpenChange = (newOpen: boolean) => { if (!newOpen) setError(""); onOpenChange(newOpen); };

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogContent className="w-[92vw] max-w-[400px]">
        <DialogHeader>
          <DialogTitle>{t("suppliers.delete.title")}</DialogTitle>
          <DialogDescription>{t("suppliers.delete.description", { name: supplier?.name })}</DialogDescription>
        </DialogHeader>
        {error && <div className="error-message text-destructive text-sm mb-4">{error}</div>}
        <DialogFooter className="mt-6">
          <Button variant="ghost" onClick={() => handleOpenChange(false)} disabled={isPending}>{t("common.cancel")}</Button>
          <Button variant="destructive" onClick={handleConfirm} disabled={isPending}>{isPending ? t("common.deleting") : t("common.delete")}</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

export default SupplyManagementPage;
