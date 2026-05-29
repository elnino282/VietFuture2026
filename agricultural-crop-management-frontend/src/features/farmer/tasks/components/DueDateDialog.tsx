import { useEffect, useState } from "react";
import { Button } from "@/shared/ui/button";
import { Input } from "@/shared/ui/input";
import { Label } from "@/shared/ui/label";
import { useTranslation } from "react-i18next";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/shared/ui";

interface DueDateDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  selectedCount: number;
  onChangeDueDate: (dueDate: string) => Promise<void> | void;
  disabled?: boolean;
  disabledReason?: string;
}

export function DueDateDialog({
  open,
  onOpenChange,
  selectedCount,
  onChangeDueDate,
  disabled = false,
  disabledReason,
}: DueDateDialogProps) {
  const { t } = useTranslation();
  const [dueDate, setDueDate] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    if (!open) {
      setDueDate("");
    }
  }, [open]);

  const handleOpenChange = (nextOpen: boolean) => {
    if (isSubmitting && !nextOpen) return;
    onOpenChange(nextOpen);
  };

  const handleSubmit = async () => {
    if (!dueDate || disabled) return;
    setIsSubmitting(true);
    try {
      await onChangeDueDate(dueDate);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogContent className="sm:max-w-[500px]" closeDisabled={isSubmitting}>
        <DialogHeader>
          <DialogTitle>{t("tasks.dialog.bulkDueDateTitle")}</DialogTitle>
          <DialogDescription>
            {t("tasks.dialog.bulkDueDateDescription", { count: selectedCount })}
          </DialogDescription>
        </DialogHeader>
        <div className="space-y-4">
          {disabled && (
            <div className="rounded-md border border-amber-300 bg-amber-50 px-3 py-2 text-sm text-amber-900">
              {disabledReason || t("tasks.dialog.bulkDueDateLocked")}
            </div>
          )}
          <div className="space-y-2">
            <Label htmlFor="bulk-due-date-input" required>
              {t("tasks.form.dueDate")}
            </Label>
            <Input
              id="bulk-due-date-input"
              data-testid="bulk-due-date-input"
              type="date"
              value={dueDate}
              onChange={(event) => setDueDate(event.target.value)}
              disabled={disabled || isSubmitting}
            />
          </div>
        </div>
        <DialogFooter>
          <Button
            variant="outline"
            onClick={() => handleOpenChange(false)}
            disabled={isSubmitting}
          >
            {t("common.cancel")}
          </Button>
          <Button
            onClick={handleSubmit}
            disabled={disabled || !dueDate || isSubmitting}
            title={disabled ? disabledReason : undefined}
            data-testid="submit-bulk-due-date"
          >
            {isSubmitting ? t("common.saving") : t("tasks.dialog.bulkDueDateSubmit")}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
