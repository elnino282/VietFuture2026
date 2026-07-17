import { useState, useEffect } from "react";
import { useI18n } from "@/shared/lib/hooks/useI18n";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
  Button,
  Label,
  Input,
  Checkbox,
} from "@/shared/ui";
import { SeasonEmployeeResponse, UpdateSeasonEmployeeRequest } from "@/api/generated/model";

interface UpdateTrainingDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  employee: SeasonEmployeeResponse | null;
  onSave: (employeeUserId: number, data: UpdateSeasonEmployeeRequest) => void;
  isPending?: boolean;
}

export function UpdateTrainingDialog({
  open,
  onOpenChange,
  employee,
  onSave,
  isPending,
}: UpdateTrainingDialogProps) {
  const { t } = useI18n();
  const [isTrained, setIsTrained] = useState(false);
  const [trainedAt, setTrainedAt] = useState("");
  const [trainingNotes, setTrainingNotes] = useState("");

  useEffect(() => {
    if (open && employee) {
      setIsTrained(employee.isTrained ?? false);
      setTrainedAt(employee.trainedAt ? employee.trainedAt.split("T")[0] : "");
      setTrainingNotes(employee.trainingNotes ?? "");
    }
  }, [open, employee]);

  const handleSave = () => {
    if (!employee || !employee.employeeUserId) return;
    
    // Only send trainedAt if isTrained is true
    let formattedTrainedAt = undefined;
    if (isTrained && trainedAt) {
      formattedTrainedAt = `${trainedAt}T00:00:00`;
    } else if (!isTrained) {
      formattedTrainedAt = undefined;
    }

    onSave(employee.employeeUserId, {
      isTrained,
      trainedAt: formattedTrainedAt,
      trainingNotes,
    });
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{t("laborWorkspace.training.updateTitle", "Cập nhật đào tạo")}</DialogTitle>
        </DialogHeader>
        <div className="space-y-4 py-4">
          <div className="flex items-center space-x-2">
            <Checkbox
              id="isTrained"
              checked={isTrained}
              onCheckedChange={(checked) => setIsTrained(checked as boolean)}
            />
            <Label htmlFor="isTrained">{t("laborWorkspace.training.isTrained", "Đã qua đào tạo")}</Label>
          </div>
          
          {isTrained && (
            <div className="space-y-2">
              <Label htmlFor="trainedAt">{t("laborWorkspace.training.trainedAt", "Ngày đào tạo")}</Label>
              <Input
                id="trainedAt"
                type="date"
                value={trainedAt}
                onChange={(e) => setTrainedAt(e.target.value)}
              />
            </div>
          )}
          
          <div className="space-y-2">
            <Label htmlFor="trainingNotes">{t("laborWorkspace.training.notes", "Ghi chú đào tạo")}</Label>
            <Input
              id="trainingNotes"
              value={trainingNotes}
              onChange={(e) => setTrainingNotes(e.target.value)}
              placeholder={t("laborWorkspace.training.notesPlaceholder", "Nhập ghi chú...")}
            />
          </div>
        </div>
        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)} disabled={isPending}>
            {t("common.cancel", "Hủy")}
          </Button>
          <Button onClick={handleSave} disabled={isPending}>
            {t("common.save", "Lưu")}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
