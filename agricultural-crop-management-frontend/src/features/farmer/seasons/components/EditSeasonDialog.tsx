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
import type { SeasonUpdateRequest } from "@/entities/season";
import { useVarietiesByCrop } from "@/entities/variety";
import { usePreferences } from "@/shared/contexts";
import {
  convertWeight,
  convertWeightToKg,
  getWeightUnitLabel,
} from "@/shared/lib";
import {
  BackButton,
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/shared/ui";
import { Edit } from "lucide-react";
import { useEffect, useMemo, useRef, useState } from "react";
import { useTranslation } from "react-i18next";
import type { Season } from "../types";

interface EditSeasonDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  season: Season | null;
  onSubmit: (id: number, data: SeasonUpdateRequest) => void;
  isSubmitting?: boolean;
}

const normalizeDate = (value?: string | null) => {
  if (!value) return "";
  if (/^\d{4}-\d{2}-\d{2}$/.test(value)) return value;
  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) return "";
  return parsed.toISOString().slice(0, 10);
};

export function EditSeasonDialog({
  open,
  onOpenChange,
  season,
  onSubmit,
  isSubmitting = false,
}: EditSeasonDialogProps) {
  const { t } = useTranslation();
  const { preferences } = usePreferences();
  const unitLabel = getWeightUnitLabel(preferences.weightUnit);
  const weightStep = preferences.weightUnit === "G" ? "1" : "0.01";
  const [seasonName, setSeasonName] = useState("");
  const [startDate, setStartDate] = useState("");
  const [plannedHarvestDate, setPlannedHarvestDate] = useState("");
  const [endDate, setEndDate] = useState("");
  const [currentPlantCount, setCurrentPlantCount] = useState("");
  const [expectedYieldKg, setExpectedYieldKg] = useState("");
  const [actualYieldKg, setActualYieldKg] = useState("");
  const [budgetAmount, setBudgetAmount] = useState("");
  const [notes, setNotes] = useState("");
  const [varietyId, setVarietyId] = useState("");
  const initialFormRef = useRef<string | null>(null);

  const cropId = season?.cropId ?? 0;
  const { data: varieties = [] } = useVarietiesByCrop(cropId);

  const farmLabel = useMemo(() => {
    if (!season) return "-";
    return season.farmName || "Farm";
  }, [season]);
  const plotLabel = useMemo(() => {
    if (!season) return "-";
    if (season.plotName) return season.plotName;
    if (season.plotId) return `Plot #${season.plotId}`;
    return "Plot";
  }, [season]);
  const cropLabel = useMemo(() => {
    if (!season) return "-";
    return season.crop || "Crop";
  }, [season]);

  const formatWeightInput = (valueKg?: number | null) => {
    if (valueKg == null) return "";
    const converted = convertWeight(valueKg, preferences.weightUnit);
    const decimals = preferences.weightUnit === "G" ? 0 : 2;
    return String(Number(converted.toFixed(decimals)));
  };

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

  useEffect(() => {
    if (!open || !season) return;
    const plantCount = season.currentPlantCount ?? season.initialPlantCount;
    const nextFormState = {
      seasonName: season.name || "",
      startDate: normalizeDate(season.startDate),
      plannedHarvestDate: normalizeDate(season.plannedHarvestDate),
      endDate: normalizeDate(season.endDate),
      currentPlantCount: plantCount != null ? String(plantCount) : "",
      expectedYieldKg: formatWeightInput(season.expectedYieldKg),
      actualYieldKg: formatWeightInput(season.actualYieldKg),
      budgetAmount: season.budgetTotal ? String(season.budgetTotal) : "",
      notes: season.notes || "",
      varietyId: season.varietyId != null ? String(season.varietyId) : "",
    };
    setSeasonName(nextFormState.seasonName);
    setStartDate(nextFormState.startDate);
    setPlannedHarvestDate(nextFormState.plannedHarvestDate);
    setEndDate(nextFormState.endDate);
    setCurrentPlantCount(nextFormState.currentPlantCount);
    setExpectedYieldKg(nextFormState.expectedYieldKg);
    setActualYieldKg(nextFormState.actualYieldKg);
    setBudgetAmount(nextFormState.budgetAmount);
    setNotes(nextFormState.notes);
    setVarietyId(nextFormState.varietyId);
    initialFormRef.current = JSON.stringify(nextFormState);
  }, [open, season?.id, preferences.weightUnit]);

  const currentPlantValue =
    currentPlantCount === "" ? null : parseInt(currentPlantCount, 10);
  const hasValidPlantCount =
    currentPlantValue !== null &&
    !Number.isNaN(currentPlantValue) &&
    currentPlantValue >= 1;
  const hasValidHarvestDate =
    !plannedHarvestDate || !startDate || plannedHarvestDate >= startDate;
  const hasValidEndDate = !endDate || !startDate || endDate >= startDate;
  const expectedYieldValue = parseWeightInput(expectedYieldKg);
  const actualYieldValue = parseWeightInput(actualYieldKg);

  const isFormValid =
    seasonName.trim() &&
    startDate &&
    hasValidPlantCount &&
    hasValidHarvestDate &&
    hasValidEndDate;

  const handleSubmit = () => {
    if (!season || !isFormValid) return;
    const id = parseInt(season.id, 10);
    if (Number.isNaN(id) || currentPlantValue === null) return;

    const payload: SeasonUpdateRequest = {
      seasonName: seasonName.trim(),
      startDate,
      plannedHarvestDate: plannedHarvestDate || undefined,
      endDate: endDate || undefined,
      currentPlantCount: currentPlantValue,
      expectedYieldKg: expectedYieldValue,
      actualYieldKg: actualYieldValue,
      budgetAmount: parseBudgetInput(budgetAmount),
      notes: notes.trim() || undefined,
      varietyId: varietyId ? parseInt(varietyId, 10) : undefined,
    };

    onSubmit(id, payload);
  };
  const currentFormState = {
    seasonName,
    startDate,
    plannedHarvestDate,
    endDate,
    currentPlantCount,
    expectedYieldKg,
    actualYieldKg,
    budgetAmount,
    notes,
    varietyId,
  };
  const isDirty = open && !!initialFormRef.current && initialFormRef.current !== JSON.stringify(currentFormState);
  const handleClose = () => {
    if (isSubmitting) return;
    if (
      isDirty &&
      !window.confirm(t("common.unsavedChangesConfirm", "You have unsaved changes. Leave this page?"))
    ) {
      return;
    }
    onOpenChange(false);
  };
  const handleOpenChange = (nextOpen: boolean) => {
    if (isSubmitting && !nextOpen) return;
    if (!nextOpen) {
      handleClose();
      return;
    }
    onOpenChange(true);
  };

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogContent className="sm:max-w-[600px]" closeDisabled={isSubmitting}>
        <DialogHeader>
          <BackButton onClick={handleClose} className="w-fit" />
          <DialogTitle className="flex items-center gap-2 text-foreground">
            <Edit className="w-5 h-5 text-primary" />
            {t("seasons.dialog.editTitle")}
          </DialogTitle>
          <DialogDescription>
            {t("seasons.dialog.editDescription")}
          </DialogDescription>
        </DialogHeader>

        <div className="grid gap-4 py-2">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label>{t("seasons.table.farm")}</Label>
              <Input value={farmLabel} disabled />
            </div>
            <div className="space-y-2">
              <Label>{t("seasons.table.plot")}</Label>
              <Input value={plotLabel} disabled />
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label>{t("seasons.table.crop")}</Label>
              <Input value={cropLabel} disabled />
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

          <div className="space-y-2">
            <Label htmlFor="seasonName" required>{t("seasons.form.seasonName")}</Label>
            <Input
              id="seasonName"
              value={seasonName}
              onChange={(e) => setSeasonName(e.target.value)}
              required
              disabled={isSubmitting}
            />
          </div>

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

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <div className="space-y-2">
              <Label htmlFor="currentPlantCount" required>{t("seasons.form.currentPlantCount")}</Label>
              <Input
                id="currentPlantCount"
                type="number"
                min="1"
                value={currentPlantCount}
                onChange={(e) => setCurrentPlantCount(e.target.value)}
                required
                aria-invalid={!hasValidPlantCount}
                aria-describedby={!hasValidPlantCount ? "edit-season-plant-count-error" : undefined}
                disabled={isSubmitting}
              />
              {!hasValidPlantCount && (
                <p id="edit-season-plant-count-error" className="text-sm text-destructive">
                  {t("seasons.dialog.currentPlantCountInvalid")}
                </p>
              )}
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
                disabled={isSubmitting}
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="actualYieldKg">{t("seasons.dialog.actualYieldLabel", { unit: unitLabel })}</Label>
              <Input
                id="actualYieldKg"
                type="number"
                min="0"
                step={weightStep}
                value={actualYieldKg}
                onChange={(e) => setActualYieldKg(e.target.value)}
                disabled={isSubmitting}
              />
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
              disabled={isSubmitting}
            />
          </div>

          <div className="space-y-2">
            <Label htmlFor="notes">{t("common.notes")}</Label>
            <Textarea
              id="notes"
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
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
            {isSubmitting ? t("common.saving") : t("common.saveChanges")}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
