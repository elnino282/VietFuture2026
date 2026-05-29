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
import { useTranslation } from "react-i18next";
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
  const { t } = useTranslation();
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
      <DialogContent className="sm:max-w-[500px]">
        <DialogHeader>
          <DialogTitle>{t("tasks.dialog.reassignTitle")}</DialogTitle>
          <DialogDescription>
            {t("tasks.dialog.reassignDescription", { count: selectedCount })}
          </DialogDescription>
        </DialogHeader>
        <div className="space-y-4">
          {disabled && (
            <div className="rounded-md border border-amber-300 bg-amber-50 px-3 py-2 text-sm text-amber-900">
              {disabledReason || t("tasks.dialog.reassignLocked")}
            </div>
          )}
          <div className="space-y-2">
            <Label htmlFor="bulk-reassign-assignee" required>
              {t("tasks.form.assignee")}
            </Label>
            <Select
              disabled={disabled || assigneeOptions.length === 0}
              value={selectedAssigneeId}
              onValueChange={setSelectedAssigneeId}
            >
              <SelectTrigger id="bulk-reassign-assignee">
                <SelectValue placeholder={t("tasks.form.selectAssignee")} />
              </SelectTrigger>
              <SelectContent>
                {assigneeOptions.map((assignee) => (
                  <SelectItem key={assignee.userId} value={String(assignee.userId)}>
                    {assignee.displayName}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
          {assigneeOptions.length === 0 && (
            <p className="text-xs text-muted-foreground">
              {t("tasks.dialog.noAssignees")}
            </p>
          )}
        </div>
        <DialogFooter>
          <Button
            variant="outline"
            onClick={() => onOpenChange(false)}
          >
            {t("common.cancel")}
          </Button>
          <Button
            onClick={handleReassign}
            disabled={disabled || !selectedAssigneeId}
            title={disabled ? disabledReason : undefined}
          >
            {t("tasks.dialog.reassignSubmit")}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
