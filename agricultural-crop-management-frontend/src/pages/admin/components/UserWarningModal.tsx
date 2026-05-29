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
      nextErrors.description = "Description is required";
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
      toast.success("Warning sent successfully");
      onSuccess();
      onOpenChange(false);
    } catch (err) {
      console.error("Failed to send warning:", err);
      toast.error("Failed to send warning");
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
            Issue Warning
          </DialogTitle>
          <DialogDescription className="text-sm text-muted-foreground">
            Send a warning or lock decision to the selected user. The user will
            receive this in the farmer portal notifications.
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-4 py-4">
          <div className="rounded-[14px] border border-border bg-muted/30 p-3 text-sm">
            <div className="font-medium text-foreground">
              {user?.username || "Unknown user"}
            </div>
            {user?.email && (
              <div className="text-muted-foreground">{user.email}</div>
            )}
          </div>

          <div className="space-y-2">
            <Label htmlFor="warning-decision">
              Decision <span className="text-destructive">*</span>
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
                <SelectValue placeholder="Select a decision" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="WARNING">First warning (no lock)</SelectItem>
                <SelectItem value="LOCK_1_DAY">
                  Lock account for 1 day
                </SelectItem>
                <SelectItem value="LOCK_PERMANENT">
                  Lock account permanently
                </SelectItem>
              </SelectContent>
            </Select>
          </div>

          <div className="space-y-2">
            <Label htmlFor="warning-description">
              Violation Description <span className="text-destructive">*</span>
            </Label>
            <Textarea
              id="warning-description"
              placeholder="Describe the abnormal behavior or policy violation"
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
