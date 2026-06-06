import {
  BackButton,
  Button,
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  Input,
  Label,
  Textarea,
} from "@/shared/ui";
import { useI18n } from "@/shared/lib/hooks/useI18n";
import {
  adminRoleApi,
  type Role,
  type RoleCreateRequest,
  type RoleUpdateRequest,
} from "@/features/admin/shared/api";
import { Edit, FileText, Loader2, Plus, Save, Shield } from "lucide-react";
import { useEffect, useState } from "react";
import { toast } from "sonner";

export interface RoleFormData {
  code: string;
  name: string;
  description: string;
}

const initialFormData: RoleFormData = {
  code: "",
  name: "",
  description: "",
};

interface RoleFormModalProps {
  isOpen: boolean;
  onOpenChange: (open: boolean) => void;
  role: Role | null; // null for create, object for edit
  onSuccess: () => void;
}

export function RoleFormModal({
  isOpen,
  onOpenChange,
  role,
  onSuccess,
}: RoleFormModalProps) {
  const { t } = useI18n();
  const [formData, setFormData] = useState<RoleFormData>(initialFormData);
  const [loading, setLoading] = useState(false);
  const [errors, setErrors] = useState<
    Partial<Record<keyof RoleFormData, string>>
  >({});

  const isEditMode = !!role;
  const baselineFormData: RoleFormData = role
    ? {
        code: role.code || "",
        name: role.name || "",
        description: role.description || "",
      }
    : initialFormData;
  const isDirty = JSON.stringify(formData) !== JSON.stringify(baselineFormData);

  // Populate form when modal opens
  useEffect(() => {
    if (isOpen) {
      if (role) {
        // Edit mode: populate form with role data
        setFormData({
          code: role.code || "",
          name: role.name || "",
          description: role.description || "",
        });
      } else {
        // Create mode: reset form
        setFormData(initialFormData);
      }
      setErrors({});
    }
  }, [isOpen, role]);

  const validateForm = (): boolean => {
    const newErrors: Partial<Record<keyof RoleFormData, string>> = {};

    if (!isEditMode && (!formData.code || formData.code.trim().length === 0)) {
      newErrors.code = t("admin.roles.form.validation.codeRequired");
    }

    if (!formData.name || formData.name.trim().length === 0) {
      newErrors.name = t("admin.roles.form.validation.nameRequired");
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async () => {
    if (!validateForm()) {
      return;
    }

    setLoading(true);
    try {
      if (isEditMode && role) {
        // Update existing role
        const updateData: RoleUpdateRequest = {
          name: formData.name,
          description: formData.description || undefined,
        };
        await adminRoleApi.update(Number(role.id), updateData);
        toast.success(t("admin.roles.form.toast.updated"));
      } else {
        // Create new role
        const createData: RoleCreateRequest = {
          code: formData.code.toUpperCase().replace(/\s+/g, "_"),
          name: formData.name,
          description: formData.description || undefined,
        };
        await adminRoleApi.create(createData);
        toast.success(t("admin.roles.form.toast.created"));
      }
      onSuccess();
      onOpenChange(false);
    } catch (err: any) {
      console.error("Failed to save role:", err);
      const errorCode = err?.response?.data?.code;
      if (errorCode === "ROLE_CODE_EXISTS") {
        setErrors({ code: t("admin.roles.form.validation.codeExists") });
      } else {
        toast.error(
          isEditMode ? t("admin.roles.form.toast.updateFailed") : t("admin.roles.form.toast.createFailed"),
        );
      }
    } finally {
      setLoading(false);
    }
  };

  const handleClose = () => {
    if (
      isDirty &&
      !window.confirm(t("common.unsavedChangesConfirm", "You have unsaved changes. Leave this page?"))
    ) {
      return;
    }

    onOpenChange(false);
    setFormData(initialFormData);
    setErrors({});
  };

  return (
    <Dialog
      open={isOpen}
      onOpenChange={(nextOpen) => {
        if (nextOpen) {
          onOpenChange(true);
        } else {
          handleClose();
        }
      }}
    >
      <DialogContent className="max-w-md rounded-[18px] border-border bg-card shadow-sm">
        <DialogHeader>
          <BackButton onClick={handleClose} className="w-fit" />
          <DialogTitle className="flex items-center gap-2 text-xl">
            {isEditMode ? (
              <>
                <Edit className="w-5 h-5 text-primary" />
                {t("admin.roles.form.editTitle")}
              </>
            ) : (
              <>
                <Plus className="w-5 h-5 text-green-600" />
                {t("admin.roles.form.addTitle")}
              </>
            )}
          </DialogTitle>
          <DialogDescription className="text-sm text-muted-foreground">
            {isEditMode
              ? t("admin.roles.form.editDescription")
              : t("admin.roles.form.addDescription")}{" "}
            {t("admin.roles.form.requiredHint")}
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-4 py-4">
          {/* Code (only for create mode) */}
          <div className="space-y-2">
            <Label htmlFor="code">
              {t("admin.roles.table.code")} <span className="text-destructive">*</span>
            </Label>
            <div className="relative">
              <Shield className="absolute left-3 top-1/2 transform -translate-y-1/2 w-4 h-4 text-muted-foreground" />
              <Input
                id="code"
                placeholder={isEditMode ? "" : t("admin.roles.form.codePlaceholder")}
                value={formData.code}
                onChange={(e) =>
                  setFormData({ ...formData, code: e.target.value })
                }
                className={`rounded-[14px] pl-10 uppercase ${errors.code ? "border-destructive" : ""}`}
                disabled={isEditMode}
              />
            </div>
            {errors.code && (
              <p className="text-xs text-destructive">{errors.code}</p>
            )}
            {isEditMode && (
              <p className="text-xs text-muted-foreground">
                {t("admin.roles.form.codeLocked")}
              </p>
            )}
          </div>

          {/* Name */}
          <div className="space-y-2">
            <Label htmlFor="name">
              {t("admin.roles.table.name")} <span className="text-destructive">*</span>
            </Label>
            <Input
              id="name"
              placeholder={t("admin.roles.form.namePlaceholder")}
              value={formData.name}
              onChange={(e) =>
                setFormData({ ...formData, name: e.target.value })
              }
              className={`rounded-[14px] ${errors.name ? "border-destructive" : ""}`}
            />
            {errors.name && (
              <p className="text-xs text-destructive">{errors.name}</p>
            )}
          </div>

          {/* Description */}
          <div className="space-y-2">
            <Label htmlFor="description" className="flex items-center gap-2">
              <FileText className="w-4 h-4" />
              {t("common.description")}
            </Label>
            <Textarea
              id="description"
              placeholder={t("admin.roles.form.descriptionPlaceholder")}
              value={formData.description}
              onChange={(e) =>
                setFormData({ ...formData, description: e.target.value })
              }
              className="rounded-[14px]"
              rows={3}
            />
          </div>
        </div>

        <DialogFooter>
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
            disabled={loading}
            className="rounded-[14px] bg-primary hover:bg-primary/90"
          >
            {loading ? (
              <Loader2 className="w-4 h-4 mr-2 animate-spin" />
            ) : (
              <Save className="w-4 h-4 mr-2" />
            )}
            {isEditMode ? t("common.update") : t("common.create")} {t("admin.roles.form.role")}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
