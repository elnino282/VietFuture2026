import { useState } from "react";
import { Plus, Check } from "lucide-react";
import { useTranslation } from "react-i18next";
import { Button } from "@/shared/ui/button";
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
import { useSoilTypes } from "@/entities/soil-type";
import { usePlotStatuses } from "@/entities/plot-status";



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
 * - addressId: number (optional)
 * - area: number (in hectares)
 * - soilTypeId: number
 * - plotStatusId: number
 */
export function AddPlotDialog({
  isOpen,
  onClose,
  onSubmit,
  isSubmitting = false
}: AddPlotDialogProps) {
  const { t } = useTranslation();
  // Fetch soil types from API
  const { data: soilTypes, isLoading: soilTypesLoading } = useSoilTypes();

  // Fetch plot statuses from API
  const { data: plotStatuses, isLoading: plotStatusesLoading } = usePlotStatuses();

  // Form state
  const [plotName, setPlotName] = useState("");
  const [areaValue, setAreaValue] = useState("");
  const [soilTypeId, setSoilTypeId] = useState<string>("");
  const [plotStatusId, setPlotStatusId] = useState<string>("1"); // Default to Active
  const [errors, setErrors] = useState<Record<string, string>>({});

  const resetForm = () => {
    setPlotName("");
    setAreaValue("");
    setSoilTypeId("");
    setPlotStatusId("1");
    setErrors({});
  };

  const handleClose = () => {
    if (isSubmitting) return;
    resetForm();
    onClose();
  };

  const handleDialogOpenChange = (nextOpen: boolean) => {
    if (isSubmitting && !nextOpen) return;
    if (!nextOpen) handleClose();
  };

  const handleSubmit = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();

    const nextErrors: Record<string, string> = {};
    if (!plotName.trim()) {
      nextErrors.plotName = t("farms.validation.plotNameRequired", "Plot name is required");
    }
    if (!areaValue || parseFloat(areaValue) <= 0) {
      nextErrors.area = t("farms.validation.areaPositive", "Area must be greater than 0");
    }
    if (!soilTypeId) {
      nextErrors.soilType = t("farms.validation.soilTypeRequired", "Soil type is required");
    }

    setErrors(nextErrors);
    if (Object.keys(nextErrors).length > 0) {
      return;
    }

    const selectedSoilType = soilTypes?.find((soil) => String(soil.id) === soilTypeId);
    const selectedStatus = plotStatuses?.find((status) => String(status.id) === plotStatusId);

    const statusName = selectedStatus?.statusName?.toLowerCase() ?? "";
    const mappedStatus: PlotRequest["status"] =
      statusName.includes("maint")
        ? "MAINTENANCE"
        : statusName.includes("fallow")
          ? "FALLOW"
          : statusName.includes("idle") || statusName.includes("dormant")
            ? "IDLE"
            : statusName.includes("use") || statusName.includes("active")
              ? "IN_USE"
              : "AVAILABLE";

    const formData: PlotRequest = {
      plotName: plotName.trim(),
      area: parseFloat(areaValue),
      soilType: selectedSoilType?.soilName,
      status: mappedStatus,
    };

    onSubmit(formData);
  };

  return (
    <Dialog open={isOpen} onOpenChange={handleDialogOpenChange}>
      <DialogContent className="sm:max-w-[560px]" closeDisabled={isSubmitting}>
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2 text-foreground">
            <Plus className="w-5 h-5 text-primary" />
            {t("farms.dialog.createPlotTitle")}
          </DialogTitle>
          <DialogDescription>
            {t("farms.dialog.createStandalonePlotDescription", "Create a new agricultural plot by entering the details below.")}
          </DialogDescription>
        </DialogHeader>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="space-y-4">
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
                value={soilTypeId}
                onValueChange={(value) => {
                  setSoilTypeId(value);
                  setErrors((prev) => ({ ...prev, soilType: "" }));
                }}
                required
                disabled={isSubmitting || soilTypesLoading}
              >
                <SelectTrigger
                  id="soilType"
                  aria-invalid={!!errors.soilType}
                  aria-describedby={errors.soilType ? "add-plot-soil-type-error" : undefined}
                >
                  <SelectValue placeholder={soilTypesLoading ? t("common.loading") : t("farms.form.selectSoilType")} />
                </SelectTrigger>
                <SelectContent>
                  {soilTypes?.map((soil) => (
                    <SelectItem key={soil.id} value={String(soil.id)}>
                      {soil.soilName}
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
              <Label htmlFor="plotStatus" required>{t("common.status")}</Label>
              <Select value={plotStatusId} onValueChange={setPlotStatusId} required disabled={isSubmitting || plotStatusesLoading}>
                <SelectTrigger id="plotStatus">
                  <SelectValue placeholder={plotStatusesLoading ? t("common.loading") : t("farms.form.selectStatus")} />
                </SelectTrigger>
                <SelectContent>
                  {plotStatuses?.map((status) => (
                    <SelectItem key={status.id} value={String(status.id)}>
                      {status.statusName}
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




