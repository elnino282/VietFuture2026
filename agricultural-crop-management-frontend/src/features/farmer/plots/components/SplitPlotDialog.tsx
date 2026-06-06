import { useEffect, useMemo, useState } from "react";
import { Check, Scissors } from "lucide-react";
import { useTranslation } from "react-i18next";
import { Button } from "@/shared/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/shared/ui/dialog";
import { Input } from "@/shared/ui/input";
import { Label } from "@/shared/ui/label";
import { Separator } from "@/shared/ui/separator";
import type { Plot, SplitPlotRequest } from "../types";

interface SplitPlotDialogProps {
  isOpen: boolean;
  plot: Plot | null;
  onClose: () => void;
  onConfirm: (data: SplitPlotRequest) => void;
  isSplitting?: boolean;
}

type FormErrors = Partial<Record<"firstPlotName" | "secondPlotName" | "firstArea", string>>;

export function SplitPlotDialog({
  isOpen,
  plot,
  onClose,
  onConfirm,
  isSplitting = false,
}: SplitPlotDialogProps) {
  const { t } = useTranslation();
  const [firstPlotName, setFirstPlotName] = useState("");
  const [secondPlotName, setSecondPlotName] = useState("");
  const [firstArea, setFirstArea] = useState("");
  const [errors, setErrors] = useState<FormErrors>({});

  const sourceArea = plot?.area ?? 0;
  const firstAreaNumber = Number.parseFloat(firstArea);
  const secondArea = useMemo(() => {
    if (!Number.isFinite(firstAreaNumber)) return 0;
    return Math.max(0, sourceArea - firstAreaNumber);
  }, [firstAreaNumber, sourceArea]);

  useEffect(() => {
    if (!isOpen || !plot) {
      setFirstPlotName("");
      setSecondPlotName("");
      setFirstArea("");
      setErrors({});
      return;
    }

    setFirstPlotName(`${plot.name} A`);
    setSecondPlotName(`${plot.name} B`);
    setFirstArea("");
    setErrors({});
  }, [isOpen, plot]);

  const validate = () => {
    const nextErrors: FormErrors = {};

    if (!firstPlotName.trim()) {
      nextErrors.firstPlotName = t("farms.validation.plotNameRequired");
    }

    if (!secondPlotName.trim()) {
      nextErrors.secondPlotName = t("farms.validation.plotNameRequired");
    }

    if (!Number.isFinite(firstAreaNumber) || firstAreaNumber <= 0) {
      nextErrors.firstArea = t("farms.validation.areaPositive");
    } else if (firstAreaNumber >= sourceArea) {
      nextErrors.firstArea = t("plots.split.areaLessThanSource");
    }

    setErrors(nextErrors);
    return Object.keys(nextErrors).length === 0;
  };

  const handleConfirm = () => {
    if (!plot || !validate()) return;

    onConfirm({
      sourcePlot: plot,
      firstPlotName: firstPlotName.trim(),
      firstArea: firstAreaNumber,
      secondPlotName: secondPlotName.trim(),
      secondArea,
    });
  };

  return (
    <Dialog
      open={isOpen}
      onOpenChange={(open) => {
        if (!open && !isSplitting) onClose();
      }}
    >
      <DialogContent className="sm:max-w-2xl">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <Scissors className="h-5 w-5 text-primary" />
            {t("plots.split.title")}
          </DialogTitle>
          <DialogDescription>
            {plot
              ? t("plots.split.description", { name: plot.name })
              : t("plots.split.descriptionFallback")}
          </DialogDescription>
        </DialogHeader>

        {plot && (
          <div className="space-y-5">
            <div className="rounded-lg border border-border bg-muted/20 p-4">
              <Label className="text-sm text-muted-foreground">
                {t("plots.split.sourcePlot")}
              </Label>
              <p className="mt-1 text-base text-foreground">
                {plot.name} - {sourceArea.toFixed(2)} ha
              </p>
            </div>

            <div className="grid gap-4 sm:grid-cols-2">
              <div className="space-y-2">
                <Label htmlFor="first-plot-name">
                  {t("plots.split.firstPlotName")} <span className="text-destructive">*</span>
                </Label>
                <Input
                  id="first-plot-name"
                  value={firstPlotName}
                  onChange={(event) => setFirstPlotName(event.target.value)}
                  disabled={isSplitting}
                />
                {errors.firstPlotName && (
                  <p className="text-sm text-destructive">{errors.firstPlotName}</p>
                )}
              </div>

              <div className="space-y-2">
                <Label htmlFor="second-plot-name">
                  {t("plots.split.secondPlotName")} <span className="text-destructive">*</span>
                </Label>
                <Input
                  id="second-plot-name"
                  value={secondPlotName}
                  onChange={(event) => setSecondPlotName(event.target.value)}
                  disabled={isSplitting}
                />
                {errors.secondPlotName && (
                  <p className="text-sm text-destructive">{errors.secondPlotName}</p>
                )}
              </div>

              <div className="space-y-2">
                <Label htmlFor="first-area">
                  {t("plots.split.firstArea")} <span className="text-destructive">*</span>
                </Label>
                <Input
                  id="first-area"
                  type="number"
                  min="0"
                  step="0.01"
                  value={firstArea}
                  onChange={(event) => setFirstArea(event.target.value)}
                  placeholder={t("plots.split.firstAreaPlaceholder")}
                  disabled={isSplitting}
                />
                {errors.firstArea && (
                  <p className="text-sm text-destructive">{errors.firstArea}</p>
                )}
              </div>

              <div className="space-y-2">
                <Label htmlFor="second-area">{t("plots.split.secondArea")}</Label>
                <Input
                  id="second-area"
                  value={secondArea > 0 ? secondArea.toFixed(2) : ""}
                  readOnly
                  disabled
                />
              </div>
            </div>

            <Separator />

            <div className="flex justify-end gap-3">
              <Button variant="outline" onClick={onClose} disabled={isSplitting}>
                {t("common.cancel")}
              </Button>
              <Button onClick={handleConfirm} disabled={isSplitting}>
                <Check className="mr-2 h-4 w-4" />
                {isSplitting ? t("common.processing") : t("plots.split.confirm")}
              </Button>
            </div>
          </div>
        )}
      </DialogContent>
    </Dialog>
  );
}
