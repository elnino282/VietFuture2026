import { ClipboardCheck, Loader2, Plus, Save, X } from "lucide-react";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/shared/ui/dialog";
import { Button } from "@/shared/ui/button";
import { Input } from "@/shared/ui/input";
import { Label } from "@/shared/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/shared/ui/select";
import { Textarea } from "@/shared/ui/textarea";
import { Separator } from "@/shared/ui/separator";
import type { HarvestFormData, HarvestGrade } from "../types";
import { GRADE_OPTIONS } from "../constants";
import { useSeasons } from "@/entities/season";
import { useMyWarehouses, useLocations } from "@/entities/inventory";
import { useHarvestStockContext } from "@/entities/harvest";
import { useProductWarehouseLots } from "@/entities/product-warehouse";
import { usePreferences } from "@/shared/contexts";
import { getWeightUnitLabel } from "@/shared/lib";
import { useMemo } from "react";

interface AddBatchDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  formData: HarvestFormData;
  onFormChange: (data: HarvestFormData) => void;
  onSubmit: () => void;
  seasonId?: number;
  isSeasonLocked?: boolean;
  lockedSeasonLabel?: string;
  isWriteLocked?: boolean;
  warehouseCount?: number;
  writeLockReason?: string;
  onCancel: () => void;
  isSubmitting?: boolean;
}

