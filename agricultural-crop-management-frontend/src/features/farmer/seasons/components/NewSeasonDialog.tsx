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
import { useCrops } from "@/entities/crop";
import { useFarms } from "@/entities/farm";
import { usePlotsByFarm } from "@/entities/plot";
import type { SeasonCreateRequest } from "@/entities/season";
import { useVarietiesByCrop } from "@/entities/variety";
import { usePreferences } from "@/shared/contexts";
import { convertWeightToKg, getWeightUnitLabel } from "@/shared/lib";
import {
  BackButton,
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/shared/ui";
import { Calendar, Check } from "lucide-react";
import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";

interface NewSeasonDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onSubmit: (data: SeasonCreateRequest) => void;
  isSubmitting?: boolean;
}

/**
 * NewSeasonDialog Component
 *
 * Dialog for creating a new growing season matching backend API structure.
 *
 * Backend expects (POST /api/v1/seasons):
 * - plotId: number (required) - ID of the plot where the season will be planted
 * - cropId: number (required) - ID of the crop type
 * - varietyId: number (optional) - ID of the specific variety
 * - seasonName: string (required) - Name of the season
 * - startDate: string (required) - Start date in YYYY-MM-DD format
 * - plannedHarvestDate: string (optional) - Planned harvest date
 * - endDate: string (optional) - End date
 * - initialPlantCount: number (required) - Number of plants at start
 * - expectedYieldKg: number (optional) - Expected yield in canonical kg
 * - notes: string (optional) - Additional notes
 */
