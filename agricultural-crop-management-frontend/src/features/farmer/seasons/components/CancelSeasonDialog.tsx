import { Button } from "@/shared/ui/button";
import { Checkbox } from "@/shared/ui/checkbox";
import { Label } from "@/shared/ui/label";
import { Textarea } from "@/shared/ui/textarea";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/shared/ui";
import { Ban } from "lucide-react";
import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import type { Season } from "../types";

interface CancelSeasonDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  season: Season | null;
  onConfirm: (data: { reason?: string; forceCancel?: boolean }) => void;
  isSubmitting?: boolean;
}

export function CancelSeasonDialog({
  open,
  onOpenChange,
  season,
  onConfirm,
  isSubmitting = false,
}: CancelSeasonDialogProps) {
  const { t } = useTranslation();
  const [reason, setReason] = useState("");
  const [forceCancel, setForceCancel] = useState(false);

  useEffect(() => {
    if (!open) return;
    setReason("");
    setForceCancel(false);
  }, [open, season?.id]);

  const handleSubmit = () => {
    if (!season || isSubmitting) return;
    onConfirm({
      reason: reason.trim() || undefined,
      forceCancel,
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
            <Ban className="w-5 h-5 text-destructive" />
            {t("seasons.dialog.cancelTitle")}
          </DialogTitle>
          <DialogDescription>
            {t("seasons.dialog.cancelDetailedDescription")}
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-4 py-2">
          <div className="space-y-2">
            <Label htmlFor="cancelReason">{t("seasons.dialog.cancelReasonLabel")}</Label>
            <Textarea
              id="cancelReason"
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              placeholder={t("seasons.dialog.cancelReasonPlaceholder")}
              disabled={isSubmitting}
              rows={4}
            />
          </div>

          <div className="flex items-start gap-3">
            <Checkbox
              checked={forceCancel}
              onCheckedChange={(checked) => setForceCancel(Boolean(checked))}
              disabled={isSubmitting}
              className="mt-1"
            />
            <div className="text-sm text-muted-foreground">
              {t("seasons.dialog.forceCancelHelp")}
            </div>
          </div>
        </div>

        <DialogFooter>
          <Button
            variant="outline"
            onClick={() => onOpenChange(false)}
            disabled={isSubmitting}
          >
            {t("seasons.dialog.keepSeason")}
          </Button>
          <Button
            onClick={handleSubmit}
            disabled={isSubmitting}
            variant="destructive"
          >
            {isSubmitting ? t("seasons.dialog.cancelling") : t("seasons.dialog.cancelConfirm")}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