export function AddBatchDialog({
  open,
  onOpenChange,
  formData,
  onFormChange,
  onSubmit,
  seasonId,
  isSeasonLocked = false,
  lockedSeasonLabel,
  isWriteLocked = false,
  warehouseCount = 0,
  writeLockReason,
  onCancel,
  isSubmitting = false,
}: AddBatchDialogProps) {
  const { preferences } = usePreferences();
  const weightUnitLabel = getWeightUnitLabel(preferences.weightUnit);

  const { data: seasonsData } = useSeasons();
  const { data: warehousesData } = useMyWarehouses("OUTPUT");

  const selectedWarehouseId = Number(formData.warehouseId);
  const hasWarehouse = Number.isFinite(selectedWarehouseId) && selectedWarehouseId > 0;

  const { data: locationsData } = useLocations(hasWarehouse ? selectedWarehouseId : undefined);
  const { data: lotsData } = useProductWarehouseLots({
    page: 0,
    size: 200,
    ...(hasWarehouse ? { warehouseId: selectedWarehouseId } : {}),
  });

  const seasonOptions = useMemo(
    () =>
      seasonsData?.items?.map((season) => ({
        value: String(season.id),
        label: season.seasonName,
      })) ?? [],
    [seasonsData]
  );

  const warehouseOptions = useMemo(
    () =>
      (warehousesData ?? []).map((warehouse) => ({
        value: String(warehouse.id),
        label: warehouse.name,
      })),
    [warehousesData]
  );
  const hasOutputWarehouse = warehouseCount > 0 || warehouseOptions.length > 0;
  const isFormDisabled = isWriteLocked || isSubmitting;
  const selectedSeasonId = seasonId
    ?? (Number.isFinite(Number(formData.season)) ? Number(formData.season) : undefined);
  const hasValidSeason = !!selectedSeasonId && selectedSeasonId > 0;
  const canSubmit = !isFormDisabled && hasOutputWarehouse && hasValidSeason;

  const locationOptions = useMemo(
    () =>
      (locationsData ?? []).map((location) => ({
        value: String(location.id),
        label: location.label ?? `Location #${location.id}`,
      })),
    [locationsData]
  );

  const productOptions = useMemo(() => {
    const map = new Map<
      string,
      { productName: string; productId?: number | null; productVariant?: string | null }
    >();
    (lotsData?.items ?? []).forEach((lot) => {
      if (!lot.productName) return;
      if (!map.has(lot.productName)) {
        map.set(lot.productName, {
          productName: lot.productName,
          productId: lot.productId,
          productVariant: lot.productVariant,
        });
      }
    });
    return Array.from(map.values()).sort((a, b) => a.productName.localeCompare(b.productName));
  }, [lotsData]);

  const productMapByName = useMemo(
    () => new Map(productOptions.map((option) => [option.productName, option])),
    [productOptions]
  );

  const lotCodeOptions = useMemo(() => {
    const lotSet = new Set<string>();
    (lotsData?.items ?? []).forEach((lot) => {
      if (!lot.lotCode) return;
      if (formData.productName && lot.productName !== formData.productName) return;
      lotSet.add(lot.lotCode);
    });
    return Array.from(lotSet).sort((a, b) => a.localeCompare(b));
  }, [formData.productName, lotsData]);

  const contextSeasonId = selectedSeasonId;
  const stockContextQuery = useHarvestStockContext(contextSeasonId, {
    warehouseId: hasWarehouse ? selectedWarehouseId : undefined,
    productName: formData.productName.trim() || undefined,
    lotCode: formData.lotCode.trim() || undefined,
  });

  return (
    <Dialog
      open={open}
      onOpenChange={(nextOpen) => {
        if (!nextOpen && isSubmitting) return;
        onOpenChange(nextOpen);
      }}
    >
      <DialogContent className="max-w-3xl max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2 text-foreground text-xl">
            <Plus className="w-5 h-5 text-primary" />
            Add Harvest Batch
          </DialogTitle>
          <DialogDescription className="text-sm text-muted-foreground">
            Record harvest and link directly to warehouse stock lots.
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-6 py-4">
          {isWriteLocked && (
            <div className="rounded-xl border border-amber-300 bg-amber-50 px-3 py-2 text-sm text-amber-900">
              {writeLockReason || "This season is locked. Harvest write actions are disabled."}
            </div>
          )}
          {!hasOutputWarehouse && (
            <div className="rounded-xl border border-destructive/30 bg-destructive/5 px-3 py-2 text-sm text-destructive">
              No output warehouse found. Create an output warehouse first to link harvest with inventory.
            </div>
          )}
          {!hasValidSeason && (
            <div className="rounded-xl border border-destructive/30 bg-destructive/5 px-3 py-2 text-sm text-destructive">
              Select a valid season before saving a harvest batch.
            </div>
          )}

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label htmlFor="batchId" className="text-foreground">
                Batch ID
              </Label>
              <Input
                id="batchId"
                placeholder="HRV-2025-XXX"
                value={formData.batchId}
                onChange={(e) => onFormChange({ ...formData, batchId: e.target.value })}
                className="rounded-xl border-border focus:border-primary"
                disabled={isFormDisabled}
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="date" className="text-foreground">
                Harvest Date <span className="text-destructive">*</span>
              </Label>
              <Input
                id="date"
                type="date"
                value={formData.date}
                onChange={(e) => onFormChange({ ...formData, date: e.target.value })}
                className="rounded-xl border-border focus:border-primary"
                disabled={isFormDisabled}
              />
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div className="space-y-2">
              <Label htmlFor="quantity" className="text-foreground">
                Quantity ({weightUnitLabel}) <span className="text-destructive">*</span>
              </Label>
              <Input
                id="quantity"
                type="number"
                placeholder="0"
                value={formData.quantity}
                onChange={(e) => onFormChange({ ...formData, quantity: e.target.value })}
                className="rounded-xl border-border focus:border-primary"
                disabled={isFormDisabled}
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="grade" className="text-foreground">
                Grade <span className="text-destructive">*</span>
              </Label>
              <Select
                value={formData.grade}
                onValueChange={(value: HarvestGrade) => onFormChange({ ...formData, grade: value })}
                disabled={isFormDisabled}
              >
                <SelectTrigger className="rounded-xl border-border">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {GRADE_OPTIONS.map((option) => (
                    <SelectItem key={option.value} value={option.value}>
                      {option.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            <div className="space-y-2">
              <Label htmlFor="moisture" className="text-foreground">
                Moisture %
              </Label>
              <Input
                id="moisture"
                type="number"
                step="0.1"
                placeholder="0.0"
                value={formData.moisture}
                onChange={(e) => onFormChange({ ...formData, moisture: e.target.value })}
                className="rounded-xl border-border focus:border-primary"
                disabled={isFormDisabled}
              />
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div className="space-y-2">
              <Label htmlFor="season" className="text-foreground">Season</Label>
              {isSeasonLocked ? (
                <div className="rounded-xl border border-border px-3 py-2 text-sm bg-muted/30">
                  {lockedSeasonLabel || "Current season"}
                </div>
              ) : (
                <Select
                  value={formData.season}
                  onValueChange={(value: string) => onFormChange({ ...formData, season: value })}
                  disabled={isFormDisabled}
                >
                  <SelectTrigger className="rounded-xl border-border">
                    <SelectValue placeholder="Select season" />
                  </SelectTrigger>
                  <SelectContent>
                    {seasonOptions.map((option) => (
                      <SelectItem key={option.value} value={option.value}>
                        {option.label}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              )}
            </div>

            <div className="space-y-2">
              <Label htmlFor="warehouse" className="text-foreground">
                Warehouse <span className="text-destructive">*</span>
              </Label>
              <Select
                value={formData.warehouseId}
                onValueChange={(value: string) =>
                  onFormChange({ ...formData, warehouseId: value, locationId: "" })
                }
                disabled={isFormDisabled || !hasOutputWarehouse}
              >
                <SelectTrigger className="rounded-xl border-border">
                  <SelectValue placeholder="Select warehouse" />
                </SelectTrigger>
                <SelectContent>
                  {warehouseOptions.map((option) => (
                    <SelectItem key={option.value} value={option.value}>
                      {option.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            <div className="space-y-2">
              <Label htmlFor="location" className="text-foreground">Location (Optional)</Label>
              <Select
                value={formData.locationId}
                onValueChange={(value: string) => onFormChange({ ...formData, locationId: value })}
                disabled={isFormDisabled || !hasWarehouse}
              >
                <SelectTrigger className="rounded-xl border-border">
                  <SelectValue placeholder="Select location" />
                </SelectTrigger>
                <SelectContent>
                  {locationOptions.map((option) => (
                    <SelectItem key={option.value} value={option.value}>
                      {option.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label htmlFor="productName" className="text-foreground">
                Product <span className="text-destructive">*</span>
              </Label>
              <Input
                id="productName"
                list="harvest-product-name-options"
                placeholder="Enter product name"
                value={formData.productName}
                onChange={(e) => {
                  const value = e.target.value;
                  const matched = productMapByName.get(value);
                  onFormChange({
                    ...formData,
                    productName: value,
                    productId:
                      matched?.productId && Number.isFinite(matched.productId)
                        ? String(matched.productId)
                        : "",
                    productVariant:
                      matched?.productVariant && !formData.productVariant
                        ? matched.productVariant
                        : formData.productVariant,
                  });
                }}
                className="rounded-xl border-border focus:border-primary"
                disabled={isFormDisabled}
              />
              <datalist id="harvest-product-name-options">
                {productOptions.map((option) => (
                  <option key={option.productName} value={option.productName} />
                ))}
              </datalist>
            </div>

            <div className="space-y-2">
              <Label htmlFor="lotCode" className="text-foreground">
                Lot Number <span className="text-destructive">*</span>
              </Label>
              <Input
                id="lotCode"
                list="harvest-lot-options"
                placeholder="Enter or select lot number"
                value={formData.lotCode}
                onChange={(e) => onFormChange({ ...formData, lotCode: e.target.value })}
                className="rounded-xl border-border focus:border-primary"
                disabled={isFormDisabled}
              />
              <datalist id="harvest-lot-options">
                {lotCodeOptions.map((lotCode) => (
                  <option key={lotCode} value={lotCode} />
                ))}
              </datalist>
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div className="space-y-2">
              <Label htmlFor="productVariant" className="text-foreground">Product Variant</Label>
              <Input
                id="productVariant"
                placeholder="Optional"
                value={formData.productVariant}
                onChange={(e) => onFormChange({ ...formData, productVariant: e.target.value })}
                className="rounded-xl border-border focus:border-primary"
                disabled={isFormDisabled}
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="inventoryUnit" className="text-foreground">Inventory Unit</Label>
              <Input
                id="inventoryUnit"
                placeholder="kg"
                value={formData.inventoryUnit}
                onChange={(e) => onFormChange({ ...formData, inventoryUnit: e.target.value })}
                className="rounded-xl border-border focus:border-primary"
                disabled={isFormDisabled}
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="productId" className="text-foreground">Product ID (Optional)</Label>
              <Input
                id="productId"
                type="number"
                placeholder="Auto-filled if matched"
                value={formData.productId}
                onChange={(e) => onFormChange({ ...formData, productId: e.target.value })}
                className="rounded-xl border-border focus:border-primary"
                disabled={isFormDisabled}
              />
            </div>
          </div>

          <div className="rounded-xl border border-border bg-muted/20 px-4 py-3">
            <p className="text-sm font-medium text-foreground">Current lot stock context</p>
            {!contextSeasonId || !hasWarehouse || !formData.productName || !formData.lotCode ? (
              <p className="text-xs text-muted-foreground mt-1">
                Select warehouse, product, and lot to preview current stock context.
              </p>
            ) : stockContextQuery.isLoading ? (
              <p className="text-xs text-muted-foreground mt-1">Loading stock context...</p>
            ) : stockContextQuery.data ? (
              <div className="mt-2 grid grid-cols-1 md:grid-cols-3 gap-2 text-xs">
                <div className="rounded-md border border-border bg-card px-3 py-2">
                  Warehouse: <span className="font-medium">{stockContextQuery.data.warehouseName || "-"}</span>
                </div>
                <div className="rounded-md border border-border bg-card px-3 py-2">
                  Matching lots: <span className="font-medium">{stockContextQuery.data.matchingLots}</span>
                </div>
                <div className="rounded-md border border-border bg-card px-3 py-2">
                  On-hand: <span className="font-medium">{stockContextQuery.data.onHandQuantity} {stockContextQuery.data.unit || ""}</span>
                </div>
              </div>
            ) : (
              <p className="text-xs text-muted-foreground mt-1">No stock context found.</p>
            )}
          </div>

          <Separator className="bg-border" />

          <div>
            <h4 className="text-sm text-foreground mb-4 flex items-center gap-2">
              <ClipboardCheck className="w-4 h-4 text-primary" />
              Quality Control Metrics (Optional)
            </h4>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              <div className="space-y-2">
                <Label htmlFor="purity" className="text-foreground">Purity %</Label>
                <Input
                  id="purity"
                  type="number"
                  step="0.1"
                  placeholder="0.0"
                  value={formData.purity}
                  onChange={(e) => onFormChange({ ...formData, purity: e.target.value })}
                  className="rounded-xl border-border focus:border-primary"
                  disabled={isFormDisabled}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="foreignMatter" className="text-foreground">Foreign Matter %</Label>
                <Input
                  id="foreignMatter"
                  type="number"
                  step="0.1"
                  placeholder="0.0"
                  value={formData.foreignMatter}
                  onChange={(e) => onFormChange({ ...formData, foreignMatter: e.target.value })}
                  className="rounded-xl border-border focus:border-primary"
                  disabled={isFormDisabled}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="brokenGrains" className="text-foreground">Broken Grains %</Label>
                <Input
                  id="brokenGrains"
                  type="number"
                  step="0.1"
                  placeholder="0.0"
                  value={formData.brokenGrains}
                  onChange={(e) => onFormChange({ ...formData, brokenGrains: e.target.value })}
                  className="rounded-xl border-border focus:border-primary"
                  disabled={isFormDisabled}
                />
              </div>
            </div>
          </div>

          <div className="space-y-2">
            <Label htmlFor="notes" className="text-foreground">Notes</Label>
            <Textarea
              id="notes"
              placeholder="Add any additional notes about this harvest batch..."
              value={formData.notes}
              onChange={(e) => onFormChange({ ...formData, notes: e.target.value })}
              className="rounded-xl border-border focus:border-primary min-h-[100px]"
              disabled={isFormDisabled}
            />
          </div>
        </div>

        <DialogFooter>
          <Button
            variant="outline"
            onClick={onCancel}
            className="rounded-xl border-border"
            disabled={isSubmitting}
            disabledHint="Harvest batch is being saved"
          >
            <X className="w-4 h-4 mr-2" />
            Cancel
          </Button>
          <Button
            onClick={onSubmit}
            className="rounded-xl"
            disabled={!canSubmit}
            disabledHint={isSubmitting ? "Harvest batch is being saved" : undefined}
          >
            {isSubmitting ? (
              <Loader2 className="w-4 h-4 mr-2 animate-spin" />
            ) : (
              <Save className="w-4 h-4 mr-2" />
            )}
            {isSubmitting ? "Saving..." : "Save Batch"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}