export function NewSeasonDialog({
  open,
  onOpenChange,
  onSubmit,
  isSubmitting = false,
}: NewSeasonDialogProps) {
  const { t } = useTranslation();
  const { preferences } = usePreferences();
  const unitLabel = getWeightUnitLabel(preferences.weightUnit);
  const weightStep = preferences.weightUnit === "G" ? "1" : "0.01";

  // Form state
  const [farmId, setFarmId] = useState<string>("");
  const [plotId, setPlotId] = useState<string>("");
  const [cropId, setCropId] = useState<string>("");
  const [varietyId, setVarietyId] = useState<string>("");
  const [seasonName, setSeasonName] = useState("");
  const [startDate, setStartDate] = useState("");
  const [plannedHarvestDate, setPlannedHarvestDate] = useState("");
  const [endDate, setEndDate] = useState("");
  const [initialPlantCount, setInitialPlantCount] = useState("");
  const [expectedYieldKg, setExpectedYieldKg] = useState("");
  const [budgetAmount, setBudgetAmount] = useState("");
  const [notes, setNotes] = useState("");

  const parseWeightInput = (value: string) => {
    if (value === "") return undefined;
    const parsed = parseFloat(value);
    if (Number.isNaN(parsed)) return undefined;
    return convertWeightToKg(parsed, preferences.weightUnit);
  };

  const parseBudgetInput = (value: string) => {
    if (value === "") return undefined;
    const parsed = parseFloat(value);
    if (Number.isNaN(parsed)) return undefined;
    return parsed;
  };

  // Fetch farms for selection
  const { data: farmsData } = useFarms({ page: 0, size: 100 });
  const farms = farmsData?.content ?? [];

  // Fetch plots based on selected farm
  const selectedFarmId = farmId ? parseInt(farmId, 10) : 0;
  const { data: plotsData } = usePlotsByFarm(selectedFarmId, {
    page: 0,
    size: 100,
  });
  const plots = plotsData?.items ?? [];

  // Fetch crops for dropdown
  const { data: crops = [] } = useCrops();

  // Fetch varieties based on selected crop
  const selectedCropId = cropId ? parseInt(cropId, 10) : 0;
  const { data: varieties = [] } = useVarietiesByCrop(selectedCropId);

  // Reset plot when farm changes
  useEffect(() => {
    setPlotId("");
  }, [farmId]);

  // Reset variety when crop changes
  useEffect(() => {
    setVarietyId("");
  }, [cropId]);

  const initialPlantValue =
    initialPlantCount === "" ? null : parseInt(initialPlantCount, 10);
  const hasValidPlantCount =
    initialPlantValue !== null &&
    !Number.isNaN(initialPlantValue) &&
    initialPlantValue >= 1;
  const hasValidHarvestDate =
    !plannedHarvestDate || plannedHarvestDate >= startDate;

  const isFormValid =
    farmId &&
    plotId &&
    cropId &&
    seasonName.trim() &&
    startDate &&
    hasValidPlantCount &&
    hasValidHarvestDate;

  const resetForm = () => {
    setFarmId("");
    setPlotId("");
    setCropId("");
    setVarietyId("");
    setSeasonName("");
    setStartDate("");
    setPlannedHarvestDate("");
    setEndDate("");
    setInitialPlantCount("");
    setExpectedYieldKg("");
    setBudgetAmount("");
    setNotes("");
  };

  const isDirty =
    farmId.length > 0 ||
    plotId.length > 0 ||
    cropId.length > 0 ||
    varietyId.length > 0 ||
    seasonName.trim().length > 0 ||
    startDate.length > 0 ||
    plannedHarvestDate.length > 0 ||
    endDate.length > 0 ||
    initialPlantCount.length > 0 ||
    expectedYieldKg.length > 0 ||
    budgetAmount.length > 0 ||
    notes.trim().length > 0;

  const handleClose = () => {
    if (isSubmitting) return;
    if (
      isDirty &&
      !window.confirm(t("common.unsavedChangesConfirm", "You have unsaved changes. Leave this page?"))
    ) {
      return;
    }
    resetForm();
    onOpenChange(false);
  };

  const handleSubmit = () => {
    if (!isFormValid) return;

    const formData: SeasonCreateRequest = {
      plotId: parseInt(plotId, 10),
      cropId: parseInt(cropId, 10),
      varietyId: varietyId ? parseInt(varietyId, 10) : undefined,
      seasonName: seasonName.trim(),
      startDate: startDate,
      plannedHarvestDate: plannedHarvestDate || undefined,
      endDate: endDate || undefined,
      initialPlantCount: parseInt(initialPlantCount, 10),
      expectedYieldKg: parseWeightInput(expectedYieldKg),
      budgetAmount: parseBudgetInput(budgetAmount),
      notes: notes.trim() || undefined,
    };

    onSubmit(formData);
  };

  return (
    <Dialog
      open={open}
      onOpenChange={(isOpen) => {
        if (isSubmitting && !isOpen) return;
        if (!isOpen) handleClose();
        else onOpenChange(isOpen);
      }}
    >
      <DialogContent className="sm:max-w-[600px]" closeDisabled={isSubmitting}>
        <DialogHeader>
          <BackButton onClick={handleClose} className="w-fit" />
          <DialogTitle className="flex items-center gap-2 text-foreground">
            <Calendar className="w-5 h-5 text-primary" />
            {t("seasons.dialog.createTitle")}
          </DialogTitle>
          <DialogDescription>
            {t("seasons.dialog.createDescription")}
          </DialogDescription>
        </DialogHeader>

        <div className="grid gap-4 py-2">
          {/* Season Name - Full Width */}
          <div className="space-y-2">
            <Label htmlFor="seasonName" required>{t("seasons.form.seasonName")}</Label>
            <Input
              id="seasonName"
              value={seasonName}
              onChange={(e) => setSeasonName(e.target.value)}
              placeholder={t("seasons.form.seasonNamePlaceholder")}
              required
              disabled={isSubmitting}
            />
          </div>

          {/* Farm and Plot Selection */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label htmlFor="farmId" required>{t("seasons.table.farm")}</Label>
              <Select value={farmId} onValueChange={setFarmId} disabled={isSubmitting}>
                <SelectTrigger id="farmId">
                  <SelectValue placeholder={t("seasons.form.selectFarm")} />
                </SelectTrigger>
                <SelectContent>
                  {farms.map((farm) => (
                    <SelectItem key={farm.id} value={String(farm.id)}>
                      {farm.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            <div className="space-y-2">
              <Label htmlFor="plotId" required>{t("seasons.table.plot")}</Label>
              <Select
                value={plotId}
                onValueChange={setPlotId}
                disabled={isSubmitting || !farmId || plots.length === 0}
              >
                <SelectTrigger id="plotId">
                  <SelectValue
                    placeholder={
                      !farmId
                        ? t("seasons.form.selectFarmFirst")
                        : plots.length === 0
                          ? t("seasons.form.noPlotsAvailable")
                          : t("seasons.form.selectPlot")
                    }
                  />
                </SelectTrigger>
                <SelectContent>
                  {plots.map((plot) => (
                    <SelectItem key={plot.id} value={String(plot.id)}>
                      {plot.plotName}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          </div>

          {/* Crop and Variety Selection */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label htmlFor="cropId" required>{t("seasons.table.crop")}</Label>
              <Select value={cropId} onValueChange={setCropId} disabled={isSubmitting}>
                <SelectTrigger id="cropId">
                  <SelectValue placeholder={t("seasons.form.selectCrop")} />
                </SelectTrigger>
                <SelectContent>
                  {crops.map((crop) => (
                    <SelectItem key={crop.id} value={String(crop.id)}>
                      {crop.cropName}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            <div className="space-y-2">
              <Label htmlFor="varietyId">{t("seasons.form.variety")}</Label>
              <Select
                value={varietyId}
                onValueChange={setVarietyId}
                disabled={isSubmitting || !cropId || varieties.length === 0}
              >
                <SelectTrigger id="varietyId">
                  <SelectValue
                    placeholder={
                      !cropId
                        ? t("seasons.form.selectCropFirst")
                        : varieties.length === 0
                          ? t("seasons.form.noVarietiesAvailable")
                          : t("seasons.form.selectVariety")
                    }
                  />
                </SelectTrigger>
                <SelectContent>
                  {varieties.map((variety) => (
                    <SelectItem key={variety.id} value={String(variety.id)}>
                      {variety.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          </div>

          {/* Dates Section */}
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <div className="space-y-2">
              <Label htmlFor="startDate" required>{t("seasons.table.startDate")}</Label>
              <Input
                id="startDate"
                type="date"
                value={startDate}
                onChange={(e) => setStartDate(e.target.value)}
                required
                disabled={isSubmitting}
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="plannedHarvestDate">{t("seasons.form.plannedHarvestDate")}</Label>
              <Input
                id="plannedHarvestDate"
                type="date"
                value={plannedHarvestDate}
                onChange={(e) => setPlannedHarvestDate(e.target.value)}
                min={startDate}
                disabled={isSubmitting}
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="endDate">{t("seasons.table.endDate")}</Label>
              <Input
                id="endDate"
                type="date"
                value={endDate}
                onChange={(e) => setEndDate(e.target.value)}
                min={startDate}
                disabled={isSubmitting}
              />
            </div>
          </div>

          {/* Plant Count and Yield */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label htmlFor="initialPlantCount" required>{t("seasons.form.initialPlantCount")}</Label>
              <Input
                id="initialPlantCount"
                type="number"
                min="1"
                value={initialPlantCount}
                onChange={(e) => setInitialPlantCount(e.target.value)}
                placeholder={t("seasons.form.initialPlantCountPlaceholder")}
                required
                disabled={isSubmitting}
              />
              <p className="text-xs text-muted-foreground">
                {t("seasons.form.initialPlantCountHelp")}
              </p>
            </div>

            <div className="space-y-2">
              <Label htmlFor="expectedYieldKg">
                {t("seasons.form.expectedYield", { unit: unitLabel })}
              </Label>
              <Input
                id="expectedYieldKg"
                type="number"
                min="0"
                step={weightStep}
                value={expectedYieldKg}
                onChange={(e) => setExpectedYieldKg(e.target.value)}
                placeholder={t("seasons.form.expectedYieldPlaceholder")}
                disabled={isSubmitting}
              />
              <p className="text-xs text-muted-foreground">
                {t("seasons.form.expectedYieldHelp")}
              </p>
            </div>
          </div>

          <div className="space-y-2">
            <Label htmlFor="budgetAmount">
              {t("seasons.form.budget", { currency: preferences.currency })}
            </Label>
            <Input
              id="budgetAmount"
              type="number"
              min="0"
              step="0.01"
              value={budgetAmount}
              onChange={(e) => setBudgetAmount(e.target.value)}
              placeholder={t("seasons.form.budgetPlaceholder")}
              disabled={isSubmitting}
            />
            <p className="text-xs text-muted-foreground">
              {t("seasons.form.budgetHelp")}
            </p>
          </div>

          {/* Notes */}
          <div className="space-y-2">
            <Label htmlFor="notes">{t("common.notes")}</Label>
            <Textarea
              id="notes"
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              placeholder={t("seasons.form.notesPlaceholder")}
              rows={3}
              disabled={isSubmitting}
            />
          </div>
        </div>

        <DialogFooter>
          <Button
            variant="outline"
            onClick={handleClose}
            disabled={isSubmitting}
          >
            {t("common.cancel")}
          </Button>
          <Button
            onClick={handleSubmit}
            disabled={!isFormValid || isSubmitting}
          >
            {isSubmitting ? (
              <>
                <span className="mr-2 h-4 w-4 animate-spin rounded-full border-2 border-white border-t-transparent" />
                {t("common.creating")}
              </>
            ) : (
              <>
                <Check className="w-4 h-4 mr-2" />
                {t("seasons.dialog.createTitle")}
              </>
            )}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
