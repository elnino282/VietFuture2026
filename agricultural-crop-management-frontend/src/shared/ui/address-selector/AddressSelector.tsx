/**
 * AddressSelector Component
 *
 * Cascading address selector for Vietnamese addresses using Backend API
 * Province > Ward (2-level hierarchy)
 */

import { Loader2, MapPin, X } from "lucide-react";
import { useId } from "react";
import { useTranslation } from "react-i18next";
import { Button } from "../button";
import { Label } from "../label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "../select";
import { useVietnameseAddress } from "./useVietnameseAddress";

export interface AddressValue {
  provinceId: number | null;
  wardId: number | null;
}

export interface AddressSelectorProps {
  /**
   * Current address value with provinceId and wardId
   */
  value?: AddressValue | null;

  /**
   * Callback when address selection changes
   */
  onChange: (address: AddressValue) => void;

  /**
   * Error message to display
   */
  error?: string;

  /**
   * Whether the selector is disabled
   */
  disabled?: boolean;

  /**
   * Whether the selector is required
   */
  required?: boolean;

  /**
   * Label for the address section
   */
  label?: string;

  /**
   * Description text below the label
   */
  description?: string;
}

/**
 * AddressSelector Component
 *
 * Two-level cascading dropdown for selecting Vietnamese addresses from backend API
 * Features:
 * - Province dropdown (always enabled)
 * - Ward dropdown (enabled after province selected)
 * - Clear button to reset all selections
 * - Breadcrumb display of selected address
 * - Loading states for each dropdown
 * - Auto-populate from initial ward ID (for edit mode)
 */
export function AddressSelector({
  value,
  onChange,
  error,
  disabled = false,
  required = false,
  label,
  description,
}: AddressSelectorProps) {
  const { t } = useTranslation();
  const errorId = useId();
  const {
    provinces,
    wards,
    selectedProvince,
    selectedWard,
    isLoadingProvinces,
    isLoadingWards,
    isLoadingInitial,
    handleProvinceChange,
    handleWardChange,
    clearSelection,
    getAddressBreadcrumb,
  } = useVietnameseAddress({
    initialWardCode: value?.wardId ?? null,
    onAddressChange: (provinceId, wardId) => {
      onChange({ provinceId, wardId });
    },
  });

  const addressBreadcrumb = getAddressBreadcrumb();
  const hasSelection = selectedProvince !== null || selectedWard !== null;
  const resolvedLabel = label ?? t("common.addressSelector.address");

  return (
    <div className="space-y-3">
      {/* Label */}
      <div className="flex items-center justify-between">
        <Label>
          <MapPin className="inline-block w-4 h-4 mr-1.5 -mt-0.5" />
          {resolvedLabel}
          {required && <span className="text-destructive ml-1">*</span>}
        </Label>

        {/* Clear button */}
        {hasSelection && !disabled && (
          <Button
            type="button"
            variant="ghost"
            size="sm"
            onClick={clearSelection}
            className="h-7 px-2 text-xs"
          >
            <X className="w-3 h-3 mr-1" />
            {t("common.addressSelector.clear")}
          </Button>
        )}
      </div>

      {/* Description */}
      {description && (
        <p className="text-xs text-muted-foreground -mt-1">{description}</p>
      )}

      {/* Loading initial state */}
      {isLoadingInitial && (
        <div className="flex items-center gap-2 py-3 px-4 bg-muted border border-border rounded-lg">
          <Loader2 className="w-4 h-4 animate-spin text-muted-foreground" />
          <span className="text-sm text-foreground">{t("common.loadingAddress")}</span>
        </div>
      )}

      {/* Address breadcrumb */}
      {addressBreadcrumb && !isLoadingInitial && (
        <div className="py-2 px-3 bg-primary/10 border border-primary/20 rounded-lg">
          <p className="text-sm text-foreground font-medium">
            {addressBreadcrumb}
          </p>
        </div>
      )}

      {/* Cascading selectors */}
      <div className="space-y-3">
        {/* Province selector */}
        <div className="space-y-1.5">
          <Label htmlFor="province" className="text-xs text-muted-foreground">
            {t("common.addressSelector.province")}
            {required && <span className="text-destructive ml-1">*</span>}
          </Label>
          <Select
            value={selectedProvince?.toString() || ""}
            onValueChange={(value) =>
              handleProvinceChange(value ? Number(value) : null)
            }
            disabled={disabled || isLoadingProvinces || isLoadingInitial}
          >
            <SelectTrigger
              id="province"
              className={`min-h-[44px] ${error ? "border-destructive" : ""}`}
              aria-invalid={!!error}
              aria-describedby={error ? errorId : undefined}
            >
              <SelectValue placeholder={t("common.addressSelector.provincePlaceholder")} />
            </SelectTrigger>
            <SelectContent>
              {isLoadingProvinces ? (
                <div className="flex items-center justify-center py-4">
                  <Loader2 className="w-4 h-4 animate-spin" />
                  <span className="ml-2 text-sm">{t("common.loading")}</span>
                </div>
              ) : provinces.length === 0 ? (
                <div className="py-4 text-center text-sm text-muted-foreground">
                  {t("common.addressSelector.noProvinces")}
                </div>
              ) : (
                provinces.map((province) => (
                  <SelectItem key={province.id} value={province.id.toString()}>
                    {province.nameWithType}
                  </SelectItem>
                ))
              )}
            </SelectContent>
          </Select>
        </div>

        {/* Ward selector */}
        <div className="space-y-1.5">
          <Label htmlFor="ward" className="text-xs text-muted-foreground">
            {t("common.addressSelector.ward")}
            {required && <span className="text-destructive ml-1">*</span>}
          </Label>
          <Select
            value={selectedWard?.toString() || ""}
            onValueChange={(value) =>
              handleWardChange(value ? Number(value) : null)
            }
            disabled={
              disabled ||
              !selectedProvince ||
              isLoadingWards ||
              isLoadingInitial
            }
          >
            <SelectTrigger
              id="ward"
              className={`min-h-[44px] ${error ? "border-destructive" : ""}`}
              aria-invalid={!!error}
              aria-describedby={error ? errorId : undefined}
            >
              <SelectValue placeholder={t("common.addressSelector.wardPlaceholder")} />
            </SelectTrigger>
            <SelectContent>
              {isLoadingWards ? (
                <div className="flex items-center justify-center py-4">
                  <Loader2 className="w-4 h-4 animate-spin" />
                  <span className="ml-2 text-sm">{t("common.loading")}</span>
                </div>
              ) : !selectedProvince ? (
                <div className="py-4 text-center text-sm text-muted-foreground">
                  {t("common.addressSelector.selectProvinceFirst")}
                </div>
              ) : wards.length === 0 ? (
                <div className="py-4 text-center text-sm text-muted-foreground">
                  {t("common.addressSelector.noWards")}
                </div>
              ) : (
                wards.map((ward) => (
                  <SelectItem key={ward.id} value={ward.id.toString()}>
                    {ward.nameWithType}
                  </SelectItem>
                ))
              )}
            </SelectContent>
          </Select>
        </div>
      </div>

      {/* Error message */}
      {error && (
        <p id={errorId} role="alert" aria-live="polite" className="text-sm text-destructive mt-1">
          {error}
        </p>
      )}
    </div>
  );
}
