import { Button } from "@/shared/ui/button";
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
import { useTranslation } from "react-i18next";

interface CloseSeasonDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  closeReason: string;
  setCloseReason: (reason: string) => void;
  onConfirm: () => void;
}

export function CloseSeasonDialog({
  open,
  onOpenChange,
  closeReason,
  setCloseReason,
  onConfirm,
}: CloseSeasonDialogProps) {
  const { t } = useTranslation();
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-[500px]">
        <DialogHeader>
          <DialogTitle>{t("seasons.dialog.closeTitle")}</DialogTitle>
          <DialogDescription>
            {t("seasons.dialog.closeDescription")}
          </DialogDescription>
        </DialogHeader>
        <div className="space-y-2 py-2">
          <Label htmlFor="close-reason" required>{t("seasons.dialog.closeReasonLabel")}</Label>
          <Textarea
            id="close-reason"
            value={closeReason}
            onChange={(e) => setCloseReason(e.target.value)}
            placeholder={t("seasons.dialog.closeReasonPlaceholder")}
            aria-invalid={!closeReason.trim()}
            aria-describedby={!closeReason.trim() ? "close-season-reason-error" : undefined}
            rows={4}
          />
          {!closeReason.trim() && (
            <p id="close-season-reason-error" className="text-sm text-destructive">
              {t("seasons.dialog.closeReasonRequired")}
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
            onClick={onConfirm}
            disabled={!closeReason.trim()}
          >
            {t("seasons.dialog.closeConfirm")}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
