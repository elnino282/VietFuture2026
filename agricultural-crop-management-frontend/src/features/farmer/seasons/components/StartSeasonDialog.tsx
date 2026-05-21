import { Button } from "@/shared/ui/button";
import { Checkbox } from "@/shared/ui/checkbox";
import { Input } from "@/shared/ui/input";
import { Label } from "@/shared/ui/label";
import { ScrollArea } from "@/shared/ui/scroll-area";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/shared/ui";
import { useEmployeeDirectory } from "@/entities/labor";
import { Play } from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import type { Season } from "../types";

interface StartSeasonDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  season: Season | null;
  onConfirm: (data: {
    actualStartDate?: string;
    currentPlantCount?: number;
    employeeUserIds?: number[];
  }) => void;
  isSubmitting?: boolean;
}

export function StartSeasonDialog({
  open,
  onOpenChange,
  season,
  onConfirm,
  isSubmitting = false,
}: StartSeasonDialogProps) {
  const [actualStartDate, setActualStartDate] = useState("");
  const [currentPlantCount, setCurrentPlantCount] = useState("");
  const [syncPlantCount, setSyncPlantCount] = useState(false);
  const [selectedEmployeeIds, setSelectedEmployeeIds] = useState<number[]>([]);

  const { data: employeeDirectoryData, isLoading: isEmployeeDirectoryLoading } =
    useEmployeeDirectory(
      { page: 0, size: 200 },
      { enabled: open }
    );
  const employeeOptions = useMemo(
    () => employeeDirectoryData?.items ?? [],
    [employeeDirectoryData]
  );

  useEffect(() => {
    if (!open) return;
    setActualStartDate("");
    setCurrentPlantCount("");
    setSyncPlantCount(false);
    setSelectedEmployeeIds([]);
  }, [open, season?.id]);

  const currentPlantValue =
    currentPlantCount === "" ? null : parseInt(currentPlantCount, 10);
  const hasValidPlantCount =
    !syncPlantCount ||
    (currentPlantValue !== null &&
      !Number.isNaN(currentPlantValue) &&
      currentPlantValue >= 1);

  const canSubmit = hasValidPlantCount && !isSubmitting;

  const handleSubmit = () => {
    if (!season || !canSubmit) return;
    onConfirm({
      actualStartDate: actualStartDate || undefined,
      currentPlantCount: syncPlantCount
        ? (currentPlantValue ?? undefined)
        : undefined,
      employeeUserIds: selectedEmployeeIds.length > 0 ? selectedEmployeeIds : undefined,
    });
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="acm-rounded-lg">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <Play className="w-5 h-5 text-primary" />
            Start Season
          </DialogTitle>
          <DialogDescription>
            Activate this planned season. Leave fields empty to keep existing
            values.
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-4 py-2">
          <div className="space-y-2">
            <Label htmlFor="actualStartDate">
              Actual Start Date (Optional)
            </Label>
            <Input
              id="actualStartDate"
              type="date"
              value={actualStartDate}
              onChange={(e) => setActualStartDate(e.target.value)}
              className="border-border acm-rounded-sm"
            />
            <p className="text-xs text-muted-foreground">
              Leave blank to keep the planned start date.
            </p>
          </div>

          <div className="flex items-start gap-3">
            <Checkbox
              checked={syncPlantCount}
              onCheckedChange={(checked) => setSyncPlantCount(Boolean(checked))}
              className="mt-1"
            />
            <div className="space-y-2 flex-1">
              <Label htmlFor="currentPlantCount">
                Current Plant Count (Optional)
              </Label>
              <Input
                id="currentPlantCount"
                type="number"
                min="1"
                value={currentPlantCount}
                onChange={(e) => setCurrentPlantCount(e.target.value)}
                placeholder="e.g., 1200"
                disabled={!syncPlantCount}
                className="border-border acm-rounded-sm"
              />
              <p className="text-xs text-muted-foreground">
                Enable to update the current plant count when the season starts.
              </p>
            </div>
          </div>

          <div className="space-y-2">
            <div className="flex items-center justify-between gap-2">
              <Label>Bulk Assign Employees (Optional)</Label>
              <div className="flex items-center gap-2">
                <Button
                  type="button"
                  size="sm"
                  variant="outline"
                  onClick={() =>
                    setSelectedEmployeeIds(employeeOptions.map((employee) => employee.userId))
                  }
                  disabled={isSubmitting || employeeOptions.length === 0}
                >
                  Select all
                </Button>
                <Button
                  type="button"
                  size="sm"
                  variant="ghost"
                  onClick={() => setSelectedEmployeeIds([])}
                  disabled={isSubmitting || selectedEmployeeIds.length === 0}
                >
                  Clear
                </Button>
              </div>
            </div>
            {isEmployeeDirectoryLoading ? (
              <p className="text-xs text-muted-foreground">Loading employee catalog...</p>
            ) : employeeOptions.length === 0 ? (
              <p className="text-xs text-muted-foreground">No employees in catalog.</p>
            ) : (
              <ScrollArea className="h-36 rounded-md border border-border p-2">
                <div className="space-y-2">
                  {employeeOptions.map((employee) => {
                    const checked = selectedEmployeeIds.includes(employee.userId);
                    const label =
                      employee.fullName || employee.username || employee.email || `Employee #${employee.userId}`;
                    return (
                      <label
                        key={employee.userId}
                        className="flex items-center gap-2 rounded-md px-2 py-1 hover:bg-muted/40"
                      >
                        <Checkbox
                          checked={checked}
                          onCheckedChange={(nextChecked) =>
                            setSelectedEmployeeIds((prev) => {
                              if (nextChecked) {
                                return prev.includes(employee.userId)
                                  ? prev
                                  : [...prev, employee.userId];
                              }
                              return prev.filter((id) => id !== employee.userId);
                            })
                          }
                          disabled={isSubmitting}
                        />
                        <span className="text-sm">{label}</span>
                      </label>
                    );
                  })}
                </div>
              </ScrollArea>
            )}
          </div>
        </div>

        <DialogFooter>
          <Button
            variant="outline"
            onClick={() => onOpenChange(false)}
            disabled={isSubmitting}
            className="acm-rounded-sm"
          >
            Cancel
          </Button>
          <Button
            onClick={handleSubmit}
            disabled={!canSubmit}
            className="bg-primary hover:bg-primary/90 text-white acm-rounded-sm"
          >
            {isSubmitting ? "Starting..." : "Start Season"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
