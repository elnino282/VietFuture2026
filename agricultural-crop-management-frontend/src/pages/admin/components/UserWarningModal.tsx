import {
  Button,
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  Label,
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
  Textarea,
} from "@/shared/ui";
import { useI18n } from "@/shared/lib/hooks/useI18n";
import {
  adminUsersApi,
  type AdminUser,
  type AdminUserWarningDecision,
} from "@/features/admin/shared/api";
import { AlertTriangle, Loader2 } from "lucide-react";
import { useEffect, useState } from "react";
import { toast } from "sonner";

interface UserWarningModalProps {
  isOpen: boolean;
  onOpenChange: (open: boolean) => void;
  user: AdminUser | null;
  onSuccess: () => void;
}

interface WarningFormData {
  description: string;
  decision: AdminUserWarningDecision;
}

const initialFormData: WarningFormData = {
  description: "",
  decision: "WARNING",
};

export function UserWarningModal({
  isOpen,
  onOpenChange,
  user,
  onSuccess,
}: UserWarningModalProps) {
  const { t } = useI18n();
  const [formData, setFormData] = useState<WarningFormData>(initialFormData);
  const [loading, setLoading] = useState(false);
  const [errors, setErrors] = useState<{ description?: string }>({});

  useEffect(() => {
    if (isOpen) {
      setFormData(initialFormData);
      setErrors({});
    }
  }, [isOpen, user]);

  const validateForm = (): boolean => {
    const nextErrors: { description?: string } = {};
    if (!formData.description.trim()) {
      nextErrors.description = t("warnings.validation.descriptionRequired");
    }
    setErrors(nextErrors);
    return Object.keys(nextErrors).length === 0;
  };

  const handleSubmit = async () => {
    if (!user) return;
    if (!validateForm()) return;

    setLoading(true);
    try {
      await adminUsersApi.warn(Number(user.id), {
        description: formData.description.trim(),
        decision: formData.decision,
      });
      toast.success(t("warnings.toast.sent"));
      onSuccess();
      onOpenChange(false);
    } catch (err) {
      console.error("Failed to send warning:", err);
      toast.error(t("warnings.toast.sendFailed"));
    } finally {
      setLoading(false);
    }
  };

  const handleClose = () => {
    onOpenChange(false);
    setFormData(initialFormData);
    setErrors({});
  };

  return (
    <Dialog open={isOpen} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-lg rounded-[18px] border-border bg-card shadow-sm">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2 text-xl">
            <AlertTriangle className="w-5 h-5 text-amber-500" />
            {t("warnings.issueTitle")}
          </DialogTitle>
          <DialogDescription className="text-sm text-muted-foreground">
            {t("warnings.issueDescription")}
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-4 py-4">
          <div className="rounded-[14px] border border-border bg-muted/30 p-3 text-sm">
            <div className="font-medium text-foreground">
              {user?.username || t("admin.users.detail.unknownUser")}
            </div>
            {user?.email && (
              <div className="text-muted-foreground">{user.email}</div>
            )}
          </div>

          <div className="space-y-2">
            <Label htmlFor="warning-decision">
              {t("warnings.decision")} <span className="text-destructive">*</span>
            </Label>
            <Select
              value={formData.decision}
              onValueChange={(value) =>
                setFormData((prev) => ({
                  ...prev,
                  decision: value as AdminUserWarningDecision,
                }))
              }
            >
              <SelectTrigger id="warning-decision" className="rounded-[14px]">
                <SelectValue placeholder={t("warnings.selectDecision")} />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="WARNING">{t("warnings.decisions.WARNING")}</SelectItem>
                <SelectItem value="LOCK_1_DAY">
                  {t("warnings.decisions.LOCK_1_DAY")}
                </SelectItem>
                <SelectItem value="LOCK_PERMANENT">
                  {t("warnings.decisions.LOCK_PERMANENT")}
                </SelectItem>
              </SelectContent>
            </Select>
          </div>

          <div className="space-y-2">
            <Label htmlFor="warning-description">
              {t("warnings.violationDescription")} <span className="text-destructive">*</span>
            </Label>
            <Textarea
              id="warning-description"
              placeholder={t("warnings.descriptionPlaceholder")}
              value={formData.description}
              onChange={(event) =>
                setFormData((prev) => ({
                  ...prev,
                  description: event.target.value,
                }))
              }
              className={`rounded-[14px] ${errors.description ? "border-destructive" : ""}`}
              rows={4}
            />
            {errors.description && (
              <p className="text-xs text-destructive">{errors.description}</p>
            )}
          </div>
        </div>

        <DialogFooter className="gap-2 sm:gap-0">
          <Button
            variant="outline"
            onClick={handleClose}
            disabled={loading}
            className="rounded-[14px]"
          >
            {t("common.cancel")}
          </Button>
          <Button
            onClick={handleSubmit}
            disabled={loading || !user}
            className="rounded-[14px]"
          >
            {loading ? (
              <>
                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                {t("warnings.sending")}
              </>
            ) : (
              t("warnings.sendWarning")
            )}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
