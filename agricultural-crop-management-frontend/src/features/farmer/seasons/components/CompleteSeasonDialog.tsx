import { Button } from "@/shared/ui/button";
import { Checkbox } from "@/shared/ui/checkbox";
import { Input } from "@/shared/ui/input";
import { Label } from "@/shared/ui/label";
import { usePreferences } from "@/shared/contexts";
import { convertWeightToKg, getWeightUnitLabel } from "@/shared/lib";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/shared/ui";
import { CheckCircle2 } from "lucide-react";
import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import type { Season } from "../types";

interface CompleteSeasonDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  season: Season | null;
  onConfirm: (data: {
    endDate: string;
    actualYieldKg?: number;
    forceComplete?: boolean;
  }) => void;
  isSubmitting?: boolean;
}

export function CompleteSeasonDialog({
  open,
  onOpenChange,
  season,
  onConfirm,
  isSubmitting = false,
}: CompleteSeasonDialogProps) {
  const { t } = useTranslation();
  const { preferences } = usePreferences();
  const unitLabel = getWeightUnitLabel(preferences.weightUnit);
  const weightStep = preferences.weightUnit === "G" ? "1" : "0.01";
  const [endDate, setEndDate] = useState("");
  const [actualYieldKg, setActualYieldKg] = useState("");
  const [forceComplete, setForceComplete] = useState(false);

  useEffect(() => {
    if (!open) return;
    setEndDate("");
    setActualYieldKg("");
    setForceComplete(false);
  }, [open, season?.id]);

  const actualYieldInput =
    actualYieldKg === "" ? null : parseFloat(actualYieldKg);
  const hasValidYield =
    actualYieldInput === null ||
    (!Number.isNaN(actualYieldInput) && actualYieldInput >= 0);
  const actualYieldValue =
    actualYieldInput === null
      ? null
      : convertWeightToKg(actualYieldInput, preferences.weightUnit);

  const canSubmit = Boolean(endDate) && hasValidYield && !isSubmitting;
  const yieldError = hasValidYield ? undefined : t("seasons.dialog.actualYieldInvalid");

  const handleSubmit = () => {
    if (!season || !canSubmit) return;
    onConfirm({
      endDate,
      actualYieldKg: actualYieldValue ?? undefined,
      forceComplete,
    });
  };
  const handleOpenChange = (nextOpen: boolean) => {
    if (isSubmitting && !nextOpen) return;
    onOpenChange(nextOpen);
  };

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogContent className="sm:max-w-[500px]" closeDisabled={isSubmitting}>
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <CheckCircle2 className="w-5 h-5 text-primary" />
            {t("seasons.dialog.completeTitle")}
          </DialogTitle>
          <DialogDescription>
            {t("seasons.dialog.completeDescription")}
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-4 py-2">
          <div className="space-y-2">
            <Label htmlFor="endDate" required>{t("seasons.dialog.endDateLabel")}</Label>
            <Input
              id="endDate"
              type="date"
              value={endDate}
              onChange={(e) => setEndDate(e.target.value)}
              min={season?.startDate}
              disabled={isSubmitting}
            />
          </div>

          <div className="space-y-2">
            <Label htmlFor="actualYieldKg">
              {t("seasons.dialog.actualYieldLabel", { unit: unitLabel })}
            </Label>
            <Input
              id="actualYieldKg"
              type="number"
              min="0"
              step={weightStep}
              value={actualYieldKg}
              onChange={(e) => setActualYieldKg(e.target.value)}
              placeholder={t("seasons.dialog.actualYieldPlaceholder")}
              aria-invalid={!!yieldError}
              aria-describedby={yieldError ? "complete-season-yield-error" : "complete-season-yield-help"}
              disabled={isSubmitting}
            />
            {yieldError ? (
              <p id="complete-season-yield-error" className="text-sm text-destructive">
                {yieldError}
              </p>
            ) : (
              <p id="complete-season-yield-help" className="text-xs text-muted-foreground">
                {t("seasons.dialog.actualYieldHelp")}
              </p>
            )}
          </div>

          <div className="flex items-start gap-3">
            <Checkbox
              checked={forceComplete}
              onCheckedChange={(checked) => setForceComplete(Boolean(checked))}
              disabled={isSubmitting}
              className="mt-1"
            />
            <div className="text-sm text-muted-foreground">
              {t("seasons.dialog.forceCompleteHelp")}
            </div>
          </div>
        </div>

        <DialogFooter>
          <Button
            variant="outline"
            onClick={() => onOpenChange(false)}
            disabled={isSubmitting}
          >
            {t("common.cancel")}
          </Button>
          <Button
            onClick={handleSubmit}
            disabled={!canSubmit}
          >
            {isSubmitting ? t("seasons.dialog.completing") : t("seasons.dialog.completeConfirm")}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
