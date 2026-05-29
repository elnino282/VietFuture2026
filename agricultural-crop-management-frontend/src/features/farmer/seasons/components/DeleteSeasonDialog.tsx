import { Button } from "@/shared/ui/button";
import {
  AlertDialog,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/shared/ui";
import { Loader2 } from "lucide-react";
import { useTranslation } from "react-i18next";

interface DeleteSeasonDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onConfirm: () => void;
  isDeleting?: boolean;
}

export function DeleteSeasonDialog({
  open,
  onOpenChange,
  onConfirm,
  isDeleting,
}: DeleteSeasonDialogProps) {
  const { t } = useTranslation();
  const handleOpenChange = (nextOpen: boolean) => {
    if (isDeleting && !nextOpen) return;
    onOpenChange(nextOpen);
  };

  return (
    <AlertDialog open={open} onOpenChange={handleOpenChange}>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>{t("seasons.dialog.deleteTitle")}</AlertDialogTitle>
          <AlertDialogDescription>
            {t("seasons.dialog.deleteDescription")}
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel disabled={isDeleting}>
            {t("common.cancel")}
          </AlertDialogCancel>
          <Button
            onClick={onConfirm}
            variant="destructive"
            disabled={isDeleting}
          >
            {isDeleting ? (
              <>
                <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                {t("common.deleting")}
              </>
            ) : (
              t("seasons.dialog.deleteTitle")
            )}
          </Button>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  );
}
