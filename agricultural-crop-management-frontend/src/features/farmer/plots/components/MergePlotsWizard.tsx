import { useEffect, useState } from "react";
import { GitMerge, ChevronRight, Check } from "lucide-react";
import { Button } from "@/shared/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/shared/ui/card";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/shared/ui/dialog";
import { Checkbox } from "@/shared/ui/checkbox";
import { Label } from "@/shared/ui/label";
import { Input } from "@/shared/ui/input";
import { ScrollArea } from "@/shared/ui/scroll-area";
import { Separator } from "@/shared/ui/separator";
import { toast } from "sonner";
import { useTranslation } from "react-i18next";
import { PlotStatusChip } from "./PlotStatusChip";
import { Plot, PlotStatus } from "../types";
import { getSoilTypeLabel } from "@/features/farmer/shared/plotOptions";

interface MergePlotsWizardProps {
  isOpen: boolean;
  onClose: () => void;
  plots: Plot[];
  selectedPlots: string[];
  setSelectedPlots: (plots: string[]) => void;
  mergeStep: number;
  setMergeStep: (step: number) => void;
  onConfirm: (newPlotName: string) => void;
  isMerging?: boolean;
}

/**
 * MergePlotsWizard Component
 *
 * Multi-step wizard for merging multiple plots into one.
 */
export function MergePlotsWizard({
  isOpen,
  onClose,
  plots,
  selectedPlots,
  setSelectedPlots,
  mergeStep,
  setMergeStep,
  onConfirm,
  isMerging = false,
}: MergePlotsWizardProps) {
  const { t } = useTranslation();
  const [mergedPlotName, setMergedPlotName] = useState("");
  const [nameError, setNameError] = useState("");

  useEffect(() => {
    if (!isOpen) {
      setMergedPlotName("");
      setNameError("");
    }
  }, [isOpen]);

  const handleClose = () => {
    if (isMerging) return;
    onClose();
    setMergeStep(1);
    setSelectedPlots([]);
    setMergedPlotName("");
    setNameError("");
  };

  const handleNext = () => {
    if (selectedPlots.length < 2) {
      toast.error(t("plots.toast.selectAtLeastTwo"));
      return;
    }
    setMergeStep(2);
  };

  const handleConfirm = () => {
    const trimmedName = mergedPlotName.trim();
    if (!trimmedName) {
      setNameError(t("farms.validation.plotNameRequired"));
      return;
    }
    onConfirm(trimmedName);
  };

  return (
    <Dialog open={isOpen} onOpenChange={(nextOpen) => {
      if (!nextOpen) handleClose();
    }}>
      <DialogContent className="sm:max-w-3xl">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2 text-foreground">
            <GitMerge className="w-5 h-5 text-primary" />
            {t("plots.merge.title")}
          </DialogTitle>
          <DialogDescription>
            {t("plots.merge.stepDescription", {
              step: mergeStep,
              total: 2,
              label: mergeStep === 1
                ? t("plots.merge.selectStep")
                : t("plots.merge.previewStep"),
            })}
          </DialogDescription>
        </DialogHeader>

        {mergeStep === 1 ? (
          <div className="py-4">
            <p className="text-sm text-muted-foreground mb-4">
              {t("plots.merge.selectInstruction")}
            </p>
            <ScrollArea className="h-[300px] border border-border rounded-lg p-4">
              <div className="space-y-2">
                {plots.map((plot) => (
                  <div
                    key={plot.id}
                    className="flex items-center space-x-3 p-3 rounded-lg hover:bg-muted cursor-pointer"
                    onClick={() => {
                      setSelectedPlots(
                        selectedPlots.includes(plot.id)
                          ? selectedPlots.filter((id) => id !== plot.id)
                          : [...selectedPlots, plot.id]
                      );
                    }}
                  >
                    <Checkbox
                      checked={selectedPlots.includes(plot.id)}
                      onCheckedChange={(checked) => {
                        if (checked) {
                          setSelectedPlots([...selectedPlots, plot.id]);
                        } else {
                          setSelectedPlots(
                            selectedPlots.filter((id) => id !== plot.id)
                          );
                        }
                      }}
                    />
                    <div className="flex-1">
                      <p className="text-sm text-foreground">{plot.name}</p>
                      <p className="text-xs text-muted-foreground">
                        {plot.area.toFixed(1)} ha - {getSoilTypeLabel(plot.soilType, t)}
                      </p>
                    </div>
                    <PlotStatusChip status={plot.status} />
                  </div>
                ))}
              </div>
            </ScrollArea>
            <p className="text-xs text-muted-foreground mt-3">
              {t("plots.selectedCount", { count: selectedPlots.length })}
            </p>
          </div>
        ) : (
          <div className="py-4">
            <Card className="border-border">
              <CardHeader>
                <CardTitle className="text-base">{t("plots.merge.previewTitle")}</CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                <div>
                  <Label className="text-sm text-muted-foreground">{t("plots.merge.plotsToMerge")}</Label>
                  <div className="mt-2 space-y-1">
                    {plots
                      .filter((p) => selectedPlots.includes(p.id))
                      .map((plot) => (
                        <p key={plot.id} className="text-sm text-foreground">
                          - {plot.name} ({plot.area.toFixed(1)} ha)
                        </p>
                      ))}
                  </div>
                </div>
                <Separator className="bg-border" />
                <div>
                  <Label className="text-sm text-muted-foreground">{t("plots.merge.newPlotName")}</Label>
                  <Input
                    value={mergedPlotName}
                    onChange={(event) => {
                      setMergedPlotName(event.target.value);
                      setNameError("");
                    }}
                    placeholder={t("plots.merge.newPlotNamePlaceholder")}
                    className="mt-2 border-border focus:border-primary"
                    disabled={isMerging}
                    aria-invalid={!!nameError}
                  />
                  {nameError && (
                    <p className="mt-1 text-sm text-destructive">{nameError}</p>
                  )}
                </div>
                <div>
                  <Label className="text-sm text-muted-foreground">{t("plots.merge.totalArea")}</Label>
                  <p className="text-xl text-foreground mt-1">
                    {plots
                      .filter((p) => selectedPlots.includes(p.id))
                      .reduce((sum, p) => sum + p.area, 0)
                      .toFixed(1)}{" "}
                    {t("plots.unit.hectares")}
                  </p>
                </div>
              </CardContent>
            </Card>
          </div>
        )}

        <DialogFooter>
          <Button type="button" variant="outline" onClick={handleClose} disabled={isMerging}>
            {t("common.cancel")}
          </Button>
          {mergeStep === 1 ? (
            <Button
              onClick={handleNext}
              className="bg-primary hover:bg-primary/90 text-white"
            >
              {t("common.next")}
              <ChevronRight className="w-4 h-4 ml-2" />
            </Button>
          ) : (
            <>
              <Button variant="outline" onClick={() => setMergeStep(1)} disabled={isMerging}>
                {t("common.back")}
              </Button>
              <Button
                onClick={handleConfirm}
                disabled={isMerging}
                className="bg-primary hover:bg-primary/90 text-white"
              >
                {isMerging ? (
                  <>
                    <span className="mr-2 h-4 w-4 animate-spin rounded-full border-2 border-white border-t-transparent" />
                    {t("common.loading")}
                  </>
                ) : (
                  <>
                    <Check className="w-4 h-4 mr-2" />
                    {t("plots.merge.confirm")}
                  </>
                )}
              </Button>
            </>
          )}
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}




