import { useEffect, useState } from "react";
import { Button } from "@/shared/ui/button";
import { Input } from "@/shared/ui/input";
import { Label } from "@/shared/ui/label";
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
  const [dueDate, setDueDate] = useState("");

  useEffect(() => {
    if (!open) {
      setDueDate("");
    }
  }, [open]);

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="acm-rounded-lg">
        <DialogHeader>
          <DialogTitle>Change Due Date</DialogTitle>
          <DialogDescription>
            Update due date for {selectedCount} selected tasks
          </DialogDescription>
        </DialogHeader>
        <div className="py-4 space-y-2">
          {disabled && (
            <div className="rounded-md border border-amber-300 bg-amber-50 px-3 py-2 text-sm text-amber-900">
              {disabledReason || "This season is locked. Due date updates are disabled."}
            </div>
          )}
          <Label htmlFor="bulk-due-date-input">New Due Date</Label>
          <Input
            id="bulk-due-date-input"
            data-testid="bulk-due-date-input"
            type="date"
            value={dueDate}
            onChange={(event) => setDueDate(event.target.value)}
            disabled={disabled}
            className="border-border acm-rounded-sm"
          />
        </div>
        <DialogFooter>
          <Button
            variant="outline"
            onClick={() => onOpenChange(false)}
            className="acm-rounded-sm"
          >
            Cancel
          </Button>
          <Button
            onClick={() => onChangeDueDate(dueDate)}
            className="bg-primary hover:bg-primary/90 text-primary-foreground acm-rounded-sm"
            disabled={disabled || !dueDate}
            title={disabled ? disabledReason : undefined}
            data-testid="submit-bulk-due-date"
          >
            Update Due Date
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
