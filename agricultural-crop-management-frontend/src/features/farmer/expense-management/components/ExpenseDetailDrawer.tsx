import { Button } from "@/shared/ui/button";
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
  DialogHeader,
  DialogTitle,
} from "@/shared/ui/dialog";
import { Textarea } from "@/shared/ui/textarea";
import { usePreferences } from "@/shared/contexts";
import { convertToDisplayCurrency, formatMoney } from "@/shared/lib";
import { ExternalLink, FileText, Link as LinkIcon } from "lucide-react";
import { useEffect, useState } from "react";
import type { Expense, ExpenseStatus } from "../types";

interface ExpenseDetailDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  expense: Expense | null;
  onQuickUpdate: (
    expense: Expense,
    updates: { status?: ExpenseStatus; notes?: string },
  ) => void;
}

export function ExpenseDetailDialog({
  open,
  onOpenChange,
  expense,
  onQuickUpdate,
}: ExpenseDetailDialogProps) {
  const { preferences } = usePreferences();
  const [status, setStatus] = useState<ExpenseStatus>("PENDING");
  const [notes, setNotes] = useState("");

  useEffect(() => {
    if (!expense) return;
    setStatus(expense.status);
    setNotes(expense.notes ?? "");
  }, [expense]);

  if (!expense) {
    return null;
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="w-full sm:max-w-lg sm:max-h-[85vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <FileText className="w-4 h-4 text-primary" />
            Expense Details
          </DialogTitle>
        </DialogHeader>

        <div className="mt-6 space-y-4 text-sm">
          <div className="grid grid-cols-2 gap-4">
            <div>
              <p className="text-xs text-muted-foreground">Date</p>
              <p className="text-foreground">{expense.date}</p>
            </div>
            <div>
              <p className="text-xs text-muted-foreground">Category</p>
              <p className="text-foreground">{expense.category}</p>
            </div>
            <div>
              <p className="text-xs text-muted-foreground">Amount</p>
              <p className="text-foreground">
                {formatMoney(
                  convertToDisplayCurrency(
                    expense.amount,
                    preferences.currency,
                  ),
                  preferences.currency,
                  preferences.locale,
                )}
              </p>
            </div>
            <div>
              <p className="text-xs text-muted-foreground">Status</p>
              <Select
                value={status}
                onValueChange={(value: ExpenseStatus) => setStatus(value)}
              >
                <SelectTrigger className="h-8">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="PAID">Paid</SelectItem>
                  <SelectItem value="PENDING">Pending</SelectItem>
                  <SelectItem value="UNPAID">Unpaid</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </div>

          <div>
            <p className="text-xs text-muted-foreground">Description</p>
            <p className="text-foreground">{expense.description}</p>
          </div>

          <div className="space-y-2">
            <p className="text-xs text-muted-foreground">Linked To</p>
            <div className="space-y-1">
              {expense.linkedSeason && (
                <div className="flex items-center gap-2 text-foreground">
                  <LinkIcon className="w-3 h-3" />
                  {expense.linkedSeason}
                </div>
              )}
              {expense.linkedTask && (
                <div className="flex items-center gap-2 text-foreground">
                  <LinkIcon className="w-3 h-3" />
                  {expense.linkedTask}
                </div>
              )}
            </div>
          </div>

          {expense.vendor && (
            <div>
              <p className="text-xs text-muted-foreground">Vendor</p>
              <p className="text-foreground">{expense.vendor}</p>
            </div>
          )}

          {expense.attachmentUrl && (
            <div>
              <p className="text-xs text-muted-foreground">Attachment</p>
              <Button
                variant="outline"
                className="mt-1 h-8 px-2 text-xs"
                onClick={() => window.open(expense.attachmentUrl, "_blank", "noopener,noreferrer")}
              >
                {expense.attachmentName ?? "Xem biên lai"}
                <ExternalLink className="w-3.5 h-3.5 ml-1" />
                Mở tab mới
              </Button>
            </div>
          )}

          <div>
            <p className="text-xs text-muted-foreground">Notes</p>
            <Textarea
              value={notes}
              onChange={(event) => setNotes(event.target.value)}
              className="min-h-[100px]"
            />
          </div>

          <Button
            className="w-full"
            onClick={() => onQuickUpdate(expense, { status, notes })}
          >
            Save Changes
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  );
}
