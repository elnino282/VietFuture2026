import { useEffect, useMemo, useState } from "react";
import { Plus, Check } from "lucide-react";
import { useTranslation } from "react-i18next";
import { Button } from "@/shared/ui/button";
import { BackButton } from "@/shared/ui/back-button";
import { Input } from "@/shared/ui/input";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/shared/ui/dialog";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/shared/ui/select";
import { Label } from "@/shared/ui/label";
import type { PlotRequest } from "@/entities/plot";
import { useFarms } from "@/entities/farm";
import { PLOT_STATUS_OPTIONS, SOIL_TYPE_OPTIONS } from "@/features/farmer/shared/plotOptions";



interface AddPlotDialogProps {
  isOpen: boolean;
  onClose: () => void;
  onSubmit: (formData: PlotRequest) => void;
  isSubmitting?: boolean;
}

/**
 * AddPlotDialog Component
 *
 * Dialog for adding a new plot with form inputs matching backend API structure.
 * 
 * Backend expects:
 * - plotName: string
 * - area: number (in hectares)
 * - soilType: string
 * - status: backend status enum
 */
export function AddPlotDialog({
  isOpen,
  onClose,
  onSubmit,
  isSubmitting = false
}: AddPlotDialogProps) {
  const { t } = useTranslation();
  const { data: farmsData, isLoading: isLoadingFarms } = useFarms({ page: 0, size: 100 });
  const activeFarms = useMemo(
    () => (farmsData?.content ?? []).filter((farm) => farm.active),
    [farmsData]
  );

  // Form state
  const [farmId, setFarmId] = useState("");
  const [plotName, setPlotName] = useState("");
  const [areaValue, setAreaValue] = useState("");
  const [soilType, setSoilType] = useState<string>("");
  const [plotStatus, setPlotStatus] = useState<NonNullable<PlotRequest["status"]>>("IN_USE");
  const [errors, setErrors] = useState<Record<string, string>>({});

  const resetForm = () => {
    setFarmId(activeFarms.length === 1 ? String(activeFarms[0].id) : "");
    setPlotName("");
    setAreaValue("");
    setSoilType("");
    setPlotStatus("IN_USE");
    setErrors({});
  };

  const handleClose = () => {
    if (isSubmitting) return;
    const isDirty =
      plotName.trim().length > 0 ||
      areaValue.length > 0 ||
      soilType.length > 0 ||
      plotStatus !== "IN_USE" ||
      (farmId.length > 0 && activeFarms.length !== 1);

    if (
      isDirty &&
      !window.confirm(t("common.unsavedChangesConfirm", "You have unsaved changes. Leave this page?"))
    ) {
      return;
    }

    resetForm();
    onClose();
  };

  const handleDialogOpenChange = (nextOpen: boolean) => {
    if (isSubmitting && !nextOpen) return;
    if (!nextOpen) handleClose();
  };

  useEffect(() => {
    if (!isOpen) return;
    if (!farmId && activeFarms.length === 1) {
      setFarmId(String(activeFarms[0].id));
    }
  }, [activeFarms, farmId, isOpen]);

  const handleSubmit = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();

    const nextErrors: Record<string, string> = {};
    if (!farmId) {
      nextErrors.farmId = t("farms.validation.farmRequired");
    }
    if (!plotName.trim()) {
      nextErrors.plotName = t("farms.validation.plotNameRequired");
    }
    if (!areaValue || parseFloat(areaValue) <= 0) {
      nextErrors.area = t("farms.validation.areaPositive");
    }
    if (!soilType) {
      nextErrors.soilType = t("farms.validation.soilTypeRequired");
    }

    setErrors(nextErrors);
    if (Object.keys(nextErrors).length > 0) {
      return;
    }

    const formData: PlotRequest = {
      farmId: Number.parseInt(farmId, 10),
      plotName: plotName.trim(),
      area: parseFloat(areaValue),
      soilType,
      status: plotStatus,
    };

    onSubmit(formData);
  };

  return (
    <Dialog open={isOpen} onOpenChange={handleDialogOpenChange}>
      <DialogContent className="sm:max-w-[560px]" closeDisabled={isSubmitting}>
        <DialogHeader>
          <BackButton onClick={handleClose} className="w-fit" />
          <DialogTitle className="flex items-center gap-2 text-foreground">
            <Plus className="w-5 h-5 text-primary" />
            {t("farms.dialog.createPlotTitle")}
          </DialogTitle>
          <DialogDescription>
            {t("farms.dialog.createStandalonePlotDescription")}
          </DialogDescription>
        </DialogHeader>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="space-y-4">
            {/* Farm */}
            <div className="space-y-2">
              <Label htmlFor="farmId" required>{t("seasons.form.farm")}</Label>
              <Select
                value={farmId}
                onValueChange={(value) => {
                  setFarmId(value);
                  setErrors((prev) => ({ ...prev, farmId: "" }));
                }}
                required
                disabled={isSubmitting || isLoadingFarms || activeFarms.length === 0}
              >
                <SelectTrigger
                  id="farmId"
                  aria-invalid={!!errors.farmId}
                  aria-describedby={errors.farmId ? "add-plot-farm-error" : undefined}
                >
                  <SelectValue
                    placeholder={
                      isLoadingFarms
                        ? t("farms.loading")
                        : t("seasons.form.selectFarm")
                    }
                  />
                </SelectTrigger>
                <SelectContent>
                  {activeFarms.length === 0 ? (
                    <div className="px-3 py-2 text-sm text-muted-foreground">
                      {t("farmManagement.noFarms")}
                    </div>
                  ) : (
                    activeFarms.map((farm) => (
                      <SelectItem key={farm.id} value={String(farm.id)}>
                        {farm.name}
                      </SelectItem>
                    ))
                  )}
                </SelectContent>
              </Select>
              {errors.farmId && (
                <p id="add-plot-farm-error" className="text-sm text-destructive">
                  {errors.farmId}
                </p>
              )}
            </div>

            {/* Plot Name */}
            <div className="space-y-2">
              <Label htmlFor="plotName" required>{t("farms.form.plotName")}</Label>
              <Input
                id="plotName"
                value={plotName}
                onChange={(e) => {
                  setPlotName(e.target.value);
                  setErrors((prev) => ({ ...prev, plotName: "" }));
                }}
                placeholder={t("farms.form.plotNameExample")}
                required
                aria-invalid={!!errors.plotName}
                aria-describedby={errors.plotName ? "add-plot-name-error" : undefined}
                disabled={isSubmitting}
              />
              {errors.plotName && (
                <p id="add-plot-name-error" className="text-sm text-destructive">
                  {errors.plotName}
                </p>
              )}
            </div>

            {/* Area */}
            <div className="space-y-2">
              <Label htmlFor="areaValue" required>{t("farms.form.areaHectares")}</Label>
              <Input
                id="areaValue"
                type="number"
                step="0.01"
                min="0.01"
                value={areaValue}
                onChange={(e) => {
                  setAreaValue(e.target.value);
                  setErrors((prev) => ({ ...prev, area: "" }));
                }}
                placeholder={t("farms.form.areaExample")}
                required
                aria-invalid={!!errors.area}
                aria-describedby={errors.area ? "add-plot-area-error" : undefined}
                disabled={isSubmitting}
              />
              {errors.area && (
                <p id="add-plot-area-error" className="text-sm text-destructive">
                  {errors.area}
                </p>
              )}
            </div>

            {/* Soil Type */}
            <div className="space-y-2">
              <Label htmlFor="soilType" required>{t("farms.form.soilType")}</Label>
              <Select
                value={soilType}
                onValueChange={(value) => {
                  setSoilType(value);
                  setErrors((prev) => ({ ...prev, soilType: "" }));
                }}
                required
                disabled={isSubmitting}
              >
                <SelectTrigger
                  id="soilType"
                  aria-invalid={!!errors.soilType}
                  aria-describedby={errors.soilType ? "add-plot-soil-type-error" : undefined}
                >
                  <SelectValue placeholder={t("farms.form.selectSoilType")} />
                </SelectTrigger>
                <SelectContent>
                  {SOIL_TYPE_OPTIONS.map((soil) => (
                    <SelectItem key={soil.value} value={soil.value}>
                      {t(soil.labelKey)}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              {errors.soilType && (
                <p id="add-plot-soil-type-error" className="text-sm text-destructive">
                  {errors.soilType}
                </p>
              )}
            </div>

            {/* Plot Status */}
            <div className="space-y-2">
              <Label htmlFor="plotStatus" required>{t("farms.form.status")}</Label>
              <Select
                value={plotStatus}
                onValueChange={(value) => setPlotStatus(value as NonNullable<PlotRequest["status"]>)}
                required
                disabled={isSubmitting}
              >
                <SelectTrigger id="plotStatus">
                  <SelectValue placeholder={t("farms.form.selectStatus")} />
                </SelectTrigger>
                <SelectContent>
                  {PLOT_STATUS_OPTIONS.map((status) => (
                    <SelectItem key={status.value} value={status.value}>
                      {t(status.labelKey)}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          </div>

          <DialogFooter>
            <Button
              type="button"
              variant="outline"
              onClick={handleClose}
              disabled={isSubmitting}
            >
              {t("common.cancel")}
            </Button>
            <Button
              type="submit"
              disabled={isSubmitting}
            >
              {isSubmitting ? (
                <>
                  <span className="mr-2 h-4 w-4 animate-spin rounded-full border-2 border-white border-t-transparent" />
                  {t("common.creating")}
                </>
              ) : (
                <>
                  <Check className="w-4 h-4 mr-2" />
                  {t("farms.dialog.createPlotTitle")}
                </>
              )}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}




