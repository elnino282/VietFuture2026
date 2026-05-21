import { Button } from "@/shared/ui/button";
import { Label } from "@/shared/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/shared/ui/select";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/shared/ui";
import { useEffect, useState } from "react";

interface AssigneeOption {
  userId: number;
  displayName: string;
}

interface ReassignDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  selectedCount: number;
  onReassign: (assigneeUserId: number) => void;
  assigneeOptions: AssigneeOption[];
  disabled?: boolean;
  disabledReason?: string;
}

export function ReassignDialog({
  open,
  onOpenChange,
  selectedCount,
  onReassign,
  assigneeOptions,
  disabled = false,
  disabledReason,
}: ReassignDialogProps) {
  const [selectedAssigneeId, setSelectedAssigneeId] = useState("");

  useEffect(() => {
    if (!open) {
      setSelectedAssigneeId("");
    }
  }, [open]);

  const handleReassign = () => {
    const assigneeUserId = Number(selectedAssigneeId);
    if (!Number.isFinite(assigneeUserId) || assigneeUserId <= 0) {
      return;
    }
    onReassign(assigneeUserId);
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="acm-rounded-lg">
        <DialogHeader>
          <DialogTitle>Reassign Tasks</DialogTitle>
          <DialogDescription>
            Assign {selectedCount} selected tasks to a new team member
          </DialogDescription>
        </DialogHeader>
        <div className="py-4 space-y-2">
          {disabled && (
            <div className="rounded-md border border-amber-300 bg-amber-50 px-3 py-2 text-sm text-amber-900">
              {disabledReason || "This season is locked. Reassignment is disabled."}
            </div>
          )}
          <Label>New Assignee</Label>
          <Select
            disabled={disabled || assigneeOptions.length === 0}
            value={selectedAssigneeId}
            onValueChange={setSelectedAssigneeId}
          >
            <SelectTrigger className="border-border acm-rounded-sm">
              <SelectValue placeholder="Select assignee" />
            </SelectTrigger>
            <SelectContent>
              {assigneeOptions.map((assignee) => (
                <SelectItem key={assignee.userId} value={String(assignee.userId)}>
                  {assignee.displayName}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
          {assigneeOptions.length === 0 && (
            <p className="text-xs text-muted-foreground">
              No assignees available for this season.
            </p>
          )}
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
            onClick={handleReassign}
            className="bg-primary hover:bg-primary/90 text-primary-foreground acm-rounded-sm"
            disabled={disabled || !selectedAssigneeId}
            title={disabled ? disabledReason : undefined}
          >
            Reassign
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
