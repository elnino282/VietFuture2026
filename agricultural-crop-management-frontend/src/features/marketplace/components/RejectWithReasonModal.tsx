import { useState } from "react";
import { AlertTriangle } from "lucide-react";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/shared/ui/dialog";
import { Button } from "@/shared/ui/button";
import { Label } from "@/shared/ui/label";
import { Textarea } from "@/shared/ui/textarea";
import { useI18n } from "@/shared/lib/hooks/useI18n";

type RejectWithReasonModalProps = {
  isOpen: boolean;
  onClose: () => void;
  onConfirm: (reason: string) => void;
  title: string;
  description: string;
  reasonLabel: string;
  reasonPlaceholder?: string;
  confirmButtonText?: string;
  isLoading?: boolean;
};

const MIN_REASON_LENGTH = 10;
const MAX_REASON_LENGTH = 500;

export function RejectWithReasonModal({
  isOpen,
  onClose,
  onConfirm,
  title,
  description,
  reasonLabel,
  reasonPlaceholder,
  confirmButtonText,
  isLoading = false,
}: RejectWithReasonModalProps) {
  const { t } = useI18n();
  const [reason, setReason] = useState("");
  const trimmedReason = reason.trim();
  const isValid = trimmedReason.length >= MIN_REASON_LENGTH && trimmedReason.length <= MAX_REASON_LENGTH;
  const showError = reason.length > 0 && !isValid;
  const resolvedReasonPlaceholder = reasonPlaceholder ?? t("admin.marketplace.components.reasonPlaceholder");
  const resolvedConfirmButtonText = confirmButtonText ?? t("common.confirm");

  const handleConfirm = () => {
    if (isValid) {
      onConfirm(trimmedReason);
      setReason("");
    }
  };

  const handleClose = () => {
    setReason("");
    onClose();
  };

  return (
    <Dialog open={isOpen} onOpenChange={(open) => !open && handleClose()}>
      <DialogContent className="sm:max-w-[500px]">
        <DialogHeader>
          <div className="flex items-center gap-3">
            <div className="rounded-lg bg-amber-100 p-2 text-amber-600">
              <AlertTriangle size={20} />
            </div>
            <DialogTitle>{title}</DialogTitle>
          </div>
          <DialogDescription className="pt-2">{description}</DialogDescription>
        </DialogHeader>

        <div className="space-y-2">
          <Label htmlFor="reason">{reasonLabel}</Label>
          <Textarea
            id="reason"
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            placeholder={resolvedReasonPlaceholder}
            rows={5}
            disabled={isLoading}
            maxLength={MAX_REASON_LENGTH}
            aria-invalid={showError}
            aria-describedby={showError ? "reason-error" : undefined}
          />
          <div className="flex items-center justify-between text-xs">
            <span id="reason-error" className={showError ? "text-red-600" : "text-gray-500"}>
              {showError && t("admin.marketplace.components.reasonValidation")}
            </span>
            <span className="text-gray-400">
              {reason.length} / {MAX_REASON_LENGTH}
            </span>
          </div>
        </div>

        <DialogFooter>
          <Button type="button" variant="outline" onClick={handleClose} disabled={isLoading}>
            {t("common.cancel")}
          </Button>
          <Button
            type="button"
            variant="destructive"
            onClick={handleConfirm}
            disabled={!isValid || isLoading}
          >
            {resolvedConfirmButtonText}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
